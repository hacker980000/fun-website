#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
IME_ID="${PACKAGE}/.ime.SocialAiInputMethodService"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
MODE="${1:-diagnostic}"
APK="${2:-}"
OUT_DIR="${3:-stage14-device-report}"

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb is not available." >&2
  exit 2
fi

mkdir -p "$OUT_DIR"
adb get-state >/dev/null
SERIAL="$(adb get-serialno)"
DATE_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
{
  echo "stage=14"
  echo "timestamp_utc=$DATE_UTC"
  echo "serial=$SERIAL"
  echo "mode=$MODE"
} > "$OUT_DIR/summary.txt"

if [[ -n "$APK" ]]; then
  test -f "$APK"
  adb install -r "$APK" | tee "$OUT_DIR/install.txt"
fi

adb shell getprop ro.build.version.release > "$OUT_DIR/android-version.txt"
adb shell getprop ro.build.version.sdk > "$OUT_DIR/android-sdk.txt"
adb shell ime list -a > "$OUT_DIR/ime-list.txt"
adb shell settings get secure default_input_method > "$OUT_DIR/default-ime-before.txt"
adb shell dumpsys input_method > "$OUT_DIR/dumpsys-input-method-before.txt"

if ! adb shell pm path "$PACKAGE" > "$OUT_DIR/package-path.txt" 2>&1; then
  echo "ERROR: $PACKAGE is not installed. Supply a debug APK as argument 2." >&2
  exit 3
fi

if [[ "$MODE" == "apply" ]]; then
  adb shell ime enable "$IME_ID" | tee "$OUT_DIR/ime-enable.txt"
  adb shell ime set "$IME_ID" | tee "$OUT_DIR/ime-set.txt"
else
  echo "Diagnostic mode: default IME is not changed." | tee "$OUT_DIR/mode-note.txt"
fi

# Debug APK only. This may fail on a release build because the validation activity is debug-only.
adb shell am start -W -n "$HARNESS" > "$OUT_DIR/validation-harness-start.txt" 2>&1 || true
sleep 1

adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo.txt" 2>&1 || true
adb shell dumpsys input_method > "$OUT_DIR/dumpsys-input-method-after.txt"
adb shell settings get secure default_input_method > "$OUT_DIR/default-ime-after.txt"
adb logcat -d -v threadtime > "$OUT_DIR/logcat-full.txt"
grep -E "FATAL EXCEPTION|ANR in|AndroidRuntime|InputMethod|SocialAiInputMethodService|com.socialaiassistant.keyboard" \
  "$OUT_DIR/logcat-full.txt" > "$OUT_DIR/logcat-ime-crash-filter.txt" || true

CRASH_COUNT="$(grep -Ec "FATAL EXCEPTION|ANR in" "$OUT_DIR/logcat-ime-crash-filter.txt" || true)"
echo "crash_or_anr_markers=$CRASH_COUNT" >> "$OUT_DIR/summary.txt"

if [[ "$CRASH_COUNT" != "0" ]]; then
  echo "WARNING: crash/ANR markers found. Inspect $OUT_DIR/logcat-ime-crash-filter.txt" >&2
  exit 4
fi

echo "PASS: Stage 14 device diagnostics completed with no captured crash/ANR marker."
echo "Report: $OUT_DIR"
