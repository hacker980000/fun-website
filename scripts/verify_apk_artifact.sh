#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
EXPECTED_PACKAGE="com.socialaiassistant.keyboard"
EXPECTED_VERSION_CODE="350301"
EXPECTED_VERSION_NAME="35.3.1"

if [[ ! -s "$APK" ]]; then
  echo "ERROR: APK not found or empty: $APK" >&2
  exit 2
fi

AAPT=""
if [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/build-tools/36.0.0/aapt" ]]; then
  AAPT="${ANDROID_HOME}/build-tools/36.0.0/aapt"
elif command -v aapt >/dev/null 2>&1; then
  AAPT="$(command -v aapt)"
else
  echo "ERROR: aapt not found; install Android build-tools 36.0.0." >&2
  exit 3
fi

BADGING="$($AAPT dump badging "$APK")"
echo "$BADGING" | grep -F "package: name='$EXPECTED_PACKAGE' versionCode='$EXPECTED_VERSION_CODE' versionName='$EXPECTED_VERSION_NAME'" >/dev/null
echo "$BADGING" | grep -F "sdkVersion:'26'" >/dev/null
echo "$BADGING" | grep -F "targetSdkVersion:'36'" >/dev/null

XMLTREE="$($AAPT dump xmltree "$APK" AndroidManifest.xml)"
echo "$XMLTREE" | grep -F "android.permission.BIND_INPUT_METHOD" >/dev/null
echo "$XMLTREE" | grep -F "SocialAiInputMethodService" >/dev/null

SHA="$(sha256sum "$APK" | awk '{print $1}')"
SIZE="$(wc -c < "$APK")"
if (( SIZE < 100000 )); then
  echo "ERROR: APK unexpectedly small: $SIZE bytes" >&2
  exit 4
fi

echo "APK ARTIFACT VERIFY: PASS"
echo "apk=$APK bytes=$SIZE sha256=$SHA"
