package com.socialaiassistant.keyboard.context

import com.socialaiassistant.keyboard.ime.BubbleFlightPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleFlightPathTest {
    private val source = BubbleFlightPoint(100f, 900f)
    private val target = BubbleFlightPoint(300f, 200f)

    @Test fun endpointsAreExact() {
        assertEquals(source, BubbleFlightPath.pointAt(source, target, 80f, 0f))
        assertEquals(target, BubbleFlightPath.pointAt(source, target, 80f, 1f))
    }

    @Test fun midpointArcsAboveStraightLine() {
        val mid = BubbleFlightPath.pointAt(source, target, 80f, 0.5f)
        assertTrue(mid.y < (source.y + target.y) / 2f)
    }

    @Test fun shortPathRemainsFinite() {
        val point = BubbleFlightPath.pointAt(source, BubbleFlightPoint(101f, 899f), 118f, 0.5f)
        assertTrue(point.isFinite())
    }
}
