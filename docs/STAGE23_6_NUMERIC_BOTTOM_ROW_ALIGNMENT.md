# Stage 23.6 - Numeric Bottom Row Alignment

## Goal

Apply the approved final numeric-pad geometry correction to every complete theme package without changing typing, AI, privacy, account, quota, or editor-action behavior.

The Stage 23.5 reference structure remains intact. Stage 23.6 only raises the full-width bottom action row slightly so the `0` key reads as the fourth-row continuation of the center numeric column and sits directly below `8`.

## Geometry contract

Main numeric block:

- Left rail: `+`, `-`, `*`, `/`
- Center: `1 2 3`, `4 5 6`, `7 8 9`
- Right rail: `%`, space, backspace

Raised bottom row:

`ABC` | `,` | `!?#` | `0` | `=` | `.` | Enter

The bottom row is lifted by 8dp from the normal Stage 23.5 flow position. Its total width remains exactly 9 units:

- `ABC`: 1.5
- `,`: 1
- `!?#`: 1
- `0`: 2
- `=`: 1
- `.`: 1
- Enter: 1.5

The cumulative width before `0` is 3.5 units. Because `0` itself is 2 units wide, its center is at 4.5 / 9 = 50% of the keyboard width. The center digit column (`2`, `5`, `8`) is also at 50%, so `0` stays directly below `8`.

## Theme behavior

The renderer is shared by all complete theme packages:

- Classic Dark
- Glass Modern
- Clean Light
- Gradient Pro

No theme-specific geometry fork is introduced. Theme packages continue to control appearance only (surface colors, fill, glass/gradient treatment, typography scale, radii, borders and pressed effects).

## Preserved behavior

- `ABC` returns to letters.
- `!?#` opens symbols.
- `0` commits `0`.
- Space keeps the existing cursor-control behavior when enabled.
- Backspace keeps repeat/delete behavior.
- Enter keeps Android editor-aware action labels/behavior.
- Sensitive-field policy, Bubble Key policy, AI behavior and backend integration are unchanged.

## Verification

Run:

```sh
python3 scripts/verify_numberpad_stage23_6.py
python3 scripts/numeric_pad_stage23_6_geometry_selftest.py
```

Then run the existing Stage 23.5, theme, Bubble Key, release, privacy/backend and XML regression checks before packaging.
