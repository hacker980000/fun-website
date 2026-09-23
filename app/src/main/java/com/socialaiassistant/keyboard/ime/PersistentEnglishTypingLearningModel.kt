package com.socialaiassistant.keyboard.ime

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.util.Base64

data class EnglishTypingLearningStats(
    val learnedWords: Int,
    val learnedTransitions: Int
)

/** Bounded device-local English personalization. No network or AI-training integration. */
class PersistentEnglishTypingLearningModel(context: Context) : EnglishTypingLearningModel {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val words = linkedMapOf<String, Int>()
    private val transitions = linkedMapOf<Pair<String, String>, Int>()
    private var dirty = false
    @Volatile private var lastOwnRevision: Long = -1L
    private val flushController = DeferredFlushController("english-typing-persist") { persistDirtyNow() }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_REVISION) {
            val revision = preferences.getLong(KEY_REVISION, 0L)
            if (revision != lastOwnRevision) synchronized(this) { load() }
        }
    }

    init {
        load()
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    @Synchronized
    override fun recordWord(word: String) {
        val normalized = normalizeEnglishToken(word) ?: return
        increment(words, normalized, MAX_WORDS)
    }

    @Synchronized
    override fun recordTransition(previous: String, next: String) {
        val left = normalizeEnglishToken(previous) ?: return
        val right = normalizeEnglishToken(next) ?: return
        increment(transitions, left to right, MAX_TRANSITIONS)
    }

    @Synchronized
    override fun wordBoost(word: String): Int =
        (words[normalizeEnglishToken(word)] ?: 0).coerceAtMost(20) * 14

    @Synchronized
    override fun transitionBoost(previous: String, next: String): Int =
        (transitions[normalizeEnglishToken(previous) to normalizeEnglishToken(next)] ?: 0).coerceAtMost(20) * 45

    @Synchronized
    override fun transitionCandidates(previous: String, limit: Int): List<LearnedNextWordCandidate> {
        val normalized = normalizeEnglishToken(previous) ?: return emptyList()
        return transitions.asSequence()
            .filter { it.key.first == normalized }
            .map { LearnedNextWordCandidate(it.key.second, it.value) }
            .sortedWith(compareByDescending<LearnedNextWordCandidate> { it.weight }.thenBy { it.text })
            .take(limit.coerceAtLeast(0))
            .toList()
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
        val revision = preferences.getLong(KEY_REVISION, 0L) + 1L
        lastOwnRevision = revision
        dirty = false
        preferences.edit()
            .putStringSet(KEY_WORDS, encodeWords(words))
            .putStringSet(KEY_TRANSITIONS, encodePairs(transitions))
            .putLong(KEY_REVISION, revision)
            .apply()
    }

    override fun clearSession() = Unit

    @Synchronized
    fun stats(): EnglishTypingLearningStats = EnglishTypingLearningStats(words.size, transitions.size)

    @Synchronized
    private fun load() {
        words.clear()
        transitions.clear()
        decodeWords(preferences.getStringSet(KEY_WORDS, emptySet()).orEmpty()).take(MAX_WORDS).forEach { (key, value) ->
            words[key] = value.coerceIn(1, MAX_COUNT)
        }
        decodePairs(preferences.getStringSet(KEY_TRANSITIONS, emptySet()).orEmpty()).take(MAX_TRANSITIONS).forEach { (key, value) ->
            transitions[key] = value.coerceIn(1, MAX_COUNT)
        }
        dirty = false
    }

    private fun <K> increment(map: LinkedHashMap<K, Int>, key: K, maxEntries: Int) {
        if (!map.containsKey(key) && map.size >= maxEntries) {
            val weakest = map.entries.minWithOrNull(compareBy<Map.Entry<K, Int>> { it.value }.thenBy { it.key.toString() })
            if (weakest != null) map.remove(weakest.key)
        }
        map[key] = ((map[key] ?: 0) + 1).coerceAtMost(MAX_COUNT)
        dirty = true
    }

    companion object {
        private const val PREFS_NAME = "english_typing_personalization_v1"
        private const val KEY_WORDS = "word_counts"
        private const val KEY_TRANSITIONS = "transition_counts"
        private const val KEY_REVISION = "revision"
        private const val MAX_WORDS = 512
        private const val MAX_TRANSITIONS = 1_024
        private const val MAX_COUNT = 1_000
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()

        fun clearStoredLearning(context: Context) {
            val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val revision = preferences.getLong(KEY_REVISION, 0L) + 1L
            preferences.edit()
                .remove(KEY_WORDS)
                .remove(KEY_TRANSITIONS)
                .putLong(KEY_REVISION, revision)
                .apply()
        }

        fun storedStats(context: Context): EnglishTypingLearningStats {
            val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return EnglishTypingLearningStats(
                learnedWords = decodeWords(preferences.getStringSet(KEY_WORDS, emptySet()).orEmpty()).take(MAX_WORDS).count(),
                learnedTransitions = decodePairs(preferences.getStringSet(KEY_TRANSITIONS, emptySet()).orEmpty()).take(MAX_TRANSITIONS).count()
            )
        }

        private fun encodeWords(values: Map<String, Int>): Set<String> = values.mapTo(linkedSetOf()) { (word, count) ->
            "${encode(word)}:$count"
        }

        private fun decodeWords(values: Set<String>): Sequence<Pair<String, Int>> = values.asSequence().mapNotNull { row ->
            val split = row.lastIndexOf(':')
            if (split <= 0) return@mapNotNull null
            val word = decode(row.substring(0, split)) ?: return@mapNotNull null
            val count = row.substring(split + 1).toIntOrNull() ?: return@mapNotNull null
            normalizeEnglishToken(word)?.let { it to count }
        }

        private fun encodePairs(values: Map<Pair<String, String>, Int>): Set<String> = values.mapTo(linkedSetOf()) { (pair, count) ->
            "${encode(pair.first)}.${encode(pair.second)}:$count"
        }

        private fun decodePairs(values: Set<String>): Sequence<Pair<Pair<String, String>, Int>> = values.asSequence().mapNotNull { row ->
            val split = row.lastIndexOf(':')
            if (split <= 0) return@mapNotNull null
            val payload = row.substring(0, split)
            val dot = payload.indexOf('.')
            if (dot <= 0) return@mapNotNull null
            val left = decode(payload.substring(0, dot)) ?: return@mapNotNull null
            val right = decode(payload.substring(dot + 1)) ?: return@mapNotNull null
            val count = row.substring(split + 1).toIntOrNull() ?: return@mapNotNull null
            val normalizedLeft = normalizeEnglishToken(left) ?: return@mapNotNull null
            val normalizedRight = normalizeEnglishToken(right) ?: return@mapNotNull null
            (normalizedLeft to normalizedRight) to count
        }

        private fun encode(value: String): String = encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        private fun decode(value: String): String? = runCatching {
            String(decoder.decode(value), StandardCharsets.UTF_8)
        }.getOrNull()
    }
}
