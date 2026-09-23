package com.socialaiassistant.keyboard.ai

enum class ManualAiAction {
    SMART,
    WITTY,
    FLIRTY,
    FUNNY,
    REWRITE,
    TRANSLATE,
    GRAMMAR_FIX;

    val isDraftAction: Boolean
        get() = this == REWRITE || this == TRANSLATE || this == GRAMMAR_FIX
}

data class ManualAiRequest(
    val action: ManualAiAction,
    val interactionType: InteractionType = InteractionType.INBOX,
    val conversationIntent: ConversationAiIntent? = null,
    val messages: List<PromptMessage> = emptyList(),
    val latestRecipientMessage: String? = null,
    val cachedLanguageMode: LanguageMode? = null,
    val customKnowledge: String = "",
    val customInstruction: String = "",
    val tonePreset: TonePreset = TonePreset.AUTO,
    val conversationMemory: String = "",
    val postText: String = "",
    val draftText: String = "",
    val translateTargetLanguage: String = "English"
)
