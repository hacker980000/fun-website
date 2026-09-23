# Stage 23.5 - Reference Numeric Pad

## Goal

Replace the previous horizontal number/symbol page with the calculator-style numeric layout supplied by the user. The same key geometry is used by every complete theme package; the active NUMBER surface theme continues to control colors, fills, radii, typography, gradients/glass and pressed effects.

Reference image: `docs/design-reference/stage23_5_numberpad_reference.jpg`.

## Layout contract

Top/main block:

- Left operator rail: `+`, `-`, `*`, `/` stacked vertically.
- Center digit grid: `1 2 3`, `4 5 6`, `7 8 9`.
- Right action rail: `%`, space, backspace.
- Thin theme-aware separators sit between the left rail / center grid and center grid / right rail.

Bottom row:

`ABC` | `,` | `!?#` | `0` | `=` | `.` | Enter

The bottom row totals 9 width units, matching the 1.5 + 6 + 1.5 main-column proportions. This keeps the left/right rails visually aligned with the full-width bottom row.

## Theme behavior

Classic Dark / Glass Modern / Clean Light / Gradient Pro all share the exact same numeric key placement and actions. Theme switching changes only the resolved `KeyboardThemeSurface.NUMBER` appearance. No theme-specific numeric layout fork is introduced.

## Behavior preserved

- `ABC` returns to letters.
- `!?#` opens the symbol layer.
- Space uses the existing spacebar behavior, including cursor-control handling when enabled.
- Backspace keeps the existing repeat/delete behavior.
- Enter continues to respect Android editor actions (`Enter`, `Go`, `Next`, `Done`, etc.).
- Bubble Key behavior is unchanged; the numeric-pad renderer does not bypass its existing eligibility/safety policy.
- Number fields continue to open directly on `KeyboardLayer.NUMBERS`.

## Verification

Run:

```sh
python3 scripts/verify_numberpad_stage23_5.py
```

The Kotlin data-contract self-test is `scripts/numeric_pad_stage23_5_selftest.kt` and can be compiled with the existing Android stubs plus `KeyboardAction.kt`, `KeyboardMode.kt`, and `KeyboardLayout.kt`.
