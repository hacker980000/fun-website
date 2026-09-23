package com.socialaiassistant.keyboard.context

class PlatformContextAdapterRegistry(
    private val adapters: List<PlatformContextAdapter>
) {
    fun supportsPackage(packageName: String): Boolean {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return false
        return adapters.any { it.supports(normalized) }
    }

    fun adapt(input: PlatformContextInput): PlatformContextResult {
        val adapter = adapters.firstOrNull { it.supports(input.packageName.trim()) }
            ?: return unsupportedResult()
        return adapter.adapt(input)
    }

    private fun unsupportedResult(): PlatformContextResult = PlatformContextResult(
        surface = ConversationSurface.GENERAL,
        conversationHint = null,
        nodes = emptyList(),
        confidenceBoost = 0f
    )

    companion object {
        fun default(): PlatformContextAdapterRegistry = PlatformContextAdapterRegistry(
            SupportedPlatformAdapters.all()
        )
    }
}
