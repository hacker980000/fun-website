# Context Adapters and AI Preferences Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden Facebook, Messenger, WhatsApp, Instagram and Telegram context detection while adding reusable tone presets and a saved custom AI instruction without changing the user-controlled insert/send boundary.

**Architecture:** Add a pure-Kotlin adapter registry that classifies supported package/window snapshots into COMMENT or INBOX, filters platform UI noise, improves conversation identity, and falls back to the existing generic adapter. Extend AI requests with a tone preset and saved custom instruction; the keyboard AI panel exposes tone cycling while the settings activity owns custom-instruction editing. Accessibility remains read-only and emits normalized snapshots only.

**Tech Stack:** Kotlin/JVM, Android `AccessibilityService` + IME, DataStore Preferences, Room/AES-GCM existing memory layer, existing OpenRouter gateway, JUnit/pure-Kotlin smoke checks where Gradle is unavailable.

**Spec:** `docs/superpowers/specs/2026-09-13-social-ai-keyboard-design.md`

## Global Constraints

- Package remains `com.socialaiassistant.keyboard`; app name remains `Social AI Keyboard`.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Normal typing remains offline and must never wait on network/database work.
- Sensitive/password/PIN/OTP/payment fields must block AI capture and AI suggestions.
- Accessibility is read-only for context extraction; it must never click or send.
- Generated text is inserted only after explicit user tap; final Send remains user-controlled.
- Local conversation history remains local-only and encrypted.
- Existing extension language matching and Smart/Witty/Flirty behavior must remain intact.
- Full Gradle verification may be unavailable in this sandbox because `services.gradle.org` is DNS-blocked; every pure-Kotlin unit introduced here must be RED→GREEN verified with `kotlinc`, and XML/source invariants must be checked before packaging.

---

### Task 1: Platform Context Classification and Adapter Registry

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/ConversationSurface.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/PlatformContextAdapter.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/PlatformContextAdapterRegistry.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/SupportedPlatformAdapters.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/ContextModels.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/PlatformContextAdapterRegistryTest.kt`

**Interfaces:**
- Consumes: `VisibleTextNode`, package name, conversation/composer hints.
- Produces: `ConversationSurface` (`INBOX`, `COMMENT`, `GENERAL`) and `PlatformContextResult(surface, conversationHint, nodes, confidenceBoost)`.

- [ ] **Step 1: Write failing tests** for package recognition, Facebook comment-vs-message hints, WhatsApp/Messenger/Instagram/Telegram inbox classification, UI-noise filtering, and generic fallback.
- [ ] **Step 2: Run pure-Kotlin compile/test and confirm RED** because the adapter registry/types do not exist.
- [ ] **Step 3: Implement minimal adapter interface/registry and five platform adapters** using package names plus normalized composer/window labels; keep selectors/heuristics declarative and side-effect-free.
- [ ] **Step 4: Run the pure-Kotlin tests and confirm GREEN.**
- [ ] **Step 5: Commit** `feat: add multi-app context adapter registry`.

### Task 2: Accessibility Metadata and Snapshot Wiring

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/ContextModels.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/GenericConversationAdapter.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/ConversationKeyFactory.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/ConversationKeyFactoryTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/GenericConversationAdapterTest.kt`

**Interfaces:**
- Consumes: normalized node metadata from Accessibility (`text`, `contentDescription`, `viewIdResourceName`, class name, sender position).
- Produces: `ContextSnapshot.surface`, stronger `conversationHint`, platform-filtered messages and more stable conversation key material.

- [ ] **Step 1: Write failing tests** proving different conversation hints produce different keys, surface is preserved, and common toolbar/status noise is removed without dropping real messages.
- [ ] **Step 2: Run the pure-Kotlin tests and confirm RED** for the new snapshot/node fields.
- [ ] **Step 3: Extend node/snapshot models and service extraction**, then route through `PlatformContextAdapterRegistry` before `GenericConversationAdapter` builds the snapshot.
- [ ] **Step 4: Run pure-Kotlin tests and XML parse checks; confirm GREEN.**
- [ ] **Step 5: Commit** `feat: wire platform adapters into accessibility context`.

### Task 3: Tone Presets and Saved Custom AI Instruction

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/TonePreset.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/AiModels.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiAction.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiPromptBuilder.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/TonePresetTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/ManualAiPromptBuilderTest.kt`

**Interfaces:**
- Consumes: `TonePreset` and `customInstruction` from settings/request.
- Produces: prompt rules that preserve extension language policy while applying a bounded user tone/instruction.

- [ ] **Step 1: Write failing pure-Kotlin tests** for tone labels/instructions, prompt inclusion, trimming/length bounds and refusal to let custom text replace security/language rules.
- [ ] **Step 2: Confirm RED** because tone/custom-instruction support is absent.
- [ ] **Step 3: Implement tone presets (`AUTO`, `FRIENDLY`, `PROFESSIONAL`, `CASUAL`, `CONCISE`, `WARM`) and a max-1200-character saved custom instruction.**
- [ ] **Step 4: Wire settings into manual and automatic prompt builders without changing model routing or send behavior.**
- [ ] **Step 5: Run pure-Kotlin tests and confirm GREEN.**
- [ ] **Step 6: Commit** `feat: add tone presets and custom AI instruction`.

### Task 4: Keyboard/Settings UI Integration and Interaction Detection

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/AiPanelPolicy.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/AiPanelPolicyTest.kt`

**Interfaces:**
- Consumes: `ContextSnapshot.surface`, settings tone/custom instruction.
- Produces: correct Smart Reply vs Smart Comment labels, tone-cycle button, custom-instruction save/clear controls.

- [ ] **Step 1: Write failing policy tests** for `COMMENT`/`INBOX` labels and deterministic tone cycling.
- [ ] **Step 2: Confirm RED.**
- [ ] **Step 3: Replace composer-string-only comment detection with `ContextSnapshot.surface`, add a tone-cycle control to the AI panel, and add custom-instruction save/clear UI to the app settings screen.**
- [ ] **Step 4: Run policy tests, parse all changed XML, and grep that no auto-send/click action was introduced.**
- [ ] **Step 5: Commit** `feat: expose context-aware AI preferences in keyboard`.

### Task 5: Verification, Device Checklist and Source Package

**Files:**
- Modify: `README.md`
- Create: `docs/testing/multi-app-context-adapters-checklist.md`

**Interfaces:**
- Consumes: all Task 1-4 outputs.
- Produces: reproducible QA checklist and packaged source ZIP.

- [ ] **Step 1: Attempt** `./gradlew test assembleDebug --no-daemon`; record the exact result without claiming Android build success if Gradle remains network-blocked.
- [ ] **Step 2: Run all Phase 2C pure-Kotlin smoke suites fresh**, plus XML parsing, no-plaintext-secret scan, no-autonomous-send/click scan, and `git diff --check`.
- [ ] **Step 3: Update README and write a manual device matrix** covering Facebook comments, Messenger, WhatsApp, Instagram DM/comments, Telegram, unknown-chat fallback, sensitive fields, custom instruction, tone presets and tap-to-insert.
- [ ] **Step 4: Inspect branch diff against baseline and package the complete source tree into `/mnt/data/Social_AI_Keyboard_Phase2C_Source_2026-09-14.zip`.**
- [ ] **Step 5: Commit** `docs: add phase 2c verification checklist`.
