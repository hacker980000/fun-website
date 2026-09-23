# Typing Core v2 — Stage 2

## Scope
Offline Bangla phonetic candidate suggestions and conservative autocorrect foundation.

## Added
- `OfflineBanglaSuggestionEngine`: pure Kotlin, network-free top-3 candidate engine.
- Candidate sources: exact lexicon match, explicit safe shorthand alias, prefix completion, bounded typo candidate, and current phonetic fallback.
- A themed 3-candidate suggestion strip in Bangla Phonetic letter mode.
- Candidate tap replaces and commits only the current composing word.
- Space-boundary autocorrect for explicit high-confidence shorthand aliases such as `amr -> আমার` and `kmn -> কেমন`.
- Fuzzy edit-distance results are suggestions only; they are never auto-applied.
- Pure Kotlin Stage-2 self-test plus JUnit coverage.

## Safety / regression contract
- Core typing remains offline.
- AI/backend/privacy/context code is unchanged in this stage.
- Normal fallback words are preserved when no safe alias exists (for example `kalo -> কালো`).
- Stage-1 phonetic behavior remains covered by its existing regression self-test.

## Verification
- `suggestion_stage2_selftest`: 14 checks PASS.
- `phonetic_stage1_selftest`: 18 checks PASS.
- `verify_keyboard_bounded_update.py`: PASS.
- `verify_release_ready.py`: PASS.
- All Android resource XML parses successfully.
- Full Gradle test could not start in the execution environment because `services.gradle.org` DNS resolution failed while downloading Gradle 9.6.0. This is an environment/network limitation, not a reported source assertion failure.

## Not yet included
- Large production Bangla dictionary/trie.
- Next-word prediction after a completed word.
- User-personalized learning/ranking.
- English suggestion/autocorrect.
- Corpus-based frequency model.
