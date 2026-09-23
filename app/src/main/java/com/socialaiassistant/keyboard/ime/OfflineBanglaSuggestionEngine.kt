package com.socialaiassistant.keyboard.ime

/** Where an offline candidate came from. Kept Android-free for fast tests. */
enum class SuggestionKind {
    EXACT,
    ALIAS,
    PREFIX,
    TYPO,
    PERSONAL,
    FALLBACK,
    NEXT_WORD
}

data class BanglaSuggestionCandidate(
    val text: String,
    val sourceRoman: String,
    val kind: SuggestionKind,
    val score: Int
)

data class BanglaSuggestionSnapshot(
    val roman: String,
    val candidates: List<BanglaSuggestionCandidate>,
    /** Conservative replacement used only when the user commits with a word boundary. */
    val autocorrectText: String? = null
)

/**
 * Stage-4 offline candidate engine.
 *
 * Improvements over Stage 2/3:
 * - indexed exact/prefix/typo lookup instead of full dictionary scans;
 * - expanded Stage-3 production lexicon + frequency priors;
 * - existing conservative autocorrect contract remains unchanged;
 * - deterministic ordering for stable UI/tests;
 * - persistent personal candidate boosts and conservative trusted personal exact correction.
 */
class OfflineBanglaSuggestionEngine(
    private val index: BanglaLexiconIndex = BanglaLexiconIndex(),
    private val aliases: Map<String, String> = CommonBanglaTypingAliases.aliasToCanonical,
    private val learning: LocalTypingLearningModel = InMemoryTypingLearningModel()
) {
    fun suggest(roman: String, renderedFallback: String, limit: Int = 3): BanglaSuggestionSnapshot {
        if (roman.isBlank() || limit <= 0) return BanglaSuggestionSnapshot(roman, emptyList())
        val normalized = roman.lowercase()
        val scored = mutableListOf<BanglaSuggestionCandidate>()

        index.exact(normalized)?.let { exact ->
            scored += BanglaSuggestionCandidate(
                text = exact.bangla,
                sourceRoman = exact.roman,
                kind = SuggestionKind.EXACT,
                score = 2_000 + exact.bonus + learning.wordBoost(exact.bangla)
            )
        }

        val aliasCanonical = aliases[normalized]
        val aliasEntry = aliasCanonical?.let(index::exact)
        val aliasText = aliasEntry?.bangla
        if (aliasEntry != null && aliasText != null) {
            scored += BanglaSuggestionCandidate(
                text = aliasText,
                sourceRoman = aliasEntry.roman,
                kind = SuggestionKind.ALIAS,
                score = 1_900 + aliasEntry.bonus + learning.wordBoost(aliasEntry.bangla)
            )
        }

        index.prefix(normalized, PREFIX_SCAN_LIMIT).forEach { entry ->
            val completionCost = (entry.roman.length - normalized.length).coerceAtLeast(0)
            scored += BanglaSuggestionCandidate(
                text = entry.bangla,
                sourceRoman = entry.roman,
                kind = SuggestionKind.PREFIX,
                score = 1_350 + entry.bonus + learning.wordBoost(entry.bangla) - completionCost * 7
            )
        }

        if (normalized.length >= MIN_TYPO_LENGTH) {
            val maxDistance = if (normalized.length >= 8) 2 else 1
            index.typoCandidates(normalized, maxDistance, TYPO_INDEX_SCAN_LIMIT)
                .mapNotNull { entry ->
                    if (entry.roman == normalized) return@mapNotNull null
                    val distance = boundedDamerauEditDistance(normalized, entry.roman, maxDistance)
                    if (distance > maxDistance) return@mapNotNull null
                    BanglaSuggestionCandidate(
                        text = entry.bangla,
                        sourceRoman = entry.roman,
                        kind = SuggestionKind.TYPO,
                        score = 980 + entry.bonus + learning.wordBoost(entry.bangla) - distance * 150 - kotlin.math.abs(entry.roman.length - normalized.length) * 8
                    )
                }
                .sortedWith(candidateComparator)
                .take(TYPO_RESULT_LIMIT)
                .forEach(scored::add)
        }

        learning.phoneticCandidates(normalized, PERSONAL_SCAN_LIMIT).forEach { learned ->
            val exactPersonal = learned.roman == normalized
            val completionCost = (learned.roman.length - normalized.length).coerceAtLeast(0)
            scored += BanglaSuggestionCandidate(
                text = learned.text,
                sourceRoman = learned.roman,
                kind = SuggestionKind.PERSONAL,
                score = (if (exactPersonal) 2_050 else 1_430) +
                    learned.weight.coerceAtMost(30) * 18 - completionCost * 7
            )
        }

        if (renderedFallback.isNotBlank()) {
            val hasExactOrAlias = scored.any { it.kind == SuggestionKind.EXACT || it.kind == SuggestionKind.ALIAS }
            val fallbackScore = if (hasExactOrAlias) 850 else 1_150
            scored += BanglaSuggestionCandidate(
                text = renderedFallback,
                sourceRoman = roman,
                kind = SuggestionKind.FALLBACK,
                score = fallbackScore
            )
        }

        val ranked = scored
            .sortedWith(candidateComparator)
            .distinctBy { it.text }
        val deduped = ranked.take(limit)
        val typoRanked = ranked.filter { it.kind == SuggestionKind.TYPO }
        val topTypo = typoRanked.firstOrNull()
        val runnerUpTypo = typoRanked.getOrNull(1)
        val safeTypoText = topTypo?.takeIf { candidate ->
            !index.containsBangla(renderedFallback) &&
                PredictionAutocorrectPolicy.isConfidentBanglaTypo(
                    tokenLength = normalized.length,
                    topScore = candidate.score,
                    runnerUpScore = runnerUpTypo?.score
                )
        }?.text

        return BanglaSuggestionSnapshot(
            roman = roman,
            candidates = deduped,
            autocorrectText = aliasText ?: safeTypoText
        )
    }

    fun commitText(roman: String, renderedFallback: String): String {
        val snapshot = suggest(roman, renderedFallback, limit = 3)
        val normalized = roman.lowercase()
        return snapshot.autocorrectText
            ?: learning.trustedPhoneticText(normalized, TRUSTED_PERSONAL_WEIGHT)
            ?: index.exact(normalized)?.bangla
            ?: renderedFallback
    }

    fun dictionarySize(): Int = index.size()

    private fun boundedDamerauEditDistance(a: String, b: String, maxDistance: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > maxDistance) return maxDistance + 1
        if (a == b) return 0
        if (a.length == b.length && isSingleAdjacentTransposition(a, b)) return 1
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            var rowMin = current[0]
            for (j in b.indices) {
                val substitution = previous[j] + if (a[i] == b[j]) 0 else 1
                val insertion = current[j] + 1
                val deletion = previous[j + 1] + 1
                current[j + 1] = minOf(substitution, insertion, deletion)
                rowMin = minOf(rowMin, current[j + 1])
            }
            if (rowMin > maxDistance) return maxDistance + 1
            previous = current
        }
        return previous[b.length]
    }

    private fun isSingleAdjacentTransposition(a: String, b: String): Boolean {
        var firstMismatch = -1
        for (index in a.indices) {
            if (a[index] == b[index]) continue
            if (firstMismatch == -1) {
                firstMismatch = index
                continue
            }
            return index == firstMismatch + 1 &&
                a[firstMismatch] == b[index] &&
                a[index] == b[firstMismatch] &&
                a.substring(index + 1) == b.substring(index + 1)
        }
        return false
    }

    private companion object {
        const val MIN_TYPO_LENGTH = 3
        const val PREFIX_SCAN_LIMIT = 18
        const val TYPO_INDEX_SCAN_LIMIT = 120
        const val TYPO_RESULT_LIMIT = 10
        const val PERSONAL_SCAN_LIMIT = 12
        const val TRUSTED_PERSONAL_WEIGHT = 9

        val candidateComparator = compareByDescending<BanglaSuggestionCandidate> { it.score }
            .thenByDescending { PredictionRankingPolicy.banglaKindPriority(it.kind) }
            .thenBy { it.sourceRoman.length }
            .thenBy { it.sourceRoman }
            .thenBy { it.text }
    }
}

/**
 * Explicit Banglish shorthand aliases safe enough for automatic replacement on Space.
 * These are intentionally conservative; fuzzy edit-distance candidates are never auto-applied.
 */
object CommonBanglaTypingAliases {
    val aliasToCanonical: Map<String, String> = buildMap {
        putAll(mapOf(
        "amr" to "amar",
        "amke" to "amake",
        "amdr" to "amader",
        "tmr" to "tomar",
        "tmke" to "tomake",
        "apnr" to "apnar",
        "apnke" to "apnake",
        "kmn" to "kemon",
        "kno" to "keno",
        "kivbe" to "kivabe",
        "kthy" to "kothay",
        "ekhn" to "ekhon",
        "smoy" to "somoy",
        "krbo" to "korbo",
        "krchi" to "korchi",
        "krte" to "korte",
        "hbe" to "hobe",
        "hcche" to "hocche",
        "jbo" to "jabo",
        "jcci" to "jacchi",
        "blbo" to "bolbo",
        "blchi" to "bolchi",
        "dhnnobad" to "dhonnobad",
        "ashchi" to "ashchi",
        "dkr" to "dorkar",
        "smssa" to "somossa",
        "dhnbd" to "dhonnobad"
        ))
        putAll(Stage5BanglaTypingAliases.aliasToCanonical)
    }
}

/** Frequency prior retained for Stage-1/2 compatibility and merged by ProductionBanglaLexicon. */
object CommonBanglaSuggestionPriority {
    val frequencyBonus: Map<String, Int> = mapOf(
        "ami" to 120,
        "amar" to 115,
        "amake" to 100,
        "amra" to 95,
        "tumi" to 120,
        "tomar" to 115,
        "tomake" to 100,
        "apni" to 120,
        "apnar" to 110,
        "ki" to 125,
        "kemon" to 115,
        "keno" to 105,
        "acho" to 115,
        "achi" to 110,
        "valo" to 110,
        "bhalo" to 108,
        "khub" to 105,
        "onek" to 100,
        "ekhon" to 105,
        "aj" to 105,
        "kal" to 100,
        "kaj" to 100,
        "hobe" to 110,
        "hoy" to 105,
        "korbo" to 100,
        "korchi" to 95,
        "jabo" to 95,
        "jani" to 95,
        "na" to 120,
        "hya" to 100,
        "thik" to 100,
        "accha" to 100,
        "dhonnobad" to 90,
        "bangla" to 90,
        "bangladesh" to 85,
        "vai" to 90,
        "bhai" to 90
    )
}
