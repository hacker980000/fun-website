#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    path = ROOT / rel
    return path.read_text() if path.exists() else ''

appearance = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBubbleAppearance.kt')
repo = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt')
activity = read('app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt')
settings_category = read('app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt')
ime = read('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt')
strings = read('app/src/main/res/values/strings.xml')
settings = read('app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt')
application = read('app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt')

checks = []
def check(name, ok):
    checks.append((name, bool(ok)))

check('theme bubble appearance model exists', 'data class ThemeBubbleAppearance' in appearance)
check('theme bubble defaults off', 'enabled: Boolean = false' in appearance)
check('theme bubble default intensity normal', 'BubbleKeyIntensity.NORMAL' in appearance)
check('enabled mutation preserves style', 'withEnabled' in appearance and 'copy(enabled = enabled)' in appearance)
check('style mutation preserves enabled', 'withIntensity' in appearance and 'copy(intensity = intensity)' in appearance)

check('repository exposes per-pack bubble flow', 'bubbleAppearanceSettings' in repo and 'Map<ThemePack, ThemeBubbleAppearance>' in repo)
check('repository exposes current bubble appearance', 'currentBubbleAppearance(pack: ThemePack)' in repo)
check('repository persists bubble enabled per pack', 'setBubbleEnabled(pack: ThemePack, enabled: Boolean)' in repo)
check('repository persists bubble style per pack', 'setBubbleIntensity(pack: ThemePack, intensity: BubbleKeyIntensity)' in repo)
check('repository enabled key is namespaced by pack', 'theme_${pack.storedId}_bubble_enabled' in repo)
check('repository intensity key is namespaced by pack', 'theme_${pack.storedId}_bubble_intensity' in repo)
check('repository can seed legacy bubble values only when missing', 'seedBubbleAppearanceIfMissing' in repo and 'preferences[enabledKey] == null' in repo and 'preferences[intensityKey] == null' in repo)
check('application seeds legacy bubble preference into theme packs', 'seedBubbleAppearanceIfMissing' in application)

check('theme card contains bubble toggle', 'theme_bubble_effect_toggle' in activity and 'setBubbleEnabled(pack' in activity)
check('theme card contains bubble style control', 'theme_bubble_style_label' in activity and 'setBubbleIntensity(pack' in activity)
check('theme card loads saved bubble appearance', 'currentBubbleAppearance(pack)' in activity)
check('settings copy explains per-theme bubble', 'theme_bubble_effect_hint' in strings)

check('IME collects per-theme bubble settings', 'bubbleAppearanceSettings.collectLatest' in ime)
check('IME resolves active theme bubble appearance', 'activeBubbleAppearance()' in ime)
check('IME policy enabled uses active appearance', 'enabled = bubbleAppearance.enabled' in ime)
check('IME policy intensity uses active appearance', 'intensity = bubbleAppearance.intensity' in ime)
check('IME cancels active flight when active theme bubble disabled', 'cancelEditor' in ime and '!activeBubbleAppearance().enabled' in ime)
check('legacy global fallback remains for non-pack themes', 'currentSettings.bubbleKeyEnabled' in ime and 'currentSettings.bubbleKeyIntensity' in ime)

check('settings bubble toggle dual-writes active pack', 'setBubbleEnabled(pack' in settings_category and 'setBubbleKeyEnabled(enabled)' in settings_category)
check('settings bubble style dual-writes active pack', 'setBubbleIntensity(pack' in settings_category and 'setBubbleKeyIntensity(intensity)' in settings_category)
check('legacy bubble repository remains for backward compatibility', 'bubbleKeyEnabled' in settings and 'bubbleKeyIntensity' in settings)

for pack in ('CLASSIC_DARK', 'GLASS_MODERN', 'CLEAN_LIGHT', 'GRADIENT_PRO'):
    check(f'all theme packs supported: {pack}', pack in read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt'))

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + ': ' + name)
print(f'\nStage 24.2 Optional Bubble contract: {len(checks)-len(failed)}/{len(checks)} PASS')
if failed:
    sys.exit(1)
