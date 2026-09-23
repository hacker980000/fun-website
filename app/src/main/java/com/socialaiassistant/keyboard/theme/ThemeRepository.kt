package com.socialaiassistant.keyboard.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.socialAiThemeDataStore by preferencesDataStore(name = "social_ai_theme_settings")

data class ResetThemeResult(val backgroundFileToDelete: String?)

class ThemeRepository(
    private val store: DataStore<Preferences>
) {
    private val data: Flow<DecodedThemePreferences> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            ThemePreferencesCodec.decode(
                preferences.asMap().entries.associate { (key, value) -> key.name to value.toString() }
            )
        }

    val selectionState: Flow<ThemeSelectionState> = data.map { it.selectionState }
    val keyBoundarySettings: Flow<Map<ThemePack, Boolean>> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            ThemePack.entries.associateWith { pack ->
                preferences[keyBoundaryKey(pack)] ?: false
            }
        }
    val bubbleAppearanceSettings: Flow<Map<ThemePack, ThemeBubbleAppearance>> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            ThemePack.entries.associateWith { pack ->
                ThemeBubbleAppearance(
                    enabled = preferences[bubbleEnabledKey(pack)] ?: false,
                    intensity = BubbleKeyIntensity.fromSetting(preferences[bubbleIntensityKey(pack)])
                )
            }
        }
    val initialThemeSetupComplete: Flow<Boolean> = data.map { it.initialThemeSetupComplete }
    val globalChromeTheme: Flow<KeyboardTheme> = selectionState.map(ThemeResolutionPolicy::resolveGlobalChrome)
    val theme: Flow<KeyboardTheme> = globalChromeTheme

    fun surfaceTheme(surface: KeyboardThemeSurface): Flow<KeyboardTheme> =
        selectionState.map { ThemeResolutionPolicy.resolveSurface(it, surface) }

    suspend fun current(): KeyboardTheme = currentGlobalChromeTheme()

    suspend fun currentSelection(): ThemeSelectionState = selectionState.first()

    suspend fun isInitialThemeSetupComplete(): Boolean = initialThemeSetupComplete.first()

    suspend fun currentGlobalChromeTheme(): KeyboardTheme = globalChromeTheme.first()

    suspend fun currentSurfaceTheme(surface: KeyboardThemeSurface): KeyboardTheme =
        ThemeResolutionPolicy.resolveSurface(currentSelection(), surface)

    suspend fun currentCustom(): KeyboardTheme = data.first().customTheme

    fun keyBoundaryEnabled(pack: ThemePack): Flow<Boolean> =
        keyBoundarySettings.map { settings -> settings[pack] ?: false }

    suspend fun currentKeyBoundaryEnabled(pack: ThemePack): Boolean =
        keyBoundaryEnabled(pack).first()

    suspend fun setKeyBoundaryEnabled(pack: ThemePack, enabled: Boolean) {
        store.edit { preferences -> preferences[keyBoundaryKey(pack)] = enabled }
    }

    fun bubbleAppearance(pack: ThemePack): Flow<ThemeBubbleAppearance> =
        bubbleAppearanceSettings.map { settings -> settings[pack] ?: ThemeBubbleAppearance() }

    suspend fun currentBubbleAppearance(pack: ThemePack): ThemeBubbleAppearance =
        bubbleAppearance(pack).first()

    suspend fun setBubbleEnabled(pack: ThemePack, enabled: Boolean) {
        store.edit { preferences -> preferences[bubbleEnabledKey(pack)] = enabled }
    }

    suspend fun setBubbleIntensity(pack: ThemePack, intensity: BubbleKeyIntensity) {
        store.edit { preferences -> preferences[bubbleIntensityKey(pack)] = intensity.settingValue }
    }

    suspend fun seedBubbleAppearanceIfMissing(enabled: Boolean, intensity: BubbleKeyIntensity) {
        store.edit { preferences ->
            ThemePack.entries.forEach { pack ->
                val enabledKey = bubbleEnabledKey(pack)
                val intensityKey = bubbleIntensityKey(pack)
                if (preferences[enabledKey] == null) preferences[enabledKey] = enabled
                if (preferences[intensityKey] == null) preferences[intensityKey] = intensity.settingValue
            }
        }
    }

    suspend fun applyGlobalPack(pack: ThemePack) {
        val current = currentSelection()
        persistSelection(
            current.copy(
                globalPack = pack,
                perSurfaceOverrides = emptyMap()
            )
        )
    }

    suspend fun completeInitialThemeSetup(pack: ThemePack) {
        val current = currentSelection()
        val next = current.copy(
            globalPack = pack,
            perSurfaceOverrides = emptyMap()
        )
        val encoded = ThemePreferencesCodec.encodeSelection(next)
        store.edit { preferences ->
            clearPremiumKeys(preferences)
            writeEncoded(preferences, encoded)
            preferences[key(ThemePreferencesCodec.INITIAL_THEME_SETUP_COMPLETE)] = true.toString()
        }
    }

    suspend fun setSurfaceOverride(surface: KeyboardThemeSurface, pack: ThemePack) {
        val current = currentSelection()
        persistSelection(
            current.copy(perSurfaceOverrides = current.perSurfaceOverrides + (surface to pack))
        )
    }

    suspend fun clearSurfaceOverride(surface: KeyboardThemeSurface) {
        val current = currentSelection()
        persistSelection(
            current.copy(perSurfaceOverrides = current.perSurfaceOverrides - surface)
        )
    }

    suspend fun resetToSelectedPack(pack: ThemePack): ResetThemeResult {
        val before = currentSelection()
        val fileToDelete = before.customTheme.background.localFileName.takeIf { it.isNotBlank() }
        val canonical = ThemeCatalog.globalChrome(pack)
            .copy(background = BackgroundPhotoConfig())
            .asCustom()
            .normalized()
        val resetState = ThemeSelectionState(
            globalPack = pack,
            perSurfaceOverrides = emptyMap(),
            legacyActiveThemeId = ThemePreset.socialAiNeon.id,
            customTheme = canonical
        )
        persistSelection(resetState)
        return ResetThemeResult(fileToDelete)
    }

    suspend fun activatePreset(id: String) {
        val normalized = when {
            id == "custom" -> "custom"
            ThemePreset.byId(id) != null -> id
            else -> ThemePreset.socialAiNeon.id
        }
        val current = currentSelection()
        persistSelection(
            current.copy(
                globalPack = null,
                perSurfaceOverrides = emptyMap(),
                legacyActiveThemeId = normalized
            )
        )
    }

    suspend fun saveCustom(theme: KeyboardTheme) {
        val current = currentSelection()
        persistSelection(ThemeSelectionMutations.withCustomAppearance(current, theme))
    }

    suspend fun editCurrent(transform: (KeyboardTheme) -> KeyboardTheme) {
        val next = transform(current()).asCustom().normalized()
        saveCustom(next)
    }

    suspend fun updateBackground(config: BackgroundPhotoConfig) {
        editCurrent { current -> current.copy(background = config.normalized()) }
    }

    suspend fun clearBackground(): String? {
        val previous = current().background.localFileName.takeIf { it.isNotBlank() }
        editCurrent { current -> current.copy(background = BackgroundPhotoConfig()) }
        return previous
    }

    suspend fun resetCustom() {
        saveCustom(ThemePreset.customFrom())
    }

    private suspend fun persistSelection(state: ThemeSelectionState) {
        val encoded = ThemePreferencesCodec.encodeSelection(state)
        store.edit { preferences ->
            clearPremiumKeys(preferences)
            writeEncoded(preferences, encoded)
        }
    }

    private fun clearPremiumKeys(preferences: MutablePreferences) {
        preferences.remove(key(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID))
        preferences.remove(key(ThemePreferencesCodec.THEME_SELECTION_SCHEMA_VERSION))
        KeyboardThemeSurface.entries.forEach { surface ->
            preferences.remove(key(ThemePreferencesCodec.overrideKey(surface)))
        }
    }

    private fun writeEncoded(preferences: MutablePreferences, values: Map<String, String>) {
        values.forEach { (name, value) -> preferences[key(name)] = value }
    }

    private fun key(name: String): Preferences.Key<String> = stringPreferencesKey(name)

    private fun keyBoundaryKey(pack: ThemePack): Preferences.Key<Boolean> =
        booleanPreferencesKey("theme_${pack.storedId}_key_boundary")

    private fun bubbleEnabledKey(pack: ThemePack): Preferences.Key<Boolean> =
        booleanPreferencesKey("theme_${pack.storedId}_bubble_enabled")

    private fun bubbleIntensityKey(pack: ThemePack): Preferences.Key<String> =
        stringPreferencesKey("theme_${pack.storedId}_bubble_intensity")

    companion object {
        fun create(context: Context): ThemeRepository = ThemeRepository(context.applicationContext.socialAiThemeDataStore)
    }
}
