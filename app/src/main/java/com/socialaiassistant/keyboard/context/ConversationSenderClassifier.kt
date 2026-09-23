package com.socialaiassistant.keyboard.context

/**
 * Conservative sender inference for visible chat rows.
 *
 * Resource/class semantics win over geometry. Accessibility content descriptions are only
 * considered when they contain explicit sender-role phrases; arbitrary message text is never
 * keyword-scanned for sender classification. Geometry is used only for relatively narrow rows
 * that are clearly aligned to one side of the screen. Centered/full-width rows remain UNKNOWN.
 */
object ConversationSenderClassifier {
    fun infer(node: VisibleTextNode, screenWidth: Int, isRtlLayout: Boolean): SenderClass {
        val semantic = semanticSender(node)
        if (semantic != SenderClass.UNKNOWN) return semantic
        if (!node.hasScreenBounds || screenWidth <= 0) return SenderClass.UNKNOWN

        val left = node.screenLeft ?: return SenderClass.UNKNOWN
        val right = node.screenRight ?: return SenderClass.UNKNOWN
        val width = (right - left).coerceAtLeast(0)
        if (width == 0) return SenderClass.UNKNOWN

        val widthFraction = width.toFloat() / screenWidth.toFloat()
        if (widthFraction >= MAX_BUBBLE_WIDTH_FRACTION) return SenderClass.UNKNOWN

        val centerFraction = ((left + right) / 2f) / screenWidth.toFloat()
        val leading = centerFraction <= LEADING_CENTER_MAX
        val trailing = centerFraction >= TRAILING_CENTER_MIN
        if (!leading && !trailing) return SenderClass.UNKNOWN

        return when {
            isRtlLayout && leading -> SenderClass.SELF
            isRtlLayout && trailing -> SenderClass.RECIPIENT
            !isRtlLayout && leading -> SenderClass.RECIPIENT
            !isRtlLayout && trailing -> SenderClass.SELF
            else -> SenderClass.UNKNOWN
        }
    }

    private fun semanticSender(node: VisibleTextNode): SenderClass {
        val resourceMetadata = normalizeMetadata(
            listOfNotNull(node.viewIdResourceName, node.className).joinToString(" ")
        )
        if (SELF_RESOURCE_MARKERS.any(resourceMetadata::contains)) return SenderClass.SELF
        if (RECIPIENT_RESOURCE_MARKERS.any(resourceMetadata::contains)) return SenderClass.RECIPIENT

        val description = normalizeMetadata(node.contentDescription.orEmpty())
        if (SELF_DESCRIPTION_MARKERS.any(description::contains)) return SenderClass.SELF
        if (RECIPIENT_DESCRIPTION_MARKERS.any(description::contains)) return SenderClass.RECIPIENT
        return SenderClass.UNKNOWN
    }

    private fun normalizeMetadata(value: String): String = value
        .replace(CAMEL_BOUNDARY, "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()

    private val CAMEL_BOUNDARY = Regex("([a-z0-9])([A-Z])")

    private val SELF_RESOURCE_MARKERS = listOf(
        "outgoing", "message out", "msg out", "from me", "my message", "bubble out",
        "sent message", "out message"
    )

    private val RECIPIENT_RESOURCE_MARKERS = listOf(
        "incoming", "message in", "msg in", "from other", "received message", "bubble in",
        "in message"
    )

    private val SELF_DESCRIPTION_MARKERS = listOf(
        "outgoing message", "message sent by you", "sent by you", "your message"
    )

    private val RECIPIENT_DESCRIPTION_MARKERS = listOf(
        "incoming message", "message received from", "received from", "message from"
    )

    private const val MAX_BUBBLE_WIDTH_FRACTION = 0.82f
    private const val LEADING_CENTER_MAX = 0.40f
    private const val TRAILING_CENTER_MIN = 0.60f
}
