package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.ContextMessage
import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.safety.FieldSafety
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualAiActionEngineTest {
    @Test
    fun missing_api_key_stops_before_gateway() = runTest {
        val gateway = RecordingGateway()
        val engine = engine(gateway = gateway, apiKeyConfigured = { false })
        engine.request(ManualAiAction.SMART, safety = FieldSafety.ALLOW_AI)
        advanceUntilIdle()
        assertEquals(ManualAiState.NeedsApiKey, engine.state.value)
        assertEquals(0, gateway.calls)
    }

    @Test
    fun social_action_requires_conversation_context() = runTest {
        val engine = engine(contextProvider = { null })
        engine.request(ManualAiAction.WITTY, safety = FieldSafety.NO_CONVERSATION)
        advanceUntilIdle()
        assertEquals(ManualAiState.NeedsContext, engine.state.value)
    }

    @Test
    fun rewrite_works_without_conversation_but_requires_draft() = runTest {
        val engine = engine(contextProvider = { null })
        engine.request(ManualAiAction.REWRITE, draftText = "", safety = FieldSafety.NO_CONVERSATION)
        advanceUntilIdle()
        assertEquals(ManualAiState.NeedsDraft, engine.state.value)

        engine.request(ManualAiAction.REWRITE, draftText = "hello there", safety = FieldSafety.NO_CONVERSATION)
        advanceUntilIdle()
        assertTrue(engine.state.value is ManualAiState.Ready)
    }

    @Test
    fun blocked_field_rejects_all_actions() = runTest {
        val gateway = RecordingGateway()
        val engine = engine(gateway = gateway)
        engine.request(ManualAiAction.REWRITE, draftText = "secret", safety = FieldSafety.BLOCK_AI)
        advanceUntilIdle()
        assertEquals(ManualAiState.Blocked, engine.state.value)
        assertEquals(0, gateway.calls)
    }

    @Test
    fun latest_request_cancels_stale_generation() = runTest {
        val gateway = RecordingGateway(delayFirst = true)
        val engine = engine(gateway = gateway)
        engine.request(ManualAiAction.SMART, safety = FieldSafety.ALLOW_AI)
        runCurrent()
        engine.request(ManualAiAction.WITTY, safety = FieldSafety.ALLOW_AI)
        advanceUntilIdle()
        val ready = engine.state.value as ManualAiState.Ready
        assertEquals(ManualAiAction.WITTY, ready.action)
        assertEquals("reply-2", ready.reply)
    }

    private fun kotlinx.coroutines.test.TestScope.engine(
        gateway: AiGateway = RecordingGateway(),
        apiKeyConfigured: () -> Boolean = { true },
        contextProvider: () -> ContextSnapshot? = { snapshot() }
    ) = ManualAiActionEngine(
        scope = this,
        contextProvider = contextProvider,
        historyProvider = { snap, _ -> snap.messages.map { PromptMessage(it.sender, it.text, it.timestampHint) } },
        settingsProvider = {
            ManualAiEngineSettings(
                modelMode = ModelMode.FAST,
                gatewayMode = GatewayMode.PERSONAL,
                managedSessionAvailable = false,
                aiPrivacyConsent = true,
                contextAccessConsent = true,
                accessibilityDisclosureAccepted = true,
                maxChatMessages = 30
            )
        },
        apiKeyConfigured = apiKeyConfigured,
        promptBuilder = ManualAiPromptBuilder(ExtensionPromptBuilder()),
        gateway = gateway,
        parseReply = { raw -> Regex("\\\"reply\\\":\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.get(1) }
    )

    private fun snapshot() = ContextSnapshot(
        packageName = "org.example.chat",
        windowSignature = "window",
        conversationHint = "Alice",
        messages = listOf(ContextMessage(SenderClass.RECIPIENT, "Hello")),
        latestRecipientMessage = "Hello",
        composerHint = "Message",
        confidence = 0.9f,
        capturedAtMillis = 1_000L
    )

    private class RecordingGateway(private val delayFirst: Boolean = false) : AiGateway {
        var calls = 0
        override suspend fun generate(request: AiGenerationRequest): AiGenerationResult {
            calls++
            if (delayFirst && calls == 1) delay(1_000)
            return AiGenerationResult("{\"reply\":\"reply-$calls\",\"confidence\":0.9}", "test")
        }
    }
}
