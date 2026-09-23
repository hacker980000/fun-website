#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/com/socialaiassistant/keyboard/settings" "$TMP/com/socialaiassistant/keyboard/theme"
cat > "$TMP/com/socialaiassistant/keyboard/settings/BubbleKeyIntensity.kt" <<'KT'
package com.socialaiassistant.keyboard.settings
enum class BubbleKeyIntensity { SOFT, NORMAL, PLAYFUL }
KT
cat > "$TMP/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt" <<'KT'
package com.socialaiassistant.keyboard.theme
class KeyboardTheme
KT
kotlinc \
  "$TMP/com/socialaiassistant/keyboard/settings/BubbleKeyIntensity.kt" \
  "$TMP/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightModels.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolver.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt" \
  "$ROOT/scripts/typing_stage24_1_bubble_flight_selftest.kt" \
  -include-runtime -d "$TMP/stage24_1_bubble_flight_selftest.jar"
java -jar "$TMP/stage24_1_bubble_flight_selftest.jar"


kotlinc \
  "$TMP/com/socialaiassistant/keyboard/settings/BubbleKeyIntensity.kt" \
  "$TMP/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightModels.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightExactTargetRefresh.kt" \
  "$ROOT/scripts/bubble_flight_dispatch_refresh_selftest.kt" \
  -include-runtime -d "$TMP/stage24_1_dispatch_refresh_selftest.jar"
java -jar "$TMP/stage24_1_dispatch_refresh_selftest.jar"
