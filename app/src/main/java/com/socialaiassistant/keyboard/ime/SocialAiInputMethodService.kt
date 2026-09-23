package com.socialaiassistant.keyboard.ime

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.ClipboardManager
import android.content.Context
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.socialaiassistant.keyboard.BuildConfig
import com.socialaiassistant.keyboard.CaptionActivity
import com.socialaiassistant.keyboard.MainActivity
import com.socialaiassistant.keyboard.R
import com.socialaiassistant.keyboard.SocialAiApplication
import com.socialaiassistant.keyboard.ai.ManualAiAction
import com.socialaiassistant.keyboard.ai.ManualAiState
import com.socialaiassistant.keyboard.ai.ReplyState
import com.socialaiassistant.keyboard.ai.TonePreset
import com.socialaiassistant.keyboard.context.ContextSnapshotBus
import com.socialaiassistant.keyboard.context.ConversationSurface
import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.safety.SensitiveFieldPolicy
import com.socialaiassistant.keyboard.settings.AppSettings
import com.socialaiassistant.keyboard.settings.OneHandedMode
import com.socialaiassistant.keyboard.settings.ToolbarProfile
import com.socialaiassistant.keyboard.theme.KeyboardTheme
import com.socialaiassistant.keyboard.theme.ThemeResolutionPolicy
import com.socialaiassistant.keyboard.theme.ThemeSelectionState
import com.socialaiassistant.keyboard.theme.ThemeBackgroundManager
import com.socialaiassistant.keyboard.theme.ThemeBubbleAppearance
import com.socialaiassistant.keyboard.theme.ThemeButtonRole
import com.socialaiassistant.keyboard.theme.ThemePack
import com.socialaiassistant.keyboard.theme.ThemePreset
import com.socialaiassistant.keyboard.theme.ThemeRepository
import com.socialaiassistant.keyboard.theme.ThemeRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


data class ImeSession(
    val packageName: String?,
    val safety: FieldSafety,
    val inputType: Int,
    val hintText: String?,
    val fieldId: Int = 0,
    val generation: Long = 0L
)

private data class ImeDisplaySuggestion(
    val text: String,
    val sourceLanguage: KeyboardLanguage,
    val targetLanguage: KeyboardLanguage = sourceLanguage,
    val nextWord: Boolean = false
)

private data class SuggestionComputeKey(
    val language: KeyboardLanguage,
    val revision: Long,
    val limit: Int,
    val smartLanguageHints: Boolean,
    val allowSmartLanguageHints: Boolean
)

private data class GlideKeyTarget(val view: View, val key: Char)

private data class GlideGestureSession(
    val firstKey: Char,
    val downRawX: Float,
    val downRawY: Float,
    val eligible: Boolean,
    val hitMap: GlideHitMap,
    val sequence: StringBuilder = StringBuilder().append(firstKey),
    var lastKey: Char = firstKey,
    var dragging: Boolean = false,
    var lastHapticEventTimeMs: Long = Long.MIN_VALUE
)

object ImeSessionRegistry {
    private val _session = MutableStateFlow<ImeSession?>(null)
    val session: StateFlow<ImeSession?> = _session.asStateFlow()

    fun update(session: ImeSession?) {
        _session.value = session
    }

    /**
     * Accessibility may discover a stricter focused-field classification than EditorInfo exposed.
     * Restrictions are monotonic for the active editor generation: ALLOW -> NO_CONVERSATION -> BLOCK.
     * A later onStartInput call owns resetting safety for the next editor.
     */
    fun restrictSafety(packageName: String, candidate: FieldSafety): Boolean {
        if (candidate == FieldSafety.ALLOW_AI) return false
        val current = _session.value ?: return false
        if (current.packageName != packageName) return false

        val restricted = when {
            current.safety == FieldSafety.BLOCK_AI || candidate == FieldSafety.BLOCK_AI -> FieldSafety.BLOCK_AI
            current.safety == FieldSafety.NO_CONVERSATION || candidate == FieldSafety.NO_CONVERSATION -> FieldSafety.NO_CONVERSATION
            else -> FieldSafety.ALLOW_AI
        }
        if (restricted == current.safety) return false
        _session.value = current.copy(safety = restricted)
        return true
    }
}

class SocialAiInputMethodService : InputMethodService() {
    private val safetyPolicy = SensitiveFieldPolicy()
    private val replyInserter = ReplyInserter()
    private lateinit var typingEngine: ImeTypingEngine
    private lateinit var englishTypingEngine: EnglishTypingEngine
    private lateinit var banglaLearningStore: PersistentTypingLearningModel
    private lateinit var englishLearningStore: PersistentEnglishTypingLearningModel
    private val smartLanguageBridge = SmartLanguageBridge()
    private val glideTypingResource = ResettableLazyResource { GlideTypingEngine() }
    private val runtimeLatencyWindow = RuntimeLatencyWindow()
    private val stage21ColdStartProfiler = ColdStartProfiler(SystemClock.elapsedRealtimeNanos())
    private val keyboardRenderGate = KeyboardRenderGate()
    private val suggestionMemo = SingleEntryMemo<SuggestionComputeKey, List<ImeDisplaySuggestion>>()
    private val suggestionBarRenderGate = SuggestionBarRenderGate()
    private val toolbarOrderGate = ToolbarOrderGate()
    private val backgroundRenderGate = BackgroundRenderGate()
    private val keyboardLayoutCache = BoundedValueCache<KeyboardLayoutRequest, KeyboardLayout>(MAX_LAYOUT_CACHE_ENTRIES)
    private val configurationFrameCoalescer = FrameWorkCoalescer()
    private val configurationRenderRunnable = Runnable {
        configurationFrameCoalescer.consume()
        applyConfigurationUiNow()
    }
    private val rapidActionGate = RapidActionGate()
    private val feedbackCadenceGate = FeedbackCadenceGate()
    private val suggestionFrameCoalescer = FrameWorkCoalescer()
    private val suggestionRenderRunnable = Runnable {
        suggestionFrameCoalescer.consume()
        renderSuggestionBar()
    }
    private val glideKeyTargets = mutableListOf<GlideKeyTarget>()
    private var systemTouchSlopPx: Int = 0
    private var glideGestureSession: GlideGestureSession? = null
    private var backspaceRepeating: Boolean = false
    private var activeBackspaceButton: Button? = null
    private val backspaceRepeatAction = object : Runnable {
        override fun run() {
            if (!backspaceRepeating) return
            handleAction(KeyboardAction.Backspace)
            activeBackspaceButton?.postDelayed(this, BACKSPACE_REPEAT_INTERVAL_MS)
        }
    }
    private val toolbarButtons = linkedMapOf<Int, Button>()
    private lateinit var recentClipboardStore: RecentClipboardStore
    private var keyboardMode = KeyboardUiMode()
    private var activePanel = KeyboardPanel.KEYS
    private var rootView: View? = null
    private var bubbleKeyRenderer: BubbleKeyEffectRenderer? = null
    private var bubbleEditorGeneration = 0L
    private var latestBubbleCursorTarget: BubbleFlightTarget? = null
    private var nextBubbleFlightId = 1L
    private val pendingBubbleRetargets = java.util.ArrayDeque<PendingBubbleRetarget>()
    private val bubbleFlightTapCoordinator = BubbleFlightTapCoordinator(::dispatchPreparedBubbleFlight)
    private var smartReplyController: SmartReplyController? = null

    private data class PendingBubbleRetarget(
        val flightId: Long,
        val editor: BubbleFlightEditorToken,
        val expiresAtUptimeMs: Long
    )
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var latestReplyState: ReplyState = ReplyState.Hidden
    private var latestManualAiState: ManualAiState = ManualAiState.Idle
    private var pendingPanelDraftConflict: String? = null
    private var currentTonePreset: TonePreset = TonePreset.AUTO
    private var currentCustomInstruction: String = ""
    private var currentPreserveDraft: Boolean = true
    private var currentSettings: AppSettings = AppSettings()
    private var currentFieldPolicy: ImeTypingFieldPolicy = ImeEditorBehaviorPolicy.typingPolicy(0x00000001)
    private val consentBannerPolicy = ConsentBannerPolicy()
    private var currentThemeState = ThemeSelectionState(
        globalPack = null,
        perSurfaceOverrides = emptyMap(),
        legacyActiveThemeId = ThemePreset.socialAiNeon.id,
        customTheme = ThemePreset.customFrom()
    )
    private var currentGlobalChromeTheme: KeyboardTheme = ThemePreset.socialAiNeon
    private var currentSurfaceTheme: KeyboardTheme = ThemePreset.socialAiNeon
    private var currentKeyBoundarySettings: Map<ThemePack, Boolean> =
        ThemePack.entries.associateWith { false }
    private var currentBubbleAppearanceSettings: Map<ThemePack, ThemeBubbleAppearance> =
        ThemePack.entries.associateWith { ThemeBubbleAppearance() }
    private lateinit var themeRepository: ThemeRepository
    private lateinit var themeRenderer: ThemeRenderer
    private lateinit var themeBackgroundManager: ThemeBackgroundManager
    private var backgroundRenderGeneration: Long = 0L
    private val lowRamDevice: Boolean by lazy {
        (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice ?: false
    }

    override fun onCreate() {
        super.onCreate()
        systemTouchSlopPx = ViewConfiguration.get(this).scaledTouchSlop
        banglaLearningStore = PersistentTypingLearningModel(applicationContext)
        englishLearningStore = PersistentEnglishTypingLearningModel(applicationContext)
        typingEngine = ImeTypingEngine(learning = banglaLearningStore)
        englishTypingEngine = EnglishTypingEngine(learning = englishLearningStore)
        recordStage21ColdStart(ColdStartMilestone.LEARNING_MODELS_READY)
        recentClipboardStore = RecentClipboardStore(applicationContext)
        val app = application as? SocialAiApplication ?: return
        themeRepository = app.themeRepository
        themeRenderer = app.themeRenderer
        themeBackgroundManager = app.themeBackgroundManager
        serviceScope.launch {
            themeRepository.selectionState.collectLatest { state ->
                currentThemeState = state
                currentGlobalChromeTheme = ThemeResolutionPolicy.resolveGlobalChrome(state).normalized()
                refreshActiveSurfaceTheme()
                cancelBubbleEffectsIfDisabled()
                keyboardRenderGate.invalidate()
                suggestionBarRenderGate.invalidate()
                rootView?.let { root ->
                    applyTheme(root, rebuildDynamic = true)
                }
            }
        }
        serviceScope.launch {
            themeRepository.keyBoundarySettings.collectLatest { settings ->
                currentKeyBoundarySettings = settings
                keyboardRenderGate.invalidate()
                if (activePanel == KeyboardPanel.KEYS) rootView?.let { renderKeys() }
            }
        }
        serviceScope.launch {
            themeRepository.bubbleAppearanceSettings.collectLatest { settings ->
                currentBubbleAppearanceSettings = settings
                cancelBubbleEffectsIfDisabled()
            }
        }
        serviceScope.launch {
            app.replyOrchestrator.state.collectLatest { state ->
                latestReplyState = state
                renderSessionState()
            }
        }
        serviceScope.launch {
            app.manualAiActionEngine.state.collectLatest { state ->
                latestManualAiState = state
                if (activePanel == KeyboardPanel.AI) renderAiPanel()
                updateToolbarLabels()
            }
        }
        serviceScope.launch {
            app.settingsRepository.settings.collectLatest { settings ->
                currentSettings = settings
                cancelBubbleEffectsIfDisabled()
                currentTonePreset = settings.tonePreset
                currentCustomInstruction = settings.customInstruction
                currentPreserveDraft = settings.preserveDraft
                val fieldSafe = ImeSessionRegistry.session.value?.safety?.let { it != FieldSafety.BLOCK_AI } ?: false
                val allowSuggestions = fieldSafe && currentFieldPolicy.showSuggestions
                val allowLearning = fieldSafe && currentFieldPolicy.allowLearning && settings.personalTypingLearning
                typingEngine.setSuggestionsEnabled(allowSuggestions)
                typingEngine.setLearningEnabled(allowLearning)
                englishTypingEngine.setLearningEnabled(allowLearning)
                englishTypingEngine.setSuggestionsEnabled(allowSuggestions && settings.englishSuggestions)
                englishTypingEngine.setAutocorrectEnabled(
                    fieldSafe && currentFieldPolicy.allowAutocorrect && settings.englishAutocorrect
                )
                if (!settings.glideTyping) glideTypingResource.release()
                when (activePanel) {
                    KeyboardPanel.KEYS -> renderKeys()
                    KeyboardPanel.CLIPBOARD -> rootView?.findViewById<LinearLayout>(R.id.keyboard_panel_container)?.let { renderClipboardPanel(it) }
                    KeyboardPanel.AI -> renderAiPanel()
                    KeyboardPanel.EMOJI -> Unit
                }
            }
        }
        serviceScope.launch {
            VoiceResultBus.pending.collectLatest { pending ->
                if (pending != null) consumePendingVoiceResult()
            }
        }
        serviceScope.launch {
            CaptionDraftBus.pending.collectLatest { pending ->
                if (pending != null) consumePendingCaption()
            }
        }
        recordStage21ColdStart(ColdStartMilestone.SERVICE_READY)
    }

    override fun onCreateInputView(): View {
        val perfStarted = debugPerfStart()
        stopBackspaceRepeat()
        glideGestureSession = null
        cancelPendingSuggestionRender()
        rapidActionGate.clear()
        feedbackCadenceGate.clear()
        toolbarButtons.clear()
        keyboardRenderGate.invalidate()
        suggestionMemo.clear()
        suggestionBarRenderGate.invalidate()
        toolbarOrderGate.invalidate()
        backgroundRenderGate.invalidate()
        cancelPendingConfigurationRender()
        val root = LayoutInflater.from(this).inflate(R.layout.ime_keyboard, null, false)
        rootView = root
        bubbleKeyRenderer?.release()
        bubbleKeyRenderer = BubbleKeyEffectRenderer(root.findViewById(R.id.bubble_key_overlay))
        smartReplyController = SmartReplyController(
            root = root,
            onReplyTap = { reply -> insertWithDraftPreference(reply) },
            onInsertAnyway = { reply ->
                replyInserter.insertAnyway(currentInputConnection, reply)
                smartReplyController?.render(SmartReplyUiState.Hidden)
            },
            onReplace = { reply ->
                insertGeneratedReply(reply, replaceDraft = true)
                smartReplyController?.render(SmartReplyUiState.Hidden)
            }
        )
        setupToolbar(root)
        showKeys()
        renderSessionState()
        applyTheme(root, rebuildDynamic = false)
        recordDebugRuntime(ImeRuntimeMetric.INPUT_VIEW_CREATE, perfStarted, INPUT_VIEW_CREATE_BUDGET_MS)
        recordStage21ColdStart(ColdStartMilestone.FIRST_INPUT_VIEW_READY)
        return root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        val previousBubbleEditor = currentBubbleEditorToken()
        previousBubbleEditor?.let { BubbleFlightBus.cancelEditor(it) }
        clearBubbleFlightTargetState()
        super.onStartInput(attribute, restarting)
        stopBackspaceRepeat()
        cancelPendingSuggestionRender()
        rapidActionGate.clear()
        feedbackCadenceGate.clear()
        glideGestureSession = null
        safeConnectionOperation("start_input_finish_previous") { it.finishComposingText() }
        typingEngine.reset()
        englishTypingEngine.reset()
        pendingPanelDraftConflict = null
        (application as? SocialAiApplication)?.manualAiActionEngine?.clear()
        latestManualAiState = ManualAiState.Idle
        val editor = attribute
        if (editor == null) {
            currentFieldPolicy = ImeEditorBehaviorPolicy.typingPolicy(0)
            disableTypingIntelligence()
            ImeSessionRegistry.update(null)
            showKeys()
            updateToolbarLabels()
            renderSessionState()
            return
        }
        keyboardMode = keyboardMode.copy(
            layer = ImeEditorBehaviorPolicy.preferredLayer(editor.inputType),
            shifted = false
        )
        val descriptor = EditorInfoSafetyAdapter.descriptor(editor)
        val fieldSafety = safetyPolicy.evaluate(descriptor)
        currentFieldPolicy = ImeEditorBehaviorPolicy.typingPolicy(editor.inputType)
        val fieldSafe = fieldSafety != FieldSafety.BLOCK_AI
        val allowSuggestions = fieldSafe && currentFieldPolicy.showSuggestions
        val allowLearning = fieldSafe && currentFieldPolicy.allowLearning && currentSettings.personalTypingLearning
        typingEngine.setSuggestionsEnabled(allowSuggestions)
        typingEngine.setLearningEnabled(allowLearning)
        englishTypingEngine.setSuggestionsEnabled(allowSuggestions && currentSettings.englishSuggestions)
        englishTypingEngine.setLearningEnabled(allowLearning)
        englishTypingEngine.setAutocorrectEnabled(
            fieldSafe && currentFieldPolicy.allowAutocorrect && currentSettings.englishAutocorrect
        )
        bubbleEditorGeneration++
        ImeSessionRegistry.update(
            ImeSession(
                packageName = editor.packageName,
                safety = fieldSafety,
                inputType = editor.inputType,
                hintText = editor.hintText?.toString(),
                fieldId = editor.fieldId,
                generation = bubbleEditorGeneration
            )
        )
        showKeys()
        updateToolbarLabels()
        renderSessionState()
        consumePendingCaption()
        consumePendingVoiceResult()
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        requestBubbleCursorUpdates()
        stopBackspaceRepeat()
        glideGestureSession = null
        showKeys()
        updateToolbarLabels()
        renderSessionState()
        rootView?.let { applyTheme(it, rebuildDynamic = false) }
        consumePendingCaption()
        consumePendingVoiceResult()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        currentBubbleEditorToken()?.let { BubbleFlightBus.cancelEditor(it) }
        clearBubbleFlightTargetState()
        stopBackspaceRepeat()
        cancelPendingSuggestionRender()
        cancelPendingConfigurationRender()
        glideGestureSession = null
        bubbleKeyRenderer?.release()
        // Keep the populated key rows and Glide View references across ordinary IME hide/show.
        // A new input view or memory-pressure path explicitly invalidates them when required.
        finishAndDiscardCompositionOnSessionEnd()
        smartReplyController?.render(SmartReplyUiState.Hidden)
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (ImeEditorBehaviorPolicy.shouldAbortCompositionForSelection(
                composing = isActiveComposing(),
                newSelStart = newSelStart,
                newSelEnd = newSelEnd,
                candidatesStart = candidatesStart,
                candidatesEnd = candidatesEnd
            )
        ) {
            safeConnectionOperation("selection_finish_composing") { it.finishComposingText() }
            typingEngine.discardComposition()
            englishTypingEngine.discardComposition()
            dismissAllNextWordSuggestions()
            renderSuggestionBar()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        stopBackspaceRepeat()
        glideGestureSession = null
        requestConfigurationUiRefresh()
    }

    private fun requestConfigurationUiRefresh() {
        val root = rootView ?: return
        if (configurationFrameCoalescer.request()) {
            root.postOnAnimation(configurationRenderRunnable)
        } else if (BuildConfig.DEBUG) {
            Log.d(PERF_LOG_TAG, "runtime_render_coalesced target=configuration")
        }
    }

    private fun cancelPendingConfigurationRender() {
        rootView?.removeCallbacks(configurationRenderRunnable)
        configurationFrameCoalescer.cancel()
    }

    private fun applyConfigurationUiNow() {
        val root = rootView ?: return
        when (activePanel) {
            KeyboardPanel.KEYS -> renderKeys()
            KeyboardPanel.EMOJI -> showPanel(KeyboardPanel.EMOJI)
            KeyboardPanel.CLIPBOARD -> showPanel(KeyboardPanel.CLIPBOARD)
            KeyboardPanel.AI -> showPanel(KeyboardPanel.AI)
        }
        applyTheme(root, rebuildDynamic = false)
        if (BuildConfig.DEBUG) Log.d(PERF_LOG_TAG, "runtime_configuration_refresh applied=1")
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onFinishInput() {
        stopBackspaceRepeat()
        cancelPendingSuggestionRender()
        glideGestureSession = null
        finishAndDiscardCompositionOnSessionEnd()
        typingEngine.reset()
        englishTypingEngine.reset()
        pendingPanelDraftConflict = null
        (application as? SocialAiApplication)?.manualAiActionEngine?.clear()
        latestManualAiState = ManualAiState.Idle
        currentFieldPolicy = ImeEditorBehaviorPolicy.typingPolicy(0)
        disableTypingIntelligence()
        currentBubbleEditorToken()?.let { BubbleFlightBus.cancelEditor(it) }
        clearBubbleFlightTargetState()
        ImeSessionRegistry.update(null)
        smartReplyController?.render(SmartReplyUiState.Hidden)
        if (::banglaLearningStore.isInitialized) banglaLearningStore.requestImmediateFlush()
        if (::englishLearningStore.isInitialized) englishLearningStore.requestImmediateFlush()
        super.onFinishInput()
    }

    override fun onDestroy() {
        stopBackspaceRepeat()
        cancelPendingSuggestionRender()
        cancelPendingConfigurationRender()
        glideGestureSession = null
        backgroundRenderGeneration++
        typingEngine.discardComposition()
        englishTypingEngine.discardComposition()
        BubbleFlightBus.cancelAll()
        clearBubbleFlightTargetState()
        ImeSessionRegistry.update(null)
        serviceScope.cancel()
        toolbarButtons.clear()
        smartReplyController = null
        bubbleKeyRenderer?.release()
        bubbleKeyRenderer = null
        rootView = null
        if (::banglaLearningStore.isInitialized) banglaLearningStore.close()
        if (::englishLearningStore.isInitialized) englishLearningStore.close()
        glideTypingResource.release()
        runtimeLatencyWindow.clear()
        keyboardRenderGate.invalidate()
        suggestionMemo.clear()
        suggestionBarRenderGate.invalidate()
        toolbarOrderGate.invalidate()
        backgroundRenderGate.invalidate()
        keyboardLayoutCache.clear()
        if (::themeBackgroundManager.isInitialized) themeBackgroundManager.clearMemoryCache()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            stopBackspaceRepeat()
            cancelPendingSuggestionRender()
            glideGestureSession = null
            glideKeyTargets.clear()
            keyboardRenderGate.invalidate()
            releaseHiddenPanelViewsForMemoryPressure()
            releaseDisplayedThemeBackgroundForMemoryPressure("ui_hidden")
            releaseOptionalRuntimeResources("ui_hidden")
            if (::banglaLearningStore.isInitialized) banglaLearningStore.requestImmediateFlush()
            if (::englishLearningStore.isInitialized) englishLearningStore.requestImmediateFlush()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            releaseOptionalRuntimeResources("trim_$level")
        }
    }

    override fun onLowMemory() {
        releaseDisplayedThemeBackgroundForMemoryPressure("low_memory")
        releaseOptionalRuntimeResources("low_memory")
        if (::banglaLearningStore.isInitialized) banglaLearningStore.requestImmediateFlush()
        if (::englishLearningStore.isInitialized) englishLearningStore.requestImmediateFlush()
        super.onLowMemory()
    }

    fun insertGeneratedReply(reply: String, replaceDraft: Boolean = false): InsertResult {
        if (ImeSessionRegistry.session.value?.safety == FieldSafety.BLOCK_AI) {
            return InsertResult.NoConnection
        }
        return replyInserter.insert(currentInputConnection, reply, replaceDraft)
    }

    private fun setupToolbar(root: View) {
        val bindings = listOf(
            R.id.toolbar_ai to KeyboardAction.OpenAiPanel,
            R.id.toolbar_language to KeyboardAction.ToggleLanguage,
            R.id.toolbar_bangla_mode to KeyboardAction.ToggleBanglaMode,
            R.id.toolbar_emoji to KeyboardAction.OpenEmoji,
            R.id.toolbar_clipboard to KeyboardAction.OpenClipboard,
            R.id.toolbar_voice to KeyboardAction.OpenVoice,
            R.id.toolbar_settings to KeyboardAction.OpenSettings
        )
        bindings.forEach { (id, action) ->
            val button = root.findViewById<Button>(id)
            toolbarButtons[id] = button
            button.contentDescription = toolbarContentDescription(action)
            button.setOnClickListener { view ->
                if (rapidActionGate.allow("toolbar:$id", SystemClock.uptimeMillis(), TOOLBAR_ACTION_DEBOUNCE_MS)) {
                    performKeyFeedback(view)
                    handleAction(action)
                }
            }
        }
        applyToolbarProfile(root)
        updateToolbarLabels(root)
    }

    private fun updateToolbarLabels(root: View? = rootView) {
        val target = root ?: return
        applyToolbarProfile(target)
        val language = toolbarButtons[R.id.toolbar_language] ?: return
        val banglaMode = toolbarButtons[R.id.toolbar_bangla_mode] ?: return
        val voice = toolbarButtons[R.id.toolbar_voice]
        val ai = toolbarButtons[R.id.toolbar_ai] ?: return
        language.text = if (currentFieldPolicy.forceLiteralLatin) "EN" else if (keyboardMode.language == KeyboardLanguage.ENGLISH) "EN" else "বাংলা"
        language.isEnabled = !currentFieldPolicy.forceLiteralLatin
        language.contentDescription = if (currentFieldPolicy.forceLiteralLatin) {
            "Language switch unavailable in literal Latin field"
        } else {
            "Language switch"
        }
        banglaMode.visibility = if (!currentFieldPolicy.forceLiteralLatin && keyboardMode.language == KeyboardLanguage.BANGLA) View.VISIBLE else View.GONE
        banglaMode.text = if (keyboardMode.banglaMode == BanglaInputMode.PHONETIC) "Phonetic" else "Bijoy"
        banglaMode.contentDescription = "Bangla input mode: ${banglaMode.text}"
        val sensitive = ImeSessionRegistry.session.value?.safety == FieldSafety.BLOCK_AI
        val aiLoading = latestReplyState == ReplyState.Loading || latestManualAiState is ManualAiState.Loading
        voice?.isEnabled = !sensitive
        ai.isEnabled = !sensitive && !aiLoading
        if (::themeRenderer.isInitialized) {
            themeRenderer.styleToolbar(
                target,
                currentGlobalChromeTheme,
                aiSelected = activePanel == KeyboardPanel.AI,
                aiEnabled = !sensitive && !aiLoading
            )
        }
    }

    private fun toolbarContentDescription(action: KeyboardAction): String = when (action) {
        KeyboardAction.OpenAiPanel -> "AI tools"
        KeyboardAction.ToggleLanguage -> "Language switch"
        KeyboardAction.ToggleBanglaMode -> "Bangla input mode"
        KeyboardAction.OpenEmoji -> "Emoji"
        KeyboardAction.OpenClipboard -> "Clipboard"
        KeyboardAction.OpenVoice -> "Voice input"
        KeyboardAction.OpenSettings -> "Keyboard settings"
        else -> "Keyboard toolbar action"
    }

    private fun keyContentDescription(key: KeySpec): String = when (key.action) {
        is KeyboardAction.Text -> "Key ${key.label}"
        KeyboardAction.Backspace -> "Backspace"
        KeyboardAction.Shift -> "Shift"
        KeyboardAction.Enter -> "Enter"
        KeyboardAction.Space -> "Space"
        KeyboardAction.ToggleLanguage -> "Language switch"
        KeyboardAction.ToggleBanglaMode -> "Bangla input mode"
        KeyboardAction.ShowNumbers -> "Numbers"
        KeyboardAction.ShowSymbols -> "Symbols"
        KeyboardAction.ShowLetters -> "Letters"
        KeyboardAction.OpenEmoji -> "Emoji"
        KeyboardAction.OpenClipboard -> "Clipboard"
        KeyboardAction.OpenVoice -> "Voice input"
        KeyboardAction.OpenSettings -> "Keyboard settings"
        KeyboardAction.OpenAiPanel -> "AI tools"
        null -> "Key ${key.label}"
    }

    private fun renderSessionState() {
        val controller = smartReplyController ?: return
        when (ImeSessionRegistry.session.value?.safety) {
            FieldSafety.BLOCK_AI -> controller.render(SmartReplyUiState.Hidden)
            FieldSafety.NO_CONVERSATION -> controller.render(SmartReplyUiState.Hidden)
            FieldSafety.ALLOW_AI -> controller.render(latestReplyState)
            null -> controller.render(SmartReplyUiState.Hidden)
        }
        updateToolbarLabels()
        if (::themeRenderer.isInitialized) controller.applyTheme(currentGlobalChromeTheme, themeRenderer)
    }

    private fun renderKeys() {
        val root = rootView ?: return
        val perfStarted = debugPerfStart()
        val rowsContainer = root.findViewById<LinearLayout>(R.id.keyboard_rows)
        val quickKeys = if (keyboardMode.layer == KeyboardLayer.LETTERS) currentFieldPolicy.quickKeys else emptyList()
        val enterLabel = ImeEditorBehaviorPolicy.enterKeyLabel(currentInputEditorInfo?.imeOptions ?: 0)
        val signature = KeyboardRenderSignature(
            mode = keyboardMode,
            showNumberRow = currentSettings.showNumberRow,
            keyBoundaryEnabled = activeKeyBoundaryEnabled(),
            quickKeys = quickKeys.toList(),
            keyHeightDp = currentSettings.keyboardKeyHeightDp,
            oneHandedMode = currentSettings.oneHandedMode.settingValue,
            glideTyping = currentSettings.glideTyping,
            spacebarCursorControl = currentSettings.spacebarCursorControl,
            glideEligiblePolicy = currentFieldPolicy.showSuggestions && !currentFieldPolicy.forceLiteralLatin,
            keyGapBits = currentSurfaceTheme.keyGapDp.toBits(),
            enterLabel = enterLabel
        )
        if (!keyboardRenderGate.shouldRebuild(signature, rowsContainer.childCount)) {
            applyOneHandedMode(root)
            updateToolbarLabels(root)
            renderSuggestionBar()
            if (BuildConfig.DEBUG) Log.d(PERF_LOG_TAG, "runtime_render_coalesced target=keyboard")
            return
        }
        rowsContainer.removeAllViews()
        glideKeyTargets.clear()
        glideGestureSession = null

        if (keyboardMode.layer == KeyboardLayer.NUMBERS) {
            renderNumericPad(rowsContainer, enterLabel)
            applyOneHandedMode(root)
            updateToolbarLabels(root)
            cancelPendingSuggestionRender()
            renderSuggestionBar()
            recordDebugRuntime(ImeRuntimeMetric.KEYBOARD_RENDER, perfStarted, KEYBOARD_RENDER_BUDGET_MS)
            return
        }

        val layoutRequest = KeyboardLayoutRequest(
            mode = keyboardMode,
            showNumberRow = currentSettings.showNumberRow,
            quickKeys = quickKeys.toList()
        )
        val layout = keyboardLayoutCache.getOrPut(layoutRequest) {
            KeyboardLayout.forMode(
                keyboardMode,
                showNumberRow = currentSettings.showNumberRow,
                quickKeys = quickKeys
            )
        }

        for (row in layout.rows) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (key in row) {
                val button = Button(this).apply {
                    text = key.label
                    contentDescription = keyContentDescription(key)
                    isAllCaps = false
                    minWidth = 0
                    minimumWidth = 0
                    layoutParams = LinearLayout.LayoutParams(0, dp(currentSettings.keyboardKeyHeightDp), key.weight).apply {
                        val gap = dp((currentSurfaceTheme.keyGapDp / 2f).coerceAtLeast(0f))
                        setMargins(gap, gap, gap, gap)
                    }
                }
                if (key.action == KeyboardAction.Enter) {
                    button.text = enterLabel
                    button.contentDescription = if (enterLabel == "↵") "Enter" else enterLabel
                }
                when {
                    key.action == KeyboardAction.Backspace -> configureBackspaceButton(button)
                    key.action == KeyboardAction.Space -> configureSpacebarButton(button)
                    shouldUseGlideTouch(key) -> configureGlideKeyButton(button, key)
                    else -> button.setOnClickListener { view ->
                        performKeyFeedback(view)
                        val prepared = prepareBubbleFlight(view, key, glideGesture = false)
                        bubbleFlightTapCoordinator.commitThenDispatch(prepared) {
                            key.action?.let(::handleAction)
                        }
                    }
                }
                if (::themeRenderer.isInitialized) {
                    if (key.visualRole == KeyVisualRole.ALPHABETIC && !activeKeyBoundaryEnabled()) {
                        themeRenderer.styleBoundarylessAlphabeticKey(button, currentSurfaceTheme)
                    } else {
                        themeRenderer.styleButton(button, themeRoleFor(key.action), currentSurfaceTheme)
                    }
                }
                rowView.addView(button)
                val glideChar = glideCharacterFor(key)
                if (glideChar != null && currentSettings.glideTyping) {
                    glideKeyTargets += GlideKeyTarget(button, glideChar)
                }
            }
            rowsContainer.addView(rowView)
        }
        applyOneHandedMode(root)
        updateToolbarLabels(root)
        cancelPendingSuggestionRender()
        renderSuggestionBar()
        recordDebugRuntime(ImeRuntimeMetric.KEYBOARD_RENDER, perfStarted, KEYBOARD_RENDER_BUDGET_MS)
    }

    /**
     * Renders the calculator-style number pad from the supplied reference layout.
     *
     * Structure:
     *   left rail:  +  -  *  /
     *   center:     1 2 3 / 4 5 6 / 7 8 9
     *   right rail: % / space / backspace
     *   bottom:     ABC , !?# 0 = . enter
     *
     * The structure is shared by every theme package. Only the resolved NUMBER-surface
     * theme changes, so switching Classic Dark / Glass Modern / Clean Light / Gradient Pro
     * never changes key placement or behavior.
     */
    private fun renderNumericPad(rowsContainer: LinearLayout, enterLabel: String) {
        val pad = KeyboardLayout.numericPad(keyboardMode)
        val gap = dp((currentSurfaceTheme.keyGapDp / 2f).coerceAtLeast(0f))
        val keyHeight = dp(currentSettings.keyboardKeyHeightDp)
        // The center grid has exactly three digit rows. Do not reserve a fourth blank row:
        // the full-width action row follows 7/8/9 immediately, with 0 centered under 8.
        val mainHeight = (keyHeight + gap * 2) * NUMERIC_PAD_MAIN_DIGIT_ROWS

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                mainHeight
            )
        }

        val leftRail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f)
        }
        pad.leftRail.forEach { key ->
            leftRail.addView(
                createNumericPadButton(
                    key = key,
                    enterLabel = enterLabel,
                    params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    ).apply { setMargins(gap, gap, gap, gap) },
                    emphasis = NumericPadEmphasis.OPERATOR
                )
            )
        }
        main.addView(leftRail)
        main.addView(createNumericPadDivider(gap))

        val centerGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6f)
        }
        pad.digitRows.forEach { digitRow ->
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            digitRow.forEach { key ->
                rowView.addView(
                    createNumericPadButton(
                        key = key,
                        enterLabel = enterLabel,
                        params = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            key.weight
                        ).apply { setMargins(gap, gap, gap, gap) },
                        emphasis = NumericPadEmphasis.DIGIT
                    )
                )
            }
            centerGrid.addView(rowView)
        }
        main.addView(centerGrid)
        main.addView(createNumericPadDivider(gap))

        val rightRail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f)
        }
        pad.rightRail.forEach { key ->
            rightRail.addView(
                createNumericPadButton(
                    key = key,
                    enterLabel = enterLabel,
                    params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    ).apply { setMargins(gap, gap, gap, gap) },
                    emphasis = NumericPadEmphasis.ACTION
                )
            )
        }
        main.addView(rightRail)
        rowsContainer.addView(main)

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            weightSum = NUMERIC_PAD_TOTAL_WIDTH_UNITS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        pad.bottomRow.forEach { key ->
            val emphasis = when {
                key.label == "0" -> NumericPadEmphasis.DIGIT
                key.action == KeyboardAction.Enter -> NumericPadEmphasis.ACTION
                else -> NumericPadEmphasis.BOTTOM
            }
            bottomRow.addView(
                createNumericPadButton(
                    key = key,
                    enterLabel = enterLabel,
                    params = LinearLayout.LayoutParams(0, keyHeight, key.weight).apply {
                        setMargins(gap, gap, gap, gap)
                    },
                    emphasis = emphasis
                )
            )
        }
        rowsContainer.addView(bottomRow)
    }

    private enum class NumericPadEmphasis { DIGIT, OPERATOR, ACTION, BOTTOM }

    private fun createNumericPadButton(
        key: KeySpec,
        enterLabel: String,
        params: LinearLayout.LayoutParams,
        emphasis: NumericPadEmphasis
    ): Button {
        val button = Button(this).apply {
            text = key.label
            contentDescription = keyContentDescription(key)
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            layoutParams = params
        }
        if (key.action == KeyboardAction.Enter) {
            button.text = enterLabel
            button.contentDescription = if (enterLabel == "↵") "Enter" else enterLabel
        }
        when (key.action) {
            KeyboardAction.Backspace -> configureBackspaceButton(button)
            KeyboardAction.Space -> configureSpacebarButton(button)
            else -> button.setOnClickListener { view ->
                performKeyFeedback(view)
                val prepared = prepareBubbleFlight(view, key, glideGesture = false)
                bubbleFlightTapCoordinator.commitThenDispatch(prepared) {
                    key.action?.let(::handleAction)
                }
            }
        }
        if (::themeRenderer.isInitialized) {
            themeRenderer.styleButton(button, themeRoleFor(key.action), currentSurfaceTheme)
        }
        val scale = currentSurfaceTheme.keyLabelScale
        val sizeSp = when (emphasis) {
            NumericPadEmphasis.DIGIT -> 28f * scale
            NumericPadEmphasis.OPERATOR -> 20f * scale
            NumericPadEmphasis.ACTION -> 20f * scale
            NumericPadEmphasis.BOTTOM -> 17f * scale
        }
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        return button
    }

    private fun createNumericPadDivider(gap: Int): View = View(this).apply {
        setBackgroundColor(currentSurfaceTheme.textSecondary)
        alpha = 0.28f
        layoutParams = LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT).apply {
            setMargins(0, gap * 2, 0, gap * 2)
        }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun renderSuggestionBar() {
        val root = rootView ?: return
        val bar = root.findViewById<LinearLayout>(R.id.suggestion_bar)

        val safe = ImeSessionRegistry.session.value?.safety != FieldSafety.BLOCK_AI
        val letterLayer = keyboardMode.layer == KeyboardLayer.LETTERS
        val shouldShow = activePanel == KeyboardPanel.KEYS && safe && currentFieldPolicy.showSuggestions &&
            !currentFieldPolicy.forceLiteralLatin && letterLayer && when (keyboardMode.language) {
            KeyboardLanguage.BANGLA -> keyboardMode.banglaMode == BanglaInputMode.PHONETIC &&
                (typingEngine.isComposing() || typingEngine.isShowingNextWordSuggestions())
            KeyboardLanguage.ENGLISH -> currentSettings.englishSuggestions &&
                (englishTypingEngine.isComposing() || englishTypingEngine.isShowingNextWordSuggestions())
        }

        if (!shouldShow) {
            val hidden = SuggestionBarSignature(visible = false)
            if (suggestionBarRenderGate.shouldRebuild(hidden, bar.childCount)) {
                bar.removeAllViews()
            }
            bar.visibility = View.GONE
            return
        }

        val suggestionStarted = debugPerfStart()
        val candidates = displaySuggestions(limit = 3)
        recordDebugRuntime(ImeRuntimeMetric.SUGGESTION_COMPUTE, suggestionStarted, SUGGESTION_COMPUTE_BUDGET_MS)
        if (candidates.isEmpty()) {
            val hidden = SuggestionBarSignature(visible = false)
            if (suggestionBarRenderGate.shouldRebuild(hidden, bar.childCount)) {
                bar.removeAllViews()
            }
            bar.visibility = View.GONE
            return
        }

        recordStage21ColdStart(ColdStartMilestone.FIRST_SUGGESTION_READY)
        val signature = SuggestionBarSignature(
            visible = true,
            entries = candidates.map { candidate ->
                "${candidate.text}|${candidate.sourceLanguage.name}|${candidate.targetLanguage.name}|${candidate.nextWord}"
            }
        )
        if (!suggestionBarRenderGate.shouldRebuild(signature, bar.childCount)) {
            bar.visibility = View.VISIBLE
            if (BuildConfig.DEBUG) Log.d(PERF_LOG_TAG, "runtime_render_coalesced target=suggestions")
            return
        }

        bar.removeAllViews()
        bar.visibility = View.VISIBLE
        recordStage21PredictionPresented(candidates)
        candidates.forEachIndexed { index, candidate ->
            val button = Button(this).apply {
                text = candidate.text
                isAllCaps = false
                minWidth = 0
                minimumWidth = 0
                val switchHint = if (candidate.targetLanguage != candidate.sourceLanguage) {
                    " Switch to ${if (candidate.targetLanguage == KeyboardLanguage.BANGLA) "Bangla" else "English"}."
                } else ""
                contentDescription = "Suggestion ${index + 1}: ${candidate.text}.$switchHint"
                layoutParams = LinearLayout.LayoutParams(0, dp(TouchResponsivenessPolicy.MIN_ACTION_TOUCH_TARGET_DP), 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener { view ->
                    if (rapidActionGate.allow("suggestion", SystemClock.uptimeMillis(), SUGGESTION_ACTION_DEBOUNCE_MS)) {
                        performKeyFeedback(view)
                        recordStage21PredictionAccepted(index + 1, candidate)
                        acceptSuggestion(candidate)
                    }
                }
            }
            if (::themeRenderer.isInitialized) {
                themeRenderer.styleButton(button, ThemeButtonRole.SECONDARY_ACTION, currentGlobalChromeTheme)
            }
            bar.addView(button)
        }
    }

    private fun displaySuggestions(limit: Int): List<ImeDisplaySuggestion> {
        if (limit <= 0) return emptyList()
        val revision = when (keyboardMode.language) {
            KeyboardLanguage.BANGLA -> typingEngine.suggestionRevision()
            KeyboardLanguage.ENGLISH -> englishTypingEngine.suggestionRevision()
        }
        val key = SuggestionComputeKey(
            language = keyboardMode.language,
            revision = revision,
            limit = limit,
            smartLanguageHints = currentSettings.smartLanguageHints,
            allowSmartLanguageHints = currentFieldPolicy.allowSmartLanguageHints
        )
        return suggestionMemo.getOrCompute(key) {
            when (keyboardMode.language) {
            KeyboardLanguage.BANGLA -> {
                val nextWord = typingEngine.isShowingNextWordSuggestions()
                val base = typingEngine.currentSuggestions(limit).candidates.map { candidate ->
                    ImeDisplaySuggestion(candidate.text, KeyboardLanguage.BANGLA, nextWord = nextWord)
                }.toMutableList()
                if (currentSettings.smartLanguageHints && currentFieldPolicy.allowSmartLanguageHints && typingEngine.isComposing()) {
                    smartLanguageBridge.englishHintFromBanglaPhonetic(typingEngine.currentRomanBuffer())
                        ?.takeIf { hint -> base.none { it.text.equals(hint, ignoreCase = true) } }
                        ?.let { hint ->
                            val cross = ImeDisplaySuggestion(hint, KeyboardLanguage.BANGLA, KeyboardLanguage.ENGLISH)
                            if (base.size >= limit) base[limit - 1] = cross else base += cross
                        }
                }
                base.take(limit)
            }
            KeyboardLanguage.ENGLISH -> {
                val nextWord = englishTypingEngine.isShowingNextWordSuggestions()
                val base = englishTypingEngine.currentSuggestions(limit).candidates.map { candidate ->
                    ImeDisplaySuggestion(candidate.text, KeyboardLanguage.ENGLISH, nextWord = nextWord)
                }.toMutableList()
                if (currentSettings.smartLanguageHints && currentFieldPolicy.allowSmartLanguageHints && englishTypingEngine.isComposing()) {
                    smartLanguageBridge.banglaHintFromEnglish(englishTypingEngine.currentBuffer())
                        ?.takeIf { hint -> base.none { it.text == hint } }
                        ?.let { hint ->
                            val cross = ImeDisplaySuggestion(hint, KeyboardLanguage.ENGLISH, KeyboardLanguage.BANGLA)
                            if (base.size >= limit) base[limit - 1] = cross else base += cross
                        }
                }
                base.take(limit)
            }
            }
        }
    }

    private fun acceptSuggestion(candidate: ImeDisplaySuggestion) {
        val connection = currentInputConnection ?: return
        if (candidate.targetLanguage != candidate.sourceLanguage) {
            when (candidate.sourceLanguage) {
                KeyboardLanguage.ENGLISH -> englishTypingEngine.discardComposition()
                KeyboardLanguage.BANGLA -> typingEngine.discardComposition()
            }
            val composed = safeConnectionOperation("cross_language_composing") { it.setComposingText(candidate.text, 1) }
            val finished = composed && safeConnectionOperation("cross_language_finish") { it.finishComposingText() }
            if (!finished) {
                recoverFromInputConnectionFailure()
                return
            }
            dismissAllNextWordSuggestions()
            keyboardMode = keyboardMode.copy(
                language = candidate.targetLanguage,
                banglaMode = if (candidate.targetLanguage == KeyboardLanguage.BANGLA) BanglaInputMode.PHONETIC else keyboardMode.banglaMode,
                layer = KeyboardLayer.LETTERS,
                shifted = false
            )
            refreshActiveSurfaceTheme()
            showKeys()
            return
        }

        when (candidate.sourceLanguage) {
            KeyboardLanguage.BANGLA -> {
                val nextWord = typingEngine.isShowingNextWordSuggestions()
                val accepted = typingEngine.acceptSuggestion(candidate.text)
                val inserted = if (nextWord) {
                    safeConnectionOperation("bangla_suggestion_commit") { it.commitText("$accepted ", 1) }
                } else {
                    val composed = safeConnectionOperation("bangla_suggestion_composing") { it.setComposingText(accepted, 1) }
                    composed && safeConnectionOperation("bangla_suggestion_finish") { it.finishComposingText() }
                }
                if (!inserted) recoverFromInputConnectionFailure()
            }
            KeyboardLanguage.ENGLISH -> {
                val nextWord = englishTypingEngine.isShowingNextWordSuggestions()
                val accepted = englishTypingEngine.acceptSuggestion(candidate.text)
                val inserted = if (nextWord) {
                    safeConnectionOperation("english_suggestion_commit") { it.commitText("$accepted ", 1) }
                } else {
                    val composed = safeConnectionOperation("english_suggestion_composing") { it.setComposingText(accepted, 1) }
                    composed && safeConnectionOperation("english_suggestion_finish") { it.finishComposingText() }
                }
                if (!inserted) recoverFromInputConnectionFailure()
            }
        }
        requestSuggestionBarRender()
    }

    private fun shouldUseGlideTouch(key: KeySpec): Boolean =
        currentSettings.glideTyping &&
            currentFieldPolicy.showSuggestions &&
            !currentFieldPolicy.forceLiteralLatin &&
            keyboardMode.layer == KeyboardLayer.LETTERS &&
            (keyboardMode.language == KeyboardLanguage.ENGLISH ||
                (keyboardMode.language == KeyboardLanguage.BANGLA && keyboardMode.banglaMode == BanglaInputMode.PHONETIC)) &&
            glideCharacterFor(key) != null

    private fun glideCharacterFor(key: KeySpec): Char? {
        val action = key.action as? KeyboardAction.Text ?: return null
        if (action.value.length != 1) return null
        val char = action.value[0].lowercaseChar()
        return char.takeIf { it in 'a'..'z' }
    }

    private fun configureGlideKeyButton(button: Button, key: KeySpec) {
        val initial = glideCharacterFor(key) ?: return
        button.setOnClickListener { }
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    glideGestureSession = GlideGestureSession(
                        firstKey = initial,
                        downRawX = event.rawX,
                        downRawY = event.rawY,
                        eligible = !isActiveComposing(),
                        hitMap = snapshotGlideHitMap(),
                        lastHapticEventTimeMs = event.eventTime
                    )
                    performKeyFeedback(view)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val session = glideGestureSession ?: return@setOnTouchListener true
                    if (!session.eligible) return@setOnTouchListener true
                    val dx = event.rawX - session.downRawX
                    val dy = event.rawY - session.downRawY
                    val glideThresholdPx = TouchResponsivenessPolicy.glideStartThresholdPx(
                        baseThresholdPx = dp(GLIDE_START_THRESHOLD_DP),
                        systemTouchSlopPx = systemTouchSlopPx
                    )
                    if (!session.dragging && kotlin.math.hypot(dx.toDouble(), dy.toDouble()) >= glideThresholdPx.toDouble()) {
                        session.dragging = true
                        dismissActiveNextWordSuggestions()
                    }
                    if (session.dragging) {
                        val touched = session.hitMap.keyAt(
                            event.rawX,
                            event.rawY,
                            tolerancePx = dp(TouchResponsivenessPolicy.GLIDE_HIT_TOLERANCE_DP).toFloat()
                        )
                        if (touched != null && touched != session.lastKey) {
                            if (session.sequence.length < MAX_GLIDE_PATH_KEYS) session.sequence.append(touched)
                            session.lastKey = touched
                            if (currentSettings.hapticFeedback && event.eventTime - session.lastHapticEventTimeMs >= GLIDE_HAPTIC_MIN_INTERVAL_MS) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                session.lastHapticEventTimeMs = event.eventTime
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val session = glideGestureSession
                    glideGestureSession = null
                    if (session != null && session.eligible && session.dragging && session.sequence.length >= MIN_GLIDE_KEYS) {
                        val committed = commitGlideSequence(session.sequence.toString())
                        if (!committed) handleAction(KeyboardAction.Text(session.firstKey.toString()))
                    } else {
                        val prepared = prepareBubbleFlight(view, key, glideGesture = session?.dragging == true)
                        bubbleFlightTapCoordinator.commitThenDispatch(prepared) {
                            handleAction(KeyboardAction.Text(initial.toString()))
                        }
                    }
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    glideGestureSession = null
                    true
                }
                else -> true
            }
        }
    }

    private fun snapshotGlideHitMap(): GlideHitMap {
        if (glideKeyTargets.isEmpty()) return GlideHitMap.EMPTY
        val location = IntArray(2)
        val boxes = glideKeyTargets.mapNotNull { target ->
            val view = target.view
            if (view.width <= 0 || view.height <= 0 || !view.isShown) return@mapNotNull null
            view.getLocationOnScreen(location)
            GlideHitBox(
                key = target.key,
                left = location[0].toFloat(),
                top = location[1].toFloat(),
                right = location[0] + view.width.toFloat(),
                bottom = location[1] + view.height.toFloat()
            )
        }
        return GlideHitMap(boxes)
    }

    private fun commitGlideSequence(sequence: String): Boolean {
        val connection = currentInputConnection ?: return false
        val glideStarted = debugPerfStart()
        val glideTypingEngine = glideTypingResource.get()
        val candidate = when {
            keyboardMode.language == KeyboardLanguage.ENGLISH -> glideTypingEngine.bestEnglish(sequence)
            keyboardMode.language == KeyboardLanguage.BANGLA && keyboardMode.banglaMode == BanglaInputMode.PHONETIC -> glideTypingEngine.bestBangla(sequence)
            else -> null
        }
        recordDebugRuntime(ImeRuntimeMetric.GLIDE_RESOLVE, glideStarted, GLIDE_RESOLVE_BUDGET_MS)
        candidate ?: return false

        val text = when (keyboardMode.language) {
            KeyboardLanguage.ENGLISH -> englishTypingEngine.commitGlideWord(candidate.text)
            KeyboardLanguage.BANGLA -> typingEngine.commitGlideWord(candidate.sourceRoman, candidate.text)
        }
        val committed = safeConnectionOperation("glide_commit") { it.commitText("$text ", 1) }
        if (!committed) {
            recoverFromInputConnectionFailure()
            return false
        }
        requestSuggestionBarRender()
        return true
    }

    private fun applyOneHandedMode(root: View) {
        val wrapper = root.findViewById<FrameLayout>(R.id.keys_wrapper)
        val params = (wrapper.layoutParams as? LinearLayout.LayoutParams)
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        when (currentSettings.oneHandedMode) {
            OneHandedMode.OFF -> {
                params.width = LinearLayout.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.CENTER_HORIZONTAL
            }
            OneHandedMode.LEFT -> {
                params.width = (resources.displayMetrics.widthPixels * ONE_HANDED_WIDTH_RATIO).toInt()
                params.gravity = Gravity.START
            }
            OneHandedMode.RIGHT -> {
                params.width = (resources.displayMetrics.widthPixels * ONE_HANDED_WIDTH_RATIO).toInt()
                params.gravity = Gravity.END
            }
        }
        wrapper.layoutParams = params
    }

    private fun applyToolbarProfile(root: View) {
        if (toolbarButtons.isEmpty()) return
        val container = root.findViewById<LinearLayout>(R.id.keyboard_toolbar)
        val order = when (currentSettings.toolbarProfile) {
            ToolbarProfile.BALANCED -> listOf(R.id.toolbar_ai, R.id.toolbar_language, R.id.toolbar_bangla_mode, R.id.toolbar_emoji, R.id.toolbar_clipboard, R.id.toolbar_voice, R.id.toolbar_settings)
            ToolbarProfile.AI_FIRST -> listOf(R.id.toolbar_ai, R.id.toolbar_emoji, R.id.toolbar_clipboard, R.id.toolbar_voice, R.id.toolbar_language, R.id.toolbar_bangla_mode, R.id.toolbar_settings)
            ToolbarProfile.TYPING -> listOf(R.id.toolbar_language, R.id.toolbar_bangla_mode, R.id.toolbar_clipboard, R.id.toolbar_emoji, R.id.toolbar_ai, R.id.toolbar_voice, R.id.toolbar_settings)
            ToolbarProfile.MINIMAL -> listOf(R.id.toolbar_ai, R.id.toolbar_language, R.id.toolbar_bangla_mode, R.id.toolbar_settings)
        }
        val currentIds = (0 until container.childCount).map { index -> container.getChildAt(index).id }
        val signature = ToolbarOrderSignature(currentSettings.toolbarProfile.name, order)
        if (!toolbarOrderGate.shouldReorder(signature, currentIds)) {
            if (BuildConfig.DEBUG) Log.d(PERF_LOG_TAG, "runtime_render_coalesced target=toolbar")
            return
        }
        container.removeAllViews()
        order.forEach { id -> toolbarButtons[id]?.let(container::addView) }
    }

    private fun configureBackspaceButton(button: Button) {
        button.setOnClickListener { }
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopBackspaceRepeat()
                    activeBackspaceButton = button
                    backspaceRepeating = true
                    performKeyFeedback(view)
                    handleAction(KeyboardAction.Backspace)
                    button.postDelayed(backspaceRepeatAction, BACKSPACE_REPEAT_START_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopBackspaceRepeat()
                    if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                    true
                }
                else -> true
            }
        }
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeating = false
        activeBackspaceButton?.removeCallbacks(backspaceRepeatAction)
        activeBackspaceButton = null
    }

    private fun configureSpacebarButton(button: Button) {
        button.setOnClickListener { }
        if (!currentSettings.spacebarCursorControl) {
            button.setOnTouchListener(null)
            button.setOnClickListener { view ->
                performKeyFeedback(view)
                handleAction(KeyboardAction.Space)
            }
            return
        }
        var downX = 0f
        var lastStep = 0
        var dragging = false
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    lastStep = 0
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = event.x - downX
                    val step = (delta / dp(SPACEBAR_CURSOR_STEP_DP).toFloat()).toInt()
                    if (step != lastStep) {
                        if (!dragging) {
                            dragging = true
                            flushActiveComposition()
                            dismissActiveNextWordSuggestions()
                            performKeyFeedback(view)
                        }
                        val movement = step - lastStep
                        sendCursorMovement(movement)
                        lastStep = step
                        requestSuggestionBarRender()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        performKeyFeedback(view)
                        handleAction(KeyboardAction.Space)
                        view.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> true
            }
        }
    }

    private fun sendCursorMovement(steps: Int) {
        val keyCode = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(steps).coerceAtMost(MAX_CURSOR_STEPS_PER_EVENT)) {
            val down = safeConnectionOperation("cursor_down") { it.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode)) }
            val up = down && safeConnectionOperation("cursor_up") { it.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode)) }
            if (!up) {
                recoverFromInputConnectionFailure()
                return
            }
        }
    }

    private fun currentBubbleEditorToken(): BubbleFlightEditorToken? {
        val session = ImeSessionRegistry.session.value ?: return null
        return BubbleFlightEditorToken(session.packageName, session.fieldId, session.generation)
    }

    private fun clearBubbleFlightTargetState() {
        latestBubbleCursorTarget = null
        pendingBubbleRetargets.clear()
    }

    private fun requestBubbleCursorUpdates() {
        val connection = currentInputConnection ?: return
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                connection.requestCursorUpdates(
                    android.view.inputmethod.InputConnection.CURSOR_UPDATE_MONITOR or
                        android.view.inputmethod.InputConnection.CURSOR_UPDATE_IMMEDIATE,
                    android.view.inputmethod.InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER
                )
            } else {
                connection.requestCursorUpdates(
                    android.view.inputmethod.InputConnection.CURSOR_UPDATE_MONITOR or
                        android.view.inputmethod.InputConnection.CURSOR_UPDATE_IMMEDIATE
                )
            }
        }
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(info)
        val editor = currentBubbleEditorToken() ?: return
        if (info == null) return
        val point = BubbleCursorAnchorMapper.map(info) ?: return
        val now = SystemClock.uptimeMillis()
        val target = BubbleFlightTarget(
            point = point,
            editor = editor,
            capturedAtUptimeMs = now,
            source = BubbleFlightTargetSource.CURSOR_ANCHOR
        )
        latestBubbleCursorTarget = target
        while (pendingBubbleRetargets.isNotEmpty() && pendingBubbleRetargets.first().expiresAtUptimeMs < now) {
            pendingBubbleRetargets.removeFirst()
        }
        val pending = pendingBubbleRetargets.toList().asReversed().firstOrNull {
            it.editor == editor && it.expiresAtUptimeMs >= now
        } ?: return
        pendingBubbleRetargets.remove(pending)
        BubbleFlightBus.retarget(pending.flightId, target)
    }

    private fun prepareBubbleFlight(view: View, key: KeySpec, glideGesture: Boolean): PreparedBubbleFlight? {
        val bubbleAppearance = activeBubbleAppearance()
        val spec = BubbleKeyPolicy.resolve(
            BubbleKeyRequest(
                enabled = bubbleAppearance.enabled,
                label = key.label,
                sensitiveField = ImeSessionRegistry.session.value?.safety == FieldSafety.BLOCK_AI,
                glideGesture = glideGesture,
                animationsEnabled = ValueAnimator.areAnimatorsEnabled(),
                letterLayer = keyboardMode.layer == KeyboardLayer.LETTERS,
                intensity = bubbleAppearance.intensity
            )
        ) ?: return null
        val editor = currentBubbleEditorToken() ?: return null
        if (view.width <= 0 || view.height <= 0 || !view.isShown) return null
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val source = BubbleFlightPoint(
            location[0] + view.width / 2f,
            location[1] + view.height / 2f
        )
        val exact = latestBubbleCursorTarget?.takeIf { it.editor == editor }
        return PreparedBubbleFlight(
            BubbleFlightRequest(
                id = nextBubbleFlightId++,
                label = spec.label,
                source = source,
                editor = editor,
                exactTarget = exact,
                spec = spec,
                theme = currentSurfaceTheme,
                createdAtUptimeMs = SystemClock.uptimeMillis()
            )
        )
    }

    private fun dispatchPreparedBubbleFlight(prepared: PreparedBubbleFlight) {
        val preparedRequest = prepared.request
        val refreshedExact = BubbleFlightExactTargetRefresh.choose(
            editor = preparedRequest.editor,
            prepared = preparedRequest.exactTarget,
            latest = latestBubbleCursorTarget
        )
        val request = if (refreshedExact == preparedRequest.exactTarget) {
            preparedRequest
        } else {
            preparedRequest.copy(exactTarget = refreshedExact)
        }
        val handled = BubbleFlightBus.dispatch(request)
        if (handled) {
            pendingBubbleRetargets.addLast(
                PendingBubbleRetarget(request.id, request.editor, SystemClock.uptimeMillis() + BUBBLE_RETARGET_WINDOW_MS)
            )
            while (pendingBubbleRetargets.size > BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES) {
                pendingBubbleRetargets.removeFirst()
            }
        } else {
            bubbleKeyRenderer?.showLocal(request.source, request.spec, request.theme)
        }
    }

    private fun performKeyFeedback(view: View) {
        val nowMs = SystemClock.uptimeMillis()
        if (currentSettings.hapticFeedback && feedbackCadenceGate.allowHaptic(nowMs)) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (currentSettings.keySound && feedbackCadenceGate.allowSound(nowMs)) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    private fun requestSuggestionBarRender() {
        val root = rootView ?: return
        if (suggestionFrameCoalescer.request()) {
            root.postOnAnimation(suggestionRenderRunnable)
        }
    }

    private fun cancelPendingSuggestionRender() {
        rootView?.removeCallbacks(suggestionRenderRunnable)
        suggestionFrameCoalescer.cancel()
    }

    private fun handleAction(action: KeyboardAction) {
        when (action) {
            is KeyboardAction.Text -> {
                val mutation = when {
                    currentFieldPolicy.forceLiteralLatin && keyboardMode.layer == KeyboardLayer.LETTERS -> {
                        flushActiveComposition(autocorrect = false)
                        TypingMutation(directCommit = action.value)
                    }
                    isEnglishTypingMode() && !isLatinWordCharacter(action.value) -> {
                        flushEnglishComposition(autocorrect = currentFieldPolicy.allowAutocorrect)
                        TypingMutation(directCommit = action.value)
                    }
                    isBanglaPhoneticTypingMode() && !isLatinWordCharacter(action.value) -> {
                        flushPhoneticComposition(autocorrect = currentFieldPolicy.allowAutocorrect)
                        TypingMutation(directCommit = action.value)
                    }
                    isEnglishTypingMode() -> englishTypingEngine.onText(action.value)
                    else -> typingEngine.onText(keyboardMode, action.value)
                }
                applyTypingMutation(mutation)
                if (keyboardMode.shifted) {
                    keyboardMode = keyboardMode.copy(shifted = false)
                    renderKeys()
                } else {
                    requestSuggestionBarRender()
                }
            }
            KeyboardAction.Space -> {
                val hadComposition = isActiveComposing()
                flushActiveComposition(autocorrect = currentFieldPolicy.allowAutocorrect)
                val spaceCommitted = safeConnectionOperation("space_commit") { it.commitText(" ", 1) }
                if (!spaceCommitted) recoverFromInputConnectionFailure()
                if (!hadComposition) dismissActiveNextWordSuggestions()
                requestSuggestionBarRender()
            }
            KeyboardAction.Backspace -> {
                val connection = currentInputConnection ?: return
                val mutation = if (isEnglishTypingMode()) {
                    englishTypingEngine.onBackspace()
                } else {
                    typingEngine.onBackspace(keyboardMode)
                }
                val success = if (mutation.deletePrevious) {
                    safeConnectionOperation("backspace_delete") { it.deleteSurroundingText(1, 0) }
                } else {
                    val composed = safeConnectionOperation("backspace_composing") { it.setComposingText(mutation.composingText.orEmpty(), 1) }
                    if (composed && mutation.composingText.isNullOrEmpty()) {
                        safeConnectionOperation("backspace_finish") { it.finishComposingText() }
                    } else composed
                }
                if (!success) recoverFromInputConnectionFailure()
                requestSuggestionBarRender()
            }
            KeyboardAction.Shift -> {
                keyboardMode = keyboardMode.reduce(action)
                refreshActiveSurfaceTheme()
                renderKeys()
            }
            KeyboardAction.Enter -> {
                flushActiveComposition()
                handleEnter()
            }
            KeyboardAction.ToggleLanguage,
            KeyboardAction.ToggleBanglaMode,
            KeyboardAction.ShowNumbers,
            KeyboardAction.ShowSymbols,
            KeyboardAction.ShowLetters -> {
                flushActiveComposition()
                dismissAllNextWordSuggestions()
                keyboardMode = keyboardMode.reduce(action)
                refreshActiveSurfaceTheme()
                showKeys()
            }
            KeyboardAction.OpenEmoji -> showPanel(KeyboardPanel.EMOJI)
            KeyboardAction.OpenClipboard -> showPanel(KeyboardPanel.CLIPBOARD)
            KeyboardAction.OpenAiPanel -> {
                val safety = ImeSessionRegistry.session.value?.safety ?: FieldSafety.NO_CONVERSATION
                if (safety != FieldSafety.BLOCK_AI) {
                    showPanel(KeyboardPanel.AI)
                }
            }
            KeyboardAction.OpenVoice -> launchVoiceInput()
            KeyboardAction.OpenSettings -> openSettings()
        }
    }

    private fun showKeys() {
        activePanel = KeyboardPanel.KEYS
        refreshActiveSurfaceTheme()
        val root = rootView ?: return
        root.findViewById<LinearLayout>(R.id.keyboard_panel_container).apply {
            removeAllViews()
            visibility = View.GONE
        }
        root.findViewById<LinearLayout>(R.id.keyboard_rows).visibility = View.VISIBLE
        renderKeys()
    }

    private fun showPanel(panel: KeyboardPanel) {
        if (panel == KeyboardPanel.KEYS) {
            showKeys()
            return
        }
        flushActiveComposition()
        activePanel = panel
        val root = rootView ?: return
        root.findViewById<LinearLayout>(R.id.keyboard_rows).visibility = View.GONE
        val container = root.findViewById<LinearLayout>(R.id.keyboard_panel_container).apply {
            removeAllViews()
            visibility = View.VISIBLE
        }
        when (panel) {
            KeyboardPanel.EMOJI -> renderEmojiPanel(container)
            KeyboardPanel.CLIPBOARD -> renderClipboardPanel(container)
            KeyboardPanel.AI -> renderAiPanel(container)
            KeyboardPanel.KEYS -> Unit
        }
        updateToolbarLabels(root)
    }

    private fun renderPanelHeader(container: LinearLayout, title: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val back = Button(this).apply {
            text = "ABC"
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(46))
            setOnClickListener { showKeys() }
        }
        if (::themeRenderer.isInitialized) themeRenderer.styleButton(back, ThemeButtonRole.SPECIAL, currentGlobalChromeTheme)
        row.addView(back)
        val heading = TextView(this).apply {
            text = title
            textSize = 16f
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
        }
        if (::themeRenderer.isInitialized) themeRenderer.styleText(heading, currentGlobalChromeTheme)
        row.addView(heading)
        container.addView(row)
    }

    private fun renderEmojiPanel(container: LinearLayout) {
        renderPanelHeader(container, "Emoji")
        val categories = EmojiCatalog.categories()
        if (categories.isEmpty()) return

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            )
        }
        val categoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        categoryScroll.addView(categoryRow)
        container.addView(categoryScroll)

        val selectedLabel = TextView(this).apply {
            textSize = 13f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        if (::themeRenderer.isInitialized) themeRenderer.styleText(selectedLabel, currentGlobalChromeTheme, secondary = true)
        container.addView(selectedLabel)

        val emojiGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val emojiScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(216)
            )
            addView(emojiGrid)
        }
        container.addView(emojiScroll)

        lateinit var selectCategory: (EmojiCategory) -> Unit
        selectCategory = { category ->
            selectedLabel.text = "${category.label} · ${category.emojis.size}"
            categoryRow.removeAllViews()
            categories.forEach { item ->
                val categoryButton = Button(this).apply {
                    text = item.icon
                    contentDescription = item.label
                    isAllCaps = false
                    textSize = 18f
                    minWidth = 0
                    minimumWidth = 0
                    layoutParams = LinearLayout.LayoutParams(dp(46), dp(42)).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    setOnClickListener {
                        if (item.id != category.id) selectCategory(item)
                    }
                }
                if (::themeRenderer.isInitialized) {
                    val role = if (item.id == category.id) ThemeButtonRole.SPECIAL else ThemeButtonRole.NORMAL
                    themeRenderer.styleButton(categoryButton, role, currentGlobalChromeTheme)
                }
                categoryRow.addView(categoryButton)
            }
            renderEmojiCategory(emojiGrid, category)
            emojiScroll.scrollTo(0, 0)
        }

        selectCategory(categories.first())
    }

    private fun renderEmojiCategory(container: LinearLayout, category: EmojiCategory) {
        container.removeAllViews()
        category.emojis.chunked(8).forEach { chunk ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            chunk.forEach { emoji ->
                val emojiButton = Button(this).apply {
                    val supported = paint.hasGlyph(emoji)
                    text = if (supported) emoji else "□"
                    contentDescription = if (supported) emoji else "Unsupported emoji"
                    isEnabled = supported
                    isAllCaps = false
                    textSize = 20f
                    minWidth = 0
                    minimumWidth = 0
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    if (supported) {
                        setOnClickListener { currentInputConnection?.commitText(emoji, 1) }
                    }
                }
                if (::themeRenderer.isInitialized) themeRenderer.styleButton(emojiButton, ThemeButtonRole.NORMAL, currentGlobalChromeTheme)
                row.addView(emojiButton)
            }
            repeat(8 - chunk.size) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
                })
            }
            container.addView(row)
        }
    }

    private fun renderClipboardPanel(container: LinearLayout) {
        container.removeAllViews()
        renderPanelHeader(container, "Clipboard")

        val fieldSensitive = ImeSessionRegistry.session.value?.safety == FieldSafety.BLOCK_AI
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipMarkedSensitive = clipboard.primaryClipDescription?.extras
            ?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true
        val hideCurrentClipboard = !ClipboardTextPolicy.shouldExposeContent(fieldSensitive, clipMarkedSensitive)

        // Inspect the description first. Never request/coerce the primary payload when the field
        // or Android sensitivity marker says clipboard content should stay hidden.
        val clip = if (hideCurrentClipboard) null else clipboard.primaryClip
        val currentValues = ClipboardTextPolicy.sanitize(
            buildList {
                if (clip != null) {
                    for (index in 0 until clip.itemCount) {
                        add(clip.getItemAt(index).coerceToText(this@SocialAiInputMethodService).toString())
                    }
                }
            }
        )

        if (currentSettings.clipboardHistory && !hideCurrentClipboard && ::recentClipboardStore.isInitialized) {
            recentClipboardStore.recordAll(currentValues)
        }
        val recent = if (currentSettings.clipboardHistory && !hideCurrentClipboard && ::recentClipboardStore.isInitialized) {
            recentClipboardStore.recent()
        } else {
            emptyList()
        }
        val values = if (hideCurrentClipboard) {
            emptyList()
        } else {
            ClipboardTextPolicy.sanitize(currentValues + recent)
        }

        if (hideCurrentClipboard) {
            val message = when {
                fieldSensitive -> "Clipboard content is hidden in password, PIN, OTP, and other sensitive fields."
                else -> "Android marked the current clipboard as sensitive, so it is hidden here."
            }
            val note = TextView(this).apply {
                text = message
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
            if (::themeRenderer.isInitialized) themeRenderer.styleText(note, currentGlobalChromeTheme, secondary = true)
            container.addView(note)
        }

        if (values.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (hideCurrentClipboard) "No clipboard content shown" else "Clipboard is empty"
                setPadding(dp(12), dp(14), dp(12), dp(14))
            }
            if (::themeRenderer.isInitialized) themeRenderer.styleText(empty, currentGlobalChromeTheme, secondary = true)
            container.addView(empty)
        } else {
            values.forEach { value ->
                val item = Button(this).apply {
                    text = ClipboardTextPolicy.label(value)
                    isAllCaps = false
                    gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                    setOnClickListener {
                        currentInputConnection?.commitText(value, 1)
                        showKeys()
                    }
                }
                if (::themeRenderer.isInitialized) themeRenderer.styleButton(item, ThemeButtonRole.SECONDARY_ACTION, currentGlobalChromeTheme)
                container.addView(item)
            }
        }

        if (!hideCurrentClipboard && currentSettings.clipboardHistory && recent.isNotEmpty()) {
            val clear = Button(this).apply {
                text = "Clear recent clipboard history"
                isAllCaps = false
                setOnClickListener {
                    recentClipboardStore.clear()
                    renderClipboardPanel(container)
                }
            }
            if (::themeRenderer.isInitialized) themeRenderer.styleButton(clear, ThemeButtonRole.SPECIAL, currentGlobalChromeTheme)
            container.addView(clear)
        }
    }

    private fun renderAiPanel(container: LinearLayout? = null) {
        val target = container ?: rootView
            ?.findViewById<LinearLayout>(R.id.keyboard_panel_container)
            ?: return
        if (activePanel != KeyboardPanel.AI) return

        target.removeAllViews()
        renderPanelHeader(target, getString(R.string.ai_panel_title))

        val safety = ImeSessionRegistry.session.value?.safety ?: FieldSafety.NO_CONVERSATION
        if (consentBannerPolicy.resolve(currentSettings, safety) == ConsentBannerState.Required) {
            renderConsentBanner(target)
            return
        }

        val surface = ContextSnapshotBus.snapshots.value?.surface ?: ConversationSurface.GENERAL
        val labels = AiPanelPolicy.socialLabels(surface)

        renderToneRow(target)

        val socialActions = mutableListOf(
            labels.smart to ManualAiAction.SMART,
            labels.witty to ManualAiAction.WITTY,
            labels.flirty to ManualAiAction.FLIRTY
        )
        if (surface == ConversationSurface.COMMENT) socialActions += "Funny Comment" to ManualAiAction.FUNNY
        addAiActionRow(target, socialActions)
        addAiActionRow(
            target,
            listOf(
                getString(R.string.ai_action_rewrite) to ManualAiAction.REWRITE,
                getString(R.string.ai_action_translate) to ManualAiAction.TRANSLATE,
                getString(R.string.ai_action_grammar) to ManualAiAction.GRAMMAR_FIX
            )
        )
        val captionButton = Button(this).apply {
            text = getString(R.string.ai_action_caption)
            isAllCaps = false
            setOnClickListener { launchCaptionWriter() }
        }
        if (::themeRenderer.isInitialized) themeRenderer.styleButton(captionButton, ThemeButtonRole.AI_ACTION, currentGlobalChromeTheme)
        target.addView(captionButton)

        val state = latestManualAiState
        val status = TextView(this).apply {
            textSize = 14f
            setPadding(dp(12), dp(12), dp(12), dp(10))
        }
        if (::themeRenderer.isInitialized) themeRenderer.styleText(status, currentGlobalChromeTheme)

        when (state) {
            ManualAiState.Idle -> Unit
            is ManualAiState.Loading -> {
                status.text = getString(R.string.ai_panel_thinking)
                target.addView(status)
            }
            is ManualAiState.Ready -> {
                status.text = state.reply
                target.addView(status)
                renderManualResultActions(target, state.reply)
            }
            ManualAiState.NeedsApiKey -> {
                status.text = getString(R.string.ai_panel_needs_api_key)
                target.addView(status)
            }
            ManualAiState.NeedsContext -> {
                status.text = getString(R.string.ai_panel_needs_context)
                target.addView(status)
            }
            ManualAiState.NeedsDraft -> {
                status.text = getString(R.string.ai_panel_needs_draft)
                target.addView(status)
            }
            ManualAiState.Blocked -> {
                status.text = getString(R.string.ai_panel_blocked)
                target.addView(status)
            }
            ManualAiState.Offline -> {
                status.text = getString(R.string.ai_panel_offline)
                target.addView(status)
            }
            is ManualAiState.Error -> {
                status.text = state.message
                target.addView(status)
            }
        }
    }

    private fun renderConsentBanner(container: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(4), dp(6), dp(4), dp(6))
            }
            if (::themeRenderer.isInitialized) {
                background = themeRenderer.panelBackground(currentGlobalChromeTheme, currentGlobalChromeTheme.aiNeon)
            }
        }
        val title = TextView(this).apply {
            text = getString(R.string.ai_consent_banner_title)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val body = TextView(this).apply {
            text = getString(R.string.ai_consent_banner_body)
            textSize = 13f
            setPadding(0, dp(6), 0, dp(10))
        }
        val enable = Button(this).apply {
            text = getString(R.string.ai_consent_banner_action)
            isAllCaps = false
            setOnClickListener { openSettings(MainActivity.SECTION_AI_PRIVACY) }
        }
        if (::themeRenderer.isInitialized) {
            themeRenderer.styleText(title, currentGlobalChromeTheme)
            themeRenderer.styleText(body, currentGlobalChromeTheme, secondary = true)
            themeRenderer.styleButton(enable, ThemeButtonRole.AI, currentGlobalChromeTheme, selected = true)
        }
        card.addView(title)
        card.addView(body)
        card.addView(enable)
        container.addView(card)
    }

    private fun renderToneRow(container: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(actionButton(AiPanelPolicy.toneButtonLabel(currentTonePreset)) {
            cycleTonePreset()
        })
        row.addView(actionButton(
            if (currentCustomInstruction.isBlank()) getString(R.string.ai_custom_prompt_off)
            else getString(R.string.ai_custom_prompt_on)
        ) {
            openSettings()
        })
        container.addView(row)
    }

    private fun cycleTonePreset() {
        val app = application as? SocialAiApplication ?: return
        val next = AiPanelPolicy.nextTone(currentTonePreset)
        currentTonePreset = next
        renderAiPanel()
        serviceScope.launch {
            app.settingsRepository.setTonePreset(next)
        }
    }

    private fun addAiActionRow(
        container: LinearLayout,
        actions: List<Pair<String, ManualAiAction>>
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        actions.forEach { (label, action) ->
            val actionButton = Button(this).apply {
                text = label
                isAllCaps = false
                minWidth = 0
                minimumWidth = 0
                isEnabled = latestReplyState != ReplyState.Loading && latestManualAiState !is ManualAiState.Loading
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                setOnClickListener { requestManualAiAction(action) }
            }
            if (::themeRenderer.isInitialized) themeRenderer.styleButton(actionButton, ThemeButtonRole.AI_ACTION, currentGlobalChromeTheme)
            row.addView(actionButton)
        }
        container.addView(row)
    }

    private fun requestManualAiAction(action: ManualAiAction) {
        val safety = ImeSessionRegistry.session.value?.safety ?: return
        if (safety == FieldSafety.BLOCK_AI) return
        if (latestReplyState == ReplyState.Loading || latestManualAiState is ManualAiState.Loading) return

        pendingPanelDraftConflict = null
        val draft = if (action.isDraftAction) readCurrentDraft() else ""
        val translateTarget = AiPanelPolicy.translateTarget(keyboardMode.language)
        (application as? SocialAiApplication)?.manualAiActionEngine?.request(
            action = action,
            draftText = draft,
            translateTargetLanguage = translateTarget,
            safety = safety
        )
    }

    private fun renderManualResultActions(container: LinearLayout, reply: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (pendingPanelDraftConflict == reply) {
            row.addView(actionButton(getString(R.string.ai_result_append)) {
                replyInserter.insertAnyway(currentInputConnection, reply)
                clearManualAiAndShowKeys()
            })
            row.addView(actionButton(getString(R.string.ai_result_replace)) {
                insertGeneratedReply(reply, replaceDraft = true)
                clearManualAiAndShowKeys()
            })
        } else {
            row.addView(actionButton(getString(R.string.ai_result_insert)) {
                when (insertWithDraftPreference(reply)) {
                    InsertResult.Inserted -> clearManualAiAndShowKeys()
                    InsertResult.DraftPresent -> {
                        pendingPanelDraftConflict = reply
                        renderAiPanel()
                    }
                    InsertResult.EmptyReply, InsertResult.NoConnection, InsertResult.CommitFailed -> Unit
                }
            })
        }

        row.addView(actionButton(getString(R.string.ai_result_regenerate)) {
            val safety = ImeSessionRegistry.session.value?.safety ?: return@actionButton
            pendingPanelDraftConflict = null
            (application as? SocialAiApplication)?.manualAiActionEngine?.regenerate(safety)
        })
        container.addView(row)
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        layoutParams = LinearLayout.LayoutParams(0, dp(TouchResponsivenessPolicy.MIN_ACTION_TOUCH_TARGET_DP), 1f).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        setOnClickListener { view ->
            if (rapidActionGate.allow("panel:$label", SystemClock.uptimeMillis(), PANEL_ACTION_DEBOUNCE_MS)) {
                performKeyFeedback(view)
                action()
            }
        }
        if (::themeRenderer.isInitialized) themeRenderer.styleButton(this, ThemeButtonRole.SECONDARY_ACTION, currentGlobalChromeTheme)
    }

    private fun activeThemePack(): ThemePack? {
        val surface = ImeThemeSurfaceResolver.forMode(keyboardMode)
        return currentThemeState.perSurfaceOverrides[surface] ?: currentThemeState.globalPack
    }

    private fun activeKeyBoundaryEnabled(): Boolean =
        activeThemePack()?.let { currentKeyBoundarySettings[it] ?: false } ?: false

    private fun activeBubbleAppearance(): ThemeBubbleAppearance =
        activeThemePack()?.let { currentBubbleAppearanceSettings[it] }
            ?: ThemeBubbleAppearance(
                enabled = currentSettings.bubbleKeyEnabled,
                intensity = currentSettings.bubbleKeyIntensity
            )

    private fun cancelBubbleEffectsIfDisabled() {
        if (!activeBubbleAppearance().enabled) {
            bubbleKeyRenderer?.release()
            currentBubbleEditorToken()?.let { BubbleFlightBus.cancelEditor(it) }
            pendingBubbleRetargets.clear()
        }
    }

    private fun refreshActiveSurfaceTheme() {
        currentSurfaceTheme = ThemeResolutionPolicy.resolveSurface(
            currentThemeState,
            ImeThemeSurfaceResolver.forMode(keyboardMode)
        ).normalized()
        rootView?.let { root ->
            if (::themeRenderer.isInitialized) {
                themeRenderer.applyKeyPanelSurface(root, currentSurfaceTheme)
            }
            root.findViewById<View>(R.id.keyboard_content)?.setPadding(
                dp(currentSurfaceTheme.outerPaddingDp),
                dp(currentSurfaceTheme.outerPaddingDp),
                dp(currentSurfaceTheme.outerPaddingDp),
                dp(currentSurfaceTheme.outerPaddingDp)
            )
        }
    }

    private fun applyTheme(root: View, rebuildDynamic: Boolean) {
        if (!::themeRenderer.isInitialized) return
        themeRenderer.applyGlobalChrome(root, currentGlobalChromeTheme)
        themeRenderer.applyKeyPanelSurface(root, currentSurfaceTheme)
        root.findViewById<View>(R.id.keyboard_content)?.setPadding(
            dp(currentSurfaceTheme.outerPaddingDp),
            dp(currentSurfaceTheme.outerPaddingDp),
            dp(currentSurfaceTheme.outerPaddingDp),
            dp(currentSurfaceTheme.outerPaddingDp)
        )
        smartReplyController?.applyTheme(currentGlobalChromeTheme, themeRenderer)
        if (rebuildDynamic) {
            when (activePanel) {
                KeyboardPanel.KEYS -> renderKeys()
                KeyboardPanel.EMOJI -> root.findViewById<LinearLayout>(R.id.keyboard_panel_container).let { container ->
                    container.removeAllViews(); renderEmojiPanel(container)
                }
                KeyboardPanel.CLIPBOARD -> root.findViewById<LinearLayout>(R.id.keyboard_panel_container).let { container ->
                    container.removeAllViews(); renderClipboardPanel(container)
                }
                KeyboardPanel.AI -> renderAiPanel()
            }
        } else {
            updateToolbarLabels(root)
        }
        themeRenderer.styleTree(root, currentGlobalChromeTheme)
        refreshThemeBackground(root)
    }

    private fun refreshThemeBackground(root: View) {
        if (!::themeRenderer.isInitialized || !::themeBackgroundManager.isInitialized) return
        val config = currentGlobalChromeTheme.background.normalized()
        val requestedWidth = root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val requestedHeight = root.height.takeIf { it > 0 } ?: dp(420)
        val target = BackgroundDecodePolicy.target(requestedWidth, requestedHeight, lowRamDevice)
        val request = BackgroundRenderSignature(
            content = BackgroundContentSignature(
                enabled = config.enabled,
                localFileName = config.localFileName,
                scope = config.scope.name,
                fit = config.fit.name,
                opacityPercent = config.opacityPercent,
                darkOverlayPercent = config.darkOverlayPercent,
                blurAmount = config.blurAmount
            ),
            targetWidthPx = target.widthPx,
            targetHeightPx = target.heightPx
        )
        when (backgroundRenderGate.decide(request)) {
            BackgroundRenderDecision.SKIP -> {
                if (BuildConfig.DEBUG) Log.d(PERF_LOG_TAG, "runtime_render_coalesced target=background")
                return
            }
            BackgroundRenderDecision.CLEAR -> {
                backgroundRenderGeneration++
                themeRenderer.applyBackground(root, config, null)
                return
            }
            BackgroundRenderDecision.LOAD_CLEAR -> themeRenderer.applyBackground(root, config, null)
            BackgroundRenderDecision.LOAD_KEEP_VISIBLE -> Unit
        }
        val generation = ++backgroundRenderGeneration
        serviceScope.launch {
            val bitmap = themeBackgroundManager.loadDisplayBitmap(config, target.widthPx, target.heightPx)
            if (generation != backgroundRenderGeneration) return@launch
            if (config != currentGlobalChromeTheme.background.normalized()) return@launch
            if (rootView !== root) return@launch
            themeRenderer.applyBackground(root, config, bitmap)
        }
    }

    private fun themeRoleFor(action: KeyboardAction?): ThemeButtonRole = when (action) {
        KeyboardAction.Enter -> ThemeButtonRole.ENTER
        KeyboardAction.Backspace,
        KeyboardAction.Shift,
        KeyboardAction.ToggleLanguage,
        KeyboardAction.ToggleBanglaMode,
        KeyboardAction.ShowNumbers,
        KeyboardAction.ShowSymbols,
        KeyboardAction.ShowLetters -> ThemeButtonRole.SPECIAL
        else -> ThemeButtonRole.NORMAL
    }

    private fun readCurrentDraft(): String {
        val connection = currentInputConnection ?: return ""
        val selected = connection.getSelectedText(0)?.toString().orEmpty().trim()
        if (selected.isNotEmpty()) return selected
        val before = connection.getTextBeforeCursor(MAX_AI_DRAFT_SCAN, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(MAX_AI_DRAFT_SCAN, 0)?.toString().orEmpty()
        return (before + after).trim()
    }

    private fun clearManualAiAndShowKeys() {
        pendingPanelDraftConflict = null
        (application as? SocialAiApplication)?.manualAiActionEngine?.clear()
        latestManualAiState = ManualAiState.Idle
        showKeys()
    }

    private fun insertWithDraftPreference(reply: String): InsertResult {
        if (ImeSessionRegistry.session.value?.safety == FieldSafety.BLOCK_AI) return InsertResult.NoConnection
        return if (currentPreserveDraft) {
            replyInserter.appendAfterDraft(currentInputConnection, reply)
        } else {
            replyInserter.insert(currentInputConnection, reply, replaceDraft = true)
        }
    }

    private fun launchCaptionWriter() {
        val session = ImeSessionRegistry.session.value ?: return
        if (session.safety == FieldSafety.BLOCK_AI) return
        flushActiveComposition()
        val target = deferredTargetFrom(session) ?: return
        startActivity(
            Intent(this, CaptionActivity::class.java)
                .putDeferredTarget(target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun consumePendingCaption() {
        val pending = CaptionDraftBus.pending.value ?: return
        val session = ImeSessionRegistry.session.value
        when (deferredInsertionDecision(pending.target, session)) {
            DeferredInsertionDecision.WAIT -> return
            DeferredInsertionDecision.REJECT -> {
                CaptionDraftBus.consume()
                return
            }
            DeferredInsertionDecision.ACCEPT -> Unit
        }
        val connection = currentInputConnection ?: return
        val primaryResult = if (currentPreserveDraft) {
            replyInserter.appendAfterDraft(connection, pending.value)
        } else {
            replyInserter.insert(connection, pending.value, replaceDraft = true)
        }
        val finalResult = if (primaryResult == InsertResult.DraftPresent) {
            replyInserter.insertAnyway(connection, pending.value)
        } else {
            primaryResult
        }
        when (finalResult) {
            InsertResult.Inserted, InsertResult.EmptyReply -> CaptionDraftBus.consume()
            InsertResult.DraftPresent, InsertResult.NoConnection, InsertResult.CommitFailed -> Unit
        }
    }

    private fun launchVoiceInput() {
        val session = ImeSessionRegistry.session.value ?: return
        if (session.safety == FieldSafety.BLOCK_AI) return
        flushActiveComposition()
        val target = deferredTargetFrom(session) ?: return
        val language = if (keyboardMode.language == KeyboardLanguage.BANGLA) "bn-BD" else "en-US"
        startActivity(
            Intent(this, VoiceInputActivity::class.java)
                .putExtra(VoiceInputActivity.EXTRA_LANGUAGE, language)
                .putDeferredTarget(target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun consumePendingVoiceResult() {
        val pending = VoiceResultBus.pending.value ?: return
        if (pending is VoiceResult.Cancelled) {
            VoiceResultBus.consume()
            return
        }
        val text = pending as VoiceResult.Text
        val session = ImeSessionRegistry.session.value
        when (deferredInsertionDecision(text.target, session)) {
            DeferredInsertionDecision.WAIT -> return
            DeferredInsertionDecision.REJECT -> {
                VoiceResultBus.consume()
                return
            }
            DeferredInsertionDecision.ACCEPT -> {
                val committed = safeConnectionOperation("voice_result_commit") { it.commitText(text.value, 1) }
                if (committed) VoiceResultBus.consume()
            }
        }
    }

    private fun deferredTargetFrom(session: ImeSession): DeferredInsertionTarget? {
        val packageName = session.packageName ?: return null
        return DeferredInsertionTarget(
            packageName = packageName,
            inputType = session.inputType,
            fieldId = session.fieldId,
            hintText = session.hintText
        )
    }

    private fun deferredInsertionDecision(
        target: DeferredInsertionTarget,
        session: ImeSession?
    ): DeferredInsertionDecision = DeferredInsertionPolicy.decision(
        target = target,
        currentPackageName = session?.packageName,
        currentInputType = session?.inputType,
        currentFieldId = session?.fieldId,
        currentHintText = session?.hintText,
        blocked = session?.safety == FieldSafety.BLOCK_AI
    )

    private fun Intent.putDeferredTarget(target: DeferredInsertionTarget): Intent = apply {
        putExtra(DeferredInsertionExtras.PACKAGE, target.packageName)
        putExtra(DeferredInsertionExtras.INPUT_TYPE, target.inputType)
        putExtra(DeferredInsertionExtras.FIELD_ID, target.fieldId)
        putExtra(DeferredInsertionExtras.HINT, target.hintText)
        putExtra(DeferredInsertionExtras.CREATED_AT_NANOS, target.createdAtNanos)
    }

    private fun openSettings(section: String? = null) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!section.isNullOrBlank()) intent.putExtra(MainActivity.EXTRA_OPEN_SECTION, section)
        startActivity(intent)
    }

    private fun applyTypingMutation(mutation: TypingMutation) {
        var success = true
        mutation.directCommit?.let { value ->
            success = safeConnectionOperation("direct_commit") { it.commitText(value, 1) } && success
        }
        mutation.composingText?.let { value ->
            success = safeConnectionOperation("set_composing") { it.setComposingText(value, 1) } && success
        }
        if (mutation.deletePrevious) {
            success = safeConnectionOperation("delete_previous") { it.deleteSurroundingText(1, 0) } && success
        }
        if (!success) recoverFromInputConnectionFailure()
    }

    private fun finishAndDiscardCompositionOnSessionEnd() {
        // Commit only the editor's already-visible composing span; never recompute/autocorrect
        // while an InputConnection is being torn down or replaced.
        safeConnectionOperation("finish_session_composing") { it.finishComposingText() }
        typingEngine.discardComposition()
        englishTypingEngine.discardComposition()
        dismissAllNextWordSuggestions()
    }

    private fun disableTypingIntelligence() {
        typingEngine.setSuggestionsEnabled(false)
        typingEngine.setLearningEnabled(false)
        englishTypingEngine.setSuggestionsEnabled(false)
        englishTypingEngine.setLearningEnabled(false)
        englishTypingEngine.setAutocorrectEnabled(false)
    }

    private fun flushActiveComposition(autocorrect: Boolean = false) {
        when (keyboardMode.language) {
            KeyboardLanguage.ENGLISH -> flushEnglishComposition(autocorrect)
            KeyboardLanguage.BANGLA -> flushPhoneticComposition(autocorrect)
        }
    }

    private fun flushEnglishComposition(autocorrect: Boolean = false) {
        if (!englishTypingEngine.isComposing()) return
        if (currentInputConnection == null) return
        val word = englishTypingEngine.flush(autocorrect = autocorrect)
        val composed = safeConnectionOperation("english_flush_composing") { it.setComposingText(word, 1) }
        val finished = composed && safeConnectionOperation("english_flush_finish") { it.finishComposingText() }
        if (!finished) recoverFromInputConnectionFailure()
        requestSuggestionBarRender()
    }

    private fun flushPhoneticComposition(autocorrect: Boolean = false) {
        if (!typingEngine.isComposing()) return
        if (currentInputConnection == null) return
        val word = if (autocorrect) typingEngine.flushWithAutocorrect() else typingEngine.flush()
        val composed = safeConnectionOperation("bangla_flush_composing") { it.setComposingText(word, 1) }
        val finished = composed && safeConnectionOperation("bangla_flush_finish") { it.finishComposingText() }
        if (!finished) recoverFromInputConnectionFailure()
        requestSuggestionBarRender()
    }

    private inline fun safeConnectionOperation(name: String, operation: (android.view.inputmethod.InputConnection) -> Boolean): Boolean {
        val connection = currentInputConnection ?: return false
        val started = if (BuildConfig.DEBUG) SystemClock.elapsedRealtimeNanos() else 0L
        val success = try {
            operation(connection)
        } catch (error: RuntimeException) {
            if (BuildConfig.DEBUG) Log.w(PERF_LOG_TAG, "InputConnection failure op=$name", error)
            false
        }
        if (BuildConfig.DEBUG) {
            val elapsedNanos = SystemClock.elapsedRealtimeNanos() - started
            val elapsedMs = elapsedNanos / 1_000_000.0
            recordDebugRuntimeElapsed(
                metric = ImeRuntimeMetric.INPUT_CONNECTION,
                elapsedNanos = elapsedNanos,
                budgetMs = SLOW_CONNECTION_OPERATION_MS
            )
            if (elapsedMs >= SLOW_CONNECTION_OPERATION_MS) {
                Log.w(PERF_LOG_TAG, "slow_input_connection op=$name duration_ms=${"%.2f".format(elapsedMs)}")
            }
        }
        return success
    }

    private fun recoverFromInputConnectionFailure() {
        stopBackspaceRepeat()
        glideGestureSession = null
        typingEngine.discardComposition()
        englishTypingEngine.discardComposition()
        dismissAllNextWordSuggestions()
        renderSuggestionBar()
    }

    private fun debugPerfStart(): Long = if (BuildConfig.DEBUG) SystemClock.elapsedRealtimeNanos() else 0L

    private fun recordStage21ColdStart(milestone: ColdStartMilestone) {
        if (!BuildConfig.DEBUG) return
        stage21ColdStartProfiler.mark(milestone, SystemClock.elapsedRealtimeNanos())?.let { sample ->
            Log.d(
                STAGE21_EVIDENCE_TAG,
                "cold_start milestone=${sample.milestone.name.lowercase()} elapsed_ms=${"%.2f".format(sample.elapsedMicros / 1000.0)}"
            )
        }
    }

    private fun recordStage21PredictionPresented(candidates: List<ImeDisplaySuggestion>) {
        if (!BuildConfig.DEBUG || candidates.isEmpty()) return
        val nextWordCount = candidates.count { it.nextWord }
        val crossLanguageCount = candidates.count { it.targetLanguage != it.sourceLanguage }
        Log.d(
            STAGE21_EVIDENCE_TAG,
            "prediction_present language=${keyboardMode.language.name.lowercase()} candidates=${candidates.size} " +
                "next_word=$nextWordCount cross_language=$crossLanguageCount"
        )
    }

    private fun recordStage21PredictionAccepted(rank: Int, candidate: ImeDisplaySuggestion) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            STAGE21_EVIDENCE_TAG,
            "prediction_accept rank=${rank.coerceAtLeast(1)} language=${candidate.sourceLanguage.name.lowercase()} " +
                "next_word=${candidate.nextWord} cross_language=${candidate.targetLanguage != candidate.sourceLanguage}"
        )
    }

    private fun recordDebugRuntime(metric: ImeRuntimeMetric, startedNanos: Long, budgetMs: Double) {
        if (!BuildConfig.DEBUG || startedNanos <= 0L) return
        recordDebugRuntimeElapsed(metric, SystemClock.elapsedRealtimeNanos() - startedNanos, budgetMs)
    }

    private fun recordDebugRuntimeElapsed(metric: ImeRuntimeMetric, elapsedNanos: Long, budgetMs: Double) {
        if (!BuildConfig.DEBUG) return
        runtimeLatencyWindow.record(
            metric = metric,
            elapsedNanos = elapsedNanos,
            budgetNanos = (budgetMs * 1_000_000.0).toLong()
        )?.let { snapshot ->
            Log.d(
                PERF_LOG_TAG,
                "runtime_metric metric=${snapshot.metric.name.lowercase()} samples=${snapshot.sampleCount} " +
                    "p50_ms=${"%.2f".format(snapshot.p50Micros / 1000.0)} " +
                    "p95_ms=${"%.2f".format(snapshot.p95Micros / 1000.0)} " +
                    "max_ms=${"%.2f".format(snapshot.maxMicros / 1000.0)} " +
                    "over_budget=${snapshot.overBudgetCount}"
            )
        }
    }

    private fun releaseOptionalRuntimeResources(reason: String) {
        suggestionMemo.clear()
        keyboardLayoutCache.clear()
        if (::themeBackgroundManager.isInitialized) themeBackgroundManager.clearMemoryCache()
        val released = glideTypingResource.release()
        if (BuildConfig.DEBUG && released) {
            Log.d(PERF_LOG_TAG, "runtime_resource_released resource=glide reason=$reason")
        }
        if (BuildConfig.DEBUG) {
            Log.d(PERF_LOG_TAG, "runtime_resource_released resource=layout_background_cache reason=$reason")
        }
    }

    private fun releaseDisplayedThemeBackgroundForMemoryPressure(reason: String) {
        backgroundRenderGeneration++
        backgroundRenderGate.invalidate()
        if (::themeBackgroundManager.isInitialized) themeBackgroundManager.clearMemoryCache()
        val root = rootView ?: return
        if (::themeRenderer.isInitialized) {
            themeRenderer.applyBackground(root, currentGlobalChromeTheme.background.normalized(), null)
        }
        if (BuildConfig.DEBUG) {
            Log.d(PERF_LOG_TAG, "runtime_resource_released resource=theme_background reason=$reason")
        }
    }

    private fun releaseHiddenPanelViewsForMemoryPressure() {
        if (activePanel == KeyboardPanel.KEYS) return
        val root = rootView ?: return
        root.findViewById<LinearLayout>(R.id.keyboard_panel_container)?.apply {
            removeAllViews()
            visibility = View.GONE
        }
        activePanel = KeyboardPanel.KEYS
        if (BuildConfig.DEBUG) {
            Log.d(PERF_LOG_TAG, "runtime_resource_released resource=dynamic_panel reason=ui_hidden")
        }
    }

    private fun isEnglishTypingMode(): Boolean =
        !currentFieldPolicy.forceLiteralLatin &&
            keyboardMode.language == KeyboardLanguage.ENGLISH &&
            keyboardMode.layer == KeyboardLayer.LETTERS &&
            ImeSessionRegistry.session.value?.safety != FieldSafety.BLOCK_AI &&
            (currentSettings.englishSuggestions || currentSettings.englishAutocorrect)

    private fun isBanglaPhoneticTypingMode(): Boolean =
        !currentFieldPolicy.forceLiteralLatin &&
            keyboardMode.language == KeyboardLanguage.BANGLA &&
            keyboardMode.banglaMode == BanglaInputMode.PHONETIC &&
            keyboardMode.layer == KeyboardLayer.LETTERS

    private fun isLatinWordCharacter(value: String): Boolean =
        value.length == 1 && (value[0] in 'a'..'z' || value[0] in 'A'..'Z' || value[0] == '\'')

    private fun isActiveComposing(): Boolean = when (keyboardMode.language) {
        KeyboardLanguage.ENGLISH -> englishTypingEngine.isComposing()
        KeyboardLanguage.BANGLA -> typingEngine.isComposing()
    }

    private fun dismissActiveNextWordSuggestions() {
        when (keyboardMode.language) {
            KeyboardLanguage.ENGLISH -> englishTypingEngine.dismissNextWordSuggestions()
            KeyboardLanguage.BANGLA -> typingEngine.dismissNextWordSuggestions()
        }
    }

    private fun dismissAllNextWordSuggestions() {
        typingEngine.dismissNextWordSuggestions()
        englishTypingEngine.dismissNextWordSuggestions()
    }

    private fun handleEnter() {
        val handled = runCatching { sendDefaultEditorAction(true) }.getOrElse { error ->
            if (BuildConfig.DEBUG) Log.w(PERF_LOG_TAG, "editor action failed", error)
            false
        }
        if (handled) return
        runCatching { sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
            .onFailure { error -> if (BuildConfig.DEBUG) Log.w(PERF_LOG_TAG, "enter key failed", error) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        private const val BUBBLE_RETARGET_WINDOW_MS = 180L
        const val NUMERIC_PAD_MAIN_DIGIT_ROWS = 3
        const val NUMERIC_PAD_TOTAL_WIDTH_UNITS = 9f
        const val MAX_AI_DRAFT_SCAN = 8_000
        const val BACKSPACE_REPEAT_START_DELAY_MS = 320L
        const val BACKSPACE_REPEAT_INTERVAL_MS = 55L
        const val TOOLBAR_ACTION_DEBOUNCE_MS = 180L
        const val SUGGESTION_ACTION_DEBOUNCE_MS = 140L
        const val PANEL_ACTION_DEBOUNCE_MS = 180L
        const val SPACEBAR_CURSOR_STEP_DP = 18
        const val MAX_CURSOR_STEPS_PER_EVENT = 12
        const val GLIDE_START_THRESHOLD_DP = 18
        const val MIN_GLIDE_KEYS = 3
        const val MAX_GLIDE_PATH_KEYS = 64
        const val GLIDE_HAPTIC_MIN_INTERVAL_MS = 24L
        const val SLOW_CONNECTION_OPERATION_MS = 16.0
        const val INPUT_VIEW_CREATE_BUDGET_MS = 32.0
        const val KEYBOARD_RENDER_BUDGET_MS = 24.0
        const val SUGGESTION_COMPUTE_BUDGET_MS = 8.0
        const val GLIDE_RESOLVE_BUDGET_MS = 12.0
        const val PERF_LOG_TAG = "SocialAiImePerf"
        const val STAGE21_EVIDENCE_TAG = "SocialAiImeStage21"
        const val ONE_HANDED_WIDTH_RATIO = 0.84f
        const val MAX_LAYOUT_CACHE_ENTRIES = 10

    }
}
