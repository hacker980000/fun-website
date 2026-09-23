# Typing Core v2 — Stage 22: Confidence-aware autocorrect + prediction-quality gates

Stage 22 builds on the Stage 21 prediction/ranking and cold-start evidence foundation. The goal is to reduce unwanted automatic replacements without weakening explicit safe aliases or the existing offline prediction architecture.

This stage does **not** change AI generation, backend routing, account/session behavior, context extraction, sensitive-field blocking, clipboard/voice behavior, cloud training, or Send/Post behavior. Normal text keys remain deliberately un-debounced.

## Confidence-aware English typo autocorrect

The previous English fuzzy-autocorrect rule applied a typo only when the indexed typo search returned exactly one candidate. That was safe but overly dependent on candidate-count shape: a very strong intended correction could be blocked merely because a much weaker second candidate also existed.

Stage 22 adds `PredictionAutocorrectPolicy`, an Android-free deterministic confidence gate. English fuzzy autocorrect now requires all of the following:

- token length of at least 4 characters;
- the top typo candidate clears a minimum score;
- when a runner-up exists, the top candidate leads by a conservative score margin.

Explicit aliases remain authoritative and unchanged (`dont -> don't`, etc.). Known exact dictionary words are never fuzzy-autocorrected. Ambiguous candidates remain suggestions only and are not applied automatically. Short typo-like tokens remain literal unless covered by an explicit safe alias.

Examples covered by the Stage 22 regression corpus:

- `dont` -> `don't` — explicit safe alias, unchanged;
- `mesage` -> `message` — confident single typo candidate;
- `wdth` -> `with` — strong top candidate despite a weaker `width` alternative;
- `shave` stays `shave` — `save` and `share` are tied/ambiguous;
- `typea` stays `typea` — `type` and `typed` are tied;
- `teh` stays `teh` — short fuzzy token guard.

Turning autocorrect off continues to preserve the literal typed token.

## Bangla safety envelope centralization

Bangla already had conservative typo-autocorrect thresholds. Stage 22 moves those thresholds into the same `PredictionAutocorrectPolicy` helper without intentionally changing the existing Bangla safety envelope:

- minimum token length: 5;
- minimum candidate score: 935;
- minimum runner-up margin: 45.

The existing rendered-fallback dictionary guard remains in place. Bangla exact, alias, prefix, personal-learning, next-word and two-word-context behavior otherwise remain unchanged.

## Reproducible offline quality gate

`scripts/typing_stage22_autocorrect_quality_selftest.kt` exercises:

- confidence gate boundaries;
- overflow-safe score-margin arithmetic;
- explicit English aliases and casing;
- confident and ambiguous English typo fixtures;
- literal behavior when autocorrect is disabled;
- Stage 21 English next-word fixtures;
- English personal next-word learning;
- Bangla exact/alias ranking;
- Bangla next-word and personal-learning behavior.

The quality test is Android-free and deterministic, so it can run even where no emulator or physical device is available.

## CI/device evidence

`scripts/stage22_ci_autocorrect_quality.sh` wraps the entire Stage 21 device chain, then runs four controlled English editor fixtures on a fresh debug validation field:

1. explicit alias (`dont -> don't`),
2. confident multi-candidate typo (`wdth -> with`),
3. ambiguity guard (`shave` remains unchanged),
4. short-token guard (`teh` remains unchanged).

The wrapper keeps process-survival and crash/ANR checks as hard gates. The fixture words are static test data; the Stage 22 wrapper does not read or log arbitrary production user text.

The API 36 automatic emulator job and API 26/30/36 manual compatibility matrix now call the Stage 22 wrapper, which transitively retains all prior Stage 21/20/19/... checks.

## Evidence boundary

Stage 22 intentionally does **not** change Stage 21 ranking weights or establish latency budgets from uncollected device evidence. The Stage 21 cold-start/meminfo/gfxinfo/logcat hooks remain the source of real runtime evidence when CI or physical-device runs are available.

No real-device result is claimed by this source update. Local verification covers deterministic Kotlin behavior, static source contracts, historical regression tests and protected-source diffs; Android build/emulator/device results must come from the configured CI/device environment.
