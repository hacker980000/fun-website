package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BubbleFlightTargetResolverTest {
    private val resolver = BubbleFlightTargetResolver(exactMaxAgeMs = 600L, fallbackMaxAgeMs = 900L)
    private val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)

    @Test fun exactTargetWinsWhenFreshAndMatching() {
        val exact = BubbleFlightTarget(BubbleFlightPoint(300f, 200f), editor, 1_000L, BubbleFlightTargetSource.CURSOR_ANCHOR)
        val fallback = BubbleFlightTarget(BubbleFlightPoint(250f, 210f), editor, 1_050L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS)
        assertEquals(exact, resolver.resolve(editor, exact, fallback, nowUptimeMs = 1_200L))
    }

    @Test fun fallbackIsUsedWhenExactIsStale() {
        val exact = BubbleFlightTarget(BubbleFlightPoint(300f, 200f), editor, 100L, BubbleFlightTargetSource.CURSOR_ANCHOR)
        val fallback = BubbleFlightTarget(BubbleFlightPoint(250f, 210f), editor, 900L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS)
        assertEquals(fallback, resolver.resolve(editor, exact, fallback, nowUptimeMs = 1_100L))
    }

    @Test fun mismatchedGenerationIsRejected() {
        val stale = BubbleFlightTarget(BubbleFlightPoint(300f, 200f), editor.copy(generation = 2L), 1_000L, BubbleFlightTargetSource.CURSOR_ANCHOR)
        assertNull(resolver.resolve(editor, stale, null, nowUptimeMs = 1_100L))
    }

    @Test fun noTrustworthyTargetReturnsNull() {
        assertNull(resolver.resolve(editor, null, null, nowUptimeMs = 1_100L))
    }
}
