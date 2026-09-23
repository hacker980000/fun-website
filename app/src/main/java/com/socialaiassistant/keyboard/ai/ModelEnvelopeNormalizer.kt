package com.socialaiassistant.keyboard.ai

internal object ModelEnvelopeNormalizer {
    private val leadingJsonLabel = Regex("^\\s*json\\s*(?:\\r?\\n)+", RegexOption.IGNORE_CASE)
    private val openingFence = Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE)
    private val closingFence = Regex("\\s*```$", RegexOption.IGNORE_CASE)
    private val standaloneJson = Regex("^\\s*json(?:\\s|$)", RegexOption.IGNORE_CASE)

    fun normalize(raw: String): String {
        var value = raw.trim()
        value = value.replaceFirst(leadingJsonLabel, "").trim()
        if (value.startsWith("```")) {
            value = value.replaceFirst(openingFence, "")
            value = value.replace(closingFence, "").trim()
        }
        return value.trim()
    }

    fun looksLikeEnvelope(raw: String): Boolean {
        val value = raw.trim()
        if (value.isEmpty()) return false
        return value.startsWith("{") ||
            value.startsWith("[") ||
            value.startsWith("```") ||
            standaloneJson.containsMatchIn(value)
    }
}
