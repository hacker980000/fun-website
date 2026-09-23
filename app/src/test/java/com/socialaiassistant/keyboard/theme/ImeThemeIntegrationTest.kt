package com.socialaiassistant.keyboard.theme

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.R
import com.socialaiassistant.keyboard.ime.BanglaInputMode
import com.socialaiassistant.keyboard.ime.ImeThemeSurfaceResolver
import com.socialaiassistant.keyboard.ime.KeyboardLanguage
import com.socialaiassistant.keyboard.ime.KeyboardLayer
import com.socialaiassistant.keyboard.ime.KeyboardUiMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImeThemeIntegrationTest {
    @Test
    fun layered_keyboard_layout_contains_all_background_targets() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = LayoutInflater.from(context).inflate(R.layout.ime_keyboard, null, false)
        assertNotNull(root.findViewById<ImageView>(R.id.full_keyboard_background))
        assertNotNull(root.findViewById<ImageView>(R.id.keys_background))
        assertNotNull(root.findViewById<ImageView>(R.id.ai_panel_background))
    }

    @Test
    fun background_scope_shows_exactly_one_layer() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = LayoutInflater.from(context).inflate(R.layout.ime_keyboard, null, false)
        val renderer = ThemeRenderer(context)
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val config = BackgroundPhotoConfig(true, "x.jpg", scope = BackgroundScope.KEYS_ONLY)
        renderer.applyBackground(root, config, bitmap)
        assertEquals(View.GONE, root.findViewById<ImageView>(R.id.full_keyboard_background).visibility)
        assertEquals(View.VISIBLE, root.findViewById<ImageView>(R.id.keys_background).visibility)
        assertEquals(View.GONE, root.findViewById<ImageView>(R.id.ai_panel_background).visibility)
    }
    @Test
    fun mode_to_theme_surface_mapping_is_stable() {
        assertEquals(KeyboardThemeSurface.ENGLISH, ImeThemeSurfaceResolver.forMode(KeyboardUiMode()))
        assertEquals(
            KeyboardThemeSurface.NUMBER,
            ImeThemeSurfaceResolver.forMode(KeyboardUiMode(layer = KeyboardLayer.NUMBERS))
        )
        assertEquals(
            KeyboardThemeSurface.SYMBOL,
            ImeThemeSurfaceResolver.forMode(KeyboardUiMode(layer = KeyboardLayer.SYMBOLS))
        )
        assertEquals(
            KeyboardThemeSurface.PHONETIC,
            ImeThemeSurfaceResolver.forMode(
                KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC)
            )
        )
        assertEquals(
            KeyboardThemeSurface.BIJOY,
            ImeThemeSurfaceResolver.forMode(
                KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY)
            )
        )
    }

    @Test
    fun toolbar_theme_stays_global_when_key_surface_is_overridden() {
        val state = ThemeSelectionState(
            globalPack = ThemePack.GLASS_MODERN,
            perSurfaceOverrides = mapOf(KeyboardThemeSurface.ENGLISH to ThemePack.CLEAN_LIGHT),
            legacyActiveThemeId = ThemePreset.socialAiNeon.id,
            customTheme = ThemePreset.customFrom()
        )
        assertEquals("premium_glass_modern_chrome", ThemeResolutionPolicy.resolveGlobalChrome(state).id)
        assertTrue(ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.ENGLISH).id.contains("clean_light"))
    }

}
