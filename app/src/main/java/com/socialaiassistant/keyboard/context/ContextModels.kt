package com.socialaiassistant.keyboard.context

enum class SenderClass {
    SELF,
    RECIPIENT,
    UNKNOWN
}

data class ContextMessage(
    val sender: SenderClass,
    val text: String,
    val timestampHint: String? = null
)

data class ContextSnapshot(
    val packageName: String,
    val windowSignature: String,
    val conversationHint: String?,
    val messages: List<ContextMessage>,
    val latestRecipientMessage: String?,
    val composerHint: String?,
    val surface: ConversationSurface = ConversationSurface.GENERAL,
    val confidence: Float,
    val capturedAtMillis: Long
)

data class VisibleTextNode(
    val order: Int,
    val text: String?,
    val senderHint: SenderClass = SenderClass.UNKNOWN,
    val editable: Boolean = false,
    val control: Boolean = false,
    val timestampHint: String? = null,
    val contentDescription: String? = null,
    val viewIdResourceName: String? = null,
    val className: String? = null,
    val clickable: Boolean = false,
    val screenLeft: Int? = null,
    val screenTop: Int? = null,
    val screenRight: Int? = null,
    val screenBottom: Int? = null
) {
    val hasScreenBounds: Boolean
        get() = screenLeft != null && screenTop != null && screenRight != null && screenBottom != null &&
            screenRight > screenLeft && screenBottom > screenTop
}
