package com.socialaiassistant.keyboard.ime

/**
 * Stage 21 keeps ranking knobs in one Android-free policy so future device/CI evidence can tune
 * weights without spreading magic numbers across the typing engines.
 */
object PredictionRankingPolicy {
    const val ENGLISH_EXACT_BASE = 2_000
    const val ENGLISH_ALIAS_BASE = 4_000
    const val ENGLISH_PREFIX_BASE = 1_450
    const val ENGLISH_TYPO_BASE = 1_300
    const val ENGLISH_PREFIX_COMPLETION_PENALTY = 7
    const val ENGLISH_TYPO_POSITION_PENALTY = 6
    const val ENGLISH_TYPO_LENGTH_DELTA_PENALTY = 8

    const val ENGLISH_NEXT_BIGRAM_WEIGHT = 120
    const val ENGLISH_NEXT_UNIGRAM_WEIGHT = 4

    const val BANGLA_NEXT_BIGRAM_WEIGHT = 115
    const val BANGLA_NEXT_CONTEXT_WEIGHT = 290
    const val BANGLA_NEXT_UNIGRAM_WEIGHT = 4
    const val BANGLA_LEARNED_ONLY_BASE = 90

    fun englishKindPriority(kind: EnglishSuggestionKind): Int = when (kind) {
        EnglishSuggestionKind.EXACT -> 6
        EnglishSuggestionKind.ALIAS -> 5
        EnglishSuggestionKind.PERSONAL -> 4
        EnglishSuggestionKind.PREFIX -> 3
        EnglishSuggestionKind.TYPO -> 2
        EnglishSuggestionKind.NEXT_WORD -> 1
    }

    fun banglaKindPriority(kind: SuggestionKind): Int = when (kind) {
        SuggestionKind.EXACT -> 7
        SuggestionKind.ALIAS -> 6
        SuggestionKind.PERSONAL -> 5
        SuggestionKind.PREFIX -> 4
        SuggestionKind.TYPO -> 3
        SuggestionKind.FALLBACK -> 2
        SuggestionKind.NEXT_WORD -> 1
    }

    fun completionPenalty(candidateLength: Int, typedLength: Int, perCharacter: Int): Int =
        (candidateLength - typedLength).coerceAtLeast(0) * perCharacter.coerceAtLeast(0)

    fun englishNextWordScore(
        corpusCount: Int,
        unigramCount: Int,
        personalTransition: Int,
        personalWord: Int
    ): Int = corpusCount.coerceAtLeast(0) * ENGLISH_NEXT_BIGRAM_WEIGHT +
        unigramCount.coerceAtLeast(0) * ENGLISH_NEXT_UNIGRAM_WEIGHT +
        personalTransition.coerceAtLeast(0) +
        personalWord.coerceAtLeast(0)

    fun banglaNextWordScore(
        corpusCount: Int,
        contextCount: Int,
        unigramCount: Int,
        personalBigram: Int,
        personalContext: Int,
        personalWord: Int
    ): Int {
        val learnedOnlyBase = if (
            corpusCount <= 0 && contextCount <= 0 && (personalBigram + personalContext) > 0
        ) BANGLA_LEARNED_ONLY_BASE else 0
        return corpusCount.coerceAtLeast(0) * BANGLA_NEXT_BIGRAM_WEIGHT +
            contextCount.coerceAtLeast(0) * BANGLA_NEXT_CONTEXT_WEIGHT +
            unigramCount.coerceAtLeast(0) * BANGLA_NEXT_UNIGRAM_WEIGHT +
            personalBigram.coerceAtLeast(0) +
            personalContext.coerceAtLeast(0) +
            personalWord.coerceAtLeast(0) +
            learnedOnlyBase
    }
}

enum class ColdStartMilestone {
    LEARNING_MODELS_READY,
    SERVICE_READY,
    FIRST_INPUT_VIEW_READY,
    FIRST_SUGGESTION_READY
}

data class ColdStartMilestoneSample(
    val milestone: ColdStartMilestone,
    val elapsedMicros: Long
)

/**
 * One-shot process-memory profiler for debug/device evidence.
 * It stores only elapsed durations and fixed milestone names; no typed text/editor/package data is kept.
 */
class ColdStartProfiler(private val startedNanos: Long) {
    private val emitted = mutableSetOf<ColdStartMilestone>()

    @Synchronized
    fun mark(milestone: ColdStartMilestone, nowNanos: Long): ColdStartMilestoneSample? {
        if (!emitted.add(milestone)) return null
        val elapsed = (nowNanos - startedNanos).coerceAtLeast(0L) / 1_000L
        return ColdStartMilestoneSample(milestone, elapsed)
    }

    @Synchronized
    fun isMarked(milestone: ColdStartMilestone): Boolean = milestone in emitted
}
