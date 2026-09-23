import com.socialaiassistant.keyboard.ime.CommonEnglishTypingAliases
import com.socialaiassistant.keyboard.ime.EnglishConversationCorpus
import com.socialaiassistant.keyboard.ime.EnglishTypingEngine
import com.socialaiassistant.keyboard.ime.InMemoryEnglishTypingLearningModel
import com.socialaiassistant.keyboard.ime.OfflineEnglishNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineEnglishSuggestionEngine
import com.socialaiassistant.keyboard.ime.SmartLanguageBridge

fun main() {
    var checks = 0
    val suggestions = OfflineEnglishSuggestionEngine()
    check(suggestions.dictionarySize() >= 500)
    checks++

    check(CommonEnglishTypingAliases.aliases["dont"] == "don't")
    checks++
    check(suggestions.commitText("dont", true) == "don't")
    checks++
    check(suggestions.commitText("Dont", true) == "Don't")
    checks++

    val typo = suggestions.suggest("mesage", 5)
    check(typo.candidates.any { it.text.equals("message", ignoreCase = true) })
    checks++
    check(suggestions.commitText("mesage", true).equals("message", ignoreCase = true))
    checks++

    val prefix = suggestions.suggest("tha", 5).candidates.map { it.text.lowercase() }
    check(prefix.any { it.startsWith("tha") })
    checks++

    check(EnglishConversationCorpus.sentences.size >= 120)
    checks++
    val next = OfflineEnglishNextWordModel().suggest("thank", 3).map { it.text }
    check(next.contains("you"))
    checks++

    val learning = InMemoryEnglishTypingLearningModel()
    repeat(5) {
        learning.recordWord("update")
        learning.recordTransition("project", "update")
    }
    val personalized = OfflineEnglishNextWordModel(emptyList(), learning).suggest("project", 3)
    check(personalized.first().text == "update")
    checks++

    val engine = EnglishTypingEngine()
    "hello".forEach { engine.onText(it.toString()) }
    check(engine.isComposing())
    checks++
    check(engine.flush(autocorrect = true).equals("hello", ignoreCase = true))
    checks++
    check(engine.isShowingNextWordSuggestions())
    checks++
    check(engine.currentSuggestions(3).candidates.isNotEmpty())
    checks++

    val bridge = SmartLanguageBridge()
    check(bridge.banglaHintFromEnglish("ami") == "আমি")
    checks++
    check(bridge.banglaHintFromEnglish("hello") == null)
    checks++
    check(bridge.englishHintFromBanglaPhonetic("message") == "message")
    checks++

    println("typing_stage7_english_multilingual_selftest: $checks checks PASS; englishDictionary=${suggestions.dictionarySize()}; englishCorpus=${EnglishConversationCorpus.sentences.size}")
}
