import com.socialaiassistant.keyboard.ime.BanglaLexiconIndex
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.InMemoryTypingLearningModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.SuggestionKind

fun main() {
    var checks = 0
    val index = BanglaLexiconIndex()
    check(index.size() >= 500) { "expanded lexicon too small: ${index.size()}" }
    checks++

    check(index.exact("dorkar")?.bangla == "দরকার")
    checks++
    check(index.exact("keyboard")?.bangla == "কিবোর্ড")
    checks++
    check(index.prefix("som", 8).any { it.bangla == "সময়" })
    checks++

    val transliterator = HybridBanglaPhoneticTransliterator()
    check(transliterator.transliterate("dorkar") == "দরকার")
    checks++
    check(transliterator.transliterate("payment") == "পেমেন্ট")
    checks++

    val engine = OfflineBanglaSuggestionEngine()
    val prefix = engine.suggest("dor", transliterator.transliterate("dor"))
    check(prefix.candidates.any { it.text == "দরকার" })
    checks++

    val exact = engine.suggest("keyboard", transliterator.transliterate("keyboard"))
    check(exact.candidates.first().kind == SuggestionKind.EXACT)
    checks++
    check(exact.candidates.first().text == "কিবোর্ড")
    checks++

    val shorthand = engine.suggest("dkr", transliterator.transliterate("dkr"))
    check(shorthand.autocorrectText == "দরকার")
    checks++

    val next = OfflineBanglaNextWordModel()
    val afterAmi = next.suggest("আমি", 3)
    check(afterAmi.isNotEmpty())
    checks++
    check(afterAmi.map { it.text }.contains("এখন"))
    checks++

    val afterThik = next.suggest("ঠিক", 3)
    check(afterThik.first().text == "আছে")
    checks++

    val learning = InMemoryTypingLearningModel()
    val personalized = OfflineBanglaNextWordModel(listOf("আমি ভালো", "আমি এখন"), learning)
    repeat(5) { personalized.learnTransition("আমি", "এখন") }
    check(personalized.suggest("আমি", 2).first().text == "এখন")
    checks++

    val learnedLast = next.learnCommittedText("আমি ভালো আছি")
    check(learnedLast == "আছি")
    checks++

    println("suggestion_stage3_selftest: $checks checks PASS; lexicon=${index.size()}")
}
