package com.socialaiassistant.keyboard.ime

import kotlin.math.abs
import kotlin.math.hypot

/** Android-free swipe/glide resolver used by the IME gesture layer. */
data class GlideCandidate(
    val text: String,
    val sourceRoman: String,
    val score: Int
)

/**
 * Stage-9 glide resolver.
 *
 * Compared with the Stage-8 foundation this scorer keeps the gesture fully local
 * while adding QWERTY-aware error costs, ordered-path coverage and a guarded
 * one-key endpoint tolerance. This improves realistic diagonal/near-key swipes
 * without making normal taps or dictionary commits less deterministic.
 */
class GlideTypingEngine(
    private val englishWords: Map<String, Int> = ProductionEnglishLexicon.words,
    private val banglaWords: Map<String, String> = ProductionBanglaLexicon.words,
    private val banglaFrequency: Map<String, Int> = ProductionBanglaLexicon.frequencyBonus
) {
    private data class EnglishIndexedEntry(val text: String, val path: String, val bonus: Int)
    private data class BanglaIndexedEntry(val roman: String, val text: String, val path: String, val bonus: Int)

    // Normalize once and bucket by exact first key. Stage 9 already requires an exact
    // first key, so scanning unrelated buckets on every gesture only wastes CPU.
    private val englishEntriesByFirst: Map<Char, List<EnglishIndexedEntry>> = englishWords.asSequence()
        .mapNotNull { (word, bonus) ->
            val path = normalizeWordPath(word) ?: return@mapNotNull null
            EnglishIndexedEntry(word, path, bonus)
        }
        .groupBy { it.path.first() }

    private val banglaEntriesByFirst: Map<Char, List<BanglaIndexedEntry>> = banglaWords.asSequence()
        .mapNotNull { (roman, bangla) ->
            val path = normalizeWordPath(roman) ?: return@mapNotNull null
            BanglaIndexedEntry(roman, bangla, path, (banglaFrequency[roman] ?: 0) * 8)
        }
        .groupBy { it.path.first() }
    fun resolveEnglish(rawSequence: String, limit: Int = 3): List<GlideCandidate> {
        val observed = normalizeSequence(rawSequence) ?: return emptyList()
        return englishEntriesByFirst[observed.first()].orEmpty().asSequence()
            .filter { kotlin.math.abs(it.path.length - observed.length) <= MAX_PATH_LENGTH_DELTA }
            .mapNotNull { entry ->
                val pathScore = scorePath(observed, entry.path) ?: return@mapNotNull null
                GlideCandidate(
                    text = entry.text,
                    sourceRoman = entry.text,
                    score = BASE_SCORE + entry.bonus + pathScore
                )
            }
            .sortedWith(compareByDescending<GlideCandidate> { it.score }.thenBy { it.text })
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    fun resolveBangla(rawSequence: String, limit: Int = 3): List<GlideCandidate> {
        val observed = normalizeSequence(rawSequence) ?: return emptyList()
        return banglaEntriesByFirst[observed.first()].orEmpty().asSequence()
            .filter { kotlin.math.abs(it.path.length - observed.length) <= MAX_PATH_LENGTH_DELTA }
            .mapNotNull { entry ->
                val pathScore = scorePath(observed, entry.path) ?: return@mapNotNull null
                GlideCandidate(
                    text = entry.text,
                    sourceRoman = entry.roman,
                    score = BASE_SCORE + entry.bonus + pathScore
                )
            }
            .distinctBy { it.text }
            .sortedWith(
                compareByDescending<GlideCandidate> { it.score }
                    .thenBy { it.sourceRoman.length }
                    .thenBy { it.sourceRoman }
            )
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    fun bestEnglish(rawSequence: String): GlideCandidate? = resolveEnglish(rawSequence, 1).firstOrNull()

    fun bestBangla(rawSequence: String): GlideCandidate? = resolveBangla(rawSequence, 1).firstOrNull()

    private fun scorePath(observed: String, candidate: String): Int? {
        if (candidate.length < 2) return null
        if (observed.first() != candidate.first()) return null

        val endpointPenalty = when {
            observed.last() == candidate.last() -> 0
            keyboardAdjacent(observed.last(), candidate.last()) -> FINAL_ENDPOINT_NEIGHBOR_PENALTY
            else -> return null
        }

        val lengthDelta = abs(observed.length - candidate.length)
        if (lengthDelta > MAX_PATH_LENGTH_DELTA) return null

        val maxCost = maxWeightedCost(observed, candidate)
        val distanceCost = weightedEditCost(observed, candidate, maxCost)
        if (distanceCost > maxCost) return null

        val coverageCount = orderedCoverage(observed, candidate)
        val coveragePercent = coverageCount * 100 / candidate.length.coerceAtLeast(1)
        if (coveragePercent < minimumCoveragePercent(candidate.length)) return null

        val exactTransitionBonus = matchingTransitions(observed, candidate) * 55
        return coveragePercent * 13 + exactTransitionBonus - distanceCost * 185 -
            lengthDelta * 48 - endpointPenalty
    }

    private fun normalizeSequence(raw: String): String? {
        val normalized = raw.lowercase().filter { it in 'a'..'z' }
        if (normalized.length < MIN_SEQUENCE_LENGTH) return null
        return compressRepeats(normalized)
    }

    private fun normalizeWordPath(word: String): String? {
        val normalized = word.lowercase().filter { it in 'a'..'z' }
        if (normalized.length < 2) return null
        return compressRepeats(normalized)
    }

    private fun maxWeightedCost(left: String, right: String): Int = when {
        maxOf(left.length, right.length) <= 4 -> 3
        maxOf(left.length, right.length) <= 7 -> 6
        maxOf(left.length, right.length) <= 10 -> 8
        else -> 10
    }

    /**
     * Weighted Levenshtein distance where crossing to a neighboring QWERTY key is
     * cheaper than an unrelated substitution. Costs are small integers so this
     * remains fast across the packaged lexicons.
     */
    private fun weightedEditCost(left: String, right: String, maxCost: Int): Int {
        if (abs(left.length - right.length) * INSERT_DELETE_COST > maxCost) return maxCost + 1
        var previous = IntArray(right.length + 1) { it * INSERT_DELETE_COST }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = (i + 1) * INSERT_DELETE_COST
            var rowMin = current[0]
            for (j in right.indices) {
                val substitutionCost = when {
                    left[i] == right[j] -> 0
                    keyboardAdjacent(left[i], right[j]) -> ADJACENT_SUBSTITUTION_COST
                    else -> FAR_SUBSTITUTION_COST
                }
                val substitution = previous[j] + substitutionCost
                val insertion = current[j] + INSERT_DELETE_COST
                val deletion = previous[j + 1] + INSERT_DELETE_COST
                current[j + 1] = minOf(substitution, insertion, deletion)
                rowMin = minOf(rowMin, current[j + 1])
            }
            if (rowMin > maxCost) return maxCost + 1
            previous = current
        }
        return previous[right.length]
    }

    /** Longest ordered key coverage; a glide should preserve most of the word path order. */
    private fun orderedCoverage(observed: String, candidate: String): Int {
        var previous = IntArray(candidate.length + 1)
        for (i in observed.indices) {
            val current = IntArray(candidate.length + 1)
            for (j in candidate.indices) {
                current[j + 1] = if (observed[i] == candidate[j]) {
                    previous[j] + 1
                } else {
                    maxOf(previous[j + 1], current[j])
                }
            }
            previous = current
        }
        return previous[candidate.length]
    }

    private fun matchingTransitions(observed: String, candidate: String): Int {
        if (observed.length < 2 || candidate.length < 2) return 0
        val candidateTransitions = candidate.windowed(2).toHashSet()
        return observed.windowed(2).count(candidateTransitions::contains)
    }

    private fun minimumCoveragePercent(candidateLength: Int): Int = when {
        candidateLength <= 4 -> 75
        candidateLength <= 7 -> 55
        else -> 50
    }

    private fun keyboardAdjacent(left: Char, right: Char): Boolean {
        if (left == right) return true
        val a = QWERTY_POSITIONS[left] ?: return false
        val b = QWERTY_POSITIONS[right] ?: return false
        return hypot(a.first - b.first, a.second - b.second) <= QWERTY_NEIGHBOR_DISTANCE
    }

    private fun compressRepeats(value: String): String = buildString(value.length) {
        var previous: Char? = null
        value.forEach { char ->
            if (char != previous) append(char)
            previous = char
        }
    }

    private companion object {
        const val MIN_SEQUENCE_LENGTH = 3
        const val MAX_PATH_LENGTH_DELTA = 5
        const val BASE_SCORE = 6_000
        const val INSERT_DELETE_COST = 2
        const val ADJACENT_SUBSTITUTION_COST = 1
        const val FAR_SUBSTITUTION_COST = 3
        const val FINAL_ENDPOINT_NEIGHBOR_PENALTY = 260
        const val QWERTY_NEIGHBOR_DISTANCE = 1.55

        val QWERTY_POSITIONS: Map<Char, Pair<Double, Double>> = buildMap {
            "qwertyuiop".forEachIndexed { index, char -> put(char, index.toDouble() to 0.0) }
            "asdfghjkl".forEachIndexed { index, char -> put(char, (index + 0.5) to 1.0) }
            "zxcvbnm".forEachIndexed { index, char -> put(char, (index + 1.0) to 2.0) }
        }
    }
}
