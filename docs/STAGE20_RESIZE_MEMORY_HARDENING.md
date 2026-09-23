# Stage 20 — Resize, startup-memory and render-jank hardening

Stage 20 focuses on repeat keyboard opens, orientation/resize churn, toolbar view reuse and theme-background memory behavior. It does not change AI/backend/context/safety behavior.

## Runtime changes

- Ordinary `onFinishInputView()` no longer invalidates already-populated key rows or clears Glide key View references. If Android reuses the same input view, the next editor can reuse the rows when the render signature still matches.
- New input-view creation and real memory-pressure paths still invalidate the relevant gates so stale View references are never reused after the root is replaced or deliberately released.
- Toolbar profile ordering is guarded by a value-based order signature. Repeated label/theme/session refreshes do not call `removeAllViews()` and re-add the same toolbar buttons.
- Configuration/orientation work is posted once per animation frame through the existing bounded `FrameWorkCoalescer` instead of immediately repeating dynamic UI work for a burst of configuration callbacks.
- Immutable keyboard layout models use a small 10-entry access-ordered cache. It is cleared under memory pressure.
- Theme background requests are keyed by normalized background content plus a bucketed decode target. Identical requests are skipped. A size-only change keeps the existing image visible until the resized image is ready, reducing resize flicker.
- Background decode targets are bucketed in 64px steps. Low-RAM devices cap keyboard background decode targets at 1024x640; other devices cap them at 1536x896.
- `ThemeBackgroundManager` now exposes an explicit decoded-bitmap memory-cache clear hook. UI-hidden/low-memory handling detaches the displayed theme bitmap and invalidates the background gate so the next visible session reloads safely.

## Privacy

Stage 20 caches only immutable layout metadata, toolbar resource IDs, normalized theme-background configuration and decoded local theme bitmaps. No typed text, clipboard content, package name, AI prompt or conversation content is added to persistence or telemetry.

## Device/CI evidence

`stage20_ci_resize_memory.sh` preserves the complete Stage 19 -> 18 -> 17 -> 16/15 chain and adds:

- six deterministic orientation changes,
- repeated validation-field reopen/refocus,
- UI-hidden trim-memory recovery,
- process-survival hard gates,
- crash/ANR hard gates,
- keyboard/toolbar/background/configuration coalescing marker counts,
- theme-background/layout-cache release markers,
- meminfo, gfxinfo and input-method dumpsys evidence.

The automatic API 36 emulator job and manual API 26/30/36 compatibility matrix now use the Stage 20 wrapper.

## Verification

- Stage 20 resize/memory pure-Kotlin self-test: 27/27 PASS.
- Stage 20 static source contract: 20/20 PASS.
- Stage 19 touch/responsiveness: 33/33 PASS.
- Stage 18 render/memo: 20/20 PASS.
- Stage 17 runtime resource: 23/23 PASS.
- Stage 16 runtime/perf: 11/11 PASS.
- Stage 13 runtime hardening: 14/14 PASS.
- Stage 12 editor policy: 46/46 PASS; Stage 12 Avro golden: 18/18 PASS.
- Stage 10 Avro quality: 53/53 PASS, dictionary remains 1,158 entries.
- Stage 9 Avro/Glide: 41/41 PASS.
- Stage 14 JVM algorithmic benchmark PASS. These JVM times are regression guards, not Android latency claims.
- 1000-emoji catalog self-test PASS.
- Stage 2–20 static contracts, release/bounded-update, XML, CI YAML and shell syntax checks PASS.
- Protected `ai/`, `backend/`, `context/`, `safety/` source is byte-for-byte unchanged from Stage 19.

A full Android APK/emulator run still requires the verified Gradle wrapper/Android SDK/network dependencies available in CI. No uncollected real-device latency or memory result is claimed here.
