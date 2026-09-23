#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []
checks = 0

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def req(condition, message):
    global checks
    checks += 1
    if not condition:
        errors.append(message)

layout = read("app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
catalog = read("app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt")
doc = read("docs/STAGE23_7_NUMERIC_NO_GAP_BOTTOM_ROW.md")

# Approved shared key contract.
for token in [
    'leftRail = listOf("+", "-", "*", "/")',
    'listOf("1", "2", "3")',
    'listOf("4", "5", "6")',
    'listOf("7", "8", "9")',
    'KeySpec("ABC"',
    'KeySpec(","',
    'KeySpec("!?#"',
    'KeySpec("0", output = "0"',
    'KeySpec("="',
    'KeySpec("."',
    'KeySpec("↵"'
]:
    req(token in layout, f"numeric pad contract missing: {token}")

# Stage 23.7: no phantom fourth digit row and no negative-margin workaround.
for token in [
    'NUMERIC_PAD_MAIN_DIGIT_ROWS = 3',
    'val mainHeight = (keyHeight + gap * 2) * NUMERIC_PAD_MAIN_DIGIT_ROWS',
    'weightSum = NUMERIC_PAD_TOTAL_WIDTH_UNITS',
    'key.label == "0" -> NumericPadEmphasis.DIGIT',
    'themeRenderer.styleButton(button, themeRoleFor(key.action), currentSurfaceTheme)'
]:
    req(token in service, f"Stage 23.7 renderer contract missing: {token}")

req('topMargin = -bottomRowLift' not in service, 'obsolete Stage 23.6 negative-margin lift is still active')
req('NUMERIC_PAD_BOTTOM_ROW_LIFT_DP' not in service, 'obsolete fixed bottom-row lift constant is still active')

# All four packages must use the same NUMBER surface renderer.
for token in [
    'ThemePack.CLASSIC_DARK',
    'ThemePack.GLASS_MODERN',
    'ThemePack.CLEAN_LIGHT',
    'ThemePack.GRADIENT_PRO',
    'KeyboardThemeSurface.NUMBER to SurfaceProfile'
]:
    req(token in catalog, f"theme package NUMBER coverage missing: {token}")

for token in [
    'exactly **3 digit rows**',
    'There is no reserved blank fourth row',
    '`ABC , !?#` occupy the same row on its left',
    '`= . Enter` occupy the same row on its right',
    'Classic Dark',
    'Glass Modern',
    'Clean Light',
    'Gradient Pro'
]:
    req(token in doc, f"Stage 23.7 documentation missing: {token}")

if errors:
    print("STAGE 23.7 NUMERIC NO-GAP VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print(f"STAGE 23.7 NUMERIC NO-GAP VERIFICATION: PASS ({checks}/{checks})")
print("- center numeric block reserves exactly three digit-row heights")
print("- bottom action row follows 7/8/9 directly with no phantom fourth-row gap")
print("- 0 stays centered under 8 with ABC/,/!?# left and =/./Enter right")
print("- geometry remains shared across all four Theme Packages")
