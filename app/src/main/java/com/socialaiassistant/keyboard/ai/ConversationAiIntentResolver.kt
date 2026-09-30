package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.ConversationSurface
import com.socialaiassistant.keyboard.context.SenderClass

enum class ConversationAiIntent {
    REPLY,
    CONTINUE,
    START,
    NEEDS_CONTEXT
}

class ConversationAiIntentResolver {
    fun resolve(snapshot: ContextSnapshot): ConversationAiIntent {
        val latest = snapshot.messages.asReversed().firstOrNull { it.text.isNotBlank() }
        if (latest != null) {
            return when (latest.sender) {
                SenderClass.SELF -> ConversationAiIntent.CONTINUE
                SenderClass.RECIPIENT -> ConversationAiIntent.REPLY
                SenderClass.UNKNOWN -> ConversationAiIntent.NEEDS_CONTEXT
            }
        }

        // An explicit latestRecipientMessage may exist even when the bounded row list is empty.
        if (latestExplicitRecipient(snapshot) != null || !snapshot.latestRecipientMessage.isNullOrBlank()) {
            return ConversationAiIntent.REPLY
        }

        return if (snapshot.surface == ConversationSurface.INBOX || !snapshot.conversationHint.isNullOrBlank()) {
            ConversationAiIntent.START
        } else {
            ConversationAiIntent.NEEDS_CONTEXT
        }
    }

    fun refineIntentWithContext(
        snapshot: ContextSnapshot,
        defaultIntent: ConversationAiIntent
    ): ConversationAiIntent {
        val latest = snapshot.messages.asReversed().firstOrNull { it.text.isNotBlank() }
        return when (latest?.sender) {
            SenderClass.SELF -> ConversationAiIntent.CONTINUE
            SenderClass.RECIPIENT -> ConversationAiIntent.REPLY
            SenderClass.UNKNOWN -> ConversationAiIntent.NEEDS_CONTEXT
            null -> if (!snapshot.latestRecipientMessage.isNullOrBlank()) {
                ConversationAiIntent.REPLY
            } else {
                defaultIntent
            }
        }
    }
    private fun latestExplicitRecipient(snapshot: ContextSnapshot) =
        snapshot.messages.asReversed().firstOrNull { message ->
            message.text.isNotBlank() && message.sender == SenderClass.RECIPIENT
        }

}
