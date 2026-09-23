# Stage 16 — Runtime / Performance Hardening

Stage 16 is intentionally not described as a real-device-result fix because this source environment cannot execute the Android CI/emulator jobs. It hardens the hot paths and makes the next CI/device run more diagnostic.

## Runtime changes

- Personal Bangla and English learning still happens fully on-device, but frequent engine `flush()` calls are now coalesced through a 650 ms background persistence controller.
- Direct model `flush()` remains synchronous for deterministic tests and explicit lifecycle shutdown.
- The IME requests immediate background persistence when an input session finishes or UI memory is trimmed, and performs a final bounded flush when the service is destroyed.
- Same-instance SharedPreferences revision notifications no longer force a redundant full decode after every self-write.
- Normal `InputConnection` operations are guarded. A stale editor connection resets local composing/prediction state rather than crashing the IME or continuing phantom edits.
- Glide geometry is snapshotted once at gesture start instead of calling `getLocationOnScreen()` for every key on every MOVE event.
- Glide haptic feedback is throttled to a minimum 24 ms interval.

## CI/device evidence

`stage16_ci_runtime_stress.sh` runs the Stage 15 functional E2E smoke first, then performs 35 real IME `ami + Space` cycles plus Backspace taps. It collects:

- process survival,
- crash / ANR markers,
- InputConnection failure markers,
- debug slow InputConnection markers,
- meminfo before/after,
- gfxinfo framestats,
- input-method dumpsys.

Host wall time is recorded only as evidence and is **not** treated as keyboard latency because it includes ADB transport overhead.

The normal API 36 emulator job and the manual API 26/30/36 compatibility matrix now run this Stage 16 stress script.
