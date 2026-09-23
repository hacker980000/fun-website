import com.socialaiassistant.keyboard.ime.GlideTypingEngine
import com.socialaiassistant.keyboard.ime.OfflineBanglaSuggestionEngine
import com.socialaiassistant.keyboard.ime.HybridBanglaPhoneticTransliterator
import kotlin.system.measureNanoTime

fun main() {
    val glideInitNs = measureNanoTime { repeat(5) { GlideTypingEngine() } }
    val glide = GlideTypingEngine()
    val glideQueries = listOf("helo", "mesage", "thnks", "keyboard", "project", "ami", "valo", "kmn", "bangla")
    var glideHits = 0
    val glideQueryNs = measureNanoTime {
        repeat(150) {
            glideQueries.forEach { query ->
                if (glide.resolveEnglish(query, 3).isNotEmpty() || glide.resolveBangla(query, 3).isNotEmpty()) glideHits++
            }
        }
    }

    val suggestion = OfflineBanglaSuggestionEngine()
    val transliterator = HybridBanglaPhoneticTransliterator()
    val banglaQueries = listOf("ami", "amr", "kemon", "kmn", "valo", "message", "project", "dhonnobad", "bangla")
    var suggestionHits = 0
    val suggestionNs = measureNanoTime {
        repeat(250) {
            banglaQueries.forEach { query ->
                val fallback = transliterator.transliterate(query)
                if (suggestion.suggest(query, fallback, 3).candidates.isNotEmpty()) suggestionHits++
            }
        }
    }

    val glideInitMs = glideInitNs / 1_000_000.0
    val glideQueryMs = glideQueryNs / 1_000_000.0
    val suggestionMs = suggestionNs / 1_000_000.0
    check(glideHits > 0)
    check(suggestionHits > 0)
    // Deliberately generous JVM regression budgets: detect algorithmic blow-ups, not machine speed.
    check(glideInitMs < 5_000.0) { "Glide init regression: ${glideInitMs}ms" }
    check(glideQueryMs < 8_000.0) { "Glide query regression: ${glideQueryMs}ms" }
    check(suggestionMs < 8_000.0) { "Suggestion query regression: ${suggestionMs}ms" }
    println("TYPING STAGE 14 PERFORMANCE BENCHMARK: PASS")
    println("glide_init_5x_ms=%.2f glide_queries_%d_ms=%.2f suggestion_queries_%d_ms=%.2f".format(
        glideInitMs, 150 * glideQueries.size, glideQueryMs, 250 * banglaQueries.size, suggestionMs
    ))
}
