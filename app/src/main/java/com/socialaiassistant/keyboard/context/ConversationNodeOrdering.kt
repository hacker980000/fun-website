package com.socialaiassistant.keyboard.context

/**
 * Produces a stable visual top-to-bottom order when Accessibility tree traversal order
 * differs from screen order. Falls back to original traversal order when bounds are absent.
 */
object ConversationNodeOrdering {
    fun chronological(nodes: List<VisibleTextNode>): List<VisibleTextNode> {
        if (nodes.size <= 1) return nodes

        val ordered = if (nodes.all { it.hasScreenBounds }) {
            nodes.sortedWith { first, second -> compareGeometry(first, second) }
        } else {
            // A mixed geometry/traversal comparator can be non-transitive. If even one eligible
            // row has no usable bounds, preserve deterministic Accessibility traversal order.
            nodes.sortedBy { it.order }
        }

        return dedupeMirroredAccessibilityRows(ordered)
    }

    private fun compareGeometry(first: VisibleTextNode, second: VisibleTextNode): Int {
        val topDiff = (first.screenTop ?: 0) - (second.screenTop ?: 0)
        if (kotlin.math.abs(topDiff) > SAME_ROW_TOLERANCE_PX) return topDiff

        val bottomDiff = (first.screenBottom ?: 0) - (second.screenBottom ?: 0)
        if (kotlin.math.abs(bottomDiff) > SAME_ROW_TOLERANCE_PX) return bottomDiff
        return first.order.compareTo(second.order)
    }

    private fun dedupeMirroredAccessibilityRows(nodes: List<VisibleTextNode>): List<VisibleTextNode> {
        val result = ArrayList<VisibleTextNode>(nodes.size)
        for (node in nodes) {
            val duplicate = result.lastOrNull()?.let { previous ->
                sameRenderedRow(previous, node)
            } == true
            if (!duplicate) result += node
        }
        return result
    }

    private fun sameRenderedRow(first: VisibleTextNode, second: VisibleTextNode): Boolean {
        val firstText = normalized(first.text)
        val secondText = normalized(second.text)
        if (firstText.isEmpty() || firstText != secondText) return false
        if (first.senderHint != second.senderHint) return false
        if (!first.hasScreenBounds || !second.hasScreenBounds) return false

        return close(first.screenLeft, second.screenLeft) &&
            close(first.screenTop, second.screenTop) &&
            close(first.screenRight, second.screenRight) &&
            close(first.screenBottom, second.screenBottom)
    }

    private fun normalized(value: String?): String = value.orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun close(first: Int?, second: Int?): Boolean {
        if (first == null || second == null) return false
        return kotlin.math.abs(first - second) <= DUPLICATE_BOUNDS_TOLERANCE_PX
    }

    private const val SAME_ROW_TOLERANCE_PX = 4
    private const val DUPLICATE_BOUNDS_TOLERANCE_PX = 3
}
