from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks = []

def require(name, condition):
    checks.append((name, bool(condition)))

settings = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt').read_text()
custom = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/settings/KeyboardCustomization.kt').read_text()
settings_activity = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt').read_text()
main_xml = (ROOT / 'app/src/main/res/layout/settings_category_theme_appearance.xml').read_text()
ime_xml = (ROOT / 'app/src/main/res/layout/ime_keyboard.xml').read_text()
service = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
renderer_path = ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyEffectRenderer.kt'
renderer = renderer_path.read_text() if renderer_path.exists() else ''
policy = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt').read_text()

require('BubbleKeyIntensity enum', 'enum class BubbleKeyIntensity' in custom)
require('settings default OFF', 'val bubbleKeyEnabled: Boolean = false' in settings)
require('settings intensity default NORMAL', 'val bubbleKeyIntensity: BubbleKeyIntensity = BubbleKeyIntensity.NORMAL' in settings)
require('settings enable setter', 'setBubbleKeyEnabled' in settings)
require('settings intensity setter', 'setBubbleKeyIntensity' in settings)
require('settings persisted enabled key', 'BUBBLE_KEY_ENABLED' in settings and 'bubble_key_enabled' in settings)
require('settings persisted intensity key', 'BUBBLE_KEY_INTENSITY' in settings and 'bubble_key_intensity' in settings)
require('settings decode enabled default false', 'preferences[BUBBLE_KEY_ENABLED] ?: false' in settings)
require('settings decode intensity', 'BubbleKeyIntensity.fromSetting(preferences[BUBBLE_KEY_INTENSITY])' in settings)

require('settings UI free toggle', '@+id/bubble_key_checkbox' in main_xml and 'theme_bubble_effect_toggle' in main_xml)
require('settings UI intensity status', '@+id/status_bubble_key_intensity' in main_xml)
for key in ('button_bubble_soft', 'button_bubble_normal', 'button_bubble_playful'):
    require(f'settings UI {key}', f'@+id/{key}' in main_xml)
require('SettingsCategory toggle wiring', 'setBubbleKeyEnabled' in settings_activity and 'bubble_key_checkbox' in settings_activity)
require('SettingsCategory intensity wiring', 'setBubbleKeyIntensity' in settings_activity and 'button_bubble_playful' in settings_activity)
require('SettingsCategory refresh state', 'settings.bubbleKeyEnabled' in settings_activity and 'settings.bubbleKeyIntensity' in settings_activity and 'bubbleAppearance' in settings_activity)

require('IME bubble overlay', '@+id/bubble_key_overlay' in ime_xml)
require('overlay not accessible', 'android:importantForAccessibility="no"' in ime_xml)
require('bubble renderer exists', bool(renderer))
require('renderer pools views', 'idleBubbles' in renderer and 'activeBubbles' in renderer)
require('renderer bounded via policy', 'MAX_SIMULTANEOUS_BUBBLES' in renderer)
require('renderer theme aware', 'theme.primaryNeon' in renderer and 'theme.keySurface' in renderer)
require('renderer releases animations', 'fun release()' in renderer)

require('service BubbleKeyPolicy request', 'BubbleKeyRequest(' in service)
require('service system animator respect', 'ValueAnimator.areAnimatorsEnabled()' in service)
require('service sensitive block input', 'sensitiveField = ' in service and 'FieldSafety.BLOCK_AI' in service)
require('service letter layer input', 'letterLayer = keyboardMode.layer == KeyboardLayer.LETTERS' in service)
require('service standard tap hook', ('maybeShowBubbleKey(button, key, glideGesture = false)' in service) or ('prepareBubbleFlight(view, key, glideGesture = false)' in service and 'bubbleFlightTapCoordinator.commitThenDispatch(prepared)' in service))
require('service glide tap hook distinguishes drag', ('maybeShowBubbleKey(view, key, glideGesture = session?.dragging == true)' in service) or ('prepareBubbleFlight(view, key, glideGesture = session?.dragging == true)' in service))
require('service lifecycle release', 'bubbleKeyRenderer?.release()' in service)
require('policy bounded eight', 'MAX_SIMULTANEOUS_BUBBLES = 8' in policy)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + ': ' + name)
print(f'Stage 23.3 Bubble Key static contract: {len(checks)-len(failed)}/{len(checks)} PASS')
if failed:
    raise SystemExit(1)
