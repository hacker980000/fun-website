# Stage 24.1 Verification Report — Bubble Flight to Caret

Date: 2026-09-22

## Automated evidence collected in this execution environment

| Check | Result | Evidence |
|---|---|---|
| Stage 24.1 static source contract | PASS | `python3 scripts/verify_typing_stage24_1.py` → 48/48 |
| Stage 24.1 Android-free Kotlin policy/resolver self-tests | PASS | `bash scripts/run_typing_stage24_1_bubble_flight_selftest.sh` → policy/resolver 8/8 + dispatch-refresh 4/4 |
| Stage 23.3 Bubble Key regression | PASS | `python3 scripts/verify_typing_stage23_3.py` → 32/32 |
| Stage 23 baseline static regression | PASS | `python3 scripts/verify_typing_stage23.py` → 36/36 |
| Stage 23.2 multi-theme regression | PASS | `python3 scripts/verify_theme_stage23_2.py` → 53/53 |
| Stage 23.7 numeric layout regression | PASS | `python3 scripts/verify_numberpad_stage23_7.py` → 31/31 |
| Stage 23.8 alphabetic number-row regression | PASS | `python3 scripts/verify_typing_stage23_8.py` → 35/35 |
| Stage 23.9 settings-theme regression | PASS | `python3 scripts/verify_settings_theme_stage23_9.py` → 57/57 |
| Stage 24.0 icon/key-boundary regression | PASS | `python3 scripts/verify_theme_stage24_0.py` → 22/22 |
| Release hardening static gate | PASS | `python3 scripts/verify_release_ready.py` |
| Shared backend static gate | PASS | `python3 scripts/verify_shared_backend_config.py` |
| Gradle unit suite | PENDING — environment blocker | `./gradlew testDebugUnitTest` exits 1 because `org.gradle.wrapper.GradleWrapperMain` is unavailable; verified `gradle-wrapper.jar` is not vendored |
| Lint + debug APK | PENDING — environment blocker | `./gradlew lintDebug assembleDebug` exits 1 for the same missing wrapper JAR |
| Physical-device acceptance | NOT EXECUTED | This container has no `adb` executable/device connection |

`python3 scripts/check_gradle_wrapper_completeness.py` reports that the verified wrapper JAR must be bootstrapped with Gradle 9.6.0. No Android build success is claimed by this report.

## Physical-device acceptance checklist

Run this checklist after Android Studio/CI can produce and install the debug APK. Record device model and Android/API level before testing.

| # | Scenario | Expected result | Current status |
|---|---|---|---|
| 1 | Normal multiline editor, Bubble Key ON, tap `a` | Text appears immediately; bubble starts at physical `a` key and ends at caret/typed character | PENDING DEVICE |
| 2 | Bangla Phonetic/Bijoy alphabetic tap | Same exact key-origin to caret behavior | PENDING DEVICE |
| 3 | Soft / Normal / Playful | Motion character changes; source/destination rules remain identical | PENDING DEVICE |
| 4 | Rapidly type 12+ letters | No keyboard lag/crash; at most 8 bubbles visible concurrently | PENDING DEVICE |
| 5 | Disable accessibility service | Text remains immediate; Bubble Key falls back locally from pressed key | PENDING DEVICE |
| 6 | Password/OTP/payment-sensitive field | No bubble flight is emitted | PENDING DEVICE |
| 7 | Switch editor/app during active flight | Old flights are cancelled and do not jump into new field | PENDING DEVICE |
| 8 | Classic Dark / Glass Modern / Clean Light / Gradient Pro | Same flight geometry with theme-specific bubble styling | PENDING DEVICE |

## Android Studio commands

After restoring the verified wrapper JAR and installing the required Android SDK components:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
./gradlew testDebugUnitTest
./gradlew lintDebug assembleDebug
```

Then install `app/build/outputs/apk/debug/app-debug.apk`, enable Social AI Keyboard and its existing accessibility service, and complete the device checklist above.
