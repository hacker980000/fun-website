import com.socialaiassistant.keyboard.ime.BanglaInputMode
import com.socialaiassistant.keyboard.ime.KeyboardLanguage
import com.socialaiassistant.keyboard.ime.KeyboardLayer
import com.socialaiassistant.keyboard.ime.KeyboardUiMode
import com.socialaiassistant.keyboard.ime.ImeTypingEngine
import com.socialaiassistant.keyboard.ime.SuggestionKind

fun main() {
    var checks = 0
    val mode = KeyboardUiMode(
        language = KeyboardLanguage.BANGLA,
        banglaMode = BanglaInputMode.PHONETIC,
        layer = KeyboardLayer.LETTERS
    )
    val engine = ImeTypingEngine()

    "ami".forEach { engine.onText(mode, it.toString()) }
    check(engine.currentSuggestions().candidates.first().text == "আমি")
    checks++
    check(engine.flushWithAutocorrect() == "আমি")
    checks++
    check(engine.isShowingNextWordSuggestions())
    checks++

    val next = engine.currentSuggestions().candidates
    check(next.isNotEmpty() && next.all { it.kind == SuggestionKind.NEXT_WORD })
    checks++
    check(next.map { it.text }.contains("এখন"))
    checks++

    val accepted = engine.acceptSuggestion("এখন")
    check(accepted == "এখন")
    checks++
    check(engine.currentSuggestions().candidates.isNotEmpty())
    checks++

    engine.onText(mode, "k")
    check(!engine.isShowingNextWordSuggestions())
    checks++
    engine.reset()

    "amr".forEach { engine.onText(mode, it.toString()) }
    check(engine.flushWithAutocorrect() == "আমার")
    checks++

    println("typing_stage3_engine_selftest: $checks checks PASS; dictionary=${engine.dictionarySize()}")
}
