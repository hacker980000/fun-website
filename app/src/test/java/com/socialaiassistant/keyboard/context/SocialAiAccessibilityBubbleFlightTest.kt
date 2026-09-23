package com.socialaiassistant.keyboard.context

import com.socialaiassistant.keyboard.ime.BubbleFlightEditorToken
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint
import com.socialaiassistant.keyboard.ime.BubbleFlightTarget
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetResolver
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialAiAccessibilityBubbleFlightTest {
    @Test fun resolverPrefersFreshCursorOverAccessibilityFallback() {
        val editor = BubbleFlightEditorToken("org.example.chat", 1, 9L)
        val exact = BubbleFlightTarget(BubbleFlightPoint(300f, 100f), editor, 1_000L, BubbleFlightTargetSource.CURSOR_ANCHOR)
        val fallback = BubbleFlightTarget(BubbleFlightPoint(280f, 120f), editor, 1_050L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS)
        assertEquals(exact, BubbleFlightTargetResolver().resolve(editor, exact, fallback, 1_100L))
    }

    @Test fun mismatchedFallbackIsRejected() {
        val editor = BubbleFlightEditorToken("org.example.chat", 1, 9L)
        val fallback = BubbleFlightTarget(BubbleFlightPoint(280f, 120f), editor.copy(generation = 8L), 1_050L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS)
        assertNull(BubbleFlightTargetResolver().resolve(editor, null, fallback, 1_100L))
    }
}
