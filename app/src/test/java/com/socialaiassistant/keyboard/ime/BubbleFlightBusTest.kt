package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.theme.KeyboardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleFlightBusTest {
    @Test fun dispatchReturnsFalseWithoutRegisteredSink() {
        BubbleFlightBus.clearForTest()
        assertFalse(BubbleFlightBus.dispatch(request()))
    }

    @Test fun registeredSinkReceivesRequestAndCanBeUnregistered() {
        BubbleFlightBus.clearForTest()
        val sink = RecordingSink(accept = true)
        BubbleFlightBus.register(sink)
        assertTrue(BubbleFlightBus.dispatch(request()))
        assertEquals(1, sink.requests.size)
        BubbleFlightBus.unregister(sink)
        assertFalse(BubbleFlightBus.dispatch(request().copy(id = 2L)))
    }

    @Test fun retargetIsForwardedOnlyToCurrentSink() {
        BubbleFlightBus.clearForTest()
        val sink = RecordingSink(accept = true)
        BubbleFlightBus.register(sink)
        val target = exactTarget()
        assertTrue(BubbleFlightBus.retarget(1L, target))
        assertEquals(listOf(1L to target), sink.retargets)
        BubbleFlightBus.unregister(sink)
    }

    private fun request(): BubbleFlightRequest {
        val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)
        return BubbleFlightRequest(
            id = 1L,
            label = "a",
            source = BubbleFlightPoint(100f, 900f),
            editor = editor,
            exactTarget = exactTarget(),
            spec = BubbleKeyAnimationSpec("a", 470L, 68f, 11f, 32f, 82f, 0.52f),
            theme = sampleTheme(),
            createdAtUptimeMs = 1_000L
        )
    }

    private fun exactTarget(): BubbleFlightTarget = BubbleFlightTarget(
        BubbleFlightPoint(300f, 200f),
        BubbleFlightEditorToken("org.example.chat", 7, 3L),
        1_000L,
        BubbleFlightTargetSource.CURSOR_ANCHOR
    )

    private class RecordingSink(private val accept: Boolean) : BubbleFlightSink {
        val requests = mutableListOf<BubbleFlightRequest>()
        val retargets = mutableListOf<Pair<Long, BubbleFlightTarget>>()
        override fun submit(request: BubbleFlightRequest): Boolean { requests += request; return accept }
        override fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean { retargets += flightId to target; return accept }
        override fun cancelEditor(editor: BubbleFlightEditorToken) = Unit
        override fun cancelAll() = Unit
    }

    private fun sampleTheme() = KeyboardTheme(
        id = "test", displayName = "Test", isBuiltIn = true,
        rootBackground = 0, panelSurface = 0, toolbarSurface = 0, keySurface = 0,
        specialKeySurface = 0, disabledSurface = 0, textPrimary = 0, textSecondary = 0,
        keyLabel = 0, specialKeyLabel = 0, disabledLabel = 0, primaryNeon = 0,
        secondaryNeon = 0, aiNeon = 0, actionAccent = 0, dangerAccent = 0
    )
}
