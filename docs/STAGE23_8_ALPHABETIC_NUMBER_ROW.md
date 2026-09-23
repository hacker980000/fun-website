# Stage 23.8 — Settings-Controlled Alphabetic Number Row

## Goal

Make the optional `1 2 3 4 5 6 7 8 9 0` row a single Settings-controlled preference across every alphabetic typing layout and every complete Theme Package.

## Behavior

- **Number Row ON**: the `1–0` row is inserted above alphabetic keys.
- **Number Row OFF**: the optional row is absent and the alphabetic keyboard remains compact.
- The preference applies to **English**, **Bangla Phonetic**, and **Bangla Bijoy** letter layouts, including shifted states.
- Dedicated NUMBER and SYMBOL surfaces never receive a duplicate optional row.
- Editor-specific quick-key rows remain supported. When both are present, the number row is first and the quick-key row follows it.
- The existing persisted `show_number_row` preference remains the source of truth, so existing user choices are preserved.
- IME settings collection triggers a key-surface rerender, and both render/layout cache signatures include `showNumberRow`, preventing a stale ON/OFF layout.

## Theme Package coverage

The number-row decision is made before theme styling. The same layout contract therefore applies to all complete packages:

- Classic Dark
- Glass Modern
- Clean Light
- Gradient Pro

Theme packages may change colors, fills, corner radii, gaps, typography and effects, but they do not change whether the optional number row is present.

## Scope safety

This stage does not change the dedicated screenshot-style numeric pad from Stage 23.7, symbol layout actions, typing composition logic, AI features, privacy policy, or theme-selection persistence.
