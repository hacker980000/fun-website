# Stage 15 CI / device validation

Stage 15 converts the debug validation surface into an end-to-end IME smoke test that taps the keyboard UI itself. It never uses `adb shell input text` for the typing assertions.

## Automated API 36 emulator smoke

The normal Android CI workflow downloads the exact debug APK built by the `debug-apk` job, boots an API 36 Google APIs x86_64 emulator, enables/selects this IME on that disposable emulator, and runs:

1. English keyboard taps: `h`, `i` -> `hi`.
2. Language switch -> Bangla phonetic `a`, `m`, `i`, Space -> `আমি`.
3. Email editor while Bangla was selected -> literal Latin `ami`.
4. Number editor -> numeric key is present and alphabetic key is absent.
5. Orientation changes while the IME is active -> process remains alive.
6. Logcat crash/ANR gate plus `dumpsys input_method` and `meminfo` evidence capture.
7. `connectedDebugAndroidTest` for Android instrumentation coverage.

Stable debug-only editor IDs and semantic keyboard `contentDescription` labels make the UIAutomator path deterministic enough for CI while also improving key accessibility metadata.

## Compatibility matrix

`workflow_dispatch` runs the same end-to-end script on API 26, API 30, and API 36 emulators. Before Play release, also run it on one low/mid-tier physical phone and one recent physical phone. The script intentionally selects this keyboard as the default IME, so use a dedicated test device/profile.

Archive APK SHA-256, Android version/API, `dumpsys input_method`, `dumpsys meminfo`, Stage 15 summary, full/filtered logcat, and manual notes for Spacebar cursor, Glide, long-press Backspace, AI explicit-tap flow, sensitive fields, Voice/Caption return flow, and app switching.

The emulator gate validates lifecycle and deterministic editor behavior but does not replace physical-device latency/ergonomics testing.
