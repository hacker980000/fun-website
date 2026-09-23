package com.socialaiassistant.keyboard.ime

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.util.Base64

data class TypingLearningStats(
    val learnedWords: Int,
    val learnedTransitions: Int,
    val learnedPhoneticPairs: Int,
    val learnedContexts: Int = 0
)

/**
 * Stage-4 bounded, device-local typing personalization store.
 *
 * No learned typing data is uploaded or mixed with AI training. The store keeps only
 * normalized word counts, word-pair counts and roman-to-rendered phonetic pairs.
 * Sensitive fields never call this model because the IME disables learning for them.
 */
class PersistentTypingLearningModel(context: Context) : LocalTypingLearningModel {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val wordCounts = linkedMapOf<String, Int>()
    private val transitionCounts = linkedMapOf<Pair<String, String>, Int>()
    private val contextTransitionCounts = linkedMapOf<Triple<String, String, String>, Int>()
    private val phoneticCounts = linkedMapOf<Pair<String, String>, Int>()
    private var dirty = false
    @Volatile private var lastOwnRevision: Long = -1L
    private val flushController = DeferredFlushController("bangla-typing-persist") { persistDirtyNow() }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_REVISION) {
            val revision = preferences.getLong(KEY_REVISION, 0L)
            if (revision != lastOwnRevision) synchronized(this) { loadFromPreferences() }
        }
    }

    init {
        loadFromPreferences()
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    @Synchronized
    override fun recordWord(word: String) {
        val normalized = normalizeBanglaToken(word) ?: return
        increment(wordCounts, normalized, MAX_WORDS)
    }

    @Synchronized
    override fun recordTransition(previous: String, next: String) {
        val left = normalizeBanglaToken(previous) ?: return
        val right = normalizeBanglaToken(next) ?: return
        increment(transitionCounts, left to right, MAX_TRANSITIONS)
    }

    @Synchronized
    override fun recordContextTransition(previousPrevious: String, previous: String, next: String) {
        val first = normalizeBanglaToken(previousPrevious) ?: return
        val second = normalizeBanglaToken(previous) ?: return
        val third = normalizeBanglaToken(next) ?: return
        increment(contextTransitionCounts, Triple(first, second, third), MAX_CONTEXT_TRANSITIONS)
    }

    @Synchronized
    override fun recordPhonetic(roman: String, text: String, weight: Int) {
        val normalizedRoman = normalizeRomanLearningKey(roman) ?: return
        val normalizedText = text.trim().takeIf { it.isNotBlank() && it.length <= MAX_TEXT_LENGTH } ?: return
        val key = normalizedRoman to normalizedText
        val amount = weight.coerceIn(1, 20)
        if (!phoneticCounts.containsKey(key) && phoneticCounts.size >= MAX_PHONETIC_PAIRS) {
            removeWeakestPair(phoneticCounts)
        }
        phoneticCounts[key] = ((phoneticCounts[key] ?: 0) + amount).coerceAtMost(MAX_COUNT)
        dirty = true
    }

    @Synchronized
    override fun wordBoost(word: String): Int = (wordCounts[word] ?: 0).coerceAtMost(20) * 14

    @Synchronized
    override fun transitionBoost(previous: String, next: String): Int =
        (transitionCounts[previous to next] ?: 0).coerceAtMost(20) * 45

    @Synchronized
    override fun contextTransitionBoost(previousPrevious: String, previous: String, next: String): Int =
        (contextTransitionCounts[Triple(previousPrevious, previous, next)] ?: 0).coerceAtMost(20) * 90

    @Synchronized
    override fun transitionCandidates(previous: String, limit: Int): List<LearnedNextWordCandidate> {
        if (limit <= 0) return emptyList()
        val normalized = normalizeBanglaToken(previous) ?: return emptyList()
        return transitionCounts.asSequence()
            .filter { (key, _) -> key.first == normalized }
            .map { (key, count) -> LearnedNextWordCandidate(key.second, count) }
            .sortedWith(compareByDescending<LearnedNextWordCandidate> { it.weight }.thenBy { it.text })
            .take(limit)
            .toList()
    }

    @Synchronized
    override fun contextCandidates(previousPrevious: String, previous: String, limit: Int): List<LearnedContextCandidate> {
        if (limit <= 0) return emptyList()
        val first = normalizeBanglaToken(previousPrevious) ?: return emptyList()
        val second = normalizeBanglaToken(previous) ?: return emptyList()
        return contextTransitionCounts.asSequence()
            .filter { (key, _) -> key.first == first && key.second == second }
            .map { (key, count) -> LearnedContextCandidate(key.third, count) }
            .sortedWith(compareByDescending<LearnedContextCandidate> { it.weight }.thenBy { it.text })
            .take(limit)
            .toList()
    }

    @Synchronized
    override fun phoneticCandidates(romanPrefix: String, limit: Int): List<LearnedPhoneticCandidate> {
        if (limit <= 0) return emptyList()
        val prefix = normalizeRomanLearningKey(romanPrefix) ?: return emptyList()
        return phoneticCounts.asSequence()
            .filter { (key, _) -> key.first.startsWith(prefix) }
            .map { (key, count) -> LearnedPhoneticCandidate(key.first, key.second, count) }
            .sortedWith(compareByDescending<LearnedPhoneticCandidate> { it.weight }.thenBy { it.roman.length }.thenBy { it.roman })
            .take(limit)
            .toList()
    }

    @Synchronized
    override fun trustedPhoneticText(roman: String, minimumWeight: Int): String? {
        val normalized = normalizeRomanLearningKey(roman) ?: return null
        return phoneticCounts.asSequence()
            .filter { (key, count) -> key.first == normalized && count >= minimumWeight }
            .maxWithOrNull(compareBy<Map.Entry<Pair<String, String>, Int>> { it.value }.thenBy { it.key.second })
            ?.key?.second
    }

    override fun requestFlush() = flushController.request()

    fun requestImmediateFlush() = flushController.request(delayMs = 0L)

    override fun flush() {
        flushController.cancelPending()
        persistDirtyNow()
    }

    override fun close() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
        flushController.closeAndFlush()
    }

    @Synchronized
    private fun persistDirtyNow() {
        if (!dirty) return
        val nextRevision = preferences.getLong(KEY_REVISION, 0L) + 1L
        lastOwnRevision = nextRevision
        dirty = false
        preferences.edit()
            .putStringSet(KEY_WORDS, encodeWords(wordCounts))
            .putStringSet(KEY_TRANSITIONS, encodePairs(transitionCounts))
            .putStringSet(KEY_CONTEXTS, encodeTriples(contextTransitionCounts))
            .putStringSet(KEY_PHONETICS, encodePairs(phoneticCounts))
            .putLong(KEY_REVISION, nextRevision)
            .apply()
    }

    /** Persistent learning survives IME sessions; resetting a session must not erase it. */
    override fun clearSession() = Unit

    @Synchronized
    fun stats(): TypingLearningStats = TypingLearningStats(
        learnedWords = wordCounts.size,
        learnedTransitions = transitionCounts.size,
        learnedPhoneticPairs = phoneticCounts.size,
        learnedContexts = contextTransitionCounts.size
    )

    @Synchronized
    private fun loadFromPreferences() {
        wordCounts.clear()
        transitionCounts.clear()
        contextTransitionCounts.clear()
        phoneticCounts.clear()
        decodeWords(preferences.getStringSet(KEY_WORDS, emptySet()).orEmpty()).take(MAX_WORDS).forEach { (key, count) ->
            wordCounts[key] = count.coerceIn(1, MAX_COUNT)
        }
        decodePairs(preferences.getStringSet(KEY_TRANSITIONS, emptySet()).orEmpty()).take(MAX_TRANSITIONS).forEach { (key, count) ->
            transitionCounts[key] = count.coerceIn(1, MAX_COUNT)
        }
        decodeTriples(preferences.getStringSet(KEY_CONTEXTS, emptySet()).orEmpty()).take(MAX_CONTEXT_TRANSITIONS).forEach { (key, count) ->
            contextTransitionCounts[key] = count.coerceIn(1, MAX_COUNT)
        }
        decodePairs(preferences.getStringSet(KEY_PHONETICS, emptySet()).orEmpty()).take(MAX_PHONETIC_PAIRS).forEach { (key, count) ->
            phoneticCounts[key] = count.coerceIn(1, MAX_COUNT)
        }
        dirty = false
    }

    private fun <K> increment(map: LinkedHashMap<K, Int>, key: K, maxEntries: Int) {
        if (!map.containsKey(key) && map.size >= maxEntries) removeWeakest(map)
        map[key] = ((map[key] ?: 0) + 1).coerceAtMost(MAX_COUNT)
        dirty = true
    }

    private fun <K> removeWeakest(map: LinkedHashMap<K, Int>) {
        val weakest = map.entries.minWithOrNull(compareBy<Map.Entry<K, Int>> { it.value }.thenBy { it.key.toString() })
        if (weakest != null) map.remove(weakest.key)
    }

    private fun removeWeakestPair(map: LinkedHashMap<Pair<String, String>, Int>) = removeWeakest(map)

    companion object {
        private const val PREFS_NAME = "typing_personalization_v1"
        private const val KEY_WORDS = "word_counts"
        private const val KEY_TRANSITIONS = "transition_counts"
        private const val KEY_CONTEXTS = "context_transition_counts"
        private const val KEY_PHONETICS = "phonetic_counts"
        private const val KEY_REVISION = "revision"
        private const val MAX_WORDS = 512
        private const val MAX_TRANSITIONS = 1_024
        private const val MAX_CONTEXT_TRANSITIONS = 1_024
        private const val MAX_PHONETIC_PAIRS = 512
        private const val MAX_COUNT = 1_000
        private const val MAX_TEXT_LENGTH = 64
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()

        fun clearStoredLearning(context: Context) {
            val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextRevision = preferences.getLong(KEY_REVISION, 0L) + 1L
            preferences.edit()
                .remove(KEY_WORDS)
                .remove(KEY_TRANSITIONS)
                .remove(KEY_CONTEXTS)
                .remove(KEY_PHONETICS)
                .putLong(KEY_REVISION, nextRevision)
                .apply()
        }

        fun storedStats(context: Context): TypingLearningStats {
            val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return TypingLearningStats(
                learnedWords = preferences.getStringSet(KEY_WORDS, emptySet()).orEmpty().size.coerceAtMost(MAX_WORDS),
                learnedTransitions = preferences.getStringSet(KEY_TRANSITIONS, emptySet()).orEmpty().size.coerceAtMost(MAX_TRANSITIONS),
                learnedPhoneticPairs = preferences.getStringSet(KEY_PHONETICS, emptySet()).orEmpty().size.coerceAtMost(MAX_PHONETIC_PAIRS),
                learnedContexts = preferences.getStringSet(KEY_CONTEXTS, emptySet()).orEmpty().size.coerceAtMost(MAX_CONTEXT_TRANSITIONS)
            )
        }

        private fun encodeWords(map: Map<String, Int>): Set<String> = map.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX_WORDS)
            .mapTo(linkedSetOf()) { entry -> "${encode(entry.key)}|${entry.value}" }

        private fun encodePairs(map: Map<Pair<String, String>, Int>): Set<String> = map.entries
            .sortedWith(compareByDescending<Map.Entry<Pair<String, String>, Int>> { it.value }.thenBy { it.key.first }.thenBy { it.key.second })
            .mapTo(linkedSetOf()) { entry -> "${encode(entry.key.first)}|${encode(entry.key.second)}|${entry.value}" }

        private fun decodeWords(entries: Set<String>): List<Pair<String, Int>> = entries.mapNotNull { raw ->
            val parts = raw.split('|')
            if (parts.size != 2) return@mapNotNull null
            val word = decode(parts[0]) ?: return@mapNotNull null
            val count = parts[1].toIntOrNull() ?: return@mapNotNull null
            word to count
        }.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

        private fun decodePairs(entries: Set<String>): List<Pair<Pair<String, String>, Int>> = entries.mapNotNull { raw ->
            val parts = raw.split('|')
            if (parts.size != 3) return@mapNotNull null
            val first = decode(parts[0]) ?: return@mapNotNull null
            val second = decode(parts[1]) ?: return@mapNotNull null
            val count = parts[2].toIntOrNull() ?: return@mapNotNull null
            (first to second) to count
        }.sortedWith(compareByDescending<Pair<Pair<String, String>, Int>> { it.second }.thenBy { it.first.first }.thenBy { it.first.second })

        private fun encodeTriples(map: Map<Triple<String, String, String>, Int>): Set<String> = map.entries
            .sortedWith(compareByDescending<Map.Entry<Triple<String, String, String>, Int>> { it.value }
                .thenBy { it.key.first }.thenBy { it.key.second }.thenBy { it.key.third })
            .take(MAX_CONTEXT_TRANSITIONS)
            .mapTo(linkedSetOf()) { entry ->
                "${encode(entry.key.first)}|${encode(entry.key.second)}|${encode(entry.key.third)}|${entry.value}"
            }

        private fun decodeTriples(entries: Set<String>): List<Pair<Triple<String, String, String>, Int>> = entries.mapNotNull { raw ->
            val parts = raw.split('|')
            if (parts.size != 4) return@mapNotNull null
            val first = decode(parts[0]) ?: return@mapNotNull null
            val second = decode(parts[1]) ?: return@mapNotNull null
            val third = decode(parts[2]) ?: return@mapNotNull null
            val count = parts[3].toIntOrNull() ?: return@mapNotNull null
            Triple(first, second, third) to count
        }.sortedWith(compareByDescending<Pair<Triple<String, String, String>, Int>> { it.second }
            .thenBy { it.first.first }.thenBy { it.first.second }.thenBy { it.first.third })

        private fun encode(value: String): String = encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(value: String): String? = runCatching {
            String(decoder.decode(value), StandardCharsets.UTF_8)
        }.getOrNull()
    }
}
