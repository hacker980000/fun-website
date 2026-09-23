package com.socialaiassistant.keyboard.settingsui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

class SettingsThemeDashboardRenderer(private val context: Context) {
    private data class Item(
        val category: SettingsCategoryId,
        val icon: String,
        val title: String,
        val detail: String
    )

    private val styler = SettingsThemeStyler(context)

    fun render(
        container: LinearLayout,
        pack: SettingsThemePack,
        onCategory: (SettingsCategoryId) -> Unit
    ) {
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        val spec = SettingsThemeCatalog.spec(pack)

        when (pack) {
            SettingsThemePack.CLEAN_MODERN -> renderCleanModern(container, spec, onCategory)
            SettingsThemePack.CARD_STYLE -> renderCardStyle(container, spec, onCategory)
            SettingsThemePack.PREMIUM -> renderPremium(container, spec, onCategory)
            SettingsThemePack.PRO_STYLE -> renderProStyle(container, spec, onCategory)
        }
    }

    private fun canonicalItems(): List<Item> = listOf(
        Item(SettingsCategoryId.KEYBOARD_SETUP, "⌨", "Keyboard Setup", "Enable and choose the keyboard"),
        Item(SettingsCategoryId.LANGUAGE_INPUT, "◎", "Language & Input", "English, বাংলা, Phonetic, Bijoy"),
        Item(SettingsCategoryId.THEME_APPEARANCE, "✦", "Theme & Appearance", "Keyboard and settings visuals"),
        Item(SettingsCategoryId.TYPING_SUGGESTIONS, "⌁", "Typing & Suggestions", "Learning, cursor, glide, feedback"),
        Item(SettingsCategoryId.AI_PRIVACY, "⌘", "AI & Privacy", "Consent, context, prompts, training"),
        Item(SettingsCategoryId.CLIPBOARD, "▣", "Clipboard", "Recent history controls"),
        Item(SettingsCategoryId.ACCOUNT_SUBSCRIPTION, "●", "Account & Subscription", "Products, usage, gateway"),
        Item(SettingsCategoryId.HELP_ABOUT, "?", "Help & About", "Support and app information")
    )

    private fun renderCleanModern(
        container: LinearLayout,
        spec: SettingsThemeSpec,
        onCategory: (SettingsCategoryId) -> Unit
    ) {
        container.addView(hero("⚙", "Settings Menu", "Your Keyboard\nYour Way", spec))
        canonicalItems().forEachIndexed { index, item ->
            container.addView(listRow(item, spec, spec.categoryAccents[index % spec.categoryAccents.size], onCategory))
        }
    }

    private fun renderCardStyle(
        container: LinearLayout,
        spec: SettingsThemeSpec,
        onCategory: (SettingsCategoryId) -> Unit
    ) {
        val grid = GridLayout(context).apply {
            columnCount = 2
            rowCount = 4
            useDefaultMargins = false
        }
        canonicalItems().forEachIndexed { index, item ->
            val accent = spec.categoryAccents[index % spec.categoryAccents.size]
            val card = categoryCard(item, spec, accent, onCategory)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(88)
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            grid.addView(card, params)
        }
        container.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun renderPremium(
        container: LinearLayout,
        spec: SettingsThemeSpec,
        onCategory: (SettingsCategoryId) -> Unit
    ) {
        canonicalItems().forEachIndexed { index, item ->
            container.addView(
                listRow(
                    item,
                    spec,
                    spec.categoryAccents[index % spec.categoryAccents.size],
                    onCategory,
                    premium = true
                )
            )
        }
    }

    private fun renderProStyle(
        container: LinearLayout,
        spec: SettingsThemeSpec,
        onCategory: (SettingsCategoryId) -> Unit
    ) {
        canonicalItems().forEachIndexed { index, item ->
            container.addView(
                listRow(
                    item,
                    spec,
                    spec.categoryAccents[index % spec.categoryAccents.size],
                    onCategory,
                    premium = true
                )
            )
        }
        val quote = TextView(context).apply {
            text = "“Personalize\nfor a Better\nYou”"
            setTextColor(spec.textPrimary)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = styler.dashboardCard(spec, spec.accent2, stronger = true)
        }
        container.addView(
            quote,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(8), 0, 0)
            }
        )
    }

    private fun hero(icon: String, title: String, subtitle: String, spec: SettingsThemeSpec): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = styler.dashboardCard(spec, spec.accent, stronger = true)
        }
        val iconView = TextView(context).apply {
            text = icon
            gravity = Gravity.CENTER
            textSize = 28f
            setTextColor(spec.textPrimary)
            background = styler.accentTile(spec.accent2, 14f)
        }
        row.addView(iconView, LinearLayout.LayoutParams(dp(62), dp(62)))

        val copy = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        copy.addView(TextView(context).apply {
            text = title
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(spec.textPrimary)
        })
        copy.addView(TextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(spec.textSecondary)
        })
        row.addView(copy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun listRow(
        item: Item,
        spec: SettingsThemeSpec,
        accent: Int,
        onCategory: (SettingsCategoryId) -> Unit,
        premium: Boolean = false
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(10), dp(9), dp(12), dp(9))
            background = styler.dashboardCard(spec, accent, stronger = premium)
            setOnClickListener { onCategory(item.category) }
        }
        val icon = TextView(context).apply {
            text = item.icon
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(spec.textPrimary)
            background = styler.accentTile(accent)
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(38), dp(38)))

        val copy = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(6), 0)
        }
        copy.addView(TextView(context).apply {
            text = item.title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(spec.textPrimary)
        })
        copy.addView(TextView(context).apply {
            text = item.detail
            textSize = 10.5f
            setTextColor(spec.textSecondary)
            maxLines = 1
        })
        row.addView(copy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(context).apply {
            text = "›"
            textSize = 24f
            setTextColor(spec.textSecondary)
        })
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(5), 0, 0)
            })
        }
    }

    private fun categoryCard(
        item: Item,
        spec: SettingsThemeSpec,
        accent: Int,
        onCategory: (SettingsCategoryId) -> Unit
    ): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = styler.dashboardCard(spec, accent, stronger = true)
        isClickable = true
        isFocusable = true
        setOnClickListener { onCategory(item.category) }
        addView(TextView(context).apply {
            text = item.icon
            gravity = Gravity.CENTER
            textSize = 17f
            setTextColor(spec.textPrimary)
            background = styler.accentTile(accent)
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), 0, 0, 0)
            addView(TextView(context).apply {
                text = item.title
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(spec.textPrimary)
            })
            addView(TextView(context).apply {
                text = item.detail
                textSize = 9.5f
                setTextColor(spec.textSecondary)
                maxLines = 1
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
