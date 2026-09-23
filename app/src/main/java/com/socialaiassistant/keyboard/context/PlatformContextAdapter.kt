package com.socialaiassistant.keyboard.context

data class PlatformContextInput(
    val packageName: String,
    val windowSignature: String,
    val conversationHint: String?,
    val composerHint: String?,
    val nodes: List<VisibleTextNode>
)

data class PlatformContextResult(
    val surface: ConversationSurface,
    val conversationHint: String?,
    val nodes: List<VisibleTextNode>,
    val confidenceBoost: Float = 0f
)

interface PlatformContextAdapter {
    fun supports(packageName: String): Boolean
    fun adapt(input: PlatformContextInput): PlatformContextResult
}
