package com.socialaiassistant.keyboard.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePresetTest {
    @Test
    fun builtIns_areCompleteAndUnique() {
        assertEquals(8, ThemePreset.builtIns.size)
        assertEquals(8, ThemePreset.builtIns.map { it.id }.toSet().size)
        ThemePreset.builtIns.forEach { assertTrue(it.isValid()) }
    }

    @Test
    fun numericTokens_areClamped() {
        val theme = ThemePreset.socialAiNeon.copy(
            glowStrength = 999,
            keyCornerRadiusDp = -10f,
            keyGapDp = 99f,
            keyLabelScale = 9f
        ).normalized()
        assertEquals(100, theme.glowStrength)
        assertEquals(4f, theme.keyCornerRadiusDp)
        assertEquals(12f, theme.keyGapDp)
        assertEquals(1.35f, theme.keyLabelScale)
    }

    @Test
    fun backgroundConfig_isNormalized() {
        val config = BackgroundPhotoConfig(
            enabled = true,
            localFileName = "../bad.jpg",
            opacityPercent = 250,
            darkOverlayPercent = -7,
            blurAmount = 99
        ).normalized()
        assertEquals("bad.jpg", config.localFileName)
        assertEquals(100, config.opacityPercent)
        assertEquals(0, config.darkOverlayPercent)
        assertEquals(30, config.blurAmount)
    }
}
