#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/social_ai_stage25_3_sensitive_field"
rm -rf "$OUT"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/safety/FieldSafety.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveMetadataClassifier.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicy.kt" \
  "$ROOT/scripts/sensitive_field_stage25_3_selftest.kt" \
  -include-runtime -d "$OUT/selftest.jar"
java -jar "$OUT/selftest.jar"
