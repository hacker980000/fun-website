package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.ContextMessage
import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.ConversationSurface
import com.socialaiassistant.keyboard.context.SenderClass
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationAiIntentResolverTest {
    private val resolver = ConversationAiIntentResolver()

    @Test fun recipient_last_means_reply() {
        val snapshot = snapshot(
            listOf(
                ContextMessage(SenderClass.SELF, "Hi"),
                ContextMessage(SenderClass.RECIPIENT, "How are you?")
            )
        )
        assertEquals(ConversationAiIntent.REPLY, resolver.resolve(snapshot))
    }

    @Test fun sender_last_means_continue() {
        val snapshot = snapshot(
            listOf(
                ContextMessage(SenderClass.RECIPIENT, "Hello"),
                ContextMessage(SenderClass.SELF, "I will call later")
            )
        )
        assertEquals(ConversationAiIntent.CONTINUE, resolver.resolve(snapshot))
    }

    @Test fun empty_history_with_conversation_hint_means_start() {
        assertEquals(ConversationAiIntent.START, resolver.resolve(snapshot(emptyList(), "Jaan")))
    }

    @Test fun empty_history_without_hint_needs_context() {
        assertEquals(ConversationAiIntent.NEEDS_CONTEXT, resolver.resolve(snapshot(emptyList(), null)))
    }

    @Test fun unknown_only_history_never_becomes_reply() {
        val snapshot = snapshot(listOf(ContextMessage(SenderClass.UNKNOWN, "something")), null)
        assertEquals(ConversationAiIntent.NEEDS_CONTEXT, resolver.resolve(snapshot))
    }

    private fun snapshot(messages: List<ContextMessage>, hint: String? = "Alice") = ContextSnapshot(
        packageName = "org.example.chat",
        windowSignature = "window-1",
        conversationHint = hint,
        messages = messages,
        latestRecipientMessage = messages.lastOrNull { it.sender == SenderClass.RECIPIENT }?.text,
        composerHint = "Message",
        surface = ConversationSurface.INBOX,
        confidence = 0.9f,
        capturedAtMillis = 1_000L
    )
}
