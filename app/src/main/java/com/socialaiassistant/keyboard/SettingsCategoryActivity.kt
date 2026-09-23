package com.socialaiassistant.keyboard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialaiassistant.keyboard.ai.AiGatewayException
import com.socialaiassistant.keyboard.ai.AiGenerationRequest
import com.socialaiassistant.keyboard.ai.GatewayMode
import com.socialaiassistant.keyboard.ai.ModelMode
import com.socialaiassistant.keyboard.ai.PromptBundle
import com.socialaiassistant.keyboard.backend.BackendErrorMessages
import com.socialaiassistant.keyboard.backend.BackendException
import com.socialaiassistant.keyboard.context.ContextAccessGate
import com.socialaiassistant.keyboard.context.SocialAiAccessibilityService
import com.socialaiassistant.keyboard.crypto.SecretStore
import com.socialaiassistant.keyboard.ime.PersistentEnglishTypingLearningModel
import com.socialaiassistant.keyboard.ime.PersistentTypingLearningModel
import com.socialaiassistant.keyboard.ime.RecentClipboardStore
import com.socialaiassistant.keyboard.ime.SocialAiInputMethodService
import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity
import com.socialaiassistant.keyboard.settings.OneHandedMode
import com.socialaiassistant.keyboard.settings.SettingsRepository
import com.socialaiassistant.keyboard.settings.ToolbarProfile
import com.socialaiassistant.keyboard.settingsui.SettingsCategoryId
import com.socialaiassistant.keyboard.settingsui.SettingsThemeRepository
import com.socialaiassistant.keyboard.settingsui.SettingsThemeStyler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SettingsCategoryActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_CATEGORY = "settings_category"

        fun createIntent(context: Context, category: SettingsCategoryId): Intent =
            Intent(context, SettingsCategoryActivity::class.java)
                .putExtra(EXTRA_CATEGORY, category.wireValue)
    }

    private data class CategoryMeta(
        val layoutRes: Int,
        val titleRes: Int,
        val subtitleRes: Int
    )

    private var category: SettingsCategoryId? = null
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsThemeRepository: SettingsThemeRepository
    private lateinit var settingsThemeStyler: SettingsThemeStyler
    private lateinit var secretStore: SecretStore
    private val app: SocialAiApplication get() = application as SocialAiApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resolved = SettingsCategoryId.fromWireValue(intent.getStringExtra(EXTRA_CATEGORY))
        if (resolved == null) {
            openHubForInvalidCategory()
            return
        }
        category = resolved
        settingsRepository = SettingsRepository.create(this)
        settingsThemeRepository = SettingsThemeRepository.create(this)
        settingsThemeStyler = SettingsThemeStyler(this)
        secretStore = SecretStore.create(this)
        setContentView(R.layout.activity_settings_category)

        findViewById<View>(R.id.settings_category_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        inflateCategoryContent(resolved)
        bindCategory(resolved)
    }

    override fun onResume() {
        super.onResume()
        if (category != null) {
            lifecycleScope.launch {
                applySettingsTheme()
                refreshCategoryState()
            }
        }
    }

    private fun metadata(category: SettingsCategoryId): CategoryMeta = when (category) {
        SettingsCategoryId.KEYBOARD_SETUP -> CategoryMeta(
            R.layout.settings_category_keyboard_setup,
            R.string.settings_category_keyboard_setup_title,
            R.string.settings_category_keyboard_setup_subtitle
        )
        SettingsCategoryId.LANGUAGE_INPUT -> CategoryMeta(
            R.layout.settings_category_language_input,
            R.string.settings_category_language_input_title,
            R.string.settings_category_language_input_subtitle
        )
        SettingsCategoryId.THEME_APPEARANCE -> CategoryMeta(
            R.layout.settings_category_theme_appearance,
            R.string.settings_category_theme_appearance_title,
            R.string.settings_category_theme_appearance_subtitle
        )
        SettingsCategoryId.TYPING_SUGGESTIONS -> CategoryMeta(
            R.layout.settings_category_typing_suggestions,
            R.string.settings_category_typing_suggestions_title,
            R.string.settings_category_typing_suggestions_subtitle
        )
        SettingsCategoryId.AI_PRIVACY -> CategoryMeta(
            R.layout.settings_category_ai_privacy,
            R.string.settings_category_ai_privacy_title,
            R.string.settings_category_ai_privacy_subtitle
        )
        SettingsCategoryId.CLIPBOARD -> CategoryMeta(
            R.layout.settings_category_clipboard,
            R.string.settings_category_clipboard_title,
            R.string.settings_category_clipboard_subtitle
        )
        SettingsCategoryId.ACCOUNT_SUBSCRIPTION -> CategoryMeta(
            R.layout.settings_category_account_subscription,
            R.string.settings_category_account_subscription_title,
            R.string.settings_category_account_subscription_subtitle
        )
        SettingsCategoryId.HELP_ABOUT -> CategoryMeta(
            R.layout.settings_category_help_about,
            R.string.settings_category_help_about_title,
            R.string.settings_category_help_about_subtitle
        )
    }

    private fun inflateCategoryContent(category: SettingsCategoryId) {
        val meta = metadata(category)
        findViewById<TextView>(R.id.settings_category_title).setText(meta.titleRes)
        findViewById<TextView>(R.id.settings_category_subtitle).setText(meta.subtitleRes)
        val host = findViewById<FrameLayout>(R.id.settings_category_content)
        host.removeAllViews()
        LayoutInflater.from(this).inflate(meta.layoutRes, host, true)
    }

    private fun bindCategory(category: SettingsCategoryId) {
        when (category) {
            SettingsCategoryId.KEYBOARD_SETUP -> bindKeyboardSetup()
            SettingsCategoryId.LANGUAGE_INPUT -> bindLanguageInput()
            SettingsCategoryId.THEME_APPEARANCE -> bindThemeAppearance()
            SettingsCategoryId.TYPING_SUGGESTIONS -> bindTypingSuggestions()
            SettingsCategoryId.AI_PRIVACY -> bindAiPrivacy()
            SettingsCategoryId.CLIPBOARD -> bindClipboard()
            SettingsCategoryId.ACCOUNT_SUBSCRIPTION -> bindAccountSubscription()
            SettingsCategoryId.HELP_ABOUT -> bindHelpAbout()
        }
    }

    private suspend fun refreshCategoryState() {
        when (category) {
            SettingsCategoryId.KEYBOARD_SETUP -> refreshKeyboardSetup()
            SettingsCategoryId.LANGUAGE_INPUT -> refreshLanguageInput()
            SettingsCategoryId.THEME_APPEARANCE -> refreshThemeAppearance()
            SettingsCategoryId.TYPING_SUGGESTIONS -> refreshTypingSuggestions()
            SettingsCategoryId.AI_PRIVACY -> refreshAiPrivacy()
            SettingsCategoryId.CLIPBOARD -> refreshClipboard()
            SettingsCategoryId.ACCOUNT_SUBSCRIPTION -> refreshAccountSubscription()
            SettingsCategoryId.HELP_ABOUT -> refreshHelpAbout()
            null -> Unit
        }
    }

    private suspend fun applySettingsTheme() {
        val pack = settingsThemeRepository.currentPack()
        settingsThemeStyler.applyCategory(
            root = findViewById(R.id.settings_category_root),
            header = findViewById<ViewGroup>(R.id.settings_category_header),
            title = findViewById(R.id.settings_category_title),
            subtitle = findViewById(R.id.settings_category_subtitle),
            content = findViewById<ViewGroup>(R.id.settings_category_content),
            pack = pack
        )
    }

    private fun openHubForInvalidCategory() {
        if (!isTaskRoot) {
            finish()
            return
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun bindKeyboardSetup() {
        findViewById<Button>(R.id.button_keyboard_settings).setOnClickListener {
            runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                .onFailure {
                    Toast.makeText(this, "Keyboard settings are unavailable on this device.", Toast.LENGTH_LONG).show()
                }
        }
        findViewById<Button>(R.id.button_choose_keyboard).setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }

    private fun bindLanguageInput() {
        findViewById<CheckBox>(R.id.english_suggestions_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setEnglishSuggestions((view as CheckBox).isChecked)
                refreshLanguageInput()
            }
        }
        findViewById<CheckBox>(R.id.english_autocorrect_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setEnglishAutocorrect((view as CheckBox).isChecked)
                refreshLanguageInput()
            }
        }
        findViewById<CheckBox>(R.id.smart_language_hints_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setSmartLanguageHints((view as CheckBox).isChecked)
                refreshLanguageInput()
            }
        }
    }
    private fun bindThemeAppearance() {
        findViewById<Button>(R.id.button_theme_settings).setOnClickListener {
            startActivity(Intent(this, ThemeSettingsActivity::class.java))
        }
        findViewById<Button>(R.id.button_settings_theme).setOnClickListener {
            startActivity(
                Intent(this, SettingsThemeOnboardingActivity::class.java)
                    .putExtra(SettingsThemeOnboardingActivity.EXTRA_MANUAL_CHANGE, true)
            )
        }
        findViewById<CheckBox>(R.id.number_row_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setShowNumberRow((view as CheckBox).isChecked)
                refreshThemeAppearance()
            }
        }
        findViewById<CheckBox>(R.id.key_boundary_checkbox).setOnClickListener { view ->
            val enabled = (view as CheckBox).isChecked
            lifecycleScope.launch {
                val pack = app.themeRepository.currentSelection().globalPack
                if (pack != null) {
                    app.themeRepository.setKeyBoundaryEnabled(pack, enabled)
                    refreshThemeAppearance()
                } else {
                    Toast.makeText(this@SettingsCategoryActivity, "Choose a complete Theme Package first.", Toast.LENGTH_SHORT).show()
                    refreshThemeAppearance()
                }
            }
        }
        findViewById<CheckBox>(R.id.bubble_key_checkbox).setOnClickListener { view ->
            val enabled = (view as CheckBox).isChecked
            lifecycleScope.launch {
                settingsRepository.setBubbleKeyEnabled(enabled)
                val pack = app.themeRepository.currentSelection().globalPack
                if (pack != null) app.themeRepository.setBubbleEnabled(pack, enabled)
                refreshThemeAppearance()
            }
        }
        findViewById<Button>(R.id.button_bubble_soft).setOnClickListener { setBubbleKeyIntensity(BubbleKeyIntensity.SOFT) }
        findViewById<Button>(R.id.button_bubble_normal).setOnClickListener { setBubbleKeyIntensity(BubbleKeyIntensity.NORMAL) }
        findViewById<Button>(R.id.button_bubble_playful).setOnClickListener { setBubbleKeyIntensity(BubbleKeyIntensity.PLAYFUL) }
        findViewById<Button>(R.id.button_one_hand_left).setOnClickListener { setOneHandedMode(OneHandedMode.LEFT) }
        findViewById<Button>(R.id.button_one_hand_off).setOnClickListener { setOneHandedMode(OneHandedMode.OFF) }
        findViewById<Button>(R.id.button_one_hand_right).setOnClickListener { setOneHandedMode(OneHandedMode.RIGHT) }
        findViewById<Button>(R.id.button_height_compact).setOnClickListener { setKeyboardHeight(44) }
        findViewById<Button>(R.id.button_height_normal).setOnClickListener { setKeyboardHeight(50) }
        findViewById<Button>(R.id.button_height_tall).setOnClickListener { setKeyboardHeight(56) }
    }

    private fun bindTypingSuggestions() {
        findViewById<CheckBox>(R.id.personal_typing_learning_checkbox).setOnClickListener { view ->
            val enabled = (view as CheckBox).isChecked
            lifecycleScope.launch {
                settingsRepository.setPersonalTypingLearning(enabled)
                refreshTypingSuggestions()
            }
        }
        findViewById<Button>(R.id.button_clear_typing_learning).setOnClickListener {
            PersistentTypingLearningModel.clearStoredLearning(this)
            PersistentEnglishTypingLearningModel.clearStoredLearning(this)
            Toast.makeText(this, "Learned Bangla and English typing data cleared from this device.", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch { refreshTypingSuggestions() }
        }
        findViewById<CheckBox>(R.id.spacebar_cursor_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setSpacebarCursorControl((view as CheckBox).isChecked)
                refreshTypingSuggestions()
            }
        }
        findViewById<CheckBox>(R.id.glide_typing_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setGlideTyping((view as CheckBox).isChecked)
                refreshTypingSuggestions()
            }
        }
        findViewById<Button>(R.id.button_toolbar_balanced).setOnClickListener { setToolbarProfile(ToolbarProfile.BALANCED) }
        findViewById<Button>(R.id.button_toolbar_ai_first).setOnClickListener { setToolbarProfile(ToolbarProfile.AI_FIRST) }
        findViewById<Button>(R.id.button_toolbar_typing).setOnClickListener { setToolbarProfile(ToolbarProfile.TYPING) }
        findViewById<Button>(R.id.button_toolbar_minimal).setOnClickListener { setToolbarProfile(ToolbarProfile.MINIMAL) }
        findViewById<CheckBox>(R.id.haptic_feedback_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setHapticFeedback((view as CheckBox).isChecked)
                refreshTypingSuggestions()
            }
        }
        findViewById<CheckBox>(R.id.key_sound_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                settingsRepository.setKeySound((view as CheckBox).isChecked)
                refreshTypingSuggestions()
            }
        }
    }
    private fun bindAiPrivacy() {
        findViewById<Button>(R.id.button_save_ai_privacy_consent).setOnClickListener { saveAiPrivacyAndDraftSettings() }
        findViewById<Button>(R.id.button_context_access).setOnClickListener { acceptDisclosureAndOpenAccessibility() }
        findViewById<Button>(R.id.button_disable_context_access).setOnClickListener { disableContextAccess() }
        findViewById<Button>(R.id.button_save_custom_instruction).setOnClickListener { saveCustomInstruction() }
        findViewById<Button>(R.id.button_clear_custom_instruction).setOnClickListener { clearCustomInstruction() }
        findViewById<Button>(R.id.button_save_personal_training).setOnClickListener { savePersonalTraining() }
        findViewById<Button>(R.id.button_clear_personal_training).setOnClickListener { clearPersonalTraining() }
        findViewById<Button>(R.id.button_caption).setOnClickListener { startActivity(Intent(this, CaptionActivity::class.java)) }
        findViewById<Button>(R.id.button_privacy_data).setOnClickListener { startActivity(Intent(this, PrivacyActivity::class.java)) }
    }
    private fun bindClipboard() {
        findViewById<CheckBox>(R.id.clipboard_history_checkbox).setOnClickListener { view ->
            lifecycleScope.launch {
                val enabled = (view as CheckBox).isChecked
                settingsRepository.setClipboardHistory(enabled)
                if (!enabled) {
                    RecentClipboardStore(this@SettingsCategoryActivity).clear()
                    Toast.makeText(
                        this@SettingsCategoryActivity,
                        "Clipboard history disabled and stored history cleared.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                refreshClipboard()
            }
        }
        findViewById<Button>(R.id.button_clear_clipboard_history).setOnClickListener {
            RecentClipboardStore(this).clear()
            Toast.makeText(this, "Recent clipboard history cleared from this device.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun bindAccountSubscription() {
        findViewById<Button>(R.id.button_toggle_advanced_ai).setOnClickListener { toggleAdvancedAi() }
        findViewById<Button>(R.id.button_login_managed).setOnClickListener { startActivity(Intent(this, AuthActivity::class.java)) }
        findViewById<Button>(R.id.button_logout_managed).setOnClickListener { logoutManaged() }
        findViewById<Button>(R.id.button_refresh_account).setOnClickListener { refreshManagedAccount(showErrors = true) }
        findViewById<Button>(R.id.button_account_portal).setOnClickListener { startActivity(Intent(this, PortalActivity::class.java)) }
        findViewById<Button>(R.id.button_use_managed).setOnClickListener { useManagedAi() }
        findViewById<Button>(R.id.button_use_personal).setOnClickListener {
            lifecycleScope.launch {
                settingsRepository.setGatewayMode(GatewayMode.PERSONAL)
                Toast.makeText(this@SettingsCategoryActivity, "Personal OpenRouter mode selected.", Toast.LENGTH_SHORT).show()
                refreshAccountSubscription()
            }
        }
        findViewById<Button>(R.id.button_managed_ai).setOnClickListener { useManagedAi() }
        findViewById<Button>(R.id.button_save_api_key).setOnClickListener { saveApiKey() }
        findViewById<Button>(R.id.button_clear_api_key).setOnClickListener {
            secretStore.clearOpenRouterKey()
            findViewById<EditText>(R.id.input_api_key).text?.clear()
            lifecycleScope.launch { refreshAccountSubscription() }
        }
        findViewById<Button>(R.id.button_test_api_key).setOnClickListener { testApiKey() }
    }

    private fun bindHelpAbout() {
        findViewById<Button>(R.id.button_whatsapp_support).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801770921730")))
        }
    }

    private suspend fun refreshKeyboardSetup() {
        val imeComponent = ComponentName(this, SocialAiInputMethodService::class.java)
        val enabledImes = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS).orEmpty()
        val selectedIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        val keyboardEnabled = enabledImes.split(':')
            .map { it.substringBefore(';') }
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it == imeComponent }
        val keyboardSelected = ComponentName.unflattenFromString(selectedIme.substringBefore(';')) == imeComponent
        findViewById<TextView>(R.id.status_keyboard_enabled).text =
            getString(R.string.status_keyboard_enabled_value, yesNo(keyboardEnabled))
        findViewById<TextView>(R.id.status_current_keyboard).text =
            getString(R.string.status_current_keyboard_value, yesNo(keyboardSelected))
    }

    private suspend fun refreshLanguageInput() {
        val settings = settingsRepository.current()
        findViewById<CheckBox>(R.id.english_suggestions_checkbox).isChecked = settings.englishSuggestions
        findViewById<CheckBox>(R.id.english_autocorrect_checkbox).isChecked = settings.englishAutocorrect
        findViewById<CheckBox>(R.id.smart_language_hints_checkbox).isChecked = settings.smartLanguageHints
    }
    private suspend fun refreshThemeAppearance() {
        val settings = settingsRepository.current()
        val theme = app.themeRepository.current()
        val pack = app.themeRepository.currentSelection().globalPack
        val settingsPack = settingsThemeRepository.currentPack()

        findViewById<TextView>(R.id.status_theme_name).text = "Current keyboard theme: ${theme.displayName}"
        findViewById<TextView>(R.id.status_settings_theme_category).text = "Settings theme: ${settingsPack.displayName}"
        findViewById<CheckBox>(R.id.number_row_checkbox).isChecked = settings.showNumberRow
        findViewById<TextView>(R.id.status_one_handed_mode).text =
            "One-handed mode: ${settings.oneHandedMode.name.lowercase().replaceFirstChar { it.uppercase() }}"
        findViewById<TextView>(R.id.status_keyboard_height).text = "Keyboard height: ${settings.keyboardKeyHeightDp}dp"

        val boundary = findViewById<CheckBox>(R.id.key_boundary_checkbox)
        boundary.isEnabled = pack != null
        boundary.isChecked = pack?.let { app.themeRepository.currentKeyBoundaryEnabled(pack) } ?: false

        val bubbleAppearance = pack?.let { app.themeRepository.currentBubbleAppearance(pack) }
        findViewById<CheckBox>(R.id.bubble_key_checkbox).isChecked = bubbleAppearance?.enabled ?: settings.bubbleKeyEnabled
        findViewById<TextView>(R.id.status_bubble_key_intensity).text =
            "Bubble Effect style: ${(bubbleAppearance?.intensity ?: settings.bubbleKeyIntensity).label}"
    }

    private suspend fun refreshTypingSuggestions() {
        val settings = settingsRepository.current()
        findViewById<CheckBox>(R.id.personal_typing_learning_checkbox).isChecked = settings.personalTypingLearning
        findViewById<CheckBox>(R.id.spacebar_cursor_checkbox).isChecked = settings.spacebarCursorControl
        findViewById<CheckBox>(R.id.glide_typing_checkbox).isChecked = settings.glideTyping
        findViewById<CheckBox>(R.id.haptic_feedback_checkbox).isChecked = settings.hapticFeedback
        findViewById<CheckBox>(R.id.key_sound_checkbox).isChecked = settings.keySound
        findViewById<TextView>(R.id.status_toolbar_profile).text =
            "Toolbar: ${settings.toolbarProfile.name.lowercase().replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }}"
        val typingStats = PersistentTypingLearningModel.storedStats(this)
        val englishTypingStats = PersistentEnglishTypingLearningModel.storedStats(this)
        findViewById<TextView>(R.id.status_typing_learning).text = if (settings.personalTypingLearning) {
            "Personal typing learning: On - Bangla ${typingStats.learnedWords} words / ${typingStats.learnedTransitions} pairs; English ${englishTypingStats.learnedWords} words / ${englishTypingStats.learnedTransitions} pairs"
        } else {
            "Personal typing learning: Off - stored Bangla/English data remains local until cleared"
        }
    }
    private suspend fun refreshAiPrivacy() {
        val settings = settingsRepository.current()
        findViewById<CheckBox>(R.id.ai_privacy_consent_checkbox).isChecked = settings.aiPrivacyConsent
        findViewById<CheckBox>(R.id.preserve_draft_checkbox).isChecked = settings.preserveDraft
        findViewById<CheckBox>(R.id.context_consent_checkbox).isChecked =
            settings.contextAccessConsent && settings.accessibilityDisclosureAccepted
        findViewById<TextView>(R.id.status_context_access).text =
            getString(R.string.status_context_access_value, yesNo(isAccessibilityServiceEnabled()))
        findViewById<TextView>(R.id.status_ai_tone).text = getString(R.string.ai_tone_status_value, settings.tonePreset.label)
        val customInput = findViewById<EditText>(R.id.input_custom_instruction)
        if (!customInput.hasFocus()) customInput.setText(settings.customInstruction)
        val trainingInput = findViewById<EditText>(R.id.input_personal_training)
        val trainingValue = if (settings.gatewayMode == GatewayMode.MANAGED) {
            app.currentManagedPersonalTraining()
        } else {
            settings.personalTraining
        }
        if (!trainingInput.hasFocus()) trainingInput.setText(trainingValue)
    }
    private suspend fun refreshClipboard() {
        val settings = settingsRepository.current()
        findViewById<CheckBox>(R.id.clipboard_history_checkbox).isChecked = settings.clipboardHistory
    }
    private suspend fun refreshAccountSubscription() {
        val settings = settingsRepository.current()
        val apiConfigured = secretStore.isOpenRouterKeyConfigured()
        val apiMask = secretStore.configuredMask()
        findViewById<TextView>(R.id.status_personal_api).text = if (apiConfigured) {
            getString(R.string.status_personal_api_configured, apiMask ?: getString(R.string.configured))
        } else {
            getString(R.string.status_personal_api_not_configured)
        }
        findViewById<TextView>(R.id.status_gateway).text =
            "Gateway: ${if (settings.gatewayMode == GatewayMode.MANAGED) "Managed AI" else "Personal OpenRouter"}"
        if (!app.managedSessionStore.isLoggedIn()) {
            setLoggedOutAccountStatus()
        } else {
            refreshManagedAccount(showErrors = false)
        }
    }

    private suspend fun refreshHelpAbout() = Unit

    private fun saveAiPrivacyAndDraftSettings() {
        val consent = findViewById<CheckBox>(R.id.ai_privacy_consent_checkbox).isChecked
        val preserveDraft = findViewById<CheckBox>(R.id.preserve_draft_checkbox).isChecked
        lifecycleScope.launch {
            settingsRepository.setAiPrivacyConsent(consent)
            settingsRepository.setPreserveDraft(preserveDraft)
            Toast.makeText(
                this@SettingsCategoryActivity,
                if (consent) "AI privacy and draft settings saved." else "AI Privacy/Data consent disabled. AI generation is now blocked.",
                Toast.LENGTH_SHORT
            ).show()
            refreshAiPrivacy()
        }
    }

    private fun acceptDisclosureAndOpenAccessibility() {
        val consent = findViewById<CheckBox>(R.id.context_consent_checkbox)
        if (!consent.isChecked) {
            Toast.makeText(this, R.string.context_disclosure_required, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            settingsRepository.setAccessibilityDisclosureAccepted(true)
            settingsRepository.setContextAccessConsent(true)
            ContextAccessGate.update(true)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun disableContextAccess() {
        lifecycleScope.launch {
            settingsRepository.setContextAccessConsent(false)
            ContextAccessGate.update(false)
            findViewById<CheckBox>(R.id.context_consent_checkbox).isChecked = false
            Toast.makeText(this@SettingsCategoryActivity, R.string.context_access_disabled, Toast.LENGTH_SHORT).show()
            refreshAiPrivacy()
        }
    }

    private fun saveCustomInstruction() {
        val value = findViewById<EditText>(R.id.input_custom_instruction).text?.toString().orEmpty()
        lifecycleScope.launch {
            settingsRepository.setCustomInstruction(value)
            Toast.makeText(this@SettingsCategoryActivity, R.string.custom_instruction_saved, Toast.LENGTH_SHORT).show()
            refreshAiPrivacy()
        }
    }

    private fun clearCustomInstruction() {
        lifecycleScope.launch {
            settingsRepository.setCustomInstruction("")
            findViewById<EditText>(R.id.input_custom_instruction).text?.clear()
            Toast.makeText(this@SettingsCategoryActivity, R.string.custom_instruction_cleared, Toast.LENGTH_SHORT).show()
            refreshAiPrivacy()
        }
    }

    private fun savePersonalTraining() {
        val value = findViewById<EditText>(R.id.input_personal_training).text?.toString().orEmpty()
        lifecycleScope.launch {
            val settings = settingsRepository.current()
            val session = app.managedSessionStore.load()
            if (settings.gatewayMode == GatewayMode.MANAGED && session != null) {
                secretStore.putAccountTraining(session.userId, value)
                settingsRepository.bumpCustomKnowledgeRevision()
                Toast.makeText(this@SettingsCategoryActivity, "Personal AI Training saved for this managed account.", Toast.LENGTH_SHORT).show()
            } else {
                settingsRepository.setPersonalTraining(value)
                Toast.makeText(this@SettingsCategoryActivity, "Personal AI Training saved for Personal OpenRouter mode.", Toast.LENGTH_SHORT).show()
            }
            refreshAiPrivacy()
        }
    }

    private fun clearPersonalTraining() {
        lifecycleScope.launch {
            val settings = settingsRepository.current()
            val session = app.managedSessionStore.load()
            if (settings.gatewayMode == GatewayMode.MANAGED && session != null) {
                secretStore.clearAccountTraining(session.userId)
                settingsRepository.bumpCustomKnowledgeRevision()
            } else {
                settingsRepository.setPersonalTraining("")
            }
            findViewById<EditText>(R.id.input_personal_training).text?.clear()
            Toast.makeText(this@SettingsCategoryActivity, "Personal AI Training cleared.", Toast.LENGTH_SHORT).show()
            refreshAiPrivacy()
        }
    }

    private fun toggleAdvancedAi() {
        val container = findViewById<LinearLayout>(R.id.advanced_ai_container)
        val button = findViewById<Button>(R.id.button_toggle_advanced_ai)
        val opening = container.visibility != View.VISIBLE
        container.visibility = if (opening) View.VISIBLE else View.GONE
        button.text = if (opening) "Advanced AI ▴" else "Advanced AI ▾"
    }

    private fun useManagedAi() {
        if (!app.managedSessionStore.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            return
        }
        lifecycleScope.launch {
            settingsRepository.setGatewayMode(GatewayMode.MANAGED)
            Toast.makeText(this@SettingsCategoryActivity, "Managed AI selected.", Toast.LENGTH_SHORT).show()
            refreshAccountSubscription()
        }
    }

    private fun logoutManaged() {
        lifecycleScope.launch {
            try {
                app.backendClient.logout()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // logout() clears the managed bearer session in its finally block; network
                // failure must not block local sign-out or leave hosted WebView credentials.
            }
            // WebView cookies / DOM storage are a separate credential surface from
            // the managed API bearer session, so logout must clear both.
            SecureWebViewSupport.clearHostedSession()
            Toast.makeText(this@SettingsCategoryActivity, "Managed account logged out.", Toast.LENGTH_SHORT).show()
            refreshAccountSubscription()
        }
    }

    private fun setLoggedOutAccountStatus() {
        findViewById<TextView>(R.id.status_managed_account).text = "Managed account: Not logged in"
        findViewById<TextView>(R.id.status_keyboard_product).text = "Social AI Keyboard: —"
        findViewById<TextView>(R.id.status_assistant_product).text = "AI Assistant Pro: —"
        findViewById<TextView>(R.id.status_usage).text = "Usage: —"
    }

    private fun refreshManagedAccount(showErrors: Boolean) {
        if (!app.managedSessionStore.isLoggedIn()) {
            setLoggedOutAccountStatus()
            return
        }
        lifecycleScope.launch {
            try {
                val state = app.backendClient.accountState()
                findViewById<TextView>(R.id.status_managed_account).text = buildString {
                    append("Managed account: ${state.email.ifBlank { state.mobile }}")
                    if (state.passwordResetRequired) append(" • password change required")
                }
                findViewById<TextView>(R.id.status_keyboard_product).text = productStatusLabel(
                    "Social AI Keyboard",
                    state.keyboardEntitlement.status,
                    state.keyboardEntitlement.cycleExpiresAt
                )
                findViewById<TextView>(R.id.status_assistant_product).text = productStatusLabel(
                    "AI Assistant Pro",
                    state.assistantProEntitlement.status,
                    state.assistantProEntitlement.cycleExpiresAt
                )
                findViewById<TextView>(R.id.status_usage).text =
                    "Usage: today ${state.dailyUsed}/${state.dailyLimit} • cycle ${state.cycleUsed}/${state.cycleLimit}"
            } catch (error: CancellationException) {
                throw error
            } catch (error: BackendException) {
                if (showErrors) {
                    Toast.makeText(this@SettingsCategoryActivity, BackendErrorMessages.userMessage(error), Toast.LENGTH_LONG).show()
                }
                if (!app.managedSessionStore.isLoggedIn()) setLoggedOutAccountStatus()
            } catch (error: Throwable) {
                if (showErrors) {
                    Toast.makeText(this@SettingsCategoryActivity, error.message ?: "Account refresh failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveApiKey() {
        val input = findViewById<EditText>(R.id.input_api_key)
        val key = input.text?.toString().orEmpty().trim()
        if (key.isEmpty()) {
            Toast.makeText(this, R.string.api_key_required, Toast.LENGTH_SHORT).show()
            return
        }
        secretStore.putOpenRouterKey(key)
        input.text?.clear()
        Toast.makeText(this, R.string.api_key_saved, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch { refreshAccountSubscription() }
    }

    private fun testApiKey() {
        if (!secretStore.isOpenRouterKeyConfigured()) {
            Toast.makeText(this, R.string.api_key_required, Toast.LENGTH_SHORT).show()
            return
        }
        val button = findViewById<Button>(R.id.button_test_api_key)
        button.isEnabled = false
        button.text = getString(R.string.testing_api_key)
        lifecycleScope.launch {
            val message = try {
                app.personalAiGateway.generate(
                    AiGenerationRequest(
                        prompt = PromptBundle(
                            system = "This is a connection test. Return only the word OK.",
                            user = "Connection test"
                        ),
                        modelMode = ModelMode.FAST
                    )
                )
                R.string.api_key_test_success
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                apiTestErrorMessage(error)
            }
            Toast.makeText(this@SettingsCategoryActivity, message, Toast.LENGTH_LONG).show()
            button.isEnabled = true
            button.text = getString(R.string.test_api_key)
        }
    }

    private fun apiTestErrorMessage(error: Throwable): Int = when (error) {
        is AiGatewayException.InvalidApiKey -> R.string.api_key_test_invalid
        is AiGatewayException.InsufficientCredits -> R.string.api_key_test_credits
        is AiGatewayException.RateLimited -> R.string.api_key_test_rate_limited
        is AiGatewayException.Offline -> R.string.api_key_test_offline
        is AiGatewayException.Timeout -> R.string.api_key_test_timeout
        else -> R.string.api_key_test_failed
    }

    private fun productStatusLabel(name: String, status: String, expiry: String?): String = buildString {
        append("$name: $status")
        expiry?.takeIf { it.isNotBlank() }?.let { append(" • ends $it") }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val expected = ComponentName(this, SocialAiAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info -> info.resolveInfo.serviceInfo?.let { ComponentName(it.packageName, it.name) } == expected }
    }

    private fun setBubbleKeyIntensity(intensity: BubbleKeyIntensity) {
        lifecycleScope.launch {
            settingsRepository.setBubbleKeyIntensity(intensity)
            val pack = app.themeRepository.currentSelection().globalPack
            if (pack != null) app.themeRepository.setBubbleIntensity(pack, intensity)
            refreshThemeAppearance()
        }
    }

    private fun setOneHandedMode(mode: OneHandedMode) {
        lifecycleScope.launch {
            settingsRepository.setOneHandedMode(mode)
            refreshThemeAppearance()
        }
    }

    private fun setToolbarProfile(profile: ToolbarProfile) {
        lifecycleScope.launch {
            settingsRepository.setToolbarProfile(profile)
            refreshTypingSuggestions()
        }
    }

    private fun setKeyboardHeight(heightDp: Int) {
        lifecycleScope.launch {
            settingsRepository.setKeyboardKeyHeightDp(heightDp)
            refreshThemeAppearance()
        }
    }

    private fun yesNo(value: Boolean): String = getString(if (value) R.string.yes else R.string.no)
}
