package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity

data class BubbleKeyRequest(
    val enabled: Boolean,
    val label: String,
    val sensitiveField: Boolean,
    val glideGesture: Boolean,
    val animationsEnabled: Boolean,
    val letterLayer: Boolean,
    val intensity: BubbleKeyIntensity
)

data class BubbleKeyAnimationSpec(
    val label: String,
    val durationMs: Long,
    val riseDp: Float,
    val driftDp: Float,
    val bubbleSizeDp: Float,
    val flightCurveDp: Float,
    val flightEndScale: Float
)

object BubbleKeyPolicy {
    const val MAX_SIMULTANEOUS_BUBBLES = 8

    fun resolve(request: BubbleKeyRequest): BubbleKeyAnimationSpec? {
        if (!request.enabled || request.sensitiveField || request.glideGesture ||
            !request.animationsEnabled || !request.letterLayer ||
            !isAlphabeticKeyLabel(request.label)
        ) return null

        val motion = when (request.intensity) {
            BubbleKeyIntensity.SOFT -> Motion(380L, 52f, 7f, 28f, flightCurveDp = 56f, flightEndScale = 0.62f)
            BubbleKeyIntensity.NORMAL -> Motion(470L, 68f, 11f, 32f, flightCurveDp = 82f, flightEndScale = 0.52f)
            BubbleKeyIntensity.PLAYFUL -> Motion(560L, 86f, 16f, 36f, flightCurveDp = 118f, flightEndScale = 0.44f)
        }
        return BubbleKeyAnimationSpec(
            label = request.label,
            durationMs = motion.durationMs,
            riseDp = motion.riseDp,
            driftDp = motion.driftDp,
            bubbleSizeDp = motion.sizeDp,
            flightCurveDp = motion.flightCurveDp,
            flightEndScale = motion.flightEndScale
        )
    }

    fun isAlphabeticKeyLabel(label: String): Boolean {
        if (label.isBlank()) return false
        if (label.length == 1 && label[0].isLetter()) return true
        if (label.length > 4) return false
        return label.all { it.code in BENGALI_BLOCK_START..BENGALI_BLOCK_END }
    }

    private data class Motion(
        val durationMs: Long,
        val riseDp: Float,
        val driftDp: Float,
        val sizeDp: Float,
        val flightCurveDp: Float,
        val flightEndScale: Float
    )

    private const val BENGALI_BLOCK_START = 0x0980
    private const val BENGALI_BLOCK_END = 0x09FF
}
