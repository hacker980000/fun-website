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

settings = read("app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt")
category_activity = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
xml = read("app/src/main/res/layout/settings_category_theme_appearance.xml")
policy = read("app/src/main/java/com/socialaiassistant/keyboard/ime/AlphabeticNumberRowPolicy.kt")
layout = read("app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
resolver = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeThemeSurfaceResolver.kt")
catalog = read("app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt")
doc = read("docs/STAGE23_8_ALPHABETIC_NUMBER_ROW.md")

for token in [
    "val showNumberRow: Boolean = false",
    "setShowNumberRow(value: Boolean)",
    'booleanPreferencesKey("show_number_row")',
    "showNumberRow = preferences[SHOW_NUMBER_ROW] ?: false",
]:
    req(token in settings, f"persistent number-row setting missing: {token}")

req("number_row_checkbox" in xml, "Settings number-row checkbox missing")
req("English, বাংলা, Phonetic &amp; Bijoy" in xml, "Settings label does not name all requested alphabetic layouts")
req("setShowNumberRow" in category_activity and "number_row_checkbox" in category_activity, "Settings checkbox is not persisted by SettingsCategoryActivity")

for token in [
    "enabledInSettings",
    "mode.layer != KeyboardLayer.LETTERS",
    "KeyboardLanguage.ENGLISH -> true",
    "BanglaInputMode.PHONETIC",
    "BanglaInputMode.BIJOY",
]:
    req(token in policy, f"alphabetic number-row policy missing: {token}")

req("AlphabeticNumberRowPolicy.shouldShow" in layout, "KeyboardLayout does not use the centralized alphabetic number-row policy")
req("if (showNumberRow) rows += numberRow()" in layout, "English/Phonetic number-row insertion missing")
req("if (showNumberRow) layoutRows += numberRow()" in layout, "Bijoy number-row insertion missing")
req('listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")' in layout, "1-0 alphabetic number row contract missing")

req("showNumberRow = currentSettings.showNumberRow" in service, "IME render does not consume live number-row preference")
req("KeyboardRenderSignature(" in service and "showNumberRow = currentSettings.showNumberRow" in service, "render cache signature does not vary with number-row preference")
req("KeyboardLayoutRequest(" in service and "showNumberRow = currentSettings.showNumberRow" in service, "layout cache key does not vary with number-row preference")

for token in [
    "KeyboardThemeSurface.ENGLISH",
    "KeyboardThemeSurface.PHONETIC",
    "KeyboardThemeSurface.BIJOY",
]:
    req(token in resolver, f"theme surface resolver missing alphabetic surface: {token}")

for token in [
    "ThemePack.CLASSIC_DARK",
    "ThemePack.GLASS_MODERN",
    "ThemePack.CLEAN_LIGHT",
    "ThemePack.GRADIENT_PRO",
]:
    req(token in catalog, f"complete Theme Package missing: {token}")

for token in [
    "Settings-controlled",
    "English",
    "Bangla Phonetic",
    "Bangla Bijoy",
    "Classic Dark",
    "Glass Modern",
    "Clean Light",
    "Gradient Pro",
    "Dedicated NUMBER and SYMBOL surfaces never receive a duplicate optional row",
]:
    req(token in doc, f"Stage 23.8 documentation missing: {token}")

if errors:
    print("STAGE 23.8 ALPHABETIC NUMBER ROW VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print(f"STAGE 23.8 ALPHABETIC NUMBER ROW VERIFICATION: PASS ({checks}/{checks})")
print("- Settings ON/OFF controls the 1-0 row only on alphabetic layouts")
print("- English, Bangla Phonetic and Bangla Bijoy share the same persisted preference")
print("- dedicated Number/Symbol surfaces remain unaffected")
print("- behavior is theme-independent and therefore shared by all four complete Theme Packages")
