package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineBanglaSuggestionEngineTest {
    private val transliterator = HybridBanglaPhoneticTransliterator()
    private val engine = OfflineBanglaSuggestionEngine()

    @Test
    fun prefix_candidates_rank_common_words_first() {
        val snapshot = engine.suggest("am", transliterator.transliterate("am"))
        assertEquals("আমি", snapshot.candidates.first().text)
        assertTrue(snapshot.candidates.map { it.text }.contains("আমার"))
    }

    @Test
    fun exact_candidate_is_first() {
        val snapshot = engine.suggest("ami", transliterator.transliterate("ami"))
        assertEquals(SuggestionKind.EXACT, snapshot.candidates.first().kind)
        assertEquals("আমি", snapshot.candidates.first().text)
    }

    @Test
    fun safe_shorthand_alias_can_autocorrect() {
        val snapshot = engine.suggest("amr", transliterator.transliterate("amr"))
        assertEquals("আমার", snapshot.candidates.first().text)
        assertEquals(SuggestionKind.ALIAS, snapshot.candidates.first().kind)
        assertEquals("আমার", snapshot.autocorrectText)
        assertEquals("আমার", engine.commitText("amr", transliterator.transliterate("amr")))
    }

    @Test
    fun fuzzy_typo_is_not_auto_applied() {
        val rendered = transliterator.transliterate("kalo")
        val snapshot = engine.suggest("kalo", rendered)
        assertEquals("কালো", snapshot.candidates.first().text)
        assertNull(snapshot.autocorrectText)
        assertEquals("কালো", engine.commitText("kalo", rendered))
    }

    @Test
    fun candidate_texts_are_deduplicated() {
        val snapshot = engine.suggest("bha", transliterator.transliterate("bha"))
        assertEquals(snapshot.candidates.map { it.text }.distinct().size, snapshot.candidates.size)
    }
}
