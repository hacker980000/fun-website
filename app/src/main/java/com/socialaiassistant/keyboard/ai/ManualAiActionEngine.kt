package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.backend.BackendErrorMessages
import com.socialaiassistant.keyboard.backend.BackendException
import com.socialaiassistant.keyboard.backend.ManagedAiPayload
import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.ConversationSurface
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.safety.FieldSafety
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManualAiEngineSettings(
    val modelMode: ModelMode = ModelMode.FAST,
    val gatewayMode: GatewayMode = GatewayMode.MANAGED,
    val managedSessionAvailable: Boolean = false,
    val aiPrivacyConsent: Boolean = false,
    val contextAccessConsent: Boolean = false,
    val accessibilityDisclosureAccepted: Boolean = false,
    val maxChatMessages: Int = 30,
    val tonePreset: TonePreset = TonePreset.AUTO,
    val customInstruction: String = "",
    val personalTraining: String = ""
)

sealed interface ManualAiState {
    data object Idle : ManualAiState
    data class Loading(val action: ManualAiAction) : ManualAiState
    data class Ready(val action: ManualAiAction, val reply: String) : ManualAiState
    data object NeedsApiKey : ManualAiState
    data object NeedsContext : ManualAiState
    data object NeedsDraft : ManualAiState
    data object Blocked : ManualAiState
    data object Offline : ManualAiState
    data class Error(val message: String) : ManualAiState
}

class ManualAiActionEngine(
    private val scope: CoroutineScope,
    private val contextProvider: () -> ContextSnapshot?,
    private val historyProvider: suspend (ContextSnapshot, Int) -> List<PromptMessage>,
    private val settingsProvider: suspend () -> ManualAiEngineSettings,
    private val apiKeyConfigured: () -> Boolean,
    private val promptBuilder: ManualAiPromptBuilder,
    private val gateway: AiGateway,
    private val parseReply: (String) -> String?
) {
    private val _state = MutableStateFlow<ManualAiState>(ManualAiState.Idle)
    val state: StateFlow<ManualAiState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private var lastRequest: PendingRequest? = null
    private val intentResolver = ConversationAiIntentResolver()

    fun request(
        action: ManualAiAction,
        draftText: String = "",
        translateTargetLanguage: String = "English",
        safety: FieldSafety
    ) {
        val pending = PendingRequest(action, draftText, translateTargetLanguage, safety)
        lastRequest = pending
        launch(pending)
    }

    fun regenerate(safety: FieldSafety) {
        val prior = lastRequest ?: return
        val pending = prior.copy(safety = safety)
        lastRequest = pending
        launch(pending)
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        lastRequest = null
        _state.value = ManualAiState.Idle
    }

    private fun launch(request: PendingRequest) {
        activeJob?.cancel()
        activeJob = scope.launch {
            execute(request)
        }
    }

    private suspend fun execute(request: PendingRequest) {
        if (request.safety == FieldSafety.BLOCK_AI) {
            _state.value = ManualAiState.Blocked
            return
        }

        val settings = settingsProvider()
        if (!settings.aiPrivacyConsent) {
            _state.value = ManualAiState.Error("AI ব্যবহার করতে আগে Settings-এ Privacy/Data consent দিন।")
            return
        }
        if (settings.gatewayMode == GatewayMode.PERSONAL && !apiKeyConfigured()) {
            _state.value = ManualAiState.NeedsApiKey
            return
        }
        if (settings.gatewayMode == GatewayMode.MANAGED && !settings.managedSessionAvailable) {
            val canUsePersonalDraftFallback = request.action.isDraftAction && apiKeyConfigured()
            if (!canUsePersonalDraftFallback) {
                _state.value = ManualAiState.Error("Managed AI ব্যবহার করতে আগে Login করুন।")
                return
            }
        }

        if (request.action.isDraftAction && request.draftText.isBlank()) {
            _state.value = ManualAiState.NeedsDraft
            return
        }

        val snapshot = contextProvider()
        if (!request.action.isDraftAction) {
            if (request.safety != FieldSafety.ALLOW_AI ||
                !settings.contextAccessConsent ||
                !settings.accessibilityDisclosureAccepted ||
                snapshot == null
            ) {
                _state.value = ManualAiState.NeedsContext
                return
            }
        }

        _state.value = ManualAiState.Loading(request.action)

        try {
            var managedPayload: kotlinx.serialization.json.JsonObject? = null
            val prompt = if (request.action.isDraftAction) {
                promptBuilder.build(
                    ManualAiRequest(
                        action = request.action,
                        draftText = request.draftText,
                        translateTargetLanguage = request.translateTargetLanguage,
                        tonePreset = settings.tonePreset,
                        customInstruction = settings.customInstruction
                    )
                )
            } else {
                val safeSnapshot = snapshot ?: run {
                    _state.value = ManualAiState.NeedsContext
                    return
                }
                val history = historyProvider(safeSnapshot, settings.maxChatMessages)
                val interactionType = if (safeSnapshot.surface == ConversationSurface.COMMENT) {
                    InteractionType.COMMENT
                } else {
                    InteractionType.INBOX
                }
                val activeCommentText = if (interactionType == InteractionType.COMMENT) {
                    activeCommentText(safeSnapshot, history)
                } else {
                    ""
                }
                val conversationIntent = intentResolver.resolve(safeSnapshot)
                if (interactionType == InteractionType.INBOX && conversationIntent == ConversationAiIntent.NEEDS_CONTEXT) {
                    _state.value = ManualAiState.NeedsContext
                    return
                }
                val cachedLanguage = history.asReversed()
                    .asSequence()
                    .filter { it.sender == com.socialaiassistant.keyboard.context.SenderClass.RECIPIENT }
                    .mapNotNull { ExtensionLanguageLogic.detectLanguageMode(it.text) }
                    .firstOrNull()
                val mode = when (request.action) {
                    ManualAiAction.SMART -> AiMode.GENERAL
                    ManualAiAction.WITTY -> AiMode.WITTY
                    ManualAiAction.FLIRTY -> if (interactionType == InteractionType.COMMENT) AiMode.FLIRT_CMT else AiMode.FLIRT_MSG
                    ManualAiAction.FUNNY -> if (interactionType == InteractionType.COMMENT) AiMode.FUNNY_CMT else AiMode.WITTY
                    else -> AiMode.GENERAL
                }
                val personalTraining = listOf(settings.personalTraining, settings.customInstruction)
                    .filter { it.isNotBlank() }.joinToString("\n").take(1800)
                managedPayload = ManagedAiPayload.social(
                    type = interactionType,
                    mode = mode,
                    messages = history.takeLast(settings.maxChatMessages),
                    latestRecipientMessage = safeSnapshot.latestRecipientMessage,
                    postText = activeCommentText,
                    languageMode = cachedLanguage,
                    personalTraining = personalTraining,
                    conversationIntent = conversationIntent
                )
                promptBuilder.build(
                    ManualAiRequest(
                        action = request.action,
                        interactionType = interactionType,
                        conversationIntent = conversationIntent,
                        messages = history.takeLast(settings.maxChatMessages),
                        latestRecipientMessage = safeSnapshot.latestRecipientMessage,
                        cachedLanguageMode = cachedLanguage,
                        postText = activeCommentText,
                        tonePreset = settings.tonePreset,
                        customInstruction = settings.customInstruction
                    )
                )
            }

            val generation = gateway.generate(
                AiGenerationRequest(
                    prompt = prompt,
                    modelMode = settings.modelMode,
                    includeConversationMemory = false,
                    managedPayload = managedPayload
                )
            )
            val reply = parseReply(generation.rawText)?.trim().orEmpty()
            if (reply.isEmpty()) {
                _state.value = ManualAiState.Error("AI returned an unreadable reply. Tap again to retry.")
                return
            }
            _state.value = ManualAiState.Ready(request.action, reply)
        } catch (error: CancellationException) {
            throw error
        } catch (error: BackendException) {
            _state.value = ManualAiState.Error(BackendErrorMessages.userMessage(error))
        } catch (_: AiGatewayException.InvalidApiKey) {
            _state.value = ManualAiState.NeedsApiKey
        } catch (_: AiGatewayException.Offline) {
            _state.value = ManualAiState.Offline
        } catch (_: AiGatewayException.RateLimited) {
            _state.value = ManualAiState.Error("AI rate limit reached. Try again shortly.")
        } catch (_: AiGatewayException.InsufficientCredits) {
            _state.value = ManualAiState.Error("OpenRouter credits are insufficient.")
        } catch (_: AiGatewayException.Timeout) {
            _state.value = ManualAiState.Error("AI request timed out. Normal typing still works.")
        } catch (_: AiGatewayException.MalformedResponse) {
            _state.value = ManualAiState.Error("AI returned an unreadable result.")
        } catch (_: AiGatewayException.ProviderFailure) {
            _state.value = ManualAiState.Error("AI provider is temporarily unavailable.")
        } catch (_: IllegalArgumentException) {
            _state.value = ManualAiState.NeedsDraft
        } catch (_: Exception) {
            _state.value = ManualAiState.Error("AI action is temporarily unavailable.")
        }
    }

    private fun activeCommentText(snapshot: ContextSnapshot, history: List<PromptMessage>): String {
        return snapshot.latestRecipientMessage
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: snapshot.messages.asReversed()
                .firstOrNull { it.sender != SenderClass.SELF && it.text.isNotBlank() }
                ?.text
                ?.trim()
            ?: history.asReversed()
                .firstOrNull { it.sender != SenderClass.SELF && it.text.isNotBlank() }
                ?.text
                ?.trim()
            ?: ""
    }

    private data class PendingRequest(
        val action: ManualAiAction,
        val draftText: String,
        val translateTargetLanguage: String,
        val safety: FieldSafety
    )
}
