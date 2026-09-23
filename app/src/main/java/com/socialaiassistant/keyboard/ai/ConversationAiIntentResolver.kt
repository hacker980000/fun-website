package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.SenderClass

enum class ConversationAiIntent {
    REPLY,
    CONTINUE,
    START,
    NEEDS_CONTEXT
}

class ConversationAiIntentResolver {
    fun resolve(snapshot: ContextSnapshot): ConversationAiIntent {
        val latest = snapshot.messages.asReversed().firstOrNull { message ->
            message.text.isNotBlank() && message.sender != SenderClass.UNKNOWN
        }
        return when (latest?.sender) {
            SenderClass.RECIPIENT -> ConversationAiIntent.REPLY
            SenderClass.SELF -> ConversationAiIntent.CONTINUE
            else -> if (!snapshot.conversationHint.isNullOrBlank()) {
                ConversationAiIntent.START
            } else {
                ConversationAiIntent.NEEDS_CONTEXT
            }
        }
    }
}
