# Stage 23.6 Verification Report

Baseline: Social AI Keyboard v35.3.1 / Typing Core v2 / Stage 23.5 Reference Numeric Pad.

Scope: raise the numeric bottom action row while preserving the shared key contract and keeping `0` centered under the `2/5/8` digit column in Classic Dark, Glass Modern, Clean Light and Gradient Pro.

## Verified results

- Stage 23.6 numeric bottom-row static contract: **26/26 PASS**.
- Stage 23.6 numeric geometry self-test: **4/4 PASS**.
- Stage 23.5 numeric-pad static regression: **30/30 PASS**.
- Stage 23.5 numeric-pad Kotlin data self-test: **13/13 PASS**.
- Stage 12 layout Kotlin regression: **6/6 PASS**.
- Stage 23.4 first-run theme-package verification: **11/11 PASS**.
- Stage 23.2 premium multi-theme verification: **53/53 PASS**.
- Stage 23.3 Bubble Key verification: **32/32 PASS**.
- Stage 23 baseline verification: **36/36 PASS**.
- Release/privacy verification: **PASS**.
- Shared backend config verification: **PASS**.
- Training parity verification: **PASS**.
- Android resource/manifest XML parse: **18/18 PASS**.

## Build-toolchain note

Full Android `assembleDebug` is not claimed in this environment because the source archive contains the wrapper checksum/policy but not the verified `gradle-wrapper.jar`, and this environment does not provide the project's complete Android SDK/Gradle toolchain. The existing wrapper-integrity policy is preserved; no unverified wrapper binary was downloaded or inserted.
