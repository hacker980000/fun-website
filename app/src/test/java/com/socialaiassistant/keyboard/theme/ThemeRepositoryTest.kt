package com.socialaiassistant.keyboard.theme

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeRepositoryTest {
    @Test
    fun defaults_to_locked_social_ai_neon() = runTest {
        val repo = repository(this)
        val theme = repo.current()
        assertEquals("social_ai_neon", theme.id)
        assertTrue(theme.isBuiltIn)
    }

    @Test
    fun preset_activation_and_custom_edit_round_trip() = runTest {
        val repo = repository(this)
        repo.activatePreset("black_gold")
        assertEquals("black_gold", repo.current().id)

        repo.saveCustom(repo.current().copy(primaryNeon = 0xFFFF00FF.toInt(), glowStrength = 31))
        val custom = repo.current()
        assertEquals("custom", custom.id)
        assertFalse(custom.isBuiltIn)
        assertEquals(0xFFFF00FF.toInt(), custom.primaryNeon)
        assertEquals(31, custom.glowStrength)
    }

    @Test
    fun photo_scope_and_fit_persist_in_custom_theme() = runTest {
        val repo = repository(this)
        repo.updateBackground(
            BackgroundPhotoConfig(
                enabled = true,
                localFileName = "keyboard.jpg",
                scope = BackgroundScope.AI_PANEL_ONLY,
                fit = BackgroundFit.FIT,
                opacityPercent = 70,
                darkOverlayPercent = 40,
                blurAmount = 6
            )
        )
        val background = repo.current().background
        assertEquals("custom", repo.current().id)
        assertEquals(BackgroundScope.AI_PANEL_ONLY, background.scope)
        assertEquals(BackgroundFit.FIT, background.fit)
        assertEquals("keyboard.jpg", background.localFileName)
    }

    @Test
    fun decode_without_premium_keys_preserves_legacy_custom_and_background() {
        val custom = ThemePreset.customFrom().copy(
            primaryNeon = 0xFF123456.toInt(),
            background = BackgroundPhotoConfig(true, "keep.jpg")
        )
        val legacy = ThemePreferencesCodec.encode("custom", custom)
        val decoded = ThemePreferencesCodec.decode(legacy)

        assertNull(decoded.selectionState.globalPack)
        assertEquals("custom", decoded.selectionState.legacyActiveThemeId)
        assertEquals(0xFF123456.toInt(), decoded.selectionState.customTheme.primaryNeon)
        assertEquals("keep.jpg", decoded.selectionState.customTheme.background.localFileName)
    }

    @Test
    fun invalid_pack_and_override_values_fall_back_without_crashing() {
        val values = ThemePreferencesCodec.encode(ThemePreset.blackGold.id, ThemePreset.customFrom()).toMutableMap().apply {
            put(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID, "not-a-pack")
            put(ThemePreferencesCodec.overrideKey(KeyboardThemeSurface.ENGLISH), "bad")
        }
        val decoded = ThemePreferencesCodec.decode(values)
        assertNull(decoded.selectionState.globalPack)
        assertTrue(decoded.selectionState.perSurfaceOverrides.isEmpty())
        assertEquals("black_gold", decoded.selectionState.legacyActiveThemeId)
    }

    @Test
    fun key_boundary_preferences_are_independent_per_theme_pack_and_default_off() = runTest {
        val repo = repository(this)
        assertFalse(repo.currentKeyBoundaryEnabled(ThemePack.CLASSIC_DARK))
        assertFalse(repo.currentKeyBoundaryEnabled(ThemePack.GLASS_MODERN))

        repo.setKeyBoundaryEnabled(ThemePack.CLASSIC_DARK, true)

        assertTrue(repo.currentKeyBoundaryEnabled(ThemePack.CLASSIC_DARK))
        assertFalse(repo.currentKeyBoundaryEnabled(ThemePack.GLASS_MODERN))
    }

    @Test
    fun changing_global_pack_clears_surface_overrides_for_complete_package() = runTest {
        val repo = repository(this)
        repo.applyGlobalPack(ThemePack.GLASS_MODERN)
        repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)
        repo.applyGlobalPack(ThemePack.GRADIENT_PRO)

        assertEquals(ThemePack.GRADIENT_PRO, repo.currentSelection().globalPack)
        assertTrue(repo.currentSelection().perSurfaceOverrides.isEmpty())
        assertEquals(
            ThemeCatalog.globalChrome(ThemePack.GRADIENT_PRO).id,
            repo.currentGlobalChromeTheme().id
        )
    }

    @Test
    fun first_run_theme_setup_requires_explicit_package_then_persists_completion() = runTest {
        val repo = repository(this)
        assertFalse(repo.isInitialThemeSetupComplete())

        repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)
        repo.completeInitialThemeSetup(ThemePack.GLASS_MODERN)

        val state = repo.currentSelection()
        assertTrue(repo.isInitialThemeSetupComplete())
        assertEquals(ThemePack.GLASS_MODERN, state.globalPack)
        assertTrue(state.perSurfaceOverrides.isEmpty())
        assertEquals(
            ThemeCatalog.surface(ThemePack.GLASS_MODERN, KeyboardThemeSurface.ENGLISH).id,
            repo.currentSurfaceTheme(KeyboardThemeSurface.ENGLISH).id
        )
    }

    @Test
    fun legacy_premium_selection_is_treated_as_already_onboarded() {
        val values = ThemePreferencesCodec.encode(ThemePreset.socialAiNeon.id, ThemePreset.customFrom())
            .toMutableMap()
            .apply { put(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID, ThemePack.GRADIENT_PRO.storedId) }

        val decoded = ThemePreferencesCodec.decode(values)
        assertTrue(decoded.initialThemeSetupComplete)
    }

    @Test
    fun clear_surface_override_clears_only_that_surface() = runTest {
        val repo = repository(this)
        repo.applyGlobalPack(ThemePack.GLASS_MODERN)
        repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)
        repo.setSurfaceOverride(KeyboardThemeSurface.BIJOY, ThemePack.CLASSIC_DARK)
        repo.clearSurfaceOverride(KeyboardThemeSurface.ENGLISH)

        val state = repo.currentSelection()
        assertFalse(state.perSurfaceOverrides.containsKey(KeyboardThemeSurface.ENGLISH))
        assertEquals(ThemePack.CLASSIC_DARK, state.perSurfaceOverrides[KeyboardThemeSurface.BIJOY])
    }

    @Test
    fun reset_to_selected_pack_is_single_persisted_transaction_and_returns_old_photo() = runTest {
        val repo = repository(this)
        repo.updateBackground(BackgroundPhotoConfig(true, "old.jpg"))
        repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)

        val result = repo.resetToSelectedPack(ThemePack.GRADIENT_PRO)
        val state = repo.currentSelection()

        assertEquals(ThemePack.GRADIENT_PRO, state.globalPack)
        assertTrue(state.perSurfaceOverrides.isEmpty())
        assertEquals("old.jpg", result.backgroundFileToDelete)
        assertFalse(state.customTheme.background.enabled)
        assertTrue(state.customTheme.background.localFileName.isBlank())
    }

    @Test
    fun saving_advanced_custom_appearance_preserves_surface_overrides() = runTest {
        val repo = repository(this)
        repo.applyGlobalPack(ThemePack.GLASS_MODERN)
        repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)

        repo.saveCustom(ThemePreset.customFrom().copy(primaryNeon = 0xFF123456.toInt()))

        val state = repo.currentSelection()
        assertNull(state.globalPack)
        assertEquals("custom", state.legacyActiveThemeId)
        assertEquals(ThemePack.CLEAN_LIGHT, state.perSurfaceOverrides[KeyboardThemeSurface.ENGLISH])
        assertEquals(0xFF123456.toInt(), state.customTheme.primaryNeon)
    }

    private fun repository(scope: TestScope): ThemeRepository {
        val file = File.createTempFile("theme", ".preferences_pb").apply { delete() }
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        return ThemeRepository(store)
    }
}
