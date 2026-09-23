package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutBanglaTest {
    @Test
    fun bijoy_letters_expose_bengali_glyphs() {
        val layout = KeyboardLayout.forMode(
            KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY)
        )
        assertTrue(layout.rows.flatten().any { it.output == "ক" })
        assertTrue(layout.rows.flatten().any { it.output == "া" })
    }

    @Test
    fun number_layer_exposes_digits_and_return_to_letters() {
        val layout = KeyboardLayout.forMode(KeyboardUiMode(layer = KeyboardLayer.NUMBERS))
        val keys = layout.rows.flatten()
        assertTrue(keys.any { it.output == "1" })
        assertTrue(keys.any { it.action == KeyboardAction.ShowLetters })
    }
}
