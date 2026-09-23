# Social AI Keyboard Premium Multi-Theme UI — Design Specification

**Project:** Social AI Keyboard v35.3.1 / Typing Core v2  
**Baseline:** Stage 23.1 Android Studio Build Fix  
**Design status:** Approved  
**Date:** 2026-09-22

## 1. Goal

Add a premium, easy-to-use multi-theme system that keeps all four approved design families inside the app and supports both one-tap complete theme packs and per-keyboard-surface customization, while preserving the existing custom appearance editor, legacy themes, user data, typing behavior, AI/backend behavior, privacy rules, safety restrictions, prediction quality, Glide, and Stage 23 evidence tooling.

The system must feel visually consistent even when users mix designs. To achieve that, toolbar, suggestion/header chrome, AI panel, AI action buttons, and other global utility surfaces always follow the selected **Global Theme Pack**. Per-surface overrides affect only the relevant keyboard/menu surface.

## 2. Approved Theme Families

Four complete theme packs are locked:

1. **Classic Dark**
2. **Glass Modern**
3. **Clean Light**
4. **Gradient Pro**

Each pack contains a matching design for seven surfaces:

- English
- Number
- Symbol
- বাংলা
- Phonetic
- Bijoy
- Settings

This produces **28 resolved surface variants** total.

### 2.1 Pack-to-surface naming

| Global Pack | English | Number | Symbol | বাংলা | Phonetic | Bijoy | Settings |
|---|---|---|---|---|---|---|---|
| Classic Dark | Classic Dark | Classic Dark | Standard Dark | Classic Dark | Dark Classic | Classic Bijoy | Clean Modern |
| Glass Modern | Glass Modern | Rounded Modern | Rounded Modern | Rounded Modern | Rounded Modern | Modern Bijoy | Card Style |
| Clean Light | Clean Light | Clean Light | Light Clean | Clean Light | Light Clean | Light Bijoy | Premium |
| Gradient Pro | Gradient Pro | Stylish Pro | Pro Style | Elegant Pro | Stylish Pro | Pro Bijoy | Pro Style |

The display names may vary by surface as shown above, but all seven variants remain members of the same global visual family.

## 3. User Experience

### 3.1 Appearance & Themes entry screen

The redesigned Theme Settings screen is organized in this order:

1. **Complete Theme Packs**
   - Four premium pack cards.
   - Each card includes a compact keyboard preview and active-state indicator.
   - Tapping a pack applies it immediately as the global pack.
   - Per-surface overrides are not deleted merely by switching the global pack; overrides continue to override their corresponding surfaces until the user clears them.

2. **Customize Each Keyboard**
   - Seven surface cards: English, Number, Symbol, বাংলা, Phonetic, Bijoy, Settings.
   - Opening a surface shows five choices:
     - Use Global Pack Design
     - Theme Family 1 variant
     - Theme Family 2 variant
     - Theme Family 3 variant
     - Theme Family 4 variant
   - Choosing a specific design creates/updates that surface override.
   - Choosing **Use Global Pack Design** removes that surface override.

3. **Advanced Custom Appearance**
   - Existing custom color, background, glow, glass opacity, key radius, spacing, and font controls remain available.
   - Existing behavior is retained unless a change is explicitly required by this specification.

4. **Previous / Legacy Themes**
   - Existing built-in themes remain available in a collapsed/secondary section.
   - Current legacy users must not lose their active theme or custom data on upgrade.

5. **Reset Theme**
   - Reset does not force a predefined theme.
   - Reset starts a deliberate two-step flow described in Section 6.

### 3.2 Live application

Theme changes apply live. No normal text content is modified, no active InputConnection is replaced, and no dictionary/prediction engine restart is required solely for a visual theme change.

### 3.3 Visual consistency rule

Global visual chrome always follows the active Global Theme Pack:

- Toolbar
- Suggestion/header chrome
- AI panel
- AI action buttons
- Global utility panels

Per-surface overrides apply only to the relevant key/menu surface.

Example:

- Global pack = Glass Modern
- English override = Clean Light
- বাংলা override = Elegant Pro

Result:

- Toolbar / AI / suggestion chrome = Glass Modern
- English keys = Clean Light
- বাংলা keys = Elegant Pro

## 4. Architecture

### 4.1 New domain models

Add focused theme-selection types rather than expanding one monolithic theme object.

```text
ThemePack
  CLASSIC_DARK
  GLASS_MODERN
  CLEAN_LIGHT
  GRADIENT_PRO

KeyboardThemeSurface
  ENGLISH
  NUMBER
  SYMBOL
  BANGLA
  PHONETIC
  BIJOY
  SETTINGS

ThemeSelectionState
  globalPack
  perSurfaceOverrides
  legacy/custom compatibility state
```

### 4.2 Catalog and resolution

Add a `ThemeCatalog` that owns the four pack families and the 28 surface-specific variants.

Add a `ThemeResolutionPolicy` responsible for resolving visual state:

```text
resolveGlobalChrome(state)
    -> design associated with state.globalPack

resolveSurface(state, surface)
    -> explicit override for surface, if present
    -> otherwise matching variant from state.globalPack
```

Global chrome resolution must never inspect a per-surface override.

### 4.3 Existing KeyboardTheme

`KeyboardTheme` remains the lower-level rendered visual token model. Theme packs and surface variants resolve into concrete `KeyboardTheme` values rather than duplicating renderer logic.

Existing `ThemeRenderer`, `NeonDrawableFactory`, background rendering, and custom appearance behaviors should be reused where possible.

### 4.4 Repository responsibilities

`ThemeRepository` evolves from a single-active-theme repository into a backward-compatible theme selection repository.

Responsibilities:

- Expose current `ThemeSelectionState`.
- Resolve global chrome theme.
- Resolve current theme for a requested surface.
- Apply a global pack.
- Set/clear a per-surface override.
- Preserve and expose legacy/custom theme state.
- Preserve existing background file references during normal migration.
- Execute reset transaction only after the user chooses the target pack.

The repository must not contain UI-specific dialog logic.

## 5. Persistence and Migration

### 5.1 DataStore continuity

Continue using the existing `social_ai_theme_settings` DataStore. New keys must be additive and backward-compatible.

Suggested logical keys:

```text
THEME_GLOBAL_PACK_ID
THEME_OVERRIDE_ENGLISH
THEME_OVERRIDE_NUMBER
THEME_OVERRIDE_SYMBOL
THEME_OVERRIDE_BANGLA
THEME_OVERRIDE_PHONETIC
THEME_OVERRIDE_BIJOY
THEME_OVERRIDE_SETTINGS
THEME_SELECTION_SCHEMA_VERSION
```

Existing keys such as `ACTIVE_THEME_ID`, custom theme values, and background configuration remain readable.

### 5.2 Existing-install migration

If new pack keys are absent:

- Do not silently replace an existing active legacy theme.
- Preserve existing custom/legacy appearance and background data.
- The user may continue using legacy/custom mode until they explicitly select a new premium pack or complete a reset flow.

A migration must never delete or overwrite an imported background file.

### 5.3 Fresh install

Fresh-install behavior must not rely on reset semantics. The product may present a normal default visual state already defined by the app, but reset itself must never force that default. The implementation plan may preserve the current app default for fresh installs unless a later product decision explicitly changes it.

## 6. Reset Theme Flow — Final Locked Behavior

The previous idea of automatically resetting to Glass Modern is cancelled.

Final reset flow:

1. User taps **Reset Theme**.
2. Show confirmation explaining that per-surface overrides and advanced custom appearance will be cleared.
3. If the user cancels, make **no changes**.
4. If the user confirms, open a pack picker showing:
   - Classic Dark
   - Glass Modern
   - Clean Light
   - Gradient Pro
5. No state is cleared yet.
6. User manually selects one pack.
7. Only after a pack is selected, perform one reset transaction:
   - Set selected pack as Global Theme Pack.
   - Clear all seven per-surface overrides.
   - Reset advanced custom appearance values to the selected pack's canonical values.
   - Clear active custom-theme selection state.
   - Clear background configuration.
   - Remove the app-private background photo file only after preference persistence succeeds, so a failed preference transaction cannot destroy the user's previous file.
8. Apply the selected pack live.
9. If the pack picker is dismissed without a selection, make **no changes**.

This guarantees there is never a temporary "no theme" state and no predefined pack is forced by reset.

## 7. Settings Theme Surface

`SETTINGS` is a first-class customizable surface. Its four variants should change the Settings screen presentation (card treatment, section containers, accents, typography hierarchy, control surfaces) while preserving accessibility and functional layout.

The Settings screen itself still uses global chrome rules for any shared top-level app chrome. The settings content design resolves through the `SETTINGS` surface override or matching global pack variant.

## 8. Preview System

Upgrade `ThemePreviewView` to accept both a theme and a `KeyboardThemeSurface`.

Preview content must match the selected surface:

- English -> English QWERTY sample
- Number -> Numeric layout sample
- Symbol -> Symbol rows sample
- বাংলা -> Bengali key sample
- Phonetic -> Latin QWERTY with phonetic/Bangla suggestion treatment
- Bijoy -> Bijoy Bengali layout sample
- Settings -> Settings-card/control sample

Complete pack cards may use a compact representative preview, but per-surface selector previews must show the actual surface type.

## 9. Visual Design Requirements

The four families must be distinguishable by more than color substitution. Variants may differ in:

- key shape
- corner radius
- key spacing
- surface depth
- border strength
- glass opacity
- shadow/glow intensity
- label hierarchy
- special-key treatment
- enter/space styling
- mode-switch styling
- accent treatment

All designs must remain easy to read and touch-friendly. Existing Stage 19 touch target and responsiveness rules remain authoritative.

## 10. IME Integration

`SocialAiInputMethodService` should hold/use two resolved visual concepts:

```text
globalChromeTheme
activeKeyboardSurfaceTheme
```

When the keyboard mode changes:

- Resolve only the active surface theme as needed.
- Keep global chrome tied to Global Theme Pack.
- Do not reset composing text.
- Do not recreate or replace InputConnection solely for theme changes.
- Do not reload dictionary/learning models solely for theme changes.
- Do not issue backend/AI calls solely for theme changes.

If a surface design changes geometry-sensitive values such as key gap/radius, rebuild only the necessary visual/layout layer; do not rebuild unrelated engines.

## 11. Performance Constraints

Theme work must stay outside the normal text-key critical path.

Requirements:

- Immutable/predefined catalog entries where practical.
- Cache/reuse renderer resources where current architecture already supports it.
- Persist theme selection only on explicit user changes.
- No network requirement for normal theme selection.
- No prediction/model reload when a visual-only value changes.
- No new debounce on normal text keys.
- Preserve Stage 19/20/23 responsiveness and evidence behavior.

## 12. Safety and Isolation

The implementation must not alter behavior in:

- `ai/`
- `backend/`
- `context/`
- `safety/`

Prediction/autocorrect behavior must remain unchanged, including Stage 21 ranking/profiling and Stage 22 confidence-aware autocorrect.

Theme state must never weaken sensitive editor restrictions or change whether context/learning/AI is allowed.

## 13. Expected Code Areas

Likely modified files/components:

- `theme/KeyboardTheme.kt`
- `theme/ThemePreset.kt`
- `theme/ThemeRepository.kt`
- `theme/ThemePreferencesCodec.kt`
- `theme/ThemeRenderer.kt`
- `theme/ThemePreviewView.kt`
- `ThemeSettingsActivity.kt`
- Theme settings layout/resources
- `ime/SocialAiInputMethodService.kt`

New focused files are expected, for example:

- `theme/ThemePack.kt`
- `theme/KeyboardThemeSurface.kt`
- `theme/ThemeSelectionState.kt`
- `theme/ThemeCatalog.kt`
- `theme/ThemeResolutionPolicy.kt`

Exact filenames may be adjusted during planning if the repository's existing conventions make a different split cleaner, but the responsibilities above remain fixed.

## 14. Testing Strategy

### 14.1 New theme tests

At minimum verify:

- Exactly 4 premium packs exist.
- Exactly 7 supported surfaces exist.
- Every pack resolves all 7 surfaces (28 combinations).
- Global pack switching resolves expected global chrome.
- Per-surface override affects only its surface.
- `Use Global Pack Design` clears only the selected surface override.
- Toolbar always follows global pack.
- AI panel always follows global pack.
- Suggestion/header chrome always follows global pack.
- Legacy theme migration preserves existing active state.
- Existing custom theme migration preserves custom values.
- Existing background reference is preserved during non-reset migration.
- Reset confirmation cancel changes nothing.
- Reset pack-picker dismissal changes nothing.
- Reset completes only after manual pack selection.
- Reset clears all seven overrides.
- Reset clears advanced custom appearance.
- Reset clears background preference and safely removes app-private photo after persisted success.
- Reset activates exactly the pack selected by the user.
- Invalid stored pack/override values fall back safely without crashing.
- Process restart preserves global pack and overrides.
- Surface previews render the correct layout category.

### 14.2 Regression verification

Run the existing Stage 1–23 relevant regression/static suites after implementation.

Where the environment supports Android/Gradle build execution, run at least:

```text
compileDebugKotlin
testDebugUnitTest
assembleDebug
```

If Android SDK/Gradle/device execution is unavailable in the implementation environment, the final report must say so explicitly rather than claiming unexecuted build/device evidence.

### 14.3 Protected-code comparison

Compare protected AI/backend/context/safety areas against the Stage 23.1 baseline and report unexpected differences.

## 15. Acceptance Criteria

The feature is complete when all of the following are true:

1. All four approved complete theme packs exist in-app.
2. All seven surfaces have four matching premium variants.
3. Users can apply one complete pack with one selection.
4. Users can independently override each of the seven surfaces.
5. Toolbar, suggestion/header chrome, and AI remain tied to the Global Theme Pack.
6. Existing custom appearance controls remain available.
7. Existing legacy themes remain available and upgrade data is preserved.
8. Reset never forces a predefined pack; user manually selects the pack that becomes active.
9. Cancelling either reset confirmation or reset pack selection changes nothing.
10. Theme switching does not modify typing, prediction, AI/backend, context, or sensitive-field safety behavior.
11. New theme tests and existing relevant regressions pass, subject to clearly disclosed environment limitations.
12. Final source can be opened and built in Android Studio without introducing a theme-related Kotlin compile error.

## 16. Non-Goals

This change does not include:

- New AI behavior.
- New prediction/autocorrect ranking logic.
- New backend endpoints.
- Cloud theme sync.
- Downloadable online theme marketplace.
- Per-app automatic theme switching.
- New keyboard layouts unrelated to visual theme presentation.

These can be considered separately after the premium multi-theme system is stable.
