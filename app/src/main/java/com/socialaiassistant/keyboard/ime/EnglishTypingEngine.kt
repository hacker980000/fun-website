package com.socialaiassistant.keyboard.ime

/** Offline English typing support added in Typing Core v2 Stage 7. */
enum class EnglishSuggestionKind {
    EXACT,
    ALIAS,
    PREFIX,
    TYPO,
    PERSONAL,
    NEXT_WORD
}

data class EnglishSuggestionCandidate(
    val text: String,
    val kind: EnglishSuggestionKind,
    val score: Int
)

data class EnglishSuggestionSnapshot(
    val source: String,
    val candidates: List<EnglishSuggestionCandidate>,
    val autocorrectText: String? = null
)

/**
 * Curated, license-independent common-word vocabulary for offline English suggestions.
 * These are ordinary words rather than copied prose/corpus content.
 */
object ProductionEnglishLexicon {
    private val rawWords = """
        the be to of and a in that have i it for not on with he as you do at this but his by from
        they we say her she or an will my one all would there their what so up out if about who get
        which go me when make can like time no just him know take people into year your good some could
        them see other than then now look only come its over think also back after use two how our work
        first well way even new want because these give day most us is are was were been being has had
        did does done may might must should shall could would can cannot cant dont wont isnt arent wasnt
        werent havent hasnt hadnt didnt doesnt shouldnt wouldnt couldnt im ive ill id youre youve youll
        youd hes shes its were theyre theyve theyll thats whats whos wheres whens whys hows lets
        hello hi hey thanks thank please sorry yes yeah yep no okay ok sure great nice awesome amazing
        fine welcome morning afternoon evening night today tomorrow yesterday later soon early late
        minute minutes hour hours day days week weeks month months year years now then before after
        again always never sometimes often usually maybe probably really very quite too enough almost
        already still yet ever once twice first second third next last same different another each every
        any many much few little more most less least several both either neither own such only even
        here there where everywhere somewhere anywhere nowhere inside outside above below under around
        near far left right front behind between among across through toward away home house room door
        road street city town village country world place area office school college university class
        shop store market restaurant hotel hospital station airport bus train car bike rickshaw flight
        phone mobile computer laptop desktop keyboard screen display camera photo picture video audio
        message messages chat reply replies call email mail internet online offline website app apps
        account login logout password code otp pin payment money price cost order delivery service
        support help issue problem solution update version file files folder document documents report
        project projects task tasks job jobs work working meeting meetings team client customer customers
        manager boss staff company business product products design designs developer development
        engineering engineer technology technical system systems software hardware data database server
        cloud network security privacy setting settings option options feature features tool tools
        ai smart model models prompt prompts text texts word words language languages english bangla
        bengali banglish translate translation grammar rewrite caption tone context suggestion suggestions
        type typing typed write writing wrote read reading send sent receive received open close save saved
        share shared upload uploaded download downloaded install installed update updated delete deleted
        create created edit edited change changed choose selected select copy copied paste pasted cut search
        find found check checked test tested try tried start started stop stopped finish finished complete
        completed continue continued wait waited move moved add added remove removed show shown hide hidden
        turn turned enable enabled disable disabled connect connected disconnect disconnected sync synced
        load loaded refresh refreshed build built run running ran fix fixed improve improved learn learned
        remember remembered forget forgot clear cleared keep kept hold held press pressed tap tapped click
        clicked swipe swiped drag dragged scroll scrolled switch switched use used using need needed want
        wanted like liked love loved prefer preferred feel felt think thought know knew understand understood
        see saw seen watch watched hear heard listen listened speak spoke spoken tell told ask asked answer
        answered say said mean meant explain explained call called come came coming go went gone going arrive
        arrived leave left leaving bring brought take took taken give gave given get got gotten make made
        making do doing did done have having had be being was were become became seem seemed look looked
        work works worked study studied learn learns play played eat ate eaten drink drank drunk sleep slept
        wake woke walk walked run ran drive drove driven ride rode ridden travel traveled meet met visit
        visited buy bought sell sold pay paid spend spent cost costs order ordered book booked confirm
        confirmed cancel cancelled return returned deliver delivered send sends sent reply replied receive
        receives received speak speaking talk talking discuss discussed plan planned decide decided choose
        chose chosen agree agreed disagree disagreed accept accepted reject rejected allow allowed block
        blocked protect protected safe unsafe public private local personal important urgent normal simple
        easy hard difficult fast slow quick quickly slowly smooth stable correct wrong right true false real
        fake best better good bad worse worst big small large huge tiny long short high low old young new
        modern latest current previous future past present clean clear beautiful smart professional casual
        formal friendly funny flirty romantic happy sad angry upset excited tired busy free ready available
        unavailable hungry thirsty sick healthy strong weak hot cold warm cool dark light bright black white
        red green blue yellow orange purple pink gray grey brown gold silver premium basic advanced pro free
        male female man woman boy girl friend friends family father mother brother sister husband wife child
        children person people user users everyone someone anyone nobody name names number numbers address
        date dates time times morning evening night monday tuesday wednesday thursday friday saturday sunday
        january february march april may june july august september october november december one two three
        four five six seven eight nine ten hundred thousand million zero first second third fourth fifth
        what when where why who whom whose which how can could should would will shall do does did is are
        am was were have has had need want like prefer think know understand remember forget mean say tell
        ask answer explain show help make create fix change update improve check test use try start stop open
        close save share send call text message reply wait come go stay sit stand walk run eat drink sleep
        good better best bad worse easy easier hard harder fast faster slow slower new newer old older
        beautiful useful helpful important possible impossible available ready sure certain common popular
        viral trending social facebook youtube instagram whatsapp messenger reel reels short shorts post
        posts comment comments content creator creators audience follower followers view views like likes
        share shares reach engagement channel page profile caption title keyword keywords tag tags seo
        ad ads advertising campaign campaigns marketing market brand brands product products customer sale
        sales offer offers discount discounts price prices buy purchase checkout cart delivery shipping
        cash bKash nagad bank card invoice receipt total amount due paid unpaid monthly yearly daily weekly
        bangladesh dhaka chittagong chattogram sylhet rajshahi khulna barisal rangpur mymensingh tangail
        sirajganj cox bazar singapore malaysia japan india usa uk america europe asia local global
        engineer electrical electronic electronics power electricity solar battery inverter transformer
        voltage current watt watts kilowatt energy load meter prepaid postpaid wiring cable circuit breaker
        motor generator substation grid rail railway train bridge building flat apartment land real estate
        housing construction piling floor lift elevator fire safety helmet factory industrial machine
        android iphone ios google play store gboard ridmik avro phonetic bijoy emoji clipboard voice cursor
        spacebar autocorrect prediction dictionary personal learning offline online cloud privacy consent
        secure sensitive field fields banking password pin otp payment accessibility permission permissions
        conversation conversations context contexts smart reply rewrite translate grammar caption image photo
        model api backend frontend source code coding compile build gradle kotlin java xml release debug test
        version package zip apk aab github worker firebase firestore database server client request response
        success failed failure error errors warning warnings status result results queue cache memory storage
        device devices phone phones tablet tablets keyboard keyboards input output key keys row rows layout
        theme themes color colors background backgrounds font fonts size height width sound haptic vibration
        feedback number symbol symbols letters letter word sentence paragraph comma period question exclamation
        apostrophe quote space enter backspace shift caps lock language switch mode modes english bengali
        bangla banglish phonetic bijoy prediction suggest suggestion suggestions autocorrect typo spelling
        dictionary dictionaries personal personalized learning privacy local offline online safe secure
    """.trimIndent()

    val words: Map<String, Int> by lazy {
        val unique = LinkedHashSet<String>()
        rawWords.split(Regex("\\s+")).forEach { token ->
            normalizeEnglishToken(token)?.let(unique::add)
        }
        unique.withIndex().associate { (index, word) ->
            word to (1_500 - index.coerceAtMost(1_300))
        }
    }
}

object CommonEnglishTypingAliases {
    val aliases: Map<String, String> = linkedMapOf(
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "isnt" to "isn't",
        "arent" to "aren't",
        "wasnt" to "wasn't",
        "werent" to "weren't",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "hadnt" to "hadn't",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "couldnt" to "couldn't",
        "im" to "I'm",
        "ive" to "I've",
        "ill" to "I'll",
        "youre" to "you're",
        "youve" to "you've",
        "youll" to "you'll",
        "theyre" to "they're",
        "theyve" to "they've",
        "thats" to "that's",
        "whats" to "what's",
        "wheres" to "where's",
        "hows" to "how's",
        "pls" to "please",
        "plz" to "please",
        "thx" to "thanks",
        "msg" to "message"
    )
}

data class EnglishLexiconEntry(val word: String, val bonus: Int)

class EnglishLexiconIndex(
    private val lexicon: Map<String, Int> = ProductionEnglishLexicon.words
) {
    private val byPrefix: Map<String, List<EnglishLexiconEntry>>
    private val byLengthAndInitial: Map<Pair<Int, Char>, List<EnglishLexiconEntry>>

    init {
        val entries = lexicon.map { EnglishLexiconEntry(it.key, it.value) }
        byPrefix = buildMap {
            entries.forEach { entry ->
                for (length in 1..minOf(4, entry.word.length)) {
                    val key = entry.word.substring(0, length)
                    val list = getOrPut(key) { mutableListOf<EnglishLexiconEntry>() } as MutableList<EnglishLexiconEntry>
                    list += entry
                }
            }
        }.mapValues { (_, values) -> values.sortedWith(compareByDescending<EnglishLexiconEntry> { it.bonus }.thenBy { it.word }) }
        byLengthAndInitial = entries.groupBy { it.word.length to it.word.first() }
    }

    fun exact(raw: String): EnglishLexiconEntry? {
        val normalized = normalizeEnglishToken(raw) ?: return null
        val bonus = lexicon[normalized] ?: return null
        return EnglishLexiconEntry(normalized, bonus)
    }

    fun prefix(raw: String, limit: Int = 16): List<EnglishLexiconEntry> {
        val normalized = normalizeEnglishToken(raw) ?: return emptyList()
        val key = normalized.take(minOf(4, normalized.length))
        return byPrefix[key].orEmpty().asSequence()
            .filter { it.word.startsWith(normalized) && it.word != normalized }
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    fun typo(raw: String, limit: Int = 8): List<EnglishLexiconEntry> {
        val normalized = normalizeEnglishToken(raw) ?: return emptyList()
        if (normalized.length < 3) return emptyList()
        val initial = normalized.first()
        val candidates = buildList {
            for (length in (normalized.length - 1).coerceAtLeast(1)..normalized.length + 1) {
                addAll(byLengthAndInitial[length to initial].orEmpty())
            }
        }
        return candidates.asSequence()
            .filter { it.word != normalized }
            .mapNotNull { entry ->
                val distance = conservativeEnglishEditDistance(normalized, entry.word)
                if (distance <= 1) entry else null
            }
            .distinctBy { it.word }
            .sortedWith(compareByDescending<EnglishLexiconEntry> { it.bonus }.thenBy { it.word })
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    fun size(): Int = lexicon.size
}

interface EnglishTypingLearningModel {
    fun recordWord(word: String)
    fun recordTransition(previous: String, next: String)
    fun wordBoost(word: String): Int
    fun transitionBoost(previous: String, next: String): Int
    fun transitionCandidates(previous: String, limit: Int): List<LearnedNextWordCandidate>
    fun flush()
    fun requestFlush() = flush()
    fun close() = flush()
    fun clearSession()
}

class InMemoryEnglishTypingLearningModel : EnglishTypingLearningModel {
    private val words = linkedMapOf<String, Int>()
    private val transitions = linkedMapOf<Pair<String, String>, Int>()

    override fun recordWord(word: String) {
        val token = normalizeEnglishToken(word) ?: return
        words[token] = ((words[token] ?: 0) + 1).coerceAtMost(1_000)
    }

    override fun recordTransition(previous: String, next: String) {
        val left = normalizeEnglishToken(previous) ?: return
        val right = normalizeEnglishToken(next) ?: return
        val key = left to right
        transitions[key] = ((transitions[key] ?: 0) + 1).coerceAtMost(1_000)
    }

    override fun wordBoost(word: String): Int = (words[normalizeEnglishToken(word)] ?: 0).coerceAtMost(20) * 14

    override fun transitionBoost(previous: String, next: String): Int =
        (transitions[normalizeEnglishToken(previous) to normalizeEnglishToken(next)] ?: 0).coerceAtMost(20) * 45

    override fun transitionCandidates(previous: String, limit: Int): List<LearnedNextWordCandidate> {
        val normalized = normalizeEnglishToken(previous) ?: return emptyList()
        return transitions.asSequence()
            .filter { it.key.first == normalized }
            .map { LearnedNextWordCandidate(it.key.second, it.value) }
            .sortedWith(compareByDescending<LearnedNextWordCandidate> { it.weight }.thenBy { it.text })
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    override fun flush() = Unit

    override fun clearSession() {
        words.clear()
        transitions.clear()
    }
}

object EnglishConversationCorpus {
    val sentences: List<String> = listOf(
        "hello how are you", "hello there", "hello good morning", "hi how are you", "hey how are you",
        "how are you", "how are you doing", "how is everything", "how was your day",
        "i am good", "i am fine", "i am ready", "i am busy", "i am free now",
        "i am at home", "i am at work", "i am coming now", "i am on the way",
        "i will call you", "i will text you", "i will let you know", "i will check it",
        "i will send it", "i will do it", "i will try again", "i will be there",
        "i need your help", "i need some time", "i need the file", "i need an update",
        "i want to know", "i want to go", "i want to see", "i want to try",
        "i think it is good", "i think so too", "i think we can", "i think this works",
        "thank you so much", "thank you very much", "thanks for your help", "thanks a lot",
        "please let me know", "please send the file", "please check again", "please call me",
        "okay no problem", "okay i understand", "okay i will check", "okay see you later",
        "sure no problem", "sure i can help", "sure let me check", "sure we can do that",
        "see you soon", "see you tomorrow", "see you later", "talk to you later",
        "good morning everyone", "good afternoon everyone", "good evening everyone", "good night",
        "have a good day", "have a nice day", "have a safe trip", "have a great weekend",
        "what are you doing", "what do you think", "what is the problem", "what is your name",
        "where are you now", "where do you live", "where should we meet", "where is the file",
        "when will you come", "when can we talk", "when is the meeting", "when are you free",
        "why is this happening", "why are you late", "why did you call", "why not try again",
        "can you help me", "can you call me", "can you send it", "can you check this",
        "could you please help", "could you send the file", "could you check again", "could you explain this",
        "we can do it", "we can talk later", "we can meet tomorrow", "we can try again",
        "we need to check", "we need more time", "we need an update", "we need a solution",
        "the project is ready", "the project is complete", "the project looks good", "the project needs an update",
        "the file is ready", "the file is attached", "the file looks good", "the file is missing",
        "the meeting is today", "the meeting is tomorrow", "the meeting starts soon", "the meeting is cancelled",
        "the message was sent", "the message looks good", "the message is clear", "the message needs editing",
        "your order is confirmed", "your payment is received", "your account is ready", "your request is approved",
        "please confirm your order", "please confirm the time", "please confirm the payment", "please confirm receipt",
        "the app is working", "the keyboard is working", "the update is ready", "the issue is fixed",
        "this looks great", "this looks good", "this is perfect", "this is very helpful",
        "that sounds good", "that makes sense", "that is correct", "that is a good idea",
        "let me check", "let me know", "let me try", "let me explain",
        "send me the details", "send me the link", "send me the photo", "send me the video",
        "call me when free", "call me later", "call me tomorrow", "call me when you arrive",
        "i love this design", "i like this idea", "i like the new version", "i prefer the first option",
        "the design looks clean", "the video looks great", "the caption looks good", "the title is better",
        "we should update this", "we should fix this", "we should test again", "we should keep it simple",
        "please keep me updated", "please share the details", "please send an update", "please wait a moment",
        "no problem at all", "no worries", "no need to worry", "no rush take your time",
        "i understand the issue", "i understand your point", "i understand now", "i understand what you mean"
    )
}

class OfflineEnglishNextWordModel(
    corpus: List<String> = EnglishConversationCorpus.sentences,
    private val learning: EnglishTypingLearningModel = InMemoryEnglishTypingLearningModel()
) {
    private val transitions: Map<String, Map<String, Int>>
    private val unigrams: Map<String, Int>

    init {
        val transitionMap = linkedMapOf<String, MutableMap<String, Int>>()
        val unigramMap = linkedMapOf<String, Int>()
        corpus.forEach { sentence ->
            val tokens = tokenizeEnglish(sentence)
            tokens.forEach { token -> unigramMap[token] = (unigramMap[token] ?: 0) + 1 }
            tokens.zipWithNext().forEach { (previous, next) ->
                val counts = transitionMap.getOrPut(previous) { linkedMapOf() }
                counts[next] = (counts[next] ?: 0) + 1
            }
        }
        transitions = transitionMap.mapValues { it.value.toMap() }
        unigrams = unigramMap.toMap()
    }

    fun suggest(previousWord: String?, limit: Int = 3): List<NextWordPrediction> {
        val previous = normalizeEnglishToken(previousWord.orEmpty()) ?: return emptyList()
        val corpusCandidates = transitions[previous].orEmpty()
        val learned = learning.transitionCandidates(previous, 12)
        val words = linkedSetOf<String>().apply {
            addAll(corpusCandidates.keys)
            learned.forEach { add(it.text) }
        }
        return words.asSequence()
            .map { word ->
                val score = PredictionRankingPolicy.englishNextWordScore(
                    corpusCount = corpusCandidates[word] ?: 0,
                    unigramCount = unigrams[word] ?: 0,
                    personalTransition = learning.transitionBoost(previous, word),
                    personalWord = learning.wordBoost(word)
                )
                NextWordPrediction(word, score)
            }
            .sortedWith(compareByDescending<NextWordPrediction> { it.score }.thenBy { it.text })
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    fun learnCommitted(text: String, previousWord: String?): String? {
        var previous = normalizeEnglishToken(previousWord.orEmpty())
        tokenizeEnglish(text).forEach { token ->
            learning.recordWord(token)
            if (previous != null) learning.recordTransition(previous!!, token)
            previous = token
        }
        return previous
    }

    fun learnTransition(previous: String, next: String) {
        learning.recordWord(next)
        learning.recordTransition(previous, next)
    }
}

class OfflineEnglishSuggestionEngine(
    private val index: EnglishLexiconIndex = EnglishLexiconIndex(),
    private val aliases: Map<String, String> = CommonEnglishTypingAliases.aliases,
    private val learning: EnglishTypingLearningModel = InMemoryEnglishTypingLearningModel()
) {
    fun suggest(raw: String, limit: Int = 3): EnglishSuggestionSnapshot {
        val normalized = normalizeEnglishToken(raw) ?: return EnglishSuggestionSnapshot(raw, emptyList())
        val scored = mutableListOf<EnglishSuggestionCandidate>()

        index.exact(normalized)?.let { exact ->
            scored += EnglishSuggestionCandidate(
                applyEnglishCasePattern(raw, exact.word),
                EnglishSuggestionKind.EXACT,
                PredictionRankingPolicy.ENGLISH_EXACT_BASE + exact.bonus + learning.wordBoost(exact.word)
            )
        }

        aliases[normalized]?.let { alias ->
            scored += EnglishSuggestionCandidate(
                applyEnglishCasePattern(raw, alias),
                EnglishSuggestionKind.ALIAS,
                PredictionRankingPolicy.ENGLISH_ALIAS_BASE + learning.wordBoost(alias)
            )
        }

        index.prefix(normalized, 16).forEachIndexed { position, entry ->
            scored += EnglishSuggestionCandidate(
                applyEnglishCasePattern(raw, entry.word),
                EnglishSuggestionKind.PREFIX,
                PredictionRankingPolicy.ENGLISH_PREFIX_BASE + entry.bonus - position * 3 +
                    learning.wordBoost(entry.word) - PredictionRankingPolicy.completionPenalty(
                        candidateLength = entry.word.length,
                        typedLength = normalized.length,
                        perCharacter = PredictionRankingPolicy.ENGLISH_PREFIX_COMPLETION_PENALTY
                    )
            )
        }

        val typos = index.typo(normalized, 8)
        typos.forEachIndexed { position, entry ->
            scored += EnglishSuggestionCandidate(
                applyEnglishCasePattern(raw, entry.word),
                EnglishSuggestionKind.TYPO,
                PredictionRankingPolicy.ENGLISH_TYPO_BASE + entry.bonus -
                    position * PredictionRankingPolicy.ENGLISH_TYPO_POSITION_PENALTY +
                    learning.wordBoost(entry.word) - kotlin.math.abs(entry.word.length - normalized.length) *
                    PredictionRankingPolicy.ENGLISH_TYPO_LENGTH_DELTA_PENALTY
            )
        }

        val candidateComparator = compareByDescending<EnglishSuggestionCandidate> { it.score }
            .thenByDescending { PredictionRankingPolicy.englishKindPriority(it.kind) }
            .thenBy { it.text.lowercase() }
            .thenBy { it.text }
        val typoRanked = scored
            .asSequence()
            .filter { it.kind == EnglishSuggestionKind.TYPO }
            .sortedWith(candidateComparator)
            .toList()
        val topTypo = typoRanked.firstOrNull()
        val runnerUpTypo = typoRanked.getOrNull(1)
        val safeTypoText = topTypo?.takeIf { candidate ->
            PredictionAutocorrectPolicy.isConfidentEnglishTypo(
                tokenLength = normalized.length,
                topScore = candidate.score,
                runnerUpScore = runnerUpTypo?.score
            )
        }?.text

        val autocorrect = when {
            aliases.containsKey(normalized) -> applyEnglishCasePattern(raw, aliases.getValue(normalized))
            index.exact(normalized) != null -> null
            else -> safeTypoText
        }

        val candidates = scored
            .sortedWith(candidateComparator)
            .distinctBy { it.text.lowercase() }
            .take(limit.coerceAtLeast(0))

        return EnglishSuggestionSnapshot(raw, candidates, autocorrect)
    }

    fun commitText(raw: String, autocorrectEnabled: Boolean): String {
        if (!autocorrectEnabled) return raw
        return suggest(raw, 4).autocorrectText ?: raw
    }

    fun dictionarySize(): Int = index.size()
}

class EnglishTypingEngine(
    private val learning: EnglishTypingLearningModel = InMemoryEnglishTypingLearningModel(),
    private val suggestionEngine: OfflineEnglishSuggestionEngine = OfflineEnglishSuggestionEngine(learning = learning),
    private val nextWordModel: OfflineEnglishNextWordModel = OfflineEnglishNextWordModel(learning = learning)
) {
    private val buffer = StringBuilder()
    private var lastCommittedWord: String? = null
    private var nextWordVisible: Boolean = false
    private var learningEnabled: Boolean = true
    private var suggestionsEnabled: Boolean = true
    private var autocorrectEnabled: Boolean = true
    private var suggestionRevision: Long = 0L

    fun onText(value: String): TypingMutation {
        nextWordVisible = false
        bumpSuggestionRevision()
        if (value.length == 1 && (value[0].isLetter() || value[0] == '\'')) {
            buffer.append(value)
            return TypingMutation(composingText = buffer.toString())
        }
        return TypingMutation(directCommit = value)
    }

    fun onBackspace(): TypingMutation {
        nextWordVisible = false
        bumpSuggestionRevision()
        if (buffer.isEmpty()) return TypingMutation(deletePrevious = true)
        buffer.deleteCharAt(buffer.lastIndex)
        return TypingMutation(composingText = buffer.toString())
    }

    fun currentSuggestions(limit: Int = 3): EnglishSuggestionSnapshot {
        if (buffer.isNotEmpty()) return suggestionEngine.suggest(buffer.toString(), limit)
        if (!nextWordVisible) return EnglishSuggestionSnapshot("", emptyList())
        val predictions = nextWordModel.suggest(lastCommittedWord, limit)
        return EnglishSuggestionSnapshot(
            "",
            predictions.map { EnglishSuggestionCandidate(it.text, EnglishSuggestionKind.NEXT_WORD, it.score) }
        )
    }

    fun flush(autocorrect: Boolean = false): String {
        if (buffer.isEmpty()) return ""
        val raw = buffer.toString()
        val output = suggestionEngine.commitText(raw, autocorrect && autocorrectEnabled)
        buffer.clear()
        rememberCommitted(output)
        nextWordVisible = suggestionsEnabled
        bumpSuggestionRevision()
        return output
    }

    fun commitGlideWord(word: String): String {
        buffer.clear()
        val normalized = normalizeEnglishToken(word) ?: return word
        rememberCommitted(normalized)
        nextWordVisible = suggestionsEnabled
        bumpSuggestionRevision()
        return word
    }

    fun acceptSuggestion(text: String): String {
        if (buffer.isNotEmpty()) {
            buffer.clear()
            rememberCommitted(text)
            nextWordVisible = false
            bumpSuggestionRevision()
            return text
        }
        if (nextWordVisible) {
            val previous = lastCommittedWord
            if (learningEnabled && previous != null) {
                nextWordModel.learnTransition(previous, text)
                learning.requestFlush()
            }
            lastCommittedWord = normalizeEnglishToken(text) ?: lastCommittedWord
            nextWordVisible = suggestionsEnabled
            bumpSuggestionRevision()
        }
        return text
    }

    fun discardComposition() {
        val changed = buffer.isNotEmpty() || nextWordVisible
        buffer.clear()
        nextWordVisible = false
        if (changed) bumpSuggestionRevision()
    }

    fun currentBuffer(): String = buffer.toString()
    fun isComposing(): Boolean = buffer.isNotEmpty()
    fun isShowingNextWordSuggestions(): Boolean = buffer.isEmpty() && nextWordVisible && lastCommittedWord != null
    fun dismissNextWordSuggestions() {
        if (nextWordVisible) {
            nextWordVisible = false
            bumpSuggestionRevision()
        }
    }
    fun setLearningEnabled(enabled: Boolean) { learningEnabled = enabled }
    fun setSuggestionsEnabled(enabled: Boolean) {
        if (suggestionsEnabled == enabled) return
        suggestionsEnabled = enabled
        if (!enabled) nextWordVisible = false
        bumpSuggestionRevision()
    }
    fun setAutocorrectEnabled(enabled: Boolean) { autocorrectEnabled = enabled }
    fun dictionarySize(): Int = suggestionEngine.dictionarySize()

    fun reset() {
        buffer.clear()
        lastCommittedWord = null
        nextWordVisible = false
        bumpSuggestionRevision()
    }

    fun suggestionRevision(): Long = suggestionRevision

    private fun bumpSuggestionRevision() {
        suggestionRevision = if (suggestionRevision == Long.MAX_VALUE) 0L else suggestionRevision + 1L
    }

    private fun rememberCommitted(text: String) {
        if (learningEnabled) {
            lastCommittedWord = nextWordModel.learnCommitted(text, lastCommittedWord)
            learning.requestFlush()
        } else {
            tokenizeEnglish(text).forEach { token -> lastCommittedWord = token }
        }
    }
}

/** Cross-language hints are user-confirmed only; this class never auto-switches the keyboard. */
class SmartLanguageBridge(
    private val englishIndex: EnglishLexiconIndex = EnglishLexiconIndex(),
    private val banglaIndex: BanglaLexiconIndex = BanglaLexiconIndex(),
    private val banglaAliases: Map<String, String> = CommonBanglaTypingAliases.aliasToCanonical
) {
    fun banglaHintFromEnglish(raw: String): String? {
        val roman = normalizeRomanLearningKey(raw) ?: return null
        if (roman.length < 3) return null
        if (englishIndex.exact(roman) != null) return null
        val canonical = banglaAliases[roman] ?: roman
        return banglaIndex.exact(canonical)?.bangla
    }

    fun englishHintFromBanglaPhonetic(raw: String): String? {
        val roman = normalizeEnglishToken(raw) ?: return null
        if (roman.length < 4) return null
        return englishIndex.exact(roman)?.word
    }
}

internal fun tokenizeEnglish(text: String): List<String> = text
    .split(Regex("\\s+"))
    .mapNotNull(::normalizeEnglishToken)

internal fun normalizeEnglishToken(raw: String): String? {
    val cleaned = raw.trim().trim { ch ->
        ch.isWhitespace() || ch in setOf('.', ',', '!', '?', ':', ';', '(', ')', '[', ']', '{', '}', '"', '…', '-', '—')
    }.lowercase()
    if (cleaned.isBlank() || cleaned.length > 48) return null
    if (cleaned.none { it in 'a'..'z' }) return null
    if (!cleaned.all { it in 'a'..'z' || it == '\'' }) return null
    return cleaned
}

internal fun applyEnglishCasePattern(source: String, suggestion: String): String = when {
    source.length > 1 && source.all { !it.isLetter() || it.isUpperCase() } -> suggestion.uppercase()
    source.firstOrNull()?.isUpperCase() == true -> suggestion.replaceFirstChar { it.uppercase() }
    else -> suggestion
}

internal fun conservativeEnglishEditDistance(left: String, right: String): Int {
    if (left == right) return 0
    if (kotlin.math.abs(left.length - right.length) > 1) return 2
    if (left.length == right.length) {
        val diffs = left.indices.filter { left[it] != right[it] }
        if (diffs.size == 2 && diffs[1] == diffs[0] + 1 &&
            left[diffs[0]] == right[diffs[1]] && left[diffs[1]] == right[diffs[0]]) return 1
    }
    var i = 0
    var j = 0
    var edits = 0
    while (i < left.length && j < right.length) {
        if (left[i] == right[j]) { i++; j++; continue }
        edits++
        if (edits > 1) return edits
        when {
            left.length > right.length -> i++
            right.length > left.length -> j++
            else -> { i++; j++ }
        }
    }
    if (i < left.length || j < right.length) edits++
    return edits
}
