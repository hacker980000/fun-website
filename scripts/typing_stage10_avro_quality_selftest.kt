import com.socialaiassistant.keyboard.ime.AvroCompatibleBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.AvroRomanNormalizer
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.ProductionBanglaLexicon

fun main() {
    var checks = 0
    val classic = AvroCompatibleBanglaPhoneticTransliterator()

    val streamCases = linkedMapOf(
        "ami, tumi?" to "আমি, তুমি?",
        "123" to "১২৩",
        "." to "।",
        ".." to "।।",
        ".`" to ".",
        ":" to "ঃ",
        ":`" to ":",
        "^" to "ঁ",
        "^`" to "^",
        "$" to "৳"
    )
    streamCases.forEach { (roman, expected) ->
        check(classic.transliterate(roman) == expected) { "$roman stream mapping failed" }
        checks++
    }

    val caseAndPhalaCases = linkedMapOf(
        "Y" to "য়",
        "Z" to "্য",
        "yahoo" to "ইয়াহু",
        "ky" to "ক্য",
        "wa" to "ওয়া",
        "w" to "ও",
        "kw" to "ক্ব",
        "x" to "এক্স"
    )
    caseAndPhalaCases.forEach { (roman, expected) ->
        check(classic.transliterate(roman) == expected) { "$roman y/w/x semantics failed" }
        checks++
    }

    val clusterCases = linkedMapOf(
        "gg" to "জ্ঞ",
        "jNG" to "জ্ঞ",
        "kSh" to "ক্ষ",
        "kx" to "ক্ষ",
        "ksh" to "কশ",
        "nk" to "ঙ্ক",
        "nc" to "ঞ্চ",
        "nj" to "ঞ্জ",
        "nd" to "ন্দ",
        "nt" to "ন্ত",
        "nD" to "ন্ড",
        "ND" to "ণ্ড",
        "ngh" to "ঙ্ঘ",
        "nga" to "ঙ্গা",
        "ngi" to "ঙ্গি",
        "ngO" to "ঙ্গো"
    )
    clusterCases.forEach { (roman, expected) ->
        check(classic.transliterate(roman) == expected) { "$roman cluster failed" }
        checks++
    }

    val forcedKarCases = linkedMapOf(
        "a`" to "া",
        "i`" to "ি",
        "I`" to "ী",
        "u`" to "ু",
        "U`" to "ূ",
        "e`" to "ে",
        "O`" to "ো",
        "OI`" to "ৈ",
        "OU`" to "ৌ",
        "rri`" to "ৃ",
        "o`" to ""
    )
    forcedKarCases.forEach { (roman, expected) ->
        check(classic.transliterate(roman) == expected) { "$roman forced kar failed" }
        checks++
    }

    check(AvroRomanNormalizer.normalizeCase("KBCF") == "kbcf")
    checks++
    check(AvroRomanNormalizer.normalizeCase("OIDGJNRSTYZ") == "OIDGJNRSTYZ")
    checks++
    check(AvroRomanNormalizer.normalizeCase("AMI") == "amI")
    checks++

    val hybrid = HybridBanglaPhoneticTransliterator()
    check(hybrid.transliterate("ami") == "আমি")
    checks++
    check(hybrid.transliterate("AMI") != "আমি") // meaningful capital I must not be flattened to lowercase dictionary input
    checks++
    check(hybrid.transliterate("ami, tumi?") == "আমি, তুমি?")
    checks++

    check(ProductionBanglaLexicon.words.size >= 1100)
    checks++
    check(ProductionBanglaLexicon.words.none { it.key.isBlank() || it.value.isBlank() })
    checks++

    println("typing_stage10_avro_quality_selftest: $checks checks PASS; dictionary=${ProductionBanglaLexicon.words.size}")
}
