package com.socialaiassistant.keyboard.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericConversationAdapterTest {
    private val adapter = GenericConversationAdapter(maxMessages = 4, maxTotalChars = 80)

    @Test
    fun keeps_visible_rows_in_order_and_drops_blank_or_control_only_text() {
        val nodes = listOf(
            VisibleTextNode(0, " Hello ", SenderClass.RECIPIENT),
            VisibleTextNode(1, "   ", SenderClass.UNKNOWN),
            VisibleTextNode(2, "⋮", SenderClass.UNKNOWN, control = true),
            VisibleTextNode(3, "Hi there", SenderClass.SELF)
        )

        val snapshot = adapter.fromNodes("com.chat", "window", "Mitu", nodes, "Message")
        assertNotNull(snapshot)
        assertEquals(listOf("Hello", "Hi there"), snapshot!!.messages.map { it.text })
    }

    @Test
    fun caps_message_count_and_total_snapshot_size() {
        val nodes = (0 until 10).map {
            VisibleTextNode(it, "message-$it-" + "x".repeat(30), SenderClass.RECIPIENT)
        }
        val snapshot = adapter.fromNodes("com.chat", "window", "Mitu", nodes, "Message")!!
        assertTrue(snapshot.messages.size <= 4)
        assertTrue(snapshot.messages.sumOf { it.text.length } <= 80)
    }

    @Test
    fun latest_recipient_message_uses_last_confident_recipient_row() {
        val nodes = listOf(
            VisibleTextNode(1, "first", SenderClass.RECIPIENT),
            VisibleTextNode(2, "my reply", SenderClass.SELF),
            VisibleTextNode(3, "latest question", SenderClass.RECIPIENT)
        )
        val snapshot = adapter.fromNodes("com.chat", "window", "Mitu", nodes, "Message")!!
        assertEquals("latest question", snapshot.latestRecipientMessage)
        assertTrue(snapshot.confidence > 0.5f)
    }

    @Test
    fun editable_composer_text_is_not_treated_as_conversation_history() {
        val nodes = listOf(
            VisibleTextNode(1, "old message", SenderClass.RECIPIENT),
            VisibleTextNode(2, "draft", SenderClass.SELF, editable = true)
        )
        val snapshot = adapter.fromNodes("com.chat", "window", null, nodes, "Message")!!
        assertFalse(snapshot.messages.any { it.text == "draft" })
    }
    @Test
    fun visual_top_to_bottom_geometry_overrides_tree_traversal_order() {
        val nodes = listOf(
            VisibleTextNode(0, "newest", SenderClass.RECIPIENT, screenLeft = 20, screenTop = 500, screenRight = 300, screenBottom = 550),
            VisibleTextNode(1, "oldest", SenderClass.RECIPIENT, screenLeft = 20, screenTop = 100, screenRight = 300, screenBottom = 150),
            VisibleTextNode(2, "middle", SenderClass.SELF, screenLeft = 700, screenTop = 300, screenRight = 980, screenBottom = 350)
        )

        val snapshot = adapter.fromNodes("com.chat", "window", "Mitu", nodes, "Message")!!
        assertEquals(listOf("oldest", "middle", "newest"), snapshot.messages.map { it.text })
    }

    @Test
    fun bounded_char_budget_keeps_recent_rows_not_oldest_rows() {
        val tiny = GenericConversationAdapter(maxMessages = 10, maxTotalChars = 12, maxMessageChars = 20)
        val nodes = listOf(
            VisibleTextNode(0, "old-old-old", SenderClass.RECIPIENT),
            VisibleTextNode(1, "recent-one", SenderClass.SELF),
            VisibleTextNode(2, "latest", SenderClass.RECIPIENT)
        )

        val snapshot = tiny.fromNodes("com.chat", "window", "Mitu", nodes, "Message")!!
        assertEquals("latest", snapshot.messages.last().text)
        assertFalse(snapshot.messages.any { it.text == "old-old-old" })
    }

    @Test
    fun mirrored_accessibility_rows_with_same_bounds_are_deduplicated() {
        val duplicateA = VisibleTextNode(0, "same bubble", SenderClass.RECIPIENT, screenLeft = 20, screenTop = 100, screenRight = 300, screenBottom = 150)
        val duplicateB = duplicateA.copy(order = 1, screenLeft = 21, screenTop = 101, screenRight = 301, screenBottom = 151)
        val snapshot = adapter.fromNodes("com.chat", "window", "Mitu", listOf(duplicateA, duplicateB), "Message")!!
        assertEquals(listOf("same bubble"), snapshot.messages.map { it.text })
    }

}
