package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage19TouchResponsivenessTest {
    @Test
    fun rapid_action_gate_suppresses_only_same_non_text_action_window() {
        val gate = RapidActionGate(defaultMinIntervalMs = 100L)
        assertTrue(gate.allow("toolbar:language", 1_000L))
        assertFalse(gate.allow("toolbar:language", 1_050L))
        assertTrue(gate.allow("toolbar:emoji", 1_051L))
        assertTrue(gate.allow("toolbar:language", 1_100L))
    }

    @Test
    fun frame_work_coalescer_posts_once_until_consumed() {
        val coalescer = FrameWorkCoalescer()
        assertTrue(coalescer.request())
        assertFalse(coalescer.request())
        assertTrue(coalescer.consume())
        assertTrue(coalescer.request())
        coalescer.cancel()
        assertFalse(coalescer.isPending())
    }

    @Test
    fun device_touch_slop_can_raise_glide_threshold() {
        assertTrue(TouchResponsivenessPolicy.glideStartThresholdPx(18, 8) == 18)
        assertTrue(TouchResponsivenessPolicy.glideStartThresholdPx(10, 12) == 18)
    }
}
