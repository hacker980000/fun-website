package com.socialaiassistant.keyboard.ime

/**
 * Stage 22 confidence gates for automatic typo replacement.
 *
 * The policy is Android-free and deterministic so it can be regression-tested without an emulator.
 * It deliberately does not inspect editor/package/context data and it never changes explicit alias
 * behavior. A typo is auto-applied only when the token is long enough, the best candidate clears a
 * minimum score, and it is separated from the runner-up by a conservative margin.
 */
object PredictionAutocorrectPolicy {
    const val ENGLISH_TYPO_MIN_LENGTH = 4
    const val ENGLISH_TYPO_MIN_SCORE = 1_500
    const val ENGLISH_TYPO_MIN_MARGIN = 120

    // Preserve the Stage 20/21 Bangla safety envelope while centralizing it for regression coverage.
    const val BANGLA_TYPO_MIN_LENGTH = 5
    const val BANGLA_TYPO_MIN_SCORE = 935
    const val BANGLA_TYPO_MIN_MARGIN = 45

    fun isConfidentTypo(
        tokenLength: Int,
        topScore: Int,
        runnerUpScore: Int?,
        minLength: Int,
        minScore: Int,
        minMargin: Int
    ): Boolean {
        if (tokenLength < minLength || topScore < minScore) return false
        if (runnerUpScore == null) return true
        return topScore.toLong() - runnerUpScore.toLong() >= minMargin.coerceAtLeast(0).toLong()
    }

    fun isConfidentEnglishTypo(tokenLength: Int, topScore: Int, runnerUpScore: Int?): Boolean =
        isConfidentTypo(
            tokenLength = tokenLength,
            topScore = topScore,
            runnerUpScore = runnerUpScore,
            minLength = ENGLISH_TYPO_MIN_LENGTH,
            minScore = ENGLISH_TYPO_MIN_SCORE,
            minMargin = ENGLISH_TYPO_MIN_MARGIN
        )

    fun isConfidentBanglaTypo(tokenLength: Int, topScore: Int, runnerUpScore: Int?): Boolean =
        isConfidentTypo(
            tokenLength = tokenLength,
            topScore = topScore,
            runnerUpScore = runnerUpScore,
            minLength = BANGLA_TYPO_MIN_LENGTH,
            minScore = BANGLA_TYPO_MIN_SCORE,
            minMargin = BANGLA_TYPO_MIN_MARGIN
        )
}
