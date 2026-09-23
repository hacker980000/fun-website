package com.socialaiassistant.keyboard.ai

enum class TonePreset(
    val label: String,
    val promptInstruction: String
) {
    AUTO(
        label = "Auto",
        promptInstruction = "Match the recipient/post context naturally without forcing a special tone."
    ),
    FRIENDLY(
        label = "Friendly",
        promptInstruction = "Use a friendly, approachable, natural tone without sounding overly familiar."
    ),
    PROFESSIONAL(
        label = "Professional",
        promptInstruction = "Use a clear, polished, professional tone that still sounds human."
    ),
    CASUAL(
        label = "Casual",
        promptInstruction = "Use a relaxed, conversational tone with natural everyday wording."
    ),
    CONCISE(
        label = "Concise",
        promptInstruction = "Keep the response especially concise while preserving the important meaning."
    ),
    WARM(
        label = "Warm",
        promptInstruction = "Use a warm, considerate tone without exaggeration or emotional pressure."
    );

    fun next(): TonePreset = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromSetting(value: String?): TonePreset = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: AUTO
    }
}
