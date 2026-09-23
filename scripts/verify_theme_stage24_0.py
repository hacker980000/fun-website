#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text()
repo = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt').read_text()
activity = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt').read_text()
renderer = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRenderer.kt').read_text()
layout = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardLayout.kt').read_text()
ime = (ROOT / 'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
strings = (ROOT / 'app/src/main/res/values/strings.xml').read_text()

checks = []
def check(name, ok):
    checks.append((name, bool(ok)))

check('manifest app icon', 'android:icon="@mipmap/ic_launcher"' in manifest)
check('manifest round icon', 'android:roundIcon="@mipmap/ic_launcher_round"' in manifest)
check('adaptive launcher icon', (ROOT / 'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml').exists())
check('adaptive round launcher icon', (ROOT / 'app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml').exists())
check('launcher foreground vector', (ROOT / 'app/src/main/res/drawable/ic_launcher_foreground.xml').exists())
check('launcher background resource', (ROOT / 'app/src/main/res/values/ic_launcher_background.xml').exists())

check('theme repository exposes key boundary flow', 'fun keyBoundaryEnabled(pack: ThemePack)' in repo)
check('theme repository exposes key boundary current', 'currentKeyBoundaryEnabled(pack: ThemePack)' in repo)
check('theme repository exposes key boundary setter', 'setKeyBoundaryEnabled(pack: ThemePack, enabled: Boolean)' in repo)
check('theme boundary defaults off', '?: false' in repo and 'key_boundary' in repo)
check('theme boundary stored per pack', 'pack.storedId' in repo and 'key_boundary' in repo)

check('theme pack card contains boundary checkbox', 'boundaryToggle = CheckBox(this)' in activity and 'theme_key_boundary_toggle' in activity)
check('theme pack boundary toggle loads stored value', 'currentKeyBoundaryEnabled(pack)' in activity)
check('theme pack boundary toggle persists', 'setKeyBoundaryEnabled(pack' in activity)

check('key spec carries visual role', 'visualRole: KeyVisualRole' in layout)
check('alphabetic visual role exists', 'enum class KeyVisualRole' in layout and 'ALPHABETIC' in layout)
check('english letters marked alphabetic', 'visualRole = KeyVisualRole.ALPHABETIC' in layout)
check('bijoy uses alphabetic key helper', 'map(::alphabeticKey)' in layout)

check('renderer has boundaryless alphabetic styling', 'styleBoundarylessAlphabeticKey' in renderer)
check('ime resolves active pack boundary preference', 'keyBoundarySettings.collectLatest' in ime and 'activeKeyBoundaryEnabled()' in ime)
check('ime applies boundaryless style only to alphabetic keys', 'KeyVisualRole.ALPHABETIC' in ime and 'styleBoundarylessAlphabeticKey' in ime)
check('settings copy explains per-theme boundary', 'key boundary' in strings.lower())

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + ': ' + name)
print(f'\nStage 24.0 contract: {len(checks)-len(failed)}/{len(checks)} PASS')
if failed:
    sys.exit(1)
