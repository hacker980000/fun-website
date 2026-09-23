# Typing Core v2 — Stage 5

Stage 5 strengthens offline Bangla/Banglish typing quality while keeping the existing AI/backend/privacy architecture unchanged.

## Added

- Curated Bangladesh-first phonetic lexicon expansion merged into the indexed dictionary.
- Merged dictionary size: 1,144 roman-to-Bangla entries in the current source build.
- 206 additional built-in, non-user conversational corpus sentences.
- Two-word (trigram-style) context ranking for next-word prediction.
- Persistent device-local personal context transitions, bounded to 1,024 entries.
- One-edit signature index for faster typo candidate retrieval without first-letter lock-in.
- Adjacent transposition-aware typo matching (for examples such as `messgae`).
- Conservative explicit shorthand alias expansion.
- Conservative high-confidence typo autocorrect gate; fuzzy candidates remain suggestions unless strict confidence checks pass.

## Privacy / safety

- Personal typing learning remains device-local.
- Context learning uses normalized word transitions only and is never uploaded to AI training.
- Existing sensitive/password/PIN/OTP/payment/banking learning gates remain unchanged.
- Protected `ai/`, `backend/`, `context/`, and `safety/` source directories are byte-for-byte unchanged from Stage 4.

## Verification

- Stage 1 phonetic regression: 18 checks PASS.
- Stage 2 suggestion regression: 14 checks PASS.
- Stage 3 engine regression: 9 checks PASS.
- Stage 4 personalization regression: 7 checks PASS.
- Stage 5 context/lexicon/autocorrect: 13 checks PASS.
- Stage 5 persistent context save/reload/clear: 7 checks PASS using Android API stubs and fake SharedPreferences.
- Stage 2/3/4/5 static verification: PASS.
- Bounded update verification: PASS.
- Release verification: PASS.
- Emoji compile/UI guards: PASS.
- XML parse: PASS.

Full Gradle wrapper execution cannot start in the current environment because `services.gradle.org` cannot be resolved, so the Gradle distribution is unavailable here.
