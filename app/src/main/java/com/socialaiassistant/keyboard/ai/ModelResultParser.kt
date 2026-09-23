package com.socialaiassistant.keyboard.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class ModelResultParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String?): ParsedAiResult? {
        val cleaned = ExtensionLanguageLogic.cleanString(raw, 8_000)
        if (cleaned.isEmpty()) return null

        val normalized = ModelEnvelopeNormalizer.normalize(cleaned)
        parseJson(normalized)?.let { return it }

        if (ModelEnvelopeNormalizer.looksLikeEnvelope(cleaned) ||
            ModelEnvelopeNormalizer.looksLikeEnvelope(normalized)
        ) {
            return null
        }

        val fallback = normalized
            .replace(Regex("^(reply|answer|comment)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        if (fallback.isEmpty()) return null
        return ParsedAiResult(reply = fallback, confidence = 0.5)
    }

    private fun parseJson(value: String): ParsedAiResult? {
        val element = runCatching { json.parseToJsonElement(value) }.getOrNull() ?: return null
        val obj = element as? JsonObject ?: return null
        val reply = obj["reply"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (reply.isEmpty()) return null

        val category = obj["category"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        val confidence = obj["confidence"]?.jsonPrimitive?.doubleOrNull?.coerceIn(0.0, 1.0) ?: 0.5
        val memory = obj["conversationMemory"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        return ParsedAiResult(reply, category, confidence, memory)
    }
}

private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else content.takeIf { it != "null" }
