# Typing Core v2 — Stage 1

## Goal
Replace the fragile hard-coded composer with a testable, replaceable phonetic-engine boundary without changing the existing AI/backend/UI flows.

## Changes
- Added `BanglaPhoneticTransliterator` interface.
- Added `HybridBanglaPhoneticTransliterator` as the Stage-1 implementation.
- Added high-frequency conversational Bangla/Banglish lexical coverage for ambiguous spellings.
- Added deterministic context-aware fallback transliteration for words not present in the lexical layer.
- Preserved case in `ImeTypingEngine` so Avro-style uppercase phonetic distinctions can be supported.
- Added regression tests for common conversation words, fallback behavior, case-sensitive input, and backspace composition.

## Compatibility
- Existing IME composition flow is unchanged.
- Existing AI, backend, privacy, accessibility, theme, emoji, clipboard and voice modules are untouched.
- Core typing remains offline.

## Important limitation
This stage is a foundation, not a claim of 100% Avro compatibility. The next stage will add the full candidate/suggestion pipeline, scalable dictionary loading, typo ranking and personal learning.
