# Release Hardening and Build Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the current Social AI Keyboard source ready for repeatable APK/AAB builds, device QA, and Google Play submission while preserving the approved user-controlled AI behavior.

**Architecture:** Keep the existing Android IME, Accessibility Context Bridge, local encrypted history, and Personal OpenRouter flow unchanged. Add a release build layer (versioning, optional upload-key signing, CI), a user-visible privacy/data-use screen with local-history deletion, and machine-checkable release/policy verification plus Play Console documentation.

**Tech Stack:** Android/Kotlin, AGP 9.4.0, Gradle 9.6.0, JDK 17+, API 36, Room, DataStore, GitHub Actions, Python 3 release verification.

**Spec:** `docs/superpowers/specs/2026-09-13-social-ai-keyboard-design.md`

## Global Constraints

- New Google Play submissions on 2026-09-14 must target Android 16 / API 36 or higher; keep `compileSdk = 36` and `targetSdk = 36`.
- AGP 9.4.0 requires Gradle 9.6.0 and JDK 17 or newer; keep the wrapper on Gradle 9.6.0.
- Accessibility is used for reply context, not as an accessibility tool: keep `android:isAccessibilityTool="false"` and `android:canPerformGestures="false"`.
- Accessibility use must remain read-only; never add auto-click, auto-send, gesture dispatch, or global-action automation.
- Prominent disclosure and affirmative consent must occur before opening Android Accessibility settings.
- Conversation history remains local-only and encrypted; no cloud sync is introduced.
- Password, PIN, OTP, card/payment, banking-sensitive, and protected fields remain AI-blocked.
- Generated text is inserted only after user action; Send remains user-controlled.
- Provider secrets must never be committed to source, workflow logs, or packaged resources.

---

### Task 1: Release Build Contract and Optional Signing

**Files:**
- Create: `scripts/verify_release_ready.py`
- Modify: `app/build.gradle.kts`
- Modify: `.gitignore`
- Test: `scripts/verify_release_ready.py`

**Interfaces:**
- Consumes: current AGP/Gradle version catalog and Android manifest.
- Produces: version `0.2.0-beta01` / versionCode `2`, optional release signing from Gradle properties, and a deterministic verifier callable with `python3 scripts/verify_release_ready.py`.

- [ ] **Step 1: Write the release verifier first**

Create `scripts/verify_release_ready.py` so it exits nonzero unless all of these are true: targetSdk/compileSdk are 36, versionCode is at least 2, versionName starts with `0.2.0`, wrapper is Gradle 9.6.0, manifest disables cleartext and backup, accessibility metadata says `isAccessibilityTool=false`, `canPerformGestures=false`, only expected manifest permission is INTERNET, and no likely OpenRouter secret (`sk-or-`) is present under tracked app/scripts/docs source.

- [ ] **Step 2: Run verifier and confirm RED**

Run:
```bash
python3 scripts/verify_release_ready.py
```
Expected: FAIL because the baseline still has `versionCode = 1` and `versionName = "0.1.0"`.

- [ ] **Step 3: Add release versioning and optional upload-key signing**

In `app/build.gradle.kts`, set:
```kotlin
versionCode = 2
versionName = "0.2.0-beta01"
```
Read release signing from Gradle properties `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. Configure `buildTypes.release.signingConfig` only when all four are present; do not hardcode values.

Append to `.gitignore`:
```text
*.jks
*.keystore
keystore.properties
local.properties
```

- [ ] **Step 4: Re-run verifier and confirm GREEN**

Run:
```bash
python3 scripts/verify_release_ready.py
```
Expected: PASS with a release-contract summary.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts .gitignore scripts/verify_release_ready.py
git commit -m "build: add release contract and optional signing"
```

### Task 2: In-App Privacy & Data Controls

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/PrivacyActivity.kt`
- Create: `app/src/main/res/layout/activity_privacy.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`
- Test: `app/src/test/java/com/socialaiassistant/keyboard/memory/ConversationRepositoryTest.kt`
- Test: `scripts/verify_release_ready.py`

**Interfaces:**
- Consumes: `ConversationRepository.clearAll()` and existing disclosure/consent state.
- Produces: an in-app Privacy & Data Use screen reachable from setup, plus a user action that clears all locally stored conversation history without touching the keyboard settings or API key.

- [ ] **Step 1: Add a failing repository behavior test**

Extend `ConversationRepositoryTest.kt` with a test that inserts conversation history, calls `clearAll()`, and asserts both the conversation and its messages are removed.

- [ ] **Step 2: Run the focused test and confirm RED or baseline coverage gap**

Run:
```bash
./gradlew testDebugUnitTest --tests '*ConversationRepositoryTest*'
```
If Gradle cannot start because dependencies/SDK cannot be resolved in the sandbox, record the environmental blocker and continue with the pure/static release verifier; do not claim the Android test passed.

- [ ] **Step 3: Expose the existing repository from the Application**

Change `SocialAiApplication` so the repository created in `onCreate()` is stored as:
```kotlin
lateinit var conversationRepository: ConversationRepository
    private set
```
and all existing orchestrator wiring uses that property.

- [ ] **Step 4: Add PrivacyActivity**

Create `PrivacyActivity` that displays the full local data-use summary and offers `Clear local conversation history`. On confirmation tap, call `conversationRepository.clearAll()` in `lifecycleScope`, then show a success toast. The action must not clear the API key or change Accessibility consent.

- [ ] **Step 5: Make privacy controls reachable from normal usage**

Add `PrivacyActivity` to the manifest, add a `Privacy & data use` button in `activity_main.xml`, and wire it in `MainActivity`.

The screen must explicitly state: visible conversational text may be read after consent; conversation history is stored locally in encrypted form; relevant text may be sent to the selected AI provider; sensitive fields are blocked; Accessibility does not perform gestures or send; the user can disable context access and clear local history.

- [ ] **Step 6: Extend the release verifier for privacy controls**

Require `PrivacyActivity` in the manifest, a setup-screen privacy button, a clear-history action string, and the disclosure-before-settings code path.

- [ ] **Step 7: Re-run verifiers**

Run:
```bash
python3 scripts/verify_release_ready.py
```
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main scripts/verify_release_ready.py app/src/test/java/com/socialaiassistant/keyboard/memory/ConversationRepositoryTest.kt
git commit -m "feat: add in-app privacy and local-history controls"
```

### Task 3: GitHub CI for APK and Signed AAB

**Files:**
- Create: `.github/workflows/android-build.yml`
- Create: `docs/release/github-build.md`
- Modify: `scripts/verify_release_ready.py`

**Interfaces:**
- Consumes: Gradle 9.6 wrapper, API 36 build config, optional Gradle signing properties from Task 1.
- Produces: CI that always builds/tests a debug APK and, on manual release workflow, builds a signed release AAB using GitHub Secrets.

- [ ] **Step 1: Extend verifier to require CI invariants**

Require workflow contents to use JDK 17, install `platforms;android-36` and `build-tools;36.0.0`, run the release verifier, run unit tests/lint/assembleDebug, upload the debug APK, and define a manual signed-AAB job using secrets without printing them.

- [ ] **Step 2: Run verifier and confirm RED**

Run:
```bash
python3 scripts/verify_release_ready.py
```
Expected: FAIL because `.github/workflows/android-build.yml` is missing.

- [ ] **Step 3: Create the CI workflow**

Use `actions/checkout@v4`, `actions/setup-java@v4` with Temurin 17, `android-actions/setup-android@v3`, `gradle/actions/setup-gradle@v4`, and `actions/upload-artifact@v4`.

For push/PR/manual runs, execute:
```bash
python3 scripts/verify_release_ready.py
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```
and upload `app/build/outputs/apk/debug/app-debug.apk`.

For `workflow_dispatch`, add a release job that decodes `PLAY_UPLOAD_KEYSTORE_B64` into a temporary `.jks`, passes the four release signing Gradle properties via environment variables, runs `bundleRelease`, uploads `app/build/outputs/bundle/release/app-release.aab`, and deletes the temporary keystore in an `always()` cleanup step.

- [ ] **Step 4: Document exact GitHub secret names**

Create `docs/release/github-build.md` documenting:
`PLAY_UPLOAD_KEYSTORE_B64`, `PLAY_UPLOAD_STORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`.
Include local commands for debug APK and signed AAB builds.

- [ ] **Step 5: Re-run verifier and confirm GREEN**

Run:
```bash
python3 scripts/verify_release_ready.py
```
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/android-build.yml docs/release/github-build.md scripts/verify_release_ready.py
git commit -m "ci: add apk and signed aab build pipeline"
```

### Task 4: Play Store Declaration and Device QA Package

**Files:**
- Create: `docs/play-store/accessibility-declaration.md`
- Create: `docs/play-store/data-safety-draft.md`
- Create: `docs/play-store/privacy-policy.md`
- Create: `docs/play-store/release-checklist.md`
- Create: `docs/testing/release-device-matrix.md`
- Modify: `README.md`
- Modify: `scripts/verify_release_ready.py`

**Interfaces:**
- Consumes: current product behavior and Task 2 privacy controls.
- Produces: text ready to adapt for Play Console declaration/Data Safety, a hostable privacy policy draft, and device tests that cover keyboard, Accessibility, sensitive fields, context adapters, AI insertion, offline behavior, and data deletion.

- [ ] **Step 1: Extend verifier for release documentation**

Require all five documents and ensure the accessibility declaration says the app is not an accessibility tool, uses read-only visible conversation context, does not perform gestures/auto-send, and requires prominent disclosure and consent.

- [ ] **Step 2: Run verifier and confirm RED**

Run:
```bash
python3 scripts/verify_release_ready.py
```
Expected: FAIL because the new release documents are missing.

- [ ] **Step 3: Write Play Console declaration draft**

Document the feature purpose, data categories (`Other in-app messages`, app activity needed to identify supported conversational surfaces), consent flow, opt-out flow, and exact review-video sequence: app open → full disclosure → accept → Android Accessibility settings → return → demonstrate suggested reply → user taps Insert → user presses Send manually → disable access.

- [ ] **Step 4: Write Data Safety and privacy policy drafts**

State local encrypted conversation-history storage, OpenRouter sharing only for AI generation in Personal API mode, no conversation-history cloud sync, user controls for disabling context, clearing history, and removing the local API key, and no sale of data.

- [ ] **Step 5: Write release/device checklist**

Cover Android 8/10/12/14/16 where available, Facebook, Messenger, WhatsApp, Instagram, Telegram, generic text fields, password/PIN/OTP/payment blocks, keyboard switching, Bangla phonetic/Bijoy, emoji/clipboard/voice, Smart/Unique/Flirty/Rewrite/Translate/Grammar, airplane mode, API failure, app restart, and local-history deletion.

- [ ] **Step 6: Update README with release commands and current limitations**

State that source targets API 36 and CI is the canonical build path. Do not claim Play approval or device validation before real results exist.

- [ ] **Step 7: Final verification**

Run:
```bash
python3 scripts/verify_release_ready.py
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```
The Python verifier must PASS. If Gradle is blocked by sandbox networking/SDK, report that exact blocker and rely on CI for the first real APK build; do not claim Android compile success locally.

- [ ] **Step 8: Commit**

```bash
git add docs README.md scripts/verify_release_ready.py
git commit -m "docs: add play store and release qa package"
```
