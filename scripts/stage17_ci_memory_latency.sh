#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage17-memory-latency-report}"
BASE_DIR="${OUT_DIR}/stage16-base"
mkdir -p "$OUT_DIR"

# Stage 17 is additive: keep the complete functional/runtime stress from Stage 16.
bash scripts/stage16_ci_runtime_stress.sh "$APK" "$BASE_DIR"

adb logcat -c
adb shell am start -W -n "${PACKAGE}/.debug.ImeValidationActivity" > "$OUT_DIR/harness-start.txt"
sleep 0.5
adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-before-trim.txt" 2>&1 || true

# Exercise Android memory callbacks. These are resilience checks, not a promise that
# every vendor maps trim levels identically.
adb shell am send-trim-memory "$PACKAGE" RUNNING_LOW > "$OUT_DIR/trim-running-low.txt" 2>&1 || true
sleep 0.4
adb shell am send-trim-memory "$PACKAGE" UI_HIDDEN > "$OUT_DIR/trim-ui-hidden.txt" 2>&1 || true
sleep 0.4

# Re-open the harness and require the IME process to remain healthy after pressure.
adb shell am start -W -n "${PACKAGE}/.debug.ImeValidationActivity" > "$OUT_DIR/harness-after-trim.txt"
sleep 0.5
adb shell pidof "$PACKAGE" > "$OUT_DIR/pid-after-trim.txt"
test -s "$OUT_DIR/pid-after-trim.txt" || { echo "ERROR: package process missing after trim-memory stress" >&2; exit 8; }

adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-after-trim.txt" 2>&1 || true
adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT_DIR/gfxinfo-after-trim.txt" 2>&1 || true
adb shell dumpsys input_method > "$OUT_DIR/dumpsys-input-method.txt" 2>&1 || true
adb logcat -d -v threadtime > "$OUT_DIR/logcat-full.txt"
grep -E "FATAL EXCEPTION|ANR in|SocialAiImePerf|runtime_metric|runtime_resource_released|${PACKAGE}" \
  "$OUT_DIR/logcat-full.txt" > "$OUT_DIR/logcat-runtime-filter.txt" || true

CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/logcat-runtime-filter.txt" || true)"
RUNTIME_METRIC_COUNT="$(grep -Ec 'runtime_metric metric=' "$OUT_DIR/logcat-runtime-filter.txt" || true)"
RESOURCE_RELEASE_COUNT="$(grep -Ec 'runtime_resource_released' "$OUT_DIR/logcat-runtime-filter.txt" || true)"

{
  echo "stage=17"
  echo "base_stage16=PASS"
  echo "trim_running_low_attempted=YES"
  echo "trim_ui_hidden_attempted=YES"
  echo "runtime_metric_markers=$RUNTIME_METRIC_COUNT"
  echo "runtime_resource_release_markers=$RESOURCE_RELEASE_COUNT"
  echo "crash_or_anr_markers=$CRASH_COUNT"
  echo "process_alive_after_trim=PASS"
  echo "metric_privacy_note=durations_and_fixed_metric_names_only_no_typed_text"
} > "$OUT_DIR/summary.txt"

if [[ "$CRASH_COUNT" != "0" ]]; then
  echo "ERROR: crash/ANR markers captured after Stage 17 trim-memory stress" >&2
  exit 9
fi

echo "PASS: Stage 17 memory/latency validation completed."
cat "$OUT_DIR/summary.txt"
