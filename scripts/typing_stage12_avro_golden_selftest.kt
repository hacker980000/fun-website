import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.ProductionBanglaLexicon
import com.socialaiassistant.keyboard.ime.Stage12AvroGoldenLexiconPack

fun main() {
    val engine = HybridBanglaPhoneticTransliterator()
    var checks = 0
    Stage12AvroGoldenLexiconPack.words.forEach { (roman, expected) ->
        val actual = engine.transliterate(roman)
        check(actual == expected) { "$roman -> $actual (expected $expected)" }
        checks++
    }
    check(Stage12AvroGoldenLexiconPack.words.size == 14)
    checks++
    check(ProductionBanglaLexicon.words.size == 1158)
    checks++
    check(engine.transliterate("ami, tumi?") == "আমি, তুমি?")
    checks++
    check(engine.transliterate("123") == "১২৩")
    checks++
    println("TYPING STAGE 12 AVRO GOLDEN SELF-TEST: PASS ($checks/18); dictionary=${ProductionBanglaLexicon.words.size}")
}
