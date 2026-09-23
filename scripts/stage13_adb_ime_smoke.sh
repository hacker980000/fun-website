#!/usr/bin/env bash
set -euo pipefail
PACKAGE="com.socialaiassistant.keyboard"
IME_ID="${PACKAGE}/.ime.SocialAiInputMethodService"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
MODE="${2:-diagnose}"
command -v adb >/dev/null || { echo "adb not found" >&2; exit 2; }
adb get-state >/dev/null
if ! adb shell pm path "$PACKAGE" | grep -q '^package:'; then
  test -f "$APK" || { echo "APK not installed and not found: $APK" >&2; exit 3; }
  adb install -r "$APK" >/dev/null
fi
echo "Installed package:"; adb shell pm path "$PACKAGE"
echo "Registered IMEs:"; adb shell ime list -s | grep -F "$PACKAGE" || true
if [[ "$MODE" == "apply" ]]; then
  echo "Enabling/selecting test IME (explicit apply mode)."
  adb shell ime enable "$IME_ID"
  adb shell ime set "$IME_ID"
else
  echo "Diagnostic mode only. Re-run with: $0 '$APK' apply"
fi
echo "Input-method state excerpt:"
adb shell dumpsys input_method | grep -E "mCurMethodId|mSelectedMethodId|${PACKAGE}" | head -n 30 || true
