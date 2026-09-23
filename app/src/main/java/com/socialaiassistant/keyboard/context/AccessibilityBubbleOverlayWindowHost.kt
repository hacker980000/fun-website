package com.socialaiassistant.keyboard.context

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

interface BubbleOverlayHost {
    val root: FrameLayout
    fun ensureAttached(): Boolean
    fun detach()
}

class AccessibilityBubbleOverlayWindowHost(
    private val service: AccessibilityService
) : BubbleOverlayHost {
    override val root: FrameLayout = FrameLayout(service).apply {
        clipChildren = false
        clipToPadding = false
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val windowManager: WindowManager? by lazy {
        service.getSystemService(WindowManager::class.java)
    }

    fun buildLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    override fun ensureAttached(): Boolean {
        if (root.parent != null) return true
        val manager = windowManager ?: return false
        return try {
            manager.addView(root, buildLayoutParams())
            true
        } catch (_: SecurityException) {
            false
        } catch (_: WindowManager.BadTokenException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    override fun detach() {
        if (root.parent == null) return
        val manager = windowManager ?: return
        runCatching { manager.removeViewImmediate(root) }
    }
}
