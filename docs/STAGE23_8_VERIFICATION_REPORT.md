# Stage 23.8 Verification Report

Scope: Settings-controlled optional alphabetic number row for English, Bangla Phonetic and Bangla Bijoy across all four Theme Packages, with no duplicate row on dedicated Number/Symbol surfaces.

## Results

- Stage 23.8 static contract: PASS (35/35)
- Stage 23.8 Kotlin layout/policy self-test: PASS (22/22)
- Stage 12 Kotlin layout regression: PASS (6/6)
- Stage 6 typing verification: PASS
- Stage 12 typing verification: PASS
- Stage 23.7 numeric no-gap regression: PASS (31/31)
- Stage 23.2 premium multi-theme regression: PASS (53/53)
- Stage 23.3 Bubble Key regression: PASS (32/32)
- Stage 23.4 first-run theme regression: PASS (11/11)
- Release verification: PASS
- Shared backend verification: PASS
- Training parity verification: PASS
- Android resource XML parse: PASS (17/17)

## Persistence and live rendering

The implementation preserves the existing `show_number_row` DataStore key, so an existing user's ON/OFF choice survives upgrade. The IME settings collector rerenders the active key panel when the preference changes, and both the keyboard render signature and layout cache request already include `showNumberRow`, preventing stale layouts.

## Build environment note

The source archive contains `gradle-wrapper.properties` and the expected wrapper-JAR checksum, but does not contain the actual verified `gradle-wrapper.jar`. Therefore `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug` cannot run in this archive-only environment and exits before Gradle starts with `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`.
