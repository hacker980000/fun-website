package com.socialaiassistant.keyboard.context

import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import android.accessibilityservice.AccessibilityService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Robolectric

class AccessibilityBubbleOverlayWindowHostTest {
    @Test fun usesAccessibilityOverlayAndNeverTakesInput() {
        val service = Robolectric.buildService(TestAccessibilityService::class.java).create().get()
        val params = AccessibilityBubbleOverlayWindowHost(service).buildLayoutParams()
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0)
        assertEquals(PixelFormat.TRANSLUCENT, params.format)
    }

    class TestAccessibilityService : AccessibilityService() {
        override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit
        override fun onInterrupt() = Unit
    }
}
