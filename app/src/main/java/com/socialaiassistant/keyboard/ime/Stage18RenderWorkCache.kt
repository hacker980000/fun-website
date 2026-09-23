package com.socialaiassistant.keyboard.ime

/**
 * Small Android-free helpers used to avoid repeating identical IME render/computation work.
 * No user text is persisted; all state is in-memory and bounded to a single entry.
 */
data class KeyboardRenderSignature(
    val mode: KeyboardUiMode,
    val showNumberRow: Boolean,
    val keyBoundaryEnabled: Boolean = false,
    val quickKeys: List<String>,
    val keyHeightDp: Int,
    val oneHandedMode: String,
    val glideTyping: Boolean,
    val spacebarCursorControl: Boolean,
    val glideEligiblePolicy: Boolean,
    val keyGapBits: Int,
    val enterLabel: String
)

class KeyboardRenderGate {
    private var last: KeyboardRenderSignature? = null

    fun shouldRebuild(signature: KeyboardRenderSignature, existingChildCount: Int): Boolean {
        if (existingChildCount <= 0 || last != signature) {
            last = signature
            return true
        }
        return false
    }

    fun invalidate() {
        last = null
    }
}

class SingleEntryMemo<K, V> {
    private var hasValue = false
    private var key: K? = null
    private var value: V? = null

    fun getOrCompute(requestKey: K, compute: () -> V): V {
        if (hasValue && key == requestKey) {
            @Suppress("UNCHECKED_CAST")
            return value as V
        }
        val computed = compute()
        key = requestKey
        value = computed
        hasValue = true
        return computed
    }

    fun clear() {
        hasValue = false
        key = null
        value = null
    }
}

data class SuggestionBarSignature(
    val visible: Boolean,
    val entries: List<String> = emptyList()
)

class SuggestionBarRenderGate {
    private var last: SuggestionBarSignature? = null

    fun shouldRebuild(signature: SuggestionBarSignature, existingChildCount: Int): Boolean {
        if (last != signature) {
            last = signature
            return true
        }
        if (signature.visible && existingChildCount != signature.entries.size) return true
        if (!signature.visible && existingChildCount != 0) return true
        return false
    }

    fun invalidate() {
        last = null
    }
}
