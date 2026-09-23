# Typing Core v2 — Stage 14

Stage 14 is a release-validation and insertion-reliability hardening stage. It intentionally does not change the Bangla/English dictionaries, Avro mappings, AI prompts, backend, context extraction, or safety policy.

## Runtime insertion acknowledgement

`ReplyInserter` now distinguishes a real editor commit from a false-success path. `InsertResult.CommitFailed` is returned when editor reads/select-all/commit operations fail or throw. This prevents AI/manual insertion from reporting success when the host editor rejected the operation.

Deferred Voice and Caption delivery now consumes pending data only after a successful insertion. A temporary/stale `InputConnection` can therefore recover on a later lifecycle callback without silently losing the result.

## Debug-only editor harness

`app/src/debug/.../ImeValidationActivity.kt` exposes normal, chat/send, no-suggestions, email, URL, password, phone and number fields in one debug APK. It is declared only in `app/src/debug/AndroidManifest.xml`, so it is absent from release builds.

## Device evidence collector

`scripts/stage14_adb_device_validation.sh` is diagnostic by default. It captures Android/API version, IME list/default state, `dumpsys input_method`, process memory and logcat crash/ANR evidence. Only the explicit `apply` mode enables/selects the keyboard.

## APK artifact gate

CI now runs `scripts/verify_apk_artifact.sh` after `assembleDebug`. The gate checks APK existence/size, application id, version code/name, min/target SDK, `BIND_INPUT_METHOD`, IME service presence, and reports the APK SHA-256.

## Performance guard

`Stage14PerformanceRegressionTest` and `typing_stage14_performance_benchmark.kt` provide coarse JVM budgets for Glide index construction/query and Bangla suggestion lookup. They are regression guards for accidental algorithmic blow-ups, not Android latency claims.

Reference local benchmark for this source/container:

- 5 Glide engine initializations: ~94 ms total
- 1,350 Glide lookups: ~59 ms total
- 2,250 Bangla suggestion lookups: ~70 ms total

Physical low/mid/high-tier Android device measurements are still required before Play release.

## Validation status

- Stage 14 insertion acknowledgement self-test: 14/14 PASS
- Stage 14 performance benchmark: PASS
- Stage 8 Glide: 11/11 PASS
- Stage 9 Avro/Glide: 41/41 PASS
- Stage 10 Avro quality: 53/53 PASS
- Stage 12 Avro golden: 18/18 PASS
- Stage 13 runtime hardening: 14/14 PASS
- Static Stage 2–14 verifiers: PASS
- XML/YAML/shell syntax/1000 emoji gates: PASS
- Protected `ai/`, `backend/`, `context/`, `safety/`: no source changes

A full Android Gradle/APK build still requires an environment with Android SDK and the verified Gradle wrapper/distribution. The canonical GitHub Actions workflow is prepared to perform that build and now verifies the resulting APK artifact.
