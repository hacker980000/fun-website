# Full AI Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Phase 2A AI placeholder with a working keyboard AI panel that supports Smart, Witty, Flirty, Rewrite, Translate, Grammar Fix, Regenerate, and safe tap-to-insert behavior using the existing extension-port AI stack.

**Architecture:** Keep automatic Best Reply in `ReplyOrchestrator` unchanged. Add a separate manual-action engine that receives the current context snapshot, local conversation history, current editor draft, selected action, settings, and existing `AiGateway`; it produces one manual result state. The IME renders that state in the AI panel and only inserts text after an explicit tap, preserving the existing no-auto-send boundary.

**Tech Stack:** Kotlin 2.3.21, coroutines/StateFlow, Android IME/InputConnection, existing OpenRouter gateway and prompt/result parser, JUnit 4/Robolectric when Gradle is available, plus standalone `kotlinc` smoke verification for pure Kotlin logic in this sandbox.

**Spec:** `docs/superpowers/specs/2026-09-13-social-ai-keyboard-design.md`

## Global Constraints

- Package remains `com.socialaiassistant.keyboard`.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Normal typing must never wait on AI/network work.
- Sensitive fields must disable AI actions and context capture.
- Final insertion requires a user tap; final Send always remains user-controlled.
- Existing extension reply modes remain behaviorally mapped: GENERAL, WITTY, FLIRT_MSG, FLIRT_CMT.
- Existing Personal OpenRouter gateway and model fallback are reused; Managed AI remains disabled in this phase.
- Manual Rewrite/Translate/Grammar actions operate on current draft text only and must never silently replace text.
- Manual Smart/Witty/Flirty actions use conversation context plus local history and match comment-vs-inbox behavior.

---

### Task 1: Manual AI action model and prompt builder

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiAction.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiPromptBuilder.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/ManualAiPromptBuilderTest.kt`

**Interfaces:**
- Produces `ManualAiAction`, `ManualAiRequest`, and `ManualAiPromptBuilder.build(request): PromptBundle`.
- Social actions map to the extension modes. Draft actions emit a security-hardened single-task prompt and JSON reply contract.

- [ ] **Step 1: Write failing tests** for Smart/Witty/Flirty mode mapping, comment-vs-inbox Flirty mapping, Rewrite requiring draft text, Translate preserving meaning and language target, and Grammar Fix preserving meaning/tone.
- [ ] **Step 2: Run standalone Kotlin compile/test harness** and confirm failure because the new production types do not exist.
- [ ] **Step 3: Implement the minimal action model and prompt builder** without Android dependencies.
- [ ] **Step 4: Re-run the standalone harness** and confirm all prompt/action assertions pass.
- [ ] **Step 5: Commit** with message `feat: add manual AI action prompts`.

### Task 2: Manual AI action engine

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngineTest.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`

**Interfaces:**
- Consumes current `ContextSnapshot?`, `ConversationHistory?`, `AppSettings`, API-key availability, draft text, `AiGateway`, and `ModelResultParser`.
- Produces `StateFlow<ManualAiState>` and `request(action, draftText)` / `regenerate()` methods.

- [ ] **Step 1: Write failing tests** for missing API key, sensitive/no-context rejection, rewrite-empty-draft rejection, successful social action, successful draft action, and stale-request cancellation.
- [ ] **Step 2: Run the pure Kotlin engine harness** and confirm expected missing-type failures.
- [ ] **Step 3: Implement the engine** so each manual request cancels the prior request, uses the existing gateway/model mode, and converts gateway failures to stable user-facing state.
- [ ] **Step 4: Wire the engine in `SocialAiApplication`** using `ContextSnapshotBus.snapshots.value`, `ConversationRepository.historyFor`, existing settings, gateway, and parser.
- [ ] **Step 5: Re-run pure Kotlin smoke verification** for the engine logic where Android-free dependencies allow it.
- [ ] **Step 6: Commit** with message `feat: add manual AI action engine`.

### Task 3: Working AI keyboard panel

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/AiPanelPolicyTest.kt`

**Interfaces:**
- AI panel buttons: Smart, Witty/Unique, Flirty, Rewrite, Translate, Grammar Fix, Regenerate.
- Translate target follows keyboard language: Bangla keyboard -> Bengali, English keyboard -> English.
- Draft actions read text around cursor; social actions use context/history.
- Result card offers Insert; if a draft already exists, existing `ReplyInserter` draft-preservation choices remain authoritative.

- [ ] **Step 1: Write failing policy tests** for surface-aware Flirty mode and translate target selection.
- [ ] **Step 2: Implement AI panel rendering** with action buttons, status/result view, Regenerate, and return-to-keyboard control.
- [ ] **Step 3: Collect `ManualAiActionEngine.state`** in the IME and refresh the AI panel without blocking typing.
- [ ] **Step 4: Implement explicit result insertion** through existing `insertGeneratedReply` / `ReplyInserter`; never auto-insert or auto-send.
- [ ] **Step 5: Verify sensitive fields keep the AI toolbar disabled and prevent panel requests.**
- [ ] **Step 6: Commit** with message `feat: build full keyboard AI panel`.

### Task 4: Verification and handoff package

**Files:**
- Create: `docs/testing/full-ai-panel-checklist.md`
- Modify: `README.md`

**Interfaces:**
- Documents manual tests for Inbox, Comment, rewrite, translation, grammar, draft preservation, offline/error states, sensitive-field blocking, and Send boundary.

- [ ] **Step 1: Run `./gradlew test` and `./gradlew assembleDebug`**; if Gradle distribution remains network-blocked, capture the exact blocker rather than claiming an APK build.
- [ ] **Step 2: Run standalone Kotlin smoke checks** for prompt mapping and panel policy.
- [ ] **Step 3: Run XML parsing and source invariant checks** for AI controls, no autonomous send, and no plaintext API key.
- [ ] **Step 4: Update README and testing checklist** with Phase 2B behavior and limitations.
- [ ] **Step 5: Commit** with message `docs: add AI panel verification checklist`.
- [ ] **Step 6: Package the complete source tree** as `Social_AI_Keyboard_Phase2B_Source_2026-09-13.zip`.
