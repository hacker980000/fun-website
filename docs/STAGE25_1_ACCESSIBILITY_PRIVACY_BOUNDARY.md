# Stage 25.1 — Accessibility Privacy Boundary

## Goal

Make AI conversation-text extraction fail closed outside the explicitly supported social/chat package adapters, while preserving normal IME typing and the existing transient Bubble Flight editable-bounds fallback.

## Supported package adapter boundary

The current built-in adapters cover these package IDs:

- Facebook: `com.facebook.katana`, `com.facebook.lite`
- Messenger: `com.facebook.orca`, `com.facebook.mlite`
- WhatsApp: `com.whatsapp`, `com.whatsapp.w4b`
- Instagram: `com.instagram.android`, `com.instagram.lite`
- Telegram-family adapters: `org.telegram.messenger`, `org.telegram.messenger.web`, `org.thunderdog.challegram`

An app outside this list does not receive generic Accessibility conversation-text extraction.

## Implementation

### 1. Registry is fail closed

`PlatformContextAdapterRegistry` now exposes `supportsPackage(packageName)` and returns an empty `PlatformContextResult` for unsupported packages:

- `conversationHint = null`
- `nodes = emptyList()`
- `confidenceBoost = 0f`

This is a defense-in-depth boundary if registry adaptation is called from another code path later.

### 2. Accessibility service gates before text traversal

`SocialAiAccessibilityService.onAccessibilityEvent()` checks package support before scheduling conversation extraction. On an unsupported package it:

- cancels pending context extractions;
- clears the transient `ContextSnapshotBus`;
- returns without scheduling `extractCurrentWindow()`.

`extractCurrentWindow()` repeats the same package-support check before accessing `rootInActiveWindow`, so a delayed or future call cannot bypass the package boundary.

### 3. Bubble Flight remains independent

The AccessibilityService XML is intentionally not restricted with `android:packageNames`. Bubble Flight may still use only transient editable-field geometry as its accessibility fallback in other editors. The AI text-reading path is separately gated by the supported-package check and AI Context Access consent.

This preserves the Stage 24.1 cross-window bubble behavior without restoring generic conversation-text capture.

## Regression coverage

Added/updated checks verify:

1. supported packages are explicitly recognized;
2. an unknown package returns no nodes or conversation identity;
3. passing that fail-closed result through `GenericConversationAdapter` cannot create a `ContextSnapshot`;
4. the service package guard occurs before debounce extraction;
5. `extractCurrentWindow()` rechecks package support before root traversal;
6. unsupported-package handling clears stale transient snapshot state;
7. product privacy/release documentation no longer promises a generic unknown-app context fallback.

## Verification performed

- `scripts/verify_accessibility_privacy_stage25_1.py`: **11/11 PASS**
- `PlatformContextAdapterRegistryTest` compiled with `kotlinc`: **PASS**
- Stage 25.0 conversation-memory verifier: **13/13 PASS**
- Release verifier: **PASS**
- Stage 24.3 settings navigation: **158/158 PASS**
- Stage 24.1 Bubble Flight: **48/48 PASS**
- Stage 24.2 Optional Bubble: **30/30 PASS**

## Device checks still required before release

On a real Android device:

1. Open WhatsApp/Messenger/Facebook/Instagram/Telegram and verify AI reply context still works.
2. Open an unsupported SMS/email/chat app and verify normal typing works but AI conversation context is unavailable.
3. Switch directly from a supported chat to an unsupported app and verify no previous conversation suggestion/context leaks across.
4. Verify sensitive fields still block AI context.
5. With Bubble Key enabled, verify animation still works in unsupported editors without enabling AI conversation-text context there.

## Build limitation in this environment

A complete Android Gradle `assembleDebug`/instrumented-device run was not executed here because the source package intentionally does not contain the verified Gradle wrapper JAR and this environment does not provide the full Android build toolchain. The source-level Kotlin registry test and the project static verification contracts above were run successfully.
