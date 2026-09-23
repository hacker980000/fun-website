# Stage 24.3 Dedicated Settings Category Navigation — Design Specification

**Date:** 2026-09-22  
**Project:** Social AI Keyboard v35.3.1  
**Baseline:** Stage 24.2 Optional Theme Bubble  
**Status:** Approved conversational design captured for written review

## 1. Goal

Replace the current single long settings page, where category cards scroll to option sections on the same page, with an Android-style two-level settings experience:

1. `MainActivity` is the Settings Hub and shows only top-level categories.
2. Tapping a category opens a dedicated inner settings screen containing only that category's controls.
3. The navigation model is identical across all four Settings Theme Packs; only visual styling changes.

The change must preserve existing preference keys, backend/account behavior, keyboard behavior, theme-package behavior, and existing external/deep-link entry points.

## 2. Current Behavior and Problem

The current `MainActivity.openSettingsCategory()` maps dashboard categories to `section_*` views in `activity_main.xml` and calls `ScrollView.smoothScrollTo()`. This means category navigation and category controls occupy the same page.

Current settings-theme dashboards also expose different subsets of category cards depending on the selected Settings Theme Pack. This makes the information architecture inconsistent even though the underlying settings are the same.

The requested behavior is that every top-level setting owns a dedicated screen and all optional controls live inside the relevant top-level setting rather than beside it on the Settings Hub.

## 3. Product Principles

- **Settings Hub is navigation-only.** No actual preference checkbox, toggle, editor, account action, or theme option is displayed below the category list on the hub.
- **One information architecture, four visual themes.** Clean Modern, Card Style, Premium, and Pro Style use the same destinations and ordering.
- **No preference duplication.** Inner screens use the existing repositories, keys, and backend services; moving a control must not create a second source of truth.
- **No behavior regression.** Keyboard typing, AI, account, clipboard, theme selection, Number Row, Key Boundary, Bubble Effect, and Bubble Style keep their current persisted semantics.
- **Android-style back behavior.** The inner-screen toolbar back arrow and the system back gesture/button return to the Settings Hub.
- **Backward-compatible deep links.** Existing callers using `EXTRA_OPEN_SECTION` for AI & Privacy must land directly on the AI & Privacy inner screen.

## 4. Navigation Architecture

### 4.1 Settings Hub

`MainActivity` becomes the top-level Settings Hub. It owns:

- App/settings header.
- Current Settings Theme presentation.
- Exactly eight top-level destinations.
- Settings Theme styling of the hub cards/list/grid.
- First-run / legacy routing that must redirect to a category destination.

It does not own category option controls after this stage.

### 4.2 Reusable Category Activity

Add one reusable `SettingsCategoryActivity` that receives a stable category identifier in its intent and renders the corresponding inner page.

Recommended contract:

```kotlin
enum class SettingsCategoryId(val wireValue: String) {
    KEYBOARD_SETUP("keyboard_setup"),
    LANGUAGE_INPUT("language_input"),
    THEME_APPEARANCE("theme_appearance"),
    TYPING_SUGGESTIONS("typing_suggestions"),
    AI_PRIVACY("ai_privacy"),
    CLIPBOARD("clipboard"),
    ACCOUNT_SUBSCRIPTION("account_subscription"),
    HELP_ABOUT("help_about")
}
```

`SettingsCategoryActivity` exposes a single intent factory so callers do not construct extras manually:

```kotlin
fun createIntent(context: Context, category: SettingsCategoryId): Intent
```

### 4.3 Dedicated Layouts

The reusable activity loads a focused layout for each category. Layouts remain separate because the control sets and accessibility semantics are different, while the activity shares navigation, theming, lifecycle, and common state-refresh behavior.

Expected layouts:

- `settings_category_keyboard_setup.xml`
- `settings_category_language_input.xml`
- `settings_category_theme_appearance.xml`
- `settings_category_typing_suggestions.xml`
- `settings_category_ai_privacy.xml`
- `settings_category_clipboard.xml`
- `settings_category_account_subscription.xml`
- `settings_category_help_about.xml`

Existing view IDs should be preserved wherever practical so business logic can be moved without renaming preference bindings unnecessarily.

## 5. Top-Level Category Model

Every Settings Theme Pack exposes these destinations in the same logical order:

1. Keyboard Setup
2. Language & Input
3. Theme & Appearance
4. Typing & Suggestions
5. AI & Privacy
6. Clipboard
7. Account & Subscription
8. Help & About

The dashboard renderer may display the categories as rows, cards, or a grid according to the selected Settings Theme Pack, but it must not omit or merge destinations.

## 6. Category Contents

### 6.1 Keyboard Setup

Purpose: Android keyboard activation and selection only.

Contains:

- Keyboard enabled/disabled status.
- Active keyboard status.
- Open Android Keyboard Settings.
- Choose Active Keyboard.

No appearance, typing, AI, or account controls belong here.

### 6.2 Language & Input

Purpose: language/layout choices and language-specific typing behavior.

Contains:

- English layout support.
- বাংলা layout support.
- Bangla Phonetic support.
- Bijoy support.
- English Suggestions ON/OFF.
- English Autocorrect ON/OFF.
- Smart Bangla ↔ English hints ON/OFF.
- Language/layout explanatory copy already present in the application.

The implementation must preserve existing layout-selection and language preference keys.

### 6.3 Theme & Appearance

Purpose: all keyboard and settings visual configuration.

Contains:

- Keyboard Theme Package selection.
- Change/Customize Keyboard Theme action.
- Settings Theme Package selection.
- Change Settings Theme action.
- Number Row ON/OFF. This remains global across alphabetic layouts.
- Keyboard height controls.
- One-handed mode controls.
- Selected Keyboard Theme Appearance controls:
  - Key Boundary ON/OFF.
  - Bubble Effect ON/OFF.
  - Bubble Style: Soft / Normal / Playful.
- Entry point for configuring all four keyboard theme packages when the existing UI supports it.

Key Boundary and Bubble preferences remain theme-specific, as established in Stages 24.0 and 24.2. Switching the selected keyboard theme updates the visible values to that theme's saved settings without changing the other themes' saved values.

### 6.4 Typing & Suggestions

Purpose: typing mechanics, prediction behavior, input feedback, and toolbar configuration.

Contains:

- Personal Typing Learning ON/OFF.
- Clear Learned Typing Data.
- Spacebar Cursor Control ON/OFF.
- Glide Typing ON/OFF.
- Toolbar Profile selection:
  - Balanced.
  - AI First.
  - Typing.
  - Minimal.
- Haptic feedback ON/OFF when present in the current project.
- Keypress sound ON/OFF when present in the current project.

Language-specific suggestion toggles stay in Language & Input rather than being duplicated here.

### 6.5 AI & Privacy

Purpose: AI data controls, context access, local AI customization, and privacy-related actions.

Contains:

- AI Data Processing Consent.
- Preserve Draft.
- AI Context Access enable/disable.
- Custom Prompt save/clear.
- Personal AI Training save/clear.
- AI tone/status information.
- Write Caption / Photo Caption action when currently exposed from settings.
- Privacy, data use, and local history action.

Existing IME deep links using `MainActivity.EXTRA_OPEN_SECTION` and `MainActivity.SECTION_AI_PRIVACY` must route directly to this category screen.

### 6.6 Clipboard

Purpose: clipboard history configuration.

Contains:

- Recent Clipboard History ON/OFF.
- Clear Recent Clipboard History.

Clipboard controls must not remain mixed into general keyboard preferences.

### 6.7 Account & Subscription

Purpose: account identity, subscription/product status, usage, gateway selection, and advanced personal AI credentials.

Contains:

- Managed account status.
- Keyboard product status.
- AI Assistant product status.
- Usage status.
- Gateway status.
- Login / Switch Account.
- Refresh Account & Usage.
- Account / Payment / Device Portal.
- Use Managed AI.
- Logout.
- Advanced AI section:
  - Personal OpenRouter API key.
  - Save key.
  - Test key.
  - Clear key.
  - Use Personal OpenRouter.

All existing managed-session, entitlement, and gateway repository behavior is preserved.

### 6.8 Help & About

Purpose: support and product identity information.

Contains:

- Help & Support heading/content.
- WhatsApp Support action.
- App version.
- Developer name.
- Email.
- WhatsApp contact.
- Phone contact.

The former Help and About long-page sections become one dedicated destination to keep the hub concise.

## 7. Settings Theme Behavior

All four Settings Theme Packs use the same eight categories and destinations:

- Clean Modern
- Card Style
- Premium
- Pro Style

The selected Settings Theme Pack styles both the Settings Hub and every `SettingsCategoryActivity` page.

Theme changes may alter:

- Root background/surface.
- Header/toolbar appearance.
- Card and section surfaces.
- Accent colors.
- Icons.
- Typography treatment.
- Button/toggle visual treatment where the existing styler supports it.

Theme changes must not alter:

- Category availability.
- Category ordering.
- Destination behavior.
- Preference values.
- Account/backend state.
- Keyboard theme selection.

`SettingsThemeStyler` and `SettingsThemeCatalog` remain the visual source of truth rather than duplicating colors in category layouts.

## 8. Header and Back Stack

Every inner screen contains a top header with:

- Back arrow.
- Category title.
- Optional short subtitle describing the category.

Behavior:

- Header back arrow calls the normal back-stack action.
- Android system back gesture/button returns to the Settings Hub when the inner activity was opened from it.
- If an inner category was opened directly from an external/deep-link entry point and there is no Settings Hub beneath it, the back action finishes normally and returns to the previous app context.
- No custom nested category stack is introduced in Stage 24.3.

## 9. Deep-Link and Compatibility Rules

### 9.1 Existing AI & Privacy Deep Link

Preserve these public constants for source compatibility:

```kotlin
MainActivity.EXTRA_OPEN_SECTION
MainActivity.SECTION_AI_PRIVACY
```

New handling:

- When `MainActivity` receives `SECTION_AI_PRIVACY`, it immediately opens `SettingsCategoryActivity(AI_PRIVACY)` instead of scrolling.
- A direct category-intent helper may also be introduced for new callers.

### 9.2 Legacy Dashboard Categories

`SettingsThemeDashboardRenderer.Category` currently contains categories such as `VOICE`, `APPEARANCE`, and `PRIVACY`. Stage 24.3 replaces theme-dependent category subsets with the canonical eight-category model.

Legacy enum values may be removed only after all current in-project call sites are migrated. If retained temporarily, mappings must be explicit and tested:

- `APPEARANCE` → `THEME_APPEARANCE`
- `PRIVACY` → `AI_PRIVACY`
- `VOICE` → `TYPING_SUGGESTIONS` only if the current voice control still exists there; otherwise no Voice top-level category is displayed.

The canonical dashboard displayed to users is always the eight-category list.

## 10. State and Data Flow

### 10.1 Read Flow

```text
Settings Hub / Category Screen
        ↓
Existing repositories and stores
        ↓
Persisted preference/account/theme state
        ↓
Refresh UI
```

`SettingsCategoryActivity` reads from the same application-scoped repositories currently used by `MainActivity`, including settings, theme, settings-theme, managed account/session, entitlement, and AI gateway stores.

### 10.2 Write Flow

User actions write through the existing repository/service APIs. The category activity does not write raw preference keys directly when a repository API already exists.

After a write:

- Update the visible status/control on the current inner screen.
- Preserve existing Toast/status feedback when it remains useful.
- Do not force navigation back to the hub.

### 10.3 Theme Changes While Inside a Category

If the user changes the Settings Theme Pack from Theme & Appearance, the current category activity re-applies the new settings theme immediately rather than requiring an app restart.

If the user changes the Keyboard Theme Package, the Theme & Appearance screen refreshes theme-specific Key Boundary/Bubble values immediately.

## 11. Error Handling and Safe Fallbacks

- Missing or unknown category extra: finish the invalid category activity and open/return to the Settings Hub rather than showing a blank screen.
- Missing view for a category layout: treat as a development error in tests; production must not silently expose partial controls.
- Account/backend refresh failure: retain current error/status handling and keep the category screen usable.
- External Android keyboard settings unavailable: preserve existing safe error handling/toast behavior.
- Settings Theme resource/style failure: fall back to existing base widget styling rather than blocking access to preferences.
- Deep-link category not recognized: fall back to the Settings Hub.

## 12. Accessibility and UX Requirements

- Category cards/rows are focusable and clickable with meaningful labels.
- Back arrow has a content description.
- Inner-screen titles are visible text, not icon-only.
- Existing checkbox/switch labels remain associated with their controls.
- Scrolling happens only within the current dedicated category page; there is no programmatic scroll from one category to another.
- The Hub must remain usable on smaller screens and with larger font scaling; theme renderers must not rely on fixed-height text containers that clip category names.

## 13. Code Organization

Expected responsibilities:

### `MainActivity.kt`

- Settings Hub setup.
- Settings Theme dashboard rendering.
- Launch category activity.
- Legacy deep-link redirect.
- No category-specific preference wiring after migration is complete.

### `SettingsCategoryActivity.kt`

- Parse/validate category ID.
- Inflate category layout.
- Apply selected Settings Theme.
- Bind category-specific existing business logic.
- Refresh category state.
- Back navigation.

If the activity grows beyond a maintainable size during implementation, category-specific binders may be extracted into focused helpers. Stage 24.3 must not introduce unrelated architecture refactors.

### `SettingsThemeDashboardRenderer.kt`

- Render the same eight canonical categories for each Settings Theme Pack.
- Vary visual presentation only.
- Emit canonical `SettingsCategoryId` selections.

### `SettingsThemeStyler.kt`

- Style Hub and inner-category surfaces consistently.
- Remain the shared Settings Theme presentation layer.

### XML Layouts

- `activity_main.xml`: Hub-only content.
- Eight category layouts: controls moved from the old long page without duplicated IDs in any single inflated hierarchy.

## 14. Migration Strategy

Implementation should migrate one category at a time while tests enforce the final Hub-only contract.

Final state requirements:

- Old `section_*` containers that represented option sections are absent from `activity_main.xml`.
- The old `smoothScrollTo()` category-navigation path is absent from `MainActivity`.
- All existing actionable settings are reachable from one of the eight category screens.
- No preference is present on both Hub and inner category pages.
- No user setting value is reset by upgrading to Stage 24.3.

## 15. Testing Strategy

### 15.1 Static/Contract Tests

Verify:

- Exactly eight canonical category IDs exist.
- All four Settings Theme Packs render all eight canonical categories.
- `MainActivity` launches category screens and contains no category `smoothScrollTo()` navigation.
- `activity_main.xml` is Hub-only and does not contain migrated option-section IDs.
- Each dedicated layout exists and contains its expected key controls.
- Existing preference keys/repository methods are reused.
- AI & Privacy legacy deep-link routes to `AI_PRIVACY`.

### 15.2 Pure Kotlin Tests

Where possible, test:

- Category wire-value parsing.
- Invalid-category fallback decision.
- Legacy dashboard/deep-link mapping.
- Canonical category ordering.

### 15.3 Android/Instrumentation or Robolectric Tests

When the Android build environment is available:

- Launch each category from the Hub.
- Verify Back returns to Hub.
- Verify deep-link AI & Privacy opens the correct inner screen.
- Change Settings Theme inside Theme & Appearance and verify current inner page restyles without losing values.
- Change Keyboard Theme and verify Key Boundary/Bubble values refresh to the selected theme's persisted values.

### 15.4 Regression Suite

Run existing verification for:

- Stage 23.8 Number Row.
- Stage 23.9 Settings Theme Packs.
- Stage 24.0 Theme Appearance / Key Boundary.
- Stage 24.1 Bubble Flight to Caret.
- Stage 24.2 Optional Theme Bubble.
- Release/backend/privacy checks.
- XML parsing.

The missing verified `gradle-wrapper.jar` remains an environment/tooling blocker for full Gradle build verification until bootstrapped; static checks must not be described as an APK build.

## 16. Acceptance Criteria

Stage 24.3 is acceptable when all of the following are true:

1. Main Settings displays only the eight top-level category destinations and hub/header presentation.
2. No actual category option remains exposed underneath the Hub category list.
3. Each category opens a dedicated inner settings screen.
4. Every existing setting is reachable from exactly one appropriate category, except intentional shortcut actions that call the same underlying setting.
5. Clean Modern, Card Style, Premium, and Pro Style expose the same eight destinations and use the same navigation behavior.
6. Settings Theme changes affect presentation only and do not reset values.
7. Number Row remains global; Key Boundary and Bubble Effect/Style remain keyboard-theme-specific.
8. Existing AI & Privacy deep links continue to work and open the dedicated AI & Privacy screen.
9. Back arrow and Android back navigation behave normally.
10. Invalid category input safely returns to the Hub instead of presenting a blank page.
11. Existing repositories, preference keys, backend/account services, and keyboard behavior remain the sources of truth.
12. Regression tests for prior completed stages remain green.

## 17. Out of Scope

- Adding new settings unrelated to the migration.
- Replacing Activities with Navigation Component/Fragments.
- Redesigning keyboard layouts.
- Changing preference semantics or default values.
- Changing account/subscription backend APIs.
- Adding a ninth top-level Voice category.
- Reworking the four Settings Theme Pack visual identities beyond what is necessary to style the new Hub/inner pages consistently.
