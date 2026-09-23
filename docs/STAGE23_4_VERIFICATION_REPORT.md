# Stage 23.4 Verification Report

Date: 2026-09-22
Baseline: Social AI Keyboard v35.3.1 / Typing Core v2 / Stage 23.3 Bubble Key
Update: First-Run Theme Package Selection

## Implemented

- Added `ThemeOnboardingActivity` as the launcher route.
- Fresh installs must choose one of the four existing complete Theme Packages before entering the main settings flow.
- Added persisted first-run completion state in the theme DataStore contract.
- Existing installations with an already selected premium global pack are treated as already onboarded.
- Complete package selection clears per-surface overrides so the chosen package is applied consistently.
- Manual complete-package switching from Theme Settings uses the same all-surface behavior.
- Existing optional per-surface overrides, advanced custom appearance, legacy themes, background photos, typing, AI, privacy, account, quota, and Bubble Key behavior remain available.
- Main settings exposes `Change Theme Package / Customize` for later manual switching.

## Verification results

- `scripts/theme_stage23_4_first_run_selftest.kt`: PASS.
- `python3 scripts/verify_theme_stage23_4.py`: 11/11 PASS.
- `python3 scripts/verify_theme_stage23_2.py`: 53/53 PASS after updating the complete-package contract to clear overrides.
- `scripts/theme_stage23_2_multitheme_selftest.kt`: PASS.
- `scripts/typing_stage23_3_bubble_key_selftest.kt`: 20/20 PASS.
- `python3 scripts/verify_typing_stage23_3.py`: 32/32 PASS.
- `scripts/theme_core_selftest.kt`: PASS.
- `scripts/theme_codec_selftest.kt`: PASS.
- `python3 scripts/verify_release_ready.py`: PASS.
- Android manifest/resource XML parse: PASS.

## Full Android build status

A full Gradle unit-test/lint/APK build was not executed in this environment because the verified `gradle-wrapper.jar` is intentionally not vendored and Gradle 9.6.0 is not installed on PATH. `scripts/check_gradle_wrapper_completeness.py` correctly reports `BOOTSTRAP REQUIRED`.

Before shipping, run with JDK 17, Android SDK 36, and Gradle 9.6.0:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```
