package com.socialaiassistant.keyboard.settingsui

enum class SettingsCategoryId(val wireValue: String) {
    KEYBOARD_SETUP("keyboard_setup"),
    LANGUAGE_INPUT("language_input"),
    THEME_APPEARANCE("theme_appearance"),
    TYPING_SUGGESTIONS("typing_suggestions"),
    AI_PRIVACY("ai_privacy"),
    CLIPBOARD("clipboard"),
    ACCOUNT_SUBSCRIPTION("account_subscription"),
    HELP_ABOUT("help_about");

    companion object {
        val canonical: List<SettingsCategoryId> = entries.toList()

        fun fromWireValue(value: String?): SettingsCategoryId? =
            entries.firstOrNull { it.wireValue == value }

        fun fromLegacySection(value: String?): SettingsCategoryId? = when (value) {
            "ai_privacy" -> AI_PRIVACY
            else -> null
        }
    }
}
