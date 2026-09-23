#!/usr/bin/env bash
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
fail=0
warn=0

ok() { printf 'PASS: %s\n' "$1"; }
no() { printf 'FAIL: %s\n' "$1"; fail=$((fail+1)); }
warning() { printf 'WARN: %s\n' "$1"; warn=$((warn+1)); }

if command -v java >/dev/null 2>&1; then
  first="$(java -version 2>&1 | head -n1)"
  major="$(printf '%s' "$first" | sed -E 's/.*version "([0-9]+).*/\1/')"
  if [[ "$major" =~ ^[0-9]+$ ]] && (( major >= 17 )); then
    ok "JDK detected ($first)"
    if [[ "$major" != "17" ]]; then
      warning "CI uses JDK 17; use JDK 17 locally for closest reproducibility."
    fi
  else
    no "JDK 17+ is required."
  fi
else
  no "Java not found; install JDK 17."
fi

if python3 scripts/check_gradle_wrapper_completeness.py; then
  ok "Gradle wrapper is complete and checksum-verified."
else
  no "Gradle wrapper JAR is missing or invalid. Run scripts/bootstrap_gradle_wrapper.sh first."
fi

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -n "$SDK_ROOT" && -d "$SDK_ROOT" ]]; then
  ok "Android SDK root: $SDK_ROOT"
  [[ -d "$SDK_ROOT/platforms/android-36" ]] && ok "Android platform 36 installed." || no "Install platforms;android-36."
  [[ -d "$SDK_ROOT/build-tools/36.0.0" ]] && ok "Android Build Tools 36.0.0 installed." || no "Install build-tools;36.0.0."
else
  no "ANDROID_SDK_ROOT/ANDROID_HOME is not configured."
fi

if python3 scripts/verify_release_ready.py >/dev/null; then
  ok "Release contract verifier passed."
else
  no "Release contract verifier failed."
fi

verifier_fail=0
key_verifiers=(
  scripts/verify_release_ready.py
  scripts/verify_network_release_stage25_6.py
  scripts/verify_webview_security_stage25_5.py
  scripts/verify_conversation_context_stage25_4.py
  scripts/verify_sensitive_field_stage25_3.py
  scripts/verify_settings_navigation_stage24_3.py
  scripts/verify_typing_stage23.py
)
for f in "${key_verifiers[@]}"; do
  if ! python3 "$f" >/dev/null 2>&1; then
    printf '  verifier failed: %s\n' "$f"
    verifier_fail=$((verifier_fail+1))
  fi
done
if (( verifier_fail == 0 )); then
  ok "Core source-contract verifier set passed (${#key_verifiers[@]} checks)."
else
  no "$verifier_fail core source-contract verifier(s) failed."
fi

if [[ "${FULL_VERIFY:-0}" == "1" ]]; then
  full_fail=0
  full_count=0
  for f in scripts/verify*.py; do
    [[ -f "$f" ]] || continue
    full_count=$((full_count+1))
    if ! python3 "$f" >/dev/null 2>&1; then
      printf '  full verifier failed: %s\n' "$f"
      full_fail=$((full_fail+1))
    fi
  done
  if (( full_fail == 0 )); then
    ok "Full verifier sweep passed ($full_count/$full_count)."
  else
    no "$full_fail of $full_count full verifiers failed."
  fi
else
  warning "Set FULL_VERIFY=1 to run the complete verify*.py sweep."
fi

printf '\nPreflight summary: %d failure(s), %d warning(s).\n' "$fail" "$warn"
if (( fail == 0 )); then
  printf 'Ready for canonical validation:\n'
  printf '  ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug\n'
  printf '  ./gradlew --no-daemon lintRelease bundleRelease   # signing properties required for a signed release\n'
  exit 0
fi
exit 1
