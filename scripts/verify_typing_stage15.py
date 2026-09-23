#!/usr/bin/env python3
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
service = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
debug = (root / 'app/src/debug/java/com/socialaiassistant/keyboard/debug/ImeValidationActivity.kt').read_text()
ids = (root / 'app/src/debug/res/values/ids.xml').read_text()
workflow = (root / '.github/workflows/android-build.yml').read_text()
script = (root / 'scripts/stage15_ci_emulator_validation.sh').read_text()
stage16_script = (root / 'scripts/stage16_ci_runtime_stress.sh').read_text() if (root / 'scripts/stage16_ci_runtime_stress.sh').exists() else ''

for token in [
    'contentDescription = keyContentDescription(key)',
    '"Key ${key.label}"',
    '"Language switch"',
    '"Language switch unavailable in literal Latin field"',
    '"Space"',
    '"Backspace"',
]:
    assert token in service, token

for rid in [
    'validation_normal', 'validation_chat', 'validation_multiline',
    'validation_no_suggestions', 'validation_email', 'validation_url',
    'validation_password', 'validation_phone', 'validation_number'
]:
    assert f'R.id.{rid}' in debug, rid
    assert f'name="{rid}"' in ids, rid

for token in [
    'ReactiveCircus/android-emulator-runner@v2.38.0',
    'connectedDebugAndroidTest',
    'stage17_ci_memory_latency.sh',
    'actions/download-artifact@v4',
    'api-level: 36',
    'api-level: [26, 30, 36]',
    'emulator-compat-matrix:',
]:
    assert token in workflow, token

for token in [
    'tap_desc "Key h"',
    'tap_desc "Language switch"',
    'assert_field_prefix validation_chat "আমি"',
    'assert_field_equals validation_email "ami"',
    'assert_desc_absent "Key a"',
    'crash_or_anr_markers',
]:
    assert token in script, token

assert 'stage15_ci_emulator_validation.sh' in stage16_script or 'stage15_ci_emulator_validation.sh' in workflow
assert re.search(r'android-emulator-runner@v2\.38\.0', workflow)
print('verify_typing_stage15: PASS')
