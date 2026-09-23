# Stage 14 Real-device IME validation

Stage 14 adds a debug-only editor harness and an ADB evidence collector. The release manifest is unchanged; `ImeValidationActivity` exists only under `app/src/debug`.

## 1. Build / install debug APK

Build in CI or a machine with Android SDK + verified Gradle wrapper. Then run:

```bash
bash scripts/verify_apk_artifact.sh app/build/outputs/apk/debug/app-debug.apk
bash scripts/stage14_adb_device_validation.sh diagnostic app/build/outputs/apk/debug/app-debug.apk stage14-device-report
```

`diagnostic` does not change the user's default keyboard. To enable and select this keyboard on a dedicated test device:

```bash
bash scripts/stage14_adb_device_validation.sh apply app/build/outputs/apk/debug/app-debug.apk stage14-device-report
```

## 2. Manual field matrix

The script attempts to open the debug-only `ImeValidationActivity`. Validate:

- Normal text: English/Bangla composition, suggestions, Space commit, Backspace hold.
- Chat / Send: action key says Send and default editor action is honored.
- No suggestions: candidate strip and autocorrect remain disabled.
- Email: literal Latin typing and email quick keys; no personal learning.
- URL: literal Latin typing and URL quick keys; no autocorrect/personal learning.
- Password: AI, clipboard history, suggestions, learning, Voice/Caption insertion remain blocked.
- Phone / Number: numeric layer is selected and no duplicate number row appears.
- Glide: enable in Settings, test English and Bangla Phonetic paths, then rotate device mid-session.
- Spacebar cursor: drag left/right; no accidental space after a drag.
- Lifecycle: switch apps/fields while composing; stale composition must not leak into the next editor.
- Voice/Caption: return to the originating field; if the editor connection refuses the commit, the pending result must be retained rather than reported as inserted.

## 3. Evidence to review

The ADB script captures:

- Android version / API level
- installed IME list and default IME before/after
- `dumpsys input_method`
- process memory (`dumpsys meminfo`)
- full logcat snapshot and a filtered IME/crash view
- crash/ANR marker count

A zero crash/ANR marker count is a smoke result, not a substitute for extended soak testing.

## 4. Performance interpretation

`Stage14PerformanceRegressionTest` and `typing_stage14_performance_benchmark.kt` are deliberately coarse JVM guards. They catch algorithmic regressions such as repeated full-dictionary scans or re-indexing. Android latency acceptance must be measured on low/mid/high-tier physical devices before Play release.
