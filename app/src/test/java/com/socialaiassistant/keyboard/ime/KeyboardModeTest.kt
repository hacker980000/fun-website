package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardModeTest {
    @Test
    fun english_toggles_to_bangla_and_keeps_letters_layer() {
        val start = KeyboardUiMode()
        val changed = start.reduce(KeyboardAction.ToggleLanguage)
        assertEquals(KeyboardLanguage.BANGLA, changed.language)
        assertEquals(KeyboardLayer.LETTERS, changed.layer)
    }

    @Test
    fun bangla_mode_toggles_between_phonetic_and_bijoy() {
        val start = KeyboardUiMode(language = KeyboardLanguage.BANGLA)
        val changed = start.reduce(KeyboardAction.ToggleBanglaMode)
        assertEquals(BanglaInputMode.BIJOY, changed.banglaMode)
    }
}
