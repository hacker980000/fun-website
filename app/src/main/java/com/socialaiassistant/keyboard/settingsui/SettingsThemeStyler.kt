package com.socialaiassistant.keyboard.settingsui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class SettingsThemeStyler(private val context: Context) {
    fun apply(
        root: View,
        sections: List<LinearLayout>,
        headerTitle: TextView,
        headerEyebrow: TextView,
        headerSubtitle: TextView,
        status: TextView,
        pack: SettingsThemePack
    ) {
        val spec = SettingsThemeCatalog.spec(pack)
        root.background = gradient(spec.rootTop, spec.rootBottom, 0f)

        headerTitle.setTextColor(spec.textPrimary)
        headerEyebrow.setTextColor(spec.accent2)
        headerSubtitle.setTextColor(spec.textSecondary)
        status.setTextColor(spec.textPrimary)
        status.background = shape(spec.panelAlt, spec.border, 1, spec.cornerDp)

        sections.forEach { section ->
            section.background = shape(spec.panel, withAlpha(spec.border, 150), 1, spec.cornerDp)
            styleTree(section, spec)
        }
    }

    private fun styleTree(view: View, spec: SettingsThemeSpec) {
        when (view) {
            is CheckBox -> {
                view.setTextColor(spec.textPrimary)
                view.buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(spec.accent, withAlpha(spec.textSecondary, 190))
                )
            }

            is EditText -> {
                view.setTextColor(spec.textPrimary)
                view.setHintTextColor(withAlpha(spec.textSecondary, 210))
                view.background = shape(spec.row, withAlpha(spec.border, 120), 1, 13f)
            }

            is Button -> {
                view.isAllCaps = false
                view.setTextColor(spec.textPrimary)
                view.background = gradient(spec.accent, spec.accent2, 14f, withAlpha(Color.WHITE, 90))
                view.stateListAnimator = null
            }

            is TextView -> {
                if ((view.textSize / context.resources.displayMetrics.scaledDensity) >= 17f) {
                    view.setTextColor(spec.textPrimary)
                } else {
                    view.setTextColor(spec.textSecondary)
                }
                if (view.background != null) {
                    view.background = shape(spec.row, withAlpha(spec.border, 90), 1, 12f)
                }
            }
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                styleTree(view.getChildAt(index), spec)
            }
        }
    }

    fun applyCategory(
        root: View,
        header: ViewGroup,
        title: TextView,
        subtitle: TextView,
        content: ViewGroup,
        pack: SettingsThemePack
    ) {
        val spec = SettingsThemeCatalog.spec(pack)
        root.background = gradient(spec.rootTop, spec.rootBottom, 0f)
        header.background = shape(spec.panelAlt, withAlpha(spec.border, 150), 1, spec.cornerDp)
        content.background = shape(spec.panel, withAlpha(spec.border, 120), 1, spec.cornerDp)
        title.setTextColor(spec.textPrimary)
        subtitle.setTextColor(spec.textSecondary)
        styleTree(content, spec)
    }

    fun dashboardCard(spec: SettingsThemeSpec, accent: Int? = null, stronger: Boolean = false): GradientDrawable {
        val fill = if (stronger) spec.panelAlt else spec.row
        return shape(fill, withAlpha(accent ?: spec.border, 185), 1, spec.cornerDp)
    }

    fun accentTile(color: Int, radiusDp: Float = 11f): GradientDrawable =
        shape(withAlpha(color, 230), withAlpha(Color.WHITE, 72), 1, radiusDp)

    fun buttonBackground(spec: SettingsThemeSpec): GradientDrawable =
        gradient(spec.accent, spec.accent2, 14f, withAlpha(Color.WHITE, 80))

    private fun shape(fill: Int, stroke: Int, strokeDp: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(radiusDp)
            if (strokeDp > 0) setStroke(dp(strokeDp.toFloat()).toInt(), stroke)
        }

    private fun gradient(start: Int, end: Int, radiusDp: Float, stroke: Int? = null): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp)
            stroke?.let { setStroke(dp(1f).toInt(), it) }
        }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density
}
