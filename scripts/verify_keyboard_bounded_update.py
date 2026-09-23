#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def require(cond, msg):
    if not cond:
        errors.append(msg)

service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
controller = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ExplicitAiTriggerController.kt")
smart = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SmartReplyController.kt")
payload = read("app/src/main/java/com/socialaiassistant/keyboard/backend/ManagedAiPayload.kt")
prompt = read("app/src/main/java/com/socialaiassistant/keyboard/ai/ExtensionPromptBuilder.kt")
caption = read("app/src/main/java/com/socialaiassistant/keyboard/CaptionActivity.kt")
main_layout = read("app/src/main/res/layout/activity_main.xml")
about_layout = read("app/src/main/res/layout/settings_category_help_about.xml")
settings_category = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
strings = read("app/src/main/res/values/strings.xml")

require("requestAuto()" not in service, "toolbar/service must not call ReplyOrchestrator.requestAuto()")
require("requestAuto()" not in controller, "ExplicitAiTriggerController must not invoke requestAuto()")
idle_match = re.search(r"ManualAiState\.Idle\s*->\s*(Unit|\{(.*?)\n\s*\})", service, re.S)
require(idle_match is not None and (idle_match.group(1) == "Unit" or "target.addView(status)" not in (idle_match.group(2) or "")),
        "AI panel Idle state must not render the instructional status block")
require("ReplyState.WaitingForContext -> SmartReplyUiState.Hidden" in smart,
        "Waiting-for-context smart reply notice must stay hidden")
require("configureBackspaceButton" in service and "MotionEvent.ACTION_DOWN" in service and "postDelayed" in service and "removeCallbacks" in service,
        "backspace must support press-and-hold repeat with cancellation")
require("developer_name" in about_layout and "developer_name" in strings,
        "Help & About category must show developer name")
require("developer_email" in about_layout and "developer_email" in strings,
        "Help & About category must show developer email")
require(("developer_phone" in about_layout or "developer_whatsapp" in about_layout) and
        ("developer_phone" in strings or "developer_whatsapp" in strings),
        "Help & About category must show phone/WhatsApp")
require("Help &amp; About" in about_layout and "bindHelpAbout" in settings_category,
        "Settings must contain and wire the dedicated Help & About category")
require('LanguageMode.BANGLISH -> "BANGLISH"' in payload and 'LanguageMode.LATIN_INFER -> "ENGLISH"' in payload,
        "managed comment payload must preserve Banglish and English language modes")
require("Bengali-script source -> Bengali-script comment" in prompt and
        "Banglish source -> Banglish comment" in prompt and
        "English source -> English comment" in prompt,
        "comment prompt must explicitly lock Bengali/Banglish/English output language")
require('LanguageMode.BANGLISH -> "BANGLISH"' in caption and 'LanguageMode.LATIN_INFER -> "LATIN_INFER"' in caption,
        "caption language routing must preserve Banglish/Latin input style")
require("setOnClickListener { requestManualAiAction(action) }" in service,
        "AI sub-actions must remain explicit per-button generation triggers")
engine = read("app/src/main/java/com/socialaiassistant/keyboard/ai/ManualAiActionEngine.kt")
require("activeCommentText" in engine and "postText = activeCommentText" in engine,
        "managed comments must route the latest active comment/post text for language detection")

if errors:
    print("BOUNDED UPDATE VERIFICATION: FAIL")
    for e in errors:
        print(f"- {e}")
    sys.exit(1)
print("BOUNDED UPDATE VERIFICATION: PASS")
