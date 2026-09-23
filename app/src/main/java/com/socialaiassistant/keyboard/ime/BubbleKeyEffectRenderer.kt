package com.socialaiassistant.keyboard.ime

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.socialaiassistant.keyboard.theme.KeyboardTheme
import com.socialaiassistant.keyboard.theme.ThemeFillStyle
import java.util.ArrayDeque
import kotlin.math.roundToInt

/** Lightweight, bounded visual-only renderer. It never participates in text commit logic. */
class BubbleKeyEffectRenderer(
    private val overlay: FrameLayout
) {
    private val idleBubbles = ArrayDeque<TextView>()
    private val activeBubbles = ArrayDeque<TextView>()
    private var sequence = 0

    fun showLocal(sourceScreen: BubbleFlightPoint, spec: BubbleKeyAnimationSpec, theme: KeyboardTheme) {
        if (overlay.width <= 0 || overlay.height <= 0 || !sourceScreen.isFinite()) return
        if (activeBubbles.size >= BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES) {
            recycleImmediately(activeBubbles.removeFirst())
        }

        val bubble = obtainBubble()
        val sizePx = dp(spec.bubbleSizeDp).roundToInt().coerceAtLeast(dp(24f).roundToInt())
        val overlayLocation = IntArray(2)
        overlay.getLocationOnScreen(overlayLocation)
        val startX = sourceScreen.x - overlayLocation[0] - sizePx / 2f
        val startY = sourceScreen.y - overlayLocation[1] - sizePx / 2f

        bubble.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        bubble.text = spec.label
        bubble.setTextColor(theme.keyLabel)
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14f * theme.keyLabelScale).coerceIn(12f, 19f))
        bubble.background = bubbleDrawable(theme)
        bubble.x = startX
        bubble.y = startY
        bubble.translationX = 0f
        bubble.translationY = 0f
        bubble.alpha = 0.96f
        bubble.scaleX = 0.82f
        bubble.scaleY = 0.82f
        bubble.visibility = View.VISIBLE

        activeBubbles.addLast(bubble)
        val direction = if ((sequence++ and 1) == 0) -1f else 1f
        bubble.animate()
            .translationY(-dp(spec.riseDp))
            .translationX(direction * dp(spec.driftDp))
            .alpha(0f)
            .scaleX(1.12f)
            .scaleY(1.12f)
            .setDuration(spec.durationMs)
            .withEndAction { recycleIfActive(bubble) }
            .start()
    }

    fun release() {
        val active = activeBubbles.toList()
        activeBubbles.clear()
        active.forEach(::recycleImmediately)
        idleBubbles.forEach { it.animate().cancel() }
        idleBubbles.clear()
        overlay.removeAllViews()
    }

    private fun obtainBubble(): TextView {
        val bubble = if (idleBubbles.isEmpty()) {
            TextView(overlay.context).apply {
                gravity = Gravity.CENTER
                includeFontPadding = false
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                typeface = Typeface.DEFAULT_BOLD
                elevation = dp(8f)
            }
        } else {
            idleBubbles.removeFirst()
        }
        if (bubble.parent == null) overlay.addView(bubble)
        return bubble
    }

    private fun recycleIfActive(bubble: TextView) {
        if (!activeBubbles.remove(bubble)) return
        recycleImmediately(bubble)
    }

    private fun recycleImmediately(bubble: TextView) {
        bubble.animate().cancel()
        if (bubble.parent === overlay) overlay.removeView(bubble)
        bubble.text = ""
        bubble.alpha = 1f
        bubble.translationX = 0f
        bubble.translationY = 0f
        bubble.scaleX = 1f
        bubble.scaleY = 1f
        bubble.visibility = View.INVISIBLE
        if (idleBubbles.size < BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES) idleBubbles.addLast(bubble)
    }

    private fun bubbleDrawable(theme: KeyboardTheme): GradientDrawable {
        val colors = if (theme.fillStyle == ThemeFillStyle.GRADIENT) {
            intArrayOf(withAlpha(theme.keyGradientStart, 88), withAlpha(theme.keyGradientEnd, 88))
        } else {
            null
        }
        return (if (colors == null) GradientDrawable() else GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)).apply {
            shape = GradientDrawable.OVAL
            if (colors == null) setColor(withAlpha(theme.keySurface, theme.glassOpacity.coerceAtLeast(72)))
            setStroke(dp(1f).roundToInt().coerceAtLeast(1), withAlpha(theme.primaryNeon, 78))
        }
    }

    private fun withAlpha(color: Int, percent: Int): Int {
        val alpha = (255f * percent.coerceIn(0, 100) / 100f).toInt()
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun dp(value: Float): Float = value * overlay.resources.displayMetrics.density
}
