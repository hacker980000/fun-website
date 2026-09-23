#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

checks = []
def check(name, condition):
    checks.append((name, bool(condition)))

pack = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemePack.kt').read_text()
repo = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeRepository.kt').read_text()
renderer = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeDashboardRenderer.kt').read_text()
styler = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt').read_text()
preview = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemePreviewView.kt').read_text()
onboarding = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/SettingsThemeOnboardingActivity.kt').read_text()
keyboard_onboarding = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ThemeOnboardingActivity.kt').read_text()
main = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt').read_text()
category_activity = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt').read_text()
appearance_layout = (ROOT / 'app/src/main/res/layout/settings_category_theme_appearance.xml').read_text()
layout = (ROOT / 'app/src/main/res/layout/activity_main.xml').read_text()
manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()

for token in ['CLEAN_MODERN', 'CARD_STYLE', 'PREMIUM', 'PRO_STYLE']:
    check(f'pack {token}', token in pack)
for token in ['Classic Dark', 'Glass Modern', 'Clean Light', 'Gradient Pro']:
    check(f'recommendation mapping mentions {token}', token.upper().replace(' ', '_') in pack or token in pack)
check('separate datastore', 'social_ai_settings_theme' in repo)
check('pack persisted', 'settings_theme_pack_id' in repo)
check('setup flag persisted', 'settings_theme_setup_complete' in repo)
check('initial setup method', 'completeInitialSetup' in repo)
check('manual set method', 'setPack' in repo)
check('clean renderer', 'renderCleanModern' in renderer)
check('card renderer', 'renderCardStyle' in renderer)
check('premium renderer', 'renderPremium' in renderer)
check('pro renderer', 'renderProStyle' in renderer)
check('complete-panel styler', 'fun apply(' in styler and 'sections:' in styler)
check('preview renders all packs', all(x in preview for x in ['drawClean', 'drawCards', 'drawPremium', 'drawPro']))
check('second setup activity', 'SettingsThemeOnboardingActivity' in keyboard_onboarding)
check('keyboard setup flows to settings setup', 'openSettingsThemeSetup' in keyboard_onboarding)
check('recommendation shown', 'recommendedFor' in onboarding and 'RECOMMENDED' in onboarding)
check('manual mode supported', 'EXTRA_MANUAL_CHANGE' in onboarding)
check('theme appearance has theme change button', 'button_settings_theme' in appearance_layout and 'button_settings_theme' in category_activity)
check('main applies settings pack', 'applySettingsTheme' in main)
check('dashboard navigation active', 'openSettingsCategory' in main)
check('all dedicated sections styled', 'settingsThemeStyler.applyCategory' in category_activity and all((ROOT / f'app/src/main/res/layout/settings_category_{name}.xml').exists() for name in ['keyboard_setup','language_input','theme_appearance','typing_suggestions','ai_privacy','clipboard','account_subscription','help_about']))
check('settings dashboard container in XML', 'settings_theme_dashboard_container' in layout)
check('settings theme status in XML', 'status_settings_theme' in layout)
check('settings theme activity in manifest', '.SettingsThemeOnboardingActivity' in manifest)
check('reference archived', (ROOT / 'docs/design-reference/stage23_9_settings_theme_reference.jpg').exists())
check('documentation present', (ROOT / 'docs/STAGE23_9_SETTINGS_THEME_PACKS.md').exists())

# Validate every Android XML file parses.
for xml in (ROOT / 'app/src/main/res').rglob('*.xml'):
    try:
        ET.parse(xml)
        ok = True
    except ET.ParseError:
        ok = False
    check(f'xml parses: {xml.relative_to(ROOT)}', ok)
try:
    ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
    ok = True
except ET.ParseError:
    ok = False
check('manifest parses', ok)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + ' - ' + name)
print(f'\n{len(checks)-len(failed)}/{len(checks)} PASS')
if failed:
    raise SystemExit(1)
