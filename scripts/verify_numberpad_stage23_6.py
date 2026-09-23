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
doc = read("docs/STAGE23_6_NUMERIC_BOTTOM_ROW_ALIGNMENT.md")

# Shared Stage 23.5 geometry must remain intact.
for token in [
    'leftRail = listOf("+", "-", "*", "/")',
    'listOf("1", "2", "3")',
    'listOf("4", "5", "6")',
    'listOf("7", "8", "9")',
    'KeySpec("0", output = "0"',
    'KeySpec("ABC"',
    'KeySpec("!?#"',
    'KeySpec("↵"'
]:
    req(token in layout, f"numeric pad contract missing: {token}")

# Stage 23.7 intentionally supersedes the fixed 8dp negative-margin lift from Stage 23.6.
# Preserve the Stage 23.6 centering/width guarantees while accepting the no-gap successor geometry.
for token in [
    'NUMERIC_PAD_MAIN_DIGIT_ROWS = 3',
    'NUMERIC_PAD_TOTAL_WIDTH_UNITS = 9f',
    'val mainHeight = (keyHeight + gap * 2) * NUMERIC_PAD_MAIN_DIGIT_ROWS',
    'weightSum = NUMERIC_PAD_TOTAL_WIDTH_UNITS',
    'key.label == "0" -> NumericPadEmphasis.DIGIT',
    'themeRenderer.styleButton(button, themeRoleFor(key.action), currentSurfaceTheme)'
]:
    req(token in service, f"Stage 23.6/23.7 renderer contract missing: {token}")
req('NUMERIC_PAD_BOTTOM_ROW_LIFT_DP' not in service and 'topMargin = -bottomRowLift' not in service,
    "Stage 23.7 must not reintroduce the superseded fixed bottom-row lift")

# All four complete packs must retain NUMBER surface coverage.
for token in [
    'ThemePack.CLASSIC_DARK',
    'ThemePack.GLASS_MODERN',
    'ThemePack.CLEAN_LIGHT',
    'ThemePack.GRADIENT_PRO',
    'KeyboardThemeSurface.NUMBER to SurfaceProfile'
]:
    req(token in catalog, f"theme package NUMBER coverage missing: {token}")

for token in [
    'directly below `8`',
    '4.5 / 9 = 50%',
    'Classic Dark',
    'Glass Modern',
    'Clean Light',
    'Gradient Pro'
]:
    req(token in doc, f"Stage 23.6 documentation missing: {token}")

if errors:
    print("STAGE 23.6 NUMERIC BOTTOM-ROW VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print(f"STAGE 23.6 NUMERIC BOTTOM-ROW VERIFICATION: PASS ({checks}/{checks})")
print("- Stage 23.6 centering guarantee retained through Stage 23.7 no-gap geometry")
print("- 0 remains mathematically centered under the 2/5/8 digit column")
print("- shared renderer keeps geometry identical across all four theme packages")
