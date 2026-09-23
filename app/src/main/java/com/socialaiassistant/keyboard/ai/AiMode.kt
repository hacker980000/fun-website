package com.socialaiassistant.keyboard.ai

enum class AiMode {
    GENERAL,
    FLIRT_MSG,
    FLIRT_CMT,
    FUNNY_CMT,
    WITTY;

    companion object {
        fun normalize(value: String?): AiMode = entries.firstOrNull { it.name == value } ?: GENERAL
    }
}

enum class ModelMode {
    FAST,
    SMART;

    companion object {
        fun fromSetting(value: String?): ModelMode = if (value.equals("smart", ignoreCase = true)) SMART else FAST
    }
}
