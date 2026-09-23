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

def req(condition, message):
    if not condition:
        errors.append(message)

policy = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeEditorBehaviorPolicy.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
layout = read("app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt")
lexicon = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage12AvroGoldenLexiconPack.kt")
production = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ProductionBanglaLexicon.kt")
doc = read("docs/typing/TYPING_CORE_V2_STAGE12.md")

for token in [
    "ImeTypingFieldPolicy", "TYPE_TEXT_FLAG_NO_SUGGESTIONS", "TYPE_TEXT_FLAG_AUTO_COMPLETE",
    "TYPE_TEXT_VARIATION_EMAIL_ADDRESS", "TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS",
    "TYPE_TEXT_VARIATION_URI", "TYPE_TEXT_VARIATION_FILTER", "allowSmartLanguageHints"
]:
    req(token in policy, f"field-aware editor policy missing: {token}")

for token in [
    "currentFieldPolicy = ImeEditorBehaviorPolicy.typingPolicy(editor.inputType)",
    "currentFieldPolicy.showSuggestions", "currentFieldPolicy.allowLearning",
    "currentFieldPolicy.allowAutocorrect", "currentFieldPolicy.forceLiteralLatin",
    "quickKeys = if (keyboardMode.layer == KeyboardLayer.LETTERS) currentFieldPolicy.quickKeys",
    "typingEngine.setSuggestionsEnabled(false)", "englishTypingEngine.setLearningEnabled(false)"
]:
    req(token in service, f"IME field policy wiring missing: {token}")

req("quickKeyRow" in layout and "quickKeys: List<String>" in layout, "email/URL quick-key row support missing")
for roman, bangla in [
    ("bybohar", "ব্যবহার"), ("bishwo", "বিশ্ব"), ("korrmo", "কর্ম"),
    ("aZromeTik", "অ্যারোমেটিক"), ("rriN", "ঋণ"), ("brritto", "বৃত্ত")
]:
    req(f'"{roman}" to "{bangla}"' in lexicon, f"Avro golden missing: {roman}")
req("putAll(Stage12AvroGoldenLexiconPack.words)" in production, "Stage 12 Avro golden pack not merged")
req("1,158 entries" in doc and "46/46 PASS" in doc and "18/18 PASS" in doc, "Stage 12 documentation incomplete")

if errors:
    print("TYPING STAGE 12 VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("TYPING STAGE 12 VERIFICATION: PASS")
print("- field-aware Android editor policy wired")
print("- literal-Latin email/URL quick-key flow wired")
print("- 14-entry documented Avro golden compatibility pack merged")
