package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardTextPolicyTest {
    @Test
    fun sanitize_drops_blank_and_limits_items() {
        val source = listOf(" one ", "", "two", "three", "four", "five", "six", "seven", "eight", "nine")
        val result = ClipboardTextPolicy.sanitize(source)
        assertEquals(8, result.size)
        assertEquals("one", result.first())
        assertEquals("eight", result.last())
    }

    @Test
    fun label_is_short_but_commit_value_is_not_changed() {
        val original = "a".repeat(80)
        val label = ClipboardTextPolicy.label(original)
        assertTrue(label.length <= 43)
        assertTrue(label.endsWith("…"))
        assertEquals(80, original.length)
    }

    @Test
    fun exposure_is_blocked_for_sensitive_field_or_sensitive_clip() {
        assertTrue(ClipboardTextPolicy.shouldExposeContent(fieldSensitive = false, clipMarkedSensitive = false))
        assertTrue(!ClipboardTextPolicy.shouldExposeContent(fieldSensitive = true, clipMarkedSensitive = false))
        assertTrue(!ClipboardTextPolicy.shouldExposeContent(fieldSensitive = false, clipMarkedSensitive = true))
        assertTrue(!ClipboardTextPolicy.shouldExposeContent(fieldSensitive = true, clipMarkedSensitive = true))
    }
}
