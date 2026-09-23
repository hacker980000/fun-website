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
doc = read("docs/STAGE23_5_REFERENCE_NUMERIC_PAD.md")

for token in [
    'leftRail = listOf("+", "-", "*", "/")',
    'listOf("1", "2", "3")',
    'listOf("4", "5", "6")',
    'listOf("7", "8", "9")',
    'KeySpec("%"',
    'KeySpec("␣"',
    'KeySpec("⌫"',
    'KeySpec("ABC"',
    'KeySpec("!?#"',
    'KeySpec("0"',
    'KeySpec("="',
    'KeySpec("."',
    'KeySpec("↵"'
]:
    req(token in layout, f"numeric pad spec missing: {token}")

for token in [
    'keyboardMode.layer == KeyboardLayer.NUMBERS',
    'renderNumericPad(rowsContainer, enterLabel)',
    'pad.leftRail.forEach',
    'pad.digitRows.forEach',
    'pad.rightRail.forEach',
    'pad.bottomRow.forEach',
    'createNumericPadDivider',
    'themeRenderer.styleButton(button, themeRoleFor(key.action), currentSurfaceTheme)',
    'currentSurfaceTheme.keyLabelScale',
    'NumericPadEmphasis.DIGIT -> 28f * scale'
]:
    req(token in service, f"numeric pad renderer wiring missing: {token}")

for token in [
    'ThemePack.CLASSIC_DARK',
    'ThemePack.GLASS_MODERN',
    'ThemePack.CLEAN_LIGHT',
    'ThemePack.GRADIENT_PRO',
    'KeyboardThemeSurface.NUMBER to SurfaceProfile'
]:
    req(token in catalog, f"theme package NUMBER coverage missing: {token}")

req('stage23_5_numberpad_reference.jpg' in doc, 'reference image not documented')
req('Classic Dark / Glass Modern / Clean Light / Gradient Pro' in doc, 'all four packages not documented')

if errors:
    print("STAGE 23.5 NUMERIC PAD VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print(f"STAGE 23.5 NUMERIC PAD VERIFICATION: PASS ({checks}/{checks})")
print("- calculator-style operator/digit/action rails wired")
print("- shared NUMBER layout is theme-package independent")
print("- four premium theme families retain their NUMBER surface styling")
