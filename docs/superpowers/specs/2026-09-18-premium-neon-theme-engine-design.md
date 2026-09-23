# Social AI Keyboard Premium Neon Theme Engine Design

Date: 2026-09-18
Status: Approved and implemented on feature/premium-neon-theme
Baseline: Social AI Keyboard v35.3.1 ported source
Visual reference: `docs/design-reference/social-ai-keyboard-neon-reference.jpg`

## 1. Goal

Turn the current functional Android IME into a premium, unique keyboard UI based on the user-selected Social AI Keyboard mockup: deep navy/AMOLED surfaces, glass-like panels, electric blue/cyan/purple neon accents, and compact premium AI controls. Add a reusable theme engine so the look can be switched or customized without changing typing, AI, backend, subscription, safety, or context behavior.

The selected reference design is the default and canonical visual baseline. The update must preserve all v35.3.1 functionality and must not change AI request rules, manual insert/send behavior, account/device policies, quota logic, privacy gates, sensitive-field handling, or existing typing behavior.

## 2. Locked UX Decisions

### 2.1 Default visual language

- Base: deep navy to AMOLED-black surfaces.
- Panels: dark translucent/glass-like surfaces with subtle separation.
- Primary neon: electric blue/cyan.
- Secondary neon: violet/purple.
- AI identity: purple/blue neon emphasis.
- Enter/action key: bright electric-blue/cyan accent.
- Standard keys: dark glass fill, subtle luminous border, clear white/off-white labels.
- AI action cards: compact rounded cards with per-action neon accent and readable icon/title/subtitle.
- Pressed states: short visual glow/ripple state only; no long animation that can increase typing latency.
- Disabled states: visibly muted while preserving theme identity.

### 2.2 Background photo scope

The user selected option C. A manually selected background image can be applied to exactly one of:

1. Full Keyboard
2. Keys Only
3. AI Panel Only

The user can remove the image at any time and return to the active theme's normal background.

### 2.3 Background image controls

- Source image chosen manually from the Android system picker.
- Fit modes: Fill / Fit / Center Crop.
- Image opacity.
- Dark overlay/dim amount.
- Blur amount.
- No broad media/storage permission.
- Selected image is copied into app-private storage and re-encoded so original EXIF/GPS metadata is not retained.
- Theme/background photos never go to the AI backend unless the user separately and explicitly uses the existing Photo Caption feature.

## 3. Scope

### In scope

- Central theme model and theme repository.
- Built-in premium theme presets.
- One editable Custom theme based on the selected preset.
- Live theme application to IME keys, toolbar, AI panel, smart reply area, special/action keys, and relevant settings preview.
- Manual background photo with Full Keyboard / Keys Only / AI Panel Only scopes.
- User controls for colors, glow, opacity, corner radius, spacing, font scale, background dim/blur, and background image fit.
- Theme settings screen with live preview and Reset to Default.
- Theme persistence across keyboard reopen, app restart, and device reboot.
- Efficient image caching and low-latency rendering.
- Regression tests protecting v35.3.1 behavior.

### Out of scope

- Theme marketplace/downloads.
- Cloud theme sync.
- Theme sharing/import/export.
- Animated/video/GIF backgrounds.
- Per-app automatic themes.
- Changes to AI backend, account rules, quota, payment, training, caption generation, context adapters, or typing layouts.

## 4. Built-in Themes

The following built-in presets will ship. Every preset uses the same component system and differs only by tokens.

1. `Social AI Neon` - locked default; dark navy + electric cyan + violet.
2. `Cyber Blue` - near-black + blue/cyan.
3. `Neon Violet` - charcoal + violet/magenta.
4. `Aurora Cyan` - dark teal/navy + cyan/aqua.
5. `Black & Gold` - AMOLED black + warm gold.
6. `Crimson Pulse` - graphite + crimson/red.
7. `Frost Glass` - cool dark slate + ice-blue glass accents.
8. `Pure AMOLED` - true black + restrained blue accent for maximum contrast and battery efficiency on OLED screens.
9. `Custom` - editable user profile initialized from the currently selected preset.

The built-in presets are immutable. User edits are written only to `Custom`, preventing accidental loss of shipped themes.

## 5. Theme Data Model

Create a dedicated `theme` package with a single source of truth.

### 5.1 `KeyboardTheme`

Core token groups:

- identity: theme id, display name, built-in/custom flag.
- surfaces: root background, panel surface, toolbar surface, key surface, special-key surface, disabled surface.
- text: primary, secondary, key label, special-key label, disabled label.
- accents: primary neon, secondary neon, AI neon, enter/action accent, danger/warning where needed.
- geometry: key corner radius, panel corner radius, key gap, outer padding.
- typography: key label scale, toolbar label scale, AI action title/subtitle scale.
- effects: border opacity, glow strength, pressed glow strength, glass opacity.
- background config reference.

### 5.2 `BackgroundPhotoConfig`

- enabled
- local private file name
- scope: `FULL_KEYBOARD`, `KEYS_ONLY`, `AI_PANEL_ONLY`
- fit: `FILL`, `FIT`, `CENTER_CROP`
- opacity percent
- dark overlay percent
- blur amount

### 5.3 Persistence

Add a dedicated `ThemeRepository` backed by DataStore Preferences. Keep theme settings separate from AI/content settings so theme edits cannot accidentally alter model, privacy, or account configuration.

Persist only token values and the app-private background file name. Never persist a raw external `content://` URI as the sole source of the theme because access may disappear.

## 6. Component Architecture

### 6.1 `ThemeRepository`

Responsibilities:

- expose active theme as a `Flow<KeyboardTheme>`.
- return built-in presets.
- save/edit Custom theme.
- activate a preset or Custom.
- reset Custom to the locked default.
- store background configuration.

It has no dependency on IME behavior or AI logic.

### 6.2 `ThemeRenderer`

Responsibilities:

- apply theme tokens to the current IME root.
- style toolbar controls.
- style generated letter/number/symbol keys.
- distinguish normal/special/enter keys.
- style AI action cards and smart-reply strip.
- re-apply when theme flow changes while the keyboard is open.

No typing action should be handled by the renderer.

### 6.3 `NeonDrawableFactory`

Create efficient reusable state drawables from theme tokens.

States:

- normal
- pressed
- disabled
- selected/active

Implementation principle: favor layered strokes, translucent borders, ripple/pressed states, and cached drawables over expensive per-frame blur/shadow animation. Visual richness must not trade off key response latency.

### 6.4 `ThemeBackgroundManager`

Responsibilities:

- launch is handled by the settings Activity; manager processes the result.
- decode a scaled bitmap off the main thread.
- re-encode into private storage without EXIF/GPS metadata.
- generate/cache a display-sized processed variant.
- apply blur/dim/opacity settings without repeatedly decoding the original image.
- delete superseded private files when the user chooses a new image or removes the background.

### 6.5 `ThemeSettingsActivity`

A dedicated screen keeps `MainActivity` from growing further.

Sections:

- large keyboard preview based on the selected reference style.
- preset theme grid.
- `Customize` section.
- color controls for key, text, surface, primary neon, secondary neon, AI accent, and enter/action accent.
- sliders for glow, glass opacity, key radius, key spacing, font scale.
- background image picker.
- scope selector: Full Keyboard / Keys Only / AI Panel Only.
- fit selector.
- image opacity, dim, and blur controls.
- Remove Background Photo.
- Reset Custom Theme.

Use Android Views/XML, matching the existing project. Do not introduce Compose for only this feature.

### 6.6 `ThemePreviewView`

A small isolated custom preview component renders representative toolbar buttons, AI cards, and key rows from the same theme tokens. It does not use the active `InputConnection` and cannot type or send anything.

## 7. IME Layout Changes

The current `ime_keyboard.xml` root uses hard-coded colors and a single vertical layout. Replace hard-coded color ownership with named containers that the renderer can theme.

Target structure:

- Root `FrameLayout`
  - optional Full Keyboard background `ImageView`
  - main vertical content container
    - smart reply strip
    - toolbar
    - AI panel wrapper with optional AI Panel background `ImageView`
    - keys wrapper with optional Keys Only background `ImageView`
      - generated key rows

The background `ImageView`s are mutually controlled by `BackgroundPhotoConfig.scope`, so one selected image is not accidentally painted three times.

## 8. Key Styling Rules

### Standard keys

- dark translucent/glass surface.
- subtle primary-neon outline.
- high-contrast label.
- short pressed-state neon boost.

### Special keys

Shift, backspace, language/symbol modifiers use a slightly stronger surface and border for discoverability.

### Enter/action key

Always receives the action accent from the theme. In the locked default it is electric blue/cyan, matching the reference.

### AI toolbar button

Uses AI purple/violet accent and selected-state glow when the AI panel is open.

### AI action cards

Use token-driven accents but a shared geometry system. Existing actions and labels remain functionally unchanged.

## 9. Theme Update Flow

1. User opens Theme Settings.
2. User chooses a preset or edits Custom.
3. `ThemeRepository` writes the theme selection/tokens.
4. Repository flow emits the new theme.
5. Open IME receives the update and `ThemeRenderer` applies it immediately.
6. If the IME is not open, the theme is applied on the next `onCreateInputView`/session.
7. No app restart or keyboard re-enable is required for normal theme changes.

Photo processing may finish asynchronously. Until it is ready, the current theme background remains visible; after processing, the image layer updates without blocking typing.

## 10. Privacy and Permissions

- Use the Android system document/photo picker; do not request broad storage/media access merely for themes.
- Copy the chosen image into internal app storage.
- Re-encode the copy to strip original metadata.
- Do not upload theme background images.
- Existing Photo Caption upload flow is separate and requires explicit user action.
- `allowBackup=false` remains unchanged.
- Theme settings are local UI preferences; they do not change AI Privacy/Data consent.

## 11. Performance Constraints

Typing responsiveness is the highest UI priority.

- No network access during theme rendering.
- No bitmap decoding on the main thread.
- No continuous animation loop on keys.
- No per-keystroke bitmap or drawable allocation.
- Cache normal/pressed/disabled drawables per active theme.
- Re-theme only when theme tokens change or when a relevant panel is rebuilt.
- Downsample user photos to an appropriate keyboard-display size before caching.
- Precompute blurred background variants after the user changes blur settings rather than blurring every frame.
- Avoid software-layer glow on every key if profiling shows latency or GPU overdraw problems; layered neon strokes are the default implementation.

## 12. Compatibility and Existing Behavior

The following must remain unchanged:

- English, Bangla, Phonetic and Bijoy input behavior.
- symbol and number layouts.
- emoji, clipboard and voice actions.
- AI panel action behavior.
- Smart/Unique/Flirty/Funny, Rewrite, Translate, Grammar, tone and custom instruction behavior.
- Managed AI and Personal OpenRouter routing.
- account/login/subscription/quota/device-proof rules.
- privacy/data consent and sensitive-field blocking.
- context access and supported platform adapters.
- preserve-draft and explicit insert behavior.
- no automatic Send/Post behavior.
- caption and Photo Caption flows.

## 13. Error Handling

- Invalid or unreadable selected photo: keep the current background and show a clear error.
- Image processing failure: do not delete the previous working background until the new processed copy succeeds.
- Missing private background file after cleanup/device migration: disable only the photo layer and fall back to the selected theme colors.
- Corrupt theme preference values: clamp numeric values and fall back token-by-token to the locked default.
- Low-contrast custom color combination: show a preview warning and keep critical keyboard labels readable; do not silently make AI/backend changes.

## 14. Testing Strategy

### Unit tests

- every preset resolves to a complete valid token set.
- DataStore encode/decode round trip.
- numeric value clamping.
- custom theme reset.
- background scope/fit serialization.
- background file replacement does not delete the previous file before success.
- metadata-stripped processed image does not retain original EXIF/GPS fields.

### Robolectric/UI-level tests

- `ThemeRenderer` applies standard/special/enter/AI styles correctly.
- selected AI toolbar state uses AI accent.
- theme flow update re-styles an already created IME view.
- each background scope shows exactly one image layer.
- missing image falls back cleanly.

### Regression tests

Run existing typing, multilingual toolbar, AI panel, context-adapter, privacy, and release verification tests. Theme code must not change key action dispatch, prompt construction, backend payloads, or insertion rules.

### Manual device checks

- small and large phones.
- portrait and landscape.
- light and dark system appearance.
- keyboard close/reopen.
- app process restart.
- select/remove/replace a large camera photo.
- rapid typing with neon theme and with photo background.
- AI panel open/close while theme is changed from settings.
- Full Keyboard / Keys Only / AI Panel Only photo scopes.

## 15. Files and Modules Expected to Change

Existing files likely modified:

- `app/src/main/res/layout/ime_keyboard.xml`
- `app/src/main/res/layout/activity_main.xml` (entry point to Theme Settings only)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ime/SmartReplyController.kt` if it owns visual styling
- AI panel rendering code inside the IME service or extracted renderer target
- strings/resources required for theme settings

New files expected:

- `app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/theme/NeonDrawableFactory.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManager.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreviewView.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt`
- `app/src/main/res/layout/activity_theme_settings.xml`
- tests under `app/src/test/.../theme/`

Exact extraction of the current inline AI-panel styling into smaller renderer helpers may be adjusted during implementation, but functional ownership must stay as described above.

## 16. Acceptance Criteria

The update is accepted when all of the following are true:

1. The default keyboard visually follows the approved reference image's dark glass + cyan/blue/violet neon direction.
2. All key and AI buttons use the new neon component system with clear normal/pressed/disabled states.
3. Eight built-in premium presets plus one editable Custom profile are available.
4. Changing a theme updates the keyboard without re-enabling the IME.
5. User can select a local photo and choose Full Keyboard, Keys Only, or AI Panel Only.
6. Photo opacity, dim, blur, and fit settings work and persist.
7. Removing/replacing a photo is safe and leaves a valid fallback theme.
8. Theme photos are stored privately, stripped of original metadata, and never uploaded by the theme system.
9. Existing v35.3.1 typing, AI, privacy, account, quota, context, and caption behavior remains unchanged.
10. Static verification and relevant automated tests pass; if full Gradle build access is unavailable, that limitation is explicitly reported rather than hidden.

