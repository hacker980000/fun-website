package com.socialaiassistant.keyboard.theme

object ThemeResolutionPolicy {
    fun resolveGlobalChrome(state: ThemeSelectionState): KeyboardTheme {
        val pack = state.globalPack
        return if (pack != null) ThemeCatalog.globalChrome(pack) else resolveLegacy(state)
    }

    fun resolveSurface(state: ThemeSelectionState, surface: KeyboardThemeSurface): KeyboardTheme {
        val pack = state.perSurfaceOverrides[surface] ?: state.globalPack
        return if (pack != null) ThemeCatalog.surface(pack, surface) else resolveLegacy(state)
    }

    private fun resolveLegacy(state: ThemeSelectionState): KeyboardTheme =
        if (state.legacyActiveThemeId == "custom") state.customTheme
        else ThemePreset.byId(state.legacyActiveThemeId) ?: ThemePreset.socialAiNeon
}
