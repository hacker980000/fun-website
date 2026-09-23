package com.socialaiassistant.keyboard.backend

import com.socialaiassistant.keyboard.ai.AiMode
import com.socialaiassistant.keyboard.ai.ConversationAiIntent
import com.socialaiassistant.keyboard.ai.ExtensionLanguageLogic
import com.socialaiassistant.keyboard.ai.InteractionType
import com.socialaiassistant.keyboard.ai.LanguageMode
import com.socialaiassistant.keyboard.ai.PromptMessage
import com.socialaiassistant.keyboard.context.SenderClass
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ManagedAiPayload {
    fun social(
        type: InteractionType,
        mode: AiMode,
        messages: List<PromptMessage>,
        latestRecipientMessage: String?,
        postText: String,
        languageMode: LanguageMode?,
        personalTraining: String,
        sourcePlatform: String = "ANDROID_KEYBOARD",
        conversationIntent: ConversationAiIntent? = null
    ): JsonObject {
        val serverMode = when (mode) {
            AiMode.GENERAL -> "GENERAL"
            AiMode.WITTY -> "WITTY"
            AiMode.FLIRT_MSG -> "FLIRT_MSG"
            AiMode.FLIRT_CMT -> "FLIRT_CMT"
            AiMode.FUNNY_CMT -> "FUNNY_CMT"
        }
        return if (type == InteractionType.COMMENT) {
            buildJsonObject {
                put("type", "COMMENT")
                put("mode", serverMode)
                put("caption", postText.ifBlank { messages.joinToString("\n") { it.text } }.take(7000))
                put("contextText", postText.take(7000))
                put("commentLanguageMode", commentLanguage(languageMode, postText))
                put("personalTraining", personalTraining.take(1800))
                put("sourcePlatform", sourcePlatform)
            }
        } else {
            buildJsonObject {
                put("type", "INBOX")
                put("mode", serverMode)
                put("recipientLastMessage", latestRecipientMessage.orEmpty().take(3000))
                put("recentConversation", recentConversation(messages))
                put("isGroup", false)
                put("personalTraining", personalTraining.take(1800))
                put("sourcePlatform", sourcePlatform)
                conversationIntent?.takeIf { it != ConversationAiIntent.NEEDS_CONTEXT }?.let {
                    put("conversationIntent", it.name)
                }
            }
        }
    }

    fun funnyComment(
        messages: List<PromptMessage>,
        postText: String,
        languageMode: LanguageMode?,
        personalTraining: String
    ): JsonObject = buildJsonObject {
        put("type", "COMMENT")
        put("mode", "FUNNY_CMT")
        put("caption", postText.ifBlank { messages.joinToString("\n") { it.text } }.take(7000))
        put("contextText", postText.take(7000))
        put("commentLanguageMode", commentLanguage(languageMode, postText))
        put("personalTraining", personalTraining.take(1800))
        put("sourcePlatform", "ANDROID_KEYBOARD")
    }

    fun caption(
        mode: String,
        contextText: String,
        captionLanguageMode: String,
        imageDataUrls: List<String>,
        personalTraining: String
    ): JsonObject = buildJsonObject {
        put("type", "CAPTION")
        put("mode", mode.uppercase())
        put("contextText", contextText.take(7000))
        put("captionLanguageMode", captionLanguageMode)
        put("imageUrls", JsonArray(imageDataUrls.take(4).map(::JsonPrimitive)))
        put("mediaNote", if (imageDataUrls.isEmpty()) "No photo requested for this caption mode." else "${imageDataUrls.size.coerceAtMost(4)} selected photo(s)")
        put("personalTraining", personalTraining.take(1800))
        put("sourcePlatform", "ANDROID_KEYBOARD")
    }

    private fun recentConversation(messages: List<PromptMessage>) = buildJsonArray {
        // UNKNOWN rows are useful as local background context, but must never be promoted to
        // OTHER in the managed payload because that fabricates recipient attribution.
        messages.takeLast(10)
            .filter { it.sender != SenderClass.UNKNOWN }
            .forEach { message ->
                add(buildJsonObject {
                    put("sender", if (message.sender == SenderClass.SELF) "SELF" else "OTHER")
                    put("text", message.text.take(1800))
                })
            }
    }

    private fun commentLanguage(languageMode: LanguageMode?, text: String): String {
        val activeMode = ExtensionLanguageLogic.detectLanguageMode(text) ?: languageMode
        return when (activeMode) {
            LanguageMode.BANGLISH -> "BANGLISH"
            LanguageMode.LATIN_INFER -> "ENGLISH"
            LanguageMode.BENGALI, LanguageMode.BENGALI_DEFAULT -> "BENGALI_DEFAULT"
            LanguageMode.SOURCE_LANGUAGE -> {
                if (text.any { it in 'a'..'z' || it in 'A'..'Z' }) "ENGLISH" else "SOURCE_LANGUAGE"
            }
            null -> {
                val hasBangla = text.any { it.code in 0x0980..0x09FF }
                if (!hasBangla && text.any { it.isLetter() }) "ENGLISH" else "BENGALI_DEFAULT"
            }
        }
    }
}
