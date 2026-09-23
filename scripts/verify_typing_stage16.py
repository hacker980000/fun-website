#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
read = lambda rel: (root / rel).read_text()
service = read('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt')
bangla_model = read('app/src/main/java/com/socialaiassistant/keyboard/ime/PersistentTypingLearningModel.kt')
english_model = read('app/src/main/java/com/socialaiassistant/keyboard/ime/PersistentEnglishTypingLearningModel.kt')
next_word = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaNextWordModel.kt')
english_engine = read('app/src/main/java/com/socialaiassistant/keyboard/ime/EnglishTypingEngine.kt')
bangla_engine = read('app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt')
hitmap = read('app/src/main/java/com/socialaiassistant/keyboard/ime/GlideHitMap.kt')
controller = read('app/src/main/java/com/socialaiassistant/keyboard/ime/DeferredFlushController.kt')
workflow = read('.github/workflows/android-build.yml')
script = read('scripts/stage16_ci_runtime_stress.sh')

for token in ['fun requestFlush() = flush()', 'fun close() = flush()']:
    assert token in next_word, token
assert 'fun requestFlush() = flush()' in english_engine
assert 'learning.requestFlush()' in bangla_engine
assert 'learning.requestFlush()' in english_engine
assert 'learning.flush()' not in bangla_engine
assert 'learning.flush()' not in english_engine

for model in [bangla_model, english_model]:
    for token in [
        'DeferredFlushController(', 'lastOwnRevision', 'requestImmediateFlush()',
        'override fun requestFlush()', 'override fun close()', 'persistDirtyNow()'
    ]:
        assert token in model, token

for token in [
    'class GlideHitMap', 'fun keyAt(x: Float, y: Float, tolerancePx: Float = 0f)',
]: assert token in hitmap, token
for token in ['ScheduledFuture', 'DEFAULT_DELAY_MS = 650L', 'closeAndFlush()']:
    assert token in controller, token

for token in [
    'hitMap = snapshotGlideHitMap()', 'session.hitMap.keyAt(',
    'GLIDE_HAPTIC_MIN_INTERVAL_MS = 24L', 'safeConnectionOperation(',
    'recoverFromInputConnectionFailure()', 'requestImmediateFlush()',
    'override fun onTrimMemory(level: Int)', 'SLOW_CONNECTION_OPERATION_MS = 16.0'
]: assert token in service, token

for token in ['stage17_ci_memory_latency.sh', 'stage17-api36-memory-latency-report', 'stage17-api${{ matrix.api-level }}-memory-latency-report']:
    assert token in workflow, token
for token in ['base_stage15=PASS', 'stress_key_taps=', 'slow_input_connection_markers=', 'crash_or_anr_markers=']:
    assert token in script, token

print('verify_typing_stage16: PASS')
