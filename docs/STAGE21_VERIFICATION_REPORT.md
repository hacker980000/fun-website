# Typing Core v2 Stage 21 - Verification Report

Date: 2026-09-21
Baseline: `Social_AI_Keyboard_v35.3.1_TYPING_CORE_V2_STAGE20_RESIZE_MEMORY_HARDENING_SOURCE(1).zip`

## Scope

Stage 21 is an additive prediction/ranking quality and evidence-foundation update. It preserves the existing AI/backend/context/safety behavior and does not change the server contract, training data, security rules, subscription/device logic, or user data schema.

## Stage 21 changes verified

- Centralized English/Bangla ranking constants in `PredictionRankingPolicy` so future tuning can be driven by evidence without scattering magic numbers.
- Safe English aliases receive explicit ranking priority (for example, `dont` can surface `don't` as the intended correction).
- Candidate ranking sorts before deduplication, preserving the highest-quality source when duplicate text is produced by multiple candidate paths.
- English completion/typo penalties and English/Bangla next-word weights are centralized without changing the intended offline architecture.
- Bangla equal-score candidate ordering has a deterministic candidate-kind tie-break.
- Added one-shot, process-memory-only cold-start milestones: learning models ready, service ready, first input view ready, first suggestion ready.
- Added debug-only prediction-presented and prediction-accepted evidence markers that record only bounded metadata such as language/count/rank/flags and never typed text, suggestion text, editor content, or package name.
- Added a Stage 21 CI/device evidence wrapper that retains the full Stage 20 evidence chain and adds deterministic English/Bangla prediction fixtures plus three cold-start rounds.

## Local verification completed

- Stage 21 prediction/cold-start self-test: **27/27 PASS**
- Stage 21 static verification: **25/25 PASS**
- Stage 20 resize/memory self-test: **27/27 PASS**
- Stage 19 touch/responsiveness self-test: **33/33 PASS**
- Stage 18 render/memo self-test: **20/20 PASS**
- Stage 7 English/multilingual self-test: **17/17 PASS**
- Stage 5 context self-test: **13/13 PASS**
- Stage 20 static verification: **20/20 PASS**
- Stage 19 static verification: **16/16 PASS**
- Historical static/source-contract checks, XML parsing, shell syntax, bounded-update, release-ready, emoji compile/panel, training parity, and shared backend configuration checks: **PASS**
- Protected `ai`, `backend`, `context`, and `safety` source directories compared with the Stage 20 baseline: **byte-identical / PASS**

## Verification boundary

This source archive does not contain the official `gradle-wrapper.jar`; it contains only `gradle-wrapper.jar.sha256` and `gradle-wrapper.properties`. The current environment also has no system Gradle or ADB executable. Therefore an Android APK build and real emulator/device CI run were not fabricated or claimed locally.

The Stage 21 workflow and `scripts/stage21_ci_prediction_coldstart.sh` are prepared to collect real API 26/30/36 evidence when run in the configured Android CI/device environment. Stage 21 deliberately treats those measurements as evidence inputs for later tuning rather than hard-coding unmeasured device assumptions.
