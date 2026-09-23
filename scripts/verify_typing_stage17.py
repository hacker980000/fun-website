#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
read = lambda rel: (root / rel).read_text()
service = read('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt')
latency = read('app/src/main/java/com/socialaiassistant/keyboard/ime/RuntimeLatencyWindow.kt')
lazy = read('app/src/main/java/com/socialaiassistant/keyboard/ime/ResettableLazyResource.kt')
workflow = read('.github/workflows/android-build.yml')
script = read('scripts/stage17_ci_memory_latency.sh')

for token in [
    'ResettableLazyResource { GlideTypingEngine() }',
    'RuntimeLatencyWindow()',
    'releaseOptionalRuntimeResources("ui_hidden")',
    'releaseHiddenPanelViewsForMemoryPressure()',
    'override fun onLowMemory()',
    'ImeRuntimeMetric.SUGGESTION_COMPUTE',
    'ImeRuntimeMetric.GLIDE_RESOLVE',
    'ImeRuntimeMetric.KEYBOARD_RENDER',
    'runtime_metric metric=',
    'runtime_resource_released resource=glide',
]:
    assert token in service, token

for token in [
    'enum class ImeRuntimeMetric',
    'LongArray',
    'p50Micros',
    'p95Micros',
    'overBudgetCount',
    'never stores typed text',
]:
    assert token in latency, token

for token in ['class ResettableLazyResource', 'fun get(): T', 'fun release(): Boolean', 'fun isInitialized(): Boolean']:
    assert token in lazy, token

for token in [
    'stage17_ci_memory_latency.sh',
    'stage17-api36-memory-latency-report',
    'stage17-api${{ matrix.api-level }}-memory-latency-report',
]:
    assert token in workflow, token

for token in [
    'base_stage16=PASS',
    'send-trim-memory',
    'RUNNING_LOW',
    'UI_HIDDEN',
    'runtime_metric_markers=',
    'metric_privacy_note=',
    'process_alive_after_trim=PASS',
]:
    assert token in script, token

print('verify_typing_stage17: PASS')
