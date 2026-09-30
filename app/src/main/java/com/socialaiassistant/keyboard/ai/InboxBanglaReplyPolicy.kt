package com.socialaiassistant.keyboard.ai

/**
 * Final client-side language guard for inbox social replies.
 *
 * The model/payload is instructed to answer in Bengali first. This guard prevents an
 * accidental English/Banglish response from reaching the keyboard UI. It intentionally
 * uses conservative Bengali fallbacks instead of pretending to translate unsupported facts.
 */
object InboxBanglaReplyPolicy {
    private val banglaScript = Regex("[\\u0980-\\u09FF]")

    fun containsBanglaScript(text: String): Boolean = banglaScript.containsMatchIn(text)

    fun ensureBangla(
        reply: String,
        action: ManualAiAction = ManualAiAction.SMART,
        intent: ConversationAiIntent? = null,
        latestRecipientMessage: String? = null
    ): String {
        val clean = reply.trim()
        if (clean.isNotEmpty() && containsBanglaScript(clean)) return clean

        return fallback(
            action = action,
            intent = intent,
            hasRecipientMessage = !latestRecipientMessage.isNullOrBlank()
        )
    }

    private fun fallback(
        action: ManualAiAction,
        intent: ConversationAiIntent?,
        hasRecipientMessage: Boolean
    ): String = when (action) {
        ManualAiAction.FLIRTY -> when {
            intent == ConversationAiIntent.START || !hasRecipientMessage ->
                "হ্যালো 🙂 কথা শুরু করতে ইচ্ছে হলো—আজ আপনার দিনটা কেমন যাচ্ছে?"
            intent == ConversationAiIntent.CONTINUE ->
                "আপনার সাথে কথা বলতে ভালো লাগছে 🙂 আজকের দিনের সবচেয়ে ভালো মুহূর্তটা কী ছিল?"
            else ->
                "আপনার কথাটা বেশ মজার লাগল 🙂 আরেকটু বলবেন?"
        }

        ManualAiAction.WITTY -> when {
            intent == ConversationAiIntent.START || !hasRecipientMessage ->
                "হ্যালো! কেমন আছেন? আজকের দিনটা কেমন কাটছে?"
            else ->
                "বুঝলাম—বিষয়টা একটু ইন্টারেস্টিং। আপনার দিকটা আরেকটু বলবেন?"
        }

        else -> when {
            intent == ConversationAiIntent.START || !hasRecipientMessage ->
                "হাই, কেমন আছেন?"
            intent == ConversationAiIntent.CONTINUE ->
                "আচ্ছা, তাহলে কথা এগিয়ে নেওয়া যাক—আপনার কী মনে হয়?"
            else ->
                "বুঝলাম। বিষয়টা নিয়ে আরেকটু বিস্তারিত বলবেন?"
        }
    }
}
