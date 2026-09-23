package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.safety.FieldSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExplicitAiTriggerControllerTest {
    @Test fun toolbar_tap_never_requests_generation() {
        var calls = 0
        val controller = ExplicitAiTriggerController { calls++ }
        assertFalse(controller.onToolbarTap(FieldSafety.ALLOW_AI, loading = false))
        assertFalse(controller.onToolbarTap(FieldSafety.ALLOW_AI, loading = true))
        assertFalse(controller.onToolbarTap(FieldSafety.BLOCK_AI, loading = false))
        assertFalse(controller.onToolbarTap(FieldSafety.NO_CONVERSATION, loading = false))
        assertEquals(0, calls)
    }
}
