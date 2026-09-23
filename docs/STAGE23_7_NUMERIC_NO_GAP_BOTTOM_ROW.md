# Stage 23.7 - Numeric No-Gap Bottom Row

## Goal

Remove the unwanted blank band below the `7 8 9` digit row while preserving the approved screenshot-style numeric layout for every complete Theme Package.

Stage 23.7 supersedes the Stage 23.6 fixed negative-margin lift. The center numeric block now reserves exactly three digit-row heights, so the full-width bottom action row follows the `7 8 9` row directly instead of sitting after an unused fourth center row.

## Geometry contract

Main numeric block:

- Left rail: `+`, `-`, `*`, `/`
- Center: `1 2 3`, `4 5 6`, `7 8 9`
- Right rail: `%`, space, backspace
- Main block height: exactly **3 digit rows**

Immediate next row:

`ABC` | `,` | `!?#` | `0` | `=` | `.` | Enter

There is no reserved blank fourth row beneath `7` or `9`.

The bottom-row width remains 9 units:

- `ABC`: 1.5
- `,`: 1
- `!?#`: 1
- `0`: 2
- `=`: 1
- `.`: 1
- Enter: 1.5

The center of `0` remains at 4.5 / 9 = 50% of the keyboard width. The center digit column (`2`, `5`, `8`) is also at 50%, so `0` remains directly below `8` while `ABC , !?#` occupy the same row on its left and `= . Enter` occupy the same row on its right.

## Theme behavior

The exact same geometry is shared by:

- Classic Dark
- Glass Modern
- Clean Light
- Gradient Pro

Theme packages continue to change visual treatment only. They do not change number-pad positions, weights, row count, actions, or alignment.

## Preserved behavior

- `ABC` returns to letters.
- `!?#` opens symbols.
- `0` commits `0`.
- Space keeps existing cursor-control behavior when enabled.
- Backspace keeps repeat/delete behavior.
- Enter keeps Android editor-aware action labels and behavior.
- Bubble Key, AI, sensitive-field, backend, and theme-selection behavior are unchanged.

## Verification

Run:

```sh
python3 scripts/verify_numberpad_stage23_7.py
python3 scripts/numeric_pad_stage23_7_geometry_selftest.py
```

Then run the Stage 23.5 numeric contract, current theme, Bubble Key, release, backend/training, and XML regressions before packaging.
