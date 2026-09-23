# Stage 25.3 — Sensitive Field Detection Hardening

## Scope

This stage hardens the field-safety boundary used by AI context access, AI actions, typing learning, voice/deferred helpers, Bubble Flight safety, and the Stage 25.2 clipboard privacy controls. It preserves the Stage 25.0 conversation-memory fix, Stage 25.1 supported-app Accessibility boundary, and Stage 25.2 encrypted clipboard behavior.

## Problems addressed

1. Production IME safety evaluation used only `inputType`, `hintText`, and `privateImeOptions`; `EditorInfo.label`, `fieldName`, `actionLabel`, and bounded semantic extras were ignored.
2. `EditorDescriptor.autofillHints` existed for tests/callers but standard `EditorInfo` does not directly expose the owning View's `autofillHints`, leaving framework-specific mirrored tokens unused.
3. A plain `TYPE_CLASS_NUMBER` editor with no useful metadata was treated as a normal AI-capable field even though OTP, PIN, and CVV inputs are commonly implemented this way.
4. Accessibility already observed `node.isPassword` for Bubble Flight geometry, but a stronger focused-node safety signal could not tighten the active IME session.
5. English-only metadata matching missed common Bangla OTP/PIN/card labels and camelCase/autofill-like tokens such as `smsOTPCode` and `creditCardSecurityCode`.

## Implementation

### 1. Rich bounded EditorInfo metadata

`EditorInfoSafetyAdapter` now builds the production `EditorDescriptor` from:

- `inputType`;
- `hintText`;
- `label`;
- `fieldName`;
- `actionLabel`;
- `privateImeOptions`;
- up to 24 bounded security-relevant EditorInfo-extra hints, with at most 160 characters per hint.

The adapter does **not** read surrounding or typed text. Extra values are inspected only for keys that look safety-relevant (for example autofill/hint/field/password/OTP/PIN/card/payment/security keys), and only simple text-like values are accepted.

Android's standard `EditorInfo` does not directly expose the owning View's `autofillHints`. The policy still accepts explicit autofill hints through `EditorDescriptor`, and the production adapter recognizes autofill/security tokens when frameworks mirror them into EditorInfo fields/extras.

### 2. Android-free semantic classifier

`SensitiveMetadataClassifier` normalizes:

- camelCase / acronym boundaries;
- punctuation and HTML-style tokens;
- Unicode letters, combining marks, and numbers, preserving Bangla text.

It recognizes password/passcode, OTP/2FA/MFA, PIN/MPIN, CVV/CVC, verification/security/auth codes, card/payment/banking terms, common mobile-finance PIN labels, HTML `one-time-code`, and autofill-like tokens such as `smsOTPCode` / `creditCardSecurityCode`.

Bangla coverage includes terms such as `ওটিপি`, `পিন`, `কার্ড নম্বর`, `বিকাশ পিন`, `নগদ পিন`, and common password/security-code wording.

### 3. Privacy-first numeric behavior

- Numeric password variation: `BLOCK_AI`.
- Numeric field with sensitive metadata: `BLOCK_AI`.
- Plain/ambiguous numeric field with weak or missing metadata: `BLOCK_AI` (fail closed).
- Clearly described benign numeric field (Amount, Quantity, Age, Postal code, etc.): `NO_CONVERSATION`.
- Phone/date-time editors: `NO_CONVERSATION`.
- Normal text message editors without sensitive metadata: `ALLOW_AI`.

This distinction keeps explicit local typing/clipboard/voice behavior available for clearly benign non-conversation numeric fields while protecting metadata-poor numeric credential/code fields.

### 4. Accessibility safety escalation

`SocialAiAccessibilityService` now checks the current focused editable node on focus/window safety events using only:

- `inputType`;
- `isPassword`;
- hint text;
- content description/label;
- view-id resource name;
- class name.

If Accessibility reveals a stricter classification, `ImeSessionRegistry.restrictSafety()` can only move the active session monotonically:

`ALLOW_AI -> NO_CONVERSATION -> BLOCK_AI`

It cannot downgrade or re-enable AI. On a stricter signal, pending context extraction is cancelled and the context snapshot is cleared. `inputType=0` virtual/Compose nodes do not force `NO_CONVERSATION` solely due missing metadata; explicit sensitive/password signals still block.

To avoid new per-keystroke overhead, this extra safety evaluation is not run on `TYPE_VIEW_TEXT_CHANGED` or selection-change events; it runs on focus/window state/content safety events.

## Verification

Targeted Stage 25.3 checks:

- `scripts/verify_sensitive_field_stage25_3.py`: **24/24 PASS**
- `scripts/run_sensitive_field_stage25_3_selftest.sh`: **PASS**

The Android-independent self-test covers:

- text/number password variants;
- unlabeled numeric fail-closed behavior;
- benign amount numeric behavior;
- phone non-conversation behavior;
- normal message allowance;
- field-name-only verification code;
- `smsOTPCode`;
- HTML `one-time-code`;
- `creditCardSecurityCode`;
- Bangla bKash PIN;
- bank support chat not being blocked by package name alone.

New Robolectric/JUnit source coverage also covers EditorInfo label/field/action metadata and bounded relevant extras.

Regression checks retained after this change:

- Stage 25.0 Conversation Memory: **13/13 PASS**
- Stage 25.1 Accessibility Privacy Boundary: **11/11 PASS**
- Stage 25.2 Clipboard Privacy & Security: **18/18 PASS**
- Release verifier: **PASS**
- Stage 24.3 Settings Navigation: **158/158 PASS**
- Stage 24.1 Bubble Flight: **48/48 PASS** + policy self-test PASS
- Stage 24.2 Optional Theme Bubble: **30/30 PASS** + policy self-test PASS
- Typing Stage 6 current-architecture verifier: **PASS**

A complete sweep of all `scripts/verify_*.py` files produced **41 PASS / 6 FAIL**. The six failures are exactly the same historical failures present in the Stage 25.2 baseline (`verify_keyboard_bounded_update`, Stage 23.6 numberpad, Stage 23.3 typing, and Typing Stages 4/7/8). No verifier shared by Stage 25.2 and Stage 25.3 changed from PASS to FAIL. The extra PASS is the new Stage 25.3 verifier.

## Files added/changed

- `app/src/main/java/com/socialaiassistant/keyboard/safety/FieldSafety.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicy.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveMetadataClassifier.kt` (new)
- `app/src/main/java/com/socialaiassistant/keyboard/ime/EditorInfoSafetyAdapter.kt` (new)
- `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt`
- `app/src/test/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicyTest.kt`
- `app/src/test/java/com/socialaiassistant/keyboard/ime/EditorInfoSafetyAdapterTest.kt` (new)
- `scripts/verify_sensitive_field_stage25_3.py` (new)
- `scripts/sensitive_field_stage25_3_selftest.kt` (new)
- `scripts/run_sensitive_field_stage25_3_selftest.sh` (new)
- privacy/data-safety/release-device documentation.

## Build limitation in this environment

A full Android Gradle `assembleDebug`, Android Lint, Robolectric suite, instrumentation suite, and real-device IME run were not executed here because the supplied source package intentionally lacks the verified Gradle wrapper JAR and this runtime has no Android SDK/system Gradle installation. The Android-independent safety classifier is compiled and executed with `kotlinc`, while source-level/current-stage regression contracts are run separately.

## Required real-device checks before release

1. Password and number-password fields block AI, clipboard exposure, voice/deferred AI helpers, and typing learning.
2. Plain numeric OTP/PIN/CVV fixtures with no hint/label fail closed.
3. Amount/Quantity/Age/Postal-code numeric fixtures remain usable as non-conversation fields.
4. `smsOTPCode`, `one-time-code`, `verificationCodeInput`, and `creditCardSecurityCode` metadata fixtures block.
5. Bangla OTP/PIN/card labels block.
6. A host field whose EditorInfo is weak but Accessibility reports `isPassword=true` tightens the active session and clears context.
7. Normal Messenger/WhatsApp/Telegram text composer fields remain `ALLOW_AI` and existing reply flows still work.
