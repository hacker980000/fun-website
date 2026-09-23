import com.socialaiassistant.keyboard.ime.AvroCompatibleBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.GlideTypingEngine
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator

fun main() {
    var checks = 0
    val classic = AvroCompatibleBanglaPhoneticTransliterator()

    val classicCases = linkedMapOf(
        "ami" to "আমি",
        "bangla" to "বাংলা",
        "ami banglay gan gai" to "আমি বাংলায় গান গাই",
        "Ng" to "ঙ",
        "NG" to "ঞ",
        "ng" to "ং",
        "c" to "চ",
        "ch" to "ছ",
        "J" to "জ",
        "z" to "য",
        "yahoo" to "ইয়াহু",
        "Y" to "য়",
        "S" to "শ",
        "Sh" to "ষ",
        "R" to "ড়",
        "Rh" to "ঢ়",
        "t``" to "ৎ",
        ":" to "ঃ",
        "^" to "ঁ",
        "rri" to "ঋ",
        "krri" to "কৃ",
        "oo" to "উ",
        "koo" to "কু",
        "O" to "ও",
        "kO" to "কো",
        "OU" to "ঔ",
        "kOU" to "কৌ",
        "kor" to "কর",
        "kalo" to "কালো"
    )
    for ((roman, expected) in classicCases) {
        val actual = classic.transliterate(roman)
        check(actual == expected) { "$roman -> $actual (expected $expected)" }
        checks++
    }

    val hybrid = HybridBanglaPhoneticTransliterator(
        lexicon = mapOf("valo" to "ভালো"),
        classic = classic
    )
    check(hybrid.transliterate("valo") == "ভালো")
    checks++
    check(hybrid.transliterate("Ng") == "ঙ")
    checks++

    val glide = GlideTypingEngine()
    check(glide.bestEnglish("helo")?.text == "hello")
    checks++
    check(glide.bestEnglish("therw")?.text == "there")
    checks++
    check(glide.bestEnglish("mesage")?.text == "message")
    checks++
    check(glide.resolveEnglish("thnks", 3).any { it.text == "thanks" })
    checks++
    check(glide.bestEnglish("to") == null)
    checks++
    check(glide.bestEnglish("zmx") == null)
    checks++

    check(glide.bestBangla("ami")?.text == "আমি")
    checks++
    check(glide.bestBangla("valp")?.text == "ভালো")
    checks++
    check(glide.resolveBangla("kmn", 3).any { it.text == "কেমন" })
    checks++
    check(glide.bestBangla("bangls")?.text == "বাংলা")
    checks++

    println("typing_stage9_avro_glide_selftest: $checks checks PASS")
}
