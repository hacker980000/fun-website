package com.socialaiassistant.keyboard.ime

data class TypingMutation(
    val directCommit: String? = null,
    val composingText: String? = null,
    val deletePrevious: Boolean = false
)

class ImeTypingEngine(
    private val learning: LocalTypingLearningModel = InMemoryTypingLearningModel(),
    private val phoneticComposer: BanglaPhoneticComposer = BanglaPhoneticComposer(),
    private val suggestionEngine: OfflineBanglaSuggestionEngine = OfflineBanglaSuggestionEngine(learning = learning),
    private val nextWordModel: OfflineBanglaNextWordModel = OfflineBanglaNextWordModel(learning = learning)
) {
    private var secondLastCommittedWord: String? = null
    private var lastCommittedWord: String? = null
    private var nextWordSuggestionsVisible: Boolean = false
    private var learningEnabled: Boolean = true
    private var suggestionsEnabled: Boolean = true
    private var suggestionRevision: Long = 0L

    fun onText(mode: KeyboardUiMode, value: String): TypingMutation {
        nextWordSuggestionsVisible = false
        bumpSuggestionRevision()
        return if (isPhonetic(mode)) {
            val result = phoneticComposer.acceptLatin(value)
            TypingMutation(composingText = result.composing)
        } else {
            TypingMutation(directCommit = value)
        }
    }

    fun onBackspace(mode: KeyboardUiMode): TypingMutation {
        nextWordSuggestionsVisible = false
        bumpSuggestionRevision()
        if (!isPhonetic(mode) || !phoneticComposer.isComposing()) {
            return TypingMutation(deletePrevious = true)
        }
        val result = phoneticComposer.backspace()
        return TypingMutation(composingText = result.composing)
    }

    fun currentSuggestions(limit: Int = 3): BanglaSuggestionSnapshot {
        if (phoneticComposer.isComposing()) {
            return suggestionEngine.suggest(
                roman = phoneticComposer.romanBuffer(),
                renderedFallback = phoneticComposer.renderedBuffer(),
                limit = limit
            )
        }
        if (!nextWordSuggestionsVisible) return BanglaSuggestionSnapshot("", emptyList())
        val next = nextWordModel.suggest(secondLastCommittedWord, lastCommittedWord, limit)
        return BanglaSuggestionSnapshot(
            roman = "",
            candidates = next.map { prediction ->
                BanglaSuggestionCandidate(
                    text = prediction.text,
                    sourceRoman = "",
                    kind = SuggestionKind.NEXT_WORD,
                    score = prediction.score
                )
            }
        )
    }

    /** Normal flush preserves the current transliteration exactly. */
    fun flush(): String {
        val roman = if (phoneticComposer.isComposing()) phoneticComposer.romanBuffer() else ""
        val output = phoneticComposer.flush()
        if (output.isNotBlank()) {
            rememberCommittedText(output)
            if (learningEnabled && roman.isNotBlank()) {
                learning.recordPhonetic(roman, output, weight = 1)
                learning.requestFlush()
            }
        }
        nextWordSuggestionsVisible = false
        bumpSuggestionRevision()
        return output
    }

    /** Word-boundary commit applies only conservative, explicit shorthand correction. */
    fun flushWithAutocorrect(): String {
        if (!phoneticComposer.isComposing()) return ""
        val roman = phoneticComposer.romanBuffer()
        val rendered = phoneticComposer.renderedBuffer()
        val output = suggestionEngine.commitText(roman, rendered)
        phoneticComposer.reset()
        rememberCommittedText(output)
        if (learningEnabled) {
            learning.recordPhonetic(roman, output, weight = 1)
            learning.requestFlush()
        }
        nextWordSuggestionsVisible = suggestionsEnabled
        bumpSuggestionRevision()
        return output
    }

    /** Commits a resolved glide word without exposing the raw path as composing text. */
    fun commitGlideWord(roman: String, text: String): String {
        phoneticComposer.reset()
        rememberCommittedText(text)
        if (learningEnabled) {
            learning.recordPhonetic(roman, text, weight = 2)
            learning.requestFlush()
        }
        nextWordSuggestionsVisible = suggestionsEnabled
        bumpSuggestionRevision()
        return text
    }

    /** Candidate taps replace the active composing region, or insert an offline next word. */
    fun acceptSuggestion(text: String): String {
        if (phoneticComposer.isComposing()) {
            val roman = phoneticComposer.romanBuffer()
            phoneticComposer.reset()
            rememberCommittedText(text)
            if (learningEnabled) {
                learning.recordPhonetic(roman, text, weight = 3)
                learning.requestFlush()
            }
            nextWordSuggestionsVisible = false
            bumpSuggestionRevision()
            return text
        }
        if (nextWordSuggestionsVisible) {
            val previous = lastCommittedWord
            if (learningEnabled && previous != null) nextWordModel.learnTransition(secondLastCommittedWord, previous, text)
            secondLastCommittedWord = lastCommittedWord
            lastCommittedWord = normalizeBanglaToken(text) ?: lastCommittedWord
            if (learningEnabled) learning.requestFlush()
            nextWordSuggestionsVisible = suggestionsEnabled
            bumpSuggestionRevision()
        }
        return text
    }

    fun isShowingNextWordSuggestions(): Boolean =
        !phoneticComposer.isComposing() && nextWordSuggestionsVisible && lastCommittedWord != null

    fun dismissNextWordSuggestions() {
        if (nextWordSuggestionsVisible) {
            nextWordSuggestionsVisible = false
            bumpSuggestionRevision()
        }
    }

    fun setLearningEnabled(enabled: Boolean) {
        learningEnabled = enabled
    }

    fun setSuggestionsEnabled(enabled: Boolean) {
        if (suggestionsEnabled == enabled) return
        suggestionsEnabled = enabled
        if (!enabled) nextWordSuggestionsVisible = false
        bumpSuggestionRevision()
    }

    fun reset() {
        phoneticComposer.reset()
        secondLastCommittedWord = null
        lastCommittedWord = null
        nextWordSuggestionsVisible = false
        bumpSuggestionRevision()
    }

    fun isComposing(): Boolean = phoneticComposer.isComposing()

    fun currentRomanBuffer(): String = if (phoneticComposer.isComposing()) phoneticComposer.romanBuffer() else ""

    fun discardComposition() {
        val changed = phoneticComposer.isComposing() || nextWordSuggestionsVisible
        phoneticComposer.reset()
        nextWordSuggestionsVisible = false
        if (changed) bumpSuggestionRevision()
    }

    fun suggestionRevision(): Long = suggestionRevision

    fun dictionarySize(): Int = suggestionEngine.dictionarySize()

    private fun bumpSuggestionRevision() {
        suggestionRevision = if (suggestionRevision == Long.MAX_VALUE) 0L else suggestionRevision + 1L
    }

    private fun rememberCommittedText(text: String) {
        if (learningEnabled) {
            val context = nextWordModel.learnCommittedTextWithContext(text, secondLastCommittedWord, lastCommittedWord)
            secondLastCommittedWord = context.first
            lastCommittedWord = context.second
        } else {
            tokenizeBangla(text).forEach { token ->
                secondLastCommittedWord = lastCommittedWord
                lastCommittedWord = token
            }
        }
    }

    private fun isPhonetic(mode: KeyboardUiMode): Boolean =
        mode.language == KeyboardLanguage.BANGLA &&
            mode.banglaMode == BanglaInputMode.PHONETIC &&
            mode.layer == KeyboardLayer.LETTERS
}
