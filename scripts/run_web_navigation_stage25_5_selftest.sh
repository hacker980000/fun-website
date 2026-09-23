#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/social_ai_stage25_5_web"
rm -rf "$OUT"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/backend/BackendConfig.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/backend/WebNavigationPolicy.kt" \
  "$ROOT/scripts/web_navigation_stage25_5_selftest.kt" \
  -include-runtime -d "$OUT/selftest.jar"
java -jar "$OUT/selftest.jar"
