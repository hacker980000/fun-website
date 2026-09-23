package com.socialaiassistant.keyboard.context

internal object SupportedPlatformAdapters {
    fun all(): List<PlatformContextAdapter> = listOf(
        ruleAdapter(
            packages = setOf("com.facebook.katana", "com.facebook.lite"),
            defaultSurface = ConversationSurface.GENERAL,
            confidenceBoost = 0.20f
        ),
        ruleAdapter(
            packages = setOf("com.facebook.orca", "com.facebook.mlite"),
            defaultSurface = ConversationSurface.INBOX,
            confidenceBoost = 0.30f
        ),
        ruleAdapter(
            packages = setOf("com.whatsapp", "com.whatsapp.w4b"),
            defaultSurface = ConversationSurface.INBOX,
            confidenceBoost = 0.30f
        ),
        ruleAdapter(
            packages = setOf("com.instagram.android", "com.instagram.lite"),
            defaultSurface = ConversationSurface.GENERAL,
            confidenceBoost = 0.25f
        ),
        ruleAdapter(
            packages = setOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram"),
            defaultSurface = ConversationSurface.INBOX,
            confidenceBoost = 0.30f
        )
    )

    private fun ruleAdapter(
        packages: Set<String>,
        defaultSurface: ConversationSurface,
        confidenceBoost: Float
    ): PlatformContextAdapter = RulePlatformContextAdapter(
        packages = packages,
        defaultSurface = defaultSurface,
        confidenceBoost = confidenceBoost
    )
}

private class RulePlatformContextAdapter(
    private val packages: Set<String>,
    private val defaultSurface: ConversationSurface,
    private val confidenceBoost: Float
) : PlatformContextAdapter {
    override fun supports(packageName: String): Boolean = packageName.lowercase() in packages

    override fun adapt(input: PlatformContextInput): PlatformContextResult {
        val meta = listOfNotNull(
            input.windowSignature,
            input.conversationHint,
            input.composerHint
        ).joinToString(" ").lowercase()
        val composerMeta = input.composerHint.orEmpty().trim().lowercase()
        val nonConversationSurface = NON_CONVERSATION_COMPOSER_PATTERN.containsMatchIn(composerMeta) ||
            WINDOW_SEARCH_PATTERN.containsMatchIn(input.windowSignature.lowercase())

        if (nonConversationSurface) {
            return PlatformContextResult(
                surface = ConversationSurface.GENERAL,
                conversationHint = null,
                nodes = emptyList(),
                confidenceBoost = 0f
            )
        }

        val surface = when {
            COMMENT_PATTERN.containsMatchIn(meta) -> ConversationSurface.COMMENT
            INBOX_PATTERN.containsMatchIn(meta) -> ConversationSurface.INBOX
            else -> defaultSurface
        }

        val filtered = input.nodes.filterNot { node ->
            val value = node.text.orEmpty().trim()
            value.isEmpty() || isUiNoise(value)
        }

        return PlatformContextResult(
            surface = surface,
            conversationHint = sanitizeConversationHint(input.conversationHint),
            nodes = filtered,
            confidenceBoost = confidenceBoost
        )
    }

    private fun sanitizeConversationHint(value: String?): String? {
        val clean = value.orEmpty().trim()
        if (clean.isEmpty()) return null
        if (GENERIC_HINTS.contains(clean.lowercase())) return null
        return clean.take(300)
    }

    private fun isUiNoise(value: String): Boolean {
        val clean = value.trim().lowercase()
        if (clean in EXACT_UI_NOISE) return true
        if (TIME_ONLY.matches(clean)) return true
        if (DAY_ONLY.matches(clean)) return true
        return false
    }

    private companion object {
        val COMMENT_PATTERN = Regex("\\b(comment|comments|add a comment|write a comment|reply to comment)\\b|মন্তব্য|কমেন্ট", RegexOption.IGNORE_CASE)
        val INBOX_PATTERN = Regex("\\b(message|messages|chat|conversation|direct|dm|type a message|write a message|reply to)\\b|বার্তা|মেসেজ", RegexOption.IGNORE_CASE)
        val NON_CONVERSATION_COMPOSER_PATTERN = Regex(
            "^\\s*(search(?: chats| messages)?|find|write a caption|add a caption|caption|create post|what's on your mind|status update|about)\\b|খুঁজুন|সার্চ|ক্যাপশন",
            RegexOption.IGNORE_CASE
        )
        val WINDOW_SEARCH_PATTERN = Regex("\\b(search|find)\\b|খুঁজুন|সার্চ", RegexOption.IGNORE_CASE)
        val TIME_ONLY = Regex("^\\d{1,2}:\\d{2}(?:\\s?[ap]m)?$", RegexOption.IGNORE_CASE)
        val DAY_ONLY = Regex("^(today|yesterday|mon|tue|wed|thu|fri|sat|sun|আজ|গতকাল)$", RegexOption.IGNORE_CASE)
        val GENERIC_HINTS = setOf("chat", "conversation", "messages", "message", "inbox")
        val EXACT_UI_NOISE = setOf(
            "active now", "online", "seen", "delivered", "sent", "send",
            "like", "reply", "share", "forward", "react", "gif", "sticker",
            "photo", "camera", "voice message", "voice clip", "audio call", "video call",
            "call", "typing…", "typing...", "read", "unread",
            "সক্রিয়", "দেখেছেন", "পাঠান", "লাইক", "শেয়ার", "রিপ্লাই"
        )
    }
}
