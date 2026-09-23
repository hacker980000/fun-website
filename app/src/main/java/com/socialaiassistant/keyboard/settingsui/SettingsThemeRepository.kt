package com.socialaiassistant.keyboard.settingsui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.socialAiSettingsThemeDataStore by preferencesDataStore(name = "social_ai_settings_theme")

data class SettingsThemeState(
    val selectedPack: SettingsThemePack?,
    val initialSetupComplete: Boolean
)

class SettingsThemeRepository(
    private val store: DataStore<Preferences>
) {
    val state: Flow<SettingsThemeState> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            SettingsThemeState(
                selectedPack = SettingsThemePack.fromStored(preferences[PACK_ID]),
                initialSetupComplete = preferences[SETUP_COMPLETE] ?: false
            )
        }

    suspend fun currentState(): SettingsThemeState = state.first()

    suspend fun currentPack(): SettingsThemePack =
        currentState().selectedPack ?: SettingsThemePack.CLEAN_MODERN

    suspend fun isInitialSetupComplete(): Boolean = currentState().initialSetupComplete

    suspend fun completeInitialSetup(pack: SettingsThemePack) {
        store.edit { preferences ->
            preferences[PACK_ID] = pack.storedId
            preferences[SETUP_COMPLETE] = true
        }
    }

    suspend fun setPack(pack: SettingsThemePack) {
        store.edit { preferences ->
            preferences[PACK_ID] = pack.storedId
            preferences[SETUP_COMPLETE] = true
        }
    }

    companion object {
        private val PACK_ID = stringPreferencesKey("settings_theme_pack_id")
        private val SETUP_COMPLETE = booleanPreferencesKey("settings_theme_setup_complete")

        fun create(context: Context): SettingsThemeRepository =
            SettingsThemeRepository(context.applicationContext.socialAiSettingsThemeDataStore)
    }
}
