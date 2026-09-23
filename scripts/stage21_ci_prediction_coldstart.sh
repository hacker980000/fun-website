#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.socialaiassistant.keyboard"
HARNESS="${PACKAGE}/.debug.ImeValidationActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage21-prediction-coldstart-report}"
BASE_DIR="${OUT_DIR}/stage20-base"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$OUT_DIR"

# Stage 21 is additive: preserve the full Stage 20 -> 19 -> 18 -> 17 -> 16/15 chain.
bash scripts/stage20_ci_resize_memory.sh "$APK" "$BASE_DIR"

ui_dump() {
  adb shell uiautomator dump --compressed /sdcard/stage21-window.xml >/dev/null 2>&1 || \
    adb shell uiautomator dump /sdcard/stage21-window.xml >/dev/null
  adb pull /sdcard/stage21-window.xml "$TMP_DIR/window.xml" >/dev/null
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
  sleep 0.22
}

tap_desc() {
  local desc="$1" xy
  xy="$(node_value content-desc "$desc" center)"
  adb shell input tap $xy >/dev/null
  sleep 0.12
}

suggestion_texts() {
  ui_dump
  python3 - "$TMP_DIR/window.xml" <<'PY'
import xml.etree.ElementTree as ET, sys
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter('node'):
    desc = node.attrib.get('content-desc', '')
    if desc.startswith('Suggestion '):
        print(node.attrib.get('text', ''))
PY
}

tap_suggestion_text() {
  local expected="$1" xy
  ui_dump
  xy="$(python3 - "$TMP_DIR/window.xml" "$expected" <<'PY'
import re, sys, xml.etree.ElementTree as ET
path, expected = sys.argv[1:]
root = ET.parse(path).getroot()
for node in root.iter('node'):
    if node.attrib.get('text') != expected:
        continue
    if not node.attrib.get('content-desc', '').startswith('Suggestion '):
        continue
    m = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', node.attrib.get('bounds', ''))
    if not m:
        raise SystemExit(3)
    x1, y1, x2, y2 = map(int, m.groups())
    print(f"{(x1+x2)//2} {(y1+y2)//2}")
    raise SystemExit(0)
raise SystemExit(4)
PY
)"
  adb shell input tap $xy >/dev/null
  sleep 0.20
}

field_text() {
  node_value resource-id "${PACKAGE}:id/$1" text
}

ALL_LOG="$OUT_DIR/logcat-stage21-all.txt"
: > "$ALL_LOG"
ENGLISH_PREDICTION_PASS=NO
BANGLA_PREDICTION_PASS=NO
ENGLISH_ACCEPT_PASS=NO
TOTAL_CRASH_COUNT=0

for round in 1 2 3; do
  adb logcat -c
  adb shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
  adb shell am start -W -n "$HARNESS" > "$OUT_DIR/cold-start-round-${round}-am-start.txt"
  sleep 0.55
  tap_resource validation_normal

  # Controlled CI text only: exercise first-suggestion cold path and a stable English next-word fixture.
  tap_desc "Key t"
  tap_desc "Key h"
  tap_desc "Key a"
  tap_desc "Key n"
  tap_desc "Key k"
  tap_desc "Space"
  sleep 0.25
  suggestion_texts > "$OUT_DIR/cold-start-round-${round}-english-suggestions.txt"
  grep -Fxq "you" "$OUT_DIR/cold-start-round-${round}-english-suggestions.txt" || {
    echo "ERROR: expected English next-word candidate 'you' was not visible in round $round" >&2
    exit 40
  }
  ENGLISH_PREDICTION_PASS=YES

  if [[ "$round" == "1" ]]; then
    tap_suggestion_text "you"
    case "$(field_text validation_normal)" in
      "thank you "*) ENGLISH_ACCEPT_PASS=YES ;;
      *) echo "ERROR: tapping expected English candidate did not insert 'thank you '" >&2; exit 41 ;;
    esac

    # Independent Bangla fixture on a fresh editor. Cross-language mode change remains an explicit user tap.
    tap_resource validation_chat
    LANG_TEXT="$(node_value resource-id "${PACKAGE}:id/toolbar_language" text | tr -d '\r')"
    if [[ "$LANG_TEXT" == "EN" ]]; then
      tap_desc "Language switch"
    fi
    tap_desc "Key a"
    tap_desc "Key m"
    tap_desc "Key i"
    tap_desc "Space"
    sleep 0.25
    suggestion_texts > "$OUT_DIR/bangla-ami-suggestions.txt"
    grep -Fxq "এখন" "$OUT_DIR/bangla-ami-suggestions.txt" || {
      echo "ERROR: expected Bangla next-word candidate 'এখন' was not visible" >&2
      exit 42
    }
    BANGLA_PREDICTION_PASS=YES
  fi

  adb shell pidof "$PACKAGE" > "$OUT_DIR/cold-start-round-${round}-pid.txt"
  test -s "$OUT_DIR/cold-start-round-${round}-pid.txt" || {
    echo "ERROR: IME process missing after Stage 21 cold-start round $round" >&2
    exit 43
  }

  adb shell dumpsys meminfo "$PACKAGE" > "$OUT_DIR/cold-start-round-${round}-meminfo.txt" 2>&1 || true
  adb shell dumpsys gfxinfo "$PACKAGE" framestats > "$OUT_DIR/cold-start-round-${round}-gfxinfo.txt" 2>&1 || true
  adb logcat -d -v threadtime > "$OUT_DIR/cold-start-round-${round}-logcat.txt"
  cat "$OUT_DIR/cold-start-round-${round}-logcat.txt" >> "$ALL_LOG"
  grep -E 'FATAL EXCEPTION|ANR in|SocialAiImeStage21|cold_start milestone=|prediction_present|prediction_accept' \
    "$OUT_DIR/cold-start-round-${round}-logcat.txt" > "$OUT_DIR/cold-start-round-${round}-stage21-filter.txt" || true

  CRASH_COUNT="$(grep -Ec 'FATAL EXCEPTION|ANR in' "$OUT_DIR/cold-start-round-${round}-stage21-filter.txt" || true)"
  TOTAL_CRASH_COUNT=$((TOTAL_CRASH_COUNT + CRASH_COUNT))
  [[ "$CRASH_COUNT" == "0" ]] || {
    echo "ERROR: crash/ANR during Stage 21 cold-start round $round" >&2
    exit 44
  }

  for milestone in learning_models_ready service_ready first_input_view_ready first_suggestion_ready; do
    grep -q "cold_start milestone=${milestone} " "$OUT_DIR/cold-start-round-${round}-stage21-filter.txt" || {
      echo "ERROR: missing cold-start milestone ${milestone} in round $round" >&2
      exit 45
    }
  done
done

# Parse fixed-name timing markers into machine-readable evidence. No user text is read from app logs.
python3 - "$OUT_DIR" <<'PY'
from pathlib import Path
import csv, re, sys
out = Path(sys.argv[1])
pattern = re.compile(r"cold_start milestone=([a-z_]+) elapsed_ms=([0-9.]+)")
rows = []
for path in sorted(out.glob("cold-start-round-*-stage21-filter.txt")):
    round_no = re.search(r"round-(\d+)", path.name).group(1)
    for line in path.read_text(errors="replace").splitlines():
        m = pattern.search(line)
        if m:
            rows.append((int(round_no), m.group(1), float(m.group(2))))
with (out / "cold-start-metrics.csv").open("w", newline="") as handle:
    writer = csv.writer(handle)
    writer.writerow(["round", "milestone", "elapsed_ms"])
    writer.writerows(rows)
if not rows:
    raise SystemExit("no Stage 21 cold-start timing rows parsed")

by = {}
for _, milestone, value in rows:
    by.setdefault(milestone, []).append(value)
with (out / "cold-start-summary.txt").open("w") as handle:
    for milestone in sorted(by):
        values = sorted(by[milestone])
        median = values[len(values)//2]
        handle.write(f"{milestone}_samples={len(values)} median_ms={median:.2f} min_ms={min(values):.2f} max_ms={max(values):.2f}\n")
PY

PRESENT_MARKERS="$(grep -Ec 'prediction_present ' "$ALL_LOG" || true)"
ACCEPT_MARKERS="$(grep -Ec 'prediction_accept ' "$ALL_LOG" || true)"
COLD_MARKERS="$(grep -Ec 'cold_start milestone=' "$ALL_LOG" || true)"

{
  echo "stage=21"
  echo "base_stage20=PASS"
  echo "cold_start_rounds=3"
  echo "cold_start_marker_count=$COLD_MARKERS"
  echo "prediction_present_markers=$PRESENT_MARKERS"
  echo "prediction_accept_markers=$ACCEPT_MARKERS"
  echo "english_thank_you_prediction=PASS"
  echo "english_prediction_tap_insert=PASS"
  echo "bangla_ami_prediction=PASS"
  echo "process_survival=PASS"
  echo "crash_or_anr_markers=$TOTAL_CRASH_COUNT"
  echo "privacy_note=app_stage21_logs_contain_only_fixed_milestones_durations_language_counts_rank_and_boolean_modes_no_typed_text"
  echo "tuning_note=cold_start_times_are_evidence_only_no_latency_threshold_is_hard_gated_until_real_device_baselines_are_collected"
} > "$OUT_DIR/summary.txt"

echo "PASS: Stage 21 prediction/cold-start evidence collected."
cat "$OUT_DIR/summary.txt"
cat "$OUT_DIR/cold-start-summary.txt"
