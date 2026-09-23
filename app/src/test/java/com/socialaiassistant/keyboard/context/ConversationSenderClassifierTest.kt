package com.socialaiassistant.keyboard.context

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationSenderClassifierTest {
    @Test fun semantic_outgoing_marker_wins_over_geometry() {
        val node = node(left = 20, right = 300, id = "com.chat:id/outgoing_message_text")
        assertEquals(SenderClass.SELF, ConversationSenderClassifier.infer(node, 1000, false))
    }

    @Test fun semantic_incoming_marker_wins_over_geometry() {
        val node = node(left = 700, right = 980, id = "com.chat:id/incoming_message_text")
        assertEquals(SenderClass.RECIPIENT, ConversationSenderClassifier.infer(node, 1000, false))
    }

    @Test fun ltr_edge_geometry_classifies_only_clear_bubbles() {
        assertEquals(SenderClass.RECIPIENT, ConversationSenderClassifier.infer(node(20, 300), 1000, false))
        assertEquals(SenderClass.SELF, ConversationSenderClassifier.infer(node(700, 980), 1000, false))
        assertEquals(SenderClass.UNKNOWN, ConversationSenderClassifier.infer(node(350, 650), 1000, false))
    }

    @Test fun full_width_system_row_is_unknown() {
        assertEquals(SenderClass.UNKNOWN, ConversationSenderClassifier.infer(node(50, 950), 1000, false))
    }

    @Test fun rtl_layout_mirrors_leading_and_trailing_roles() {
        assertEquals(SenderClass.SELF, ConversationSenderClassifier.infer(node(20, 300), 1000, true))
        assertEquals(SenderClass.RECIPIENT, ConversationSenderClassifier.infer(node(700, 980), 1000, true))
    }

    private fun node(left: Int, right: Int, id: String? = null) = VisibleTextNode(
        order = 0,
        text = "hello",
        viewIdResourceName = id,
        screenLeft = left,
        screenTop = 100,
        screenRight = right,
        screenBottom = 150
    )
}
