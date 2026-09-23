package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.theme.KeyboardTheme

data class BubbleFlightPoint(val x: Float, val y: Float) {
    fun isFinite(): Boolean = x.isFinite() && y.isFinite()
}

data class BubbleFlightEditorToken(
    val packageName: String?,
    val fieldId: Int,
    val generation: Long
)

enum class BubbleFlightTargetSource {
    CURSOR_ANCHOR,
    ACCESSIBILITY_BOUNDS
}

data class BubbleFlightTarget(
    val point: BubbleFlightPoint,
    val editor: BubbleFlightEditorToken,
    val capturedAtUptimeMs: Long,
    val source: BubbleFlightTargetSource
)

data class BubbleFlightRequest(
    val id: Long,
    val label: String,
    val source: BubbleFlightPoint,
    val editor: BubbleFlightEditorToken,
    val exactTarget: BubbleFlightTarget?,
    val spec: BubbleKeyAnimationSpec,
    val theme: KeyboardTheme,
    val createdAtUptimeMs: Long
)

data class PreparedBubbleFlight(
    val request: BubbleFlightRequest
)
