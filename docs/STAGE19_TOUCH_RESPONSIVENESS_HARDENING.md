# Stage 19 — Touch responsiveness and low-end jank hardening

Stage 19 intentionally avoids debouncing normal text keys so legitimate rapid double letters remain intact.

## Changes

- Toolbar, suggestion and AI action buttons use a bounded rapid-action gate to prevent accidental duplicate actions.
- Text keys are never routed through that gate.
- Haptic/sound feedback uses a tiny cadence guard to avoid duplicate feedback bursts without affecting human-speed typing.
- Suggestion-bar updates are posted once per animation frame and duplicate requests coalesce.
- Glide start respects Android system touch slop in addition to the keyboard's own minimum threshold.
- Glide hit testing has a 6dp nearest-key tolerance across visual key gaps, reducing dead zones while keeping a hard distance bound.
- Toolbar, suggestion and AI action targets are at least 48dp high. Compact letter-key height remains user-controlled.
- Removed a duplicate literal-Latin composition flush in the direct text path.
- Stage 19 CI retains the complete Stage 18 validation chain and adds a rapid toolbar-double-tap test, 48dp target evidence, 40-key real IME burst, process-survival check, gfxinfo and crash/ANR evidence.

## Privacy

All new gates/caches are in-memory only. They store action IDs/timestamps or pending flags, never typed text, clipboard data, package names, AI prompts or conversation content.

## Verification

- Stage 19 pure-Kotlin touch/responsiveness self-test: 33/33 PASS.
- Stage 19 static source contract: 16/16 PASS.
- Stage 16 GlideHitMap/runtime self-test: 11/11 PASS with the new tolerance-capable hit map.
- Stage 17 runtime-resource self-test: 23/23 PASS.
- Stage 18 render/memo self-test: 20/20 PASS.
- Stage 7 English: 17/17 PASS; Stage 9 Avro/Glide: 41/41 PASS; Stage 10 Avro quality: 53/53 PASS; Stage 12 editor policy: 46/46 PASS; Stage 12 Avro golden: 18/18 PASS.
- Release, bounded-update, 1000-emoji, XML, CI YAML and shell syntax checks PASS.
- Protected `ai/`, `backend/`, `context/`, `safety/` source remains byte-for-byte unchanged from Stage 18.

A full Android APK/emulator run still requires the verified Gradle wrapper/Android SDK/network dependencies available in CI; the Stage 19 CI wrapper is prepared for that run.
