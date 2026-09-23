# Stage 23.7 Verification Report

Baseline: Social AI Keyboard v35.3.1 / Typing Core v2 / Stage 23.6 Numeric Bottom Row Alignment.

Scope: remove the unused fourth center-row height beneath `7 8 9`, place the full-width `ABC , !?# 0 = . Enter` row immediately after the three digit rows, keep `0` centered under `8`, and preserve identical NUMBER geometry across Classic Dark, Glass Modern, Clean Light and Gradient Pro.

## Verified results

- Stage 23.7 numeric no-gap static contract: **31/31 PASS**.
- Stage 23.7 numeric geometry self-test: **8/8 PASS**.
- Stage 23.5 shared numeric-pad contract regression: **30/30 PASS**.
- Stage 23.5 numeric-pad Kotlin data self-test: **13/13 PASS**.
- Stage 12 static typing regression: **PASS**.
- Stage 12 layout Kotlin regression: **6/6 PASS**.
- Stage 23.4 first-run Theme Package verification: **11/11 PASS**.
- Stage 23.2 premium multi-theme verification: **53/53 PASS**.
- Stage 23.3 Bubble Key verification: **32/32 PASS**.
- Stage 23 baseline verification: **36/36 PASS**.
- Release/privacy verification: **PASS**.
- Shared backend config verification: **PASS**.
- Training parity verification: **PASS**.
- Android resource/manifest XML parse: **18/18 PASS**.

## Intentional Stage 23.6 supersession

Stage 23.6 used an 8dp negative top margin to visually pull the bottom row upward while the main numeric container still reserved four row-heights. Stage 23.7 removes that workaround and changes the main numeric container to exactly three digit-row heights. Therefore the Stage 23.6 fixed-lift contract is historical and intentionally not treated as a current regression requirement.

## Build-toolchain note

Full Android `assembleDebug` is not claimed in this environment because the source archive does not vendor the verified `gradle-wrapper.jar`. `scripts/check_gradle_wrapper_completeness.py` correctly reports that wrapper bootstrap is required. The existing verified-wrapper policy is preserved; no unverified wrapper binary was inserted.
