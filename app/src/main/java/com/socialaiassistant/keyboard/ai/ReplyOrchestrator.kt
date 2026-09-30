package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.backend.BackendErrorMessages
import com.socialaiassistant.keyboard.backend.BackendException
import com.socialaiassistant.keyboard.backend.ManagedAiPayload
import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.ContextFreshnessPolicy
import com.socialaiassistant.keyboard.context.ConversationKeyFactory
import com.socialaiassistant.keyboard.context.ConversationSurface
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.context.MAX_LIVE_CHAT_MESSAGES
import com.socialaiassistant.keyboard.ime.ImeSession
import com.socialaiassistant.keyboard.memory.ConversationHistory
import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.settings.AppSettings
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReplyOrchestrator(
    private val scope: CoroutineScope,
    private val snapshots: StateFlow<ContextSnapshot?>,
    private val sessions: StateFlow<ImeSession?>,
    private val mergeHistory: suspend (ContextSnapshot) -> ConversationHistory,
    private val settingsProvider: suspend () -> AppSettings,
    private val apiKeyConfigured: () -> Boolean,
    private val managedSessionAvailable: () -> Boolean,
    private val promptBuilder: ExtensionPromptBuilder,
    private val gateway: AiGateway,
    private val resultParser: ModelResultParser,
    private val keyFactory: ConversationKeyFactory = ConversationKeyFactory(),
    private val intentResolver: ConversationAiIntentResolver = ConversationAiIntentResolver(),
    observerScope: CoroutineScope = scope
) {
    private val _state = MutableStateFlow<ReplyState>(ReplyState.Hidden)
    val state: StateFlow<ReplyState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var requestToken: Long = 0L

    init {
        sessionObserverJob = observerScope.launch {
            sessions.collectLatest { session ->
                if (session == null || session.safety != FieldSafety.ALLOW_AI) {
                    clear()
                }
            }
        }
    }

    fun requestAuto() {
        if (activeJob?.isActive == true) return
        val session = sessions.value
        val snapshot = snapshots.value
        val token = ++requestToken
        activeJob = scope.launch {
            try {
                processExplicit(session, snapshot, token)
            } finally {
                if (requestToken == token) activeJob = null
            }
        }
    }

    fun clear() {
        requestToken++
        activeJob?.cancel()
        activeJob = null
        _state.value = ReplyState.Hidden
    }

    fun destroy() {
        clear()
        sessionObserverJob?.cancel()
        sessionObserverJob = null
    }

    private suspend fun processExplicit(session: ImeSession?, snapshot: ContextSnapshot?, token: Long) {
        if (session == null || session.safety != FieldSafety.ALLOW_AI) {
            publishIfCurrent(token, ReplyState.Hidden)
            return
        }
        val packageName = session.packageName?.takeIf { it.isNotBlank() }
        if (packageName == null || !ContextFreshnessPolicy.isUsableForPackage(snapshot, packageName)) {
            publishIfCurrent(token, ReplyState.WaitingForContext)
            return
        }
        val safeSnapshot = requireNotNull(snapshot)

        val settings = settingsProvider()
        if (!settings.aiPrivacyConsent ||
            !settings.contextAccessConsent ||
            !settings.accessibilityDisclosureAccepted
        ) {
            publishIfCurrent(token, ReplyState.Hidden)
            return
        }
        if (settings.gatewayMode == GatewayMode.PERSONAL && !apiKeyConfigured()) {
            publishIfCurrent(token, ReplyState.NeedsApiKey)
            return
        }
        if (settings.gatewayMode == GatewayMode.MANAGED && !managedSessionAvailable()) {
            publishIfCurrent(token, ReplyState.Error("Managed AI ব্যবহার করতে আগে Login করুন।"))
            return
        }

        if (safeSnapshot.surface == ConversationSurface.COMMENT) {
            publishIfCurrent(token, ReplyState.Hidden)
            return
        }

        val intent = intentResolver.resolve(safeSnapshot)
        if (intent == ConversationAiIntent.NEEDS_CONTEXT) {
            publishIfCurrent(token, ReplyState.WaitingForContext)
            return
        }

        publishIfCurrent(token, ReplyState.Loading)
        try {
            val history = mergeHistory(safeSnapshot)
            val promptRequest = buildPromptRequest(safeSnapshot, history, settings, intent)
            val prompt = promptBuilder.build(promptRequest)
            val personalTraining = listOf(settings.personalTraining, settings.customInstruction)
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .take(1800)
            val managedPayload = ManagedAiPayload.social(
                type = promptRequest.type,
                mode = promptRequest.mode,
                messages = promptRequest.messages,
                latestRecipientMessage = promptRequest.latestRecipientMessage,
                postText = if (promptRequest.type == InteractionType.COMMENT) {
                    promptRequest.messages.joinToString("\n") { it.text }
                } else {
                    ""
                },
                languageMode = promptRequest.cachedLanguageMode,
                personalTraining = personalTraining,
                conversationIntent = intent
            )
            val generation = gateway.generate(
                AiGenerationRequest(
                    prompt = prompt,
                    modelMode = settings.modelMode,
                    includeConversationMemory = false,
                    managedPayload = managedPayload
                )
            )
            if (!isCurrent(token)) return
            val parsed = resultParser.parse(generation.rawText)
                ?: run {
                    publishIfCurrent(token, ReplyState.Error("AI returned an unreadable reply. Tap again to retry."))
                    return
                }
            val sanitizedReply = enforceContextualAccuracy(
                sanitizeHumanReply(parsed.reply, promptRequest.latestRecipientMessage),
                promptRequest.latestRecipientMessage
            )
            val rawReply = InboxBanglaReplyPolicy.ensureBangla(
                reply = sanitizedReply,
                action = ManualAiAction.SMART,
                intent = intent,
                latestRecipientMessage = promptRequest.latestRecipientMessage
            )
            val rawReplies = parsed.replies.map {
                InboxBanglaReplyPolicy.ensureBangla(
                    reply = enforceContextualAccuracy(
                        sanitizeHumanReply(it, promptRequest.latestRecipientMessage),
                        promptRequest.latestRecipientMessage
                    ),
                    action = ManualAiAction.SMART,
                    intent = intent,
                    latestRecipientMessage = promptRequest.latestRecipientMessage
                )
            }.distinct().ifEmpty { listOf(rawReply) }
            publishIfCurrent(token, ReplyState.Ready(reply = rawReply, replies = rawReplies, fingerprint = fingerprint(safeSnapshot, settings, intent)))
        } catch (error: CancellationException) {
            throw error
        } catch (error: BackendException) {
            publishIfCurrent(token, ReplyState.Error(BackendErrorMessages.userMessage(error)))
        } catch (_: AiGatewayException.InvalidApiKey) {
            publishIfCurrent(token, ReplyState.NeedsApiKey)
        } catch (_: AiGatewayException.Offline) {
            publishIfCurrent(token, localFallbackState(safeSnapshot, intent, settings))
        } catch (_: AiGatewayException.RateLimited) {
            publishIfCurrent(token, ReplyState.Error("AI rate limit reached. Please try again shortly."))
        } catch (_: AiGatewayException.InsufficientCredits) {
            publishIfCurrent(token, ReplyState.Error("OpenRouter credits are insufficient. Update your account and retry."))
        } catch (_: AiGatewayException.Timeout) {
            publishIfCurrent(token, ReplyState.Error("AI request timed out. Typing still works normally."))
        } catch (_: AiGatewayException.MalformedResponse) {
            publishIfCurrent(token, ReplyState.Error("AI returned an unreadable reply. Please retry."))
        } catch (_: AiGatewayException.ProviderFailure) {
            publishIfCurrent(token, localFallbackState(safeSnapshot, intent, settings))
        } catch (_: Exception) {
            publishIfCurrent(token, ReplyState.Error("Smart Reply is temporarily unavailable."))
        }
    }

    private fun localFallbackState(
        snapshot: ContextSnapshot,
        intent: ConversationAiIntent,
        settings: AppSettings
    ): ReplyState {
        val reply = InboxBanglaReplyPolicy.ensureBangla(
            reply = "",
            action = ManualAiAction.SMART,
            intent = intent,
            latestRecipientMessage = snapshot.latestRecipientMessage
        )
        return ReplyState.Ready(
            reply = reply,
            replies = listOf(reply),
            fingerprint = fingerprint(snapshot, settings, intent)
        )
    }

    private fun buildPromptRequest(
        snapshot: ContextSnapshot,
        history: ConversationHistory,
        settings: AppSettings,
        intent: ConversationAiIntent
    ): PromptRequest {
        val contextLimit = minOf(settings.maxChatMessages.coerceAtLeast(1), MAX_LIVE_CHAT_MESSAGES)
        val selected = history.messages.takeLast(contextLimit)
        val interactionType = if (snapshot.surface == ConversationSurface.COMMENT) {
            InteractionType.COMMENT
        } else {
            InteractionType.INBOX
        }

        val recipientMessage = snapshot.latestRecipientMessage
            ?.takeIf { it.isNotBlank() }
            ?: selected.asReversed()
                .firstOrNull { it.sender == SenderClass.RECIPIENT.name }
                ?.text

        return PromptRequest(
            type = interactionType,
            mode = AiMode.GENERAL,
            messages = selected.map { message ->
                PromptMessage(
                    sender = runCatching { SenderClass.valueOf(message.sender) }
                        .getOrDefault(SenderClass.UNKNOWN),
                    text = message.text,
                    timestampHint = message.timestampHint
                )
            },
            latestRecipientMessage = recipientMessage,
            cachedLanguageMode = LanguageMode.BENGALI_DEFAULT,
            tonePreset = settings.tonePreset,
            customInstruction = settings.customInstruction,
            createConversationMemory = false,
            conversationIntent = intent
        )
    }

    fun enforceContextualAccuracy(aiOutput: String, incoming: String?): String =
        sanitizeHumanReply(aiOutput, incoming)

    fun sanitizeHumanReply(rawResponse: String, latestIncoming: String?): String {
        val clean = rawResponse
            .replace(Regex("^(reply|answer|comment)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        return clean
    }

    fun ensureHumanLikeResponse(generated: String, incoming: String?): String =
        sanitizeHumanReply(generated, incoming)

    private fun publishIfCurrent(token: Long, state: ReplyState) {
        if (isCurrent(token)) _state.value = state
    }

    private fun isCurrent(token: Long): Boolean =
        token == requestToken && sessions.value?.safety == FieldSafety.ALLOW_AI

    private fun fingerprint(
        snapshot: ContextSnapshot,
        settings: AppSettings,
        intent: ConversationAiIntent
    ): String {
        val conversationKey = keyFactory.create(snapshot).value
        val messageHashes = snapshot.messages
            .takeLast(minOf(settings.maxChatMessages.coerceAtLeast(1), MAX_LIVE_CHAT_MESSAGES))
            .joinToString("|") { message ->
                "${message.sender.name}:${sha256(ExtensionLanguageLogic.cleanString(message.text, 2_000))}"
            }
        val latestRecipientHash = sha256(
            ExtensionLanguageLogic.cleanString(snapshot.latestRecipientMessage, 2_000)
        )
        val material = listOf(
            conversationKey,
            messageHashes,
            latestRecipientHash,
            snapshot.surface.name,
            settings.modelMode.name,
            settings.maxChatMessages.toString(),
            settings.customKnowledgeRevision.toString(),
            AiMode.GENERAL.name,
            intent.name,
            requestToken.toString()
        ).joinToString("|")
        return sha256(material)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
