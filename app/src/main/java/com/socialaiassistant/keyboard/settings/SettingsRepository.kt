package com.socialaiassistant.keyboard.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.socialaiassistant.keyboard.ai.GatewayMode
import com.socialaiassistant.keyboard.ai.ModelMode
import com.socialaiassistant.keyboard.ai.TonePreset
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.socialAiSettingsDataStore by preferencesDataStore(name = "social_ai_settings")

data class AppSettings(
    val modelMode: ModelMode = ModelMode.FAST,
    val gatewayMode: GatewayMode = GatewayMode.MANAGED,
    val preserveDraft: Boolean = true,
    val personalTypingLearning: Boolean = true,
    val englishSuggestions: Boolean = true,
    val englishAutocorrect: Boolean = true,
    val smartLanguageHints: Boolean = true,
    val showNumberRow: Boolean = false,
    val spacebarCursorControl: Boolean = true,
    val hapticFeedback: Boolean = true,
    val keySound: Boolean = false,
    val clipboardHistory: Boolean = false,
    val keyboardKeyHeightDp: Int = 50,
    val glideTyping: Boolean = false,
    val bubbleKeyEnabled: Boolean = false,
    val bubbleKeyIntensity: BubbleKeyIntensity = BubbleKeyIntensity.NORMAL,
    val oneHandedMode: OneHandedMode = OneHandedMode.OFF,
    val toolbarProfile: ToolbarProfile = ToolbarProfile.BALANCED,
    val aiPrivacyConsent: Boolean = false,
    val contextAccessConsent: Boolean = false,
    val accessibilityDisclosureAccepted: Boolean = false,
    val maxChatMessages: Int = 30,
    val tonePreset: TonePreset = TonePreset.AUTO,
    val customInstruction: String = "",
    val personalTraining: String = "",
    val customKnowledgeRevision: Long = 0L
)

class SettingsRepository(
    private val store: DataStore<Preferences>
) {
    val settings: Flow<AppSettings> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)

    suspend fun current(): AppSettings = settings.first()

    suspend fun setModelMode(value: ModelMode) = update(MODEL_MODE, value.name.lowercase())

    suspend fun setGatewayMode(value: GatewayMode) = update(GATEWAY_MODE, value.name.lowercase())

    suspend fun setPreserveDraft(value: Boolean) = update(PRESERVE_DRAFT, value)

    suspend fun setPersonalTypingLearning(value: Boolean) = update(PERSONAL_TYPING_LEARNING, value)

    suspend fun setEnglishSuggestions(value: Boolean) = update(ENGLISH_SUGGESTIONS, value)

    suspend fun setEnglishAutocorrect(value: Boolean) = update(ENGLISH_AUTOCORRECT, value)

    suspend fun setSmartLanguageHints(value: Boolean) = update(SMART_LANGUAGE_HINTS, value)

    suspend fun setShowNumberRow(value: Boolean) = update(SHOW_NUMBER_ROW, value)

    suspend fun setSpacebarCursorControl(value: Boolean) = update(SPACEBAR_CURSOR_CONTROL, value)

    suspend fun setHapticFeedback(value: Boolean) = update(HAPTIC_FEEDBACK, value)

    suspend fun setKeySound(value: Boolean) = update(KEY_SOUND, value)

    suspend fun setClipboardHistory(value: Boolean) = update(CLIPBOARD_HISTORY, value)

    suspend fun setKeyboardKeyHeightDp(value: Int) = update(KEYBOARD_KEY_HEIGHT_DP, normalizeKeyHeight(value))

    suspend fun setGlideTyping(value: Boolean) = update(GLIDE_TYPING, value)

    suspend fun setBubbleKeyEnabled(value: Boolean) = update(BUBBLE_KEY_ENABLED, value)

    suspend fun setBubbleKeyIntensity(value: BubbleKeyIntensity) = update(BUBBLE_KEY_INTENSITY, value.settingValue)

    suspend fun setOneHandedMode(value: OneHandedMode) = update(ONE_HANDED_MODE, value.settingValue)

    suspend fun setToolbarProfile(value: ToolbarProfile) = update(TOOLBAR_PROFILE, value.settingValue)

    suspend fun setAiPrivacyConsent(value: Boolean) = update(AI_PRIVACY_CONSENT, value)

    suspend fun setContextAccessConsent(value: Boolean) = update(CONTEXT_ACCESS_CONSENT, value)

    suspend fun setAccessibilityDisclosureAccepted(value: Boolean) =
        update(ACCESSIBILITY_DISCLOSURE_ACCEPTED, value)

    suspend fun setMaxChatMessages(value: Int) = update(MAX_CHAT_MESSAGES, normalizeChatLimit(value))

    suspend fun setTonePreset(value: TonePreset) {
        store.edit { preferences ->
            preferences[TONE_PRESET] = value.name.lowercase()
            preferences[CUSTOM_KNOWLEDGE_REVISION] = (preferences[CUSTOM_KNOWLEDGE_REVISION] ?: 0L) + 1L
        }
    }

    suspend fun setCustomInstruction(value: String) {
        val normalized = value.replace(Regex("\\s+"), " ").trim().take(MAX_CUSTOM_INSTRUCTION_CHARS)
        store.edit { preferences ->
            preferences[CUSTOM_INSTRUCTION] = normalized
            preferences[CUSTOM_KNOWLEDGE_REVISION] = (preferences[CUSTOM_KNOWLEDGE_REVISION] ?: 0L) + 1L
        }
    }

    suspend fun setPersonalTraining(value: String) {
        val normalized = value.replace(Regex("\\s+"), " ").trim().take(MAX_PERSONAL_TRAINING_CHARS)
        store.edit { preferences ->
            preferences[PERSONAL_TRAINING] = normalized
            preferences[CUSTOM_KNOWLEDGE_REVISION] = (preferences[CUSTOM_KNOWLEDGE_REVISION] ?: 0L) + 1L
        }
    }

    suspend fun bumpCustomKnowledgeRevision() {
        store.edit { preferences ->
            preferences[CUSTOM_KNOWLEDGE_REVISION] = (preferences[CUSTOM_KNOWLEDGE_REVISION] ?: 0L) + 1L
        }
    }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private fun decode(preferences: Preferences): AppSettings = AppSettings(
        modelMode = ModelMode.fromSetting(preferences[MODEL_MODE]),
        gatewayMode = GatewayMode.fromSetting(preferences[GATEWAY_MODE]),
        preserveDraft = preferences[PRESERVE_DRAFT] ?: true,
        personalTypingLearning = preferences[PERSONAL_TYPING_LEARNING] ?: true,
        englishSuggestions = preferences[ENGLISH_SUGGESTIONS] ?: true,
        englishAutocorrect = preferences[ENGLISH_AUTOCORRECT] ?: true,
        smartLanguageHints = preferences[SMART_LANGUAGE_HINTS] ?: true,
        showNumberRow = preferences[SHOW_NUMBER_ROW] ?: false,
        spacebarCursorControl = preferences[SPACEBAR_CURSOR_CONTROL] ?: true,
        hapticFeedback = preferences[HAPTIC_FEEDBACK] ?: true,
        keySound = preferences[KEY_SOUND] ?: false,
        clipboardHistory = preferences[CLIPBOARD_HISTORY] ?: false,
        keyboardKeyHeightDp = normalizeKeyHeight(preferences[KEYBOARD_KEY_HEIGHT_DP] ?: 50),
        glideTyping = preferences[GLIDE_TYPING] ?: false,
        bubbleKeyEnabled = preferences[BUBBLE_KEY_ENABLED] ?: false,
        bubbleKeyIntensity = BubbleKeyIntensity.fromSetting(preferences[BUBBLE_KEY_INTENSITY]),
        oneHandedMode = OneHandedMode.fromSetting(preferences[ONE_HANDED_MODE]),
        toolbarProfile = ToolbarProfile.fromSetting(preferences[TOOLBAR_PROFILE]),
        aiPrivacyConsent = preferences[AI_PRIVACY_CONSENT] ?: false,
        contextAccessConsent = preferences[CONTEXT_ACCESS_CONSENT] ?: false,
        accessibilityDisclosureAccepted = preferences[ACCESSIBILITY_DISCLOSURE_ACCEPTED] ?: false,
        maxChatMessages = normalizeChatLimit(preferences[MAX_CHAT_MESSAGES] ?: 30),
        tonePreset = TonePreset.fromSetting(preferences[TONE_PRESET]),
        customInstruction = preferences[CUSTOM_INSTRUCTION].orEmpty().take(MAX_CUSTOM_INSTRUCTION_CHARS),
        personalTraining = preferences[PERSONAL_TRAINING].orEmpty().take(MAX_PERSONAL_TRAINING_CHARS),
        customKnowledgeRevision = preferences[CUSTOM_KNOWLEDGE_REVISION] ?: 0L
    )

    private fun normalizeChatLimit(value: Int): Int = if (value == 10) 10 else 30

    private fun normalizeKeyHeight(value: Int): Int = when {
        value <= 46 -> 44
        value >= 54 -> 56
        else -> 50
    }

    companion object {
        private val MODEL_MODE = stringPreferencesKey("model_mode")
        private val GATEWAY_MODE = stringPreferencesKey("gateway_mode")
        private val PRESERVE_DRAFT = booleanPreferencesKey("preserve_draft")
        private val PERSONAL_TYPING_LEARNING = booleanPreferencesKey("personal_typing_learning")
        private val ENGLISH_SUGGESTIONS = booleanPreferencesKey("english_suggestions")
        private val ENGLISH_AUTOCORRECT = booleanPreferencesKey("english_autocorrect")
        private val SMART_LANGUAGE_HINTS = booleanPreferencesKey("smart_language_hints")
        private val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
        private val SPACEBAR_CURSOR_CONTROL = booleanPreferencesKey("spacebar_cursor_control")
        private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val KEY_SOUND = booleanPreferencesKey("key_sound")
        private val CLIPBOARD_HISTORY = booleanPreferencesKey("clipboard_history")
        private val KEYBOARD_KEY_HEIGHT_DP = intPreferencesKey("keyboard_key_height_dp")
        private val GLIDE_TYPING = booleanPreferencesKey("glide_typing")
        private val BUBBLE_KEY_ENABLED = booleanPreferencesKey("bubble_key_enabled")
        private val BUBBLE_KEY_INTENSITY = stringPreferencesKey("bubble_key_intensity")
        private val ONE_HANDED_MODE = stringPreferencesKey("one_handed_mode")
        private val TOOLBAR_PROFILE = stringPreferencesKey("toolbar_profile")
        private val AI_PRIVACY_CONSENT = booleanPreferencesKey("ai_privacy_consent")
        private val CONTEXT_ACCESS_CONSENT = booleanPreferencesKey("context_access_consent")
        private val ACCESSIBILITY_DISCLOSURE_ACCEPTED = booleanPreferencesKey("accessibility_disclosure_accepted")
        private val MAX_CHAT_MESSAGES = intPreferencesKey("max_chat_messages")
        private val TONE_PRESET = stringPreferencesKey("tone_preset")
        private val CUSTOM_INSTRUCTION = stringPreferencesKey("custom_instruction")
        private val PERSONAL_TRAINING = stringPreferencesKey("personal_training")
        private val CUSTOM_KNOWLEDGE_REVISION = longPreferencesKey("custom_knowledge_revision")
        private const val MAX_CUSTOM_INSTRUCTION_CHARS = 1_200
        private const val MAX_PERSONAL_TRAINING_CHARS = 1_800

        fun create(context: Context): SettingsRepository =
            SettingsRepository(context.applicationContext.socialAiSettingsDataStore)
    }
}
