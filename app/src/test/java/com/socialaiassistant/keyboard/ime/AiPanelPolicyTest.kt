package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.context.ConversationSurface
import org.junit.Assert.assertEquals
import org.junit.Test

class AiPanelPolicyTest {
    @Test
    fun translate_target_follows_keyboard_language() {
        assertEquals("English", AiPanelPolicy.translateTarget(KeyboardLanguage.ENGLISH))
        assertEquals("Bengali", AiPanelPolicy.translateTarget(KeyboardLanguage.BANGLA))
    }

    @Test
    fun social_labels_follow_comment_or_reply_surface() {
        val comment = AiPanelPolicy.socialLabels(ConversationSurface.COMMENT)
        val inbox = AiPanelPolicy.socialLabels(ConversationSurface.INBOX)

        assertEquals("Smart Comment", comment.smart)
        assertEquals("Unique Comment", comment.witty)
        assertEquals("Flirty Comment", comment.flirty)
        assertEquals("Smart Reply", inbox.smart)
        assertEquals("Unique Reply", inbox.witty)
        assertEquals("Flirty Reply", inbox.flirty)
    }
}
