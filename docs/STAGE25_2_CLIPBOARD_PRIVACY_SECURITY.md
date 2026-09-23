# Stage 25.2 — Clipboard Privacy & Security Hardening

## Scope

This stage hardens the optional recent-clipboard feature without changing normal typing, AI generation, conversation memory, supported-app Accessibility boundaries, themes, or Bubble Flight behavior.

## Problems fixed

1. Recent clipboard history was enabled by default.
2. Clipboard history values were stored as plaintext `SharedPreferences` entries in `recent_clipboard_history_v1`.
3. Stored history had no automatic retention/expiry window.
4. Turning Clipboard History OFF did not guarantee deletion of previously stored history.
5. The clipboard panel could still display the current clipboard in protected fields.
6. A clip marked sensitive by Android was excluded from persistence but could still be displayed by the panel.
7. Historical Typing Stage 6 verification still targeted the pre-Stage-24.3 settings architecture and produced false failures.

## Implementation

### Opt-in default

`AppSettings.clipboardHistory` now defaults to `false`, and a missing DataStore preference also decodes to `false`. Existing users who explicitly enabled the setting keep that explicit value.

### Encrypted v2 history store

`RecentClipboardStore` now uses `recent_clipboard_history_v2_encrypted` and persists only:

- one Base64 AES-GCM ciphertext blob;
- one Base64 GCM IV.

The entire bounded history payload is encrypted with AES-256-GCM using a dedicated Android Keystore key:

`social_ai_clipboard_history_key_v1`

There is no plaintext-storage fallback if encryption fails.

### Legacy plaintext purge

Stage 25.2 deliberately does not migrate `recent_clipboard_history_v1` values. The legacy preferences are cleared when `RecentClipboardStore` is constructed. `SocialAiApplication` constructs the store at process startup, so legacy plaintext is removed without requiring the user to open the Clipboard settings page or keyboard clipboard panel.

### Retention

History remains bounded to 20 items and 4,000 characters per item. Each record has an encrypted capture timestamp and expires after 24 hours. Expired/corrupt/undecryptable history fails closed and is deleted.

### Sensitive clipboard behavior

The clipboard panel checks `primaryClipDescription` first. If either:

- the current editor is classified as `FieldSafety.BLOCK_AI`, or
- Android marks the current clip with `android.content.extra.IS_SENSITIVE`,

then the keyboard does not request/coerce the primary clipboard payload for the panel and does not decrypt/render recent history. A privacy notice is shown instead.

### Disable-and-delete semantics

Turning Clipboard History OFF from Settings immediately clears both encrypted v2 history and legacy v1 preferences. An application-level settings observer also clears history whenever the persisted setting is disabled, providing a second fail-safe path.

## Tests and verification

- `scripts/verify_clipboard_privacy_stage25_2.py`: **18/18 PASS**
- `scripts/clipboard_stage25_2_privacy_selftest.kt`: **PASS**
- `scripts/verify_typing_stage6.py`: **PASS** after updating the historical verifier to the current Stage 24.3 settings-category architecture.
- Stage 25.0 Conversation Memory verifier: **13/13 PASS**
- Stage 25.1 Accessibility Privacy verifier: **11/11 PASS**
- Release verifier: **PASS**
- Stage 24.3 Settings Navigation: **158/158 PASS**
- Stage 24.1 Bubble Flight: **48/48 PASS**
- Stage 24.2 Optional Theme Bubble: **30/30 PASS**
- Modified XML resources parse successfully.

New/updated unit coverage includes:

- encrypted history round-trip without plaintext preference values;
- legacy plaintext purge;
- 24-hour expiry;
- bounded/deduplicated history ordering;
- clear removes v1 + v2 history;
- sensitive field / Android-sensitive clip exposure policy;
- clipboard history OFF default in `SettingsRepository`.

## Files changed

- `app/src/main/java/com/socialaiassistant/keyboard/ime/RecentClipboardStore.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ime/ClipboardTextPolicy.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`
- `app/src/test/java/com/socialaiassistant/keyboard/ime/RecentClipboardStoreTest.kt`
- `app/src/test/java/com/socialaiassistant/keyboard/ime/ClipboardTextPolicyTest.kt`
- `app/src/test/java/com/socialaiassistant/keyboard/settings/SettingsRepositoryTest.kt`
- `app/src/main/res/layout/settings_category_clipboard.xml`
- `app/src/main/res/values/strings.xml`
- `scripts/verify_clipboard_privacy_stage25_2.py`
- `scripts/clipboard_stage25_2_privacy_selftest.kt`
- `scripts/verify_typing_stage6.py`
- Play/privacy/testing documentation.

## Build limitation in this environment

A full Android Gradle `assembleDebug`, Android Lint, Robolectric suite, and real-device IME run were not executed here because the provided source package intentionally does not include the verified Gradle wrapper JAR and this runtime has no Android SDK/system Gradle installation. The source-level regression contracts above were executed successfully.

## Recommended device checks

Before production release, verify on a real Android device that:

1. Fresh install shows Clipboard History OFF.
2. Enabling history records normal clipboard text and survives IME restart.
3. Stored history disappears after the retention window (a debug clock/test build can accelerate this check).
4. Turning history OFF removes all recent history immediately.
5. Password/PIN/OTP/payment fields show no clipboard content/history.
6. A clip marked sensitive by Android is not shown in the keyboard clipboard panel.
7. Normal current-clipboard paste from the explicit clipboard panel still works when the field and clip are not sensitive.
