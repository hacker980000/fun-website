#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/settings/KeyboardCustomization.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBubbleAppearance.kt" \
  "$ROOT/scripts/typing_stage24_2_theme_bubble_selftest.kt" \
  -include-runtime -d "$TMP/stage24_2_theme_bubble_selftest.jar"
java -jar "$TMP/stage24_2_theme_bubble_selftest.jar"
