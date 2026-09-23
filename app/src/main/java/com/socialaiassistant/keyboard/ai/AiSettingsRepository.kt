package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.crypto.SecretStore
import com.socialaiassistant.keyboard.settings.AppSettings
import com.socialaiassistant.keyboard.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow

enum class GatewayMode {
    PERSONAL,
    MANAGED;

    companion object {
        fun fromSetting(value: String?): GatewayMode =
            if (value.equals("personal", ignoreCase = true)) PERSONAL else MANAGED
    }
}

class AiSettingsRepository(
    private val settingsRepository: SettingsRepository,
    private val secretStore: SecretStore
) {
    val settings: Flow<AppSettings> = settingsRepository.settings

    suspend fun current(): AppSettings = settingsRepository.current()

    fun personalApiKey(): String? = secretStore.getOpenRouterKey()

    fun isPersonalApiConfigured(): Boolean = secretStore.isOpenRouterKeyConfigured()

    fun personalApiMask(): String? = secretStore.configuredMask()

    fun savePersonalApiKey(value: String) = secretStore.putOpenRouterKey(value)

    fun clearPersonalApiKey() = secretStore.clearOpenRouterKey()

    companion object {
        const val MANAGED_AI_AVAILABLE = true
    }
}
