package com.socialaiassistant.keyboard.context

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.socialaiassistant.keyboard.ime.BubbleFlightEditorToken
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint
import com.socialaiassistant.keyboard.ime.BubbleFlightRequest
import com.socialaiassistant.keyboard.ime.BubbleFlightTarget
import com.socialaiassistant.keyboard.ime.BubbleKeyPolicy
import com.socialaiassistant.keyboard.theme.KeyboardTheme
import com.socialaiassistant.keyboard.theme.ThemeFillStyle
import java.util.ArrayDeque
import java.util.LinkedHashMap
import kotlin.math.roundToInt

class BubbleFlightOverlayRenderer(
    private val host: BubbleOverlayHost
) {
    private val activeFlights = LinkedHashMap<Long, ActiveFlight>()
    private val idleBubbles = ArrayDeque<TextView>()

    fun show(request: BubbleFlightRequest, target: BubbleFlightTarget): Boolean {
        if (!request.source.isFinite() || !target.point.isFinite() || target.editor != request.editor) return false
        if (!host.ensureAttached()) return false

        if (activeFlights.size >= BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES) {
            activeFlights.entries.firstOrNull()?.key?.let(::finishFlightImmediately)
        }

        val bubble = obtainBubble()
        val sizePx = dp(request.spec.bubbleSizeDp).roundToInt().coerceAtLeast(dp(24f).roundToInt())
        bubble.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        styleBubble(bubble, request)
        setCenter(bubble, request.source, sizePx)
        bubble.alpha = START_ALPHA
        bubble.scaleX = START_SCALE
        bubble.scaleY = START_SCALE
        bubble.visibility = View.VISIBLE

        val active = ActiveFlight(
            request = request,
            target = target,
            view = bubble,
            sizePx = sizePx,
            segmentSource = request.source,
            segmentStartScale = START_SCALE,
            totalDurationMs = request.spec.durationMs.coerceAtLeast(1L),
            startedAtUptimeMs = android.os.SystemClock.uptimeMillis()
        )
        activeFlights[request.id] = active
        startSegment(active, active.totalDurationMs)
        return true
    }

    fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean {
        val active = activeFlights[flightId] ?: return false
        if (active.retargeted || target.editor != active.request.editor || !target.point.isFinite()) return false
        val elapsed = (android.os.SystemClock.uptimeMillis() - active.startedAtUptimeMs).coerceAtLeast(0L)
        val elapsedFraction = (elapsed.toFloat() / active.totalDurationMs.toFloat()).coerceIn(0f, 1f)
        if (elapsedFraction > RETARGET_MAX_FRACTION) return false

        val currentCenter = BubbleFlightPoint(
            active.view.x + active.sizePx / 2f,
            active.view.y + active.sizePx / 2f
        )
        val currentScale = active.view.scaleX
        active.animator?.removeAllListeners()
        active.animator?.cancel()
        active.animator = null
        active.retargeted = true
        active.target = target
        active.segmentSource = currentCenter
        active.segmentStartScale = currentScale
        val remaining = (active.totalDurationMs * (1f - elapsedFraction)).toLong().coerceAtLeast(MIN_RETARGET_DURATION_MS)
        startSegment(active, remaining)
        return true
    }

    fun cancelEditor(editor: BubbleFlightEditorToken) {
        activeFlights.values
            .filter { it.request.editor == editor }
            .map { it.request.id }
            .forEach(::finishFlightImmediately)
    }

    fun cancelAll() {
        activeFlights.keys.toList().forEach(::finishFlightImmediately)
        detachIfIdle()
    }

    fun release() {
        activeFlights.values.forEach { active ->
            active.animator?.removeAllListeners()
            active.animator?.cancel()
        }
        activeFlights.clear()
        idleBubbles.forEach { it.animate().cancel() }
        idleBubbles.clear()
        host.root.removeAllViews()
        host.detach()
    }

    internal fun activeFlightCountForTest(): Int = activeFlights.size

    private fun startSegment(active: ActiveFlight, durationMs: Long) {
        val curvePx = dp(active.request.spec.flightCurveDp)
        val targetPoint = active.target.point
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { valueAnimator ->
                if (activeFlights[active.request.id] !== active) return@addUpdateListener
                val fraction = valueAnimator.animatedValue as Float
                val point = BubbleFlightPath.pointAt(active.segmentSource, targetPoint, curvePx, fraction)
                setCenter(active.view, point, active.sizePx)
                val alpha = if (fraction <= FADE_START_FRACTION) {
                    START_ALPHA
                } else {
                    START_ALPHA * ((1f - fraction) / (1f - FADE_START_FRACTION)).coerceIn(0f, 1f)
                }
                active.view.alpha = alpha
                val scale = lerp(active.segmentStartScale, active.request.spec.flightEndScale, fraction)
                active.view.scaleX = scale
                active.view.scaleY = scale
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishFlightIfActive(active.request.id, active)
                }
            })
        }
        active.animator = animator
        animator.start()
    }

    private fun finishFlightIfActive(id: Long, expected: ActiveFlight) {
        val current = activeFlights[id] ?: return
        if (current !== expected) return
        activeFlights.remove(id)
        current.animator = null
        recycleView(current.view)
        detachIfIdle()
    }

    private fun finishFlightImmediately(id: Long) {
        val active = activeFlights.remove(id) ?: return
        active.animator?.removeAllListeners()
        active.animator?.cancel()
        active.animator = null
        recycleView(active.view)
        detachIfIdle()
    }

    private fun detachIfIdle() {
        if (activeFlights.isEmpty()) host.detach()
    }

    private fun obtainBubble(): TextView {
        val bubble = if (idleBubbles.isEmpty()) {
            TextView(host.root.context).apply {
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
        if (bubble.parent == null) host.root.addView(bubble)
        return bubble
    }

    private fun recycleView(view: TextView) {
        view.animate().cancel()
        if (view.parent === host.root) host.root.removeView(view)
        view.text = ""
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.visibility = View.INVISIBLE
        if (idleBubbles.size < BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES) idleBubbles.addLast(view)
    }

    private fun styleBubble(view: TextView, request: BubbleFlightRequest) {
        val theme = request.theme
        view.text = request.label
        view.setTextColor(theme.keyLabel)
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14f * theme.keyLabelScale).coerceIn(12f, 19f))
        view.background = bubbleDrawable(theme)
    }

    private fun bubbleDrawable(theme: KeyboardTheme): GradientDrawable {
        val colors = if (theme.fillStyle == ThemeFillStyle.GRADIENT) {
            intArrayOf(withAlpha(theme.keyGradientStart, 88), withAlpha(theme.keyGradientEnd, 88))
        } else null
        return (if (colors == null) GradientDrawable() else GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)).apply {
            shape = GradientDrawable.OVAL
            if (colors == null) setColor(withAlpha(theme.keySurface, theme.glassOpacity.coerceAtLeast(72)))
            setStroke(dp(1f).roundToInt().coerceAtLeast(1), withAlpha(theme.primaryNeon, 78))
        }
    }

    private fun setCenter(view: View, point: BubbleFlightPoint, sizePx: Int) {
        view.x = point.x - sizePx / 2f
        view.y = point.y - sizePx / 2f
    }

    private fun withAlpha(color: Int, percent: Int): Int {
        val alpha = (255f * percent.coerceIn(0, 100) / 100f).toInt()
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun dp(value: Float): Float = value * host.root.resources.displayMetrics.density
    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t.coerceIn(0f, 1f)

    private data class ActiveFlight(
        val request: BubbleFlightRequest,
        var target: BubbleFlightTarget,
        val view: TextView,
        val sizePx: Int,
        var segmentSource: BubbleFlightPoint,
        var segmentStartScale: Float,
        val totalDurationMs: Long,
        val startedAtUptimeMs: Long,
        var animator: ValueAnimator? = null,
        var retargeted: Boolean = false
    )

    private companion object {
        const val START_ALPHA = 0.96f
        const val START_SCALE = 0.82f
        const val FADE_START_FRACTION = 0.72f
        const val RETARGET_MAX_FRACTION = 0.35f
        const val MIN_RETARGET_DURATION_MS = 80L
    }
}
