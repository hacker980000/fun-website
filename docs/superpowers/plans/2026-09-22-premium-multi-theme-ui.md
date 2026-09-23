# Premium Multi-Theme UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship four complete premium theme packs with 28 catalogued surface variants, per-surface customization, global toolbar/AI consistency, backward-compatible legacy/custom theme persistence, and a manual-pack reset flow on top of the Stage 23.1 Android Studio build-fix baseline.

**Architecture:** Keep `KeyboardTheme` as the concrete render-token model, add a premium `ThemePack`/surface catalog above it, and make `ThemeRepository` expose a backward-compatible `ThemeSelectionState`. The IME resolves two visual channels—global chrome and active keyboard surface—without touching typing/prediction/AI/safety behavior; Settings owns the pack picker, per-surface selectors, preview UI, and the two-step reset interaction.

**Tech Stack:** Kotlin, Android Views/XML, DataStore Preferences, coroutines/Flow, Robolectric/JUnit4, existing `ThemeRenderer`/`NeonDrawableFactory`, existing Stage 1–23 verification scripts.

**Spec:** `docs/superpowers/specs/2026-09-22-premium-multi-theme-ui-design.md`

## Global Constraints

- Baseline is **Social AI Keyboard v35.3.1 / Typing Core v2 / Stage 23.1 Android Studio Build Fix**.
- Exactly four premium packs: `Classic Dark`, `Glass Modern`, `Clean Light`, `Gradient Pro`.
- Exactly seven catalog surfaces: `ENGLISH`, `NUMBER`, `SYMBOL`, `BANGLA`, `PHONETIC`, `BIJOY`, `SETTINGS`; all 28 pack/surface combinations must resolve.
- Toolbar, suggestion/header chrome, AI panel, AI action buttons, and global utility panels always resolve from the **Global Theme Pack** (or the preserved legacy/custom active theme before the user selects a premium pack).
- A per-surface override never changes global chrome.
- Existing custom appearance controls and all eight legacy presets remain available.
- Existing `social_ai_theme_settings` DataStore is retained; new keys are additive.
- Existing installations with no premium-pack key keep their legacy/custom active state and background reference.
- Reset is two-step: confirm first, then manually choose one of the four packs; no mutation occurs until pack selection.
- Reset clears all seven overrides, resets custom appearance to the selected pack's canonical visual values, clears the background preference, persists first, and only then deletes the old app-private background file.
- Dismissing reset confirmation or the pack picker changes nothing.
- Normal typing/prediction stays offline; theme selection performs no backend/AI call.
- No new debounce is added to normal text keys; Stage 19 touch behavior remains authoritative.
- Do not change behavior under `ai/`, `backend/`, `context/`, or `safety/`.
- Do not change Stage 21 prediction/ranking/profiling or Stage 22 confidence-aware autocorrect logic.
- No new direct Bangla typing layout is introduced by this theme-only feature. The existing runtime has English letters plus Bangla Phonetic/Bijoy letter modes; the `BANGLA` catalog variant is retained as the shared Bangla family/preview design, while runtime letter styling binds to the concrete `PHONETIC` or `BIJOY` surface. This avoids silently changing typing behavior.

## Review Focus

1. **Legacy/custom install with no premium key:** reopening Settings and the IME must preserve the exact old active preset/custom values and background reference; Task 3 adds migration tests.
2. **Reset picker dismissed after confirmation:** no preference, background reference, file, override, or active theme may change; Task 4 and Task 6 add repository/UI tests.
3. **Per-surface override while global pack changes:** override remains on that surface, while toolbar/AI immediately follow the new global pack; Task 2, Task 4, and Task 7 add resolution tests.
4. **Gradient/light themes on low-contrast labels:** `ThemeRenderer`/drawable changes must preserve safe label contrast and not leak cached drawables across visual styles; Task 1 adds rendering/signature tests.
5. **Bangla runtime mode mapping:** Phonetic and Bijoy must resolve their own surface themes without introducing a new layout or changing composition state; Task 7 adds explicit mode-to-surface and no-engine-reload tests.

---

## File Structure

### New domain files

- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt` — premium pack IDs/display names and safe parsing.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt` — seven catalog surfaces and stable storage IDs.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSelectionState.kt` — persisted premium/legacy selection state.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt` — four base visual families plus 28 concrete surface variants.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicy.kt` — pure resolution of global chrome and a requested surface.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSettingsUiStyler.kt` — Settings content/card styling using resolved global/settings themes without moving theme logic into the Activity.

### Modified production files

- `app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt` — add explicit fill-style/gradient tokens to support real Glass/Gradient rendering while keeping old themes source-compatible.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/NeonDrawableFactory.kt` — render solid/glass/gradient surfaces and include new tokens in drawable cache signatures.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt` — keep the existing eight legacy presets unchanged; only helper compatibility code may be added.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt` — encode/decode premium global pack, seven overrides, schema version, and legacy/custom data.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt` — expose state/resolution APIs, pack/override mutations, and reset transaction.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreviewView.kt` — surface-aware preview drawing.
- `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt` — add explicit global-chrome/surface styling entry points while preserving current helpers.
- `app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt` — pack cards, seven customizers, live preview, legacy/advanced sections, reset confirmation + picker.
- `app/src/main/res/layout/activity_theme_settings.xml` — premium section containers, seven surface rows/cards, collapsible advanced/legacy areas, reset button.
- `app/src/main/res/values/strings.xml` — stable user-facing theme copy.
- `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt` — collect `ThemeSelectionState`, resolve global chrome vs active key surface, and restyle/rebuild only visuals.

### Tests

- Create `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeCatalogTest.kt`.
- Create `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicyTest.kt`.
- Modify `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRepositoryTest.kt`.
- Modify `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRendererTest.kt`.
- Modify `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt`.
- Modify `app/src/test/java/com/socialaiassistant/keyboard/theme/ImeThemeIntegrationTest.kt`.
- Create `scripts/theme_stage23_2_multitheme_selftest.kt` for Android-free catalog/codec/resolution checks.
- Create `scripts/verify_theme_stage23_2.py` for source-contract/protected-area verification.

---

### Task 1: Add Visual Tokens for Solid, Glass, and Gradient Premium Surfaces

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/theme/NeonDrawableFactory.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRendererTest.kt`

**Interfaces:**
- Consumes: existing `KeyboardTheme.normalized()`, `visualSignature()`, `ThemeButtonRole`.
- Produces: `ThemeFillStyle`, new gradient token fields on `KeyboardTheme`, and drawable behavior consumed by `ThemeCatalog` and `ThemeRenderer`.

- [ ] **Step 1: Write failing tests for new visual tokens and cache signatures**

Add tests that pin old-theme compatibility and ensure Gradient Pro can render with a distinct signature:

```kotlin
@Test
fun visual_signature_changes_when_fill_style_or_gradient_changes() {
    val base = ThemePreset.socialAiNeon
    val gradient = base.copy(
        fillStyle = ThemeFillStyle.GRADIENT,
        keyGradientStart = 0xFF5B2EFF.toInt(),
        keyGradientEnd = 0xFF0EA5E9.toInt()
    )
    assertNotEquals(base.visualSignature(), gradient.visualSignature())
}

@Test
fun old_preset_defaults_to_solid_and_stays_valid() {
    val theme = ThemePreset.socialAiNeon
    assertEquals(ThemeFillStyle.SOLID, theme.fillStyle)
    assertTrue(theme.isValid())
}
```

- [ ] **Step 2: Run the renderer tests and confirm they fail before implementation**

Run:

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeRendererTest
```

Expected: compilation/test failure because `ThemeFillStyle` and gradient fields do not exist.

- [ ] **Step 3: Add fill-style and gradient tokens with backward-compatible defaults**

Implement in `KeyboardTheme.kt`:

```kotlin
enum class ThemeFillStyle { SOLID, GLASS, GRADIENT }

data class KeyboardTheme(
    // existing fields stay in the same order
    // ...
    val glassOpacity: Int = 86,
    val fillStyle: ThemeFillStyle = ThemeFillStyle.SOLID,
    val keyGradientStart: Int = keySurface,
    val keyGradientEnd: Int = keySurface,
    val specialGradientStart: Int = specialKeySurface,
    val specialGradientEnd: Int = specialKeySurface,
    val panelGradientStart: Int = panelSurface,
    val panelGradientEnd: Int = panelSurface,
    val background: BackgroundPhotoConfig = BackgroundPhotoConfig()
)
```

Update `visualSignature()` so the drawable cache cannot reuse a SOLID drawable for a GLASS/GRADIENT theme:

```kotlin
fun visualSignature(): Int = listOf(
    rootBackground, panelSurface, toolbarSurface, keySurface, specialKeySurface,
    textPrimary, keyLabel, primaryNeon, secondaryNeon, aiNeon, actionAccent,
    keyCornerRadiusDp.toBits(), panelCornerRadiusDp.toBits(), keyGapDp.toBits(),
    borderOpacity, glowStrength, pressedGlowStrength, glassOpacity,
    fillStyle.ordinal,
    keyGradientStart, keyGradientEnd,
    specialGradientStart, specialGradientEnd,
    panelGradientStart, panelGradientEnd
).fold(17) { acc, value -> 31 * acc + value }
```

- [ ] **Step 4: Teach `NeonDrawableFactory` to render the three styles**

Replace the solid-only rounded helper with a style-aware helper:

```kotlin
private fun rounded(
    fill: Int,
    stroke: Int,
    strokeDp: Int,
    radiusDp: Float,
    gradient: IntArray? = null
): GradientDrawable = GradientDrawable(
    GradientDrawable.Orientation.TL_BR,
    gradient
).apply {
    shape = GradientDrawable.RECTANGLE
    if (gradient == null) setColor(fill)
    setStroke(dp(strokeDp), stroke)
    cornerRadius = dp(radiusDp)
}
```

For normal button fill, choose tokens without changing pressed/disabled semantics:

```kotlin
private fun gradientFor(theme: KeyboardTheme, role: ThemeButtonRole): IntArray? {
    if (theme.fillStyle != ThemeFillStyle.GRADIENT) return null
    return when (role) {
        ThemeButtonRole.NORMAL -> intArrayOf(theme.keyGradientStart, theme.keyGradientEnd)
        else -> intArrayOf(theme.specialGradientStart, theme.specialGradientEnd)
    }
}
```

Use `gradientFor(...)` on NORMAL state; pressed/selected states may continue to use the existing blended solid feedback so pressed state remains obvious. `GLASS` continues to use alpha-controlled solid fills via `glassOpacity`.

- [ ] **Step 5: Run focused renderer tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeRendererTest
```

Expected: PASS.

- [ ] **Step 6: Commit the visual-token unit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt \
        app/src/main/java/com/socialaiassistant/keyboard/theme/NeonDrawableFactory.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRendererTest.kt
git commit -m "feat(theme): add premium surface fill styles"
```

---

### Task 2: Build the Four-Pack / 28-Variant Catalog and Pure Resolution Policy

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSelectionState.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicy.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeCatalogTest.kt`
- Create: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicyTest.kt`

**Interfaces:**
- Consumes: `KeyboardTheme`, `ThemePreset.byId()`, `ThemeFillStyle`.
- Produces: `ThemePack.fromStored`, `KeyboardThemeSurface.fromStored`, `ThemeSelectionState`, `ThemeCatalog.globalChrome(pack)`, `ThemeCatalog.surface(pack, surface)`, `ThemeResolutionPolicy.resolveGlobalChrome(state)`, `ThemeResolutionPolicy.resolveSurface(state, surface)`.

- [ ] **Step 1: Write failing catalog coverage tests**

Create `ThemeCatalogTest.kt`:

```kotlin
class ThemeCatalogTest {
    @Test
    fun exposes_exactly_four_packs_and_seven_surfaces() {
        assertEquals(4, ThemePack.entries.size)
        assertEquals(7, KeyboardThemeSurface.entries.size)
    }

    @Test
    fun every_pack_resolves_all_28_unique_surface_variants() {
        val variants = ThemePack.entries.flatMap { pack ->
            KeyboardThemeSurface.entries.map { surface -> ThemeCatalog.surface(pack, surface) }
        }
        assertEquals(28, variants.size)
        assertEquals(28, variants.map { it.id }.toSet().size)
        assertTrue(variants.all { it.isValid() })
    }

    @Test
    fun premium_families_are_not_color_only_clones() {
        val surfaces = ThemePack.entries.map { ThemeCatalog.surface(it, KeyboardThemeSurface.ENGLISH) }
        assertEquals(4, surfaces.map { it.keyCornerRadiusDp to it.fillStyle }.toSet().size)
    }
}
```

- [ ] **Step 2: Write failing global-vs-surface resolution tests**

Create `ThemeResolutionPolicyTest.kt`:

```kotlin
@Test
fun surface_override_does_not_change_global_chrome() {
    val state = ThemeSelectionState(
        globalPack = ThemePack.GLASS_MODERN,
        perSurfaceOverrides = mapOf(KeyboardThemeSurface.ENGLISH to ThemePack.CLEAN_LIGHT),
        legacyActiveThemeId = ThemePreset.socialAiNeon.id,
        customTheme = ThemePreset.customFrom()
    )
    assertEquals(
        ThemeCatalog.globalChrome(ThemePack.GLASS_MODERN).id,
        ThemeResolutionPolicy.resolveGlobalChrome(state).id
    )
    assertEquals(
        ThemeCatalog.surface(ThemePack.CLEAN_LIGHT, KeyboardThemeSurface.ENGLISH).id,
        ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.ENGLISH).id
    )
}

@Test
fun legacy_state_without_global_pack_resolves_old_active_theme() {
    val state = ThemeSelectionState(
        globalPack = null,
        perSurfaceOverrides = emptyMap(),
        legacyActiveThemeId = ThemePreset.blackGold.id,
        customTheme = ThemePreset.customFrom()
    )
    assertEquals("black_gold", ThemeResolutionPolicy.resolveGlobalChrome(state).id)
    assertEquals("black_gold", ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.ENGLISH).id)
}
```

- [ ] **Step 3: Run new tests and confirm they fail**

```bash
./gradlew testDebugUnitTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeCatalogTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeResolutionPolicyTest
```

Expected: compilation failure because new types do not exist.

- [ ] **Step 4: Implement stable pack/surface enums and selection state**

`ThemePack.kt`:

```kotlin
enum class ThemePack(val storedId: String, val displayName: String) {
    CLASSIC_DARK("classic_dark", "Classic Dark"),
    GLASS_MODERN("glass_modern", "Glass Modern"),
    CLEAN_LIGHT("clean_light", "Clean Light"),
    GRADIENT_PRO("gradient_pro", "Gradient Pro");

    companion object {
        fun fromStored(value: String?): ThemePack? = entries.firstOrNull { it.storedId == value }
    }
}
```

`KeyboardThemeSurface.kt`:

```kotlin
enum class KeyboardThemeSurface(val storedId: String, val displayName: String) {
    ENGLISH("english", "English"),
    NUMBER("number", "Number"),
    SYMBOL("symbol", "Symbol"),
    BANGLA("bangla", "বাংলা"),
    PHONETIC("phonetic", "Phonetic"),
    BIJOY("bijoy", "Bijoy"),
    SETTINGS("settings", "Settings");

    companion object {
        fun fromStored(value: String?): KeyboardThemeSurface? = entries.firstOrNull { it.storedId == value }
    }
}
```

`ThemeSelectionState.kt`:

```kotlin
data class ThemeSelectionState(
    val globalPack: ThemePack?,
    val perSurfaceOverrides: Map<KeyboardThemeSurface, ThemePack>,
    val legacyActiveThemeId: String,
    val customTheme: KeyboardTheme
) {
    fun overrideFor(surface: KeyboardThemeSurface): ThemePack? = perSurfaceOverrides[surface]
}
```

- [ ] **Step 5: Implement the concrete premium catalog**

Use four base palettes and seven surface profiles. Keep IDs deterministic as `premium_<pack>_<surface>` and use the approved display-name table. The exact base values are locked as follows:

```kotlin
private data class Family(
    val root: Int, val panel: Int, val toolbar: Int,
    val key: Int, val special: Int,
    val text: Int, val secondaryText: Int,
    val primary: Int, val secondary: Int, val ai: Int, val action: Int,
    val fillStyle: ThemeFillStyle,
    val keyRadius: Float, val panelRadius: Float, val gap: Float,
    val glow: Int, val border: Int, val glass: Int,
    val keyGradientStart: Int = key,
    val keyGradientEnd: Int = key,
    val specialGradientStart: Int = special,
    val specialGradientEnd: Int = special
)

private val families = mapOf(
    ThemePack.CLASSIC_DARK to Family(
        root = 0xFF0A0D12.toInt(), panel = 0xFF131821.toInt(), toolbar = 0xFF10151D.toInt(),
        key = 0xFF1A202A.toInt(), special = 0xFF262E3A.toInt(),
        text = 0xFFF7F9FC.toInt(), secondaryText = 0xFF9AA6B5.toInt(),
        primary = 0xFF4DA3FF.toInt(), secondary = 0xFF7C8B9D.toInt(), ai = 0xFF8B5CF6.toInt(), action = 0xFF2F8CFF.toInt(),
        fillStyle = ThemeFillStyle.SOLID, keyRadius = 8f, panelRadius = 12f, gap = 3f,
        glow = 24, border = 46, glass = 100
    ),
    ThemePack.GLASS_MODERN to Family(
        root = 0xFF07111F.toInt(), panel = 0xD21A2A3C.toInt(), toolbar = 0xD0122234.toInt(),
        key = 0xC8233548.toInt(), special = 0xD02B425A.toInt(),
        text = 0xFFF6FBFF.toInt(), secondaryText = 0xFFA8BDD0.toInt(),
        primary = 0xFF5CD6FF.toInt(), secondary = 0xFF62A8FF.toInt(), ai = 0xFFB67CFF.toInt(), action = 0xFF3B9EFF.toInt(),
        fillStyle = ThemeFillStyle.GLASS, keyRadius = 16f, panelRadius = 20f, gap = 5f,
        glow = 48, border = 58, glass = 72
    ),
    ThemePack.CLEAN_LIGHT to Family(
        root = 0xFFF4F7FB.toInt(), panel = 0xFFFFFFFF.toInt(), toolbar = 0xFFF0F4F8.toInt(),
        key = 0xFFFFFFFF.toInt(), special = 0xFFE8EEF6.toInt(),
        text = 0xFF172033.toInt(), secondaryText = 0xFF657185.toInt(),
        primary = 0xFF2F6FED.toInt(), secondary = 0xFF5B6B85.toInt(), ai = 0xFF7557D9.toInt(), action = 0xFF245EDB.toInt(),
        fillStyle = ThemeFillStyle.SOLID, keyRadius = 12f, panelRadius = 16f, gap = 4f,
        glow = 10, border = 24, glass = 100
    ),
    ThemePack.GRADIENT_PRO to Family(
        root = 0xFF090A18.toInt(), panel = 0xE3161830.toInt(), toolbar = 0xE0111327.toInt(),
        key = 0xE31B1D38.toInt(), special = 0xE72A2149.toInt(),
        text = 0xFFFFFFFF.toInt(), secondaryText = 0xFFB9B8D8.toInt(),
        primary = 0xFF6A5CFF.toInt(), secondary = 0xFF00C8FF.toInt(), ai = 0xFFE05CFF.toInt(), action = 0xFF3E8BFF.toInt(),
        fillStyle = ThemeFillStyle.GRADIENT, keyRadius = 18f, panelRadius = 22f, gap = 4f,
        glow = 70, border = 66, glass = 90,
        keyGradientStart = 0xFF312A65.toInt(), keyGradientEnd = 0xFF153A63.toInt(),
        specialGradientStart = 0xFF51306F.toInt(), specialGradientEnd = 0xFF18507B.toInt()
    )
)
```

Surface profiles modify shape/spacing/scales so variants are not name-only:

```kotlin
private data class SurfaceProfile(
    val radiusDelta: Float = 0f,
    val gapDelta: Float = 0f,
    val labelScale: Float = 1f,
    val accentMix: Float = 0f
)

private val profiles = mapOf(
    KeyboardThemeSurface.ENGLISH to SurfaceProfile(),
    KeyboardThemeSurface.NUMBER to SurfaceProfile(radiusDelta = 2f, gapDelta = 1f, labelScale = 1.06f),
    KeyboardThemeSurface.SYMBOL to SurfaceProfile(radiusDelta = -1f, labelScale = 0.96f),
    KeyboardThemeSurface.BANGLA to SurfaceProfile(radiusDelta = 1f, labelScale = 1.05f, accentMix = 0.08f),
    KeyboardThemeSurface.PHONETIC to SurfaceProfile(gapDelta = 0.5f, labelScale = 1.01f, accentMix = 0.05f),
    KeyboardThemeSurface.BIJOY to SurfaceProfile(radiusDelta = -2f, gapDelta = -0.5f, labelScale = 0.98f, accentMix = 0.10f),
    KeyboardThemeSurface.SETTINGS to SurfaceProfile(radiusDelta = 3f, gapDelta = 1f, labelScale = 1.00f, accentMix = 0.04f)
)
```

Expose:

```kotlin
object ThemeCatalog {
    fun globalChrome(pack: ThemePack): KeyboardTheme = build(pack, KeyboardThemeSurface.ENGLISH, chrome = true)
    fun surface(pack: ThemePack, surface: KeyboardThemeSurface): KeyboardTheme = build(pack, surface, chrome = false)
    fun allVariants(): List<KeyboardTheme> = ThemePack.entries.flatMap { pack ->
        KeyboardThemeSurface.entries.map { surface -> ThemeCatalog.surface(pack, surface) }
    }
}
```

Inside `build(...)`, assign deterministic IDs so global chrome and the 28 surfaces cannot collide:

```kotlin
val id = if (chrome) {
    "premium_${pack.storedId}_chrome"
} else {
    "premium_${pack.storedId}_${surface.storedId}"
}
```

Use the approved display names from the spec table, not generated labels.

- [ ] **Step 6: Implement the pure resolution policy**

```kotlin
object ThemeResolutionPolicy {
    fun resolveGlobalChrome(state: ThemeSelectionState): KeyboardTheme {
        val pack = state.globalPack
        return if (pack != null) ThemeCatalog.globalChrome(pack) else resolveLegacy(state)
    }

    fun resolveSurface(state: ThemeSelectionState, surface: KeyboardThemeSurface): KeyboardTheme {
        val pack = state.perSurfaceOverrides[surface] ?: state.globalPack
        return if (pack != null) ThemeCatalog.surface(pack, surface) else resolveLegacy(state)
    }

    private fun resolveLegacy(state: ThemeSelectionState): KeyboardTheme =
        if (state.legacyActiveThemeId == "custom") state.customTheme
        else ThemePreset.byId(state.legacyActiveThemeId) ?: ThemePreset.socialAiNeon
}
```

- [ ] **Step 7: Run catalog/resolution tests**

```bash
./gradlew testDebugUnitTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeCatalogTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeResolutionPolicyTest
```

Expected: PASS.

- [ ] **Step 8: Commit the catalog/resolution unit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt \
        app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt \
        app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSelectionState.kt \
        app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt \
        app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicy.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeCatalogTest.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicyTest.kt
git commit -m "feat(theme): add premium theme catalog and resolution"
```

---

### Task 3: Add Backward-Compatible Premium Selection Persistence and Migration

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRepositoryTest.kt`
- Create: `scripts/theme_stage23_2_multitheme_selftest.kt`

**Interfaces:**
- Consumes: existing custom-theme keys and `ThemeSelectionState` from Task 2.
- Produces: `DecodedThemePreferences.selectionState`, `ThemePreferencesCodec.encodeSelection(...)`, additive premium keys, safe invalid-value fallback.

- [ ] **Step 1: Write migration and invalid-value tests before changing the codec**

Add to `ThemeRepositoryTest.kt` or a focused codec test section:

```kotlin
@Test
fun decode_without_premium_keys_preserves_legacy_custom_and_background() {
    val custom = ThemePreset.customFrom().copy(
        primaryNeon = 0xFF123456.toInt(),
        background = BackgroundPhotoConfig(true, "keep.jpg")
    )
    val legacy = ThemePreferencesCodec.encode("custom", custom)
    val decoded = ThemePreferencesCodec.decode(legacy)

    assertNull(decoded.selectionState.globalPack)
    assertEquals("custom", decoded.selectionState.legacyActiveThemeId)
    assertEquals(0xFF123456.toInt(), decoded.selectionState.customTheme.primaryNeon)
    assertEquals("keep.jpg", decoded.selectionState.customTheme.background.localFileName)
}

@Test
fun invalid_pack_and_override_values_fall_back_without_crashing() {
    val values = ThemePreferencesCodec.encode(ThemePreset.blackGold.id, ThemePreset.customFrom()).toMutableMap().apply {
        put(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID, "not-a-pack")
        put(ThemePreferencesCodec.overrideKey(KeyboardThemeSurface.ENGLISH), "bad")
    }
    val decoded = ThemePreferencesCodec.decode(values)
    assertNull(decoded.selectionState.globalPack)
    assertTrue(decoded.selectionState.perSurfaceOverrides.isEmpty())
    assertEquals("black_gold", decoded.selectionState.legacyActiveThemeId)
}
```

- [ ] **Step 2: Run focused tests and confirm the new assertions fail**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeRepositoryTest
```

Expected: compilation failure because premium codec fields/helpers do not exist.

- [ ] **Step 3: Extend decoded preferences without removing legacy fields**

Change the decoded model to keep old callers source-compatible during the migration:

```kotlin
data class DecodedThemePreferences(
    val activeThemeId: String,
    val customTheme: KeyboardTheme,
    val selectionState: ThemeSelectionState
)
```

Add constants:

```kotlin
const val THEME_GLOBAL_PACK_ID = "theme_global_pack_id"
const val THEME_SELECTION_SCHEMA_VERSION = "theme_selection_schema_version"
const val CURRENT_SELECTION_SCHEMA_VERSION = 1
fun overrideKey(surface: KeyboardThemeSurface): String = "theme_override_${surface.storedId}"
```

- [ ] **Step 4: Decode premium state additively**

Inside `decode(values)` after building the normalized custom theme:

```kotlin
val activeThemeId = normalizeActiveId(values[ACTIVE_THEME_ID])
val globalPack = ThemePack.fromStored(values[THEME_GLOBAL_PACK_ID])
val overrides = KeyboardThemeSurface.entries.mapNotNull { surface ->
    ThemePack.fromStored(values[overrideKey(surface)])?.let { pack -> surface to pack }
}.toMap()
val selectionState = ThemeSelectionState(
    globalPack = globalPack,
    perSurfaceOverrides = overrides,
    legacyActiveThemeId = activeThemeId,
    customTheme = custom
)
return DecodedThemePreferences(activeThemeId, custom, selectionState)
```

Do not write premium keys simply because old data was read. Migration is lazy and non-destructive until the user explicitly selects a premium pack/override/reset.

- [ ] **Step 5: Add `encodeSelection` for repository mutations**

```kotlin
fun encodeSelection(state: ThemeSelectionState): Map<String, String> {
    val values = encode(state.legacyActiveThemeId, state.customTheme).toMutableMap()
    values[THEME_SELECTION_SCHEMA_VERSION] = CURRENT_SELECTION_SCHEMA_VERSION.toString()
    state.globalPack?.let { values[THEME_GLOBAL_PACK_ID] = it.storedId }
    state.perSurfaceOverrides.forEach { (surface, pack) ->
        values[overrideKey(surface)] = pack.storedId
    }
    return values
}
```

Repository code will explicitly remove stale premium keys before writing this map so cleared overrides cannot survive.

- [ ] **Step 6: Add Android-free codec/catalog self-test**

Create `scripts/theme_stage23_2_multitheme_selftest.kt` with assertions for 4 packs, 7 surfaces, 28 unique IDs, legacy decode, premium encode/decode, override clearing, and invalid stored values. Compile it with the same Kotlin CLI pattern used by existing theme self-tests.

- [ ] **Step 7: Run focused Gradle + Android-free tests**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeRepositoryTest
kotlinc \
  app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSelectionState.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicy.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt \
  scripts/theme_stage23_2_multitheme_selftest.kt \
  -include-runtime -d /tmp/theme_stage23_2_multitheme_selftest.jar
java -jar /tmp/theme_stage23_2_multitheme_selftest.jar
```

Expected: focused repository tests PASS; Android-free self-test PASS.

- [ ] **Step 8: Commit persistence/migration**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRepositoryTest.kt \
        scripts/theme_stage23_2_multitheme_selftest.kt
git commit -m "feat(theme): persist premium pack selections safely"
```

---

### Task 4: Evolve `ThemeRepository` into the Single Theme-Selection Boundary

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRepositoryTest.kt`

**Interfaces:**
- Consumes: `ThemePreferencesCodec`, `ThemeResolutionPolicy`, DataStore.
- Produces:
  - `val selectionState: Flow<ThemeSelectionState>`
  - `val globalChromeTheme: Flow<KeyboardTheme>`
  - `fun surfaceTheme(surface): Flow<KeyboardTheme>`
  - `suspend fun applyGlobalPack(pack: ThemePack)`
  - `suspend fun setSurfaceOverride(surface, pack)`
  - `suspend fun clearSurfaceOverride(surface)`
  - `suspend fun resetToSelectedPack(pack): ResetThemeResult`

- [ ] **Step 1: Write repository behavior tests first**

Add tests:

```kotlin
@Test
fun changing_global_pack_keeps_surface_override() = runTest {
    val repo = repository(this)
    repo.applyGlobalPack(ThemePack.GLASS_MODERN)
    repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)
    repo.applyGlobalPack(ThemePack.GRADIENT_PRO)

    assertEquals(ThemePack.GRADIENT_PRO, repo.currentSelection().globalPack)
    assertEquals(
        ThemePack.CLEAN_LIGHT,
        repo.currentSelection().perSurfaceOverrides[KeyboardThemeSurface.ENGLISH]
    )
    assertEquals(
        ThemeCatalog.globalChrome(ThemePack.GRADIENT_PRO).id,
        repo.currentGlobalChromeTheme().id
    )
}

@Test
fun clear_surface_override_clears_only_that_surface() = runTest {
    val repo = repository(this)
    repo.applyGlobalPack(ThemePack.GLASS_MODERN)
    repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)
    repo.setSurfaceOverride(KeyboardThemeSurface.BIJOY, ThemePack.CLASSIC_DARK)
    repo.clearSurfaceOverride(KeyboardThemeSurface.ENGLISH)

    val state = repo.currentSelection()
    assertFalse(state.perSurfaceOverrides.containsKey(KeyboardThemeSurface.ENGLISH))
    assertEquals(ThemePack.CLASSIC_DARK, state.perSurfaceOverrides[KeyboardThemeSurface.BIJOY])
}

@Test
fun reset_to_selected_pack_is_single_persisted_transaction_and_returns_old_photo() = runTest {
    val repo = repository(this)
    repo.updateBackground(BackgroundPhotoConfig(true, "old.jpg"))
    repo.setSurfaceOverride(KeyboardThemeSurface.ENGLISH, ThemePack.CLEAN_LIGHT)

    val result = repo.resetToSelectedPack(ThemePack.GRADIENT_PRO)
    val state = repo.currentSelection()

    assertEquals(ThemePack.GRADIENT_PRO, state.globalPack)
    assertTrue(state.perSurfaceOverrides.isEmpty())
    assertEquals("old.jpg", result.backgroundFileToDelete)
    assertFalse(state.customTheme.background.enabled)
    assertTrue(state.customTheme.background.localFileName.isBlank())
}
```

- [ ] **Step 2: Run repository tests and confirm failure**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeRepositoryTest
```

Expected: compilation failure for missing repository APIs.

- [ ] **Step 3: Add selection/resolution flows while keeping the legacy `theme` flow**

Implement:

```kotlin
val selectionState: Flow<ThemeSelectionState> = data.map { it.selectionState }
val globalChromeTheme: Flow<KeyboardTheme> = selectionState.map(ThemeResolutionPolicy::resolveGlobalChrome)
val theme: Flow<KeyboardTheme> = globalChromeTheme // backward-compatible alias for non-IME callers

fun surfaceTheme(surface: KeyboardThemeSurface): Flow<KeyboardTheme> =
    selectionState.map { ThemeResolutionPolicy.resolveSurface(it, surface) }

suspend fun currentSelection(): ThemeSelectionState = selectionState.first()
suspend fun currentGlobalChromeTheme(): KeyboardTheme = globalChromeTheme.first()
suspend fun currentSurfaceTheme(surface: KeyboardThemeSurface): KeyboardTheme =
    ThemeResolutionPolicy.resolveSurface(currentSelection(), surface)
```

- [ ] **Step 4: Implement additive pack/override mutations with stale-key cleanup**

Use one repository helper that clears only premium selection keys before writing the new state:

```kotlin
private fun clearPremiumKeys(preferences: MutablePreferences) {
    preferences.remove(key(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID))
    preferences.remove(key(ThemePreferencesCodec.THEME_SELECTION_SCHEMA_VERSION))
    KeyboardThemeSurface.entries.forEach { surface ->
        preferences.remove(key(ThemePreferencesCodec.overrideKey(surface)))
    }
}

private suspend fun persistSelection(state: ThemeSelectionState) {
    val encoded = ThemePreferencesCodec.encodeSelection(state)
    store.edit { preferences ->
        clearPremiumKeys(preferences)
        writeEncoded(preferences, encoded)
    }
}
```

Then:

```kotlin
suspend fun applyGlobalPack(pack: ThemePack) =
    persistSelection(currentSelection().copy(globalPack = pack))

suspend fun setSurfaceOverride(surface: KeyboardThemeSurface, pack: ThemePack) =
    persistSelection(currentSelection().copy(
        perSurfaceOverrides = currentSelection().perSurfaceOverrides + (surface to pack)
    ))

suspend fun clearSurfaceOverride(surface: KeyboardThemeSurface) {
    val current = currentSelection()
    persistSelection(current.copy(perSurfaceOverrides = current.perSurfaceOverrides - surface))
}
```

Avoid calling `currentSelection()` twice inside one mutation; fetch once in final code.

- [ ] **Step 5: Implement reset as one preference transaction and return the file only after success**

Add:

```kotlin
data class ResetThemeResult(val backgroundFileToDelete: String?)

suspend fun resetToSelectedPack(pack: ThemePack): ResetThemeResult {
    val before = currentSelection()
    val fileToDelete = before.customTheme.background.localFileName.takeIf { it.isNotBlank() }
    val canonical = ThemeCatalog.globalChrome(pack)
        .copy(background = BackgroundPhotoConfig())
        .asCustom()
        .normalized()
    val resetState = ThemeSelectionState(
        globalPack = pack,
        perSurfaceOverrides = emptyMap(),
        legacyActiveThemeId = ThemePreset.socialAiNeon.id,
        customTheme = canonical
    )
    persistSelection(resetState) // throws on failure; caller must not delete file
    return ResetThemeResult(fileToDelete)
}
```

The Activity deletes `backgroundFileToDelete` only after this suspend function returns successfully.

- [ ] **Step 6: Preserve old preset/custom APIs**

`activatePreset(id)` and `saveCustom(theme)` must explicitly return the app to legacy/custom mode by setting `globalPack = null` while preserving premium overrides in storage only if the spec calls for them. For predictable UX, premium overrides should be cleared when the user intentionally activates a legacy/custom theme:

```kotlin
suspend fun activatePreset(id: String) {
    val normalized = normalizeLegacyPresetId(id)
    val current = currentSelection()
    persistSelection(current.copy(
        globalPack = null,
        perSurfaceOverrides = emptyMap(),
        legacyActiveThemeId = normalized
    ))
}
```

Use the same rule in `saveCustom(...)` with `legacyActiveThemeId = "custom"`.

- [ ] **Step 7: Run repository tests**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeRepositoryTest
```

Expected: PASS.

- [ ] **Step 8: Commit repository boundary changes**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeRepositoryTest.kt
git commit -m "feat(theme): add premium theme repository state"
```

---

### Task 5: Make Previews and Settings Content Surface-Aware

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreviewView.kt`
- Create: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSettingsUiStyler.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt`

**Interfaces:**
- Consumes: `KeyboardThemeSurface`, `KeyboardTheme`, `ThemeRenderer`.
- Produces: `ThemePreviewView.setPreview(theme, surface)`, settings-section styling helper.

- [ ] **Step 1: Write a preview contract test**

Add a Robolectric test that switches surfaces and verifies the preview retains the requested surface through a test-visible accessor:

```kotlin
@Test
fun preview_tracks_requested_surface() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preview = ThemePreviewView(context)
    preview.setPreview(ThemeCatalog.surface(ThemePack.GLASS_MODERN, KeyboardThemeSurface.BIJOY), KeyboardThemeSurface.BIJOY)
    assertEquals(KeyboardThemeSurface.BIJOY, preview.previewSurfaceForTest())
}
```

- [ ] **Step 2: Run the focused test and confirm failure**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest
```

Expected: compilation failure for `setPreview`/`previewSurfaceForTest`.

- [ ] **Step 3: Upgrade `ThemePreviewView` to render real surface categories**

Replace `setTheme` with a compatibility wrapper plus a new API:

```kotlin
private var surface: KeyboardThemeSurface = KeyboardThemeSurface.ENGLISH

fun setTheme(value: KeyboardTheme) = setPreview(value, surface)

fun setPreview(value: KeyboardTheme, surface: KeyboardThemeSurface) {
    theme = value.normalized()
    this.surface = surface
    invalidate()
}

internal fun previewSurfaceForTest(): KeyboardThemeSurface = surface
```

Use label arrays rather than blank rectangles:

```kotlin
private fun rowsFor(surface: KeyboardThemeSurface): List<List<String>> = when (surface) {
    KeyboardThemeSurface.ENGLISH -> listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM" ).map { it.map(Char::toString) }
    KeyboardThemeSurface.NUMBER -> listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("0",".","↵"))
    KeyboardThemeSurface.SYMBOL -> listOf(listOf("@","#","$","%"), listOf("&","*","(",")"), listOf("+","-","=","/"))
    KeyboardThemeSurface.BANGLA -> listOf(listOf("অ","আ","ই","ঈ"), listOf("ক","খ","গ","ঘ"), listOf("ত","থ","দ","ধ"))
    KeyboardThemeSurface.PHONETIC -> listOf(listOf("a","m","i","t"), listOf("আমি","তুমি","কি"), listOf("space","↵"))
    KeyboardThemeSurface.BIJOY -> listOf(listOf("ক","ি","া","র"), listOf("ে","ন","ম","ত"), listOf("space","↵"))
    KeyboardThemeSurface.SETTINGS -> listOf(listOf("Theme Pack"), listOf("Customize"), listOf("Background"), listOf("Reset"))
}
```

Draw readable labels with `theme.keyLabel`/`textPrimary` using the same contrast philosophy as `ThemeRenderer`.

- [ ] **Step 4: Add Settings content styler**

Create `ThemeSettingsUiStyler.kt` with a single responsibility:

```kotlin
class ThemeSettingsUiStyler(private val renderer: ThemeRenderer) {
    fun apply(
        root: View,
        globalChromeTheme: KeyboardTheme,
        settingsTheme: KeyboardTheme,
        sectionContainers: List<View>
    ) {
        root.setBackgroundColor(settingsTheme.rootBackground)
        sectionContainers.forEach { it.background = renderer.panelBackground(settingsTheme, settingsTheme.primaryNeon) }
        renderer.styleTree(root, settingsTheme)
        root.findViewById<TextView>(R.id.theme_settings_title)?.let { renderer.styleText(it, globalChromeTheme) }
    }
}
```

Keep top-level title/global chrome on the global pack while cards/controls use the resolved SETTINGS surface.

- [ ] **Step 5: Run preview/settings tests**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest
```

Expected: PASS for preview contract tests added in this task; full Activity layout tests may remain pending Task 6.

- [ ] **Step 6: Commit preview/styling unit**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreviewView.kt \
        app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSettingsUiStyler.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt
git commit -m "feat(theme): add surface-aware theme previews"
```

---

### Task 6: Redesign Theme Settings UX and Implement Manual-Pack Reset Flow

**Files:**
- Modify: `app/src/main/res/layout/activity_theme_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt`

**Interfaces:**
- Consumes: repository APIs from Task 4, preview/styler from Task 5, `ThemeBackgroundManager.removePhoto`.
- Produces: complete pack cards, seven surface customizers, advanced/legacy sections, reset confirmation + manual pack picker.

- [ ] **Step 1: Replace the old preset-count test with premium/legacy structure tests**

Add stable IDs in XML and tests:

```kotlin
@Test
fun activity_renders_four_premium_pack_choices_and_seven_surface_customizers() {
    val activity = Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().get()
    val packs = activity.findViewById<LinearLayout>(R.id.theme_pack_container)
    val surfaces = activity.findViewById<LinearLayout>(R.id.theme_surface_container)
    assertEquals(4, packs.childCount)
    assertEquals(7, surfaces.childCount)
}

@Test
fun legacy_presets_remain_available() {
    val activity = Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().get()
    val legacy = activity.findViewById<LinearLayout>(R.id.theme_legacy_preset_container)
    assertEquals(8, legacy.childCount)
}
```

Add a test-only reset helper path if Robolectric dialog interaction is brittle:

```kotlin
@Test
fun reset_confirmation_cancel_does_not_invoke_repository_reset() {
    val activity = Robolectric.buildActivity(ThemeSettingsActivity::class.java).setup().get()
    assertFalse(activity.resetMutationStartedForTest())
    activity.showResetConfirmationForTest()
    activity.cancelResetForTest()
    assertFalse(activity.resetMutationStartedForTest())
}
```

- [ ] **Step 2: Run Activity tests and confirm failure**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest
```

Expected: missing IDs/new structure failures.

- [ ] **Step 3: Restructure `activity_theme_settings.xml` into explicit sections**

Keep a `ScrollView`, but add these stable containers/IDs in order:

```xml
<LinearLayout android:id="@+id/theme_pack_section" ...>
    <TextView android:text="Complete Theme Packs" ... />
    <LinearLayout android:id="@+id/theme_pack_container" android:orientation="vertical" ... />
</LinearLayout>

<LinearLayout android:id="@+id/theme_surface_section" ...>
    <TextView android:text="Customize Each Keyboard" ... />
    <LinearLayout android:id="@+id/theme_surface_container" android:orientation="vertical" ... />
</LinearLayout>

<LinearLayout android:id="@+id/theme_advanced_section" ...>
    <!-- move existing custom controls/background controls here without dropping IDs -->
</LinearLayout>

<LinearLayout android:id="@+id/theme_legacy_section" ...>
    <LinearLayout android:id="@+id/theme_legacy_preset_container" ... />
</LinearLayout>

<Button
    android:id="@+id/button_reset_theme"
    android:text="Reset Theme"
    ... />
```

Preserve all existing editor/background control IDs so old functionality and tests continue to work.

- [ ] **Step 4: Build premium pack cards and seven surface rows dynamically**

In `ThemeSettingsActivity`:

```kotlin
private fun setupPremiumPackCards() {
    val container = findViewById<LinearLayout>(R.id.theme_pack_container)
    ThemePack.entries.forEach { pack ->
        container.addView(buildThemeChoiceButton(pack.displayName) {
            lifecycleScope.launch { repository.applyGlobalPack(pack) }
        })
    }
}

private fun setupSurfaceCards() {
    val container = findViewById<LinearLayout>(R.id.theme_surface_container)
    KeyboardThemeSurface.entries.forEach { surface ->
        container.addView(buildThemeChoiceButton(surface.displayName) {
            showSurfacePicker(surface)
        })
    }
}
```

`showSurfacePicker(surface)` displays exactly five choices: `Use Global Pack Design` plus the four pack-specific variant display names from `ThemeCatalog.surface(pack, surface).displayName`.

Selecting `Use Global Pack Design` calls `repository.clearSurfaceOverride(surface)`; selecting a pack calls `repository.setSurfaceOverride(surface, pack)`.

- [ ] **Step 5: Keep legacy/custom UI functional and secondary**

Move existing `ThemePreset.builtIns` button creation from `theme_preset_container` to `theme_legacy_preset_container`. Keep `saveCustomFromControls()`, background import/update/remove, contrast warning, and existing control listeners intact. Replace old `Reset Custom to Social AI Neon` behavior with a neutral `Reset Custom Controls` action only if retained; the product-level reset is `button_reset_theme` and follows the approved two-step flow.

- [ ] **Step 6: Implement the approved two-step reset interaction**

```kotlin
private fun showResetConfirmation() {
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.theme_reset_title))
        .setMessage(getString(R.string.theme_reset_message))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.theme_reset_continue) { _, _ -> showResetPackPicker() }
        .show()
}

private fun showResetPackPicker() {
    val packs = ThemePack.entries.toTypedArray()
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.theme_reset_choose_pack))
        .setItems(packs.map { it.displayName }.toTypedArray()) { _, which ->
            val selected = packs[which]
            lifecycleScope.launch {
                val result = repository.resetToSelectedPack(selected)
                result.backgroundFileToDelete?.let(backgroundManager::removePhoto)
                toast("${selected.displayName} applied.")
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
```

No repository mutation occurs in `showResetConfirmation()` or merely opening/dismissing `showResetPackPicker()`.

- [ ] **Step 7: Collect state live and update selected indicators/previews/styles**

Collect `repository.selectionState` in `repeatOnLifecycle(Lifecycle.State.STARTED)`, then:

```kotlin
val globalTheme = ThemeResolutionPolicy.resolveGlobalChrome(state)
val settingsTheme = ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.SETTINGS)
uiStyler.apply(root, globalTheme, settingsTheme, sectionContainers)
preview.setPreview(settingsTheme, KeyboardThemeSurface.SETTINGS)
```

When a surface picker opens, preview the currently resolved theme for that surface. Pack cards show active checkmark only when `state.globalPack == pack`. Legacy active label remains meaningful when `globalPack == null`.

- [ ] **Step 8: Run the Settings tests**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest
```

Expected: PASS, including 4 pack cards, 7 surface cards, 8 legacy presets, reset cancel/no-mutation tests.

- [ ] **Step 9: Commit Settings UX**

```bash
git add app/src/main/res/layout/activity_theme_settings.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ThemeSettingsActivityTest.kt
git commit -m "feat(theme): add premium theme settings experience"
```

---

### Task 7: Integrate Dual Theme Resolution into the IME Without Touching Typing Logic

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- Modify: `app/src/test/java/com/socialaiassistant/keyboard/theme/ImeThemeIntegrationTest.kt`

**Interfaces:**
- Consumes: `ThemeSelectionState`, `ThemeResolutionPolicy`, `KeyboardUiMode`, existing renderer methods.
- Produces: `currentGlobalChromeTheme`, `currentSurfaceTheme`, `surfaceForMode(mode)`, split renderer application paths.

- [ ] **Step 1: Write mode mapping/global-chrome tests first**

Add pure/static coverage in `ImeThemeIntegrationTest` or a small helper exposed `internal` for tests:

```kotlin
@Test
fun mode_to_theme_surface_mapping_is_stable() {
    assertEquals(KeyboardThemeSurface.ENGLISH, ImeThemeSurfaceResolver.forMode(KeyboardUiMode()))
    assertEquals(
        KeyboardThemeSurface.NUMBER,
        ImeThemeSurfaceResolver.forMode(KeyboardUiMode(layer = KeyboardLayer.NUMBERS))
    )
    assertEquals(
        KeyboardThemeSurface.SYMBOL,
        ImeThemeSurfaceResolver.forMode(KeyboardUiMode(layer = KeyboardLayer.SYMBOLS))
    )
    assertEquals(
        KeyboardThemeSurface.PHONETIC,
        ImeThemeSurfaceResolver.forMode(
            KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC)
        )
    )
    assertEquals(
        KeyboardThemeSurface.BIJOY,
        ImeThemeSurfaceResolver.forMode(
            KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY)
        )
    )
}
```

Also verify global chrome ignores an English override:

```kotlin
@Test
fun toolbar_theme_stays_global_when_key_surface_is_overridden() {
    val state = ThemeSelectionState(
        globalPack = ThemePack.GLASS_MODERN,
        perSurfaceOverrides = mapOf(KeyboardThemeSurface.ENGLISH to ThemePack.CLEAN_LIGHT),
        legacyActiveThemeId = ThemePreset.socialAiNeon.id,
        customTheme = ThemePreset.customFrom()
    )
    assertEquals("premium_glass_modern_chrome", ThemeResolutionPolicy.resolveGlobalChrome(state).id)
    assertTrue(ThemeResolutionPolicy.resolveSurface(state, KeyboardThemeSurface.ENGLISH).id.contains("clean_light"))
}
```

- [ ] **Step 2: Run IME theme tests and confirm failure**

```bash
./gradlew testDebugUnitTest --tests com.socialaiassistant.keyboard.theme.ImeThemeIntegrationTest
```

Expected: compilation failure for the resolver/dual theme path.

- [ ] **Step 3: Add a pure `ImeThemeSurfaceResolver` next to theme integration helpers**

Place it in `theme/` or `ime/` according to package dependency direction; if placed in `ime/`, it may import `KeyboardThemeSurface` without making the theme package depend on IME internals:

```kotlin
internal object ImeThemeSurfaceResolver {
    fun forMode(mode: KeyboardUiMode): KeyboardThemeSurface = when (mode.layer) {
        KeyboardLayer.NUMBERS -> KeyboardThemeSurface.NUMBER
        KeyboardLayer.SYMBOLS -> KeyboardThemeSurface.SYMBOL
        KeyboardLayer.LETTERS -> when (mode.language) {
            KeyboardLanguage.ENGLISH -> KeyboardThemeSurface.ENGLISH
            KeyboardLanguage.BANGLA -> when (mode.banglaMode) {
                BanglaInputMode.PHONETIC -> KeyboardThemeSurface.PHONETIC
                BanglaInputMode.BIJOY -> KeyboardThemeSurface.BIJOY
            }
        }
    }
}
```

The `BANGLA` catalog variant remains available in Settings/previews as the shared language-family design; this task does not create a new typing layout.

- [ ] **Step 4: Split the service's single `currentTheme` into global chrome and active surface**

Replace:

```kotlin
private var currentTheme: KeyboardTheme = ThemePreset.socialAiNeon
```

with:

```kotlin
private var currentThemeState = ThemeSelectionState(
    globalPack = null,
    perSurfaceOverrides = emptyMap(),
    legacyActiveThemeId = ThemePreset.socialAiNeon.id,
    customTheme = ThemePreset.customFrom()
)
private var currentGlobalChromeTheme: KeyboardTheme = ThemePreset.socialAiNeon
private var currentSurfaceTheme: KeyboardTheme = ThemePreset.socialAiNeon
```

Collect `app.themeRepository.selectionState` instead of only `theme`:

```kotlin
app.themeRepository.selectionState.collectLatest { state ->
    currentThemeState = state
    currentGlobalChromeTheme = ThemeResolutionPolicy.resolveGlobalChrome(state).normalized()
    currentSurfaceTheme = ThemeResolutionPolicy.resolveSurface(
        state,
        ImeThemeSurfaceResolver.forMode(keyboardMode)
    ).normalized()
    rootView?.let { applyTheme(it, rebuildDynamic = true) }
}
```

- [ ] **Step 5: Use surface theme only for key geometry and key buttons**

In `renderKeys()` use:

```kotlin
val keyTheme = currentSurfaceTheme
val gap = dp((keyTheme.keyGapDp / 2f).coerceAtLeast(0f))
// ...
themeRenderer.styleButton(button, themeRoleFor(key.action), keyTheme)
```

Update any render signature fields that currently use `currentTheme.keyGapDp` to use the active surface theme so visual geometry invalidation remains correct.

- [ ] **Step 6: Use global chrome theme for toolbar/suggestions/AI/global panels**

Add explicit renderer entry points if needed:

```kotlin
fun applyGlobalChrome(root: View, theme: KeyboardTheme) {
    root.setBackgroundColor(theme.rootBackground)
    root.findViewById<View>(R.id.keyboard_toolbar)?.background = drawables.toolbar(theme)
    styleSmartReply(root, theme)
}

fun applyKeyPanelSurface(root: View, theme: KeyboardTheme) {
    root.findViewById<View>(R.id.keyboard_panel_container)?.background = drawables.panel(theme, theme.primaryNeon)
}
```

Then `applyTheme(...)` uses:

```kotlin
themeRenderer.applyGlobalChrome(root, currentGlobalChromeTheme)
themeRenderer.applyKeyPanelSurface(root, currentSurfaceTheme)
root.findViewById<View>(R.id.keyboard_content)?.setPadding(
    dp(currentSurfaceTheme.outerPaddingDp),
    dp(currentSurfaceTheme.outerPaddingDp),
    dp(currentSurfaceTheme.outerPaddingDp),
    dp(currentSurfaceTheme.outerPaddingDp)
)
smartReplyController?.applyTheme(currentGlobalChromeTheme, themeRenderer)
// toolbar/AI/global text/buttons use currentGlobalChromeTheme
```

All AI panel buttons/status text, suggestion/header chrome, emoji/clipboard utility controls, and toolbar controls use `currentGlobalChromeTheme`. Actual alphanumeric/number/symbol key buttons use `currentSurfaceTheme`.

- [ ] **Step 7: Re-resolve the active surface on mode changes without touching composition/engines**

Immediately after `keyboardMode = keyboardMode.reduce(action)` for layer/language/Bangla-mode actions:

```kotlin
private fun refreshActiveSurfaceTheme() {
    currentSurfaceTheme = ThemeResolutionPolicy.resolveSurface(
        currentThemeState,
        ImeThemeSurfaceResolver.forMode(keyboardMode)
    ).normalized()
}
```

Call `refreshActiveSurfaceTheme()` before `renderKeys()`. Do not call any dictionary/model reload, `restartInput`, or backend function.

- [ ] **Step 8: Keep background semantics global/custom and safe**

Background photo is a global advanced appearance setting, so `refreshThemeBackground` reads `currentGlobalChromeTheme.background` when in legacy/custom mode. Premium catalog themes have no background photo. A reset clears custom background state through Task 4 before the Activity removes the file.

- [ ] **Step 9: Run IME theme tests and typing regressions**

```bash
./gradlew testDebugUnitTest \
  --tests com.socialaiassistant.keyboard.theme.ImeThemeIntegrationTest \
  --tests com.socialaiassistant.keyboard.ime.ImeTypingEngineTest \
  --tests com.socialaiassistant.keyboard.ime.Stage19TouchResponsivenessTest
```

Expected: PASS; no typing behavior changes.

- [ ] **Step 10: Commit IME integration**

```bash
git add app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt \
        app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt \
        app/src/test/java/com/socialaiassistant/keyboard/theme/ImeThemeIntegrationTest.kt
git commit -m "feat(theme): resolve global chrome and keyboard surfaces separately"
```

---

### Task 8: Add Stage 23.2 Verification, Build in Android Studio-Compatible Form, and Package

**Files:**
- Create: `scripts/verify_theme_stage23_2.py`
- Create: `docs/STAGE23_2_PREMIUM_MULTI_THEME_UI.md`
- Create: `docs/STAGE23_2_VERIFICATION_REPORT.md` during execution from actual outputs
- Modify only if required: `README.md` to point Android Studio users to the Stage 23.2 theme verification/build steps

**Interfaces:**
- Consumes: all previous tasks and the Stage 23.1 baseline copy for protected-area comparison.
- Produces: reproducible verification commands, protected-code diff evidence, Android Studio-ready source ZIP.

- [ ] **Step 1: Write a source-contract verifier before final verification**

`verify_theme_stage23_2.py` must fail unless it finds all required types/keys/flows. Include explicit checks, for example:

```python
checks = {
    "four_theme_packs": "enum class ThemePack" in pack_text and pack_text.count("(") >= 4,
    "seven_surfaces": all(name in surface_text for name in ["ENGLISH", "NUMBER", "SYMBOL", "BANGLA", "PHONETIC", "BIJOY", "SETTINGS"]),
    "global_pack_key": "THEME_GLOBAL_PACK_ID" in codec_text,
    "manual_reset_api": "resetToSelectedPack" in repo_text,
    "dual_ime_theme": "currentGlobalChromeTheme" in ime_text and "currentSurfaceTheme" in ime_text,
    "normal_key_no_debounce_rule_preserved": "TEXT_KEY" not in any newly introduced rapid-action gate path,
}
```

Have the script print `PASS/FAIL` per contract and return non-zero on failure.

- [ ] **Step 2: Run focused new theme suites**

```bash
./gradlew testDebugUnitTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeCatalogTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeResolutionPolicyTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeRepositoryTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeRendererTest \
  --tests com.socialaiassistant.keyboard.theme.ThemeSettingsActivityTest \
  --tests com.socialaiassistant.keyboard.theme.ImeThemeIntegrationTest
```

Expected: PASS.

- [ ] **Step 3: Run the existing Stage 1–23 relevant regression/static chain**

Run these existing static/source-contract checks exactly:

```bash
python3 scripts/verify_typing_stage7.py
python3 scripts/verify_typing_stage18.py
python3 scripts/verify_typing_stage19.py
python3 scripts/verify_typing_stage20.py
python3 scripts/verify_typing_stage21.py
python3 scripts/verify_typing_stage22.py
python3 scripts/verify_typing_stage23.py
python3 scripts/stage23_evidence_parser_selftest.py
python3 scripts/verify_theme_stage23_2.py
```

Run the existing theme core/codec self-tests against the new source:

```bash
kotlinc \
  app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSelectionState.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicy.kt \
  scripts/theme_core_selftest.kt \
  -include-runtime -d /tmp/theme_core_selftest.jar
java -jar /tmp/theme_core_selftest.jar

kotlinc \
  app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreset.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeSelectionState.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeResolutionPolicy.kt \
  app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt \
  scripts/theme_codec_selftest.kt \
  -include-runtime -d /tmp/theme_codec_selftest.jar
java -jar /tmp/theme_codec_selftest.jar
```

Record the actual output counts/results in `docs/STAGE23_2_VERIFICATION_REPORT.md`; do not reuse historical counts unless the commands were executed during this implementation.

- [ ] **Step 4: Run compile/test/APK build where Android tooling is available**

```bash
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug
```

Expected: BUILD SUCCESSFUL. If the environment lacks a usable Android SDK/Gradle wrapper/network, state that limitation in the report and do not claim these commands passed.

- [ ] **Step 5: Compare protected areas against the Stage 23.1 baseline**

From two extracted trees:

```bash
diff -qr STAGE23_1_BASELINE/app/src/main/java/com/socialaiassistant/keyboard/ai \
         CURRENT/app/src/main/java/com/socialaiassistant/keyboard/ai
diff -qr STAGE23_1_BASELINE/app/src/main/java/com/socialaiassistant/keyboard/backend \
         CURRENT/app/src/main/java/com/socialaiassistant/keyboard/backend
diff -qr STAGE23_1_BASELINE/app/src/main/java/com/socialaiassistant/keyboard/context \
         CURRENT/app/src/main/java/com/socialaiassistant/keyboard/context
diff -qr STAGE23_1_BASELINE/app/src/main/java/com/socialaiassistant/keyboard/safety \
         CURRENT/app/src/main/java/com/socialaiassistant/keyboard/safety
```

Expected: no output for all four protected directories.

Also compare Stage 21/22 prediction files explicitly:

```bash
cmp -s STAGE23_1_BASELINE/app/src/main/java/com/socialaiassistant/keyboard/ime/Stage21PredictionProfiling.kt \
       CURRENT/app/src/main/java/com/socialaiassistant/keyboard/ime/Stage21PredictionProfiling.kt
cmp -s STAGE23_1_BASELINE/app/src/main/java/com/socialaiassistant/keyboard/ime/Stage22PredictionConfidence.kt \
       CURRENT/app/src/main/java/com/socialaiassistant/keyboard/ime/Stage22PredictionConfidence.kt
```

Expected: both return status 0.

- [ ] **Step 6: Perform a final Android Studio manual smoke pass**

On the detected physical device or emulator, verify this exact sequence:

```text
Open Theme Settings
→ apply Glass Modern globally
→ English override = Clean Light
→ Bangla Phonetic override = Gradient Pro
→ switch English ↔ Bangla Phonetic ↔ Bijoy ↔ Number ↔ Symbol
→ confirm toolbar/AI remain Glass Modern
→ confirm only selected key surfaces change
→ choose a background photo in Advanced Custom Appearance
→ cancel Reset confirmation: nothing changes
→ confirm Reset, dismiss pack picker: nothing changes
→ confirm Reset, choose Classic Dark: all overrides/custom/background clear and Classic Dark becomes global
→ close/reopen keyboard and Theme Settings: state persists
```

Also verify password/OTP fields still block AI/context exactly as before.

- [ ] **Step 7: Write the Stage 23.2 change/verification documents from actual results**

`docs/STAGE23_2_PREMIUM_MULTI_THEME_UI.md` documents the feature and migration model. `docs/STAGE23_2_VERIFICATION_REPORT.md` records only commands actually executed, their PASS/FAIL results, build limitations, protected diffs, and manual-device evidence if performed.

- [ ] **Step 8: Package the final Android Studio-ready source ZIP**

From the verified source root:

```bash
zip -r /mnt/data/Social_AI_Keyboard_v35.3.1_TYPING_CORE_V2_STAGE23_2_PREMIUM_MULTI_THEME_UI_SOURCE.zip . \
  -x '.git/*' '.gradle/*' 'build/*' 'app/build/*' 'local.properties'
sha256sum /mnt/data/Social_AI_Keyboard_v35.3.1_TYPING_CORE_V2_STAGE23_2_PREMIUM_MULTI_THEME_UI_SOURCE.zip
unzip -t /mnt/data/Social_AI_Keyboard_v35.3.1_TYPING_CORE_V2_STAGE23_2_PREMIUM_MULTI_THEME_UI_SOURCE.zip
```

Expected: ZIP integrity PASS and SHA-256 recorded in the verification report.

- [ ] **Step 9: Commit verification/docs if Git metadata is available**

```bash
git add scripts/verify_theme_stage23_2.py \
        docs/STAGE23_2_PREMIUM_MULTI_THEME_UI.md \
        docs/STAGE23_2_VERIFICATION_REPORT.md \
        README.md
git commit -m "test(theme): verify premium multi-theme release"
```

If this extracted source has no `.git` directory, document that commits cannot be created here; do not fabricate commit history.

---

## Self-Review Result

- **Spec coverage:** All approved requirements are assigned to Tasks 1–8: four packs/28 variants, per-surface overrides, global toolbar/AI rule, existing custom/legacy preservation, preview categories, Settings surface styling, manual-pack reset, persistence/migration, performance isolation, protected code, and build/regression verification.
- **Completeness scan:** Every implementation/testing step names concrete files, interfaces, commands, and expected outcomes; no intentionally unfinished step remains.
- **Type consistency:** `ThemePack`, `KeyboardThemeSurface`, `ThemeSelectionState`, `ThemeCatalog`, `ThemeResolutionPolicy`, repository API names, and reset result names are consistent across tasks.
- **Review Focus coverage:** Legacy migration is tested in Task 3; reset cancellation/dismissal in Tasks 4/6; global-vs-override behavior in Tasks 2/4/7; drawable/cache contrast/style in Task 1; Bangla Phonetic/Bijoy runtime mapping in Task 7.
- **Scope check:** This remains one subsystem—the theme/presentation layer—with one controlled IME integration boundary. No AI/backend/prediction work is included.
