# Typing Core v2 — Stage 10

Stage 10 hardens the Bangla phonetic fallback and adds measurable quality gates.

## Avro grammar hardening

- Avro-compatible case normalization keeps only the documented case-sensitive Roman letters significant.
- Corrected `Y → য়`, added explicit `Z → ্য`, and context-aware `y` behavior.
- Added context-aware `w` (`ও`, word-start `ওয়`, consonant `্ব`) and word-start `x → এক্স`.
- Added high-value conjunct patterns such as `gg/jNG → জ্ঞ`, `kSh/kx/kkh → ক্ষ`, nasal clusters, and vowelized `ng*` forms.
- Added backtick forced-kar forms and stream punctuation/escape mappings.
- Attached punctuation no longer causes an entire Roman phrase to bypass transliteration.
- ASCII digits are rendered as Bengali digits in the classic Avro fallback.

The project still does not claim exhaustive byte-for-byte parity for every historical Avro grammar rule. The renderer remains isolated behind `BanglaPhoneticTransliterator` so remaining rules can be expanded safely.

## Dictionary quality benchmark

`typing_stage10_dictionary_benchmark.kt` reports dictionary size, blank/invalid entries, case-normalization conflicts, exact dictionary mismatches, and a non-gating local throughput sample. Hard quality gates reject blank entries, normalization conflicts, or dictionary lookup mismatches.

## Safety boundary

No AI, backend, context-access, or safety subsystem is changed by Stage 10.

## Build-environment note

The inherited Stage 9 source contains `gradle-wrapper.properties` but does not contain `gradle/wrapper/gradle-wrapper.jar`. In this environment the official wrapper JAR cannot be fetched because outbound DNS/download access is blocked, so a full `./gradlew test` cannot start here. Stage 10 does not fabricate or vendor an unverified wrapper binary. All Android-free typing regressions, static release checks, XML checks, and protected-source hash checks are run independently.
