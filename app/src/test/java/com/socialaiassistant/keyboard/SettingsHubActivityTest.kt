package com.socialaiassistant.keyboard

import android.content.Intent
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.settings.SettingsRepository
import com.socialaiassistant.keyboard.settingsui.SettingsCategoryId
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

    @Test
    fun settings_dashboard_loads() {
        val activity =
            Robolectric.buildActivity(MainActivity::class.java)
                .setup()
                .get()

        assertNotNull(
            activity.findViewById<LinearLayout>(
                R.id.settings_theme_dashboard_container
            )
        )
    }

    @Test
    fun ai_privacy_deep_link_opens_settings_category_activity() {
        val intent =
            Intent(
                ApplicationProvider.getApplicationContext(),
                MainActivity::class.java
            )
                .putExtra(
                    MainActivity.EXTRA_OPEN_SECTION,
                    MainActivity.SECTION_AI_PRIVACY
                )

        val activity =
            Robolectric.buildActivity(
                MainActivity::class.java,
                intent
            )
                .setup()
                .get()

        shadowOf(Looper.getMainLooper()).idle()

        val started =
            shadowOf(activity).nextStartedActivity

        assertEquals(
            SettingsCategoryActivity::class.java.name,
            started.component?.className
        )
    }

    @Test
    fun ai_privacy_and_preserve_draft_persist() = runBlocking {

        val intent =
            SettingsCategoryActivity.createIntent(
                ApplicationProvider.getApplicationContext(),
                SettingsCategoryId.AI_PRIVACY
            )

        val activity =
            Robolectric.buildActivity(
                SettingsCategoryActivity::class.java,
                intent
            )
                .setup()
                .get()

        activity.findViewById<CheckBox>(
            R.id.ai_privacy_consent_checkbox
        ).isChecked = true

        activity.findViewById<CheckBox>(
            R.id.preserve_draft_checkbox
        ).isChecked = false

        activity.findViewById<Button>(
            R.id.button_save_ai_privacy_consent
        ).performClick()

        shadowOf(Looper.getMainLooper()).idle()

        val current =
            SettingsRepository.create(activity).current()

        assertTrue(current.aiPrivacyConsent)
        assertFalse(current.preserveDraft)
    }

    @Test
    fun settings_screen_requests_no_media_or_storage_permissions() {

        val context =
            ApplicationProvider.getApplicationContext<android.content.Context>()

        val permissions =
            context.packageManager
                .getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_PERMISSIONS
                )
                .requestedPermissions
                ?.toSet()
                .orEmpty()

        assertFalse(
            "android.permission.READ_MEDIA_IMAGES" in permissions
        )

        assertFalse(
            "android.permission.READ_EXTERNAL_STORAGE" in permissions
        )

        assertFalse(
            "android.permission.WRITE_EXTERNAL_STORAGE" in permissions
        )
    }
}