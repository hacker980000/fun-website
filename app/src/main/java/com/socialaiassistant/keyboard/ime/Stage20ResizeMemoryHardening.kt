package com.socialaiassistant.keyboard.ime

/**
 * Android-free Stage 20 helpers for keeping resize/startup UI work bounded.
 * All state is process-memory only and contains no typed text.
 */
data class ToolbarOrderSignature(
    val profile: String,
    val orderedIds: List<Int>
)

class ToolbarOrderGate {
    private var last: ToolbarOrderSignature? = null

    fun shouldReorder(signature: ToolbarOrderSignature, currentIds: List<Int>): Boolean {
        if (last != signature || currentIds != signature.orderedIds) {
            last = signature
            return true
        }
        return false
    }

    fun invalidate() {
        last = null
    }
}

data class BackgroundContentSignature(
    val enabled: Boolean,
    val localFileName: String,
    val scope: String,
    val fit: String,
    val opacityPercent: Int,
    val darkOverlayPercent: Int,
    val blurAmount: Int
)

data class BackgroundRenderSignature(
    val content: BackgroundContentSignature,
    val targetWidthPx: Int,
    val targetHeightPx: Int
)

enum class BackgroundRenderDecision {
    SKIP,
    CLEAR,
    LOAD_CLEAR,
    LOAD_KEEP_VISIBLE
}

class BackgroundRenderGate {
    private var last: BackgroundRenderSignature? = null

    fun decide(request: BackgroundRenderSignature): BackgroundRenderDecision {
        val previous = last
        if (previous == request) return BackgroundRenderDecision.SKIP
        last = request
        if (!request.content.enabled) return BackgroundRenderDecision.CLEAR
        return if (previous != null && previous.content == request.content) {
            BackgroundRenderDecision.LOAD_KEEP_VISIBLE
        } else {
            BackgroundRenderDecision.LOAD_CLEAR
        }
    }

    fun invalidate() {
        last = null
    }
}

data class BackgroundDecodeTarget(val widthPx: Int, val heightPx: Int)

object BackgroundDecodePolicy {
    private const val DEFAULT_BUCKET_PX = 64
    private const val LOW_RAM_MAX_WIDTH = 1024
    private const val LOW_RAM_MAX_HEIGHT = 640
    private const val NORMAL_MAX_WIDTH = 1536
    private const val NORMAL_MAX_HEIGHT = 896

    fun target(
        requestedWidthPx: Int,
        requestedHeightPx: Int,
        lowRamDevice: Boolean,
        bucketPx: Int = DEFAULT_BUCKET_PX
    ): BackgroundDecodeTarget {
        val bucket = bucketPx.coerceAtLeast(1)
        val maxWidth = if (lowRamDevice) LOW_RAM_MAX_WIDTH else NORMAL_MAX_WIDTH
        val maxHeight = if (lowRamDevice) LOW_RAM_MAX_HEIGHT else NORMAL_MAX_HEIGHT
        val width = bucketUp(requestedWidthPx.coerceAtLeast(1), bucket).coerceAtMost(maxWidth)
        val height = bucketUp(requestedHeightPx.coerceAtLeast(1), bucket).coerceAtMost(maxHeight)
        return BackgroundDecodeTarget(width, height)
    }

    private fun bucketUp(value: Int, bucket: Int): Int =
        (((value.toLong() + bucket - 1L) / bucket.toLong()) * bucket.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}

data class KeyboardLayoutRequest(
    val mode: KeyboardUiMode,
    val showNumberRow: Boolean,
    val quickKeys: List<String>
)

/** Small access-ordered cache for immutable layout models. */
class BoundedValueCache<K, V>(private val maxEntries: Int) {
    private val values = LinkedHashMap<K, V>(maxEntries.coerceAtLeast(1), 0.75f, true)

    fun getOrPut(key: K, create: () -> V): V {
        values[key]?.let { return it }
        val value = create()
        values[key] = value
        trim()
        return value
    }

    fun clear() {
        values.clear()
    }

    fun size(): Int = values.size

    private fun trim() {
        val max = maxEntries.coerceAtLeast(1)
        while (values.size > max) {
            val first = values.entries.iterator()
            if (!first.hasNext()) return
            first.next()
            first.remove()
        }
    }
}
