package com.socialaiassistant.keyboard.theme

data class ThemeSelectionState(
    val globalPack: ThemePack?,
    val perSurfaceOverrides: Map<KeyboardThemeSurface, ThemePack>,
    val legacyActiveThemeId: String,
    val customTheme: KeyboardTheme
) {
    fun overrideFor(surface: KeyboardThemeSurface): ThemePack? = perSurfaceOverrides[surface]
}
