package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogTest {
    @Test
    fun catalog_has_twenty_categories_with_fifty_unique_emojis_each() {
        val categories = EmojiCatalog.categories()
        val all = categories.flatMap { it.emojis }

        assertEquals(20, categories.size)
        assertEquals(20, categories.map { it.id }.distinct().size)
        categories.forEach { category ->
            assertEquals("${category.label} should contain 50 emojis", 50, category.emojis.size)
            assertEquals(category.emojis.size, category.emojis.distinct().size)
        }
        assertEquals(1000, all.size)
        assertEquals(1000, all.distinct().size)
    }

    @Test
    fun categories_keep_expected_representative_emojis() {
        val representatives = mapOf(
            "smileys" to "😀", "people" to "🧑", "love" to "❤️", "gestures" to "👍",
            "body" to "👀", "animals" to "🐶", "nature" to "☀️", "food_drink" to "🍔",
            "activities" to "🎮", "sports" to "⚽", "travel" to "🚗", "places" to "🏠",
            "objects" to "🔑", "technology" to "💻", "fashion" to "👕", "work_study" to "📚",
            "health_fitness" to "🩺", "celebration" to "🎉", "symbols" to "✅", "flags" to "🇧🇩"
        )
        representatives.forEach { (id, emoji) ->
            assertTrue("$id should contain $emoji", emoji in EmojiCatalog.category(id).emojis)
        }
    }

    @Test
    fun previous_common_emojis_are_preserved_in_expanded_catalog() {
        val all = EmojiCatalog.categories().flatMap { it.emojis }.toSet()
        EmojiCatalog.recentAndCommon().forEach { emoji ->
            assertTrue("Missing previous common emoji: $emoji", emoji in all)
        }
    }
}
