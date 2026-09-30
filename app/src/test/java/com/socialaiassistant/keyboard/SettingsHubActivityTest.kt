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
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertNotNull(activity.findViewById<LinearLayout>(R.id.settings_theme_dashboard_container))
    }

    @Test
    fun ai_privacy_deep_link_opens_settings_category_activity() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_SECTION, MainActivity.SECTION_AI_PRIVACY)
        val activity = Robolectric.buildActivity(MainActivity::class.java, intent).setup().get()
        shadowOf(Looper.getMainLooper()).idle()
        val started = shadowOf(activity).nextStartedActivity
        assertEquals(SettingsCategoryActivity::class.java.name, started.component?.className)
    }

    @Test
    fun ai_privacy_and_preserve_draft_persist() = runBlocking {
        val repository = SettingsRepository.create(ApplicationProvider.getApplicationContext())
        repository.setAiPrivacyConsent(true)
        repository.setPreserveDraft(false)
        val current = repository.current()
        assertTrue(current.aiPrivacyConsent)
        assertFalse(current.preserveDraft)
    }

    @Test
    fun premium_dashboard_ai_mode_toggles_persist() = runBlocking {
        val repository = SettingsRepository.create(ApplicationProvider.getApplicationContext())
        repository.setSmartReplyEnabled(false)
        repository.setUniqueReplyEnabled(true)
        repository.setFlirtyReplyEnabled(false)
        val current = repository.current()
        assertFalse(current.smartReplyEnabled)
        assertTrue(current.uniqueReplyEnabled)
        assertFalse(current.flirtyReplyEnabled)
    }

    @Test
    fun settings_screen_requests_no_media_or_storage_permissions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = context.packageManager
            .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions?.toSet().orEmpty()
        assertFalse("android.permission.READ_MEDIA_IMAGES" in permissions)
        assertFalse("android.permission.READ_EXTERNAL_STORAGE" in permissions)
        assertFalse("android.permission.WRITE_EXTERNAL_STORAGE" in permissions)
    }
}
