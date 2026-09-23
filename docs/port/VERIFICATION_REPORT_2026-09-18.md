# Verification Report - 2026-09-18

## Completed checks

### Release verifier

Command:

```bash
python3 scripts/verify_release_ready.py
```

Result: **PASS**

Verified contracts reported by the script:

- API 36 compile/target contract.
- Social AI Assistant Pro v35.3.1 version contract.
- Gradle 9.6 wrapper.
- Cleartext traffic and backup disabled.
- Accessibility declared non-tool and no gesture capability.
- Manifest permissions constrained.
- No likely embedded OpenRouter secret.

### Additional static preflight

Result: **PASS**

- Parsed every Android resource XML and AndroidManifest.
- Checked `@string/...` and `R.string...` references against `strings.xml`.
- Performed a basic delimiter-balance scan across main Kotlin sources.
- Checked v35.3.1 Worker payload fields against `routes-ai.js` / `ai-prompts.js`.
- Checked Device Proof canonical message format against Worker `device-proof.js`.
- Checked subscription expiry field against Worker `subscriptions.js` (`cycle_expires_at`).

## Full Android build limitation

A Gradle unit-test/build attempt could not progress because the execution environment could not resolve/download Gradle 9.6 from `services.gradle.org` (`UnknownHostException`) and did not contain a compatible cached Gradle distribution/Android dependency cache.

Therefore this handoff is a **source-level port with passing static verification**, not a claim that a fresh APK was successfully compiled in this environment.

## Required pre-release command

On Android Studio/CI with JDK 17, Android SDK 36 and network/cached Gradle available:

```bash
python3 scripts/verify_release_ready.py
./gradlew --no-daemon clean testDebugUnitTest lintDebug assembleDebug
```

Then complete the existing real-device release matrix before production distribution.

## 2026-09-19 explicit-trigger / settings / entitlement verification addendum

Source-level verification completed for the next Keyboard update:

- explicit-only AI generation contract;
- tap-time REPLY / CONTINUE / START intent resolution;
- no raw JSON envelope rendering;
- premium actionable consent banner;
- six-section Premium Neon Settings Hub;
- `X-SocialAI-Product: KEYBOARD` request identity;
- separate Keyboard and Assistant Pro entitlement parsing/display;
- legacy-response compatibility when the `entitlements` field is absent.

Executed PASS checks:

```text
RELEASE VERIFICATION: PASS
TASK8_XML_RESOURCE_MANIFEST_PASS
TASK8_INTENT_PASS
TASK8_TRIGGER_PASS
TASK8_ENVELOPE_PASS
TASK8_ENTITLEMENT_PASS
TASK8_DIFF_CHECK_PASS
```

Canonical Gradle unit/lint/APK commands were attempted and remain environment-blocked before project configuration by `UnknownHostException: services.gradle.org`. Real-device WhatsApp/Messenger behavior therefore remains a required external release gate; no APK/build-pass claim is made by this report.
