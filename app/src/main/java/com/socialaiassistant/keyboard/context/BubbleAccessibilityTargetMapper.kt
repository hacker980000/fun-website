package com.socialaiassistant.keyboard.context

import android.graphics.Rect
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint

object BubbleAccessibilityTargetMapper {
    fun pointInside(bounds: Rect, rtl: Boolean, trailingInsetPx: Float): BubbleFlightPoint? {
        if (bounds.width() <= 0 || bounds.height() <= 0) return null
        val inset = trailingInsetPx.coerceIn(1f, (bounds.width() / 2f).coerceAtLeast(1f))
        val x = if (rtl) bounds.left + inset else bounds.right - inset
        val y = bounds.exactCenterY()
        return BubbleFlightPoint(x, y)
    }
}
