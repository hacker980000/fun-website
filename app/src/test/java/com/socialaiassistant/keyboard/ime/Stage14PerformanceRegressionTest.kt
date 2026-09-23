package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * Coarse performance regression guard.
 *
 * Budgets are intentionally generous so the test catches accidental O(dictionary)
 * blow-ups / repeated re-indexing without pretending a desktop JVM is an Android
 * latency benchmark. Real-device numbers are collected by the Stage 14 ADB harness.
 */
class Stage14PerformanceRegressionTest {
    @Test
    fun glideAndBanglaSuggestionsStayWithinCoarseJvmBudget() {
        val glideInitMs = measureNanoTime { repeat(5) { GlideTypingEngine() } } / 1_000_000.0
        val glide = GlideTypingEngine()
        val glideQueries = listOf("helo", "mesage", "thnks", "keyboard", "project", "ami", "valo", "kmn", "bangla")
        var glideHits = 0
        val glideQueryMs = measureNanoTime {
            repeat(150) {
                glideQueries.forEach { query ->
                    if (glide.resolveEnglish(query, 3).isNotEmpty() || glide.resolveBangla(query, 3).isNotEmpty()) {
                        glideHits++
                    }
                }
            }
        } / 1_000_000.0

        val suggestion = OfflineBanglaSuggestionEngine()
        val transliterator = HybridBanglaPhoneticTransliterator()
        val banglaQueries = listOf("ami", "amr", "kemon", "kmn", "valo", "message", "project", "dhonnobad", "bangla")
        var suggestionHits = 0
        val suggestionMs = measureNanoTime {
            repeat(250) {
                banglaQueries.forEach { query ->
                    if (suggestion.suggest(query, transliterator.transliterate(query), 3).candidates.isNotEmpty()) {
                        suggestionHits++
                    }
                }
            }
        } / 1_000_000.0

        assertTrue(glideHits > 0)
        assertTrue(suggestionHits > 0)
        assertTrue("Glide init regression: ${glideInitMs}ms", glideInitMs < 5_000.0)
        assertTrue("Glide query regression: ${glideQueryMs}ms", glideQueryMs < 8_000.0)
        assertTrue("Suggestion query regression: ${suggestionMs}ms", suggestionMs < 8_000.0)
    }
}
