#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/social_ai_stage25_4_context"
rm -rf "$OUT"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/ConversationSurface.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/ContextModels.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/ConversationHintResolver.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/ConversationKeyFactory.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/ConversationSenderClassifier.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/ConversationNodeOrdering.kt" \
  "$ROOT/app/src/main/java/com/socialaiassistant/keyboard/context/GenericConversationAdapter.kt" \
  "$ROOT/scripts/conversation_context_stage25_4_selftest.kt" \
  -include-runtime -d "$OUT/selftest.jar"
java -jar "$OUT/selftest.jar"
