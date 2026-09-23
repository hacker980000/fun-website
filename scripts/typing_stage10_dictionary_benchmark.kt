import com.socialaiassistant.keyboard.ime.AvroRomanNormalizer
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import com.socialaiassistant.keyboard.ime.ProductionBanglaLexicon

fun main() {
    val lexicon = ProductionBanglaLexicon.words
    val invalidKeys = lexicon.keys.filter { key -> key.any { !(it.isLetter() || it == ' ' || it == '-' || it == '\'') } }
    val normalizationConflicts = lexicon.entries
        .groupBy { AvroRomanNormalizer.normalizeCase(it.key) }
        .filterValues { entries -> entries.map { it.value }.distinct().size > 1 }

    val engine = HybridBanglaPhoneticTransliterator()
    val dictionaryMismatches = lexicon.entries.count { (roman, bangla) -> engine.transliterate(roman) != bangla }

    val sample = lexicon.keys.take(400)
    val started = System.nanoTime()
    repeat(20) { sample.forEach(engine::transliterate) }
    val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
    val operations = sample.size * 20

    println("Stage10 dictionary quality benchmark")
    println("entries=${lexicon.size}")
    println("blankEntries=${lexicon.count { it.key.isBlank() || it.value.isBlank() }}")
    println("invalidRomanKeys=${invalidKeys.size}")
    println("normalizationConflicts=${normalizationConflicts.size}")
    println("dictionaryExactMismatches=$dictionaryMismatches")
    println("sampleOperations=$operations")
    println("sampleElapsedMs=${"%.2f".format(elapsedMs)}")

    check(lexicon.size >= 1100)
    check(lexicon.none { it.key.isBlank() || it.value.isBlank() })
    check(normalizationConflicts.isEmpty()) { "conflicting dictionary outputs after Avro case normalization: ${normalizationConflicts.keys.take(10)}" }
    check(dictionaryMismatches == 0)
    println("typing_stage10_dictionary_benchmark: QUALITY GATES PASS")
}
