package com.socialaiassistant.keyboard.settingsui

import android.graphics.Color

data class SettingsThemeSpec(
    val rootTop: Int,
    val rootBottom: Int,
    val panel: Int,
    val panelAlt: Int,
    val row: Int,
    val border: Int,
    val accent: Int,
    val accent2: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val categoryAccents: List<Int>,
    val cornerDp: Float
)

object SettingsThemeCatalog {
    fun spec(pack: SettingsThemePack): SettingsThemeSpec = when (pack) {
        SettingsThemePack.CLEAN_MODERN -> SettingsThemeSpec(
            rootTop = Color.rgb(2, 16, 26),
            rootBottom = Color.rgb(4, 28, 39),
            panel = Color.rgb(7, 24, 36),
            panelAlt = Color.rgb(10, 31, 45),
            row = Color.rgb(12, 33, 48),
            border = Color.rgb(28, 206, 238),
            accent = Color.rgb(45, 213, 245),
            accent2 = Color.rgb(87, 229, 255),
            textPrimary = Color.rgb(248, 251, 255),
            textSecondary = Color.rgb(167, 188, 207),
            categoryAccents = listOf(
                Color.rgb(55, 137, 255), Color.rgb(58, 197, 222), Color.rgb(121, 146, 168),
                Color.rgb(93, 92, 255), Color.rgb(18, 201, 168), Color.rgb(242, 96, 125)
            ),
            cornerDp = 16f
        )

        SettingsThemePack.CARD_STYLE -> SettingsThemeSpec(
            rootTop = Color.rgb(5, 16, 39),
            rootBottom = Color.rgb(8, 25, 57),
            panel = Color.rgb(11, 29, 62),
            panelAlt = Color.rgb(14, 38, 79),
            row = Color.rgb(15, 43, 87),
            border = Color.rgb(50, 142, 255),
            accent = Color.rgb(114, 86, 255),
            accent2 = Color.rgb(54, 146, 255),
            textPrimary = Color.WHITE,
            textSecondary = Color.rgb(178, 197, 222),
            categoryAccents = listOf(
                Color.rgb(117, 87, 255), Color.rgb(255, 166, 65), Color.rgb(18, 210, 171),
                Color.rgb(181, 86, 255), Color.rgb(160, 79, 242), Color.rgb(52, 137, 255),
                Color.rgb(32, 108, 226), Color.rgb(239, 80, 115)
            ),
            cornerDp = 14f
        )

        SettingsThemePack.PREMIUM -> SettingsThemeSpec(
            rootTop = Color.rgb(3, 18, 29),
            rootBottom = Color.rgb(5, 29, 41),
            panel = Color.rgb(7, 25, 37),
            panelAlt = Color.rgb(9, 33, 48),
            row = Color.rgb(10, 35, 50),
            border = Color.rgb(24, 184, 219),
            accent = Color.rgb(32, 207, 231),
            accent2 = Color.rgb(87, 222, 255),
            textPrimary = Color.rgb(249, 252, 255),
            textSecondary = Color.rgb(168, 190, 207),
            categoryAccents = listOf(
                Color.rgb(44, 183, 116), Color.rgb(76, 95, 255), Color.rgb(246, 110, 147),
                Color.rgb(28, 206, 238), Color.rgb(116, 92, 255), Color.rgb(239, 80, 115)
            ),
            cornerDp = 16f
        )

        SettingsThemePack.PRO_STYLE -> SettingsThemeSpec(
            rootTop = Color.rgb(4, 16, 39),
            rootBottom = Color.rgb(7, 29, 64),
            panel = Color.rgb(8, 28, 58),
            panelAlt = Color.rgb(11, 38, 78),
            row = Color.rgb(12, 42, 84),
            border = Color.rgb(37, 140, 255),
            accent = Color.rgb(50, 139, 255),
            accent2 = Color.rgb(61, 215, 242),
            textPrimary = Color.WHITE,
            textSecondary = Color.rgb(183, 199, 224),
            categoryAccents = listOf(
                Color.rgb(52, 118, 230), Color.rgb(244, 72, 105), Color.rgb(255, 196, 54),
                Color.rgb(94, 91, 241), Color.rgb(46, 205, 166), Color.rgb(45, 154, 255)
            ),
            cornerDp = 16f
        )
    }
}
