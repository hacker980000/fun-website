# Typing Core v2 Stage 23.3 — Bubble Key Verification Report

Date: 2026-09-22  
Baseline: `Social_AI_Keyboard_v35.3.1_TYPING_CORE_V2_STAGE23_2_PREMIUM_MULTI_THEME_UI_SOURCE.zip`

## Scope verified

- Free optional Bubble Key setting, default Off.
- Persisted Soft / Normal / Playful intensity profiles.
- Alphabetic tap-only eligibility across English/Phonetic/Bijoy letter surfaces.
- Number/symbol/quick-word/non-letter exclusion.
- Sensitive-field suppression.
- Glide gesture suppression while preserving ordinary taps when Glide is enabled.
- Android animation-disable respect.
- Theme-aware bounded overlay renderer with maximum 8 simultaneous bubbles.
- Lifecycle cleanup and live disable cleanup.
- Stage 23.2 Theme Pack / per-menu override architecture preserved.

## Fresh local verification completed

- Stage 23.3 Bubble Key Android-free policy self-test: **20/20 PASS**.
- Stage 23.3 Bubble Key static/source contract: **32/32 PASS**.
- Stage 19 touch/responsiveness Android-free self-test: **33/33 PASS**.
- Stage 19 static verification: **16/16 PASS**.
- Stage 21 static verification: **25/25 PASS**.
- Stage 22 static verification: **31/31 PASS**.
- Stage 23 real-device-evidence static verification: **36/36 PASS**.
- Stage 23.2 premium multi-theme static verification: **53/53 PASS**.
- Stage 18 static verification: **11/11 PASS**.
- Stage 20 static verification: **20/20 PASS**.
- Stage 23 evidence parser self-test: **25/25 PASS**.
- Theme core self-test: **PASS**.
- Theme codec self-test: **PASS**.
- Release-ready verification: **PASS**.
- Shared backend configuration verification: **PASS**.
- Training parity verification: **PASS**.
- Emoji panel UI verification: **PASS**.
- Protected `ai/`, `backend/`, `context/`, `safety/` directories compared with the Stage 23.2 baseline: **byte-identical / PASS**.
- `Stage21PredictionProfiling.kt` and `Stage22PredictionConfidence.kt` compared with the Stage 23.2 baseline: **byte-identical / PASS**.

## TDD evidence

The Bubble Key policy self-test was executed before the production types existed and failed with unresolved `BubbleKeyPolicy`, `BubbleKeyRequest` and `BubbleKeyIntensity` references. After the minimal policy implementation it passed **20/20**.

The integration/static contract was executed before settings/UI/overlay/service wiring and passed only **4/32**. After implementation it passed **32/32**.

A follow-up wiring test deliberately required a started Glide drag to be distinguished from a tap. It failed **31/32** before the wiring fix, then passed **32/32** after `session?.dragging == true` was passed into the policy.

## Android build boundary

The canonical command was attempted:

```bash
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug
```

It could not start because this source archive/environment does not contain the verified `gradle-wrapper.jar` and no system Gradle/Android SDK is installed here. The observed failure was:

```text
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

Therefore this report does **not** claim an Android APK build or physical-device animation result. Complete that final step in Android Studio with JDK 17 / SDK 36 and the verified Gradle 9.6 wrapper, then run the manual checklist in `docs/STAGE23_3_BUBBLE_KEY.md`.
