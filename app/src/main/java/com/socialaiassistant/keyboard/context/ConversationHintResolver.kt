package com.socialaiassistant.keyboard.context

object ConversationHintResolver {
    @Suppress("UNUSED_PARAMETER")
    fun resolve(
        explicitHint: String?,
        windowSignature: String,
        nodes: List<VisibleTextNode>
    ): String? {
        // A toolbar/thread identity node is stronger than an Accessibility event's transient
        // contentDescription (which may describe the button or row that triggered the event).
        val metadataCandidate = ConversationNodeOrdering.chronological(nodes.filter(::isIdentityNode))
            .asSequence()
            .mapNotNull { node -> normalizeCandidate(node.text) ?: normalizeCandidate(node.contentDescription) }
            .firstOrNull()
        if (metadataCandidate != null) return metadataCandidate

        // AccessibilityEvent.contentDescription is transient event metadata (Back, avatar, row,
        // button, etc.), not a trustworthy thread identifier. Likewise, windowId/className can
        // be reused for different chats. If no explicit identity node is exposed, fail closed and
        // let the repository use snapshot-only transient history instead of persisting a guess.
        return null
    }

    fun isIdentityNode(node: VisibleTextNode): Boolean {
        val id = node.viewIdResourceName.orEmpty().lowercase()
        return IDENTITY_ID_MARKERS.any(id::contains)
    }

    private fun normalizeCandidate(value: String?): String? {
        val clean = value.orEmpty().replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty() || clean.length > 160) return null
        val lower = clean.lowercase()
        if (lower in GENERIC_HINTS) return null
        if (GENERIC_PATTERNS.any { it.matches(lower) }) return null
        return clean
    }

    private val IDENTITY_ID_MARKERS = listOf(
        "conversation_contact_name",
        "conversation_title",
        "thread_title",
        "thread_name",
        "chat_title",
        "chat_name",
        "toolbar_title",
        "action_bar_title",
        "recipient_name",
        "conversation_name",
        "header_title"
    )

    private val GENERIC_HINTS = setOf(
        "chat", "conversation", "messages", "message", "inbox", "direct", "direct messages",
        "comment", "comments", "replies", "post", "new message", "send message", "reply",
        "active now", "online", "typing…", "typing..."
    )

    private val GENERIC_PATTERNS = listOf(
        Regex("^\\d+:[a-z0-9_.$]+$", RegexOption.IGNORE_CASE),
        Regex("^(android\\.|com\\.).*(view|layout|window).*$", RegexOption.IGNORE_CASE)
    )

}
