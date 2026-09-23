import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.SuggestionKind

fun main() {
    val transliterator = HybridBanglaPhoneticTransliterator()
    val engine = OfflineBanglaSuggestionEngine()
    var checks = 0

    val am = engine.suggest("am", transliterator.transliterate("am"))
    check(am.candidates.first().text == "আমি")
    checks++
    check(am.candidates.map { it.text }.contains("আমার"))
    checks++

    val ami = engine.suggest("ami", transliterator.transliterate("ami"))
    check(ami.candidates.first().kind == SuggestionKind.EXACT)
    checks++
    check(ami.candidates.first().text == "আমি")
    checks++

    val amr = engine.suggest("amr", transliterator.transliterate("amr"))
    check(amr.candidates.first().kind == SuggestionKind.ALIAS)
    checks++
    check(amr.candidates.first().text == "আমার")
    checks++
    check(amr.autocorrectText == "আমার")
    checks++
    check(engine.commitText("amr", transliterator.transliterate("amr")) == "আমার")
    checks++

    val kaloRendered = transliterator.transliterate("kalo")
    val kalo = engine.suggest("kalo", kaloRendered)
    check(kalo.candidates.first().text == "কালো")
    checks++
    check(kalo.autocorrectText == null)
    checks++
    check(engine.commitText("kalo", kaloRendered) == "কালো")
    checks++

    val kmn = engine.suggest("kmn", transliterator.transliterate("kmn"))
    check(kmn.autocorrectText == "কেমন")
    checks++

    val prefix = engine.suggest("to", transliterator.transliterate("to"))
    check(prefix.candidates.size <= 3)
    checks++
    check(prefix.candidates.map { it.text }.distinct().size == prefix.candidates.size)
    checks++

    println("suggestion_stage2_selftest: $checks checks PASS")
}
