package com.socialaiassistant.keyboard.theme

import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import com.socialaiassistant.keyboard.R
import com.socialaiassistant.keyboard.ThemeSettingsActivity
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.SettingsCategoryActivity
import com.socialaiassistant.keyboard.settingsui.SettingsCategoryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeSettingsActivityTest {
    @Test
    fun activity_renders_four_premium_pack_choices_without_per_keyboard_customizer() {
        val activity = Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().get()
        val packs = activity.findViewById<LinearLayout>(R.id.theme_pack_container)
        val scope = activity.findViewById<Spinner>(R.id.background_scope_spinner)
        assertEquals(4, packs.childCount)
        assertEquals(3, scope.adapter.count)
    }

    @Test
    fun legacy_presets_remain_available() {
        val activity = Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().get()
        val legacy = activity.findViewById<LinearLayout>(R.id.theme_legacy_preset_container)
        assertEquals(8, legacy.childCount)
    }

    @Test
    fun reset_confirmation_cancel_does_not_invoke_repository_reset() {
        val activity = Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().get()
        assertFalse(activity.resetMutationStartedForTest())
        activity.showResetConfirmationForTest()
        activity.cancelResetForTest()
        assertFalse(activity.resetMutationStartedForTest())
    }

    @Test
    fun preview_tracks_requested_surface() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = ThemePreviewView(context)
        preview.setPreview(
            ThemeCatalog.surface(ThemePack.GLASS_MODERN, KeyboardThemeSurface.BIJOY),
            KeyboardThemeSurface.BIJOY
        )
        assertEquals(KeyboardThemeSurface.BIJOY, preview.previewSurfaceForTest())
    }

    @Test
    fun theme_appearance_category_has_theme_settings_entry() {
        val intent = SettingsCategoryActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            SettingsCategoryId.THEME_APPEARANCE
        )
        val activity = Robolectric.buildActivity(SettingsCategoryActivity::class.java, intent).setup().get()
        assertNotNull(activity.findViewById<Button>(R.id.button_theme_settings))
    }
}
