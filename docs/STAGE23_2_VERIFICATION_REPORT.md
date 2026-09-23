# Stage 23.2 Verification Report

## Scope

Verified source: Social AI Keyboard v35.3.1 / Typing Core v2 / Stage 23.2 Premium Multi-Theme UI, based on the Stage 23.1 Android Studio build-fix source.

## Executed Android-free verification

The following commands were executed in this implementation environment and passed:

- `python3 scripts/verify_typing_stage5.py` — PASS.
- `python3 scripts/verify_typing_stage7.py` — PASS.
- `python3 scripts/verify_typing_stage18.py` — 11/11 PASS.
- `python3 scripts/verify_typing_stage19.py` — 16/16 PASS.
- `python3 scripts/verify_typing_stage20.py` — 20/20 PASS.
- `python3 scripts/verify_typing_stage21.py` — 25/25 PASS.
- `python3 scripts/verify_typing_stage22.py` — 31/31 PASS.
- `python3 scripts/verify_typing_stage23.py` — 36/36 PASS.
- `python3 scripts/stage23_evidence_parser_selftest.py` — 25/25 PASS.
- `python3 scripts/verify_theme_stage23_2.py` — 53/53 PASS.
- `scripts/theme_core_selftest.kt` compiled with `kotlinc` and ran — PASS.
- `scripts/theme_codec_selftest.kt` compiled with `kotlinc` and ran — PASS.
- `scripts/theme_stage23_2_multitheme_selftest.kt` compiled with `kotlinc` and ran — PASS, including four packs, seven surfaces, 28 unique valid variants, migration/background preservation, global/override round trips, override clearing, advanced-custom preservation of per-surface overrides, and invalid-value fallback.
- `ImeThemeSurfaceResolver` Android-free probe — 5/5 mode mappings PASS.

## Android/Gradle build status in this environment

The command below was executed:

```bash
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug
```

It did **not** run the Android build because the extracted source does not contain the verified `gradle-wrapper.jar` and this container has no usable system Gradle/Android SDK. The observed error was:

```text
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

Therefore this report does not claim a Gradle, Robolectric, APK, emulator, or physical-device PASS. Those checks must be completed in Android Studio after restoring/bootstraping the verified wrapper and syncing the Android toolchain.

## Protected-code comparison

Compared byte-for-byte/directory-recursively against the Stage 23.1 baseline:

- `ai/` — identical.
- `backend/` — identical.
- `context/` — identical.
- `safety/` — identical.
- `ime/Stage21PredictionProfiling.kt` — identical.
- `ime/Stage22PredictionConfidence.kt` — identical.

The IME service itself changes only for theme selection/resolution/render routing; the Stage 19 no-debounce text-key contract remains covered by the Stage 19 verifier.

## Manual device smoke status

Not executed in this container because no Android device/emulator/ADB environment is available. In Android Studio, perform the documented Stage 23.2 sequence: global Glass Modern, independent English/Phonetic overrides, switch through English/Phonetic/Bijoy/Number/Symbol, verify global toolbar/AI consistency, exercise Reset cancel/dismiss/selection paths, reopen for persistence, and recheck password/OTP AI/context blocking.

## Packaging note

The final source ZIP is produced after this report and its SHA-256 is delivered alongside the archive. The archive cannot contain a report that truthfully embeds the hash of that same final archive without creating a self-referential hash cycle, so the final SHA-256 is recorded in the delivery response and adjacent `.sha256` file instead.
