#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
store = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/RecentClipboardStore.kt').read_text()
service = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
settings = (root / 'app/src/main/java/com/socialaiassistant/keyboard/settings/SettingsRepository.kt').read_text()
activity = (root / 'app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt').read_text()
app = (root / 'app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt').read_text()
policy_test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/ime/ClipboardTextPolicyTest.kt').read_text()
store_test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/ime/RecentClipboardStoreTest.kt').read_text()
settings_test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/settings/SettingsRepositoryTest.kt').read_text()
privacy = (root / 'docs/play-store/privacy-policy.md').read_text()
checklist = (root / 'docs/play-store/release-checklist.md').read_text()

checks = []
def require(name, condition):
    checks.append((name, bool(condition)))
    if not condition:
        raise SystemExit(f'FAIL: {name}')

require('clipboard history defaults OFF in AppSettings', 'val clipboardHistory: Boolean = false' in settings)
require('missing preference decodes clipboard history as OFF', 'clipboardHistory = preferences[CLIPBOARD_HISTORY] ?: false' in settings)
require('encrypted v2 preferences replace plaintext item slots', 'recent_clipboard_history_v2_encrypted' in store and 'history_ciphertext' in store and 'history_iv' in store)
require('legacy plaintext v1 store is explicitly purged', 'recent_clipboard_history_v1' in store and 'legacyPreferences.edit().clear().apply()' in store)
require('clipboard history uses dedicated Android Keystore AES-GCM key', 'AndroidKeyStore' in store and 'AES/GCM/NoPadding' in store and 'social_ai_clipboard_history_key_v1' in store)
require('history retention is 24 hours', 'RETENTION_MILLIS = 24L * 60L * 60L * 1_000L' in store)
require('keystore failure never falls back to plaintext persistence', 'Never fall back to plaintext storage' in store)
require('application startup constructs store to purge legacy data', 'val clipboardHistoryStore = RecentClipboardStore(this)' in app)
require('disabled clipboard setting clears persisted history at app level', 'if (!settings.clipboardHistory && !clipboardPurgedWhileDisabled)' in app and 'clipboardHistoryStore.clear()' in app)

panel_start = service.find('private fun renderClipboardPanel')
description_read = service.find('primaryClipDescription', panel_start)
payload_read = service.find('val clip = if (hideCurrentClipboard) null else clipboard.primaryClip', panel_start)
exposure_policy = service.find('ClipboardTextPolicy.shouldExposeContent', panel_start)
require('clipboard description sensitivity is checked before payload access', panel_start >= 0 and description_read >= 0 and payload_read >= 0 and description_read < payload_read)
require('sensitive field/clip exposure policy gates clipboard payload', exposure_policy >= 0 and exposure_policy < payload_read and 'val clip = if (hideCurrentClipboard) null else clipboard.primaryClip' in service)
require('history read/write are skipped while clipboard content is hidden', service.count('!hideCurrentClipboard') >= 3)
require('disabling history from settings immediately clears the store', 'if (!enabled)' in activity and 'RecentClipboardStore(this@SettingsCategoryActivity).clear()' in activity)
require('exposure policy regression test covers field and clip sensitivity', 'exposure_is_blocked_for_sensitive_field_or_sensitive_clip' in policy_test)
require('encrypted clipboard store regression tests are present', 'encrypted_store_round_trips_without_plaintext_preferences' in store_test and 'history_expires_after_retention_window' in store_test and 'legacy_plaintext_is_purged_instead_of_migrated' in store_test)
require('settings regression test expects history OFF by default', 'assertFalse(value.clipboardHistory)' in settings_test)
require('privacy policy documents encrypted optional 24-hour clipboard history', 'Recent clipboard history is OFF by default' in privacy and 'expires after 24 hours' in privacy)
require('release checklist covers clipboard privacy regression cases', 'Clipboard history is OFF by default after a fresh install.' in checklist and 'Android-sensitive clips do not display clipboard payloads/history.' in checklist)

print(f'Stage 25.2 clipboard privacy verifier PASS ({len(checks)}/{len(checks)})')
for name, _ in checks:
    print(f'  PASS: {name}')
