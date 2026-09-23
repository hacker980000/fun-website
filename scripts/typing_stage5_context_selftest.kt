import com.socialaiassistant.keyboard.ime.BanglaInputMode
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.ImeTypingEngine
import com.socialaiassistant.keyboard.ime.InMemoryTypingLearningModel
import com.socialaiassistant.keyboard.ime.KeyboardLanguage
import com.socialaiassistant.keyboard.ime.KeyboardLayer
import com.socialaiassistant.keyboard.ime.KeyboardUiMode
import com.socialaiassistant.keyboard.ime.OfflineBanglaNextWordModel
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.Stage5ConversationCorpus

fun main() {
    var checks = 0
    val suggestions = OfflineBanglaSuggestionEngine()
    check(suggestions.dictionarySize() >= 1100)
    checks++

    val privacyFallback = HybridBanglaPhoneticTransliterator().transliterate("privacy")
    check(suggestions.suggest("privacy", privacyFallback).candidates.first().text == "প্রাইভেসি")
    checks++

    val pleaseFallback = HybridBanglaPhoneticTransliterator().transliterate("plz")
    check(suggestions.commitText("plz", pleaseFallback) == "প্লিজ")
    checks++

    val typoFallback = HybridBanglaPhoneticTransliterator().transliterate("messgae")
    val typoSnapshot = suggestions.suggest("messgae", typoFallback, 5)
    check(typoSnapshot.candidates.any { it.text == "মেসেজ" })
    checks++
    check(suggestions.commitText("messgae", typoFallback) == "মেসেজ")
    checks++

    check(Stage5ConversationCorpus.sentences.size >= 180)
    checks++

    val contextCorpus = listOf(
        "আমি এখন বাসায়", "আমি এখন অফিসে", "আমি এখন কাজে",
        "তুমি এখন কোথায়", "তুমি এখন কেমন", "তুমি এখন বাসায়"
    )
    val contextModel = OfflineBanglaNextWordModel(contextCorpus)
    val amiNow = contextModel.suggest("আমি", "এখন", 3).map { it.text }
    val tumiNow = contextModel.suggest("তুমি", "এখন", 3).map { it.text }
    check(amiNow.contains("অফিসে") || amiNow.contains("কাজে"))
    checks++
    check(tumiNow.contains("কোথায়") || tumiNow.contains("কেমন"))
    checks++
    check(contextModel.contextKeyCount() >= 2)
    checks++

    val learning = InMemoryTypingLearningModel()
    repeat(5) {
        learning.recordWord("খেলছি")
        learning.recordTransition("এখন", "খেলছি")
        learning.recordContextTransition("আমি", "এখন", "খেলছি")
    }
    val personalContextModel = OfflineBanglaNextWordModel(emptyList(), learning)
    check(personalContextModel.suggest("আমি", "এখন", 3).first().text == "খেলছি")
    checks++

    val mode = KeyboardUiMode(
        language = KeyboardLanguage.BANGLA,
        banglaMode = BanglaInputMode.PHONETIC,
        layer = KeyboardLayer.LETTERS
    )
    val engine = ImeTypingEngine()
    "ami".forEach { engine.onText(mode, it.toString()) }
    check(engine.flushWithAutocorrect() == "আমি")
    checks++
    "ekhon".forEach { engine.onText(mode, it.toString()) }
    check(engine.flushWithAutocorrect() == "এখন")
    checks++
    val next = engine.currentSuggestions().candidates.map { it.text }
    check(next.isNotEmpty())
    checks++

    println("typing_stage5_context_selftest: $checks checks PASS; dictionary=${suggestions.dictionarySize()}; stage5Corpus=${Stage5ConversationCorpus.sentences.size}")
}
