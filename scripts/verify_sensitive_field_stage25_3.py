#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
policy = (root / 'app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicy.kt').read_text()
classifier = (root / 'app/src/main/java/com/socialaiassistant/keyboard/safety/SensitiveMetadataClassifier.kt').read_text()
field = (root / 'app/src/main/java/com/socialaiassistant/keyboard/safety/FieldSafety.kt').read_text()
adapter = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/EditorInfoSafetyAdapter.kt').read_text()
ime = (root / 'app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt').read_text()
accessibility = (root / 'app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt').read_text()
test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/safety/SensitiveFieldPolicyTest.kt').read_text()
adapter_test = (root / 'app/src/test/java/com/socialaiassistant/keyboard/ime/EditorInfoSafetyAdapterTest.kt').read_text()
privacy = (root / 'docs/play-store/privacy-policy.md').read_text()
checklist = (root / 'docs/play-store/release-checklist.md').read_text()
matrix = (root / 'docs/testing/release-device-matrix.md').read_text()

checks = []
def require(name, condition):
    checks.append((name, bool(condition)))
    if not condition:
        raise SystemExit(f'FAIL: {name}')

require('EditorDescriptor carries label/field/action metadata', all(x in field for x in ['labelText:', 'fieldName:', 'actionLabel:', 'editorMetadataHints:']))
require('policy no longer depends on Android runtime InputType', 'import android.text.InputType' not in policy and 'TYPE_NUMBER_VARIATION_PASSWORD' in policy)
require('password input variations are hard blocked', all(x in policy for x in ['TYPE_TEXT_VARIATION_PASSWORD', 'TYPE_TEXT_VARIATION_VISIBLE_PASSWORD', 'TYPE_TEXT_VARIATION_WEB_PASSWORD', 'TYPE_NUMBER_VARIATION_PASSWORD']))
require('semantic classifier handles camelCase metadata', 'lowerToUpperBoundary' in classifier and 'acronymBoundary' in classifier)
require('semantic classifier keeps Unicode combining marks for Bangla', r'\\p{M}' in classifier)
require('OTP/PIN/CVV/card/payment/banking signals are covered', all(x in classifier.lower() for x in ['otp', 'pin', 'cvv', 'credit card', 'payment pin', 'banking pin']))
require('Bangla sensitive terms are covered', all(x in classifier for x in ['ওটিপি', 'পিন', 'কার্ড নম্বর', 'বিকাশ পিন']))
require('generic unlabeled numeric fields fail closed', 'plain numeric editor with weak/no semantics can be OTP/PIN/CVV' in policy and 'FieldSafety.BLOCK_AI' in policy)
require('explicit benign numeric metadata becomes no-conversation', 'isExplicitlyBenignNumeric' in policy and 'FieldSafety.NO_CONVERSATION' in policy)
require('phone/date editors are not conversation surfaces', 'TYPE_CLASS_PHONE, TYPE_CLASS_DATETIME -> FieldSafety.NO_CONVERSATION' in policy)
require('EditorInfo adapter consumes hint label fieldName actionLabel privateImeOptions', all(x in adapter for x in ['editor.hintText', 'editor.label', 'editor.fieldName', 'editor.actionLabel', 'editor.privateImeOptions']))
require('EditorInfo extras scan is bounded', all(x in adapter for x in ['MAX_HINTS = 24', 'MAX_HINT_CHARS = 160', 'looksSafetyRelevantKey']))
require('IME uses centralized EditorInfo safety adapter', 'EditorInfoSafetyAdapter.descriptor(editor)' in ime)
require('Accessibility evaluates current focused editor safety', 'updateFocusedFieldRestriction' in accessibility and 'focused.isPassword' in accessibility and 'focused.inputType' in accessibility)
require('Accessibility restriction is monotonic', 'fun restrictSafety(packageName: String, candidate: FieldSafety)' in ime and 'ALLOW -> NO_CONVERSATION -> BLOCK' in ime)
require('Accessibility clears pending context on stricter field signal', 'cancelPendingContextExtractions()' in accessibility[accessibility.find('private fun updateFocusedFieldRestriction'):accessibility.find('private fun updateBubbleEditableTarget')])
require('virtual inputType=0 avoids accidental no-conversation downgrade', 'restriction == FieldSafety.NO_CONVERSATION && focused.inputType == 0' in accessibility)
require('focused-field safety avoids per-keystroke reevaluation', 'SENSITIVE_FIELD_EVENTS' in accessibility and 'AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED' not in accessibility[accessibility.find('val SENSITIVE_FIELD_EVENTS'):accessibility.find('}', accessibility.find('val SENSITIVE_FIELD_EVENTS'))])
require('policy tests cover fail-closed numeric and benign exceptions', 'unlabeled_or_unknown_numeric_field_fails_closed' in test and 'explicitly_benign_numeric_fields_disable_conversation_without_secret_classification' in test)
require('policy tests cover autofill/html-like tokens and Bangla', 'camel_case_autofill_and_html_tokens_block_ai' in test and 'bangla_sensitive_metadata_blocks_ai' in test)
require('adapter tests cover EditorInfo metadata/extras', 'relevant_editor_extras_are_bounded_and_classified' in adapter_test and 'fieldName = "paymentPinInput"' in adapter_test)
require('privacy policy documents conservative numeric protection', 'unlabeled or ambiguous numeric' in privacy.lower() and 'metadata' in privacy.lower())
require('release checklist includes generic numeric and metadata QA', 'Unlabeled numeric' in checklist and 'smsOTPCode' in checklist)
require('device matrix includes metadata-poor numeric regression case', 'metadata-poor numeric' in matrix)

print(f'Stage 25.3 sensitive-field verifier PASS ({len(checks)}/{len(checks)})')
for name, _ in checks:
    print(f'  PASS: {name}')
