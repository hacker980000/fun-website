package com.socialaiassistant.keyboard.theme

import android.app.Activity
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeRendererTest {
    @Test
    fun renderer_applies_distinct_ai_and_enter_styles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val renderer = ThemeRenderer(context)
        val ai = Button(context)
        val enter = Button(context)
        val theme = ThemePreset.socialAiNeon

        renderer.styleButton(ai, ThemeButtonRole.AI, theme, selected = true)
        renderer.styleButton(enter, ThemeButtonRole.ENTER, theme)

        assertNotNull(ai.background)
        assertNotNull(enter.background)
        assertNotSame(ai.background, enter.background)
        assertEquals(theme.specialKeyLabel, ai.currentTextColor)
        assertEquals(theme.specialKeyLabel, enter.currentTextColor)
    }

    @Test
    fun disabled_button_uses_disabled_label_color() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val renderer = ThemeRenderer(context)
        val button = Button(context).apply { isEnabled = false }
        renderer.styleButton(button, ThemeButtonRole.AI, ThemePreset.socialAiNeon)
        assertEquals(ThemePreset.socialAiNeon.disabledLabel, button.currentTextColor)
    }
    @Test
    fun visual_signature_changes_when_fill_style_or_gradient_changes() {
        val base = ThemePreset.socialAiNeon
        val gradient = base.copy(
            fillStyle = ThemeFillStyle.GRADIENT,
            keyGradientStart = 0xFF5B2EFF.toInt(),
            keyGradientEnd = 0xFF0EA5E9.toInt()
        )
        assertNotEquals(base.visualSignature(), gradient.visualSignature())
    }

    @Test
    fun old_preset_defaults_to_solid_and_stays_valid() {
        val theme = ThemePreset.socialAiNeon
        assertEquals(ThemeFillStyle.SOLID, theme.fillStyle)
        assertTrue(theme.isValid())
    }

}
