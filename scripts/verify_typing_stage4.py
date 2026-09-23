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

next_word = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaNextWordModel.kt")
persistent = read("app/src/main/java/com/socialaiassistant/keyboard/ime/PersistentTypingLearningModel.kt")
suggestion = read("app/src/main/java/com/socialaiassistant/keyboard/ime/OfflineBanglaSuggestionEngine.kt")
typing = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
settings = read("app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt")
settings_activity = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
layout = read("app/src/main/res/layout/settings_category_typing_suggestions.xml")
privacy = read("docs/play-store/privacy-policy.md")

require("class PersistentTypingLearningModel" in persistent, "persistent typing learning model missing")
require("SharedPreferences" in persistent and "MAX_WORDS = 512" in persistent and "MAX_TRANSITIONS = 1_024" in persistent, "bounded local learning store contract missing")
require("KEY_PHONETICS" in persistent and "clearStoredLearning" in persistent, "phonetic persistence or clear control missing")
require("transitionCandidates" in next_word, "learned-only next-word candidate source missing")
require("SuggestionKind.PERSONAL" in suggestion and "trustedPhoneticText" in suggestion, "personal candidate ranking/autocorrect foundation missing")
require("learning.recordPhonetic" in typing and "weight = 3" in typing, "explicit suggestion acceptance must strengthen phonetic learning")
require("setSuggestionsEnabled" in typing, "suggestion visibility must be independent from personal learning preference")
require("PersistentTypingLearningModel(applicationContext)" in service, "IME must use persistent local typing learning")
require("currentFieldPolicy.allowLearning" in service and "currentSettings.personalTypingLearning" in service, "learning must respect sensitive/editor-field and user preference gates")
require("personalTypingLearning" in settings and "setPersonalTypingLearning" in settings, "typing learning setting missing")
require("personal_typing_learning_checkbox" in layout and "button_clear_typing_learning" in layout, "typing learning user controls missing")
require("PersistentTypingLearningModel.clearStoredLearning" in settings_activity and "PersistentEnglishTypingLearningModel.clearStoredLearning" in settings_activity, "Settings clear action must erase local Bangla and English typing learning")
require("not uploaded" in privacy.lower() and "typing learning" in privacy.lower(), "privacy policy must disclose device-local typing learning")

if errors:
    print("TYPING STAGE 4 VERIFICATION: FAIL")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("TYPING STAGE 4 VERIFICATION: PASS")
print("- bounded persistent local word/bigram/phonetic learning present")
print("- personal candidate and learned-only next-word ranking present")
print("- user disable/clear controls present")
print("- sensitive-field learning gate retained")
