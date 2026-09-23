package com.socialaiassistant.keyboard.ime

import android.view.inputmethod.CursorAnchorInfo

object BubbleCursorAnchorMapper {
    fun map(info: CursorAnchorInfo): BubbleFlightPoint? {
        val x = info.insertionMarkerHorizontal
        val top = info.insertionMarkerTop
        val bottom = info.insertionMarkerBottom
        val baseline = info.insertionMarkerBaseline
        if (!x.isFinite()) return null

        val y = when {
            top.isFinite() && bottom.isFinite() -> (top + bottom) / 2f
            baseline.isFinite() -> baseline
            else -> return null
        }
        val points = floatArrayOf(x, y)
        info.matrix.mapPoints(points)
        return BubbleFlightPoint(points[0], points[1]).takeIf(BubbleFlightPoint::isFinite)
    }
}
