package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class BanglaPhoneticComposerTest {
    @Test
    fun ami_produces_bangla() = assertWord("ami", "আমি")

    @Test
    fun bangla_produces_bangla() = assertWord("bangla", "বাংলা")

    @Test
    fun common_conversation_words_are_resolved() {
        val cases = mapOf(
            "tumi" to "তুমি",
            "tomar" to "তোমার",
            "kemon" to "কেমন",
            "acho" to "আছো",
            "bhalobashi" to "ভালোবাসি",
            "korbo" to "করবো",
            "jacchi" to "যাচ্ছি",
            "dhonnobad" to "ধন্যবাদ",
            "bangladesh" to "বাংলাদেশ",
            "engineer" to "ইঞ্জিনিয়ার"
        )
        cases.forEach { (roman, bangla) -> assertWord(roman, bangla) }
    }

    @Test
    fun generic_fallback_handles_non_lexicon_words() {
        assertWord("kalo", "কালো")
        assertWord("pani", "পানি")
    }

    @Test
    fun case_sensitive_tokens_are_not_destroyed() {
        assertWord("T", "ট")
        assertWord("t", "ত")
    }

    @Test
    fun backspace_updates_preview() {
        val composer = BanglaPhoneticComposer()
        "ami".forEach { composer.acceptLatin(it.toString()) }
        val result = composer.backspace()
        assertEquals("আম", result.composing)
        assertEquals("am", composer.romanBuffer())
    }

    @Test
    fun unknown_sequence_is_preserved() {
        val composer = BanglaPhoneticComposer()
        "@@".forEach { composer.acceptLatin(it.toString()) }
        assertEquals("@@", composer.flush())
    }

    private fun assertWord(roman: String, expected: String) {
        val composer = BanglaPhoneticComposer()
        roman.forEach { composer.acceptLatin(it.toString()) }
        assertEquals(expected, composer.flush())
    }
}
