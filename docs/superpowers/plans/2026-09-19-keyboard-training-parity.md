# Keyboard Training Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Social AI Keyboard Personal OpenRouter mode the same compact Bengali/Banglish flirty style, anti-repeat, and boundary behavior as the protected server library without embedding the 500-conversation corpus or changing explicit AI-trigger behavior.

**Architecture:** Keep Managed AI unchanged so it reads the shared Worker/D1 library. Extend only the current `ExtensionPromptBuilder`/manual prompt path with compact rules distilled from Packs 0010/0011, and add static verification proving the corpus is not bundled in the APK source/resources.

**Tech Stack:** Android/Kotlin, JUnit/Robolectric where already used, Gradle Kotlin DSL, existing release verifier scripts.

**Spec:** `docs/superpowers/specs/2026-09-19-protected-training-library-design.md`

## Global Constraints

- Do not embed the 500-conversation JSON, Markdown corpus, or generated 500-example module into `app/`.
- Managed AI continues to use `/api/v1/ai/generate` and the shared server-side protected training library.
- Personal OpenRouter receives compact style parity only.
- Existing explicit AI-button generation, one-click/one-generation behavior, REPLY/CONTINUE/START intent, privacy, product entitlement, and manual-send rules must remain unchanged.
- Bengali context stays Bengali; Banglish context stays Banglish; English context stays English where current logic selects English.
- Boundary/de-escalation signals immediately stop flirt escalation.
- No destructive changes to existing user settings or theme data.

## Review Focus

1. Banglish input must remain Latin-script Banglish instead of being converted to Bengali script; Task 1 tests this through the actual prompt builder.
2. Boundary text such as stop, uncomfortable, friend-only, busy, or talk-later must produce a prompt that explicitly de-escalates; Task 1 pins this policy.
3. REPLY/CONTINUE/START rules must remain present after adding new style text; Task 1 includes regression assertions.
4. The full 500-conversation corpus must not be bundled anywhere under `app/src/main` or Android resources/assets; Task 2 scans file paths and content markers.
5. Managed AI payload/product identity must not change while Personal mode prompt changes; Task 2 runs existing managed payload/entitlement tests and static verification.

---

### Task 1: Add compact FLIRT_MSG style and boundary parity to Personal OpenRouter prompts

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/ai/ManualAiPromptBuilderTest.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/ai/ExtensionLanguageLogicTest.kt`

**Interfaces:**
- Consumes: existing `PromptRequest`, `AiMode.FLIRT_MSG`, `ConversationAiIntent`, and `ExtensionLanguageLogic` output.
- Produces: the same existing `PromptBundle` API with additional compact style/boundary/anti-copy instructions for Personal OpenRouter mode.

- [ ] **Step 1: Add failing prompt regression tests**

Add assertions to the real prompt builder tests:

```kotlin
@Test
fun flirty_inbox_prompt_contains_compact_video_style_without_copying_examples() {
    val bundle = builder.build(inboxRequest(
        mode = AiMode.FLIRT_MSG,
        recipient = "tumi ajke eto chup keno",
        intent = ConversationAiIntent.REPLY
    ))
    val system = bundle.system.lowercase()
    assertTrue(system.contains("context") && system.contains("playful"))
    assertTrue(system.contains("soft flirt") || system.contains("wordplay"))
    assertTrue(system.contains("continuation"))
    assertTrue(system.contains("do not copy") || system.contains("never copy"))
    assertTrue(system.contains("opener") && system.contains("punchline"))
}

@Test
fun flirty_inbox_prompt_deescalates_on_boundary_signals() {
    val bundle = builder.build(inboxRequest(
        mode = AiMode.FLIRT_MSG,
        recipient = "please normal kotha bolo, flirt korona",
        intent = ConversationAiIntent.REPLY
    ))
    val system = bundle.system.lowercase()
    assertTrue(system.contains("stop") || system.contains("disinterest"))
    assertTrue(system.contains("friend-only") || system.contains("normal chat"))
    assertTrue(system.contains("busy") || system.contains("reply-later"))
    assertTrue(system.contains("de-escalate") || system.contains("no further romantic"))
}
```

Add/retain language assertions proving Bengali recipient text gets Bengali-script policy and Banglish recipient text gets Banglish Latin-script policy. Retain REPLY/CONTINUE/START assertions.

- [ ] **Step 2: Run the focused tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*ManualAiPromptBuilderTest' --tests '*ExtensionLanguageLogicTest'`
Expected: new style/boundary assertions FAIL while existing intent/language tests remain meaningful.

If Gradle distribution download is unavailable in the execution environment, run the repository's existing pure-Kotlin/static test path and record the Gradle network limitation; do not claim Android Gradle success without a real green Gradle run.

- [ ] **Step 3: Implement the minimal prompt text change**

Within `ExtensionPromptBuilder.buildInboxRules`, extend only the `AiMode.FLIRT_MSG` branch with compact policy text equivalent to:

```text
FLIRT_MSG STYLE:
- Start from the active context, then use a playful observation/twist or wordplay, then a soft flirt only if it fits, then a natural continuation hook only when useful.
- Examples and learned patterns are style demonstrations, never lines to copy verbatim.
- Avoid repeating an opener, metaphor, emoji pattern, punchline, or recent response already used in this conversation.
- If the other person rejects flirting, asks to stop, asks for normal/friend-only chat, is uncomfortable, is busy, says reply later, or closes the conversation, immediately de-escalate to respectful normal chat and do not continue romantic escalation.
```

Do not alter `ConversationAiIntent` logic or explicit-trigger code.

- [ ] **Step 4: Re-run focused tests**

Run: `./gradlew testDebugUnitTest --tests '*ManualAiPromptBuilderTest' --tests '*ExtensionLanguageLogicTest'`
Expected: PASS.

- [ ] **Step 5: Run existing intent/explicit-trigger regression tests**

Run: `./gradlew testDebugUnitTest --tests '*ConversationAiIntentResolverTest' --tests '*ExplicitAiTriggerControllerTest' --tests '*ManualAiActionEngineTest' --tests '*ReplyOrchestratorTest'`
Expected: PASS; no background generation behavior returns.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt app/src/test/java/com/socialaiassistant/keyboard/ai/ManualAiPromptBuilderTest.kt app/src/test/java/com/socialaiassistant/keyboard/ai/ExtensionLanguageLogicTest.kt
git commit -m "feat: align keyboard personal flirt prompt policy"
```

---

### Task 2: Prove the 500-conversation corpus is not bundled and Managed AI contract is unchanged

**Files:**
- Create: `scripts/verify_training_parity.py`
- Modify: `scripts/verify_release_ready.py`
- Test: `scripts/verify_training_parity.py` itself plus existing Kotlin tests.

**Interfaces:**
- Consumes: Android source tree and current Managed AI payload classes.
- Produces: static verifier that fails if corpus files/markers are embedded or if required compact-policy markers are missing.

- [ ] **Step 1: Create a failing static verifier first**

The script must inspect `app/src/main` and fail if any of these are true:

```python
forbidden_names = {
    'flirty_conversations_500.json',
    'flirty_conversations_500.md',
    'flirty-conversation-expansion-pack-0011.js'
}
forbidden_markers = ['flirt-conv-0011-bl-', 'flirt-conv-0011-bn-']
required_prompt_markers = ['soft flirt', 'continuation', 'normal chat']
```

It must also assert `ManagedAiPayload.kt` still emits `conversationIntent` and current product identity logic remains in the backend client/payload path.

- [ ] **Step 2: Run the new verifier before adding it to release verification**

Run: `python3 scripts/verify_training_parity.py`
Expected: initially FAIL because required compact policy markers are not all detectable or because the script is not yet integrated with release verification.

- [ ] **Step 3: Complete verifier and wire it into `verify_release_ready.py`**

`verify_release_ready.py` must invoke/import the parity checks so a release cannot pass if the corpus is bundled or compact policy is absent. Do not add the corpus under any Android resource directory.

- [ ] **Step 4: Run static verification**

Run: `python3 scripts/verify_training_parity.py && python3 scripts/verify_release_ready.py`
Expected: PASS; no 500-conversation corpus path or marker exists under `app/src/main`.

- [ ] **Step 5: Run Managed AI entitlement/payload tests**

Run: `./gradlew testDebugUnitTest --tests '*ProductEntitlementClientTest' --tests '*ManualAiActionEngineTest' --tests '*ConversationAiIntentResolverTest'`
Expected: PASS and Managed AI behavior unchanged.

- [ ] **Step 6: Commit**

```bash
git add scripts/verify_training_parity.py scripts/verify_release_ready.py
git commit -m "test: guard keyboard training parity and apk size"
```

---

### Task 3: Final Keyboard regression gate

**Files:**
- Modify only if RED-GREEN verification exposes a defect: files owned by Tasks 1-2.

**Interfaces:**
- Consumes: completed Keyboard changes.
- Produces: verified source tree ready for release packaging.

- [ ] **Step 1: Run all available unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: zero test failures.

- [ ] **Step 2: Run lint and debug assembly when SDK/network are available**

Run: `./gradlew lintDebug assembleDebug`
Expected: BUILD SUCCESSFUL.

If the environment cannot download Gradle or lacks Android SDK components, record the exact failure as an environment limitation and run all repository static/pure tests instead. Do not convert an environment-limited run into a build-success claim.

- [ ] **Step 3: Run release/static verifiers fresh**

Run: `python3 scripts/verify_training_parity.py && python3 scripts/verify_release_ready.py`
Expected: PASS.

- [ ] **Step 4: Confirm no corpus was added**

Run: `find app/src/main -type f | grep -E 'flirty_conversations_500|flirty-conversation-expansion-pack-0011' && exit 1 || exit 0`
Expected: exit 0 with no matching files.

- [ ] **Step 5: Record exact verification output for the release packaging plan**

Keep the test counts, static-verifier result, and any Gradle environment limitation in the release report source notes.
