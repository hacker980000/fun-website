package com.socialaiassistant.keyboard.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.socialaiassistant.keyboard.ai.GatewayMode
import com.socialaiassistant.keyboard.ai.ModelMode
import com.socialaiassistant.keyboard.ai.TonePreset
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @Test
    fun defaults_match_foundation_policy() = runTest {
        val repo = repository(this)
        val value = repo.current()

        assertEquals(ModelMode.FAST, value.modelMode)
        assertEquals(GatewayMode.MANAGED, value.gatewayMode)
        assertTrue(value.preserveDraft)
        assertFalse(value.clipboardHistory)
        assertFalse(value.aiPrivacyConsent)
        assertFalse(value.contextAccessConsent)
        assertFalse(value.accessibilityDisclosureAccepted)
        assertEquals(30, value.maxChatMessages)
        assertEquals(TonePreset.AUTO, value.tonePreset)
        assertEquals("", value.customInstruction)
    }

    @Test
    fun supported_settings_persist_and_invalid_chat_limit_is_normalized() = runTest {
        val repo = repository(this)

        repo.setModelMode(ModelMode.SMART)
        repo.setGatewayMode(GatewayMode.MANAGED)
        repo.setPreserveDraft(false)
        repo.setClipboardHistory(true)
        repo.setAiPrivacyConsent(true)
        repo.setContextAccessConsent(true)
        repo.setAccessibilityDisclosureAccepted(true)
        repo.setMaxChatMessages(10)
        repo.setTonePreset(TonePreset.PROFESSIONAL)
        repo.setCustomInstruction("Keep replies practical and end naturally.")

        val value = repo.current()
        assertEquals(ModelMode.SMART, value.modelMode)
        assertEquals(GatewayMode.MANAGED, value.gatewayMode)
        assertFalse(value.preserveDraft)
        assertTrue(value.clipboardHistory)
        assertTrue(value.aiPrivacyConsent)
        assertTrue(value.contextAccessConsent)
        assertTrue(value.accessibilityDisclosureAccepted)
        assertEquals(10, value.maxChatMessages)
        assertEquals(TonePreset.PROFESSIONAL, value.tonePreset)
        assertEquals("Keep replies practical and end naturally.", value.customInstruction)

        repo.setMaxChatMessages(99)
        assertEquals(30, repo.current().maxChatMessages)
    }

    private fun repository(scope: TestScope): SettingsRepository {
        val file = File.createTempFile("settings", ".preferences_pb").apply { delete() }
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
        return SettingsRepository(store)
    }
}
