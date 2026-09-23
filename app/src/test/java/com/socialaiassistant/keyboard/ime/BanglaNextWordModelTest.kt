package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BanglaNextWordModelTest {
    @Test
    fun expanded_dictionary_is_large_and_indexed() {
        val index = BanglaLexiconIndex()
        assertTrue(index.size() >= 700)
        assertEquals("দরকার", index.exact("dorkar")?.bangla)
        assertTrue(index.prefix("key", 5).any { it.bangla == "কিবোর্ড" })
    }

    @Test
    fun common_context_returns_local_next_word_predictions() {
        val model = OfflineBanglaNextWordModel()
        val predictions = model.suggest("আমি", 3)
        assertTrue(predictions.isNotEmpty())
        assertTrue(predictions.any { it.text == "এখন" })
    }

    @Test
    fun in_memory_learning_can_re_rank_a_transition() {
        val learning = InMemoryTypingLearningModel()
        val model = OfflineBanglaNextWordModel(listOf("আমি ভালো", "আমি এখন"), learning)
        repeat(5) { model.learnTransition("আমি", "এখন") }
        assertEquals("এখন", model.suggest("আমি", 2).first().text)
    }
}
