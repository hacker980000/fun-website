# Stage 26.0 — Build Readiness & QA Alignment

Date: 22 September 2026
Baseline: Social AI Keyboard v35.3.1 / Stage 25.6

## Purpose

This maintenance stage improves build readiness and test-signal quality without changing keyboard, AI, privacy, backend, or network behavior.

## Gradle / Android toolchain contract

- Android Gradle Plugin: **9.4.0**
- Gradle distribution: **9.6.0**
- JDK baseline: **17** (the CI baseline and AGP documented minimum/default)
- compileSdk / targetSdk: **36**
- Android Build Tools: **36.0.0**
- Java/Kotlin bytecode target configured by the app: **Java 17**
- AGP 9 built-in Kotlin is used; Room annotation processing remains on `com.android.legacy-kapt`.

The Gradle distribution and wrapper JAR are checksum-pinned. The wrapper bootstrap scripts accept the JAR only when its SHA-256 equals the published Gradle checksum.

## QA verifier refresh

Six old verifier scripts still assumed the pre-Stage-24 monolithic `activity_main.xml` settings layout or the Stage 23.6 negative-margin numeric workaround. The app had already moved to Settings Category screens and Stage 23.7 no-gap numeric geometry, so those scripts produced stale red results.

The scripts were updated to validate the current architecture:

- `verify_keyboard_bounded_update.py`
- `verify_numberpad_stage23_6.py`
- `verify_typing_stage23_3.py`
- `verify_typing_stage4.py`
- `verify_typing_stage7.py`
- `verify_typing_stage8.py`

Result after alignment: **50/50 `verify*.py` scripts PASS** in this source tree. The debug CI job now runs the complete suite as a fail-closed source-contract gate before Gradle unit/lint/APK tasks.

## Wrapper bootstrap quality

`bootstrap_gradle_wrapper.sh` now has two safe restoration paths:

1. download the wrapper JAR from the official Gradle GitHub tag and verify the pinned SHA-256;
2. fall back to generating it with an already-installed Gradle 9.6.0 and verify the same SHA-256.

A Windows PowerShell equivalent, `bootstrap_gradle_wrapper.ps1`, is included.

The source ZIP used for this audit did not contain `gradle-wrapper.jar`, so the wrapper completeness check remains **BOOTSTRAP REQUIRED** until one of those scripts succeeds on a networked development machine or CI.

## Local preflight

Run:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
bash scripts/preflight_local_build.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\bootstrap_gradle_wrapper.ps1
```

Then in Android Studio use **Gradle JDK 17**, install Android SDK Platform 36 and Build Tools 36.0.0, sync, and run the canonical build chain:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

For release validation:

```bash
./gradlew --no-daemon lintRelease bundleRelease
```

The release task requires the four `RELEASE_*` Gradle signing properties used by `app/build.gradle.kts`.

## Validation performed in this audit environment

- 50/50 Python source-contract verifiers: PASS after stale-verifier alignment.
- Stage 25.4 conversation context self-test: PASS.
- Stage 25.6 network policy self-test: PASS.
- Stage 25.3 sensitive-field self-test: PASS.
- Stage 24.3 settings navigation self-test: PASS.
- Stage 24.1 bubble-flight self-tests: PASS.
- Stage 24.2 theme-bubble self-test: PASS.
- Stage 25.5 Web navigation policy self-test: PASS.
- Release verifier: PASS.
- Full Gradle compile/lint/APK/AAB was not executed in this container because it has no Android SDK and cannot fetch the missing wrapper/distribution from the required hosts.

## Recommended next modernization stage

Do not mix dependency modernization into this build-readiness patch. First obtain a green Gradle/Android Studio baseline. Then migrate Room annotation processing from legacy KAPT to KSP, and update AndroidX/OkHttp dependencies in small batches with unit, lint, emulator and release-R8 coverage after each batch.
