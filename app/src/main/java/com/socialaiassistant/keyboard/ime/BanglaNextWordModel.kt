package com.socialaiassistant.keyboard.ime

/** A next-word candidate produced entirely on-device. */
data class NextWordPrediction(
    val text: String,
    val score: Int
)

/** Stage-4 personalization contract shared by in-memory tests and the persistent device-local store. */
data class LearnedPhoneticCandidate(
    val roman: String,
    val text: String,
    val weight: Int
)

data class LearnedNextWordCandidate(
    val text: String,
    val weight: Int
)

data class LearnedContextCandidate(
    val text: String,
    val weight: Int
)

interface LocalTypingLearningModel {
    fun recordWord(word: String)
    fun recordTransition(previous: String, next: String)
    fun recordContextTransition(previousPrevious: String, previous: String, next: String) = Unit
    fun recordPhonetic(roman: String, text: String, weight: Int = 1)
    fun wordBoost(word: String): Int
    fun transitionBoost(previous: String, next: String): Int
    fun contextTransitionBoost(previousPrevious: String, previous: String, next: String): Int = 0
    fun transitionCandidates(previous: String, limit: Int): List<LearnedNextWordCandidate>
    fun contextCandidates(previousPrevious: String, previous: String, limit: Int): List<LearnedContextCandidate> = emptyList()
    fun phoneticCandidates(romanPrefix: String, limit: Int): List<LearnedPhoneticCandidate>
    fun trustedPhoneticText(roman: String, minimumWeight: Int = 9): String?
    fun flush()
    fun requestFlush() = flush()
    fun close() = flush()
    fun clearSession()
}

class InMemoryTypingLearningModel : LocalTypingLearningModel {
    private val wordCounts = linkedMapOf<String, Int>()
    private val transitionCounts = linkedMapOf<Pair<String, String>, Int>()
    private val contextTransitionCounts = linkedMapOf<Triple<String, String, String>, Int>()
    private val phoneticCounts = linkedMapOf<Pair<String, String>, Int>()

    override fun recordWord(word: String) {
        val normalized = normalizeBanglaToken(word) ?: return
        wordCounts[normalized] = ((wordCounts[normalized] ?: 0) + 1).coerceAtMost(MAX_COUNT)
    }

    override fun recordTransition(previous: String, next: String) {
        val left = normalizeBanglaToken(previous) ?: return
        val right = normalizeBanglaToken(next) ?: return
        val key = left to right
        transitionCounts[key] = ((transitionCounts[key] ?: 0) + 1).coerceAtMost(MAX_COUNT)
    }

    override fun recordContextTransition(previousPrevious: String, previous: String, next: String) {
        val first = normalizeBanglaToken(previousPrevious) ?: return
        val second = normalizeBanglaToken(previous) ?: return
        val third = normalizeBanglaToken(next) ?: return
        val key = Triple(first, second, third)
        contextTransitionCounts[key] = ((contextTransitionCounts[key] ?: 0) + 1).coerceAtMost(MAX_COUNT)
    }

    override fun recordPhonetic(roman: String, text: String, weight: Int) {
        val normalizedRoman = normalizeRomanLearningKey(roman) ?: return
        val normalizedText = text.trim().takeIf { it.isNotBlank() } ?: return
        val key = normalizedRoman to normalizedText
        phoneticCounts[key] = ((phoneticCounts[key] ?: 0) + weight.coerceAtLeast(1)).coerceAtMost(MAX_COUNT)
    }

    override fun wordBoost(word: String): Int = (wordCounts[word] ?: 0).coerceAtMost(20) * 14

    override fun transitionBoost(previous: String, next: String): Int =
        (transitionCounts[previous to next] ?: 0).coerceAtMost(20) * 45

    override fun contextTransitionBoost(previousPrevious: String, previous: String, next: String): Int =
        (contextTransitionCounts[Triple(previousPrevious, previous, next)] ?: 0).coerceAtMost(20) * 90

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

    override fun trustedPhoneticText(roman: String, minimumWeight: Int): String? {
        val normalized = normalizeRomanLearningKey(roman) ?: return null
        return phoneticCounts.asSequence()
            .filter { (key, count) -> key.first == normalized && count >= minimumWeight }
            .maxWithOrNull(compareBy<Map.Entry<Pair<String, String>, Int>> { it.value }.thenBy { it.key.second })
            ?.key?.second
    }

    override fun flush() = Unit

    override fun clearSession() {
        wordCounts.clear()
        transitionCounts.clear()
        contextTransitionCounts.clear()
        phoneticCounts.clear()
    }

    private companion object {
        const val MAX_COUNT = 1_000
    }
}

/**
 * Small built-in conversational corpus used to derive deterministic offline bigram priors.
 * It contains no user data and never leaves the device.
 */
object BanglaConversationCorpus {
    val sentences: List<String> = listOf(
        "আমি ভালো আছি", "আমি এখন বাসায় আছি", "আমি এখন কাজে আছি", "আমি পরে কথা বলবো",
        "আমি তোমাকে পরে বলবো", "আমি আজ বাসায় থাকবো", "আমি কাল যাবো", "আমি এখন আসছি",
        "আমি একটু পরে আসছি", "আমি জানি না", "আমি বুঝতে পারছি", "আমি চেষ্টা করছি",
        "আমি কাজ করছি", "আমি খেতে যাচ্ছি", "আমি ঘুমাতে যাচ্ছি", "আমি তোমার সাথে কথা বলবো",
        "আমি আপনার সাথে কথা বলবো", "আমি এটা করতে পারবো", "আমি এটা দেখে বলবো", "আমি এখন ফ্রি আছি",
        "আমি এখন বিজি আছি", "আমি মেসেজ দেখেছি", "আমি কল দিচ্ছি", "আমি পরে কল দিবো",
        "তুমি কেমন আছো", "তুমি এখন কোথায়", "তুমি কি করছো", "তুমি কি আসবে",
        "তুমি কখন আসবে", "তুমি বাসায় আছো", "তুমি ভালো থেকো", "তুমি পরে জানিও",
        "তুমি আমাকে বলো", "তুমি আমাকে কল দিও", "তুমি এখন ফ্রি আছো", "তুমি কি খেয়েছো",
        "আপনি কেমন আছেন", "আপনি এখন কোথায় আছেন", "আপনি কি আসবেন", "আপনি একটু অপেক্ষা করুন",
        "আপনি আমাকে জানাবেন", "আপনি পরে কল করবেন", "আপনি ভালো থাকবেন", "আপনাকে অনেক ধন্যবাদ",
        "আপনার সময় হলে জানাবেন", "আপনার সাথে কথা বলতে চাই", "আপনার সাহায্য দরকার", "আপনার মতামত চাই",
        "আজ অনেক কাজ আছে", "আজ খুব ভালো লাগছে", "আজ বাসায় থাকবো", "আজ দেখা হবে",
        "আজ একটু দেরি হবে", "আজ সময় হবে না", "আজ অফিসে আছি", "আজ ক্লাস আছে",
        "কাল দেখা হবে", "কাল কথা হবে", "কাল আমি যাবো", "কাল সকালে আসবো",
        "কাল অফিসে যাবো", "কাল বাসায় থাকবো", "কাল সময় হলে জানাবো", "কাল আবার কথা বলবো",
        "এখন কি করছো", "এখন কোথায় আছো", "এখন সময় আছে", "এখন যেতে হবে",
        "এখন কাজ করছি", "এখন বাসায় আছি", "এখন কল দিতে পারি", "এখন কথা বলতে পারি",
        "ঠিক আছে ভাই", "ঠিক আছে আপু", "ঠিক আছে পরে কথা হবে", "ঠিক আছে আমি দেখছি",
        "ঠিক আছে আমি আসছি", "ঠিক আছে সমস্যা নেই", "ঠিক আছে জানাবো", "ঠিক আছে ধন্যবাদ",
        "আচ্ছা ঠিক আছে", "আচ্ছা পরে কথা হবে", "আচ্ছা আমি দেখছি", "আচ্ছা তুমি বলো",
        "আচ্ছা সমস্যা নেই", "আচ্ছা বুঝলাম", "আচ্ছা জানিও", "আচ্ছা ভালো থেকো",
        "অনেক ভালো লাগছে", "অনেক ধন্যবাদ ভাই", "অনেক ধন্যবাদ আপু", "অনেক কাজ আছে",
        "অনেক দিন পরে", "অনেক সুন্দর হয়েছে", "অনেক ভালো হয়েছে", "অনেক মজা হয়েছে",
        "খুব ভালো হয়েছে", "খুব সুন্দর হয়েছে", "খুব ভালো লাগছে", "খুব মজা লাগছে",
        "খুব জরুরি দরকার", "খুব বেশি সময় লাগবে না", "খুব তাড়াতাড়ি আসছি", "খুব ভালো থাকবেন",
        "কোন সমস্যা নেই", "কোন চিন্তা নেই", "কোন সমস্যা হলে জানাবেন", "কোন প্রশ্ন থাকলে বলবেন",
        "কিছু দরকার হলে বলো", "কিছু জানতে চাই", "কিছু সমস্যা হয়েছে", "কিছু সময় লাগবে",
        "কেমন আছো তুমি", "কেমন আছেন আপনি", "কেমন হলো কাজ", "কেমন লাগছে এখন",
        "কোথায় আছো এখন", "কোথায় যাচ্ছো", "কোথায় দেখা হবে", "কোথায় যেতে হবে",
        "কেন এমন হলো", "কেন আসবে না", "কেন বলো তো", "কেন সমস্যা হচ্ছে",
        "কিভাবে করতে হবে", "কিভাবে কাজ করে", "কিভাবে যাবো সেখানে", "কিভাবে ঠিক করবো",
        "পরে কথা বলবো", "পরে জানাবো তোমাকে", "পরে কল দিবো", "পরে দেখা হবে",
        "পরে আমি দেখছি", "পরে বিস্তারিত বলবো", "পরে সময় হলে আসবো", "পরে আবার চেষ্টা করবো",
        "ভালো আছি আলহামদুলিল্লাহ", "ভালো থেকো সবসময়", "ভালো থাকবেন ভাই", "ভালো হয়েছে কাজটা",
        "ভালো লাগছে দেখতে", "ভালো করে দেখো", "ভালো করে লিখো", "ভালো করে বুঝিয়ে বলো",
        "ধন্যবাদ ভাই", "ধন্যবাদ আপু", "ধন্যবাদ আপনাকে", "ধন্যবাদ অনেক সাহায্য করার জন্য",
        "দয়া করে জানাবেন", "দয়া করে একটু অপেক্ষা করুন", "দয়া করে আবার চেষ্টা করুন", "দয়া করে বিস্তারিত বলুন",
        "সময় হলে জানিও", "সময় হলে কল দিও", "সময় লাগবে একটু", "সময় খুব কম",
        "সমস্যা নেই ভাই", "সমস্যা হলে জানিও", "সমস্যা ঠিক হয়ে গেছে", "সমস্যার সমাধান হয়েছে",
        "কাজ শেষ হয়েছে", "কাজ চলছে এখন", "কাজ করতে হবে", "কাজ ভালো হয়েছে",
        "অফিসে আছি এখন", "অফিস থেকে বের হচ্ছি", "অফিসে মিটিং আছে", "অফিসে অনেক কাজ",
        "বাসায় আছি এখন", "বাসায় যাচ্ছি এখন", "বাসায় সবাই ভালো আছে", "বাসায় পৌঁছে জানাবো",
        "মেসেজ দেখেছি", "মেসেজ পেয়েছি", "মেসেজ করে জানিও", "মেসেজের রিপ্লাই দিবো",
        "কল দিচ্ছি এখন", "কল ধরতে পারিনি", "কল দিলে কথা হবে", "কল করে জানাবো",
        "ফেসবুকে পোস্ট করেছি", "ইউটিউবে ভিডিও দিয়েছি", "ভিডিওটা অনেক সুন্দর", "ফটোটা ভালো হয়েছে",
        "প্রজেক্টের কাজ চলছে", "প্রজেক্ট আপডেট জানাবো", "ফাইল পাঠিয়ে দিয়েছি", "রিপোর্ট তৈরি করছি",
        "পেমেন্ট করে জানাবেন", "পেমেন্ট পেয়েছি ধন্যবাদ", "অর্ডার কনফার্ম হয়েছে", "ডেলিভারি হয়ে যাবে",
        "দাম কত হবে", "দাম একটু কম হবে", "অফার এখন চলছে", "সার্ভিস ভালো লেগেছে",
        "শুভ সকাল সবাইকে", "শুভ রাত্রি", "শুভেচ্ছা রইলো", "আসসালামু আলাইকুম ভাই",
        "ওয়ালাইকুম আসসালাম", "আলহামদুলিল্লাহ ভালো আছি", "ইনশাআল্লাহ দেখা হবে", "ইনশাআল্লাহ কাজ হবে"
    )
}

class OfflineBanglaNextWordModel(
    corpus: List<String> = BanglaConversationCorpus.sentences + Stage5ConversationCorpus.sentences,
    private val learning: LocalTypingLearningModel = InMemoryTypingLearningModel()
) {
    private val transitionCounts: Map<String, Map<String, Int>>
    private val contextTransitionCounts: Map<Pair<String, String>, Map<String, Int>>
    private val unigramCounts: Map<String, Int>
    private val corpusSizeValue: Int = corpus.size

    init {
        val transitions = linkedMapOf<String, MutableMap<String, Int>>()
        val contexts = linkedMapOf<Pair<String, String>, MutableMap<String, Int>>()
        val unigrams = linkedMapOf<String, Int>()
        corpus.forEach { sentence ->
            val tokens = tokenizeBangla(sentence)
            tokens.forEach { token -> unigrams[token] = (unigrams[token] ?: 0) + 1 }
            tokens.zipWithNext().forEach { (previous, next) ->
                val nextCounts = transitions.getOrPut(previous) { linkedMapOf() }
                nextCounts[next] = (nextCounts[next] ?: 0) + 1
            }
            if (tokens.size >= 3) {
                for (index in 0..tokens.size - 3) {
                    val key = tokens[index] to tokens[index + 1]
                    val next = tokens[index + 2]
                    val nextCounts = contexts.getOrPut(key) { linkedMapOf() }
                    nextCounts[next] = (nextCounts[next] ?: 0) + 1
                }
            }
        }
        transitionCounts = transitions.mapValues { it.value.toMap() }
        contextTransitionCounts = contexts.mapValues { it.value.toMap() }
        unigramCounts = unigrams.toMap()
    }

    /** Backward-compatible bigram-only entry point. */
    fun suggest(previousWord: String?, limit: Int = 3): List<NextWordPrediction> =
        suggest(previousPreviousWord = null, previousWord = previousWord, limit = limit)

    /** Stage-5 two-word context ranking with persistent personal context boosts. */
    fun suggest(previousPreviousWord: String?, previousWord: String?, limit: Int = 3): List<NextWordPrediction> {
        if (limit <= 0) return emptyList()
        val previous = previousWord?.let(::normalizeBanglaToken) ?: return emptyList()
        val previousPrevious = previousPreviousWord?.let(::normalizeBanglaToken)
        val corpusCandidates = transitionCounts[previous].orEmpty()
        val contextCandidates = previousPrevious?.let { contextTransitionCounts[it to previous].orEmpty() }.orEmpty()
        val learnedCandidates = learning.transitionCandidates(previous, PERSONAL_NEXT_WORD_SCAN_LIMIT)
        val learnedContextCandidates = if (previousPrevious != null) {
            learning.contextCandidates(previousPrevious, previous, PERSONAL_CONTEXT_SCAN_LIMIT)
        } else emptyList()

        if (corpusCandidates.isEmpty() && contextCandidates.isEmpty() && learnedCandidates.isEmpty() && learnedContextCandidates.isEmpty()) {
            return emptyList()
        }

        val words = linkedSetOf<String>().apply {
            addAll(contextCandidates.keys)
            addAll(corpusCandidates.keys)
            learnedContextCandidates.forEach { add(it.text) }
            learnedCandidates.forEach { add(it.text) }
        }
        return words.asSequence()
            .map { word ->
                val corpusCount = corpusCandidates[word] ?: 0
                val contextCount = contextCandidates[word] ?: 0
                val personalBigram = learning.transitionBoost(previous, word)
                val personalContext = if (previousPrevious != null) {
                    learning.contextTransitionBoost(previousPrevious, previous, word)
                } else 0
                val personalWord = learning.wordBoost(word)
                val score = PredictionRankingPolicy.banglaNextWordScore(
                    corpusCount = corpusCount,
                    contextCount = contextCount,
                    unigramCount = unigramCounts[word] ?: 0,
                    personalBigram = personalBigram,
                    personalContext = personalContext,
                    personalWord = personalWord
                )
                NextWordPrediction(word, score)
            }
            .sortedWith(compareByDescending<NextWordPrediction> { it.score }.thenBy { it.text })
            .take(limit)
            .toList()
    }

    fun learnCommittedText(text: String, previousWord: String? = null): String? =
        learnCommittedTextWithContext(text, null, previousWord).second

    /** Returns (second-last, last) context after learning committed text. */
    fun learnCommittedTextWithContext(
        text: String,
        previousPreviousWord: String?,
        previousWord: String?
    ): Pair<String?, String?> {
        val tokens = tokenizeBangla(text)
        var secondLast = previousPreviousWord?.let(::normalizeBanglaToken)
        var last = previousWord?.let(::normalizeBanglaToken)
        if (tokens.isEmpty()) return secondLast to last
        tokens.forEach { token ->
            learning.recordWord(token)
            if (last != null) learning.recordTransition(last!!, token)
            if (secondLast != null && last != null) learning.recordContextTransition(secondLast!!, last!!, token)
            secondLast = last
            last = token
        }
        return secondLast to last
    }

    fun learnTransition(previousWord: String, nextWord: String) {
        learning.recordWord(nextWord)
        learning.recordTransition(previousWord, nextWord)
    }

    fun learnTransition(previousPreviousWord: String?, previousWord: String, nextWord: String) {
        learnTransition(previousWord, nextWord)
        if (previousPreviousWord != null) {
            learning.recordContextTransition(previousPreviousWord, previousWord, nextWord)
        }
    }

    fun clearLearningSession() = learning.clearSession()

    fun corpusSize(): Int = corpusSizeValue
    fun contextKeyCount(): Int = contextTransitionCounts.size

    private companion object {
        const val PERSONAL_NEXT_WORD_SCAN_LIMIT = 16
        const val PERSONAL_CONTEXT_SCAN_LIMIT = 12
    }
}

internal fun tokenizeBangla(text: String): List<String> = text
    .split(Regex("\\s+"))
    .mapNotNull(::normalizeBanglaToken)

internal fun normalizeRomanLearningKey(raw: String): String? {
    val normalized = raw.trim().lowercase().filter { it in 'a'..'z' || it == '\'' }
    return normalized.takeIf { it.length in 1..48 }
}

internal fun normalizeBanglaToken(raw: String): String? {
    val cleaned = raw.trim().trim { ch ->
        ch.isWhitespace() || ch in setOf('.', ',', '!', '?', ':', ';', '।', '(', ')', '[', ']', '{', '}', '"', '\'', '…', '-', '—')
    }
    return cleaned.takeIf { it.isNotBlank() }
}
