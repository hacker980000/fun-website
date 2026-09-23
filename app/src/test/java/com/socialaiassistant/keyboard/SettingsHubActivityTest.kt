package com.socialaiassistant.keyboard

import android.content.Intent
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SettingsHubActivityTest {
    @Test fun all_six_premium_sections_exist() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        listOf(
            R.id.section_ai_privacy,
            R.id.section_theme_appearance,
            R.id.section_keyboard_preferences,
            R.id.section_language_input,
            R.id.section_account_subscription,
            R.id.section_help_support
        ).forEach { assertNotNull(activity.findViewById<LinearLayout>(it)) }
    }

    @Test fun ai_privacy_deep_link_focuses_ai_privacy_section() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_SECTION, MainActivity.SECTION_AI_PRIVACY)
        val activity = Robolectric.buildActivity(MainActivity::class.java, intent).setup().get()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.findViewById<LinearLayout>(R.id.section_ai_privacy).hasFocus())
    }

    @Test fun ai_privacy_and_preserve_draft_persist() = runBlocking {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.findViewById<CheckBox>(R.id.ai_privacy_consent_checkbox).isChecked = true
        activity.findViewById<CheckBox>(R.id.preserve_draft_checkbox).isChecked = false
        activity.findViewById<Button>(R.id.button_save_ai_privacy_consent).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        val current = SettingsRepository.create(activity).current()
        assertTrue(current.aiPrivacyConsent)
        assertFalse(current.preserveDraft)
    }

    @Test fun theme_card_opens_existing_theme_editor() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.findViewById<Button>(R.id.button_theme_settings).performClick()
        val started = shadowOf(activity).nextStartedActivity
        assertEquals(ThemeSettingsActivity::class.java.name, started.component?.className)
    }

    @Test fun settings_screen_requests_no_media_or_storage_permissions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions?.toSet().orEmpty()
        assertFalse("android.permission.READ_MEDIA_IMAGES" in permissions)
        assertFalse("android.permission.READ_EXTERNAL_STORAGE" in permissions)
        assertFalse("android.permission.WRITE_EXTERNAL_STORAGE" in permissions)
    }
}
