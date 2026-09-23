#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage16-runtime-report}"
BASE_DIR="${OUT_DIR}/stage15-base"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

command -v adb >/dev/null 2>&1 || { echo "ERROR: adb missing" >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "ERROR: python3 missing" >&2; exit 2; }
test -f "$APK" || { echo "ERROR: APK not found: $APK" >&2; exit 2; }
mkdir -p "$OUT_DIR"

# First run the full functional smoke from Stage 15. Runtime stress is additive.
bash scripts/stage15_ci_emulator_validation.sh "$APK" "$BASE_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage16-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage16-window.xml >/dev/null
  adb pull /sdcard/stage16-window.xml "$TMP_DIR/window.xml" >/dev/null
}

node_center() {
  local attr="$1" value="$2"
  ui_dump
  python3 - "$TMP_DIR/window.xml" "$attr" "$value" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, attr, value = sys.argv[1:]
root = ET.parse(path).getroot()
for node in root.iter('node'):
    if node.attrib.get(attr) == value:
        m = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib.get('bounds', ''))
        if not m:
            raise SystemExit(3)
        x1, y1, x2, y2 = map(int, m.groups())
        print(f"{(x1+x2)//2} {(y1+y2)//2}")
        raise SystemExit(0)
raise SystemExit(4)
PY
}

tap_resource() {
  local id="$1" xy
  xy="$(node_center resource-id "${PACKAGE}:id/${id}")"
  adb shell input tap $xy >/dev/null
  sleep 0.25
}

adb logcat -c
adb shell am start -W -n "$HARNESS" > "$OUT_DIR/harness-restart.txt"
sleep 0.5
tap_resource validation_normal

# Cache coordinates once. Repeated stress taps therefore exercise the IME rather than
# repeatedly paying UIAutomator hierarchy-dump cost.
A_XY="$(node_center content-desc 'Key a')"
M_XY="$(node_center content-desc 'Key m')"
I_XY="$(node_center content-desc 'Key i')"
SPACE_XY="$(node_center content-desc 'Space')"
BACKSPACE_XY="$(node_center content-desc 'Backspace')"

adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-before.txt" 2>&1 || true
START_MS="$(python3 - <<'PY'
import time
print(int(time.monotonic()*1000))
PY
)"

# 35 committed words = 140 real IME taps. This intentionally exercises composition,
# suggestions, personal-learning flush requests and editor commits without adb text injection.
ITERATIONS=35
for _ in $(seq 1 "$ITERATIONS"); do
  adb shell input tap $A_XY >/dev/null
  adb shell input tap $M_XY >/dev/null
  adb shell input tap $I_XY >/dev/null
  adb shell input tap $SPACE_XY >/dev/null
done

# Backspace repeat path receives direct taps too; long-press behavior remains covered manually.
for _ in $(seq 1 12); do adb shell input tap $BACKSPACE_XY >/dev/null; done

END_MS="$(python3 - <<'PY'
import time
print(int(time.monotonic()*1000))
PY
)"
ELAPSED_MS=$((END_MS-START_MS))

sleep 1
adb shell pidof "$PACKAGE" > "$OUT_DIR/pid-after-stress.txt"
test -s "$OUT_DIR/pid-after-stress.txt" || { echo "ERROR: package process died during stress" >&2; exit 6; }
adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-after.txt" 2>&1 || true
adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT_DIR/gfxinfo-framestats.txt" 2>&1 || true
adb shell dumpsys input_method > "$OUT_DIR/dumpsys-input-method.txt" 2>&1 || true
adb logcat -d -v threadtime > "$OUT_DIR/logcat-full.txt"
grep -E "FATAL EXCEPTION|ANR in|SocialAiImePerf|InputConnection failure|${PACKAGE}" \
  "$OUT_DIR/logcat-full.txt" > "$OUT_DIR/logcat-runtime-filter.txt" || true

CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/logcat-runtime-filter.txt" || true)"
SLOW_COUNT="$(grep -Ec 'slow_input_connection' "$OUT_DIR/logcat-runtime-filter.txt" || true)"
CONNECTION_FAILURE_COUNT="$(grep -Ec 'InputConnection failure' "$OUT_DIR/logcat-runtime-filter.txt" || true)"
TOTAL_TAPS=$((ITERATIONS*4+12))

{
  echo "stage=16"
  echo "base_stage15=PASS"
  echo "stress_iterations=$ITERATIONS"
  echo "stress_key_taps=$TOTAL_TAPS"
  echo "host_wall_elapsed_ms=$ELAPSED_MS"
  echo "host_wall_latency_note=includes_adb_transport_not_an_IME_latency_metric"
  echo "slow_input_connection_markers=$SLOW_COUNT"
  echo "input_connection_failure_markers=$CONNECTION_FAILURE_COUNT"
  echo "crash_or_anr_markers=$CRASH_COUNT"
  echo "process_alive_after_stress=PASS"
} > "$OUT_DIR/summary.txt"

if [[ "$CRASH_COUNT" != "0" ]]; then
  echo "ERROR: crash/ANR markers captured; inspect $OUT_DIR/logcat-runtime-filter.txt" >&2
  exit 7
fi

# Connection failures are collected as evidence because app/editor transitions can legitimately
# invalidate a connection. They are not a CI failure unless accompanied by process death/crash.
echo "PASS: Stage 16 runtime stress completed."
cat "$OUT_DIR/summary.txt"
