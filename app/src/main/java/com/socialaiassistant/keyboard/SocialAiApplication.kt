package com.socialaiassistant.keyboard

import android.app.Application
import com.socialaiassistant.keyboard.ai.AiGateway
import com.socialaiassistant.keyboard.ai.AiSettingsRepository
import com.socialaiassistant.keyboard.ai.ExtensionPromptBuilder
import com.socialaiassistant.keyboard.ai.GatewayRouter
import com.socialaiassistant.keyboard.ai.GatewayMode
import com.socialaiassistant.keyboard.ai.ManualAiActionEngine
import com.socialaiassistant.keyboard.ai.ManualAiEngineSettings
import com.socialaiassistant.keyboard.ai.ManualAiPromptBuilder
import com.socialaiassistant.keyboard.ai.ModelResultParser
import com.socialaiassistant.keyboard.ai.OpenRouterGateway
import com.socialaiassistant.keyboard.ai.PromptMessage
import com.socialaiassistant.keyboard.ai.ReplyOrchestrator
import com.socialaiassistant.keyboard.backend.DeviceProofManager
import com.socialaiassistant.keyboard.backend.InstallationIdentity
import com.socialaiassistant.keyboard.backend.ManagedSessionStore
import com.socialaiassistant.keyboard.backend.SocialAiBackendClient
import com.socialaiassistant.keyboard.context.ContextAccessGate
import com.socialaiassistant.keyboard.context.ContextSnapshotBus
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.crypto.AesGcmCipher
import com.socialaiassistant.keyboard.crypto.SecretStore
import com.socialaiassistant.keyboard.ime.ImeSessionRegistry
import com.socialaiassistant.keyboard.ime.RecentClipboardStore
import com.socialaiassistant.keyboard.memory.AppDatabase
import com.socialaiassistant.keyboard.memory.ConversationRepository
import com.socialaiassistant.keyboard.network.SecureHttpClientFactory
import com.socialaiassistant.keyboard.settings.SettingsRepository
import com.socialaiassistant.keyboard.theme.ThemeBackgroundManager
import com.socialaiassistant.keyboard.theme.ThemeRenderer
import com.socialaiassistant.keyboard.theme.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SocialAiApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var secretStore: SecretStore
        private set
    lateinit var aiSettingsRepository: AiSettingsRepository
        private set
    lateinit var managedSessionStore: ManagedSessionStore
        private set
    lateinit var installationIdentity: InstallationIdentity
        private set
    lateinit var deviceProofManager: DeviceProofManager
        private set
    lateinit var backendClient: SocialAiBackendClient
        private set
    lateinit var personalAiGateway: AiGateway
        private set
    lateinit var aiGateway: AiGateway
        private set
    lateinit var replyOrchestrator: ReplyOrchestrator
        private set
    lateinit var manualAiActionEngine: ManualAiActionEngine
        private set
    lateinit var conversationRepository: ConversationRepository
        private set
    lateinit var themeRepository: ThemeRepository
        private set
    lateinit var themeRenderer: ThemeRenderer
        private set
    lateinit var themeBackgroundManager: ThemeBackgroundManager
        private set

    override fun onCreate() {
        super.onCreate()

        settingsRepository = SettingsRepository.create(this)
        val clipboardHistoryStore = RecentClipboardStore(this)
        secretStore = SecretStore.create(this)
        themeRepository = ThemeRepository.create(this)
        themeRenderer = ThemeRenderer(this)
        themeBackgroundManager = ThemeBackgroundManager(this)
        applicationScope.launch {
            val legacyBubble = settingsRepository.current()
            themeRepository.seedBubbleAppearanceIfMissing(
                enabled = legacyBubble.bubbleKeyEnabled,
                intensity = legacyBubble.bubbleKeyIntensity
            )
        }
        aiSettingsRepository = AiSettingsRepository(settingsRepository, secretStore)
        managedSessionStore = ManagedSessionStore(secretStore)
        installationIdentity = InstallationIdentity(this)
        deviceProofManager = DeviceProofManager()

        applicationScope.launch {
            var clipboardPurgedWhileDisabled = false
            settingsRepository.settings.collectLatest { settings ->
                if (!settings.clipboardHistory && !clipboardPurgedWhileDisabled) {
                    clipboardHistoryStore.clear()
                    clipboardPurgedWhileDisabled = true
                } else if (settings.clipboardHistory) {
                    clipboardPurgedWhileDisabled = false
                }
                ContextAccessGate.update(
                    settings.contextAccessConsent && settings.accessibilityDisclosureAccepted
                )
            }
        }

        conversationRepository = ConversationRepository(
            dao = AppDatabase.get(this).conversationDao(),
            cipher = AesGcmCipher()
        )

        val httpClient = SecureHttpClientFactory.create()
        val personalGateway = OpenRouterGateway(
            client = httpClient,
            secretStore = secretStore
        )
        personalAiGateway = personalGateway

        backendClient = SocialAiBackendClient(
            client = httpClient,
            sessionStore = managedSessionStore,
            installationIdentity = installationIdentity,
            deviceProof = deviceProofManager
        )

        val router = GatewayRouter(
            settingsProvider = settingsRepository::current,
            personalGateway = personalGateway,
            personalApiConfigured = secretStore::isOpenRouterKeyConfigured,
            managedClient = backendClient,
            managedSessionStore = managedSessionStore
        )
        aiGateway = router

        val resultParser = ModelResultParser()

        replyOrchestrator = ReplyOrchestrator(
            scope = applicationScope,
            snapshots = ContextSnapshotBus.snapshots,
            sessions = ImeSessionRegistry.session,
            mergeHistory = conversationRepository::mergeSnapshot,
            settingsProvider = settingsRepository::current,
            apiKeyConfigured = secretStore::isOpenRouterKeyConfigured,
            managedSessionAvailable = managedSessionStore::isLoggedIn,
            promptBuilder = ExtensionPromptBuilder(),
            gateway = router,
            resultParser = resultParser
        )

        manualAiActionEngine = ManualAiActionEngine(
            scope = applicationScope,
            contextProvider = { ContextSnapshotBus.snapshots.value },
            historyProvider = { snapshot, limit ->
                conversationRepository.mergeSnapshot(snapshot).messages
                    .takeLast(limit)
                    .map { message ->
                        PromptMessage(
                            sender = runCatching { SenderClass.valueOf(message.sender) }
                                .getOrDefault(SenderClass.UNKNOWN),
                            text = message.text,
                            timestampHint = message.timestampHint
                        )
                    }
            },
            settingsProvider = {
                val settings = settingsRepository.current()
                ManualAiEngineSettings(
                    modelMode = settings.modelMode,
                    gatewayMode = settings.gatewayMode,
                    managedSessionAvailable = managedSessionStore.isLoggedIn(),
                    aiPrivacyConsent = settings.aiPrivacyConsent,
                    contextAccessConsent = settings.contextAccessConsent,
                    accessibilityDisclosureAccepted = settings.accessibilityDisclosureAccepted,
                    maxChatMessages = settings.maxChatMessages,
                    tonePreset = settings.tonePreset,
                    customInstruction = settings.customInstruction,
                    personalTraining = if (settings.gatewayMode == GatewayMode.MANAGED) {
                        managedSessionStore.load()?.let { session ->
                            secretStore.getAccountTraining(session.userId).orEmpty()
                        }.orEmpty()
                    } else {
                        settings.personalTraining
                    }
                )
            },
            apiKeyConfigured = secretStore::isOpenRouterKeyConfigured,
            promptBuilder = ManualAiPromptBuilder(ExtensionPromptBuilder()),
            gateway = router,
            parseReply = { raw -> resultParser.parse(raw)?.reply }
        )
    }

    fun currentManagedPersonalTraining(): String =
        managedSessionStore.load()?.let { session ->
            secretStore.getAccountTraining(session.userId).orEmpty()
        }.orEmpty()
}
