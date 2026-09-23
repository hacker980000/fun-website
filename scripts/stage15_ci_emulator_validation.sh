#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
IME_ID="${PACKAGE}/.ime.SocialAiInputMethodService"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage15-emulator-report}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

command -v adb >/dev/null 2>&1 || { echo "ERROR: adb missing" >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "ERROR: python3 missing" >&2; exit 2; }
test -f "$APK" || { echo "ERROR: APK not found: $APK" >&2; exit 2; }
mkdir -p "$OUT_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage15-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage15-window.xml >/dev/null
  adb pull /sdcard/stage15-window.xml "$TMP_DIR/window.xml" >/dev/null
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
            print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
        else:
            print(node.attrib.get(out, ''))
        raise SystemExit(0)
raise SystemExit(4)
PY
}

tap_resource() {
  local id="$1" xy
  xy="$(node_value resource-id "${PACKAGE}:id/${id}" center)" || {
    echo "ERROR: resource not found: $id" >&2
    return 1
  }
  adb shell input tap $xy >/dev/null
  sleep 0.35
}

tap_desc() {
  local desc="$1" xy
  xy="$(node_value content-desc "$desc" center)" || {
    echo "ERROR: content description not found: $desc" >&2
    return 1
  }
  adb shell input tap $xy >/dev/null
  sleep 0.18
}

field_text() {
  node_value resource-id "${PACKAGE}:id/$1" text
}

assert_field_equals() {
  local id="$1" expected="$2" actual
  actual="$(field_text "$id")"
  [[ "$actual" == "$expected" ]] || {
    echo "ERROR: $id text mismatch. expected=[$expected] actual=[$actual]" >&2
    return 1
  }
}

assert_field_prefix() {
  local id="$1" expected="$2" actual
  actual="$(field_text "$id")"
  [[ "$actual" == "$expected"* ]] || {
    echo "ERROR: $id prefix mismatch. expected prefix=[$expected] actual=[$actual]" >&2
    return 1
  }
}

assert_desc_present() {
  node_value content-desc "$1" center >/dev/null
}

assert_desc_absent() {
  if node_value content-desc "$1" center >/dev/null 2>&1; then
    echo "ERROR: unexpected content description present: $1" >&2
    return 1
  fi
}

adb wait-for-device
adb install -r "$APK" | tee "$OUT_DIR/install.txt"
adb shell ime enable "$IME_ID" | tee "$OUT_DIR/ime-enable.txt"
adb shell ime set "$IME_ID" | tee "$OUT_DIR/ime-set.txt"
adb shell settings get secure default_input_method > "$OUT_DIR/default-ime.txt"
grep -Fq "$IME_ID" "$OUT_DIR/default-ime.txt" || { echo "ERROR: keyboard is not default IME" >&2; exit 5; }

adb logcat -c
adb shell am force-stop "$PACKAGE" || true
adb shell am start -W -n "$HARNESS" | tee "$OUT_DIR/harness-start.txt"
sleep 1

# Real IME taps: no adb text injection is used for the typing assertions.
tap_resource validation_normal
tap_desc "Key h"
tap_desc "Key i"
assert_field_equals validation_normal "hi"

tap_desc "Language switch"
tap_resource validation_chat
tap_desc "Key a"
tap_desc "Key m"
tap_desc "Key i"
tap_desc "Space"
assert_field_prefix validation_chat "আমি"

tap_resource validation_email
tap_desc "Key a"
tap_desc "Key m"
tap_desc "Key i"
assert_field_equals validation_email "ami"
assert_desc_present "Language switch unavailable in literal Latin field"

tap_resource validation_number
assert_desc_present "Key 1"
assert_desc_absent "Key a"

# Rotation/lifecycle smoke while the IME is active.
PREV_ACCEL="$(adb shell settings get system accelerometer_rotation | tr -d '\r')"
PREV_ROT="$(adb shell settings get system user_rotation | tr -d '\r')"
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
sleep 1
adb shell settings put system user_rotation 0
sleep 1
adb shell settings put system accelerometer_rotation "${PREV_ACCEL:-1}" || true
adb shell settings put system user_rotation "${PREV_ROT:-0}" || true
adb shell pidof "$PACKAGE" > "$OUT_DIR/pid-after-rotation.txt"
test -s "$OUT_DIR/pid-after-rotation.txt" || { echo "ERROR: package process not alive after rotation" >&2; exit 6; }

adb shell getprop ro.build.version.release > "$OUT_DIR/android-version.txt"
adb shell getprop ro.build.version.sdk > "$OUT_DIR/android-sdk.txt"
adb shell dumpsys input_method > "$OUT_DIR/dumpsys-input-method.txt"
adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/meminfo.txt" 2>&1 || true
adb logcat -d -v threadtime > "$OUT_DIR/logcat-full.txt"
grep -E "FATAL EXCEPTION|ANR in|AndroidRuntime|SocialAiInputMethodService|${PACKAGE}" \
  "$OUT_DIR/logcat-full.txt" > "$OUT_DIR/logcat-ime-filter.txt" || true
CRASH_COUNT="$(grep -Ec "FATAL EXCEPTION|ANR in" "$OUT_DIR/logcat-ime-filter.txt" || true)"
{
  echo "stage=15"
  echo "api=$(cat "$OUT_DIR/android-sdk.txt" | tr -d '\r')"
  echo "default_ime=$(cat "$OUT_DIR/default-ime.txt" | tr -d '\r')"
  echo "english_e2e=PASS"
  echo "bangla_phonetic_e2e=PASS"
  echo "email_literal_latin=PASS"
  echo "numeric_layer=PASS"
  echo "rotation_survival=PASS"
  echo "crash_or_anr_markers=$CRASH_COUNT"
} > "$OUT_DIR/summary.txt"

if [[ "$CRASH_COUNT" != "0" ]]; then
  echo "ERROR: crash/ANR markers captured; inspect $OUT_DIR/logcat-ime-filter.txt" >&2
  exit 7
fi

echo "PASS: Stage 15 end-to-end emulator validation completed."
cat "$OUT_DIR/summary.txt"
