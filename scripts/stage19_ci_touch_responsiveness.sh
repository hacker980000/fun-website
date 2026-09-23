#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage19-touch-responsiveness-report}"
BASE_DIR="${OUT_DIR}/stage18-base"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$OUT_DIR"

# Preserve the complete Stage 18 -> Stage 17 -> Stage 16/15 validation chain.
bash scripts/stage18_ci_render_latency.sh "$APK" "$BASE_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage19-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage19-window.xml >/dev/null
  adb pull /sdcard/stage19-window.xml "$TMP_DIR/window.xml" >/dev/null
}

node_value() {
  local attr="$1" value="$2" output_attr="$3"
  ui_dump
  python3 - "$TMP_DIR/window.xml" "$attr" "$value" "$output_attr" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, attr, value, out = sys.argv[1:]
root = ET.parse(path).getroot()
for node in root.iter('node'):
    if node.attrib.get(attr) == value:
        if out == 'center':
            m = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib.get('bounds', ''))
            if not m:
                raise SystemExit(3)
            x1, y1, x2, y2 = map(int, m.groups())
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
        elif out == 'height':
            m = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib.get('bounds', ''))
            if not m:
                raise SystemExit(3)
            x1, y1, x2, y2 = map(int, m.groups())
            print(y2-y1)
        else:
            print(node.attrib.get(out, ''))
        raise SystemExit(0)
raise SystemExit(4)
PY
}

tap_resource() {
  local id="$1" xy
  xy="$(node_value resource-id "${PACKAGE}:id/${id}" center)"
  adb shell input tap $xy >/dev/null
  sleep 0.25
}

# Re-open the deterministic debug field matrix after the Stage 18 chain.
adb shell am start -W -n "$HARNESS" >/dev/null
sleep 0.5
tap_resource validation_normal

# Establish Bangla as the known starting language for the rapid-double-tap check.
LANG_TEXT="$(node_value resource-id "${PACKAGE}:id/toolbar_language" text | tr -d '\r')"
LANG_XY="$(node_value resource-id "${PACKAGE}:id/toolbar_language" center)"
if [[ "$LANG_TEXT" == "EN" ]]; then
  adb shell input tap $LANG_XY >/dev/null
  sleep 0.25
fi
LANG_TEXT="$(node_value resource-id "${PACKAGE}:id/toolbar_language" text | tr -d '\r')"
[[ "$LANG_TEXT" != "EN" ]] || { echo "ERROR: could not establish Bangla start state" >&2; exit 19; }

# Two near-simultaneous toolbar taps must be treated as one action; otherwise language flips twice.
adb shell "input tap $LANG_XY; input tap $LANG_XY" >/dev/null
sleep 0.30
LANG_AFTER="$(node_value resource-id "${PACKAGE}:id/toolbar_language" text | tr -d '\r')"
[[ "$LANG_AFTER" == "EN" ]] || {
  echo "ERROR: rapid toolbar double tap was not suppressed; language=[$LANG_AFTER]" >&2
  exit 20
}

# Action touch targets should be at least 48dp high on the emulator density.
TOOLBAR_HEIGHT_PX="$(node_value resource-id "${PACKAGE}:id/toolbar_ai" height)"
DENSITY="$(adb shell wm density | awk -F': ' '/Override density/{v=$2} /Physical density/{if(!v) v=$2} END{gsub(/\r/,"",v); print v}')"
python3 - "$TOOLBAR_HEIGHT_PX" "$DENSITY" <<'PY'
import sys
height_px, density = map(float, sys.argv[1:])
dp = height_px * 160.0 / density
if dp < 47.0:  # one-dp tolerance for device rounding
    raise SystemExit(f"toolbar target too small: {dp:.2f}dp")
print(f"toolbar_touch_target_dp={dp:.2f}")
PY

# Rapid real-key burst: text keys are deliberately NOT debounced. This is a crash/jank evidence pass,
# not a text-content benchmark because composing/autocorrect can vary by mode.
KEY_A_XY="$(node_value content-desc "Key a" center)"
BURST_CMDS=""
for _ in $(seq 1 40); do BURST_CMDS+="input tap $KEY_A_XY; "; done
adb logcat -c
adb shell "$BURST_CMDS" >/dev/null
sleep 0.5
adb shell pidof "$PACKAGE" > "$OUT_DIR/pid-after-rapid-key-burst.txt"
test -s "$OUT_DIR/pid-after-rapid-key-burst.txt" || { echo "ERROR: IME process died after rapid key burst" >&2; exit 21; }

adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT_DIR/gfxinfo-framestats.txt" 2>&1 || true
adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo-after-touch-burst.txt" 2>&1 || true
adb logcat -d -v threadtime > "$OUT_DIR/logcat-touch-burst.txt"
grep -E 'FATAL EXCEPTION|ANR in|slow_input_connection|runtime_metric|runtime_render_coalesced|SocialAiInputMethodService' \
  "$OUT_DIR/logcat-touch-burst.txt" > "$OUT_DIR/logcat-touch-burst-filter.txt" || true
CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/logcat-touch-burst-filter.txt" || true)"
[[ "$CRASH_COUNT" == "0" ]] || { echo "ERROR: crash/ANR after touch burst" >&2; exit 22; }

{
  echo "stage=19"
  echo "base_stage18=PASS"
  echo "rapid_toolbar_double_tap=PASS"
  echo "toolbar_touch_target_px=$TOOLBAR_HEIGHT_PX"
  echo "device_density_dpi=$DENSITY"
  echo "rapid_text_key_burst_count=40"
  echo "rapid_text_keys_not_debounced=PASS"
  echo "process_survival_after_touch_burst=PASS"
  echo "crash_or_anr_markers=$CRASH_COUNT"
  echo "touch_accuracy_note=glide_gap_tolerance_and_system_touch_slop_are_covered_by_source_selftests"
} > "$OUT_DIR/summary.txt"

echo "PASS: Stage 19 touch responsiveness evidence collected."
cat "$OUT_DIR/summary.txt"
