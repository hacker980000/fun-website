#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage20-resize-memory-report}"
BASE_DIR="${OUT_DIR}/stage19-base"
TMP_DIR="$(mktemp -d)"
ORIGINAL_ACCEL="$(adb shell settings get system accelerometer_rotation 2>/dev/null | tr -d '\r' || echo 1)"
ORIGINAL_ROTATION="$(adb shell settings get system user_rotation 2>/dev/null | tr -d '\r' || echo 0)"
cleanup() {
  adb shell settings put system accelerometer_rotation "${ORIGINAL_ACCEL:-1}" >/dev/null 2>&1 || true
  adb shell settings put system user_rotation "${ORIGINAL_ROTATION:-0}" >/dev/null 2>&1 || true
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT
mkdir -p "$OUT_DIR"

# Keep every Stage 19 -> 18 -> 17 -> 16/15 functional/runtime check.
bash scripts/stage19_ci_touch_responsiveness.sh "$APK" "$BASE_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage20-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage20-window.xml >/dev/null
  adb pull /sdcard/stage20-window.xml "$TMP_DIR/window.xml" >/dev/null
}

node_center_by_resource() {
  local id="$1"
  ui_dump
  python3 - "$TMP_DIR/window.xml" "${PACKAGE}:id/${id}" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, resource_id = sys.argv[1:]
root = ET.parse(path).getroot()
for node in root.iter('node'):
    if node.attrib.get('resource-id') == resource_id:
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
  xy="$(node_center_by_resource "$id")"
  adb shell input tap $xy >/dev/null
  sleep 0.20
}

adb shell am start -W -n "$HARNESS" >/dev/null
sleep 0.5
tap_resource validation_normal
adb logcat -c
adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-before-resize.txt" 2>&1 || true

# Deterministic rapid orientation/configuration burst. The IME should coalesce same-frame UI work.
adb shell settings put system accelerometer_rotation 0 >/dev/null
for rotation in 1 0 1 0 1 0; do
  adb shell settings put system user_rotation "$rotation" >/dev/null
  sleep 0.18
done
sleep 0.7

adb shell pidof "$PACKAGE" > "$OUT_DIR/pid-after-orientation-burst.txt"
test -s "$OUT_DIR/pid-after-orientation-burst.txt" || {
  echo "ERROR: IME process died during orientation burst" >&2
  exit 30
}

# Exercise ordinary hide/show/re-focus. Stage 20 should retain identical populated key rows.
for _ in 1 2 3 4; do
  adb shell am start -W -n "$HARNESS" >/dev/null
  sleep 0.18
  tap_resource validation_normal
done

# Explicit UI-hidden pressure must release optional bitmap/layout resources and recover cleanly.
adb shell am send-trim-memory "$PACKAGE" UI_HIDDEN >/dev/null 2>&1 || true
sleep 0.35
adb shell am start -W -n "$HARNESS" >/dev/null
sleep 0.35
tap_resource validation_normal
adb shell pidof "$PACKAGE" > "$OUT_DIR/pid-after-ui-hidden-recovery.txt"
test -s "$OUT_DIR/pid-after-ui-hidden-recovery.txt" || {
  echo "ERROR: IME process died after UI-hidden recovery" >&2
  exit 31
}

adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-after-resize.txt" 2>&1 || true
adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT_DIR/gfxinfo-framestats.txt" 2>&1 || true
adb shell dumpsys input_method > "$OUT_DIR/input-method-state.txt" 2>&1 || true
adb logcat -d -v threadtime > "$OUT_DIR/logcat-resize-memory.txt"
grep -E 'FATAL EXCEPTION|ANR in|runtime_render_coalesced|runtime_configuration_refresh|runtime_resource_released|runtime_metric|SocialAiImePerf' \
  "$OUT_DIR/logcat-resize-memory.txt" > "$OUT_DIR/logcat-resize-memory-filter.txt" || true

CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"
[[ "$CRASH_COUNT" == "0" ]] || {
  echo "ERROR: crash/ANR during Stage 20 resize/memory run" >&2
  exit 32
}

KEYBOARD_COALESCE="$(grep -c 'runtime_render_coalesced target=keyboard' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"
TOOLBAR_COALESCE="$(grep -c 'runtime_render_coalesced target=toolbar' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"
BACKGROUND_COALESCE="$(grep -c 'runtime_render_coalesced target=background' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"
CONFIG_COALESCE="$(grep -c 'runtime_render_coalesced target=configuration' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"
BACKGROUND_RELEASE="$(grep -c 'runtime_resource_released resource=theme_background' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"
CACHE_RELEASE="$(grep -c 'runtime_resource_released resource=layout_background_cache' "$OUT_DIR/logcat-resize-memory-filter.txt" || true)"

{
  echo "stage=20"
  echo "base_stage19=PASS"
  echo "orientation_burst_count=6"
  echo "orientation_burst_process_survival=PASS"
  echo "ordinary_refocus_count=4"
  echo "ui_hidden_recovery=PASS"
  echo "crash_or_anr_markers=$CRASH_COUNT"
  echo "keyboard_render_coalesced_markers=$KEYBOARD_COALESCE"
  echo "toolbar_render_coalesced_markers=$TOOLBAR_COALESCE"
  echo "background_render_coalesced_markers=$BACKGROUND_COALESCE"
  echo "configuration_coalesced_markers=$CONFIG_COALESCE"
  echo "theme_background_release_markers=$BACKGROUND_RELEASE"
  echo "layout_background_cache_release_markers=$CACHE_RELEASE"
  echo "note=coalescing marker counts are evidence, not hard pass thresholds; process/crash checks are hard gates"
} > "$OUT_DIR/summary.txt"

echo "PASS: Stage 20 resize/memory evidence collected."
cat "$OUT_DIR/summary.txt"
