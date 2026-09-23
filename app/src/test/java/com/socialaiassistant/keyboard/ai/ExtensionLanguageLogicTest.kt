package com.socialaiassistant.keyboard.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtensionLanguageLogicTest {
    @Test
    fun clean_string_matches_extension_whitespace_and_length_behavior() {
        val dirty = "  hello  \n\n\n\nworld\u0000  "
        assertEquals("hello\n\nworld", ExtensionLanguageLogic.cleanString(dirty, 100))
        assertEquals("abc", ExtensionLanguageLogic.cleanString("abcdef", 3))
    }

    @Test
    fun invalid_mode_falls_back_to_general() {
        assertEquals(AiMode.GENERAL, AiMode.normalize("something_else"))
        assertEquals(AiMode.WITTY, AiMode.normalize("WITTY"))
    }

    @Test
    fun detects_bengali_and_banglish_before_generic_latin() {
        assertEquals(LanguageMode.BENGALI, ExtensionLanguageLogic.detectLanguageMode("কেমন আছেন?"))
        assertEquals(LanguageMode.BANGLISH, ExtensionLanguageLogic.detectLanguageMode("kemon acho tumi"))
        assertEquals(LanguageMode.LATIN_INFER, ExtensionLanguageLogic.detectLanguageMode("How are you today?"))
    }


    @Test
    fun builds_distinct_bengali_and_banglish_prompt_policies() {
        val bengali = ExtensionLanguageLogic.buildInboxLanguageInstruction(LanguageMode.BENGALI)
        val banglish = ExtensionLanguageLogic.buildInboxLanguageInstruction(LanguageMode.BANGLISH)
        assertEquals(true, bengali.contains("Bengali script"))
        assertEquals(true, banglish.contains("Latin letters"))
        assertEquals(true, banglish.contains("Do not convert Banglish into Bengali script"))
    }

    @Test
    fun source_scripts_are_preserved() {
        assertEquals(LanguageMode.SOURCE_LANGUAGE, ExtensionLanguageLogic.detectLanguageMode("مرحبا كيف حالك"))
        assertEquals(LanguageMode.SOURCE_LANGUAGE, ExtensionLanguageLogic.detectLanguageMode("आप कैसे हैं"))
        assertEquals(LanguageMode.SOURCE_LANGUAGE, ExtensionLanguageLogic.detectLanguageMode("Привет"))
        assertEquals(LanguageMode.SOURCE_LANGUAGE, ExtensionLanguageLogic.detectLanguageMode("こんにちは"))
    }

    @Test
    fun emoji_only_has_no_language_signal() {
        assertNull(ExtensionLanguageLogic.detectLanguageMode("😊❤️"))
    }

    @Test
    fun latest_recipient_language_wins_then_cache_then_bengali_default() {
        assertEquals(
            LanguageMode.LATIN_INFER,
            ExtensionLanguageLogic.resolveInboxLanguageMode("How are you?", LanguageMode.BENGALI)
        )
        assertEquals(
            LanguageMode.BANGLISH,
            ExtensionLanguageLogic.resolveInboxLanguageMode("😊", LanguageMode.BANGLISH)
        )
        assertEquals(
            LanguageMode.BENGALI_DEFAULT,
            ExtensionLanguageLogic.resolveInboxLanguageMode("😊", null)
        )
    }
}
