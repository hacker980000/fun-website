#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage23-real-device-baseline-report}"
BASE_DIR="${OUT_DIR}/stage22-base"
ROUNDS="${STAGE23_ROUNDS:-5}"
DEVICE_LABEL="${STAGE23_DEVICE_LABEL:-unlabeled}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

command -v adb >/dev/null 2>&1 || { echo "ERROR: adb missing" >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "ERROR: python3 missing" >&2; exit 2; }
test -f "$APK" || { echo "ERROR: APK not found: $APK" >&2; exit 2; }
[[ "$ROUNDS" =~ ^[0-9]+$ ]] && (( ROUNDS >= 3 && ROUNDS <= 20 )) || {
  echo "ERROR: STAGE23_ROUNDS must be an integer from 3 to 20" >&2
  exit 2
}
mkdir -p "$OUT_DIR"

# Stage 23 is additive: keep the complete Stage 22 -> 21 -> 20 -> ... device/functional chain.
bash scripts/stage22_ci_autocorrect_quality.sh "$APK" "$BASE_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage23-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage23-window.xml >/dev/null
  adb pull /sdcard/stage23-window.xml "$TMP_DIR/window.xml" >/dev/null
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
  sleep 0.20
}

tap_desc() {
  local desc="$1" xy
  xy="$(node_value content-desc "$desc" center)"
  adb shell input tap $xy >/dev/null
  sleep 0.10
}

suggestion_texts() {
  ui_dump
  python3 - "$TMP_DIR/window.xml" <<'PY'
import xml.etree.ElementTree as ET, sys
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter('node'):
    if node.attrib.get('content-desc', '').startswith('Suggestion '):
        print(node.attrib.get('text', ''))
PY
}

prop() {
  adb shell getprop "$1" 2>/dev/null | tr -d '\r\n'
}

# Deliberately do not collect adb serial, account IDs, editor contents, clipboard, or user-app context.
{
  echo "label=$DEVICE_LABEL"
  echo "api=$(prop ro.build.version.sdk)"
  echo "android_release=$(prop ro.build.version.release)"
  echo "manufacturer=$(prop ro.product.manufacturer)"
  echo "model=$(prop ro.product.model)"
  echo "product=$(prop ro.product.name)"
  echo "abi=$(prop ro.product.cpu.abi)"
  echo "is_emulator=$(prop ro.kernel.qemu)"
  echo "build_type=$(prop ro.build.type)"
} > "$OUT_DIR/device.properties"
adb shell wm size > "$OUT_DIR/device-wm-size.txt" 2>&1 || true
adb shell wm density > "$OUT_DIR/device-wm-density.txt" 2>&1 || true
adb shell dumpsys display > "$OUT_DIR/device-display.txt" 2>&1 || true

TOTAL_CRASH_COUNT=0
REQUIRED_MARKERS=(learning_models_ready service_ready first_input_view_ready first_suggestion_ready)

for round in $(seq 1 "$ROUNDS"); do
  adb logcat -c
  adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  sleep 0.15
  adb shell am start -W -n "$HARNESS" > "$OUT_DIR/round-${round}-cold-am-start.txt"
  sleep 0.50
  tap_resource validation_normal

  # Fixed controlled CI fixture only; no real user text is captured or replayed.
  tap_desc "Key t"
  tap_desc "Key h"
  tap_desc "Key a"
  tap_desc "Key n"
  tap_desc "Key k"
  tap_desc "Space"
  sleep 0.25
  suggestion_texts > "$OUT_DIR/round-${round}-controlled-suggestions.txt"
  grep -Fxq "you" "$OUT_DIR/round-${round}-controlled-suggestions.txt" || {
    echo "ERROR: controlled 'thank -> you' prediction missing in Stage 23 round $round" >&2
    exit 60
  }

  adb shell pidof "$PACKAGE" > "$OUT_DIR/round-${round}-pid.txt"
  test -s "$OUT_DIR/round-${round}-pid.txt" || {
    echo "ERROR: IME process missing in Stage 23 round $round" >&2
    exit 61
  }

  adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/round-${round}-meminfo.txt" 2>&1 || true
  adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT_DIR/round-${round}-gfxinfo.txt" 2>&1 || true
  adb logcat -d -v threadtime > "$OUT_DIR/round-${round}-logcat.txt"

  CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/round-${round}-logcat.txt" || true)"
  TOTAL_CRASH_COUNT=$((TOTAL_CRASH_COUNT + CRASH_COUNT))
  [[ "$CRASH_COUNT" == "0" ]] || {
    echo "ERROR: crash/ANR during Stage 23 round $round" >&2
    exit 62
  }
  for milestone in "${REQUIRED_MARKERS[@]}"; do
    grep -q "cold_start milestone=${milestone} " "$OUT_DIR/round-${round}-logcat.txt" || {
      echo "ERROR: missing cold-start milestone ${milestone} in Stage 23 round $round" >&2
      exit 63
    }
  done

  # Warm activity relaunch while keeping the process alive. This is evidence only.
  adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
  sleep 0.15
  adb shell am start -W -n "$HARNESS" > "$OUT_DIR/round-${round}-warm-am-start.txt"
  sleep 0.20
  adb shell pidof "$PACKAGE" > "$OUT_DIR/round-${round}-warm-pid.txt"
  test -s "$OUT_DIR/round-${round}-warm-pid.txt" || {
    echo "ERROR: IME process missing after warm relaunch in round $round" >&2
    exit 64
  }
done

python3 scripts/stage23_parse_device_evidence.py "$OUT_DIR"

{
  echo "stage=23"
  echo "base_stage22=PASS"
  echo "rounds=$ROUNDS"
  echo "controlled_prediction_fixture=PASS"
  echo "required_cold_start_milestones=PASS"
  echo "process_survival=PASS"
  echo "crash_or_anr_markers=$TOTAL_CRASH_COUNT"
  echo "device_serial_collected=NO"
  echo "real_user_text_collected=NO"
  echo "hard_latency_budget=NONE"
  echo "hard_memory_budget=NONE"
  echo "hard_frame_budget=NONE"
  echo "tuning_rule=Stage23_records_baseline_evidence_only_Stage24_may_tune_after_comparing_real_results"
} > "$OUT_DIR/summary.txt"

cat "$OUT_DIR/summary.txt"
cat "$OUT_DIR/baseline-summary.txt"
echo "PASS: Stage 23 real-device/emulator baseline evidence collected."
