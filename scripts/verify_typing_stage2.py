#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def require(condition, message):
    if not condition:
        errors.append(message)

engine = read("app/src/main/java/com/socialaiassistant/keyboard/ime/OfflineBanglaSuggestionEngine.kt")
typing = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
layout = read("app/src/main/res/layout/ime_keyboard.xml")

require("class OfflineBanglaSuggestionEngine" in engine, "offline suggestion engine missing")
require("SuggestionKind.EXACT" in engine and "SuggestionKind.PREFIX" in engine and "SuggestionKind.TYPO" in engine, "candidate ranking sources incomplete")
require("autocorrectText = aliasText" in engine, "autocorrect must stay constrained to explicit alias matches")
require('"amr" to "amar"' in engine and '"kmn" to "kemon"' in engine, "safe shorthand aliases missing")
require("fun currentSuggestions" in typing, "typing engine must expose candidates")
require("fun flushWithAutocorrect" in typing, "typing engine must expose boundary autocorrect")
require('android:id="@+id/suggestion_bar"' in layout, "suggestion bar resource missing")
require("private fun renderSuggestionBar()" in service, "suggestion bar rendering missing")
require("flushPhoneticComposition(autocorrect = currentFieldPolicy.allowAutocorrect)" in service, "Space boundary must use field-aware conservative autocorrect")
require("acceptSuggestion(candidate)" in service and "setOnClickListener" in service, "candidate tap must be user-triggered")

if errors:
    print("TYPING STAGE 2 VERIFICATION: FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("TYPING STAGE 2 VERIFICATION: PASS")
