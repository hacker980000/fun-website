# Stage 23.2 — Premium Multi-Theme UI

Stage 23.2 adds the approved premium multi-theme presentation system on top of the Stage 23.1 Android Studio build-fix baseline. It does not change typing intelligence, prediction/autocorrect policy, AI/backend contracts, sensitive-field safety, or Stage 23 evidence semantics.

## Theme packs and surfaces

Four complete premium theme packs are built in:

1. Classic Dark
2. Glass Modern
3. Clean Light
4. Gradient Pro

Each pack resolves seven catalog surfaces: English, Number, Symbol, Bangla family, Phonetic, Bijoy, and Settings. This produces 28 deterministic surface variants. The Bangla family entry is a catalog/preview design family; runtime Bangla letter typing continues to use the existing Phonetic or Bijoy modes and no new typing layout is introduced.

## Global pack + per-menu customization

A user may apply one complete Theme Pack globally or independently override any of the seven catalog surfaces. The global pack remains the source of truth for global chrome: toolbar, suggestions/header chrome, AI panels/actions, emoji/clipboard utility surfaces, and other global controls. Per-menu overrides only change the active keyboard/settings surface that they target.

## Settings experience

Theme Settings now exposes:

- Complete Theme Packs with live mini previews.
- Customize Each Keyboard with `Use Global Pack Design` plus four pack-specific choices per surface.
- Advanced Custom Appearance with the existing color, glow, glass, radius, spacing, font and background-photo controls.
- Previous / Legacy Themes with all eight pre-existing presets retained.
- A product-level Reset Theme flow.

## Reset flow

Reset is intentionally two-step and never forces a predefined pack:

1. Tap Reset Theme and confirm intent.
2. A manual picker displays Classic Dark, Glass Modern, Clean Light and Gradient Pro.
3. No repository mutation occurs while the picker is merely open or if it is dismissed.
4. Only after a pack is selected does one reset transaction apply that pack, clear all per-surface overrides, reset custom appearance/background configuration, persist the new state, and return the old background filename for safe deletion.

## Persistence and migration

The existing `social_ai_theme_settings` DataStore remains in use. New pack/schema/override keys are additive. If an older installation has no premium selection keys, its legacy active theme, custom colors and background configuration are decoded and preserved instead of being replaced by a premium pack. Advanced custom appearance/background edits enter the existing custom global mode while preserving any per-surface overrides, so editing appearance does not silently destroy menu-specific choices. Explicit legacy-preset activation remains available as a separate compatibility mode.

## Runtime isolation

The IME keeps separate resolved themes:

- `currentGlobalChromeTheme` for toolbar, suggestions, AI and global utility surfaces.
- `currentSurfaceTheme` for the active English/Number/Symbol/Phonetic/Bijoy key surface.

Changing language/layer/Bangla mode re-resolves only the active surface theme and renders the existing layout. It does not restart input, reload dictionaries, request AI, or modify learning/prediction engines.

## Visual rendering

`KeyboardTheme` now supports solid, glass and gradient fill tokens while keeping all existing legacy presets valid. The renderer has separate global-chrome and key-surface application paths. Theme previews are surface-aware so English, numeric, symbol, Bangla-family, phonetic, Bijoy and Settings previews use appropriate sample content.

## Verification entry points

Run the Android-free source contract and theme self-tests with:

```bash
python3 scripts/verify_theme_stage23_2.py
python3 scripts/verify_typing_stage23.py
python3 scripts/stage23_evidence_parser_selftest.py
```

With the verified Gradle wrapper JAR, JDK 17 and Android SDK installed, also run:

```bash
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug
```

See `docs/STAGE23_2_VERIFICATION_REPORT.md` for the evidence captured during this implementation.
