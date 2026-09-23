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

engine = read("app/src/main/java/com/socialaiassistant/keyboard/ime/EnglishTypingEngine.kt")
persistent = read("app/src/main/java/com/socialaiassistant/keyboard/ime/PersistentEnglishTypingLearningModel.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
settings = read("app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt")
settings_activity = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
layout = read("app/src/main/res/layout/settings_category_language_input.xml")

for token in ["ProductionEnglishLexicon", "OfflineEnglishSuggestionEngine", "OfflineEnglishNextWordModel", "EnglishTypingEngine", "SmartLanguageBridge"]:
    req(token in engine, f"missing English typing component: {token}")
req("EnglishConversationCorpus" in engine and "englishDictionary" not in engine, "English corpus/lexicon foundation missing")
req("aliases" in engine and '"dont" to "don\'t"' in engine, "conservative English alias autocorrect missing")
req("conservativeEnglishEditDistance" in engine, "English typo correction foundation missing")
req("MAX_WORDS = 512" in persistent and "MAX_TRANSITIONS = 1_024" in persistent, "bounded persistent English learning missing")
req("englishSuggestions" in settings and "englishAutocorrect" in settings and "smartLanguageHints" in settings, "Stage 7 settings missing")
req("PersistentEnglishTypingLearningModel" in service and "englishTypingEngine" in service, "IME English engine wiring missing")
req("banglaHintFromEnglish" in service and "englishHintFromBanglaPhonetic" in service, "cross-language hint wiring missing")
req("candidate.targetLanguage != candidate.sourceLanguage" in service and "showKeys()" in service, "tap-confirmed language switching missing")
req("isEnglishTypingMode" in service and "FieldSafety.BLOCK_AI" in service, "sensitive-field English intelligence guard missing")
req("isLatinWordCharacter" in service and "flushPhoneticComposition(autocorrect = currentFieldPolicy.allowAutocorrect)" in service, "number-row/non-letter composition boundary guard missing")
req("english_suggestions_checkbox" in layout and "english_autocorrect_checkbox" in layout and "smart_language_hints_checkbox" in layout, "Stage 7 settings UI missing")
req("PersistentEnglishTypingLearningModel.clearStoredLearning" in settings_activity, "Clear Learned Typing Data must clear English store too")
req("tap to switch" in layout.lower(), "UI must state that smart language hints are user-confirmed")

if errors:
    print("TYPING STAGE 7 VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("TYPING STAGE 7 VERIFICATION: PASS")
print("- offline English suggestions/autocorrect/next-word engine wired")
print("- bounded persistent English learning wired")
print("- Bangla/English cross-language hints require explicit candidate tap")
print("- sensitive-field and number-row composition guards present")
