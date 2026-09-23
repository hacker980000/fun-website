package com.socialaiassistant.keyboard.settingsui

import com.socialaiassistant.keyboard.theme.ThemePack

enum class SettingsThemePack(
    val storedId: String,
    val displayName: String,
    val subtitle: String
) {
    CLEAN_MODERN(
        storedId = "clean_modern",
        displayName = "Clean Modern",
        subtitle = "Focused list navigation with cyan neon accents"
    ),
    CARD_STYLE(
        storedId = "card_style",
        displayName = "Card Style",
        subtitle = "Color-coded category cards for fast access"
    ),
    PREMIUM(
        storedId = "premium",
        displayName = "Premium",
        subtitle = "Elegant category rows with vivid premium icons"
    ),
    PRO_STYLE(
        storedId = "pro_style",
        displayName = "Pro Style",
        subtitle = "Professional menu hierarchy with a personalization banner"
    );

    companion object {
        fun fromStored(value: String?): SettingsThemePack? = entries.firstOrNull { it.storedId == value }

        fun recommendedFor(keyboardPack: ThemePack?): SettingsThemePack = when (keyboardPack) {
            ThemePack.CLASSIC_DARK -> CLEAN_MODERN
            ThemePack.GLASS_MODERN -> CARD_STYLE
            ThemePack.CLEAN_LIGHT -> PREMIUM
            ThemePack.GRADIENT_PRO -> PRO_STYLE
            null -> CLEAN_MODERN
        }
    }
}
