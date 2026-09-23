package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.SenderClass

enum class InteractionType {
    INBOX,
    COMMENT
}

enum class LanguageMode {
    BENGALI,
    BANGLISH,
    LATIN_INFER,
    SOURCE_LANGUAGE,
    BENGALI_DEFAULT
}

data class PromptMessage(
    val sender: SenderClass,
    val text: String,
    val timestampHint: String? = null
)

data class PromptRequest(
    val type: InteractionType = InteractionType.INBOX,
    val mode: AiMode = AiMode.GENERAL,
    val conversationIntent: ConversationAiIntent? = null,
    val messages: List<PromptMessage> = emptyList(),
    val latestRecipientMessage: String? = null,
    val cachedLanguageMode: LanguageMode? = null,
    val customKnowledge: String = "",
    val customInstruction: String = "",
    val tonePreset: TonePreset = TonePreset.AUTO,
    val conversationMemory: String = "",
    val postText: String = "",
    val createConversationMemory: Boolean = false
)

data class PromptBundle(
    val system: String,
    val user: String
)

data class ParsedAiResult(
    val reply: String,
    val category: String? = null,
    val confidence: Double = 0.5,
    val conversationMemory: String? = null
)
