# Typing Core v2 — Stage 13

Stage 13 hardens runtime/session boundaries and Glide performance before real-device beta testing.

## Runtime safety
- Delayed insertions carry an editor fingerprint: package, input type, stable fieldId when available, hint, and monotonic expiry timestamp.
- Delayed voice/caption results wait while helper activities are foreground, then insert only when the originating editor fingerprint returns; BLOCK_AI fields reject them.
- Stale/mismatched caption drafts are discarded rather than inserted into another field.
- `onFinishInput()` finishes the editor's already-visible composing span and discards local buffers; it does not recompute/autocorrect during teardown.
- Null `EditorInfo` disables suggestions, autocorrect, and learning instead of inheriting the prior field policy.
- Glide gesture path storage is capped at 64 key transitions.

## Glide performance
- English and Bangla Glide lexicons are normalized once per `GlideTypingEngine` instance.
- Entries are bucketed by exact first key, preserving the Stage-9 first-key safety rule while avoiding unrelated lexicon scans.
- Length-delta filtering runs before weighted edit/path scoring.

## Device smoke harness
`scripts/stage13_adb_ime_smoke.sh` is diagnostic by default. Passing `apply` explicitly enables/selects the IME, preventing accidental default-keyboard changes on a developer device.

## Sandbox limitation
This environment still lacks Android SDK/verified Gradle wrapper availability, so a real APK/device run is not claimed here. GitHub CI remains the APK build path; the ADB smoke helper is ready for the resulting artifact.
