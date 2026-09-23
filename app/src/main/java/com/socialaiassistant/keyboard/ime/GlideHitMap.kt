package com.socialaiassistant.keyboard.ime

import kotlin.math.max

data class GlideHitBox(
    val key: Char,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun contains(x: Float, y: Float): Boolean = x >= left && x <= right && y >= top && y <= bottom

    fun distanceSquaredTo(x: Float, y: Float): Float {
        val dx = when {
            x < left -> left - x
            x > right -> x - right
            else -> 0f
        }
        val dy = when {
            y < top -> top - y
            y > bottom -> y - bottom
            else -> 0f
        }
        return dx * dx + dy * dy
    }
}

/**
 * Immutable per-gesture key geometry snapshot. Avoids View location lookups on every MOVE event.
 * Stage 19 adds a small nearest-key tolerance so visual key gaps do not create dead Glide zones.
 */
class GlideHitMap(private val boxes: List<GlideHitBox>) {
    fun keyAt(x: Float, y: Float, tolerancePx: Float = 0f): Char? {
        boxes.firstOrNull { it.contains(x, y) }?.let { return it.key }
        val tolerance = max(0f, tolerancePx)
        if (tolerance <= 0f) return null
        val maxDistanceSquared = tolerance * tolerance
        var best: GlideHitBox? = null
        var bestDistance = Float.POSITIVE_INFINITY
        for (box in boxes) {
            val distance = box.distanceSquaredTo(x, y)
            if (distance <= maxDistanceSquared && distance < bestDistance) {
                best = box
                bestDistance = distance
            }
        }
        return best?.key
    }

    fun size(): Int = boxes.size

    companion object {
        val EMPTY = GlideHitMap(emptyList())
    }
}
