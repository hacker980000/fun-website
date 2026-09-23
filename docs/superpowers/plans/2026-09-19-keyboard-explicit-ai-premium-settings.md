# Keyboard Explicit AI + Premium Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Social AI Keyboard generate only from explicit user actions, safely normalize model output, replace the consent notice with a premium CTA, redesign Settings into the locked Neon/Glass hub, and consume product-specific account entitlement state without breaking existing keyboard/theme/data behavior.

**Architecture:** Keep accessibility/context capture passive and move all generation behind request methods invoked by explicit taps. Add a pure conversation-intent resolver, harden `ModelResultParser`, keep manual AI actions explicit, and make the toolbar AI button call the request-driven orchestrator. Redesign `MainActivity` as a card-based settings hub backed by the existing repositories, while extending the backend client with `KEYBOARD` product identity and normalized entitlement state.

**Tech Stack:** Android/Kotlin, Coroutines/StateFlow, AndroidX DataStore, kotlinx.serialization, OkHttp, AppCompat/XML layouts, Robolectric/JUnit, existing Premium Neon Theme Engine.

**Spec:** `docs/superpowers/specs/2026-09-19-explicit-ai-settings-product-entitlements-design.md`

## Global Constraints

- Baseline remains Social AI Keyboard v35.3.1 Premium Neon; existing typing, Bangla/English/Phonetic/Bijoy, voice, clipboard, captions, theme engine, account/device-proof, quota and privacy features must keep working.
- No context/accessibility event may call an AI gateway.
- One explicit tap produces at most one generation request; context changes after a result produce zero additional requests.
- No AI result is auto-sent or auto-posted; insert/append/replace and final Send/Post remain explicit.
- Raw JSON, code fences, category/confidence metadata, or backend envelopes must never render as reply text.
- Existing settings and theme preferences must not be silently reset.
- Android client product identity is `KEYBOARD` on product-sensitive authenticated requests.
- No new storage/media permission is added for settings or theme background features.

## Review Focus

1. Rapid double-tap on toolbar AI while one request is loading must not create two gateway calls; the loading UI disables/ignores the duplicate tap.
2. A context snapshot with only `UNKNOWN` sender messages must not pretend a recipient replied; it resolves to START only when safe hints exist, otherwise NEEDS_CONTEXT.
3. A malformed payload that starts with `JSON`, braces, or code fences must never fall through as visible plain text; it must return parse failure and a friendly retry state.
4. Existing DataStore values and ThemeRepository state must survive the Settings redesign unchanged across app restart.
5. A logged-in account that has `ASSISTANT_PRO` only must show Keyboard access required/expired rather than appearing generally subscribed.

---

### Task 1: Add pure conversation AI intent resolution

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ConversationAiIntentResolver.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/ai/ConversationAiIntentResolverTest.kt`

**Interfaces:**
- Consumes: `ContextSnapshot`, `ContextMessage`, `SenderClass`.
- Produces: `enum class ConversationAiIntent { REPLY, CONTINUE, START, NEEDS_CONTEXT }` and `ConversationAiIntentResolver.resolve(snapshot: ContextSnapshot): ConversationAiIntent`.

- [ ] **Step 1: Write failing resolver tests**

```kotlin
class ConversationAiIntentResolverTest {
    private val resolver = ConversationAiIntentResolver()

    @Test fun recipient_last_means_reply() {
        val snapshot = snapshot(
            listOf(
                ContextMessage(SenderClass.SELF, "Hi"),
                ContextMessage(SenderClass.RECIPIENT, "How are you?")
            )
        )
        assertEquals(ConversationAiIntent.REPLY, resolver.resolve(snapshot))
    }

    @Test fun sender_last_means_continue() {
        val snapshot = snapshot(
            listOf(
                ContextMessage(SenderClass.RECIPIENT, "Hello"),
                ContextMessage(SenderClass.SELF, "I will call later")
            )
        )
        assertEquals(ConversationAiIntent.CONTINUE, resolver.resolve(snapshot))
    }

    @Test fun empty_history_with_conversation_hint_means_start() {
        assertEquals(ConversationAiIntent.START, resolver.resolve(snapshot(emptyList(), "Jaan")))
    }

    @Test fun empty_history_without_hint_needs_context() {
        assertEquals(ConversationAiIntent.NEEDS_CONTEXT, resolver.resolve(snapshot(emptyList(), null)))
    }

    @Test fun unknown_only_history_never_becomes_reply() {
        val snapshot = snapshot(listOf(ContextMessage(SenderClass.UNKNOWN, "something")), null)
        assertEquals(ConversationAiIntent.NEEDS_CONTEXT, resolver.resolve(snapshot))
    }

    private fun snapshot(messages: List<ContextMessage>, hint: String? = "Alice") = ContextSnapshot(
        packageName = "org.example.chat",
        windowSignature = "window-1",
        conversationHint = hint,
        messages = messages,
        latestRecipientMessage = messages.lastOrNull { it.sender == SenderClass.RECIPIENT }?.text,
        composerHint = "Message",
        surface = ConversationSurface.INBOX,
        confidence = 0.9f,
        capturedAtMillis = 1_000L
    )
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run:
```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ConversationAiIntentResolverTest'
```
Expected: compilation failure because `ConversationAiIntentResolver` does not exist.

- [ ] **Step 3: Implement the minimal pure resolver**

```kotlin
enum class ConversationAiIntent { REPLY, CONTINUE, START, NEEDS_CONTEXT }

class ConversationAiIntentResolver {
    fun resolve(snapshot: ContextSnapshot): ConversationAiIntent {
        val latest = snapshot.messages.asReversed().firstOrNull { message ->
            message.text.isNotBlank() && message.sender != SenderClass.UNKNOWN
        }
        return when (latest?.sender) {
            SenderClass.RECIPIENT -> ConversationAiIntent.REPLY
            SenderClass.SELF -> ConversationAiIntent.CONTINUE
            else -> if (!snapshot.conversationHint.isNullOrBlank()) {
                ConversationAiIntent.START
            } else {
                ConversationAiIntent.NEEDS_CONTEXT
            }
        }
    }
}
```

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run the same Gradle command. Expected: all resolver tests pass.

- [ ] **Step 5: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai/ConversationAiIntentResolver.kt app/src/test/java/com/socialaiassistant/keyboard/ai/ConversationAiIntentResolverTest.kt
git commit -m "feat: resolve explicit conversation AI intent"
```

### Task 2: Refactor ReplyOrchestrator to explicit request-only generation

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ReplyOrchestrator.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/AiModels.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiAction.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/backend/ManagedAiPayload.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/ai/ReplyOrchestratorTest.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/ai/ExtensionLanguageLogicTest.kt` only if prompt-language assertions need adjustment.

**Interfaces:**
- Consumes: `ConversationAiIntentResolver`, latest `snapshots.value`, latest `sessions.value`, existing gateway/settings/history providers.
- Produces: `ReplyOrchestrator.requestAuto(): Unit`, `ReplyOrchestrator.clear(): Unit`; `PromptRequest.conversationIntent: ConversationAiIntent?`; managed social payload field `conversationIntent` with values `REPLY`, `CONTINUE`, or `START`.

- [ ] **Step 1: Replace auto-generation tests with explicit-trigger tests**

```kotlin
@Test fun context_changes_make_zero_calls_until_request() = runTest {
    val f = fixture()
    f.snapshots.value = snapshot(SenderClass.RECIPIENT, "How are you?")
    advanceUntilIdle()
    f.snapshots.value = snapshot(SenderClass.RECIPIENT, "Are you free?", 2_000L)
    advanceUntilIdle()
    assertEquals(0, f.gateway.calls)
}

@Test fun one_request_makes_exactly_one_call() = runTest {
    val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"))
    f.orchestrator.requestAuto()
    advanceUntilIdle()
    assertEquals(1, f.gateway.calls)
    assertTrue(f.orchestrator.state.value is ReplyState.Ready)
}

@Test fun context_change_after_result_does_not_regenerate() = runTest {
    val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"))
    f.orchestrator.requestAuto(); advanceUntilIdle()
    f.snapshots.value = snapshot(SenderClass.RECIPIENT, "New incoming message", 2_000L)
    advanceUntilIdle()
    assertEquals(1, f.gateway.calls)
}

@Test fun second_explicit_request_creates_second_call() = runTest {
    val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"))
    f.orchestrator.requestAuto(); advanceUntilIdle()
    f.snapshots.value = snapshot(SenderClass.RECIPIENT, "New incoming message", 2_000L)
    f.orchestrator.requestAuto(); advanceUntilIdle()
    assertEquals(2, f.gateway.calls)
}

@Test fun duplicate_tap_while_loading_does_not_duplicate_request() = runTest {
    val f = fixture(snapshot(SenderClass.RECIPIENT, "How are you?"), gatewayDelayMs = 1_000)
    f.orchestrator.requestAuto(); runCurrent()
    f.orchestrator.requestAuto(); advanceUntilIdle()
    assertEquals(1, f.gateway.calls)
}

@Test fun recipient_last_uses_reply_intent() = runTest {
    val f = fixture(snapshot(SenderClass.RECIPIENT, "Answer me"))
    f.orchestrator.requestAuto(); advanceUntilIdle()
    assertTrue(f.gateway.lastPromptUser.contains("Answer me"))
    assertTrue(f.gateway.lastPromptSystem.contains("Reply to the latest OTHER message"))
}

@Test fun sender_last_uses_continue_intent() = runTest {
    val f = fixture(snapshot(SenderClass.SELF, "I will call later"))
    f.orchestrator.requestAuto(); advanceUntilIdle()
    assertTrue(f.gateway.lastPromptSystem.contains("Continue naturally without pretending OTHER replied"))
}

@Test fun empty_with_hint_uses_start_intent() = runTest {
    val f = fixture(emptySnapshot("Alice"))
    f.orchestrator.requestAuto(); advanceUntilIdle()
    assertTrue(f.gateway.lastPromptSystem.contains("Write a natural first message"))
}

@Test fun empty_without_hint_needs_context_without_gateway_call() = runTest {
    val f = fixture(emptySnapshot(null))
    f.orchestrator.requestAuto(); advanceUntilIdle()
    assertEquals(ReplyState.WaitingForContext, f.orchestrator.state.value)
    assertEquals(0, f.gateway.calls)
}

private data class Fixture(
    val snapshots: MutableStateFlow<ContextSnapshot?>,
    val sessions: MutableStateFlow<ImeSession?>,
    val gateway: RecordingGateway,
    val orchestrator: ReplyOrchestrator
)

private fun TestScope.fixture(
    initialSnapshot: ContextSnapshot? = null,
    gatewayDelayMs: Long = 0L
): Fixture {
    val snapshots = MutableStateFlow(initialSnapshot)
    val sessions = MutableStateFlow(allowedSession())
    val gateway = RecordingGateway(gatewayDelayMs)
    val orchestrator = ReplyOrchestrator(
        scope = this,
        snapshots = snapshots,
        sessions = sessions,
        mergeHistory = { snap -> history(snap) },
        settingsProvider = { AppSettings(aiPrivacyConsent = true, contextAccessConsent = true, accessibilityDisclosureAccepted = true) },
        apiKeyConfigured = { true },
        managedSessionAvailable = { false },
        promptBuilder = ExtensionPromptBuilder(),
        gateway = gateway,
        resultParser = ModelResultParser()
    )
    return Fixture(snapshots, sessions, gateway, orchestrator)
}

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

private class RecordingGateway(private val delayMs: Long = 0L) : AiGateway {
    var calls = 0
    var lastPromptSystem = ""
    var lastPromptUser = ""
    override suspend fun generate(request: AiGenerationRequest): AiGenerationResult {
        calls++
        if (delayMs > 0) delay(delayMs)
        lastPromptSystem = request.prompt.system
        lastPromptUser = request.prompt.user
        return AiGenerationResult("{\"reply\":\"ok\",\"confidence\":1.0}", OpenRouterModels.FAST_PRIMARY)
    }
}
```

- [ ] **Step 2: Run `ReplyOrchestratorTest` and confirm RED**

Run:
```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ReplyOrchestratorTest'
```
Expected: failures because generation is still driven by `combine(sessions, snapshots)` and `requestAuto()` does not exist.

- [ ] **Step 3: Convert the orchestrator to request-driven state**

Implement these rules:

```kotlin
private var activeJob: Job? = null

fun requestAuto() {
    if (activeJob?.isActive == true) return
    val session = sessions.value
    val snapshot = snapshots.value
    activeJob = scope.launch { processExplicit(session, snapshot) }
}

fun clear() {
    activeJob?.cancel()
    activeJob = null
    _state.value = ReplyState.Hidden
}
```

Remove the initialization path that calls `process()` for each snapshot. Keep a lightweight session observer only to cancel/hide if the field becomes blocked:

```kotlin
scope.launch {
    sessions.collectLatest { session ->
        if (session == null || session.safety != FieldSafety.ALLOW_AI) clear()
    }
}
```

At request time call `ConversationAiIntentResolver.resolve(snapshot)`. `NEEDS_CONTEXT` returns `WaitingForContext` without a gateway call. Pass the resolved intent into `PromptRequest` and into the managed payload.

For social manual actions, resolve the same intent from the tap-time snapshot and carry it through `ManualAiRequest.conversationIntent` and `ManagedAiPayload.social(..., conversationIntent = ...)`. Draft actions leave the field null. This keeps Smart/Unique/Flirty/Funny explicit and prevents the managed server from falling back to an older recipient turn when SELF is latest.

- [ ] **Step 4: Make inbox prompt semantics explicit**

Add to `PromptRequest`:

```kotlin
val conversationIntent: ConversationAiIntent? = null
```

Extend `ManagedAiPayload.social` with:

```kotlin
conversationIntent: ConversationAiIntent? = null
```

and for INBOX payloads add:

```kotlin
conversationIntent?.takeIf { it != ConversationAiIntent.NEEDS_CONTEXT }?.let {
    put("conversationIntent", it.name)
}
```

In `ExtensionPromptBuilder.buildInboxRules`, append an exact branch:

```kotlin
val intentRules = when (request.conversationIntent) {
    ConversationAiIntent.REPLY -> "Reply to the latest OTHER message."
    ConversationAiIntent.CONTINUE -> "The latest meaningful message is SELF. Continue naturally without pretending OTHER replied."
    ConversationAiIntent.START -> "Write a natural first message. Do not invent prior familiarity or facts."
    ConversationAiIntent.NEEDS_CONTEXT, null -> "Use only supported visible context."
}
```

- [ ] **Step 5: Preserve safety and stale-result handling**

Each explicit request captures a monotonically increasing request token. Before publishing a result, verify the token is still current and session safety remains `ALLOW_AI`; cancellation/blocked state must hide the result.

- [ ] **Step 6: Run focused tests and then all AI unit tests**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ReplyOrchestratorTest' --tests '*ConversationAiIntentResolverTest' --tests '*ManualAiActionEngineTest'
```
Expected: PASS, with zero auto-generation assertions green.

- [ ] **Step 7: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai app/src/test/java/com/socialaiassistant/keyboard/ai
git commit -m "fix: require explicit taps for conversational AI"
```

### Task 3: Wire the toolbar AI button to explicit AUTO generation

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SmartReplyController.kt`
- Create or modify: `app/src/test/java/com/socialaiassistant/keyboard/ime/AiPanelPolicyTest.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/ExplicitAiTriggerController.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/ime/ExplicitAiTriggerControllerTest.kt`

**Interfaces:**
- Consumes: `ReplyOrchestrator.requestAuto()` and `ReplyState`.
- Produces: toolbar `AI` tap opens a result/loading surface and starts exactly one AUTO request; no other IME lifecycle event triggers it.

- [ ] **Step 1: Write a failing IME trigger test**

Introduce a tiny testable controller used by the IME toolbar:

```kotlin
internal class ExplicitAiTriggerController(
    private val requestAuto: () -> Unit
) {
    fun onToolbarTap(safety: FieldSafety, loading: Boolean): Boolean {
        if (safety != FieldSafety.ALLOW_AI || loading) return false
        requestAuto()
        return true
    }
}

class ExplicitAiTriggerControllerTest {
    @Test fun toolbar_tap_requests_once_when_allowed() {
        var calls = 0
        val controller = ExplicitAiTriggerController { calls++ }
        assertTrue(controller.onToolbarTap(FieldSafety.ALLOW_AI, loading = false))
        assertEquals(1, calls)
    }

    @Test fun loading_or_blocked_state_never_requests() {
        var calls = 0
        val controller = ExplicitAiTriggerController { calls++ }
        assertFalse(controller.onToolbarTap(FieldSafety.ALLOW_AI, loading = true))
        assertFalse(controller.onToolbarTap(FieldSafety.BLOCK_AI, loading = false))
        assertEquals(0, calls)
    }
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ExplicitAiTriggerControllerTest'
```
Expected: failure because toolbar AI only opens the panel.

- [ ] **Step 3: Change the toolbar action**

`KeyboardAction.OpenAiPanel` must:
1. refuse blocked fields;
2. show the AI result surface;
3. call `replyOrchestrator.requestAuto()` once;
4. never invoke a request from `renderSessionState`, `onStartInput`, or collectors.

Keep Smart/Unique/Flirty/Funny/Rewrite/Translate/Grammar routed through `ManualAiActionEngine.request()` only from their button listeners.

- [ ] **Step 4: Disable duplicate request UI while loading**

When `ReplyState.Loading` or `ManualAiState.Loading` is active, disable the initiating AI action until state leaves loading. Do not queue a second request.

- [ ] **Step 5: Run IME and AI tests**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ExplicitAiTriggerControllerTest' --tests '*ReplyOrchestratorTest' --tests '*ManualAiActionEngineTest'
```
Expected: PASS.

- [ ] **Step 6: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ime app/src/test/java/com/socialaiassistant/keyboard/ime
git commit -m "fix: trigger AI only from explicit keyboard actions"
```

### Task 4: Harden model-output parsing so raw JSON can never render

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ModelResultParser.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/ai/ModelResultParserTest.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt` only to map parse failure to a friendly state/message if needed.

**Interfaces:**
- Consumes: raw model text.
- Produces: `ParsedAiResult?`; JSON-looking invalid envelopes always return null, natural plain text remains supported.

- [ ] **Step 1: Add failing parser regression tests from the real screenshot shape**

````kotlin
@Test fun parses_uppercase_label_and_fenced_json() {
    val raw = """JSON
```JSON
{
  "reply": "হ্যাঁ, বলো কি এনে দিতে হবে।",
  "category": "general",
  "confidence": 1.0
}
```
"""
    assertEquals("হ্যাঁ, বলো কি এনে দিতে হবে।", parser.parse(raw)?.reply)
}

@Test fun malformed_json_looking_payload_is_never_plain_text_fallback() {
    assertNull(parser.parse("""JSON\n```json\n{\"reply\":\"hello\", bad}\n```"""))
    assertNull(parser.parse("""{\"reply\":\"hello\", bad}"""))
}

@Test fun plain_natural_language_still_works() {
    assertEquals("Sure, I can do that.", parser.parse("Reply: Sure, I can do that.")?.reply)
}
````

- [ ] **Step 2: Run parser tests and confirm RED**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ModelResultParserTest'
```
Expected: the prefixed uppercase/fenced case fails before parser changes.

- [ ] **Step 3: Normalize wrappers before JSON parsing**

Add a focused helper that:
- trims;
- strips one standalone leading `JSON`/`json` label;
- strips a single triple-backtick fence with optional case-insensitive language token;
- trims again.

After JSON parse fails, classify JSON-looking text using starts-with `{`/`[` or remaining fences/standalone `JSON`; return null instead of fallback.

- [ ] **Step 4: Ensure UI maps null parse to friendly error only**

`ReplyOrchestrator` and `ManualAiActionEngine` may show `AI returned an unreadable reply. Tap again to retry.` but must never put raw input into `Ready`.

- [ ] **Step 5: Run parser + AI engine tests**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ModelResultParserTest' --tests '*ReplyOrchestratorTest' --tests '*ManualAiActionEngineTest'
```
Expected: PASS.

- [ ] **Step 6: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai/ModelResultParser.kt app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt app/src/test/java/com/socialaiassistant/keyboard/ai/ModelResultParserTest.kt
git commit -m "fix: block raw model envelopes from keyboard UI"
```

### Task 5: Replace the plain consent notice with a premium actionable banner

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Add test: `app/src/test/java/com/socialaiassistant/keyboard/ime/ConsentBannerPolicyTest.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/ConsentBannerPolicy.kt`

**Interfaces:**
- Consumes: `AppSettings.aiPrivacyConsent`, context/accessibility consent, current safety.
- Produces: `ConsentBannerState.Hidden` or `ConsentBannerState.Required`; `Enable AI` opens `MainActivity` with `EXTRA_OPEN_SECTION = "ai_privacy"`.

- [ ] **Step 1: Write failing policy tests**

```kotlin
@Test fun missing_ai_privacy_shows_banner() {
    assertEquals(Required, policy.resolve(settings.copy(aiPrivacyConsent = false), FieldSafety.ALLOW_AI))
}

@Test fun full_consent_hides_banner() {
    assertEquals(Hidden, policy.resolve(fullConsentSettings, FieldSafety.ALLOW_AI))
}
```

- [ ] **Step 2: Run and confirm RED**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ConsentBannerPolicyTest'
```

- [ ] **Step 3: Add the compact Neon/Glass banner renderer**

The AI panel must show title `AI Features Locked`, one short localized description, and `Enable AI`. The action launches:

```kotlin
Intent(this, MainActivity::class.java)
    .putExtra(MainActivity.EXTRA_OPEN_SECTION, MainActivity.SECTION_AI_PRIVACY)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
```

Do not bypass consent. Once settings flow emits consent=true, rerender the panel and remove the banner immediately.

- [ ] **Step 4: Run policy and IME tests**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ConsentBannerPolicyTest' --tests '*ExplicitAiTriggerControllerTest'
```

- [ ] **Step 5: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ime app/src/main/res/values/strings.xml app/src/test/java/com/socialaiassistant/keyboard/ime
git commit -m "feat: add premium AI consent banner"
```

### Task 6: Redesign MainActivity as the Premium Neon Settings Hub

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify if needed: `app/src/main/res/values-night/themes.xml`
- Keep/reuse: `ThemeSettingsActivity.kt`, `PrivacyActivity.kt`, `ThemeRepository.kt`, `SettingsRepository.kt`
- Add: `app/src/test/java/com/socialaiassistant/keyboard/SettingsHubActivityTest.kt`

**Interfaces:**
- Consumes: existing `SettingsRepository`, `ThemeRepository`, `ManagedSessionStore`, `SocialAiBackendClient`.
- Produces: six top-level cards/sections: AI & Privacy, Theme & Appearance, Keyboard Preferences, Language & Input, Account & Subscription, Help & Support; supports deep-link extra `ai_privacy`.

- [ ] **Step 1: Add failing Robolectric settings-hub tests**

Tests must prove:
- all six section cards exist;
- `EXTRA_OPEN_SECTION=ai_privacy` scrolls/focuses AI & Privacy;
- toggling AI privacy/preserve draft persists through `SettingsRepository`;
- tapping Theme opens existing Theme Settings without resetting theme state;
- no media/storage permission is requested by the settings screen.

- [ ] **Step 2: Run focused tests and confirm RED**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*SettingsHubActivityTest'
```

- [ ] **Step 3: Replace the long raw form layout with card sections**

Use the locked deep-navy/glass/neon visual tokens. Keep large tap targets and short labels. Do not duplicate theme editor internals: `Theme & Appearance` launches `ThemeSettingsActivity` and shows a current-theme preview/status.

- [ ] **Step 4: Preserve all existing settings/actions**

Move, do not remove:
- keyboard enable/chooser;
- AI privacy consent and Preserve Draft;
- accessibility disclosure/context access;
- tone/custom instruction/personal training;
- managed login/logout/account refresh/user portal;
- personal OpenRouter controls;
- caption entry;
- WhatsApp support;
- privacy/local-history action.

Hide advanced Personal OpenRouter controls under an `Advanced AI` expander/card so ordinary users do not face technical setup by default.

- [ ] **Step 5: Persist optional keyboard preference fields only through DataStore**

If key-size/font-size/sound/vibration toggles are introduced, add explicit keys and defaults to `SettingsRepository`; do not change existing preference defaults. If a preference cannot be wired safely in this release, omit it rather than showing a dead control.

- [ ] **Step 6: Run Settings + Theme regression tests**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*SettingsHubActivityTest' --tests '*SettingsRepositoryTest' --tests '*ThemeRepositoryTest' --tests '*ThemeSettingsActivityTest' --tests '*ImeThemeIntegrationTest'
```
Expected: PASS.

- [ ] **Step 7: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt app/src/main/res/layout/activity_main.xml app/src/main/res/values app/src/test/java/com/socialaiassistant/keyboard
git commit -m "feat: redesign keyboard settings as premium neon hub"
```

### Task 7: Add Keyboard product identity and entitlement-aware account state

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/backend/SocialAiBackendClient.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/backend/BackendModels.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/backend/BackendErrorMessages.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Add: `app/src/test/java/com/socialaiassistant/keyboard/backend/ProductEntitlementClientTest.kt`

**Interfaces:**
- Produces request header `X-SocialAI-Product: KEYBOARD` on `/api/v1/auth/me`, `/api/v1/usage`, `/api/v1/ai/generate`, and other product-sensitive authenticated requests.
- Parses `entitlements` array into `ProductEntitlement(productCode, status, cycleExpiresAt)` and exposes `ManagedAccountState.keyboardEntitlement` / `assistantProEntitlement`.

- [ ] **Step 1: Write failing MockWebServer tests**

```kotlin
@Test fun managed_ai_request_identifies_keyboard_product() = runTest {
    client.generate(testPayload())
    assertEquals("KEYBOARD", server.takeRequest().getHeader("X-SocialAI-Product"))
}

@Test fun account_state_parses_independent_entitlements() = runTest {
    enqueueMe(entitlements = listOf(
        entitlement("KEYBOARD", "ACTIVE", "2026-10-19T00:00:00Z"),
        entitlement("ASSISTANT_PRO", "EXPIRED", "2026-09-01T00:00:00Z")
    ))
    val state = client.accountState()
    assertEquals("ACTIVE", state.keyboardEntitlement.status)
    assertEquals("EXPIRED", state.assistantProEntitlement.status)
}

private fun entitlement(code: String, status: String, expiry: String?) = buildJsonObject {
    put("productCode", code)
    put("status", status)
    expiry?.let { put("cycle_expires_at", it) }
}
```

- [ ] **Step 2: Run focused tests and confirm RED**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ProductEntitlementClientTest'
```

- [ ] **Step 3: Add normalized product model and headers**

```kotlin
data class ProductEntitlement(
    val productCode: String,
    val status: String,
    val cycleExpiresAt: String? = null
)
```

Use one constant:

```kotlin
private const val PRODUCT_HEADER = "X-SocialAI-Product"
private const val PRODUCT_KEYBOARD = "KEYBOARD"
```

Attach it to product-sensitive requests without changing device-proof signing material, because the signature remains method/path/timestamp/requestId/bodyText.

- [ ] **Step 4: Show product-specific account status in Settings**

Account & Subscription must display `Social AI Keyboard` status/expiry prominently and `AI Assistant Pro` status/expiry separately. Map backend `PRODUCT_ACCESS_REQUIRED` to a clear user message instead of generic subscription failure.

- [ ] **Step 5: Run backend-client and account regression tests**

```bash
./gradlew --no-daemon testDebugUnitTest --tests '*ProductEntitlementClientTest' --tests '*OpenRouterGatewayTest' --tests '*AppBootstrapTest'
```

- [ ] **Step 6: Commit the task**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/backend app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt app/src/test/java/com/socialaiassistant/keyboard/backend
git commit -m "feat: identify keyboard product entitlement"
```

### Task 8: Full keyboard regression and release verification

**Files:**
- Modify verification docs only if results require updates: `THEME_ENGINE_VERIFICATION.md`, `docs/port/VERIFICATION_REPORT_2026-09-18.md`
- No feature code changes unless a failing regression test requires a focused fix and new test.

**Interfaces:**
- Validates all prior tasks together.

- [ ] **Step 1: Run the full unit suite**

```bash
./gradlew --no-daemon testDebugUnitTest
```
Expected: PASS.

- [ ] **Step 2: Run static/release verification**

```bash
python3 scripts/verify_release_ready.py
```
Expected: PASS.

- [ ] **Step 3: Run lint and APK build**

```bash
./gradlew --no-daemon lintDebug assembleDebug
```
Expected: PASS and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Run manual device checks on WhatsApp/Messenger**

Verify exactly:
1. opening a chat causes zero AI generation;
2. incoming message causes zero AI generation;
3. toolbar AI tap starts one request;
4. result displays only reply text, never JSON;
5. sending/inserting a result causes no new request;
6. a second AI tap generates the next result;
7. consent banner disappears after enabling consent;
8. settings cards and theme/background settings remain usable.

- [ ] **Step 5: Commit verification documentation**

```bash
git add THEME_ENGINE_VERIFICATION.md docs/port/VERIFICATION_REPORT_2026-09-18.md
git commit -m "test: verify explicit AI and settings update"
```
