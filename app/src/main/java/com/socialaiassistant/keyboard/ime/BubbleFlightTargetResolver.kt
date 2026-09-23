package com.socialaiassistant.keyboard.ime

class BubbleFlightTargetResolver(
    private val exactMaxAgeMs: Long = 600L,
    private val fallbackMaxAgeMs: Long = 900L
) {
    fun resolve(
        editor: BubbleFlightEditorToken,
        exact: BubbleFlightTarget?,
        fallback: BubbleFlightTarget?,
        nowUptimeMs: Long
    ): BubbleFlightTarget? {
        return exact?.takeIf { valid(it, editor, nowUptimeMs, exactMaxAgeMs) }
            ?: fallback?.takeIf { valid(it, editor, nowUptimeMs, fallbackMaxAgeMs) }
    }

    private fun valid(
        target: BubbleFlightTarget,
        editor: BubbleFlightEditorToken,
        nowUptimeMs: Long,
        maxAgeMs: Long
    ): Boolean {
        if (target.editor != editor || !target.point.isFinite()) return false
        val age = nowUptimeMs - target.capturedAtUptimeMs
        return age in 0L..maxAgeMs
    }
}
