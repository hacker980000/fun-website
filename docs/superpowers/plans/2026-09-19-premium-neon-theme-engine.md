# Premium Neon Theme Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved premium dark-glass neon keyboard design, eight immutable presets plus Custom, live token-based customization, and private manual photo backgrounds scoped to Full Keyboard / Keys Only / AI Panel Only without changing v35.3.1 typing, AI, privacy, account, quota, context, or insertion behavior.

**Architecture:** Add an isolated `theme` package with a DataStore-backed repository, immutable presets, drawable factory, renderer, preview, and private image processor/cache. A dedicated `ThemeSettingsActivity` edits only theme state. The IME observes theme state, rebuilds visual components only when theme state changes, and keeps all key dispatch and AI flows untouched.

**Tech Stack:** Android Views/XML, Kotlin, DataStore Preferences, coroutines/Flow, Android Canvas/Drawable APIs, app-private files, Robolectric/JUnit4. No Compose and no new third-party dependency.

**Spec:** `docs/superpowers/specs/2026-09-18-premium-neon-theme-engine-design.md`

## Global Constraints

- Baseline application version remains `35.3.1` (`versionCode 350301`).
- `minSdk=26`, `targetSdk=36`, `compileSdk=36` remain unchanged.
- No storage/media permission is added; photo selection uses the Android document picker.
- Selected theme photo is copied to app-private storage and re-encoded; original EXIF/GPS is not retained.
- No network access occurs during theme rendering or photo processing.
- No bitmap decode/blur on the main thread.
- No per-keystroke bitmap/drawable allocation and no continuous key animation loop.
- Existing AI/privacy/account/quota/context/caption/typing behavior remains unchanged.
- Built-in presets are immutable; user edits are stored only in `Custom`.
- Background photo scopes are exactly `FULL_KEYBOARD`, `KEYS_ONLY`, `AI_PANEL_ONLY`.
- `allowBackup=false` remains unchanged.

---

### Task 1: Theme model, presets, and repository

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemePresetTest.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRepositoryTest.kt`

**Interfaces:**
- Produces `data class KeyboardTheme`, `data class BackgroundPhotoConfig`, `enum class BackgroundScope`, `enum class BackgroundFit`.
- Produces `ThemePreset.byId(id: String): KeyboardTheme` and `ThemePreset.builtIns: List<KeyboardTheme>`.
- Produces `ThemeRepository.theme: Flow<KeyboardTheme>`, `current()`, `activatePreset(id)`, `saveCustom(theme)`, `resetCustom()`, and background-config update methods.

- [ ] **Step 1: Write failing preset/model tests**

```kotlin
@Test fun builtIns_areCompleteAndUnique() {
    assertEquals(8, ThemePreset.builtIns.size)
    assertEquals(8, ThemePreset.builtIns.map { it.id }.toSet().size)
    ThemePreset.builtIns.forEach { assertTrue(it.isValid()) }
}

@Test fun numericTokens_areClamped() {
    val theme = ThemePreset.socialAiNeon.copy(glowStrength = 999, keyCornerRadiusDp = -10f).normalized()
    assertEquals(100, theme.glowStrength)
    assertEquals(4f, theme.keyCornerRadiusDp)
}
```

- [ ] **Step 2: Run model tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemePresetTest'`
Expected: FAIL because theme classes do not exist.

- [ ] **Step 3: Implement theme model and eight presets**

Use explicit ARGB `Int` tokens, normalized numeric ranges, and the IDs: `social_ai_neon`, `cyber_blue`, `neon_violet`, `aurora_cyan`, `black_gold`, `crimson_pulse`, `frost_glass`, `pure_amoled`, plus editable `custom`.

- [ ] **Step 4: Write failing DataStore repository tests**

```kotlin
@Test fun selectedPreset_roundTrips() = runTest {
    repository.activatePreset("black_gold")
    assertEquals("black_gold", repository.current().id)
}

@Test fun editingBuiltIn_createsCustomInstead() = runTest {
    repository.saveCustom(ThemePreset.socialAiNeon.copy(primaryNeon = 0xFFFF00FF.toInt()))
    assertEquals("custom", repository.current().id)
    assertEquals(0xFFFF00FF.toInt(), repository.current().primaryNeon)
}
```

- [ ] **Step 5: Implement dedicated theme DataStore repository**

Persist `active_theme_id`, all Custom token values, and background config in `social_ai_theme_settings`. Decode corrupt/missing values token-by-token against `ThemePreset.socialAiNeon` and normalize numeric values.

- [ ] **Step 6: Run repository/model tests**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.*'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme app/src/test/java/com/socialaiassistant/keyboard/theme
git commit -m "feat: add keyboard theme model and repository"
```

### Task 2: Neon component renderer and preview

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/NeonDrawableFactory.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreviewView.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRendererTest.kt`

**Interfaces:**
- Produces `enum class ThemeButtonRole { NORMAL, SPECIAL, ENTER, AI, AI_ACTION, SECONDARY_ACTION }`.
- Produces `NeonDrawableFactory.button(theme, role, enabled, selected): Drawable` with drawable caching per theme signature.
- Produces `ThemeRenderer.applyRoot(root, theme)`, `styleButton(button, role, theme, selected)`, `styleText(textView, theme, secondary)`, and `styleSmartReply(root, theme)`.

- [ ] **Step 1: Write failing Robolectric renderer tests**

Assert standard key, AI, and Enter buttons receive distinct state-list drawables and text colors, and disabled AI controls remain readable.

- [ ] **Step 2: Run renderer tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemeRendererTest'`
Expected: FAIL because renderer does not exist.

- [ ] **Step 3: Implement cached neon drawables**

Use `GradientDrawable` + `StateListDrawable`/`RippleDrawable`, layered translucent strokes/fills, no shadow animation loop, and cached roles keyed by normalized theme tokens.

- [ ] **Step 4: Implement ThemeRenderer and ThemePreviewView**

`ThemePreviewView` draws a mini toolbar, AI cards, and representative key rows using the same token colors/geometry; it has no `InputConnection` access.

- [ ] **Step 5: Run renderer tests**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemeRendererTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRendererTest.kt
git commit -m "feat: add neon renderer and theme preview"
```

### Task 3: Private background photo processing and caching

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManager.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManagerTest.kt`

**Interfaces:**
- Produces `suspend fun importPhoto(uri: Uri): Result<String>` returning app-private filename only after successful decode/re-encode.
- Produces `suspend fun loadDisplayBitmap(config: BackgroundPhotoConfig, targetWidth: Int, targetHeight: Int): Bitmap?` using an LRU cache and off-main decode/blur.
- Produces `suspend fun removePhoto(fileName: String?)`.

- [ ] **Step 1: Write failing replacement/privacy tests**

Create an input JPEG containing EXIF-like metadata bytes, import it, verify the private output is a newly encoded bitmap and does not contain source metadata strings. Verify failed import leaves the previous filename untouched in repository logic.

- [ ] **Step 2: Run background-manager tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemeBackgroundManagerTest'`
Expected: FAIL because manager does not exist.

- [ ] **Step 3: Implement private image import**

Decode bounds first, sample to keyboard-appropriate size, decode on `Dispatchers.IO`, draw/re-encode to JPEG in app-private `files/theme_backgrounds/`, and atomically rename temp output after success.

- [ ] **Step 4: Implement cached display transform**

Support `FILL`, `FIT`, `CENTER_CROP`, opacity/dim in the view layer, and a bounded bitmap blur routine performed off-main; cache by `(fileName, targetWidth, targetHeight, blur)`.

- [ ] **Step 5: Run background tests**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemeBackgroundManagerTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManager.kt app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManagerTest.kt
git commit -m "feat: add private keyboard background manager"
```

### Task 4: Theme Settings UI and Custom editor

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt`
- Create: `app/src/main/res/layout/activity_theme_settings.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt`

**Interfaces:**
- Main settings exposes one `Customize Keyboard Theme` entry.
- Activity supports preset selection, Custom token fields, sliders for glow/glass/radius/gap/font, background picker, scope/fit selectors, image opacity/dim/blur, remove photo, and reset Custom.
- Photo selection uses `ActivityResultContracts.OpenDocument(arrayOf("image/*"))`; no storage permission.

- [ ] **Step 1: Write failing activity smoke tests**

Verify eight preset controls render, selecting Black & Gold activates it, editing a token switches to Custom, and scope selector contains Full Keyboard / Keys Only / AI Panel Only.

- [ ] **Step 2: Run UI tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest'`
Expected: FAIL because activity/layout do not exist.

- [ ] **Step 3: Add Theme Settings activity/layout and manifest entry**

Use existing Android Views. Keep the preview at top, presets next, customization controls below. Use hex text controls for color tokens plus SeekBars/Spinners for numeric/scope/fit fields.

- [ ] **Step 4: Add MainActivity entry point**

Open `ThemeSettingsActivity` from the new button only; do not mix theme persistence into `SettingsRepository`.

- [ ] **Step 5: Wire photo import safely**

Only update `BackgroundPhotoConfig.localFileName` after `ThemeBackgroundManager.importPhoto()` succeeds; delete the old private image only after repository successfully references the new one.

- [ ] **Step 6: Run theme-settings tests**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt app/src/main/res/layout app/src/main/res/values/strings.xml app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt
git commit -m "feat: add premium keyboard theme settings"
```

### Task 5: IME layout and live theme integration

**Files:**
- Modify: `app/src/main/res/layout/ime_keyboard.xml`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SmartReplyController.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ImeThemeIntegrationTest.kt`

**Interfaces:**
- XML root becomes a `FrameLayout` with `full_keyboard_background`, `keyboard_content`, `ai_panel_background`, and `keys_background` layers.
- IME observes `ThemeRepository.theme` in `serviceScope`, stores `currentTheme`, applies static surfaces, re-renders key/AI controls only when theme changes, and asynchronously loads exactly one background layer for the selected scope.
- Existing action listeners and `KeyboardAction` dispatch remain unchanged.

- [ ] **Step 1: Write failing IME integration tests**

Verify default theme is Social AI Neon, Enter and AI roles are distinct, changing repository theme re-styles an existing root, and each photo scope turns on exactly one background layer.

- [ ] **Step 2: Run IME integration tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ImeThemeIntegrationTest'`
Expected: FAIL before integration.

- [ ] **Step 3: Refactor IME XML into layered containers**

Keep all existing IDs used by controllers while adding the three background ImageViews and wrappers. Do not change typing row ownership or AI panel IDs.

- [ ] **Step 4: Wire theme dependencies from application**

Expose `themeRepository`, `themeRenderer`, and `themeBackgroundManager` on `SocialAiApplication` alongside existing repositories/gateways.

- [ ] **Step 5: Style generated controls by explicit role**

Normal letter/number/symbol buttons use `NORMAL`; shift/backspace/language modifiers use `SPECIAL`; Enter uses `ENTER`; toolbar AI uses `AI`; AI cards use `AI_ACTION`; result/tone controls use `SECONDARY_ACTION`.

- [ ] **Step 6: Apply live theme changes and scoped backgrounds**

Collect theme flow while IME service is alive. Reapply surface/text/button styling and reload only the chosen photo layer. Clear all three image layers before enabling the selected one.

- [ ] **Step 7: Style SmartReplyController**

Add `applyTheme(theme, renderer)` so Smart Reply text/buttons match the active theme without changing render state logic.

- [ ] **Step 8: Run IME theme tests**

Run: `./gradlew testDebugUnitTest --tests 'com.socialaiassistant.keyboard.theme.ImeThemeIntegrationTest'`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/layout/ime_keyboard.xml app/src/main/java/com/socialaiassistant/keyboard/ime app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt app/src/test/java/com/socialaiassistant/keyboard/theme/ImeThemeIntegrationTest.kt
git commit -m "feat: apply live neon themes to keyboard IME"
```

### Task 6: Regression verification, documentation, and deliverable

**Files:**
- Modify: `README.md`
- Modify: `scripts/verify_release_ready.py` only if it needs new manifest/layout awareness without weakening existing checks.
- Create: `THEME_ENGINE_VERIFICATION.md`

**Interfaces:**
- No production behavior introduced; this task proves the feature is safe and packages the updated source.

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Run lint/build when dependencies are available**

Run: `./gradlew lintDebug assembleDebug`
Expected: PASS. If Gradle dependency/network access prevents execution, record the exact limitation rather than claiming success.

- [ ] **Step 3: Run release/static verifier**

Run: `python3 scripts/verify_release_ready.py`
Expected: PASS without weakening privacy/security checks.

- [ ] **Step 4: Check manifest constraints**

Confirm no storage/media permission, `allowBackup=false`, and application version remains 35.3.1.

- [ ] **Step 5: Update README and verification report**

Document preset names, Custom controls, background scopes, private image handling, and exactly which automated checks ran.

- [ ] **Step 6: Create final source ZIP and checksum**

Package repository working tree excluding `.git`, build outputs, and local Gradle caches. Run `unzip -t` and SHA-256.

- [ ] **Step 7: Commit**

```bash
git add README.md THEME_ENGINE_VERIFICATION.md scripts/verify_release_ready.py
git commit -m "docs: verify premium neon theme engine"
```
