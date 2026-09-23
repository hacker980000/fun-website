package com.socialaiassistant.keyboard.context

import com.socialaiassistant.keyboard.ime.BubbleFlightPoint

object BubbleFlightPath {
    fun pointAt(
        source: BubbleFlightPoint,
        target: BubbleFlightPoint,
        curvePx: Float,
        t: Float
    ): BubbleFlightPoint {
        val clamped = t.coerceIn(0f, 1f)
        val oneMinus = 1f - clamped
        val controlX = (source.x + target.x) / 2f
        val controlY = minOf(source.y, target.y) - curvePx.coerceAtLeast(0f)
        val x = oneMinus * oneMinus * source.x +
            2f * oneMinus * clamped * controlX +
            clamped * clamped * target.x
        val y = oneMinus * oneMinus * source.y +
            2f * oneMinus * clamped * controlY +
            clamped * clamped * target.y
        return BubbleFlightPoint(x, y)
    }
}
