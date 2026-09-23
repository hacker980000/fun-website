# Stage 24.3 Dedicated Settings Category Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert Social AI Keyboard settings from one long scroll page into an eight-destination Settings Hub where every category opens its own dedicated settings screen, without changing existing saved values or backend/keyboard behavior.

**Architecture:** `MainActivity` becomes a navigation-only hub. A new reusable `SettingsCategoryActivity` accepts a pure-Kotlin `SettingsCategoryId`, inflates one of eight category content layouts inside a shared header/root, applies the selected Settings Theme Pack, binds the existing repositories/actions for that category, and refreshes only that category's state. `SettingsThemeDashboardRenderer` renders the same canonical eight categories for every Settings Theme Pack; visual presentation may differ, but navigation and ordering do not.

**Tech Stack:** Kotlin, Android AppCompat Activities, Android XML layouts, DataStore-backed existing repositories, existing `SettingsThemeCatalog`/`SettingsThemeStyler`, Python static contract scripts, standalone `kotlinc` self-tests, existing Stage 23/24 regression scripts.

**Spec:** `docs/superpowers/specs/2026-09-22-dedicated-settings-category-navigation-design.md`

## Global Constraints

- Baseline is **Stage 24.2 Optional Theme Bubble**.
- The Settings Hub is navigation-only; no category checkbox, editor, backend/account action, keyboard action, or theme preference control remains on the Hub.
- There are exactly eight canonical categories, in this order: Keyboard Setup; Language & Input; Theme & Appearance; Typing & Suggestions; AI & Privacy; Clipboard; Account & Subscription; Help & About.
- Clean Modern, Card Style, Premium, and Pro Style expose the same eight destinations in the same logical order; only styling/layout presentation differs.
- Reuse current repositories, preference keys, managed-session/backend services, gateway logic, theme repositories, and `SecretStore`; do not introduce duplicate state.
- Number Row remains global. Key Boundary and Bubble Effect/Bubble Style remain per keyboard Theme Pack.
- Preserve `MainActivity.EXTRA_OPEN_SECTION` and `MainActivity.SECTION_AI_PRIVACY`; legacy AI & Privacy deep links open the dedicated AI & Privacy page.
- Android back and header back return normally; no nested custom category stack is introduced.
- Unknown category input must not display a blank/partial page; route safely to the Settings Hub.
- Do not add a ninth Voice category or migrate to Fragments/Navigation Component.
- Do not claim APK/build success while the verified `gradle-wrapper.jar` remains unavailable.

## File Map

**Create**
- `app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt` — pure category identity, wire parsing, canonical ordering, legacy section mapping.
- `app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt` — shared category-screen lifecycle, category-specific binding/state refresh, theming, back/fallback behavior.
- `app/src/main/res/layout/activity_settings_category.xml` — common themed header/back + category-content host.
- `app/src/main/res/layout/settings_category_keyboard_setup.xml`
- `app/src/main/res/layout/settings_category_language_input.xml`
- `app/src/main/res/layout/settings_category_theme_appearance.xml`
- `app/src/main/res/layout/settings_category_typing_suggestions.xml`
- `app/src/main/res/layout/settings_category_ai_privacy.xml`
- `app/src/main/res/layout/settings_category_clipboard.xml`
- `app/src/main/res/layout/settings_category_account_subscription.xml`
- `app/src/main/res/layout/settings_category_help_about.xml`
- `scripts/verify_settings_navigation_stage24_3.py` — Stage 24.3 static contract/regression guard.
- `scripts/settings_navigation_stage24_3_selftest.kt` — pure Kotlin routing/order tests.
- `scripts/run_settings_navigation_stage24_3_selftest.sh` — standalone `kotlinc` runner.
- `docs/STAGE24_3_DEDICATED_SETTINGS_NAVIGATION.md`
- `docs/STAGE24_3_VERIFICATION_REPORT.md`

**Modify**
- `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt` — remove category business logic; render hub, launch categories, route legacy deep link.
- `app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeDashboardRenderer.kt` — emit canonical `SettingsCategoryId` and all eight categories for every theme.
- `app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt` — add category-screen styling entry point while retaining hub styling.
- `app/src/main/res/layout/activity_main.xml` — hub-only header/status/dashboard; remove all old `section_*` option containers.
- `app/src/main/AndroidManifest.xml` — register `.SettingsCategoryActivity` as non-exported.
- `app/src/main/res/values/strings.xml` — inner-page titles/subtitles/back content description and any missing category copy.
- Existing Stage 23.9/24.0/24.2 verifiers only if they assert old same-page structure; update them to accept the migrated location without weakening their behavioral contract.

## Review Focus

The following five failure modes are high-risk and must be pinned by tests in the owning tasks:

1. **Theme-dependent missing category:** any of the four Settings Theme Packs omits one of the eight destinations. Task 1's static verifier must count all eight categories for every pack.
2. **Legacy deep link loops or opens Hub instead of AI & Privacy:** Task 3's routing self-test/static check must prove `SECTION_AI_PRIVACY` maps directly to `AI_PRIVACY` and MainActivity does not scroll.
3. **Unknown category produces blank screen or activity loop:** Task 2's pure routing test/static check must prove invalid wire values resolve to `null` and the Activity finishes/opens Hub only when not already returning to one.
4. **Moving controls silently resets or forks saved state:** Tasks 4–6 static checks must assert existing `SettingsRepository`, `ThemeRepository`, `SettingsThemeRepository`, `SecretStore`, managed-session/backend APIs and current preference methods are used rather than new raw keys.
5. **Settings Theme change inside Theme & Appearance leaves the current page visually stale:** Task 5 must test/verify `onResume()` reapplies `SettingsThemeStyler` and refreshes category state after returning from `SettingsThemeOnboardingActivity`.

---

### Task 1: Canonical Category Model and Eight-Destination Dashboard

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt`
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeDashboardRenderer.kt`
- Create: `scripts/settings_navigation_stage24_3_selftest.kt`
- Create: `scripts/run_settings_navigation_stage24_3_selftest.sh`
- Create: `scripts/verify_settings_navigation_stage24_3.py`

**Interfaces:**
- Consumes: existing `SettingsThemePack`, `SettingsThemeCatalog`, `SettingsThemeStyler`.
- Produces: `enum class SettingsCategoryId(val wireValue: String)`; `SettingsCategoryId.fromWireValue(String?): SettingsCategoryId?`; `SettingsCategoryId.fromLegacySection(String?): SettingsCategoryId?`; `SettingsCategoryId.canonical: List<SettingsCategoryId>`; dashboard callback `(SettingsCategoryId) -> Unit`.

- [ ] **Step 1: Write the pure routing/order test before the model exists**

Create `scripts/settings_navigation_stage24_3_selftest.kt`:

```kotlin
import com.socialaiassistant.keyboard.settingsui.SettingsCategoryId

fun main() {
    var passed = 0
    fun checkCase(name: String, ok: Boolean) {
        check(ok) { name }
        passed += 1
    }

    val expected = listOf(
        SettingsCategoryId.KEYBOARD_SETUP,
        SettingsCategoryId.LANGUAGE_INPUT,
        SettingsCategoryId.THEME_APPEARANCE,
        SettingsCategoryId.TYPING_SUGGESTIONS,
        SettingsCategoryId.AI_PRIVACY,
        SettingsCategoryId.CLIPBOARD,
        SettingsCategoryId.ACCOUNT_SUBSCRIPTION,
        SettingsCategoryId.HELP_ABOUT
    )

    checkCase("exactly eight canonical categories", SettingsCategoryId.canonical == expected)
    checkCase("wire parse theme", SettingsCategoryId.fromWireValue("theme_appearance") == SettingsCategoryId.THEME_APPEARANCE)
    checkCase("wire parse account", SettingsCategoryId.fromWireValue("account_subscription") == SettingsCategoryId.ACCOUNT_SUBSCRIPTION)
    checkCase("invalid wire is null", SettingsCategoryId.fromWireValue("missing") == null)
    checkCase("null wire is null", SettingsCategoryId.fromWireValue(null) == null)
    checkCase("legacy ai privacy maps", SettingsCategoryId.fromLegacySection("ai_privacy") == SettingsCategoryId.AI_PRIVACY)
    checkCase("unknown legacy section is null", SettingsCategoryId.fromLegacySection("typing") == null)
    checkCase("wire values are unique", SettingsCategoryId.entries.map { it.wireValue }.distinct().size == 8)

    println("Stage 24.3 settings category policy: $passed/8 PASS")
}
```

Create `scripts/run_settings_navigation_stage24_3_selftest.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt" \
  "$ROOT/scripts/settings_navigation_stage24_3_selftest.kt" \
  -include-runtime -d "$TMP/stage24_3_settings_category_selftest.jar"
java -jar "$TMP/stage24_3_settings_category_selftest.jar"
```

Run:

```bash
bash scripts/run_settings_navigation_stage24_3_selftest.sh
```

Expected: FAIL because `SettingsCategoryId.kt` does not exist.

- [ ] **Step 2: Create the canonical pure-Kotlin model**

Create `SettingsCategoryId.kt` exactly around this contract:

```kotlin
package com.socialaiassistant.keyboard.settingsui

enum class SettingsCategoryId(val wireValue: String) {
    KEYBOARD_SETUP("keyboard_setup"),
    LANGUAGE_INPUT("language_input"),
    THEME_APPEARANCE("theme_appearance"),
    TYPING_SUGGESTIONS("typing_suggestions"),
    AI_PRIVACY("ai_privacy"),
    CLIPBOARD("clipboard"),
    ACCOUNT_SUBSCRIPTION("account_subscription"),
    HELP_ABOUT("help_about");

    companion object {
        val canonical: List<SettingsCategoryId> = entries.toList()

        fun fromWireValue(value: String?): SettingsCategoryId? =
            entries.firstOrNull { it.wireValue == value }

        fun fromLegacySection(value: String?): SettingsCategoryId? = when (value) {
            "ai_privacy" -> AI_PRIVACY
            else -> null
        }
    }
}
```

- [ ] **Step 3: Run the pure test GREEN**

Run:

```bash
bash scripts/run_settings_navigation_stage24_3_selftest.sh
```

Expected: `Stage 24.3 settings category policy: 8/8 PASS`.

- [ ] **Step 4: Write the first static dashboard contract and watch it fail**

Create `scripts/verify_settings_navigation_stage24_3.py` with initial checks that read the category model and renderer:

```python
#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
def read(rel):
    path = ROOT / rel
    return path.read_text() if path.exists() else ""

category = read("app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt")
renderer = read("app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeDashboardRenderer.kt")
checks = []
def check(name, ok): checks.append((name, bool(ok)))

for token in (
    "KEYBOARD_SETUP", "LANGUAGE_INPUT", "THEME_APPEARANCE", "TYPING_SUGGESTIONS",
    "AI_PRIVACY", "CLIPBOARD", "ACCOUNT_SUBSCRIPTION", "HELP_ABOUT"
):
    check(f"canonical category {token}", token in category)

check("dashboard uses canonical SettingsCategoryId", "SettingsCategoryId" in renderer and "enum class Category" not in renderer)
check("dashboard callback emits canonical ids", "onCategory: (SettingsCategoryId) -> Unit" in renderer)
for title in (
    "Keyboard Setup", "Language & Input", "Theme & Appearance", "Typing & Suggestions",
    "AI & Privacy", "Clipboard", "Account & Subscription", "Help & About"
):
    check(f"dashboard exposes {title}", title in renderer)

failed = [name for name, ok in checks if not ok]
for name, ok in checks: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"\nStage 24.3 settings navigation contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)
```

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
```

Expected: FAIL because the renderer still owns its legacy `Category` enum and theme-dependent subsets.

- [ ] **Step 5: Replace legacy dashboard categories with the canonical eight**

Modify `SettingsThemeDashboardRenderer.kt`:

- Delete the local `enum class Category`.
- Change `Item.category` to `SettingsCategoryId`.
- Change every callback signature to `(SettingsCategoryId) -> Unit`.
- Define one `canonicalItems()` list in the required order:

```kotlin
private fun canonicalItems(): List<Item> = listOf(
    Item(SettingsCategoryId.KEYBOARD_SETUP, "⌨", "Keyboard Setup", "Enable and choose the keyboard"),
    Item(SettingsCategoryId.LANGUAGE_INPUT, "◎", "Language & Input", "English, বাংলা, Phonetic, Bijoy"),
    Item(SettingsCategoryId.THEME_APPEARANCE, "✦", "Theme & Appearance", "Keyboard and settings visuals"),
    Item(SettingsCategoryId.TYPING_SUGGESTIONS, "⌁", "Typing & Suggestions", "Learning, cursor, glide, feedback"),
    Item(SettingsCategoryId.AI_PRIVACY, "⌘", "AI & Privacy", "Consent, context, prompts, training"),
    Item(SettingsCategoryId.CLIPBOARD, "▣", "Clipboard", "Recent history controls"),
    Item(SettingsCategoryId.ACCOUNT_SUBSCRIPTION, "●", "Account & Subscription", "Products, usage, gateway"),
    Item(SettingsCategoryId.HELP_ABOUT, "?", "Help & About", "Support and app information")
)
```

- All four theme renderers must iterate this same list. Clean Modern/Premium/Pro may render rows; Card Style may render a 2-column × 4-row grid. Do not filter or reorder the list.
- Keep Pro Style's personalization quote after the eight categories; it is presentation, not a ninth destination.

- [ ] **Step 6: Run Task 1 tests**

Run:

```bash
bash scripts/run_settings_navigation_stage24_3_selftest.sh
python3 scripts/verify_settings_navigation_stage24_3.py
```

Expected: both PASS; static contract must show all eight titles and canonical callback usage.

- [ ] **Step 7: Commit Task 1**

```bash
git add \
  app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt \
  app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeDashboardRenderer.kt \
  scripts/settings_navigation_stage24_3_selftest.kt \
  scripts/run_settings_navigation_stage24_3_selftest.sh \
  scripts/verify_settings_navigation_stage24_3.py
git commit -m "feat: define canonical settings categories"
```

---

### Task 2: Reusable Category Activity Shell, Safe Routing, Back, and Theme Styling

**Files:**
- Create: `app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt`
- Create: `app/src/main/res/layout/activity_settings_category.xml`
- Create: eight `settings_category_*.xml` files listed in File Map (initially each with its required category root/container and key title copy; controls are filled in Tasks 4–6)
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Extend: `scripts/verify_settings_navigation_stage24_3.py`

**Interfaces:**
- Consumes: `SettingsCategoryId.fromWireValue`, `SettingsThemeRepository.currentPack()`, `SettingsThemeCatalog.spec()`, `SettingsThemeStyler`.
- Produces: `SettingsCategoryActivity.createIntent(context: Context, category: SettingsCategoryId): Intent`; `EXTRA_CATEGORY = "settings_category"`; category → layout mapping; common themed header/back behavior; `refreshCategoryState()` dispatch point.

- [ ] **Step 1: Extend static verifier for the category shell and watch it fail**

Append checks to `verify_settings_navigation_stage24_3.py` for:

```python
activity = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
manifest = read("app/src/main/AndroidManifest.xml")
styler = read("app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt")

check("category activity registered", '.SettingsCategoryActivity' in manifest and 'android:exported="false"' in manifest)
check("category intent factory exists", "fun createIntent(context: Context, category: SettingsCategoryId): Intent" in activity)
check("category parses stable wire value", "SettingsCategoryId.fromWireValue" in activity)
check("invalid category safely routes away", "openHubForInvalidCategory" in activity and "finish()" in activity)
check("back arrow uses normal back stack", "onBackPressedDispatcher.onBackPressed()" in activity)
check("category theme reapplies on resume", "override fun onResume()" in activity and "applySettingsTheme()" in activity)
check("styler has inner category entry point", "fun applyCategory(" in styler)
```

Also check that all eight layout files exist.

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
```

Expected: FAIL because the activity/layouts/styler entry point do not exist.

- [ ] **Step 2: Create the common category activity wrapper layout**

Create `activity_settings_category.xml` as a vertical root with these stable IDs:

```xml
<LinearLayout android:id="@+id/settings_category_root" ... android:orientation="vertical">
    <LinearLayout android:id="@+id/settings_category_header" ...>
        <ImageButton
            android:id="@+id/settings_category_back"
            android:contentDescription="@string/settings_back" ... />
        <LinearLayout ...>
            <TextView android:id="@+id/settings_category_title" ... />
            <TextView android:id="@+id/settings_category_subtitle" ... />
        </LinearLayout>
    </LinearLayout>
    <ScrollView ...>
        <FrameLayout
            android:id="@+id/settings_category_content"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </ScrollView>
</LinearLayout>
```

Add `settings_back` and eight category title/subtitle string pairs to `strings.xml`.

- [ ] **Step 3: Create the eight dedicated content layout files**

Each file must have a single vertical `LinearLayout` root with ID `settings_category_section` and no hub/dashboard content. At this task, include a category heading/comment plus the final control IDs that Tasks 4–6 will bind. Do not create duplicate IDs within the same file.

Required file names are exactly the eight names in the File Map.

- [ ] **Step 4: Add category layout metadata and shell behavior**

Implement `SettingsCategoryActivity` with:

```kotlin
class SettingsCategoryActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_CATEGORY = "settings_category"

        fun createIntent(context: Context, category: SettingsCategoryId): Intent =
            Intent(context, SettingsCategoryActivity::class.java)
                .putExtra(EXTRA_CATEGORY, category.wireValue)
    }

    private var category: SettingsCategoryId? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resolved = SettingsCategoryId.fromWireValue(intent.getStringExtra(EXTRA_CATEGORY))
        if (resolved == null) {
            openHubForInvalidCategory()
            return
        }
        category = resolved
        setContentView(R.layout.activity_settings_category)
        findViewById<View>(R.id.settings_category_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        inflateCategoryContent(resolved)
        bindCategory(resolved)
    }

    override fun onResume() {
        super.onResume()
        if (category != null) {
            lifecycleScope.launch {
                applySettingsTheme()
                refreshCategoryState()
            }
        }
    }
}
```

Use a private metadata mapping with exact `layoutRes`, `titleRes`, and `subtitleRes` for all eight IDs. `inflateCategoryContent()` inflates the matching `settings_category_*` layout into `settings_category_content`.

Implement `openHubForInvalidCategory()` so it does not loop:

```kotlin
private fun openHubForInvalidCategory() {
    if (!isTaskRoot) {
        finish()
        return
    }
    startActivity(Intent(this, MainActivity::class.java))
    finish()
}
```

`bindCategory()` and `refreshCategoryState()` may initially dispatch to no-op private functions that Tasks 4–6 will fill, but every enum branch must be explicit so a new category cannot silently fall through.

- [ ] **Step 5: Extend `SettingsThemeStyler` for inner pages**

Add:

```kotlin
fun applyCategory(
    root: View,
    header: ViewGroup,
    title: TextView,
    subtitle: TextView,
    content: ViewGroup,
    pack: SettingsThemePack
)
```

It must:
- use `SettingsThemeCatalog.spec(pack)`;
- apply root gradient/background;
- color title/subtitle;
- style header/content panel surfaces from the same theme spec;
- call existing `styleTree(content, spec)` so checkboxes, buttons, text and edits retain theme treatment.

Do not hardcode per-theme colors in XML.

- [ ] **Step 6: Register the Activity**

Add before `MainActivity` in `AndroidManifest.xml`:

```xml
<activity
    android:name=".SettingsCategoryActivity"
    android:exported="false" />
```

- [ ] **Step 7: Run Task 2 tests**

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
bash scripts/run_settings_navigation_stage24_3_selftest.sh
```

Expected: PASS for shell/routing/layout existence; no Android build claim.

- [ ] **Step 8: Commit Task 2**

```bash
git add \
  app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt \
  app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt \
  app/src/main/res/layout/activity_settings_category.xml \
  app/src/main/res/layout/settings_category_*.xml \
  app/src/main/res/values/strings.xml \
  app/src/main/AndroidManifest.xml \
  scripts/verify_settings_navigation_stage24_3.py
git commit -m "feat: add dedicated settings category shell"
```

---

### Task 3: Make MainActivity a Hub-Only Navigator and Preserve Legacy AI Deep Link

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Extend: `scripts/verify_settings_navigation_stage24_3.py`

**Interfaces:**
- Consumes: `SettingsThemeDashboardRenderer.render(..., onCategory: (SettingsCategoryId) -> Unit)`, `SettingsCategoryActivity.createIntent`, `SettingsCategoryId.fromLegacySection`.
- Produces: hub-only `MainActivity.openSettingsCategory(SettingsCategoryId)` and direct legacy AI route.

- [ ] **Step 1: Add failing Hub-only and deep-link checks**

Append checks:

```python
main = read("app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt")
hub = read("app/src/main/res/layout/activity_main.xml")

check("main launches category activity", "SettingsCategoryActivity.createIntent(this, category)" in main)
check("main has no settings smooth scroll", "smoothScrollTo" not in main)
check("main has no old section routing", "section_ai_privacy" not in main and "section_keyboard_preferences" not in main)
check("legacy ai deep link maps to canonical category", "SettingsCategoryId.fromLegacySection" in main and "SECTION_AI_PRIVACY" in main)
check("hub xml has dashboard container", "settings_theme_dashboard_container" in hub)
for old_id in (
    "section_ai_privacy", "section_theme_appearance", "section_keyboard_preferences",
    "section_language_input", "section_account_subscription", "section_help_support", "section_about"
):
    check(f"hub removed {old_id}", old_id not in hub)
```

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
```

Expected: FAIL on old scrolling/sections.

- [ ] **Step 2: Reduce `activity_main.xml` to Hub-only content**

Retain:
- `settings_scroll` root if useful for small screens/font scaling;
- `settings_header_eyebrow`, `settings_header_title`, `settings_header_subtitle`;
- `status_settings_theme` as read-only current theme presentation;
- `settings_theme_dashboard_container`.

Remove:
- `button_settings_theme` from Hub; Settings Theme changing belongs in Theme & Appearance.
- every category control and all seven old `section_*` containers.

The Hub contains no checkboxes, edit fields, backend buttons, keyboard setup buttons, or direct theme-change action.

- [ ] **Step 3: Reduce `MainActivity` to navigation/theming/setup only**

Keep fields:

```kotlin
private lateinit var settingsThemeRepository: SettingsThemeRepository
private lateinit var settingsThemeStyler: SettingsThemeStyler
private lateinit var settingsThemeDashboardRenderer: SettingsThemeDashboardRenderer
```

Remove category repositories/actions from MainActivity (`SettingsRepository`, `SecretStore`, account refresh, keyboard status, AI/clipboard/typing/theme option listeners and helper methods).

Use:

```kotlin
private fun openSettingsCategory(category: SettingsCategoryId) {
    startActivity(SettingsCategoryActivity.createIntent(this, category))
}
```

In `onCreate`, route legacy deep links before normal Hub interaction:

```kotlin
val requested = SettingsCategoryId.fromLegacySection(intent.getStringExtra(EXTRA_OPEN_SECTION))
if (requested != null) {
    startActivity(SettingsCategoryActivity.createIntent(this, requested))
    finish()
    return
}
```

Preserve constants exactly:

```kotlin
const val EXTRA_OPEN_SECTION = "social_ai_open_section"
const val SECTION_AI_PRIVACY = "ai_privacy"
```

- [ ] **Step 4: Adjust Hub theming call for the simplified layout**

Either overload `SettingsThemeStyler.apply` or simplify its existing Hub call so it styles the root/header/status/dashboard without expecting old section views. Do not pass removed IDs.

The dashboard still renders on `onResume()` so a changed Settings Theme Pack is reflected when returning to Hub.

- [ ] **Step 5: Run Task 3 tests**

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
bash scripts/run_settings_navigation_stage24_3_selftest.sh
```

Expected: PASS for Hub-only contract and legacy AI routing.

- [ ] **Step 6: Commit Task 3**

```bash
git add \
  app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt \
  app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt \
  app/src/main/res/layout/activity_main.xml \
  scripts/verify_settings_navigation_stage24_3.py
git commit -m "refactor: make settings main screen a hub"
```

---

### Task 4: Migrate Keyboard Setup, Language & Input, and Clipboard Controls

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt`
- Modify: `app/src/main/res/layout/settings_category_keyboard_setup.xml`
- Modify: `app/src/main/res/layout/settings_category_language_input.xml`
- Modify: `app/src/main/res/layout/settings_category_clipboard.xml`
- Extend: `scripts/verify_settings_navigation_stage24_3.py`

**Interfaces:**
- Consumes: existing `SettingsRepository`, `RecentClipboardStore`, `SocialAiInputMethodService`, Android `Settings.ACTION_INPUT_METHOD_SETTINGS`, `InputMethodManager`.
- Produces: category-local listeners/status refresh for keyboard activation, English language preferences, and clipboard history.

- [ ] **Step 1: Add failing control ownership checks**

Extend verifier so the dedicated layouts must own these IDs:

Keyboard Setup:
- `status_keyboard_enabled`
- `status_current_keyboard`
- `button_keyboard_settings`
- `button_choose_keyboard`

Language & Input:
- `english_suggestions_checkbox`
- `english_autocorrect_checkbox`
- `smart_language_hints_checkbox`
- visible copy mentioning English, বাংলা, Phonetic, Bijoy

Clipboard:
- `clipboard_history_checkbox`
- `button_clear_clipboard_history`

Also assert none of those IDs remain in `activity_main.xml`.

Run verifier and expect FAIL until controls move.

- [ ] **Step 2: Fill the three layouts with their existing IDs**

Move the existing XML controls into the three dedicated layouts. Preserve current style references (`SettingsCheckBox`, `SetupStatusRow`, `SettingsActionButton`, `SettingsSecondaryButton`) and labels where they still fit.

Language layout must include non-interactive explanatory text confirming all four supported layouts; no new layout-selection preference is invented in this stage.

- [ ] **Step 3: Bind Keyboard Setup**

In `SettingsCategoryActivity.bindKeyboardSetup()`:

```kotlin
findViewById<Button>(R.id.button_keyboard_settings).setOnClickListener {
    runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        .onFailure { Toast.makeText(this, "Keyboard settings are unavailable on this device.", Toast.LENGTH_LONG).show() }
}
findViewById<Button>(R.id.button_choose_keyboard).setOnClickListener {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
}
```

In `refreshKeyboardSetup()`, move the exact enabled/default IME status calculation from old `MainActivity.refreshStatuses()` and update only the two keyboard status views.

- [ ] **Step 4: Bind Language & Input through existing SettingsRepository APIs**

Use the existing setters unchanged:

```kotlin
settingsRepository.setEnglishSuggestions(...)
settingsRepository.setEnglishAutocorrect(...)
settingsRepository.setSmartLanguageHints(...)
```

`refreshLanguageInput()` reads `settingsRepository.current()` and updates the three checkboxes.

- [ ] **Step 5: Bind Clipboard through existing state**

Use:

```kotlin
settingsRepository.setClipboardHistory(...)
RecentClipboardStore(this).clear()
```

Keep the existing clear-history Toast. `refreshClipboard()` reflects `settings.clipboardHistory`.

- [ ] **Step 6: Run Task 4 tests**

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
python3 scripts/verify_typing_stage23_8.py
```

Expected: Stage 24.3 checks PASS for these controls; Number Row regression remains unaffected because Number Row has not been moved into these categories.

- [ ] **Step 7: Commit Task 4**

```bash
git add \
  app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt \
  app/src/main/res/layout/settings_category_keyboard_setup.xml \
  app/src/main/res/layout/settings_category_language_input.xml \
  app/src/main/res/layout/settings_category_clipboard.xml \
  scripts/verify_settings_navigation_stage24_3.py
git commit -m "feat: migrate basic settings categories"
```

---

### Task 5: Migrate Theme & Appearance and Typing & Suggestions with Immediate Theme Refresh

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt`
- Modify: `app/src/main/res/layout/settings_category_theme_appearance.xml`
- Modify: `app/src/main/res/layout/settings_category_typing_suggestions.xml`
- Extend: `scripts/verify_settings_navigation_stage24_3.py`
- Update if necessary: `scripts/verify_theme_stage24_0.py`, `scripts/verify_theme_stage24_2_optional_bubble.py`, `scripts/verify_typing_stage23_8.py` only to follow controls to their new Activity/layout, never to weaken behavior checks.

**Interfaces:**
- Consumes: `SettingsRepository`, `ThemeRepository`, `SettingsThemeRepository`, `ThemeSettingsActivity`, `SettingsThemeOnboardingActivity`, `BubbleKeyIntensity`, `OneHandedMode`, `ToolbarProfile`, persistent typing learning models.
- Produces: global Number Row; selected-pack Key Boundary/Bubble controls; keyboard/settings theme navigation; keyboard height/one-handed controls; typing mechanics/feedback/profile controls.

- [ ] **Step 1: Add failing ownership and refresh checks**

Theme & Appearance layout must contain:
- `status_theme_name`
- `button_theme_settings`
- `status_settings_theme_category`
- `button_settings_theme`
- `number_row_checkbox`
- `key_boundary_checkbox`
- `bubble_key_checkbox`
- `status_bubble_key_intensity`
- `button_bubble_soft`, `button_bubble_normal`, `button_bubble_playful`
- `status_one_handed_mode`, `button_one_hand_left`, `button_one_hand_off`, `button_one_hand_right`
- `status_keyboard_height`, `button_height_compact`, `button_height_normal`, `button_height_tall`

Typing & Suggestions layout must contain:
- `status_typing_learning`
- `personal_typing_learning_checkbox`
- `button_clear_typing_learning`
- `spacebar_cursor_checkbox`
- `glide_typing_checkbox`
- `status_toolbar_profile`
- four toolbar profile buttons
- `haptic_feedback_checkbox`
- `key_sound_checkbox`

Static checks must also assert:
- `SettingsCategoryActivity.onResume()` calls `applySettingsTheme()` and `refreshCategoryState()`;
- Theme & Appearance launches `SettingsThemeOnboardingActivity.EXTRA_MANUAL_CHANGE = true`;
- Key Boundary uses `currentKeyBoundaryEnabled(pack)` / `setKeyBoundaryEnabled(pack, enabled)`;
- Bubble uses `currentBubbleAppearance(pack)` / `setBubbleEnabled(pack, enabled)` / `setBubbleIntensity(pack, intensity)`;
- Number Row still uses `SettingsRepository.setShowNumberRow`.

Run Stage 24.3 verifier; expect FAIL.

- [ ] **Step 2: Move Theme & Appearance controls into its dedicated layout**

Use existing IDs where available; add only `status_settings_theme_category` and `key_boundary_checkbox` for the selected-theme summary/control. Keep `button_theme_settings` as the full four-pack/customization entry point.

- [ ] **Step 3: Bind global and selected-theme appearance state**

In `bindThemeAppearance()`:

- `button_theme_settings` → `ThemeSettingsActivity`.
- `button_settings_theme` → `SettingsThemeOnboardingActivity` with `EXTRA_MANUAL_CHANGE=true`.
- `number_row_checkbox` → `settingsRepository.setShowNumberRow`.
- `key_boundary_checkbox`: resolve `val pack = app.themeRepository.currentSelection().globalPack`; if non-null write `setKeyBoundaryEnabled(pack, enabled)`; if null disable the checkbox and direct user to select a premium package via `button_theme_settings`.
- `bubble_key_checkbox`: keep Stage 24.2 dual-write semantics for backward compatibility: `settingsRepository.setBubbleKeyEnabled(enabled)` plus `themeRepository.setBubbleEnabled(pack, enabled)` when a pack exists.
- Bubble style buttons: `settingsRepository.setBubbleKeyIntensity(intensity)` plus `themeRepository.setBubbleIntensity(pack, intensity)` when a pack exists.
- One-handed and height buttons continue using the existing `SettingsRepository` APIs and exact values `44`, `50`, `56`.

`refreshThemeAppearance()` must read:

```kotlin
val settings = settingsRepository.current()
val theme = app.themeRepository.current()
val pack = app.themeRepository.currentSelection().globalPack
val settingsPack = settingsThemeRepository.currentPack()
```

Then update global Number Row/height/one-handed state and selected-pack Key Boundary/Bubble state. Changing keyboard Theme Pack in `ThemeSettingsActivity` and returning must show the new pack's saved Key Boundary/Bubble values.

- [ ] **Step 4: Move and bind Typing & Suggestions**

Port existing listener logic without changing APIs:

```kotlin
setPersonalTypingLearning
setSpacebarCursorControl
setGlideTyping
setToolbarProfile
setHapticFeedback
setKeySound
```

`button_clear_typing_learning` continues clearing both `PersistentTypingLearningModel` and `PersistentEnglishTypingLearningModel`.

`refreshTypingSuggestions()` must rebuild the existing learning-stat status string and reflect toolbar profile/check states.

- [ ] **Step 5: Verify immediate Settings Theme restyle after returning**

Because manual Settings Theme selection occurs in a separate Activity, the contract is that `SettingsCategoryActivity.onResume()` re-runs `applySettingsTheme()` before/with `refreshCategoryState()`. Add a static check for this ordering in the source text and ensure `status_settings_theme_category` refreshes to the newly selected pack.

- [ ] **Step 6: Run Task 5 tests/regressions**

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
python3 scripts/verify_theme_stage24_0.py
python3 scripts/verify_theme_stage24_2_optional_bubble.py
bash scripts/run_typing_stage24_2_theme_bubble_selftest.sh
python3 scripts/verify_typing_stage23_8.py
```

Expected: all PASS. If an old verifier fails solely because it hardcodes `MainActivity` as the control owner, update that verifier to inspect `SettingsCategoryActivity`/the dedicated layout while keeping the same persistence/behavior assertions.

- [ ] **Step 7: Commit Task 5**

```bash
git add \
  app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt \
  app/src/main/res/layout/settings_category_theme_appearance.xml \
  app/src/main/res/layout/settings_category_typing_suggestions.xml \
  scripts/verify_settings_navigation_stage24_3.py \
  scripts/verify_theme_stage24_0.py \
  scripts/verify_theme_stage24_2_optional_bubble.py \
  scripts/verify_typing_stage23_8.py
git commit -m "feat: migrate appearance and typing settings"
```

Only add modified verifier files to the commit; do not touch them pre-emptively if they already pass.

---

### Task 6: Migrate AI & Privacy, Account & Subscription, and Help & About

**Files:**
- Modify: `app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt`
- Modify: `app/src/main/res/layout/settings_category_ai_privacy.xml`
- Modify: `app/src/main/res/layout/settings_category_account_subscription.xml`
- Modify: `app/src/main/res/layout/settings_category_help_about.xml`
- Extend: `scripts/verify_settings_navigation_stage24_3.py`
- Update existing release/backend/settings-theme verifiers only if they are location-coupled to `MainActivity`.

**Interfaces:**
- Consumes: existing `SettingsRepository`, `SecretStore`, `ContextAccessGate`, `SocialAiAccessibilityService`, `SocialAiApplication` managed session/backend/gateway, `AuthActivity`, `PortalActivity`, `CaptionActivity`, `PrivacyActivity`, existing `AiGatewayException` error mapping.
- Produces: dedicated AI/privacy/account/help behavior identical to Stage 24.2, with category-local refresh.

- [ ] **Step 1: Add failing control ownership checks**

AI & Privacy layout owns:
- `ai_privacy_consent_checkbox`
- `preserve_draft_checkbox`
- `button_save_ai_privacy_consent`
- `status_ai_tone`
- `context_consent_checkbox`
- `button_context_access`
- `button_disable_context_access`
- `input_custom_instruction`
- save/clear custom prompt buttons
- `input_personal_training`
- save/clear personal training buttons
- `button_caption`
- `button_privacy_data`

Account & Subscription owns:
- five status rows (`status_managed_account`, `status_keyboard_product`, `status_assistant_product`, `status_usage`, `status_gateway`)
- managed login/refresh/portal/use/logout buttons
- `button_toggle_advanced_ai`, `advanced_ai_container`
- `status_personal_api`, `input_api_key`, save/test/clear/use-personal controls

Help & About owns:
- `button_whatsapp_support`
- visible app version/developer/email/WhatsApp/phone copy already present in old About section.

Assert these IDs do not appear in `activity_main.xml`.

- [ ] **Step 2: Fill AI & Privacy layout and port existing logic exactly**

Move the current controls and port these helper behaviors into `SettingsCategoryActivity`:
- `saveAiPrivacyAndDraftSettings()`
- `acceptDisclosureAndOpenAccessibility()`
- `disableContextAccess()`
- `saveCustomInstruction()` / `clearCustomInstruction()`
- `savePersonalTraining()` / `clearPersonalTraining()`
- Caption and Privacy activity launch buttons.

`refreshAiPrivacy()` reads only the current `SettingsRepository`/managed training sources and updates AI consent, preserve draft, context consent, tone, custom instruction, and personal training text without overwriting a focused `EditText`.

Keep `ContextAccessGate.update(...)` behavior unchanged.

- [ ] **Step 3: Fill Account & Subscription layout and port backend/gateway logic exactly**

Port:
- `toggleAdvancedAi()`
- `useManagedAi()`
- `logoutManaged()`
- `refreshManagedAccount(showErrors)`
- `saveApiKey()`
- `testApiKey()`
- `apiTestErrorMessage()`
- Personal/managed gateway selection actions.

Use existing `SecretStore`, `app.backendClient`, `app.managedSessionStore`, and `SettingsRepository.setGatewayMode` unchanged. Do not add new preference keys or cache account state in the Activity.

`refreshAccountSubscription()` updates API/gateway/account/product/usage status and preserves the current backend error Toast policy.

- [ ] **Step 4: Fill Help & About**

Move the existing WhatsApp support action and About copy to `settings_category_help_about.xml`. Keep the existing support URI unchanged unless an existing string resource already centralizes it. No new network/backend behavior is added.

- [ ] **Step 5: Run Task 6 tests/regressions**

Run:

```bash
python3 scripts/verify_settings_navigation_stage24_3.py
python3 scripts/verify_release_ready.py
python3 scripts/verify_shared_backend_config.py
python3 scripts/verify_settings_theme_stage23_9.py
```

Expected: all PASS. If old verifiers are location-coupled, update only their source-location search while preserving the original behavioral assertions.

- [ ] **Step 6: Commit Task 6**

```bash
git add \
  app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt \
  app/src/main/res/layout/settings_category_ai_privacy.xml \
  app/src/main/res/layout/settings_category_account_subscription.xml \
  app/src/main/res/layout/settings_category_help_about.xml \
  scripts/verify_settings_navigation_stage24_3.py \
  scripts/verify_release_ready.py \
  scripts/verify_shared_backend_config.py \
  scripts/verify_settings_theme_stage23_9.py
git commit -m "feat: migrate ai account and support settings"
```

Only stage verifier files that actually changed.

---

### Task 7: Full Contract, XML, Regression, Documentation, and Build Availability

**Files:**
- Finalize: `scripts/verify_settings_navigation_stage24_3.py`
- Create: `docs/STAGE24_3_DEDICATED_SETTINGS_NAVIGATION.md`
- Create: `docs/STAGE24_3_VERIFICATION_REPORT.md`
- Modify: no production code unless a failing regression identifies a real defect and is fixed via a new RED→GREEN test.

**Interfaces:**
- Consumes: all Task 1–6 outputs and existing Stage 23/24 verification scripts.
- Produces: final Stage 24.3 evidence and documented Android Studio/manual acceptance checklist.

- [ ] **Step 1: Finalize Stage 24.3 verifier to cover all acceptance criteria**

The final verifier must check at minimum:
- exactly eight `SettingsCategoryId` values and unique wire values;
- every Settings Theme renderer path uses the canonical list;
- `MainActivity` has no `smoothScrollTo()` and no old `section_*` category routing;
- Hub XML contains no migrated category controls;
- `SettingsCategoryActivity` is registered and has an intent factory;
- invalid category fallback exists;
- back arrow uses normal back stack;
- all eight category layouts exist and each owns its expected controls;
- legacy AI & Privacy constants remain and route to `AI_PRIVACY`;
- Theme & Appearance preserves global Number Row and per-pack Key Boundary/Bubble APIs;
- settings-theme restyling runs in category `onResume()`;
- no new raw DataStore/preference keys were introduced in `SettingsCategoryActivity`.

Run and require full PASS.

- [ ] **Step 2: Parse all Android XML files**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path('app/src/main')
files = sorted(list((root/'res').rglob('*.xml')) + [root/'AndroidManifest.xml'])
for path in files:
    ET.parse(path)
print(f'XML parse: {len(files)}/{len(files)} PASS')
PY
```

Expected: every XML file parses successfully.

- [ ] **Step 3: Run the complete relevant regression suite**

Run:

```bash
set -e
bash scripts/run_settings_navigation_stage24_3_selftest.sh
python3 scripts/verify_settings_navigation_stage24_3.py
python3 scripts/verify_settings_theme_stage23_9.py
python3 scripts/verify_theme_stage24_0.py
python3 scripts/verify_typing_stage24_1.py
python3 scripts/verify_theme_stage24_2_optional_bubble.py
bash scripts/run_typing_stage24_2_theme_bubble_selftest.sh
python3 scripts/verify_typing_stage23_8.py
python3 scripts/verify_numberpad_stage23_7.py
python3 scripts/verify_release_ready.py
python3 scripts/verify_shared_backend_config.py
```

Expected: every command exits `0`. Record exact PASS counts in the verification report.

- [ ] **Step 4: Check Gradle wrapper availability; build only if available**

Run the existing wrapper completeness checker (use the repository's actual script name; current baseline has a wrapper checker used in Stage 24.1/24.2 verification). If the checker reports bootstrap required, record that exact blocker and do **not** claim compilation/APK success.

If the verified wrapper is available, run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Expected when available: exit `0`, with APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 5: Write Stage 24.3 documentation**

`docs/STAGE24_3_DEDICATED_SETTINGS_NAVIGATION.md` must document:
- Hub-only information architecture;
- eight categories and ownership map;
- four Settings Theme Packs sharing identical navigation;
- legacy AI deep-link compatibility;
- global Number Row vs per-theme Key Boundary/Bubble semantics;
- back/fallback behavior.

`docs/STAGE24_3_VERIFICATION_REPORT.md` must include actual command outputs/counts and explicitly separate:
- static/pure-Kotlin verification;
- Android Gradle build result or blocker;
- real-device checks still required.

Real-device checklist:
1. Open all eight categories from each of the four Settings Theme Packs.
2. Back arrow and Android back return to Hub.
3. Trigger IME AI & Privacy deep link and confirm direct AI & Privacy page.
4. Toggle representative settings in every category; relaunch and confirm persistence.
5. Change Settings Theme while inside Theme & Appearance; return and confirm current page restyles without value loss.
6. Switch keyboard Theme Pack; confirm Key Boundary/Bubble values update to that pack's stored values.
7. Test invalid category intent from `adb`/debug helper only if practical; confirm no blank page.

- [ ] **Step 6: Fresh final verification after docs and any verifier updates**

Re-run the exact suite from Step 3 plus XML parse. Do not use earlier runs as completion evidence.

Expected: all available checks green; build status reported truthfully.

- [ ] **Step 7: Commit Task 7**

```bash
git add \
  scripts/verify_settings_navigation_stage24_3.py \
  docs/STAGE24_3_DEDICATED_SETTINGS_NAVIGATION.md \
  docs/STAGE24_3_VERIFICATION_REPORT.md
git commit -m "docs: verify dedicated settings navigation"
```

Include any regression-verifier compatibility edits in this commit only if they were not committed in the task that required them.

## Plan Self-Review Results

- **Spec coverage:** All twelve acceptance criteria are mapped to Tasks 1–7. The Hub-only rule is Task 3; eight dedicated screens are Tasks 2/4/5/6; theme parity is Task 1/2; state preservation is Tasks 4–6; deep links are Task 3; back/fallback is Task 2; regressions are Task 7.
- **Placeholder scan:** No `TBD`, `TODO`, “implement later”, unspecified error-handling step, or undefined production interface remains. Task 7's Gradle checker wording intentionally references the existing checker because the current baseline's exact filename must be discovered at execution, but the decision rule and commands are explicit.
- **Type consistency:** `SettingsCategoryId` wire values, canonical callback, `SettingsCategoryActivity.createIntent`, and `EXTRA_CATEGORY` are consistent across producer/consumer tasks.
- **Review Focus:** All five high-risk conditions have owning test/static checks embedded in Tasks 1, 2, 3, 5, and 4–6 respectively.
- **Scope check:** This is one cohesive settings-navigation migration; no independent subsystem needs a separate plan.
