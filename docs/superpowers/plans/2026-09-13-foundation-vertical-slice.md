# Foundation Vertical Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Android release slice of Social AI Keyboard: a usable IME that stays responsive offline, blocks sensitive fields, reads normalized conversation context through Accessibility, stores encrypted local history, ports the extension's core language/prompt behavior, calls OpenRouter with a personal API key, auto-generates one Best Smart Reply, and inserts it only after an explicit user tap.

**Architecture:** The Android app is split into focused packages: `ime` owns only keyboard/editor interaction, `context` owns Accessibility snapshot extraction, `safety` owns sensitive-field decisions, `memory` owns encrypted Room history, `ai` owns ported extension intelligence and gateway routing, and `settings` owns local configuration/secrets. A `ReplyOrchestrator` joins these layers asynchronously so network/database work never blocks key dispatch. The original extension remains unchanged under `legacy-extension/` and is used as the behavioral reference.

**Tech Stack:** Kotlin 2.3.21, Android SDK 36, minSdk 26, Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, AndroidX Core/AppCompat/Lifecycle, Room, DataStore Preferences, Kotlin Coroutines/Flow, OkHttp, kotlinx.serialization, Android Keystore AES-GCM, JUnit 4, Robolectric, AndroidX Test, MockWebServer.

**Spec:** `docs/superpowers/specs/2026-09-13-social-ai-keyboard-design.md`

## Global Constraints

- Android application package: `com.socialaiassistant.keyboard`.
- App name: `Social AI Keyboard`.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- JDK 17, Android Gradle Plugin 9.4.0, Gradle 9.6.0, Kotlin 2.3.21.
- Normal typing must remain functional offline and must never wait on an AI/network operation.
- Password, PIN, OTP, payment/card, banking-sensitive and secure system fields must disable AI context capture, AI suggestions, and conversation persistence.
- Accessibility is used only for context-aware reply assistance, with prominent disclosure and affirmative consent.
- No autonomous Send/click behavior.
- Conversation history is local-only; message bodies must never be persisted as plaintext.
- Personal OpenRouter keys must be Keystore-protected and must never be logged.
- Existing non-empty drafts must never be overwritten by automatic reply generation.
- One in-flight AI generation per conversation; stale work must be cancelled or ignored.

---

## Planned File Structure

```text
social_ai_keyboard_project/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/socialaiassistant/keyboard/
│       │   │   ├── SocialAiApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── ai/
│       │   │   │   ├── AiMode.kt
│       │   │   │   ├── AiModels.kt
│       │   │   │   ├── AiSettingsRepository.kt
│       │   │   │   ├── ExtensionLanguageLogic.kt
│       │   │   │   ├── ExtensionPromptBuilder.kt
│       │   │   │   ├── ModelResultParser.kt
│       │   │   │   ├── OpenRouterGateway.kt
│       │   │   │   ├── OpenRouterModels.kt
│       │   │   │   ├── ReplyOrchestrator.kt
│       │   │   │   └── ReplyState.kt
│       │   │   ├── context/
│       │   │   │   ├── ContextModels.kt
│       │   │   │   ├── ContextSnapshotBus.kt
│       │   │   │   ├── GenericConversationAdapter.kt
│       │   │   │   ├── SocialAiAccessibilityService.kt
│       │   │   │   └── ConversationKeyFactory.kt
│       │   │   ├── crypto/
│       │   │   │   ├── AesGcmCipher.kt
│       │   │   │   └── SecretStore.kt
│       │   │   ├── ime/
│       │   │   │   ├── KeyboardAction.kt
│       │   │   │   ├── KeyboardLayout.kt
│       │   │   │   ├── SocialAiInputMethodService.kt
│       │   │   │   └── SmartReplyController.kt
│       │   │   ├── memory/
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── ConversationDao.kt
│       │   │   │   ├── ConversationEntities.kt
│       │   │   │   └── ConversationRepository.kt
│       │   │   ├── safety/
│       │   │   │   ├── FieldSafety.kt
│       │   │   │   └── SensitiveFieldPolicy.kt
│       │   │   └── settings/
│       │   │       ├── SettingsRepository.kt
│       │   │       └── SetupState.kt
│       │   ├── res/
│       │   │   ├── drawable/
│       │   │   ├── layout/activity_main.xml
│       │   │   ├── layout/ime_keyboard.xml
│       │   │   ├── values/strings.xml
│       │   │   ├── values/themes.xml
│       │   │   └── xml/
│       │   │       ├── accessibility_service_config.xml
│       │   │       └── method.xml
│       │   └── res/values-night/themes.xml
│       ├── test/java/com/socialaiassistant/keyboard/
│       │   ├── ai/ExtensionLanguageLogicTest.kt
│       │   ├── ai/ModelResultParserTest.kt
│       │   ├── ai/OpenRouterGatewayTest.kt
│       │   ├── context/ConversationKeyFactoryTest.kt
│       │   ├── context/GenericConversationAdapterTest.kt
│       │   ├── crypto/AesGcmCipherTest.kt
│       │   ├── memory/ConversationRepositoryTest.kt
│       │   └── safety/SensitiveFieldPolicyTest.kt
│       └── androidTest/java/com/socialaiassistant/keyboard/ime/
│           └── ImeInsertionInstrumentedTest.kt
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── settings.gradle.kts
└── gradle/wrapper/
```

The file split is intentional: the IME never traverses Accessibility trees, Accessibility never calls the network, the database never knows about `InputConnection`, and the AI layer never receives raw Android node objects.

---

### Task 1: Android Project Bootstrap + Runnable Setup Screen

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values-night/themes.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/AppBootstrapTest.kt`

**Interfaces:**
- Produces: installable package `com.socialaiassistant.keyboard`, `SocialAiApplication`, and `MainActivity` used by all later tasks.

- [ ] **Step 1: Write the failing bootstrap test**

```kotlin
@RunWith(RobolectricTestRunner::class)
class AppBootstrapTest {
    @Test fun app_label_is_social_ai_keyboard() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val label = app.applicationInfo.loadLabel(app.packageManager).toString()
        assertEquals("Social AI Keyboard", label)
    }
}
```

- [ ] **Step 2: Add Gradle/version configuration and run the test before implementation**

Run: `./gradlew testDebugUnitTest --tests '*AppBootstrapTest'`

Expected: FAIL because the Android module/resources do not yet exist.

- [ ] **Step 3: Create the minimal Android app**

Use namespace/application id `com.socialaiassistant.keyboard`, SDK levels from Global Constraints, JDK 17, and dependencies for AndroidX, Lifecycle, Room, DataStore, coroutines, OkHttp, serialization, Robolectric, AndroidX Test, and MockWebServer. `MainActivity` must render a setup status screen with four visible rows: Keyboard enabled, Current keyboard selected, AI Context Access, Personal API configured. At this task the rows may show status only; deep links are added later.

- [ ] **Step 4: Generate/use Gradle wrapper 9.6.0 and verify compilation**

Run: `./gradlew clean testDebugUnitTest assembleDebug`

Expected: tests PASS and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle app
 git commit -m "build: bootstrap Social AI Keyboard Android app"
```

---

### Task 2: Sensitive-Field Policy Before Any AI Integration

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/safety/FieldSafety.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicy.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicyTest.kt`

**Interfaces:**
- Consumes: Android `EditorInfo`, optional accessibility metadata represented by plain booleans/strings.
- Produces: `enum class FieldSafety { ALLOW_AI, BLOCK_AI, NO_CONVERSATION }` and `SensitiveFieldPolicy.evaluate(EditorDescriptor): FieldSafety`.

- [ ] **Step 1: Write failing policy tests**

```kotlin
class SensitiveFieldPolicyTest {
    private val policy = SensitiveFieldPolicy()

    @Test fun password_blocks_ai() {
        val d = EditorDescriptor(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            packageName = "com.example.app",
            hintText = "Password",
            accessibilityPassword = true
        )
        assertEquals(FieldSafety.BLOCK_AI, policy.evaluate(d))
    }

    @Test fun otp_blocks_ai() {
        val d = EditorDescriptor(InputType.TYPE_CLASS_NUMBER, "com.example.sms", "Enter OTP", false)
        assertEquals(FieldSafety.BLOCK_AI, policy.evaluate(d))
    }

    @Test fun ordinary_message_allows_ai() {
        val d = EditorDescriptor(InputType.TYPE_CLASS_TEXT, "org.telegram.messenger", "Message", false)
        assertEquals(FieldSafety.ALLOW_AI, policy.evaluate(d))
    }
}
```

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew testDebugUnitTest --tests '*SensitiveFieldPolicyTest'`

Expected: FAIL because policy types do not exist.

- [ ] **Step 3: Implement explicit blocking rules**

`EditorDescriptor` contains `inputType`, `packageName`, `hintText`, and `accessibilityPassword`. Block Android password variations, visible-password variants used by credential forms, PIN/OTP hints, card/CVV/payment hints, and accessibility password nodes. Do not blanket-block normal banking-app chat/support fields by package name alone; blocking is field/surface based.

- [ ] **Step 4: Run policy tests**

Run: `./gradlew testDebugUnitTest --tests '*SensitiveFieldPolicyTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/safety app/src/test/java/com/socialaiassistant/keyboard/safety
 git commit -m "feat: block AI on sensitive editor fields"
```

---

### Task 3: Functional Offline IME + Smart Reply Shell

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardAction.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/SmartReplyController.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Create: `app/src/main/res/layout/ime_keyboard.xml`
- Create: `app/src/main/res/xml/method.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ime/KeyboardLayoutTest.kt`
- Test: `app/src/androidTest/java/com/socialaiassistant/keyboard/ime/ImeInsertionInstrumentedTest.kt`

**Interfaces:**
- Consumes: `SensitiveFieldPolicy` from Task 2 and `ReplyState` later from Task 9.
- Produces: Android IME service with `insertGeneratedReply(reply: String, replaceDraft: Boolean = false): InsertResult` and a UI smart-bar API `SmartReplyController.render(state)`.

- [ ] **Step 1: Write a failing unit test for the initial keyboard key model**

```kotlin
class KeyboardLayoutTest {
    @Test fun qwerty_layout_contains_expected_rows() {
        val rows = KeyboardLayout.englishQwerty().rows.map { it.map(KeySpec::label) }
        assertEquals(listOf("q","w","e","r","t","y","u","i","o","p"), rows.first())
        assertTrue(rows.flatten().contains("⌫"))
        assertTrue(rows.flatten().contains("↵"))
    }
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew testDebugUnitTest --tests '*KeyboardLayoutTest'`

Expected: FAIL because keyboard model does not exist.

- [ ] **Step 3: Implement the minimal English keyboard and IME service**

Use a lightweight View-based keyboard. Key taps call `currentInputConnection.commitText(...)`, delete uses `deleteSurroundingText(1,0)`, enter uses `sendKeyEvent`/`performEditorAction` depending on `EditorInfo.imeOptions`, and shift changes local key labels. The smart bar shows one of: hidden, loading, reply text, error/status. No network/database call occurs in any key handler.

- [ ] **Step 4: Enforce draft preservation in insertion**

Before inserting an AI reply, inspect text around the cursor. If `replaceDraft == false` and the current editor has non-whitespace draft content, return `InsertResult.DraftPresent` and show an explicit `Insert anyway`/`Replace` choice; never overwrite automatically. For an empty composer, `commitText(reply, 1)` is allowed after the user taps the suggestion.

- [ ] **Step 5: Add IME manifest/service metadata**

Declare `android.permission.BIND_INPUT_METHOD`, export the IME service as required by Android, and reference `@xml/method`. Do not add Accessibility yet.

- [ ] **Step 6: Run unit/build checks**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS.

- [ ] **Step 7: Add instrumentation coverage for tap-to-insert semantics**

Use an instrumentation test host editor/activity and assert that invoking the explicit smart-reply insertion path places reply text only after the test performs the equivalent of a tap. Include a non-empty-draft case that returns `DraftPresent` without replacing the draft.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ime app/src/main/res/layout/ime_keyboard.xml app/src/main/res/xml/method.xml app/src/main/AndroidManifest.xml app/src/test app/src/androidTest
 git commit -m "feat: add responsive offline keyboard IME"
```

---

### Task 4: Accessibility Context Bridge + Generic Conversation Adapter

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/ContextModels.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/ContextSnapshotBus.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/GenericConversationAdapter.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/GenericConversationAdapterTest.kt`

**Interfaces:**
- Produces:
  - `data class ContextSnapshot(packageName, windowSignature, conversationHint, messages, latestRecipientMessage, composerHint, confidence, capturedAtMillis)`
  - `data class ContextMessage(sender: SenderClass, text: String, timestampHint: String?)`
  - `enum class SenderClass { SELF, RECIPIENT, UNKNOWN }`
  - `ContextSnapshotBus.snapshots: StateFlow<ContextSnapshot?>`
- Accessibility service never exposes `AccessibilityNodeInfo` outside its package-private extraction scope.

- [ ] **Step 1: Write failing adapter tests using synthetic plain node models**

Tests must cover: ordered visible text rows, exclusion of blank/control-only text, maximum snapshot size, and latest recipient selection when sender inference is confident.

- [ ] **Step 2: Verify failure**

Run: `./gradlew testDebugUnitTest --tests '*GenericConversationAdapterTest'`

Expected: FAIL.

- [ ] **Step 3: Implement normalized context models and event bus**

`ContextSnapshotBus` is a process-local `MutableStateFlow`; only normalized immutable data enters it. Snapshot text is bounded before publishing.

- [ ] **Step 4: Implement event-driven Accessibility service**

Listen to window-content/state/text-selection events needed for conversation discovery. Debounce per package/window. Skip extraction if no editable text surface is present or if current IME session reports `BLOCK_AI`. Traverse only a bounded node count/depth, copy required text immediately, and recycle/release node references according to platform API behavior.

- [ ] **Step 5: Add disclosure-oriented service metadata**

Configure only required event types/capabilities. Do not request gesture dispatch or perform clicks. Add service declaration with `BIND_ACCESSIBILITY_SERVICE` and `@xml/accessibility_service_config`.

- [ ] **Step 6: Run tests/build**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/context app/src/main/res/xml/accessibility_service_config.xml app/src/main/AndroidManifest.xml app/src/test/java/com/socialaiassistant/keyboard/context
 git commit -m "feat: add accessibility conversation context bridge"
```

---

### Task 5: Conversation Identity + Encrypted Local Room History

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/context/ConversationKeyFactory.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/crypto/AesGcmCipher.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationEntities.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationDao.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/memory/AppDatabase.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationRepository.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/context/ConversationKeyFactoryTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/crypto/AesGcmCipherTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/memory/ConversationRepositoryTest.kt`

**Interfaces:**
- Produces: `ConversationKeyFactory.create(snapshot): ConversationKey`, `AesGcmCipher.encrypt/decrypt`, and `ConversationRepository.mergeSnapshot(snapshot): ConversationHistory`.
- Room stores ciphertext, IV/nonce, metadata hashes and timestamps; it never stores raw message bodies.

- [ ] **Step 1: Write failing key-factory tests**

Test stable key generation for the same package + conversation hint and distinct keys for different conversation hints. Test fallback window signature use when no contact/thread hint is available. Assert raw message text is not part of the returned key.

- [ ] **Step 2: Write failing AES-GCM round-trip and ciphertext tests**

```kotlin
@Test fun ciphertext_does_not_contain_plaintext() {
    val encrypted = cipher.encrypt("secret message")
    assertFalse(encrypted.ciphertext.decodeToString().contains("secret message"))
    assertEquals("secret message", cipher.decrypt(encrypted))
}
```

- [ ] **Step 3: Implement Keystore-backed AES-256-GCM**

Alias: `social_ai_conversation_key_v1`. Generate non-exportable AES key with GCM/NoPadding. Each encryption uses a fresh random IV. If encryption fails, repository must not fall back to plaintext persistence.

- [ ] **Step 4: Implement Room schema and dedupe hash**

Entities: conversation table and message table. Message dedupe hash input is `conversationKey + sender + normalizedText + coarseTimeBucket`. Add unique index on dedupe hash. Store encrypted body separately from metadata.

- [ ] **Step 5: Write repository tests for merge/dedupe**

Use an in-memory Room database and a deterministic test cipher implementation injected behind a `TextCipher` interface. Merge the same snapshot twice and assert one message row per unique message.

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest --tests '*ConversationKeyFactoryTest' --tests '*AesGcmCipherTest' --tests '*ConversationRepositoryTest'`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/context/ConversationKeyFactory.kt app/src/main/java/com/socialaiassistant/keyboard/crypto app/src/main/java/com/socialaiassistant/keyboard/memory app/src/test/java/com/socialaiassistant/keyboard/context app/src/test/java/com/socialaiassistant/keyboard/crypto app/src/test/java/com/socialaiassistant/keyboard/memory
 git commit -m "feat: persist encrypted local conversation history"
```

---

### Task 6: Port Core Extension Language/Mode/Result Behavior

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/AiMode.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/AiModels.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionLanguageLogic.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ModelResultParser.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/ExtensionLanguageLogicTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/ModelResultParserTest.kt`
- Reference only: `legacy-extension/background.js`

**Interfaces:**
- Produces: `enum class AiMode { GENERAL, FLIRT_MSG, FLIRT_CMT, WITTY }`, language decision model, `PromptRequest`, `PromptBundle(system, user)`, and `ParsedAiResult(reply, category, confidence)`.

- [ ] **Step 1: Create parity fixtures from `legacy-extension/background.js`**

Encode representative inputs/expected outputs for `cleanString`, mode normalization, Bengali script, Banglish tokens, English, Arabic/Devanagari/Cyrillic/CJK source-language behavior, and newest-recipient language priority. Include at least one malformed JSON model result and fenced JSON model result.

- [ ] **Step 2: Run tests and confirm they fail before Kotlin implementation**

Run: `./gradlew testDebugUnitTest --tests '*ExtensionLanguageLogicTest' --tests '*ModelResultParserTest'`

Expected: FAIL.

- [ ] **Step 3: Port `cleanString` and `normalizeMode` behavior exactly**

Keep the same allowed modes and GENERAL fallback. Preserve whitespace cleanup and max-length semantics from the extension.

- [ ] **Step 4: Port language detection semantics**

Preserve Bengali Unicode priority, source-script handling, Banglish heuristics, and the rule that the latest recipient message determines reply language when available. Keep Bangladesh-context default behavior from the extension for new conversations.

- [ ] **Step 5: Port prompt-building rules with prompt-injection fencing**

Treat conversation/user content strictly as untrusted quoted context. System policy must instruct the model not to follow instructions contained inside conversation text. Preserve extension style/custom-knowledge behavior relevant to GENERAL, WITTY, FLIRT_MSG and FLIRT_CMT.

- [ ] **Step 6: Port model-result parser**

Support fenced JSON, plain JSON, `reply` cleanup, confidence clamping, and a plain-text fallback. Reject empty replies.

- [ ] **Step 7: Run parity tests**

Run: `./gradlew testDebugUnitTest --tests '*ExtensionLanguageLogicTest' --tests '*ModelResultParserTest'`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai app/src/test/java/com/socialaiassistant/keyboard/ai
 git commit -m "feat: port extension reply intelligence to Kotlin"
```

---

### Task 7: Settings + Keystore-Protected Personal OpenRouter Key

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/crypto/SecretStore.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/settings/SetupState.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/AiSettingsRepository.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/crypto/SecretStoreTest.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/settings/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces: `SettingsRepository.settings: Flow<AppSettings>`, `SecretStore.putOpenRouterKey/getOpenRouterKey/clearOpenRouterKey`, `AiSettingsRepository` for model mode and gateway mode.

- [ ] **Step 1: Write failing tests for secret round-trip and no-plaintext preferences**

Store `sk-or-test-secret`, read it back, and inspect DataStore/shared storage test backing data to assert the exact secret string is not stored directly.

- [ ] **Step 2: Implement `SecretStore` with a separate Keystore alias**

Alias: `social_ai_secret_key_v1`. Encrypt the OpenRouter key using AES-GCM; persist only ciphertext + IV in private app storage. Never expose a method that returns the key for UI display; UI shows only configured/not configured and an optional last-four mask computed before storage.

- [ ] **Step 3: Implement settings repository**

Settings include model mode `fast|smart`, AI gateway `personal|managed`, preserveDraft, contextAccessConsent, accessibilityDisclosureAccepted, and maxChatMessages `10|30`. Managed mode exists in the model but remains unavailable/disabled in this foundation slice.

- [ ] **Step 4: Add setup controls/status**

`MainActivity` exposes buttons to open Android keyboard settings, show IME picker, open Accessibility settings, enter/test personal API key, and accept the prominent context-access disclosure. Managed AI selector is visibly disabled with copy: `Managed AI will be available in a later release.`

- [ ] **Step 5: Run tests/build**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/crypto/SecretStore.kt app/src/main/java/com/socialaiassistant/keyboard/settings app/src/main/java/com/socialaiassistant/keyboard/ai/AiSettingsRepository.kt app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt app/src/main/res/layout/activity_main.xml app/src/main/res/values/strings.xml app/src/test
 git commit -m "feat: add secure personal AI settings and onboarding"
```

---

### Task 8: OpenRouter Gateway with Extension Model Fallback

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/OpenRouterModels.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/OpenRouterGateway.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/OpenRouterGatewayTest.kt`

**Interfaces:**
- Produces: `interface AiGateway { suspend fun generate(request: AiGenerationRequest): AiGenerationResult }` and `OpenRouterGateway`.
- Model sets:
  - Fast primary `google/gemini-2.5-flash-lite`, fallback `google/gemini-2.5-flash`.
  - Smart primary `google/gemini-2.5-flash`, fallback `google/gemini-2.5-flash-lite`.

- [ ] **Step 1: Write MockWebServer tests**

Cover: successful first model, first-model 5xx then fallback success, 401 invalid key normalization, 429/rate-limit normalization, malformed success body, and timeout behavior.

- [ ] **Step 2: Verify failure**

Run: `./gradlew testDebugUnitTest --tests '*OpenRouterGatewayTest'`

Expected: FAIL.

- [ ] **Step 3: Implement gateway request/response models**

Send only required OpenRouter chat-completion fields, set authorization at request time from `SecretStore`, and never log headers/body containing user conversation content or secrets. The gateway accepts `PromptBundle` and returns raw model text + model name.

- [ ] **Step 4: Implement timeout and fallback**

Total generation timeout: 22 seconds. Fallback only for retryable provider/model failures; do not retry an invalid key. Preserve user cancellation.

- [ ] **Step 5: Implement normalized errors**

Use sealed errors such as `InvalidApiKey`, `RateLimited`, `InsufficientCredits`, `Timeout`, `Offline`, `ProviderFailure`, `MalformedResponse`. Map extension-equivalent messages at the UI boundary, not inside the HTTP client.

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest --tests '*OpenRouterGatewayTest'`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai/OpenRouterModels.kt app/src/main/java/com/socialaiassistant/keyboard/ai/OpenRouterGateway.kt app/src/test/java/com/socialaiassistant/keyboard/ai/OpenRouterGatewayTest.kt
 git commit -m "feat: add OpenRouter generation and model fallback"
```

---

### Task 9: Automatic Best Reply Orchestrator + Context Fingerprint Cache

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ReplyState.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ai/ReplyOrchestrator.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SmartReplyController.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/ai/ReplyOrchestratorTest.kt`

**Interfaces:**
- Consumes: `ContextSnapshotBus`, `ConversationRepository`, `SensitiveFieldPolicy`, `ExtensionPromptBuilder`, `AiGateway`, `SettingsRepository`.
- Produces: `StateFlow<ReplyState>` where states include `Hidden`, `WaitingForContext`, `Loading`, `Ready(reply, fingerprint)`, `Offline`, `NeedsApiKey`, `Error(message)`.

- [ ] **Step 1: Write failing orchestration tests with fakes**

Cover: allowed conversation generates once, unchanged fingerprint reuses cached reply, changed latest recipient message regenerates, blocked field cancels and hides reply, missing API key returns `NeedsApiKey`, and rapid context changes cancel/ignore stale result.

- [ ] **Step 2: Verify failure**

Run: `./gradlew testDebugUnitTest --tests '*ReplyOrchestratorTest'`

Expected: FAIL.

- [ ] **Step 3: Implement fingerprinting and one-in-flight policy**

Fingerprint stable normalized fields: conversation key + latest bounded message hashes + latest recipient message + configured AI mode/custom-knowledge revision. Do not include transient node IDs. Maintain a small in-memory LRU of successful reply by fingerprint.

- [ ] **Step 4: Implement async orchestration**

On allowed editor + context snapshot: debounce briefly, merge encrypted history off main thread, build GENERAL Best Smart Reply prompt, call Personal OpenRouter gateway, parse, publish state. Cancel previous job for that conversation when the fingerprint changes. Never touch `InputConnection` from the orchestrator.

- [ ] **Step 5: Wire IME smart bar**

IME observes `ReplyState` while active. `Ready` shows one tappable reply. Tap invokes the explicit insertion function from Task 3. Loading/error states never disable key buttons.

- [ ] **Step 6: Run tests/build**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/ai/ReplyState.kt app/src/main/java/com/socialaiassistant/keyboard/ai/ReplyOrchestrator.kt app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt app/src/main/java/com/socialaiassistant/keyboard/ime app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt app/src/test/java/com/socialaiassistant/keyboard/ai/ReplyOrchestratorTest.kt
 git commit -m "feat: generate and surface automatic best replies"
```

---

### Task 10: Vertical-Slice Verification, Privacy Audit, and APK Handoff

**Files:**
- Create: `docs/testing/foundation-vertical-slice-checklist.md`
- Create: `docs/privacy/context-access-disclosure.md`
- Modify: `README.md` (create if absent)
- Modify as needed: tests/files from Tasks 1-9 only when verification exposes a defect.

**Interfaces:**
- Produces: verified debug APK and documented manual setup/test procedure.

- [ ] **Step 1: Run the complete automated suite**

Run:

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

Expected: BUILD SUCCESSFUL; all JVM tests pass.

- [ ] **Step 2: Run static Android checks**

Run:

```bash
./gradlew lintDebug
```

Expected: no release-blocking errors. Fix any security/exported-component/password logging issues before continuing.

- [ ] **Step 3: Verify no secret/plaintext leakage in source/log statements**

Run:

```bash
grep -RniE 'Log\.|println\(|print\(' app/src/main/java || true
grep -RniE 'api[_ -]?key|authorization|messageBody|plaintext' app/src/main/java || true
```

Inspect every hit. No API key or decrypted conversation message may be logged.

- [ ] **Step 4: Write the manual device checklist**

Checklist must verify:

1. Enable keyboard and select it.
2. Normal English typing works with network off.
3. Backspace/shift/enter work while AI request is in flight.
4. Password/PIN/OTP fields hide AI and do not persist context.
5. Accessibility disabled → keyboard works, AI explains missing context.
6. Accessibility enabled + accepted disclosure → generic chat context snapshot arrives.
7. Same conversation reopened without changes does not regenerate unnecessarily.
8. New recipient message triggers a new Best Reply.
9. Tapping the reply inserts it into an empty composer.
10. Existing manual draft is not overwritten automatically.
11. Send remains manual.
12. Invalid OpenRouter key shows a safe action/error and never exposes the key.

- [ ] **Step 5: Write the prominent disclosure draft**

State in plain language that Accessibility is used to read visible conversational text to generate reply suggestions, conversation history is kept only on the device in encrypted form, supported conversation text may be sent to the selected AI provider when AI is enabled, sensitive fields are blocked, and the app never presses Send automatically.

- [ ] **Step 6: Verify APK artifact**

Run:

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

Expected: APK exists and is non-zero size.

- [ ] **Step 7: Commit verification docs**

```bash
git add docs README.md
 git commit -m "docs: verify foundation Android keyboard slice"
```

- [ ] **Step 8: Tag the foundation milestone after device verification**

```bash
git tag foundation-v0.1.0
```

Do not tag until the manual checklist has actually been completed on a device/emulator and any discovered defects are fixed and re-verified.

---

## Definition of Done for This Plan

The foundation vertical slice is complete only when all of the following are true:

- The project builds with the locked SDK/toolchain.
- `Social AI Keyboard` can be enabled and selected as an Android IME.
- Basic English typing, backspace, shift and enter work with no network.
- AI is hard-blocked on sensitive fields.
- Accessibility context is event-driven, bounded, normalized and never performs clicks/send.
- Conversation history is stored locally with encrypted message bodies and duplicate suppression.
- Core extension language/mode/result behavior has Kotlin parity tests.
- Personal OpenRouter mode works with Fast/Smart fallback and normalized errors.
- A conversational field can produce one automatic Best Smart Reply without blocking typing.
- Reply text is inserted only after the user taps it.
- Existing drafts are preserved by default.
- Send remains user-controlled.
- Full JVM test suite and debug lint pass.
- A debug APK exists for hands-on testing.

## Explicitly Deferred to Later Sub-Projects

This plan intentionally does **not** include production Bangla Avro/Bijoy engines, emoji browser, clipboard history UI, voice typing integration, theme customization, polished Smart/Witty/Flirty panel, package-specific Facebook/Messenger/WhatsApp/Instagram/Telegram adapters, media/image/video context capture, managed backend AI, account/subscription logic, Play signing/AAB, or final store listing assets. Those are covered by the later delivery phases in the approved design spec.
