package com.socialaiassistant.keyboard.ime

/**
 * Android-free editor/session decisions for IME lifecycle and field-aware typing.
 *
 * Numeric constants mirror android.text.InputType / EditorInfo so this policy can be
 * regression-tested with plain Kotlin and without an Android runtime.
 */
enum class EditorSurfaceKind {
    GENERIC_TEXT,
    EMAIL,
    URI,
    FILTER,
    EDITOR_AUTOCOMPLETE,
    NO_SUGGESTIONS,
    PASSWORD,
    NON_TEXT
}

data class ImeTypingFieldPolicy(
    val kind: EditorSurfaceKind,
    val showSuggestions: Boolean,
    val allowAutocorrect: Boolean,
    val allowLearning: Boolean,
    val forceLiteralLatin: Boolean,
    val quickKeys: List<String> = emptyList()
) {
    val allowSmartLanguageHints: Boolean
        get() = showSuggestions && !forceLiteralLatin
}

object ImeEditorBehaviorPolicy {
    private const val TYPE_MASK_CLASS = 0x0000000f
    private const val TYPE_CLASS_TEXT = 0x00000001
    private const val TYPE_CLASS_NUMBER = 0x00000002
    private const val TYPE_CLASS_PHONE = 0x00000003
    private const val TYPE_CLASS_DATETIME = 0x00000004

    private const val TYPE_MASK_VARIATION = 0x00000ff0
    private const val TYPE_TEXT_VARIATION_URI = 0x00000010
    private const val TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 0x00000020
    private const val TYPE_TEXT_VARIATION_PASSWORD = 0x00000080
    private const val TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090
    private const val TYPE_TEXT_VARIATION_FILTER = 0x000000b0
    private const val TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS = 0x000000d0
    private const val TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0

    private const val TYPE_TEXT_FLAG_AUTO_CORRECT = 0x00008000
    private const val TYPE_TEXT_FLAG_AUTO_COMPLETE = 0x00010000
    private const val TYPE_TEXT_FLAG_NO_SUGGESTIONS = 0x00080000

    private const val IME_MASK_ACTION = 0x000000ff
    private const val IME_ACTION_GO = 0x00000002
    private const val IME_ACTION_SEARCH = 0x00000003
    private const val IME_ACTION_SEND = 0x00000004
    private const val IME_ACTION_NEXT = 0x00000005
    private const val IME_ACTION_DONE = 0x00000006
    private const val IME_ACTION_PREVIOUS = 0x00000007
    private const val IME_FLAG_NO_ENTER_ACTION = 0x40000000

    fun preferredLayer(inputType: Int): KeyboardLayer {
        return when (inputType and TYPE_MASK_CLASS) {
            TYPE_CLASS_NUMBER, TYPE_CLASS_PHONE, TYPE_CLASS_DATETIME -> KeyboardLayer.NUMBERS
            else -> KeyboardLayer.LETTERS
        }
    }

    /**
     * Respect editor intent instead of treating every non-password text field as prose.
     *
     * - NO_SUGGESTIONS means no dictionary candidate UI.
     * - AUTO_COMPLETE means the editor owns its completion UI.
     * - Email/URI fields stay literal Latin and never learn/autocorrect tokens.
     * - FILTER fields avoid personal learning because their content is usually ephemeral/search-like.
     */
    fun typingPolicy(inputType: Int): ImeTypingFieldPolicy {
        if ((inputType and TYPE_MASK_CLASS) != TYPE_CLASS_TEXT) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.NON_TEXT,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = false
            )
        }

        val variation = inputType and TYPE_MASK_VARIATION
        val noSuggestions = inputType and TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0
        val editorAutocomplete = inputType and TYPE_TEXT_FLAG_AUTO_COMPLETE != 0
        val requestsAutocorrect = inputType and TYPE_TEXT_FLAG_AUTO_CORRECT != 0

        if (variation == TYPE_TEXT_VARIATION_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.PASSWORD,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = true
            )
        }

        if (variation == TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.EMAIL,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = true,
                quickKeys = listOf("@", ".", "_", "-", ".com")
            )
        }

        if (variation == TYPE_TEXT_VARIATION_URI) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.URI,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = true,
                quickKeys = listOf("https://", "www.", ".com", "/", ".")
            )
        }

        if (noSuggestions) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.NO_SUGGESTIONS,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = false
            )
        }

        if (editorAutocomplete) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.EDITOR_AUTOCOMPLETE,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = false
            )
        }

        if (variation == TYPE_TEXT_VARIATION_FILTER) {
            return ImeTypingFieldPolicy(
                kind = EditorSurfaceKind.FILTER,
                showSuggestions = false,
                allowAutocorrect = false,
                allowLearning = false,
                forceLiteralLatin = false
            )
        }

        return ImeTypingFieldPolicy(
            kind = EditorSurfaceKind.GENERIC_TEXT,
            showSuggestions = true,
            allowAutocorrect = requestsAutocorrect,
            allowLearning = true,
            forceLiteralLatin = false
        )
    }

    fun enterKeyLabel(imeOptions: Int): String {
        if (imeOptions and IME_FLAG_NO_ENTER_ACTION != 0) return "↵"
        return when (imeOptions and IME_MASK_ACTION) {
            IME_ACTION_GO -> "Go"
            IME_ACTION_SEARCH -> "Search"
            IME_ACTION_SEND -> "Send"
            IME_ACTION_NEXT -> "Next"
            IME_ACTION_DONE -> "Done"
            IME_ACTION_PREVIOUS -> "Previous"
            else -> "↵"
        }
    }

    /**
     * Android calls onUpdateSelection after setComposingText(). That normal callback points to the
     * end of the candidate region and must not cancel composition. If selection moves elsewhere,
     * the local Roman/composing buffer is stale and must be discarded before the next key press.
     */
    fun shouldAbortCompositionForSelection(
        composing: Boolean,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ): Boolean {
        if (!composing) return false
        if (candidatesStart < 0 || candidatesEnd < 0) return true
        return newSelStart != candidatesEnd || newSelEnd != candidatesEnd
    }
}
