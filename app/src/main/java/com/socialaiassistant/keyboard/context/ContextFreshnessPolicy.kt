package com.socialaiassistant.keyboard.context

/**
 * Prevents a reply request from using an accessibility snapshot that belongs to an old UI state.
 *
 * Accessibility callbacks can race with IME focus changes. Package/session checks remain the
 * primary identity boundary; this policy adds a short wall-clock freshness window so an old
 * snapshot is never silently reused after the user changes thread or returns later.
 */
object ContextFreshnessPolicy {
    const val MAX_CONTEXT_AGE_MS: Long = 20_000L
    private const val FUTURE_CLOCK_SKEW_MS: Long = 5_000L

    fun isFresh(snapshot: ContextSnapshot, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (snapshot.capturedAtMillis <= 0L) return false
        val age = nowMillis - snapshot.capturedAtMillis
        return age in -FUTURE_CLOCK_SKEW_MS..MAX_CONTEXT_AGE_MS
    }

    fun isUsableForPackage(
        snapshot: ContextSnapshot?,
        packageName: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (snapshot == null) return false
        if (snapshot.packageName != packageName) return false
        return isFresh(snapshot, nowMillis)
    }
}
