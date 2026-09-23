# Stage 23.9 Verification Report

## Scope

Premium Settings Theme Packs and two-step first-run theme onboarding.

## Verified contracts

- Four Settings Theme Packs exist: Clean Modern, Card Style, Premium, Pro Style.
- Settings theme state is persisted in a DataStore independent of keyboard theme state.
- Fresh setup flows Keyboard Theme -> Settings Theme -> Main Settings.
- Existing installs with completed keyboard-theme setup but no settings-theme selection are routed through the new Settings Theme step once.
- Manual `Change Settings Theme` does not mutate the keyboard layout Theme Pack.
- Current keyboard Theme Pack only drives a non-forcing recommendation badge.
- Complete Main Settings sections retain their existing IDs and are styled without replacing control/event bindings.
- Screenshot-inspired category dashboard variants are implemented for all four packs.
- Android resource XML and manifest parse successfully.
- Stage 23.8 alphabetic number-row, Stage 23.7/23.5 numeric pad, Stage 23.4/23.2 theme, Stage 23.3 Bubble Key, release, backend config, and training parity regressions remain green.

## Results

- Stage 23.9 static contract: 51/51 PASS
- Stage 23.9 pure Kotlin pack/recommendation self-test: 9/9 PASS
- Stage 23.8 alphabetic number row: 35/35 PASS
- Stage 23.7 numeric no-gap: 31/31 PASS
- Stage 23.5 numeric pad: 30/30 PASS
- Stage 23.4 first-run keyboard theme: 11/11 PASS
- Stage 23.2 multi-theme: 53/53 PASS
- Stage 23.3 Bubble Key: 32/32 PASS
- Release verification: PASS
- Shared backend configuration: PASS
- Training parity: PASS

## Build limitation in this environment

The archive pins the verified Gradle wrapper JAR checksum but does not vendor the actual `gradle-wrapper.jar`, and Gradle 9.6.0 is not installed in this execution environment. Therefore full `testDebugUnitTest`, `lintDebug`, and `assembleDebug` must be run in Android Studio/CI after `scripts/bootstrap_gradle_wrapper.sh` restores the verified wrapper.
