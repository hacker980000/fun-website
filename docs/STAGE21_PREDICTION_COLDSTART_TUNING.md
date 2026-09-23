# Typing Core v2 — Stage 21: Prediction/ranking quality + cold-start evidence foundation

Stage 21 polishes deterministic offline prediction ranking and adds privacy-safe evidence hooks for tuning from CI/emulator/physical-device runs. It does not change AI generation, backend routing, context access, sensitive-field blocking, manual Send behavior, or cloud training.

## Prediction/ranking quality polish

- English safe shorthand aliases are now ranked consistently with their existing autocorrect contract. For example, `dont -> don't` is a conservative explicit alias, so the corrected form is visible ahead of the raw dictionary spelling instead of only being applied at Space commit time.
- English candidate ranking now sorts by score/source quality before text deduplication. If multiple sources produce the same visible candidate, the strongest source survives.
- English prefix ranking applies a small completion-length penalty so equally strong shorter completions are preferred over unnecessarily long completions.
- English typo ranking applies an explicit candidate-length delta penalty in addition to the existing position/frequency/personal-learning terms.
- English and Bangla candidate tie breaks now include deterministic source-kind priority before lexical fallback ordering.
- English bigram and Bangla bigram/two-word-context score weights are centralized in `PredictionRankingPolicy`. This is the tuning surface for later measured adjustments; current Bangla/English next-word semantics remain compatible with Stage 20.
- Existing bounded on-device personal learning remains part of ranking. Stage 21 does not upload or persist new prediction telemetry.

## Cold-start profiling hooks

Debug builds now emit one-shot fixed-name milestones through `SocialAiImeStage21`:

- `learning_models_ready`
- `service_ready`
- `first_input_view_ready`
- `first_suggestion_ready`

`ColdStartProfiler` stores only monotonic elapsed duration and the fixed milestone enum in process memory. The log line contains only the milestone and elapsed milliseconds. No typed text, candidate text, package name, editor hint, clipboard content, AI prompt, or conversation content is logged by these hooks.

Stage 21 also emits debug-only prediction evidence markers when a candidate row is actually presented and when a user taps a candidate. These markers contain only language, candidate count, accepted rank, next-word boolean and cross-language boolean. They deliberately exclude candidate/query text.

## CI/device evidence foundation

`scripts/stage21_ci_prediction_coldstart.sh` wraps the complete Stage 20 validation chain and then performs three fresh IME-service rounds. It:

- captures `am start -W`, process survival, `meminfo`, `gfxinfo` and filtered/full logcat for each round;
- requires all four cold-start milestones in every round;
- writes `cold-start-metrics.csv` and a min/median/max summary from measured milestone durations;
- exercises a controlled English fixture (`thank` -> next-word `you`) on every round;
- taps the controlled English candidate on round 1 so rank-accept evidence is captured;
- exercises a controlled Bangla fixture (`আমি` -> includes `এখন`) on round 1;
- keeps crash/ANR and process-survival checks as hard gates;
- intentionally does **not** hard-gate latency thresholds yet. The collected evidence is the foundation for setting realistic budgets from emulator plus low/mid-tier and recent physical-phone runs.

The API 36 automatic workflow and API 26/30/36 manual compatibility matrix now call the Stage 21 wrapper and archive the Stage 21 evidence directory.

## Verification boundary

- Stage 21 Android-free prediction/cold-start self-test covers ranking policy, safe aliases, deterministic dedupe/tie-break behavior, English/Bangla prediction fixtures, personal-learning re-ranking and one-shot cold-start state.
- Stage 21 static source verification checks ranking-policy wiring, privacy-safe logging, profiling hooks, CI wrapper and workflow integration.
- Historical Stage 1-20 verification remains in the wrapped chain and is not replaced.
- Protected `ai/`, `backend/`, `context/`, and `safety/` behavior is intentionally unchanged.

No uncollected real-device result is claimed by Stage 21. CI/emulator/physical-device timings must come from actual runs of the evidence script before thresholds or ranking weights are changed based on device performance.
