#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/backend/BackendConfig.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/network/NetworkEndpointPolicy.kt" \
  "$ROOT/scripts/network_policy_stage25_6_selftest.kt" \
  -include-runtime -d "$TMP/network-policy.jar"
java -jar "$TMP/network-policy.jar"
