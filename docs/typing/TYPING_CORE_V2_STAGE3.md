# Typing Core v2 - Stage 3

Stage 3 expands the offline Bangla typing foundation without changing the AI/backend/privacy stack.

## Added
- Expanded offline roman-to-Bangla lexicon (700+ unique entries after merge with Stage 1).
- `BanglaLexiconIndex` for exact, prefix and bounded typo lookup without scanning the full dictionary on each key press.
- Corpus-derived offline next-word prediction using a built-in conversational Bangla corpus.
- `NEXT_WORD` candidates reuse the existing three-slot suggestion bar.
- Session-local personal learning seam (`LocalTypingLearningModel`) with word and bigram boosts.
- Space-boundary context tracking and next-word candidate insertion.

## Safety / behavior constraints
- No network is used for word or next-word prediction.
- Fuzzy typo candidates remain suggestion-only and are never automatically committed.
- Automatic replacement remains limited to explicit high-confidence aliases.
- Extra Space or Backspace dismisses stale next-word predictions.
- Sensitive/password/OTP fields hide suggestions and disable session learning.
- Existing AI, backend, accessibility and privacy contracts remain unchanged.

## Verification
- Stage 1 phonetic self-test remains green.
- Stage 2 suggestion self-test remains green.
- Stage 3 lexicon/next-word self-test covers index scale, expanded vocabulary, alias behavior, next-word ranking and local learning.
- Android Gradle build should still be run in CI/a network-enabled Android environment.
