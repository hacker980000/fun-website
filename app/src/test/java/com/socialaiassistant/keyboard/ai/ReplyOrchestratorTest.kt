package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.ContextMessage
import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.ConversationKey
import com.socialaiassistant.keyboard.context.ConversationSurface
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.ime.ImeSession
import com.socialaiassistant.keyboard.memory.ConversationHistory
import com.socialaiassistant.keyboard.memory.ConversationHistoryMessage
import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyOrchestratorTest {
    @Test
    fun context_changes_make_zero_calls_until_request() = runTest {
        val f = fixture()
        f.snapshots.value = snapshot(SenderClass.RECIPIENT, "How are you?")
        advanceUntilIdle()
        f.snapshots.value = snapshot(SenderClass.RECIPIENT, "Are you free?", 2_000L)
        advanceUntilIdle()
        assertEquals(0, f.gateway.calls)
    }

    @Test
    fun one_request_makes_exactly_one_call() = runTest {
        val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertEquals(1, f.gateway.calls)
        assertTrue(f.orchestrator.state.value is ReplyState.Ready)
    }

    @Test
    fun context_change_after_result_does_not_regenerate() = runTest {
        val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        f.snapshots.value = snapshot(SenderClass.RECIPIENT, "New incoming message", 2_000L)
        advanceUntilIdle()
        assertEquals(1, f.gateway.calls)
    }

    @Test
    fun second_explicit_request_creates_second_call() = runTest {
        val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        f.snapshots.value = snapshot(SenderClass.RECIPIENT, "New incoming message", 2_000L)
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertEquals(2, f.gateway.calls)
    }

    @Test
    fun duplicate_tap_while_loading_does_not_duplicate_request() = runTest {
        val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"), gatewayDelayMs = 1_000)
        f.orchestrator.requestAuto()
        runCurrent()
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertEquals(1, f.gateway.calls)
    }

    @Test
    fun recipient_last_uses_reply_intent() = runTest {
        val f = fixture(snapshot(SenderClass.RECIPIENT, "Answer me"))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertTrue(f.gateway.lastPromptUser.contains("Answer me"))
        assertTrue(f.gateway.lastPromptSystem.contains("Reply to the latest OTHER message"))
    }

    @Test
    fun sender_last_uses_continue_intent() = runTest {
        val f = fixture(snapshot(SenderClass.SELF, "I will call later"))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertTrue(f.gateway.lastPromptSystem.contains("Continue naturally without pretending OTHER replied"))
    }

    @Test
    fun empty_with_hint_uses_start_intent() = runTest {
        val f = fixture(emptySnapshot("Alice"))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertTrue(f.gateway.lastPromptSystem.contains("Write a natural first message"))
    }

    @Test
    fun empty_without_hint_needs_context_without_gateway_call() = runTest {
        val f = fixture(emptySnapshot(null))
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertEquals(ReplyState.WaitingForContext, f.orchestrator.state.value)
        assertEquals(0, f.gateway.calls)
    }

    @Test
    fun blocked_field_cancels_in_flight_request_and_hides_reply() = runTest {
        val f = fixture(snapshot(SenderClass.RECIPIENT, "first"), gatewayDelayMs = 1_000)
        f.orchestrator.requestAuto()
        runCurrent()
        assertEquals(ReplyState.Loading, f.orchestrator.state.value)
        f.sessions.value = allowedSession().copy(safety = FieldSafety.BLOCK_AI)
        runCurrent()
        assertEquals(ReplyState.Hidden, f.orchestrator.state.value)
        advanceUntilIdle()
        assertEquals(ReplyState.Hidden, f.orchestrator.state.value)
    }

    @Test
    fun managed_mode_generates_only_after_explicit_request() = runTest {
        val f = fixture(
            initialSnapshot = snapshot(SenderClass.RECIPIENT, "Managed context"),
            gatewayMode = GatewayMode.MANAGED
        )
        advanceUntilIdle()
        assertEquals(0, f.gateway.calls)
        f.orchestrator.requestAuto()
        advanceUntilIdle()
        assertEquals(1, f.gateway.calls)
    }

    private data class Fixture(
        val snapshots: MutableStateFlow<ContextSnapshot?>,
        val sessions: MutableStateFlow<ImeSession?>,
        val gateway: RecordingGateway,
        val orchestrator: ReplyOrchestrator
    )

    private fun TestScope.fixture(
        initialSnapshot: ContextSnapshot? = null,
        gatewayDelayMs: Long = 0L,
        gatewayMode: GatewayMode = GatewayMode.PERSONAL
    ): Fixture {
        val snapshots = MutableStateFlow(initialSnapshot)
        val sessions = MutableStateFlow<ImeSession?>(allowedSession())
        val gateway = RecordingGateway(gatewayDelayMs)
        val orchestrator = ReplyOrchestrator(
            scope = this,
            snapshots = snapshots,
            sessions = sessions,
            mergeHistory = { snap -> history(snap) },
            settingsProvider = {
                AppSettings(
                    gatewayMode = gatewayMode,
                    aiPrivacyConsent = true,
                    contextAccessConsent = true,
                    accessibilityDisclosureAccepted = true,
                    maxChatMessages = 30
                )
            },
            apiKeyConfigured = { true },
            managedSessionAvailable = { true },
            promptBuilder = ExtensionPromptBuilder(),
            gateway = gateway,
            resultParser = ModelResultParser()
        )
        return Fixture(snapshots, sessions, gateway, orchestrator)
    }

    private fun allowedSession() = ImeSession(
        packageName = "org.example.chat",
        safety = FieldSafety.ALLOW_AI,
        inputType = 1,
        hintText = "Message"
    )

    private fun snapshot(sender: SenderClass, text: String, capturedAt: Long = 1_000L) = ContextSnapshot(
        packageName = "org.example.chat",
        windowSignature = "window-1",
        conversationHint = "Alice",
        messages = listOf(ContextMessage(sender, text)),
        latestRecipientMessage = text.takeIf { sender == SenderClass.RECIPIENT },
        composerHint = "Message",
        surface = ConversationSurface.INBOX,
        confidence = 0.9f,
        capturedAtMillis = capturedAt
    )

    private fun emptySnapshot(hint: String?) = ContextSnapshot(
        packageName = "org.example.chat",
        windowSignature = "window-1",
        conversationHint = hint,
        messages = emptyList(),
        latestRecipientMessage = null,
        composerHint = "Message",
        surface = ConversationSurface.INBOX,
        confidence = 0.9f,
        capturedAtMillis = 1_000L
    )

    private fun history(snapshot: ContextSnapshot) = ConversationHistory(
        key = ConversationKey("conv_test"),
        packageName = snapshot.packageName,
        messages = snapshot.messages.map {
            ConversationHistoryMessage(
                sender = it.sender.name,
                text = it.text,
                capturedAtMillis = snapshot.capturedAtMillis,
                timestampHint = it.timestampHint
            )
        }
    )

    private class RecordingGateway(private val delayMs: Long = 0L) : AiGateway {
        var calls = 0
        var lastPromptSystem = ""
        var lastPromptUser = ""

        override suspend fun generate(request: AiGenerationRequest): AiGenerationResult {
            calls++
            lastPromptSystem = request.prompt.system
            lastPromptUser = request.prompt.user
            if (delayMs > 0) delay(delayMs)
            return AiGenerationResult(
                rawText = """{"reply":"ok","confidence":1.0}""",
                model = OpenRouterModels.FAST_PRIMARY
            )
        }
    }
}
