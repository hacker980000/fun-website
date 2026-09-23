# Stage 24.3 - Dedicated Settings Category Navigation

Date: 2026-09-22

Stage 24.3 changes the settings experience from one long scrolling page into a hub-and-category structure. The Main Settings page is now a navigation hub only; every actual setting lives inside its owning dedicated category screen.

## Main Settings Hub

The hub exposes exactly eight destinations:

1. **Keyboard Setup** - keyboard enable/select status and Android input-method actions.
2. **Language & Input** - English suggestions, English autocorrect, smart language hints, and language/layout guidance for English, বাংলা, Phonetic, and Bijoy.
3. **Theme & Appearance** - keyboard theme, Settings Theme, Number Row, per-theme Key Boundary, per-theme Bubble Effect/Style, one-handed mode, and keyboard height.
4. **Typing & Suggestions** - personal typing learning, cursor control, glide typing, toolbar profile, haptic feedback, and key sound.
5. **AI & Privacy** - AI consent, draft preservation, context-access controls, custom instruction, personal training, caption entry, and privacy/data information.
6. **Clipboard** - recent clipboard history and clear-history action.
7. **Account & Subscription** - managed account/product/usage status, portal actions, gateway selection, and optional personal OpenRouter key controls.
8. **Help & About** - WhatsApp support plus app/developer contact information.

`MainActivity` no longer scrolls to `section_*` blocks and no migrated setting control remains in `activity_main.xml`.

## Dedicated Category Activity

All eight destinations are hosted by one reusable `SettingsCategoryActivity`. A stable `SettingsCategoryId.wireValue` is passed through `SettingsCategoryActivity.createIntent(...)`; the activity selects the corresponding focused layout and existing repository-backed behavior.

The category header provides a back arrow that uses the normal Android back stack. Android system back/gesture behaves the same way. If an invalid category value is supplied, the app does not display a blank page: it finishes back to the caller when possible or routes to the Settings Hub when it is the task root.

## Settings Theme parity

The four Settings Theme Packs share identical information architecture and destinations:

- Clean Modern
- Card Style
- Premium
- Pro Style

The active pack changes only presentation: root/background surface, category header, cards/rows, buttons, inputs, typography accents, and supporting visual treatment. It does not move settings between categories or change saved values.

When the user changes the Settings Theme from Theme & Appearance and returns, `SettingsCategoryActivity.onResume()` reapplies the current Settings Theme before refreshing the category state.

## State and ownership rules

Stage 24.3 reuses the existing repositories and persistence keys; category navigation does not create a second settings state.

- **Number Row** remains global and is controlled through the existing `SettingsRepository` preference.
- **Key Boundary** remains specific to the selected keyboard Theme Package.
- **Bubble Effect** and **Bubble Style** remain specific to the selected keyboard Theme Package, including the Stage 24.1 caret-flight behavior when enabled.
- Existing account/backend, SecretStore, AI consent, accessibility/context, clipboard, typing-learning, and toolbar-profile storage are preserved.

## Legacy AI & Privacy deep link

The existing public constants remain compatible:

- `MainActivity.EXTRA_OPEN_SECTION = "social_ai_open_section"`
- `MainActivity.SECTION_AI_PRIVACY = "ai_privacy"`

A caller using the legacy AI & Privacy section request now opens the dedicated **AI & Privacy** category directly instead of opening the Hub and scrolling inside one long page.

## File ownership overview

- `MainActivity.kt` / `activity_main.xml`: Settings Hub only.
- `SettingsCategoryId.kt`: canonical eight-category identity and legacy mapping.
- `SettingsCategoryActivity.kt`: common navigation shell plus existing setting bindings.
- `SettingsThemeDashboardRenderer.kt`: all four Settings Theme dashboards using the same canonical categories.
- `SettingsThemeStyler.kt`: Hub/category styling for the selected Settings Theme.
- `settings_category_*.xml`: one focused layout per category.

## Build and device status

Static source contracts, pure-Kotlin category tests, XML parsing, and the relevant Stage 23/24 regression gates are covered by `docs/STAGE24_3_VERIFICATION_REPORT.md`.

The source archive still does not vendor the verified `gradle-wrapper.jar`. The repository checker therefore reports **BOOTSTRAP REQUIRED**; Android compilation, lint, APK generation, and physical-device navigation testing must be run after the verified Gradle 9.6.0 wrapper is restored.
