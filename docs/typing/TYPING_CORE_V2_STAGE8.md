# Typing Core v2 — Stage 8

Stage 8 adds daily-use ergonomics without changing the existing AI/backend/privacy contract.

## Added

- Optional offline Glide typing foundation for English and Bangla Phonetic modes.
  - Gesture capture is separated from normal taps by an 18dp movement threshold.
  - Repeated-letter paths are normalized (for example, `hello` can resolve from a `helo` key path).
  - Resolution is entirely on-device against the packaged English and Bangla phonetic lexicons.
  - The feature defaults to Off while the glide model is still a foundation implementation.
- One-handed key layout: Off / Left / Right.
  - One-handed keys use 84% of screen width and remain user-controlled.
- Toolbar layout profiles: Balanced / AI First / Typing / Minimal.
  - AI, language and settings remain reachable in all profiles.
- Glide commits feed the existing local English/Bangla learning models and then expose normal next-word suggestions.

## Safety and compatibility

- Sensitive-field rules are unchanged.
- No AI, backend, context, or safety source files were changed from Stage 7.
- No network is used for glide resolution.
- Normal key taps keep the pre-Stage-8 behavior when Glide is disabled.

## Verification

- Stage 1 phonetic: 18/18 PASS
- Stage 2 suggestion: 14/14 PASS
- Stage 3 suggestion: 15/15 PASS
- Stage 3 engine: 9/9 PASS
- Stage 4 personalization: 7/7 PASS
- Stage 5 context: 13/13 PASS
- Stage 7 English/multilingual: 17/17 PASS
- Stage 8 glide/customization: 11/11 PASS
- Stage 2–8 static verification: PASS
- bounded-update/release/emoji/XML checks: PASS
- Full Gradle build could not start because the environment cannot resolve `services.gradle.org`.
