package com.socialaiassistant.keyboard.context

class GenericConversationAdapter(
    private val maxMessages: Int = 40,
    private val maxTotalChars: Int = 12_000,
    private val maxMessageChars: Int = 1_200
) {
    fun fromNodes(
        packageName: String,
        windowSignature: String,
        conversationHint: String?,
        nodes: List<VisibleTextNode>,
        composerHint: String?,
        surface: ConversationSurface = ConversationSurface.GENERAL,
        confidenceBoost: Float = 0f,
        capturedAtMillis: Long = System.currentTimeMillis()
    ): ContextSnapshot? {
        if (packageName.isBlank()) return null

        val eligibleNodes = nodes.filterNot { node ->
            node.editable || node.control || ConversationHintResolver.isIdentityNode(node)
        }.filter { node ->
            val text = normalize(node.text)
            text.isNotEmpty() && !isControlOnly(text)
        }

        val candidates = ConversationNodeOrdering.chronological(eligibleNodes)
            .asSequence()
            .mapNotNull { node ->
                val text = normalize(node.text)
                if (text.isEmpty() || isControlOnly(text)) {
                    null
                } else {
                    Candidate(node, text.take(maxMessageChars))
                }
            }
            .filter { it.text.isNotEmpty() }
            .toList()

        if (candidates.isEmpty()) return null

        // Accessibility can expose a long scroll window. Spend the bounded snapshot budget on
        // the most recent visible rows, then restore chronological order for the prompt.
        var remainingChars = maxTotalChars
        val newestFirst = ArrayList<ContextMessage>(minOf(candidates.size, maxMessages))
        for (candidate in candidates.asReversed()) {
            if (newestFirst.size >= maxMessages || remainingChars <= 0) break
            val bounded = candidate.text.take(remainingChars)
            if (bounded.isEmpty()) continue
            newestFirst += ContextMessage(
                sender = candidate.node.senderHint,
                text = bounded,
                timestampHint = candidate.node.timestampHint
            )
            remainingChars -= bounded.length
        }
        val normalized = newestFirst.asReversed()
        if (normalized.isEmpty()) return null

        val confident = normalized.count { it.sender != SenderClass.UNKNOWN }
        val baseConfidence = confident.toFloat() / normalized.size.toFloat()
        val confidence = (baseConfidence + confidenceBoost.coerceIn(0f, 0.5f)).coerceIn(0f, 1f)
        val latestRecipient = normalized.lastOrNull { it.sender == SenderClass.RECIPIENT }?.text

        return ContextSnapshot(
            packageName = packageName,
            windowSignature = windowSignature.take(500),
            conversationHint = normalize(conversationHint).ifEmpty { null },
            messages = normalized,
            latestRecipientMessage = latestRecipient,
            composerHint = normalize(composerHint).ifEmpty { null },
            surface = surface,
            confidence = confidence,
            capturedAtMillis = capturedAtMillis
        )
    }

    private fun normalize(value: String?): String = value.orEmpty()
        .replace('\u0000'.toString(), "")
        .replace(Regex("[ \\t]+\\n"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun isControlOnly(value: String): Boolean =
        value.length <= 3 && value.all { it.isWhitespace() || it in CONTROL_GLYPHS }

    private data class Candidate(
        val node: VisibleTextNode,
        val text: String
    )

    private companion object {
        val CONTROL_GLYPHS = setOf('⋮', '⋯', '•', '·', '›', '‹', '→', '←', '✓', '✕')
    }
}
