import com.socialaiassistant.keyboard.ime.BanglaInputMode
import com.socialaiassistant.keyboard.ime.BanglaPhoneticComposer
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.ImeTypingEngine
import com.socialaiassistant.keyboard.ime.InMemoryTypingLearningModel
import com.socialaiassistant.keyboard.ime.KeyboardLanguage
import com.socialaiassistant.keyboard.ime.KeyboardLayer
import com.socialaiassistant.keyboard.ime.KeyboardUiMode
import com.socialaiassistant.keyboard.ime.OfflineBanglaNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.SuggestionKind

fun main() {
    var checks = 0
    val personal = "\u0993\u09ae\u09b0"
    val nextText = "\u0986\u09b8\u09ac\u09c7"
    val learning = InMemoryTypingLearningModel()

    repeat(3) { learning.recordPhonetic("omor", personal, weight = 3) }
    val suggestions = OfflineBanglaSuggestionEngine(learning = learning)
    val fallback = HybridBanglaPhoneticTransliterator().transliterate("omor")
    val snapshot = suggestions.suggest("omor", fallback)
    check(snapshot.candidates.first().text == personal)
    checks++
    check(snapshot.candidates.first().kind == SuggestionKind.PERSONAL)
    checks++
    check(suggestions.commitText("omor", fallback) == personal)
    checks++

    repeat(4) {
        learning.recordWord(nextText)
        learning.recordTransition(personal, nextText)
    }
    val learnedOnlyModel = OfflineBanglaNextWordModel(emptyList(), learning)
    check(learnedOnlyModel.suggest(personal, 3).first().text == nextText)
    checks++

    val mode = KeyboardUiMode(
        language = KeyboardLanguage.BANGLA,
        banglaMode = BanglaInputMode.PHONETIC,
        layer = KeyboardLayer.LETTERS
    )
    val disabledLearning = InMemoryTypingLearningModel()
    val disabledEngine = ImeTypingEngine(learning = disabledLearning)
    disabledEngine.setLearningEnabled(false)
    "amr".forEach { disabledEngine.onText(mode, it.toString()) }
    check(disabledEngine.flushWithAutocorrect().isNotBlank())
    checks++
    check(disabledLearning.phoneticCandidates("amr", 3).isEmpty())
    checks++

    val predictionEngine = ImeTypingEngine()
    predictionEngine.setSuggestionsEnabled(false)
    "ami".forEach { predictionEngine.onText(mode, it.toString()) }
    predictionEngine.flushWithAutocorrect()
    check(!predictionEngine.isShowingNextWordSuggestions())
    checks++

    println("typing_stage4_personalization_selftest: $checks checks PASS")
}
