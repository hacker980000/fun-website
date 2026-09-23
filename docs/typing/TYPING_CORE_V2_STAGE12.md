# Typing Core v2 — Stage 12

Stage 12 hardens editor-specific IME behavior and expands documented Avro compatibility without changing the managed AI/backend/privacy stack.

## Editor-aware typing policy

`ImeEditorBehaviorPolicy.typingPolicy()` now treats Android editor intent as a first-class input:

- `TYPE_TEXT_FLAG_NO_SUGGESTIONS`: no keyboard dictionary candidates, autocorrect, personal learning, or cross-language hints.
- `TYPE_TEXT_FLAG_AUTO_COMPLETE`: the editor owns completion UI; keyboard candidates/autocorrect/learning are suppressed.
- Email / web-email fields: literal Latin input, no autocorrect/learning, and quick keys for `@`, `.`, `_`, `-`, `.com`.
- URI fields: literal Latin input, no autocorrect/learning, and quick keys for `https://`, `www.`, `.com`, `/`, `.`.
- Filter fields: no dictionary suggestions/autocorrect/personal learning.
- Password-like text fields: no suggestion intelligence or learning; existing sensitive-field policy remains authoritative.
- Generic text fields: suggestions remain available. Autocorrect is enabled only when the editor explicitly requests `TYPE_TEXT_FLAG_AUTO_CORRECT` and the user setting also allows it.

The field policy is reset at input-session boundaries so a sensitive/special editor cannot leak typing state into the next field.

## Avro documented golden compatibility pack

Fourteen documented/high-value exact Avro examples were added to the dictionary-first compatibility layer, including:

- `bybohar` → `ব্যবহার`
- `byakti` → `ব্যক্তি`
- `bishwo` → `বিশ্ব`
- `swagoto` → `স্বাগত`
- `korrmo` → `কর্ম`
- `nirrmol` → `নির্মল`
- `urrdi` → `উর্দি`
- `aZromeTik` → `অ্যারোমেটিক`
- `aZDmin` → `অ্যাডমিন`
- `rriN` → `ঋণ`
- `brritto` → `বৃত্ত`
- `shikSha` / `shikkha` → `শিক্ষা`
- `brohmputro` → `ব্রহ্মপুত্র`

The merged offline Bangla/Banglish lexicon is now 1,158 entries. This exact compatibility pack complements the deterministic fallback grammar; it does not claim exhaustive historical Avro parity.

## Verification

- Stage 12 editor policy self-test: 46/46 PASS
- Stage 12 editor quick-key layout self-test: 6/6 PASS
- Stage 12 Avro golden self-test: 18/18 PASS
- Stage 10 dictionary benchmark: 1,158 entries, 0 blanks, 0 invalid Roman keys, 0 normalization conflicts, 0 exact mismatches
- Stage 1–11 typing regression suites remain green
- Historical static verifiers updated only where Stage 12 intentionally replaced older hard-coded source-string expectations with field-aware policy gates

## Build note

The source still intentionally does not vendor an unverified Gradle wrapper JAR. `scripts/check_gradle_wrapper_completeness.py` reports that verified wrapper bootstrap is required. CI is prepared to install Gradle 9.6.0, bootstrap the wrapper, verify the pinned checksum, and then run the Android build.
