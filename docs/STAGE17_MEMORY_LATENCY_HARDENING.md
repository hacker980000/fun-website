# Stage 17 — Memory / Latency Hardening

Stage 17 continues release hardening without claiming real-device results that have not yet been collected. The changes reduce optional startup work, add bounded privacy-safe latency evidence, and strengthen memory-pressure behavior.

## Runtime changes

- `GlideTypingEngine` is no longer initialized when the IME service is constructed. Glide indexes are created only when a Glide commit is actually requested.
- The optional Glide engine can be released under Android memory pressure and recreated on demand.
- If the keyboard UI is hidden while a dynamic Emoji/Clipboard/AI panel is open, the panel's generated child views are dropped under UI-hidden memory pressure; the next input view returns to the normal keys panel.
- `onLowMemory()` now releases optional runtime resources and requests local typing-learning persistence.
- Debug builds maintain a bounded 64-sample rolling latency window for fixed metric names only: input-view creation, keyboard rendering, suggestion computation, Glide resolution, and InputConnection operations.
- Runtime metrics contain durations only. They do not contain typed text, clipboard contents, package names, editor hints, AI prompts, or conversation context.
- Debug latency summaries report rolling p50/p95/max and over-budget counts every 32 samples.

## CI/device evidence

`stage17_ci_memory_latency.sh` runs the complete Stage 16 end-to-end and runtime stress suite first. It then sends `RUNNING_LOW` and `UI_HIDDEN` trim-memory requests, re-opens the validation harness, and collects:

- process survival after memory pressure,
- crash/ANR markers,
- Stage 17 runtime latency markers,
- optional-resource release markers,
- meminfo before/after trim,
- gfxinfo and input-method dumpsys.

The automatic API 36 emulator job and manual API 26/30/36 compatibility matrix now use the Stage 17 evidence script.

## Important limitation

This source environment still cannot run the Android Gradle/emulator job because the Android SDK / verified wrapper dependencies are not available locally and the required Google/Gradle hosts cannot be resolved. The CI/device path is prepared, but no real-device latency claim is made from the standalone JVM tests.
