package com.socialaiassistant.keyboard.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.socialaiassistant.keyboard.R

enum class ThemeButtonRole {
    NORMAL,
    SPECIAL,
    ENTER,
    AI,
    AI_ACTION,
    SECONDARY_ACTION
}

class ThemeRenderer(
    private val context: Context,
    private val drawables: NeonDrawableFactory = NeonDrawableFactory(context)
) {
    fun applyRoot(root: View, theme: KeyboardTheme) {
        applyGlobalChrome(root, theme)
        applyKeyPanelSurface(root, theme)
    }

    fun applyGlobalChrome(root: View, theme: KeyboardTheme) {
        val t = theme.normalized()
        root.setBackgroundColor(t.rootBackground)
        root.findViewById<View>(R.id.keyboard_toolbar)?.background = drawables.toolbar(t)
        root.findViewById<View>(R.id.keyboard_panel_container)?.let { panel ->
            panel.background = drawables.panel(t, t.aiNeon)
            panel.setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        root.findViewById<View>(R.id.suggestion_bar)?.background = drawables.panel(t, t.primaryNeon)
        styleSmartReply(root, t)
    }

    fun applyKeyPanelSurface(root: View, theme: KeyboardTheme) {
        val t = theme.normalized()
        root.findViewById<View>(R.id.keys_wrapper)?.background = drawables.panel(t, t.primaryNeon)
    }

    fun styleButton(
        button: Button,
        role: ThemeButtonRole,
        theme: KeyboardTheme,
        selected: Boolean = false
    ) {
        val t = theme.normalized()
        button.isAllCaps = false
        button.isSelected = selected
        button.background = drawables.button(t, role, selected)
        button.setTextColor(if (button.isEnabled) labelColor(t, role) else t.disabledLabel)
        button.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            when (role) {
                ThemeButtonRole.AI_ACTION -> 12.5f * t.aiTitleScale
                ThemeButtonRole.AI, ThemeButtonRole.SECONDARY_ACTION -> 13f * t.toolbarLabelScale
                else -> 15f * t.keyLabelScale
            }
        )
        button.typeface = when (role) {
            ThemeButtonRole.ENTER, ThemeButtonRole.AI, ThemeButtonRole.AI_ACTION -> Typeface.DEFAULT_BOLD
            else -> Typeface.DEFAULT
        }
        button.stateListAnimator = null
        button.elevation = 0f
    }

    fun styleBoundarylessAlphabeticKey(button: Button, theme: KeyboardTheme) {
        val t = theme.normalized()
        button.isAllCaps = false
        button.background = null
        button.setTextColor(safeContrast(t.keyLabel, t.panelSurface))
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f * t.keyLabelScale)
        button.typeface = Typeface.DEFAULT
        button.stateListAnimator = null
        button.elevation = 0f
    }

    fun styleText(textView: TextView, theme: KeyboardTheme, secondary: Boolean = false) {
        val t = theme.normalized()
        textView.setTextColor(if (secondary) t.textSecondary else t.textPrimary)
    }

    fun styleSmartReply(root: View, theme: KeyboardTheme) {
        val container = root.findViewById<View>(R.id.smart_reply_container) ?: return
        container.background = drawables.panel(theme, theme.primaryNeon)
        root.findViewById<TextView>(R.id.smart_reply_text)?.let { styleText(it, theme) }
        root.findViewById<Button>(R.id.smart_reply_insert_anyway)?.let {
            styleButton(it, ThemeButtonRole.SECONDARY_ACTION, theme)
        }
        root.findViewById<Button>(R.id.smart_reply_replace)?.let {
            styleButton(it, ThemeButtonRole.ENTER, theme)
        }
    }

    fun styleToolbar(root: View, theme: KeyboardTheme, aiSelected: Boolean, aiEnabled: Boolean) {
        root.findViewById<Button>(R.id.toolbar_ai)?.let {
            it.isEnabled = aiEnabled
            styleButton(it, ThemeButtonRole.AI, theme, selected = aiSelected)
        }
        listOf(
            R.id.toolbar_language,
            R.id.toolbar_bangla_mode,
            R.id.toolbar_emoji,
            R.id.toolbar_clipboard,
            R.id.toolbar_voice,
            R.id.toolbar_settings
        ).forEach { id ->
            root.findViewById<Button>(id)?.let { styleButton(it, ThemeButtonRole.SPECIAL, theme) }
        }
    }

    fun styleTree(root: View, theme: KeyboardTheme) {
        if (root is TextView && root !is Button) styleText(root, theme)
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) styleTree(root.getChildAt(index), theme)
        }
    }

    fun applyBackground(root: View, config: BackgroundPhotoConfig, bitmap: Bitmap?) {
        val full = root.findViewById<ImageView>(R.id.full_keyboard_background)
        val keys = root.findViewById<ImageView>(R.id.keys_background)
        val ai = root.findViewById<ImageView>(R.id.ai_panel_background)
        listOfNotNull(full, keys, ai).forEach { view ->
            view.visibility = View.GONE
            view.setImageDrawable(null)
            view.clearColorFilter()
            view.alpha = 1f
        }
        val normalized = config.normalized()
        if (!normalized.enabled || bitmap == null) return
        val target = when (normalized.scope) {
            BackgroundScope.FULL_KEYBOARD -> full
            BackgroundScope.KEYS_ONLY -> keys
            BackgroundScope.AI_PANEL_ONLY -> ai
        } ?: return
        target.scaleType = when (normalized.fit) {
            BackgroundFit.FILL -> ImageView.ScaleType.FIT_XY
            BackgroundFit.FIT -> ImageView.ScaleType.FIT_CENTER
            BackgroundFit.CENTER_CROP -> ImageView.ScaleType.CENTER_CROP
        }
        target.setImageBitmap(bitmap)
        target.alpha = normalized.opacityPercent / 100f
        if (normalized.darkOverlayPercent > 0) {
            val alpha = (255f * normalized.darkOverlayPercent / 100f).toInt()
            target.setColorFilter(Color.argb(alpha, 0, 0, 0), PorterDuff.Mode.SRC_OVER)
        }
        target.visibility = View.VISIBLE
    }

    fun panelBackground(theme: KeyboardTheme, accent: Int = theme.primaryNeon) = drawables.panel(theme, accent)

    private fun labelColor(theme: KeyboardTheme, role: ThemeButtonRole): Int {
        val requested = if (role == ThemeButtonRole.NORMAL) theme.keyLabel else theme.specialKeyLabel
        val surface = when (role) {
            ThemeButtonRole.NORMAL -> theme.keySurface
            else -> theme.specialKeySurface
        }
        return safeContrast(requested, surface)
    }

    private fun safeContrast(requested: Int, surface: Int): Int {
        val difference = kotlin.math.abs(luma(requested) - luma(surface))
        if (difference >= 90) return requested
        return if (luma(surface) < 140) Color.WHITE else Color.BLACK
    }

    private fun luma(color: Int): Int =
        (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
