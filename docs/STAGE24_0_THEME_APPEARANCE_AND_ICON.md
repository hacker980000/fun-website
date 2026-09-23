# Stage 24.0 — Theme Appearance & Launcher Icon

## Scope

Stage 24.0 restores an explicit launcher icon contract and adds a per-Theme-Package alphabetic key-boundary preference.

## Launcher icon

The application manifest now declares `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`. Adaptive icon resources are present under `mipmap-anydpi-v26`, with fallback vector resources under `mipmap-anydpi`.

## Per-theme Key Boundary

Each complete keyboard Theme Package has an independent `Key Boundary` switch inside its own Theme Pack card in Theme Settings:

- Classic Dark
- Glass Modern
- Clean Light
- Gradient Pro

The value is stored separately per pack. Changing one package does not change the other packages.

Default for all four packages is **Off**, matching the clean borderless alphabetic-key design requested for fresh installs.

### Off

Only alphabetic typing keys are rendered without a key surface/boundary. Special keys (Shift, Backspace, Space, Enter, language and layer controls), the optional 1–0 number row, Symbol surface and dedicated Number Pad keep their existing visual treatment.

### On

Alphabetic keys use the selected Theme Package's normal themed key surface/boundary again.

## Language coverage

The alphabetic visual role is explicitly marked in the layout model for:

- English QWERTY
- Bangla Phonetic QWERTY
- Bangla Bijoy, including combining-mark keys such as vowel signs

This avoids guessing from Unicode letter categories and keeps non-letter rows unaffected.

## Theme resolution

If a keyboard surface has a per-surface Theme Package override, Key Boundary follows that actual package. Otherwise it follows the selected global Theme Package.

## Verification

- `scripts/verify_theme_stage24_0.py`
- `scripts/typing_stage24_0_key_boundary_selftest.kt`
- Existing Stage 23.2/23.4/23.8/23.9 theme and layout regressions
