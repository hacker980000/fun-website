package com.socialaiassistant.keyboard.settings

enum class OneHandedMode(val settingValue: String) {
    OFF("off"),
    LEFT("left"),
    RIGHT("right");

    companion object {
        fun fromSetting(value: String?): OneHandedMode = entries.firstOrNull { it.settingValue == value } ?: OFF
    }
}

enum class ToolbarProfile(val settingValue: String) {
    BALANCED("balanced"),
    AI_FIRST("ai_first"),
    TYPING("typing"),
    MINIMAL("minimal");

    companion object {
        fun fromSetting(value: String?): ToolbarProfile = entries.firstOrNull { it.settingValue == value } ?: BALANCED
    }
}

enum class BubbleKeyIntensity(val settingValue: String, val label: String) {
    SOFT("soft", "Soft"),
    NORMAL("normal", "Normal"),
    PLAYFUL("playful", "Playful");

    companion object {
        fun fromSetting(value: String?): BubbleKeyIntensity =
            entries.firstOrNull { it.settingValue == value } ?: NORMAL
    }
}
