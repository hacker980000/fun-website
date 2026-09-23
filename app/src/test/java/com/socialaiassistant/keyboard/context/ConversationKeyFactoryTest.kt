package com.socialaiassistant.keyboard.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationKeyFactoryTest {
    private val factory = ConversationKeyFactory()

    @Test
    fun same_package_and_conversation_hint_produce_stable_key() {
        val first = snapshot("com.chat", "window-1", "Mitu", "hello", 1_000L)
        val second = snapshot("com.chat", "window-2", "Mitu", "different raw text", 2_000L)
        assertEquals(factory.create(first), factory.create(second))
        assertTrue(factory.hasStableIdentity(first))
    }

    @Test
    fun different_conversation_hints_produce_distinct_keys() {
        assertNotEquals(
            factory.create(snapshot("com.chat", "window", "Mitu", "hello", 1_000L)),
            factory.create(snapshot("com.chat", "window", "Sarah", "hello", 1_000L))
        )
    }

    @Test
    fun missing_hint_is_capture_scoped_instead_of_window_persistent() {
        val first = snapshot("com.chat", "thread-window-7", null, "secret text", 1_000L)
        val later = snapshot("com.chat", "thread-window-7", null, "other text", 2_000L)
        assertFalse(factory.hasStableIdentity(first))
        assertNotEquals(factory.create(first), factory.create(later))
        assertFalse(factory.create(first).value.contains("secret text"))
    }

    private fun snapshot(
        pkg: String,
        window: String,
        hint: String?,
        text: String,
        capturedAt: Long
    ) = ContextSnapshot(
        packageName = pkg,
        windowSignature = window,
        conversationHint = hint,
        messages = listOf(ContextMessage(SenderClass.RECIPIENT, text)),
        latestRecipientMessage = text,
        composerHint = "Message",
        confidence = 1f,
        capturedAtMillis = capturedAt
    )
}
