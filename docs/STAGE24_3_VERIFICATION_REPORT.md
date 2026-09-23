# Stage 24.3 Verification Report - Dedicated Settings Category Navigation

Date: 2026-09-22

## Automated evidence collected in this execution environment

| Check | Result | Evidence |
|---|---|---|
| Stage 24.3 pure-Kotlin category policy | PASS | `bash scripts/run_settings_navigation_stage24_3_selftest.sh` -> 8/8 |
| Stage 24.3 final static/navigation contract | PASS | `python3 scripts/verify_settings_navigation_stage24_3.py` -> 158/158 |
| Android XML parse | PASS | all `app/src/main/res/**/*.xml` plus manifest -> 34/34 |
| Stage 23.9 Settings Theme regression | PASS | `python3 scripts/verify_settings_theme_stage23_9.py` -> 66/66 |
| Stage 24.0 launcher/key-boundary regression | PASS | `python3 scripts/verify_theme_stage24_0.py` -> 22/22 |
| Stage 24.1 Bubble Flight static regression | PASS | `python3 scripts/verify_typing_stage24_1.py` -> 48/48 |
| Stage 24.2 optional per-theme Bubble regression | PASS | `python3 scripts/verify_theme_stage24_2_optional_bubble.py` -> 30/30 |
| Stage 24.2 pure-Kotlin per-theme Bubble policy | PASS | `bash scripts/run_typing_stage24_2_theme_bubble_selftest.sh` -> 8/8 |
| Stage 23.8 alphabetic Number Row regression | PASS | `python3 scripts/verify_typing_stage23_8.py` -> 35/35 |
| Stage 23.7 numeric layout regression | PASS | `python3 scripts/verify_numberpad_stage23_7.py` -> 31/31 |
| Release hardening gate | PASS | `python3 scripts/verify_release_ready.py` |
| Shared backend configuration gate | PASS | `python3 scripts/verify_shared_backend_config.py` |
| Gradle wrapper completeness | BLOCKED / BOOTSTRAP REQUIRED | `python3 scripts/check_gradle_wrapper_completeness.py` reports verified `gradle-wrapper.jar` is not vendored |
| Gradle unit tests / lint / debug APK | NOT EXECUTED | Wrapper blocker above; no compilation or APK success is claimed |
| Physical-device acceptance | NOT EXECUTED | No `adb` executable/device is available in this execution environment |

## Stage 24.3 acceptance coverage

The final Stage 24.3 verifier checks that:

- `SettingsCategoryId` contains exactly eight categories with eight unique wire values;
- all four Settings Theme dashboard paths consume the same canonical category list;
- `MainActivity` contains no `smoothScrollTo()` and no old `section_*` category routing;
- the Hub XML contains no migrated category controls;
- `SettingsCategoryActivity` is manifest-registered, has an intent factory, normal back handling, and invalid-category fallback;
- all eight dedicated layouts exist and own their expected setting controls;
- the legacy AI & Privacy constants remain and resolve to `AI_PRIVACY`;
- Number Row remains global while Key Boundary and Bubble settings use per-keyboard-theme APIs;
- category `onResume()` reapplies the selected Settings Theme before state refresh;
- `SettingsCategoryActivity` introduces no raw SharedPreferences/DataStore preference keys.

## Gradle build blocker

The exact wrapper check result in this environment is:

```text
GRADLE WRAPPER COMPLETENESS: BOOTSTRAP REQUIRED - verified wrapper JAR is not vendored
- run: bash scripts/bootstrap_gradle_wrapper.sh with Gradle 9.6.0 on PATH
```

After a verified Gradle 9.6.0 wrapper is restored, run:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
./gradlew testDebugUnitTest lintDebug assembleDebug
```

A successful build should produce `app/build/outputs/apk/debug/app-debug.apk`. This report does not claim that result until those commands execute successfully.

## Physical-device acceptance checklist

Run after the debug APK can be built and installed. Record device model and Android/API version.

| # | Scenario | Expected result | Current status |
|---|---|---|---|
| 1 | In each of Clean Modern, Card Style, Premium, and Pro Style, open all eight Hub categories | Every category opens its dedicated page; category set/order remains consistent across themes | PENDING DEVICE |
| 2 | Use header back arrow and Android system back/gesture | Returns to Settings Hub without losing saved values | PENDING DEVICE |
| 3 | Trigger the IME legacy AI & Privacy deep link | Opens AI & Privacy dedicated page directly, without Hub scrolling | PENDING DEVICE |
| 4 | Toggle representative settings in every category, close/relaunch Settings | Existing repository-backed values persist | PENDING DEVICE |
| 5 | From Theme & Appearance, change the Settings Theme, then return | Current category is restyled to the new Settings Theme and values are unchanged | PENDING DEVICE |
| 6 | Switch keyboard Theme Package and inspect Theme & Appearance | Key Boundary and Bubble Effect/Style update to that keyboard pack's stored values; Number Row remains global | PENDING DEVICE |
| 7 | Send an invalid category value with `adb` or a debug helper, if practical | No blank category page; safe back-to-caller or Hub fallback | PENDING DEVICE |

## Final evidence rule

The completion claim for Stage 24.3 is limited to the source/static/pure-Kotlin/XML/regression evidence above. Android compilation, lint, APK installation, and physical UI behavior remain pending until the wrapper and device environment are available.
