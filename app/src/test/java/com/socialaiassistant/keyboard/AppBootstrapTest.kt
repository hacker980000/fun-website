package com.socialaiassistant.keyboard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppBootstrapTest {
    @Test
    fun app_label_is_social_ai_keyboard() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val label = app.applicationInfo.loadLabel(app.packageManager).toString()
        assertEquals("Social AI Keyboard", label)
    }
}
