#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage22-autocorrect-quality-report}"
BASE_DIR="${OUT_DIR}/stage21-base"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$OUT_DIR"

# Stage 22 is additive: preserve the complete Stage 21 -> 20 -> 19 -> ... validation chain.
bash scripts/stage21_ci_prediction_coldstart.sh "$APK" "$BASE_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage22-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage22-window.xml >/dev/null
  adb pull /sdcard/stage22-window.xml "$TMP_DIR/window.xml" >/dev/null
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
  sleep 0.18
}

tap_desc() {
  local desc="$1" xy
  xy="$(node_value content-desc "$desc" center)"
  adb shell input tap $xy >/dev/null
  sleep 0.07
}

field_text() {
  node_value resource-id "${PACKAGE}:id/$1" text
}

fresh_english_editor() {
  adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  adb shell am start -W -n "$HARNESS" > "$TMP_DIR/am-start.txt"
  sleep 0.45
  tap_resource validation_normal
  local lang
  lang="$(node_value resource-id "${PACKAGE}:id/toolbar_language" text | tr -d '\r')"
  if [[ "$lang" != "EN" ]]; then
    tap_desc "Language switch"
  fi
}

type_letters() {
  local word="$1"
  local i ch
  for ((i=0; i<${#word}; i++)); do
    ch="${word:i:1}"
    tap_desc "Key ${ch}"
  done
}

assert_fixture() {
  local name="$1" typed="$2" expected="$3"
  fresh_english_editor
  type_letters "$typed"
  tap_desc "Space"
  sleep 0.18
  local actual
  actual="$(field_text validation_normal | tr -d '\r')"
  printf '%s\tinput=%s\texpected=%s\tactual=%s\n' "$name" "$typed" "$expected" "$actual" >> "$OUT_DIR/fixtures.tsv"
  [[ "$actual" == "$expected" ]] || {
    echo "ERROR: Stage 22 fixture '$name' expected '$expected' but got '$actual'" >&2
    exit 52
  }
}

: > "$OUT_DIR/fixtures.tsv"
adb logcat -c

# Controlled fixture text only. These are static test words, never user/editor content from production.
assert_fixture "explicit_alias" "dont" "don't "
assert_fixture "confident_multi_candidate_typo" "wdth" "with "
assert_fixture "ambiguous_typo_guard" "shave" "shave "
assert_fixture "short_typo_guard" "teh" "teh "

adb shell pidof "$PACKAGE" > "$OUT_DIR/final-pid.txt"
test -s "$OUT_DIR/final-pid.txt" || {
  echo "ERROR: IME process missing after Stage 22 fixtures" >&2
  exit 53
}

adb logcat -d -v threadtime > "$OUT_DIR/logcat-stage22.txt"
CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/logcat-stage22.txt" || true)"
[[ "$CRASH_COUNT" == "0" ]] || {
  echo "ERROR: crash/ANR during Stage 22 autocorrect fixtures" >&2
  exit 54
}

{
  echo "stage=22"
  echo "base_stage21=PASS"
  echo "explicit_alias_autocorrect=PASS"
  echo "confident_multi_candidate_typo=PASS"
  echo "ambiguous_typo_guard=PASS"
  echo "short_typo_guard=PASS"
  echo "process_survival=PASS"
  echo "crash_or_anr_markers=$CRASH_COUNT"
  echo "privacy_note=stage22_device_fixtures_use_only_static_control_words_and_do_not_log_real_user_text"
  echo "tuning_note=ranking_weights_and_latency_budgets_are_not_changed from uncollected device evidence"
} > "$OUT_DIR/summary.txt"

echo "PASS: Stage 22 autocorrect confidence fixtures completed."
cat "$OUT_DIR/summary.txt"
