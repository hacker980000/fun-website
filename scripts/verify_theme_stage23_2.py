#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding='utf-8')

pack = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt')
surface = read('app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardThemeSurface.kt')
catalog = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeCatalog.kt')
codec = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt')
repo = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt')
activity = read('app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt')
renderer = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt')
resolver = read('app/src/main/java/com/socialaiassistant/keyboard/ime/ImeThemeSurfaceResolver.kt')
ime = read('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt')
layout = read('app/src/main/res/layout/activity_theme_settings.xml')
preview = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreviewView.kt')

expected_packs = ['CLASSIC_DARK', 'GLASS_MODERN', 'CLEAN_LIGHT', 'GRADIENT_PRO']
expected_surfaces = ['ENGLISH', 'NUMBER', 'SYMBOL', 'BANGLA', 'PHONETIC', 'BIJOY', 'SETTINGS']

checks = []
def check(name: str, condition: bool):
    checks.append((name, bool(condition)))

for name in expected_packs:
    check(f'pack {name}', re.search(rf'\b{name}\s*\(', pack) is not None)
for name in expected_surfaces:
    check(f'surface {name}', re.search(rf'\b{name}\s*\(', surface) is not None)
check('catalog enumerates all 4x7 variants', 'ThemePack.entries.flatMap' in catalog and 'KeyboardThemeSurface.entries.map' in catalog)
check('catalog exposes global chrome', 'fun globalChrome(pack: ThemePack)' in catalog)
check('catalog exposes per-surface design', 'fun surface(pack: ThemePack, surface: KeyboardThemeSurface)' in catalog)
check('premium visual fill styles', all(token in read('app/src/main/java/com/socialaiassistant/keyboard/theme/KeyboardTheme.kt') for token in ['SOLID', 'GLASS', 'GRADIENT']))
check('global pack preference key', 'THEME_GLOBAL_PACK_ID' in codec)
check('selection schema version key', 'THEME_SELECTION_SCHEMA_VERSION' in codec)
check('surface override preference keys', 'theme_override_${surface.storedId}' in codec)
check('legacy active theme retained', 'ACTIVE_THEME_ID' in codec and 'legacyActiveThemeId' in codec)
check('custom background retained in codec', 'background_file_name' in codec and 'customTheme' in codec)
check('repository exposes live selection state', 'val selectionState: Flow<ThemeSelectionState>' in repo)
check('repository applies a complete global pack and clears overrides', 'globalPack = pack' in repo and 'perSurfaceOverrides = emptyMap()' in repo)
check('repository can set surface override', 'suspend fun setSurfaceOverride' in repo)
check('repository can clear surface override', 'suspend fun clearSurfaceOverride' in repo)
check('advanced custom appearance preserves surface overrides', 'ThemeSelectionMutations.withCustomAppearance(current, theme)' in repo)
check('manual reset API takes selected pack', 'suspend fun resetToSelectedPack(pack: ThemePack)' in repo)
check('reset clears all per-surface overrides', 'perSurfaceOverrides = emptyMap()' in repo)
check('reset clears background config before file removal handoff', 'copy(background = BackgroundPhotoConfig())' in repo and 'ResetThemeResult(fileToDelete)' in repo)
check('settings renders four pack choices dynamically', 'ThemePack.entries.forEach' in activity and 'theme_pack_container' in layout)
check('settings renders seven surface choices dynamically', 'KeyboardThemeSurface.entries.forEach' in activity and 'theme_surface_container' in layout)
check('surface picker includes Use Global option', 'theme_use_global_pack' in activity)
check('legacy themes remain available', 'ThemePreset.builtIns.forEach' in activity and 'theme_legacy_preset_container' in layout)
check('advanced controls remain available', 'theme_advanced_section' in layout and 'button_save_custom_theme' in layout and 'button_pick_background_photo' in layout)
check('product reset has confirmation then manual pack picker', 'showResetConfirmation()' in activity and 'showResetPackPicker()' in activity and '.setItems(packs.map { it.displayName }.toTypedArray())' in activity)
check('reset mutation happens only after pack selection', activity.find('resetMutationStarted = true') > activity.find('.setItems(packs.map { it.displayName }.toTypedArray())'))
check('settings state collected live', 'repository.selectionState.collectLatest' in activity)
check('preview is surface aware', 'fun setPreview(' in preview and 'surface: KeyboardThemeSurface' in preview)
check('IME resolver maps number and symbol surfaces', 'KeyboardLayer.NUMBERS -> KeyboardThemeSurface.NUMBER' in resolver and 'KeyboardLayer.SYMBOLS -> KeyboardThemeSurface.SYMBOL' in resolver)
check('IME resolver maps phonetic and bijoy surfaces', 'BanglaInputMode.PHONETIC -> KeyboardThemeSurface.PHONETIC' in resolver and 'BanglaInputMode.BIJOY -> KeyboardThemeSurface.BIJOY' in resolver)
check('IME collects theme selection state', 'themeRepository.selectionState.collectLatest' in ime)
check('IME has separate global and surface themes', 'currentGlobalChromeTheme' in ime and 'currentSurfaceTheme' in ime)
check('IME key geometry uses surface theme', 'keyGapBits = currentSurfaceTheme.keyGapDp.toBits()' in ime)
check('IME normal/special key buttons use surface theme', 'themeRoleFor(key.action), currentSurfaceTheme' in ime)
check('IME toolbar uses global theme', 'currentGlobalChromeTheme,\n                aiSelected' in ime)
check('IME suggestion buttons use global theme', 'ThemeButtonRole.SECONDARY_ACTION, currentGlobalChromeTheme' in ime)
check('IME AI actions use global theme', 'ThemeButtonRole.AI_ACTION, currentGlobalChromeTheme' in ime)
check('IME background semantics use global/custom theme', 'val config = currentGlobalChromeTheme.background.normalized()' in ime)
check('mode changes refresh active surface without restartInput', 'refreshActiveSurfaceTheme()' in ime and 'restartInput' not in resolver)
check('renderer exposes split chrome and key-panel paths', 'fun applyGlobalChrome' in renderer and 'fun applyKeyPanelSurface' in renderer)
check('utility panel remains global chrome', 'keyboard_panel_container' in renderer and 'drawables.panel(t, t.aiNeon)' in renderer)
check('key wrapper receives surface treatment', 'R.id.keys_wrapper' in renderer)
check('normal text actions still bypass rapid-action debounce', 'rapidActionGate.allow("text' not in ime and 'rapidActionGate.allow("key' not in ime)
check('Stage21 profiling source still referenced', 'recordStage21PredictionPresented' in ime and 'recordStage21ColdStart' in ime)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
print(f"Stage23.2 premium multi-theme static verification: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed:
    sys.exit(1)
