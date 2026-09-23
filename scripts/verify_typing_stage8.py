#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def read(rel):
    p=ROOT/rel
    if not p.exists(): errors.append(f"missing: {rel}"); return ""
    return p.read_text(encoding="utf-8")
def req(c,m):
    if not c: errors.append(m)
settings=read("app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt")
custom=read("app/src/main/java/com/socialaiassistant/keyboard/settings/KeyboardCustomization.kt")
glide=read("app/src/main/java/com/socialaiassistant/keyboard/ime/GlideTypingEngine.kt")
service=read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
settings_activity=read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
typing_xml=read("app/src/main/res/layout/settings_category_typing_suggestions.xml")
appearance_xml=read("app/src/main/res/layout/settings_category_theme_appearance.xml")
english=read("app/src/main/java/com/socialaiassistant/keyboard/ime/EnglishTypingEngine.kt")
bangla=read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt")
for token in ["glideTyping", "oneHandedMode", "toolbarProfile"]: req(token in settings, f"missing Stage 8 setting {token}")
req("enum class OneHandedMode" in custom and "LEFT" in custom and "RIGHT" in custom, "one-handed mode model missing")
req("enum class ToolbarProfile" in custom and "AI_FIRST" in custom and "MINIMAL" in custom, "toolbar profile model missing")
req("class GlideTypingEngine" in glide and "resolveEnglish" in glide and "resolveBangla" in glide, "glide resolver missing")
req("configureGlideKeyButton" in service and ("glideKeyAt" in service or "snapshotGlideHitMap" in service) and "commitGlideSequence" in service, "glide IME touch wiring missing")
req("GLIDE_START_THRESHOLD_DP" in service and "MIN_GLIDE_KEYS" in service, "glide tap-vs-swipe guard missing")
req("applyOneHandedMode" in service and "ONE_HANDED_WIDTH_RATIO" in service, "one-handed IME layout wiring missing")
req("applyToolbarProfile" in service and "toolbarButtons" in service, "toolbar reorder wiring missing")
req("commitGlideWord" in english and "commitGlideWord" in bangla, "glide commit/learning bridge missing")
req("glide_typing_checkbox" in typing_xml and "button_one_hand_left" in appearance_xml and "button_one_hand_right" in appearance_xml, "Stage 8 ergonomics settings UI missing")
req("button_toolbar_ai_first" in typing_xml and "button_toolbar_typing" in typing_xml and "button_toolbar_minimal" in typing_xml, "toolbar profile settings UI missing")
req("setGlideTyping" in settings_activity and "setOneHandedMode" in settings_activity and "setToolbarProfile" in settings_activity, "Stage 8 settings listeners missing")
if errors:
    print("TYPING STAGE 8 VERIFICATION: FAIL")
    for e in errors: print("-", e)
    sys.exit(1)
print("TYPING STAGE 8 VERIFICATION: PASS")
print("- offline English/Bangla phonetic glide foundation wired")
print("- one-handed left/off/right layout wired")
print("- persistent toolbar profile customization wired")
