package com.socialaiassistant.keyboard.context

import android.graphics.Rect
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleAccessibilityTargetMapperTest {
    @Test fun trailingPointStaysInsideLtrAndRtlBounds() {
        val bounds = Rect(100, 200, 400, 280)
        assertEquals(BubbleFlightPoint(384f, 240f), BubbleAccessibilityTargetMapper.pointInside(bounds, false, 16f))
        assertEquals(BubbleFlightPoint(116f, 240f), BubbleAccessibilityTargetMapper.pointInside(bounds, true, 16f))
    }

    @Test fun invalidBoundsReturnNull() {
        assertNull(BubbleAccessibilityTargetMapper.pointInside(Rect(0, 0, 0, 0), false, 16f))
    }

    @Test fun narrowFieldTargetRemainsInside() {
        val bounds = Rect(100, 200, 120, 280)
        val point = BubbleAccessibilityTargetMapper.pointInside(bounds, false, 16f)!!
        assertTrue(point.x in 100f..120f)
    }
}
