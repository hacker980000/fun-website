#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
keyboard_action = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/KeyboardAction.kt').read_text()
service = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
workflow = (root / '.github/workflows/android-build.yml').read_text()
debug_manifest = (root / 'app/src/debug/AndroidManifest.xml').read_text()
debug_activity = (root / 'app/src/debug/java/com/socialaiassistant/keyboard/debug/ImeValidationActivity.kt').read_text()
instrumented = (root / 'app/src/androidTest/java/com/socialaiassistant/keyboard/ime/ImeInsertionInstrumentedTest.kt').read_text()
perf_test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/ime/Stage14PerformanceRegressionTest.kt').read_text()
ack_test = (root / 'scripts/typing_stage14_insertion_ack_selftest.kt').read_text()
device_script = (root / 'scripts/stage14_adb_device_validation.sh').read_text()
apk_script = (root / 'scripts/verify_apk_artifact.sh').read_text()

assert 'CommitFailed' in keyboard_action
assert 'safeCommitText' in keyboard_action
assert 'runCatching { connection.commitText(text, 1) }.getOrDefault(false)' in keyboard_action
assert 'if (committed) VoiceResultBus.consume()' in service
assert 'VoiceResultBus.consume()\n                connection.commitText' not in service
assert 'val primaryResult = if (currentPreserveDraft)' in service
assert 'InsertResult.DraftPresent, InsertResult.NoConnection, InsertResult.CommitFailed -> Unit' in service
assert 'failed_commit_is_reported_instead_of_false_success' in instrumented
assert 'failed_select_all_does_not_append_over_existing_draft' in instrumented
assert 'ImeValidationActivity' in debug_manifest and 'android:exported="true"' in debug_manifest
assert 'Password (sensitive)' in debug_activity and 'Email (literal Latin)' in debug_activity
assert 'Stage14PerformanceRegressionTest' in perf_test and '8_000.0' in perf_test
assert 'TYPING STAGE 14 INSERTION ACK SELF-TEST' in ack_test and 'CommitFailed' in ack_test
assert 'stage14_adb_device_validation.sh' not in workflow  # device test remains explicit/manual, never silently changes IME
assert 'bash scripts/verify_apk_artifact.sh' in workflow
assert 'default IME is not changed' in device_script
assert 'FATAL EXCEPTION|ANR in' in device_script
assert 'BIND_INPUT_METHOD' in apk_script and '350301' in apk_script and '35.3.1' in apk_script
print('verify_typing_stage14: PASS')
