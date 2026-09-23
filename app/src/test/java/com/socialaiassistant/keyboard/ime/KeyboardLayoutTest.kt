package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun qwerty_layout_contains_expected_rows() {
        val rows = KeyboardLayout.englishQwerty().rows.map { it.map(KeySpec::label) }
        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), rows.first())
        assertTrue(rows.flatten().contains("⌫"))
        assertTrue(rows.flatten().contains("↵"))
    }

    @Test
    fun shifted_layout_uppercases_letters_only() {
        val shifted = KeyboardLayout.englishQwerty(shifted = true).rows.flatten()
        assertTrue(shifted.any { it.label == "Q" && it.output == "Q" })
        assertTrue(shifted.any { it.label == "⌫" })
    }
}
