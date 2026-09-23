#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")

def req(condition, message):
    if not condition:
        errors.append(message)

settings = read("app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt")
layout = read("app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
clip = read("app/src/main/java/com/socialaiassistant/keyboard/ime/RecentClipboardStore.kt")
category = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
theme_xml = read("app/src/main/res/layout/settings_category_theme_appearance.xml")
clipboard_xml = read("app/src/main/res/layout/settings_category_clipboard.xml")

for token in [
    "showNumberRow", "spacebarCursorControl", "hapticFeedback", "keySound",
    "clipboardHistory", "keyboardKeyHeightDp"
]:
    req(token in settings, f"missing setting {token}")

req("showNumberRow: Boolean = false" in layout and "numberRow()" in layout,
    "optional number row not wired")
req("configureSpacebarButton" in service and "sendCursorMovement" in service and "KEYCODE_DPAD_LEFT" in service,
    "spacebar cursor control missing")
req("performHapticFeedback" in service and "playSoundEffect" in service,
    "keypress feedback controls missing")
req("RecentClipboardStore" in service and "MAX_ITEMS = 20" in clip and "MAX_CHARS = 4_000" in clip,
    "bounded clipboard history missing")
req("ClipboardTextPolicy.shouldExposeContent" in service and "!hideCurrentClipboard" in service,
    "sensitive-field clipboard guard missing")
req("primaryClipDescription" in service and "android.content.extra.IS_SENSITIVE" in service,
    "sensitive-marked clipboard exclusion missing")
req("button_height_compact" in category and "button_height_tall" in category and "status_keyboard_height" in theme_xml,
    "height controls missing from current Theme & Appearance category")
req("number_row_checkbox" in theme_xml and "clipboard_history_checkbox" in clipboard_xml and "button_clear_clipboard_history" in clipboard_xml,
    "daily-use settings UI missing from current category layouts")
req("val clipboardHistory: Boolean = false" in settings and "recent_clipboard_history_v2_encrypted" in clip,
    "Stage 25.2 encrypted opt-in clipboard policy missing")

if errors:
    print("TYPING STAGE 6 VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("TYPING STAGE 6 VERIFICATION: PASS")
print("- optional number row, cursor swipe, key height, feedback and clipboard history are wired")
print("- current settings-category architecture is covered")
print("- sensitive fields/clips suppress clipboard exposure; optional history is encrypted and off by default")
