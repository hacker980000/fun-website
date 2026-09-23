#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt" \
  "$ROOT/scripts/settings_navigation_stage24_3_selftest.kt" \
  -include-runtime -d "$TMP/stage24_3_settings_category_selftest.jar"
java -jar "$TMP/stage24_3_settings_category_selftest.jar"
