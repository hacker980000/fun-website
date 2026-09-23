package com.socialaiassistant.keyboard.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeResolutionPolicyTest {
    @Test
    fun surface_override_does_not_change_global_chrome() {
        val state = ThemeSelectionState(
            globalPack = ThemePack.GLASS_MODERN,
            perSurfaceOverrides = mapOf(KeyboardThemeSurface.ENGLISH to ThemePack.CLEAN_LIGHT),
            legacyActiveThemeId = ThemePreset.socialAiNeon.id,
            customTheme = ThemePreset.customFrom()
        )
        assertEquals(
            ThemeCatalog.globalChrome(ThemePack.GLASS_MODERN).id,
            ThemeResolutionPolicy.resolveGlobalChrome(state).id
        )
        assertEquals(
            ThemeCatalog.surface(ThemePack.CLEAN_LIGHT, KeyboardThemeSurface.ENGLISH).id,
            ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.ENGLISH).id
        )
    }

    @Test
    fun legacy_state_without_global_pack_resolves_old_active_theme() {
        val state = ThemeSelectionState(
            globalPack = null,
            perSurfaceOverrides = emptyMap(),
            legacyActiveThemeId = ThemePreset.blackGold.id,
            customTheme = ThemePreset.customFrom()
        )
        assertEquals("black_gold", ThemeResolutionPolicy.resolveGlobalChrome(state).id)
        assertEquals("black_gold", ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.ENGLISH).id)
    }
}
