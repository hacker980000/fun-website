package com.socialaiassistant.keyboard.theme

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import java.util.concurrent.ConcurrentHashMap

class NeonDrawableFactory(
    private val context: Context
) {
    private data class CacheKey(
        val signature: Int,
        val role: ThemeButtonRole,
        val selected: Boolean
    )

    private val cache = ConcurrentHashMap<CacheKey, Drawable.ConstantState>()

    fun button(theme: KeyboardTheme, role: ThemeButtonRole, selected: Boolean = false): Drawable {
        val normalized = theme.normalized()
        val key = CacheKey(normalized.visualSignature(), role, selected)
        cache[key]?.newDrawable(context.resources)?.mutate()?.let { return it }

        val state = StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), layered(normalized, role, VisualState.DISABLED, selected))
            addState(intArrayOf(android.R.attr.state_pressed), layered(normalized, role, VisualState.PRESSED, selected))
            addState(intArrayOf(android.R.attr.state_selected), layered(normalized, role, VisualState.SELECTED, true))
            addState(intArrayOf(), layered(normalized, role, VisualState.NORMAL, selected))
        }
        state.constantState?.let { cache[key] = it }
        return state
    }

    fun panel(theme: KeyboardTheme, accent: Int = theme.primaryNeon): Drawable {
        val t = theme.normalized()
        return rounded(
            fill = withAlpha(t.panelSurface, t.glassOpacity),
            stroke = withAlpha(accent, (t.borderOpacity * 0.55f).toInt()),
            strokeDp = 1,
            radiusDp = t.panelCornerRadiusDp,
            gradient = panelGradientFor(t)
        )
    }

    fun toolbar(theme: KeyboardTheme): Drawable {
        val t = theme.normalized()
        return rounded(
            fill = withAlpha(t.toolbarSurface, t.glassOpacity),
            stroke = withAlpha(t.primaryNeon, (t.borderOpacity * 0.35f).toInt()),
            strokeDp = 1,
            radiusDp = (t.panelCornerRadiusDp - 2f).coerceAtLeast(6f),
            gradient = panelGradientFor(t)
        )
    }

    private fun layered(
        theme: KeyboardTheme,
        role: ThemeButtonRole,
        state: VisualState,
        selected: Boolean
    ): Drawable {
        val accent = accentFor(theme, role)
        val surface = surfaceFor(theme, role)
        val glowBase = when (state) {
            VisualState.PRESSED -> theme.pressedGlowStrength
            VisualState.SELECTED -> (theme.pressedGlowStrength + 4).coerceAtMost(100)
            VisualState.DISABLED -> 10
            VisualState.NORMAL -> if (selected) theme.pressedGlowStrength else theme.glowStrength
        }
        val borderAlpha = when (state) {
            VisualState.DISABLED -> 20
            VisualState.PRESSED, VisualState.SELECTED -> (theme.borderOpacity + 18).coerceAtMost(100)
            VisualState.NORMAL -> theme.borderOpacity
        }
        val fill = when (state) {
            VisualState.DISABLED -> withAlpha(theme.disabledSurface, 92)
            VisualState.PRESSED -> blend(surface, accent, 0.24f)
            VisualState.SELECTED -> blend(surface, accent, 0.18f)
            VisualState.NORMAL -> withAlpha(surface, theme.glassOpacity)
        }
        val outer = rounded(
            fill = withAlpha(surface, 0),
            stroke = withAlpha(accent, (glowBase * 0.50f).toInt()),
            strokeDp = 2,
            radiusDp = theme.keyCornerRadiusDp + 1.5f
        )
        val inner = rounded(
            fill = fill,
            stroke = withAlpha(accent, borderAlpha),
            strokeDp = if (state == VisualState.PRESSED || state == VisualState.SELECTED) 2 else 1,
            radiusDp = theme.keyCornerRadiusDp,
            gradient = if (state == VisualState.NORMAL) gradientFor(theme, role) else null
        )
        val inset = dp(1)
        return LayerDrawable(arrayOf(outer, inner)).apply {
            setLayerInset(1, inset, inset, inset, inset)
        }
    }

    private fun rounded(
        fill: Int,
        stroke: Int,
        strokeDp: Int,
        radiusDp: Float,
        gradient: IntArray? = null
    ): GradientDrawable {
        val drawable = if (gradient == null) {
            GradientDrawable()
        } else {
            GradientDrawable(GradientDrawable.Orientation.TL_BR, gradient)
        }
        return drawable.apply {
            shape = GradientDrawable.RECTANGLE
            if (gradient == null) setColor(fill)
            setStroke(dp(strokeDp), stroke)
            cornerRadius = dp(radiusDp)
        }
    }

    private fun gradientFor(theme: KeyboardTheme, role: ThemeButtonRole): IntArray? {
        if (theme.fillStyle != ThemeFillStyle.GRADIENT) return null
        return when (role) {
            ThemeButtonRole.NORMAL -> intArrayOf(theme.keyGradientStart, theme.keyGradientEnd)
            else -> intArrayOf(theme.specialGradientStart, theme.specialGradientEnd)
        }
    }

    private fun panelGradientFor(theme: KeyboardTheme): IntArray? =
        if (theme.fillStyle == ThemeFillStyle.GRADIENT) {
            intArrayOf(theme.panelGradientStart, theme.panelGradientEnd)
        } else {
            null
        }

    private fun surfaceFor(theme: KeyboardTheme, role: ThemeButtonRole): Int = when (role) {
        ThemeButtonRole.NORMAL -> theme.keySurface
        ThemeButtonRole.SPECIAL, ThemeButtonRole.AI, ThemeButtonRole.AI_ACTION,
        ThemeButtonRole.SECONDARY_ACTION, ThemeButtonRole.ENTER -> theme.specialKeySurface
    }

    private fun accentFor(theme: KeyboardTheme, role: ThemeButtonRole): Int = when (role) {
        ThemeButtonRole.NORMAL -> theme.primaryNeon
        ThemeButtonRole.SPECIAL -> theme.secondaryNeon
        ThemeButtonRole.ENTER -> theme.actionAccent
        ThemeButtonRole.AI, ThemeButtonRole.AI_ACTION -> theme.aiNeon
        ThemeButtonRole.SECONDARY_ACTION -> theme.primaryNeon
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density

    private fun withAlpha(color: Int, percent: Int): Int {
        val alpha = (255f * percent.coerceIn(0, 100) / 100f).toInt()
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun blend(base: Int, accent: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val b = base ushr shift and 0xFF
            val c = accent ushr shift and 0xFF
            return (b + (c - b) * a).toInt().coerceIn(0, 255)
        }
        val alpha = base ushr 24 and 0xFF
        return (alpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private enum class VisualState { NORMAL, PRESSED, SELECTED, DISABLED }
}
