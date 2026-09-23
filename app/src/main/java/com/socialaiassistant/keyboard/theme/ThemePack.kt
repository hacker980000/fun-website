package com.socialaiassistant.keyboard.theme

enum class ThemePack(val storedId: String, val displayName: String) {
    CLASSIC_DARK("classic_dark", "Classic Dark"),
    GLASS_MODERN("glass_modern", "Glass Modern"),
    CLEAN_LIGHT("clean_light", "Clean Light"),
    GRADIENT_PRO("gradient_pro", "Gradient Pro");

    companion object {
        fun fromStored(value: String?): ThemePack? = entries.firstOrNull { it.storedId == value }
    }
}
