package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeTypingEngineTest {
    @Test
    fun phonetic_text_updates_composing_bangla() {
        val engine = ImeTypingEngine()
        val mode = KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC)
        engine.onText(mode, "a")
        engine.onText(mode, "m")
        val result = engine.onText(mode, "i")
        assertEquals("আমি", result.composingText)
        assertEquals(null, result.directCommit)
    }

    @Test
    fun english_text_commits_directly() {
        val engine = ImeTypingEngine()
        val result = engine.onText(KeyboardUiMode(), "q")
        assertEquals("q", result.directCommit)
        assertEquals(null, result.composingText)
    }

    @Test
    fun flush_returns_phonetic_word_and_clears_composition() {
        val engine = ImeTypingEngine()
        val mode = KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC)
        "ami".forEach { engine.onText(mode, it.toString()) }
        assertEquals("আমি", engine.flush())
        assertTrue(engine.flush().isEmpty())
    }
}
