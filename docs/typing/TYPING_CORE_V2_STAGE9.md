# Typing Core v2 — Stage 9

Stage 9 deepens Avro compatibility in the deterministic Bangla phonetic fallback and improves offline Glide candidate scoring. Existing AI/backend/privacy behavior is intentionally unchanged.

## Avro compatibility improvements

A new Android-free `AvroCompatibleBanglaPhoneticTransliterator` is the classic fallback behind the existing hybrid production transliterator. The production flow still prefers packaged conversational dictionary/autocorrect entries first, which preserves the keyboard's existing common-spelling behavior.

The classic fallback now covers the high-value documented case-sensitive Avro forms, including:

- `Ng → ঙ`, `NG → ঞ`, `ng → ং`
- `c → চ`, `ch → ছ`, `J → জ`
- `z → য`, contextual small `y`, force-capital `Y`
- `S/sh → শ`, `Sh → ষ`
- `R → ড়`, `Rh → ঢ়`
- `t`` → ৎ`, `: → ঃ`, `^ → ঁ`
- `rri → ঋ/ৃ`
- `oo → উ/ু`, `U → ঊ/ূ`
- `O → ও/ো`, `OU → ঔ/ৌ`

Word-initial small `y` follows the documented `ইয়...` behavior. Whitespace is preserved when the pure transliterator is used on a phrase.

This stage deliberately does **not** claim 100% parity with every historical Avro grammar/context edge case. The renderer is isolated behind a stable interface so remaining grammar rules can be added without changing the IME or AI stack.

## Glide scoring improvements

The Stage-8 offline resolver now uses a QWERTY-aware scoring model:

- repeated-letter path normalization remains supported;
- first key must match exactly for a strong gesture safety boundary;
- final key may be a directly neighboring QWERTY key with an explicit score penalty;
- neighboring-key substitutions cost less than unrelated substitutions;
- ordered-path/LCS coverage rejects candidates that do not preserve enough swipe order;
- matching key-to-key transitions receive a modest ranking bonus;
- length and endpoint uncertainty are explicitly penalized.

This remains entirely on-device and uses the packaged English/Bangla phonetic lexicons. Glide is still user-controlled and retains the Stage-8 default-Off rollout posture.

## Verification

- Stage 1 phonetic regression: 18/18 PASS
- Stage 2 suggestion regression: 14/14 PASS
- Stage 3 suggestion regression: 15/15 PASS
- Stage 3 engine regression: 9/9 PASS
- Stage 4 personalization regression: 7/7 PASS
- Stage 5 context regression: 13/13 PASS
- Stage 7 English/multilingual regression: 17/17 PASS
- Stage 8 glide/customization regression: 11/11 PASS
- Stage 9 Avro + glide self-test: 41/41 PASS
