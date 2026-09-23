# Stage 23.9 — Premium Settings Theme Packs

Stage 23.9 introduces a settings-theme system that is independent from the keyboard layout theme system.

## First-run flow

1. The launcher opens `ThemeOnboardingActivity` for the keyboard layout theme.
2. After a keyboard Theme Pack is selected, setup continues to `SettingsThemeOnboardingActivity`.
3. The user selects one of four Settings Theme Packs.
4. `MainActivity` opens with the selected settings design applied to the complete settings panel.
5. Later, `Change Settings Theme` re-opens the Settings Theme picker without altering the keyboard theme.

Existing installations that already completed keyboard-theme onboarding but have no settings-theme selection are sent through the new settings-theme step once after upgrading.

## Packs

- **Clean Modern** — cyan neon list navigation inspired by the supplied reference.
- **Card Style** — colorful 2-column category cards.
- **Premium** — premium dark list with vivid category icon tiles.
- **Pro Style** — professional category list plus a personalization quote/banner.

All packs preserve the same existing settings IDs, event bindings, AI/privacy controls, keyboard preferences, account actions, and backend behavior. The pack changes navigation presentation, colors, surfaces, borders, typography accents, and category dashboard style only.

## Keyboard-theme recommendation

Stage 23.9 does not force-link the two theme systems. It recommends a settings pack based on the currently selected keyboard pack while preserving user choice:

- Classic Dark -> Clean Modern
- Glass Modern -> Card Style
- Clean Light -> Premium
- Gradient Pro -> Pro Style

The recommendation is shown in the onboarding/picker. The user may select any settings pack. Changing the keyboard pack later does not silently overwrite a manually chosen settings pack.

## Persistence

Settings-theme state is stored separately in `social_ai_settings_theme`:

- `settings_theme_pack_id`
- `settings_theme_setup_complete`

This keeps keyboard theme selection and Settings UI selection independent.
