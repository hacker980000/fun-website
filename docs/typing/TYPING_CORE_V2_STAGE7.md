# Typing Core v2 — Stage 7

Stage 7 adds an offline English typing layer without changing the existing AI/backend/privacy architecture.

## Added

- Curated offline English lexicon with 1,000+ common, creator, work, technology and Bangladesh-relevant terms.
- English composing-word suggestions with exact, alias, prefix and conservative one-edit typo candidates.
- Conservative autocorrect on Space. Explicit aliases such as `dont → don't` are safe replacements; ambiguous fuzzy candidates are suggestions only.
- Offline English next-word prediction from a built-in conversational corpus.
- Bounded, device-local English personal learning: up to 512 learned words and 1,024 word transitions.
- English learning uses the existing Personal typing learning switch and is cleared by Clear Learned Typing Data.
- Smart Bangla ↔ English hints. Cross-language candidates never change mode automatically; language changes only when the user taps the cross-language candidate.
- English suggestions, English autocorrect and smart language hints have independent Settings controls.
- Sensitive/password/PIN/OTP/payment fields bypass English composition intelligence and never learn English typing data.
- Non-letter keys from the optional number row flush an active English/Bangla phonetic composition before inserting the number, preventing mixed composing-state corruption.

## Privacy

English personal learning remains on-device in a bounded SharedPreferences store. It is not uploaded to the AI backend and is not mixed with AI training data.
