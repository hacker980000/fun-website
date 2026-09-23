#!/usr/bin/env python3
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
service = (root/'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
glide = (root/'app/src/main/java/com/socialaiassistant/keyboard/ime/GlideTypingEngine.kt').read_text()
caption = (root/'app/src/main/java/com/socialaiassistant/keyboard/CaptionActivity.kt').read_text()
bus = (root/'app/src/main/java/com/socialaiassistant/keyboard/ime/CaptionDraftBus.kt').read_text()

def function_body(name):
    marker = f'override fun {name}('
    start = service.index(marker)
    brace = service.index('{', start)
    depth = 1; i = brace + 1
    while i < len(service) and depth:
        if service[i] == '{': depth += 1
        elif service[i] == '}': depth -= 1
        i += 1
    return service[brace+1:i-1]
finish = function_body('onFinishInput')
assert 'finishAndDiscardCompositionOnSessionEnd()' in finish
assert 'flushActiveComposition()' not in finish
assert 'disableTypingIntelligence()' in finish
assert 'MAX_GLIDE_PATH_KEYS = 64' in service
assert 'session.sequence.length < MAX_GLIDE_PATH_KEYS' in service
assert 'englishEntriesByFirst' in glide and 'banglaEntriesByFirst' in glide
assert glide.count('.groupBy { it.path.first() }') == 2
assert 'DeferredInsertionPolicy.decision' in service
assert 'VoiceResultBus.pending.collectLatest' in service
assert 'fieldId = editor.fieldId' in service
assert 'DeferredInsertionExtras.CREATED_AT_NANOS' in caption
assert 'PendingCaptionDraft' in bus
print('verify_typing_stage13: PASS')
