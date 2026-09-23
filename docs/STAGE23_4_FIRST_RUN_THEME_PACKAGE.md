# Typing Core v2 Stage 23.4 - First-Run Theme Package Selection

Stage 23.4 adds a required first-run Theme Package chooser on top of the Stage 23.3 source without changing typing, AI, privacy, backend, account, quota, insertion, or Bubble Key behavior.

## User flow

1. The launcher opens `ThemeOnboardingActivity`.
2. On a fresh install, the user chooses one of the four existing complete packages: Classic Dark, Glass Modern, Clean Light, or Gradient Pro.
3. The selection is persisted atomically as the global package and all per-surface overrides are cleared so the complete package is consistent across the keyboard.
4. The user is sent to the existing main setup/settings screen.
5. Future launches skip the chooser after the first selection.
6. The complete package can be changed later from `Customize Keyboard Theme`. Selecting a complete package again clears any older per-surface overrides.

## Surface coverage

The package resolves the coordinated catalog variants for English, Number, Symbol, Bangla, Phonetic, Bijoy, Settings, toolbar/global chrome, suggestions, and AI chrome. Existing optional advanced custom controls remain available after onboarding.

## Migration behavior

An installation that already has a Stage 23.2+ global Theme Pack stored is treated as already onboarded. A fresh install has no global package and must choose one. Legacy/custom-only installations may see the one-time chooser after upgrading; after completion, legacy/custom controls remain available manually.

## Verification

Run:

```bash
python3 scripts/verify_theme_stage23_4.py
python3 scripts/verify_theme_stage23_2.py
python3 scripts/verify_typing_stage23_3.py
```

Then run the canonical Android build when Gradle/Android SDK dependencies are available:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```
