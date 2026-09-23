#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]

def read(rel):
    path = root / rel
    return path.read_text(encoding="utf-8") if path.exists() else ""

policy = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage22PredictionConfidence.kt")
english = read("app/src/main/java/com/socialaiassistant/keyboard/ime/EnglishTypingEngine.kt")
bangla = read("app/src/main/java/com/socialaiassistant/keyboard/ime/OfflineBanglaSuggestionEngine.kt")
selftest = read("scripts/typing_stage22_autocorrect_quality_selftest.kt")
ci = read("scripts/stage22_ci_autocorrect_quality.sh")
workflow = read(".github/workflows/android-build.yml")
doc = read("docs/STAGE22_AUTOCORRECT_PREDICTION_QUALITY.md")
readme = read("README.md")

english_autocorrect_block = english.split("val autocorrect = when", 1)[1].split("val candidates = scored", 1)[0] if "val autocorrect = when" in english else ""

checks = {
    "central Stage22 autocorrect policy": "object PredictionAutocorrectPolicy" in policy,
    "English minimum token length gate": "ENGLISH_TYPO_MIN_LENGTH = 4" in policy,
    "English score-margin ambiguity gate": "ENGLISH_TYPO_MIN_MARGIN = 120" in policy and "runnerUpScore" in policy,
    "overflow-safe confidence subtraction": "topScore.toLong() - runnerUpScore.toLong()" in policy,
    "Bangla Stage21 safety envelope centralized": "BANGLA_TYPO_MIN_LENGTH = 5" in policy and "BANGLA_TYPO_MIN_MARGIN = 45" in policy,
    "English typo candidates are confidence-ranked": "val typoRanked = scored" in english and "EnglishSuggestionKind.TYPO" in english,
    "English uses confidence policy": "PredictionAutocorrectPolicy.isConfidentEnglishTypo" in english,
    "old English one-candidate autocorrect gate removed": "typos.size == 1" not in english,
    "English explicit aliases remain first autocorrect authority": "aliases.containsKey(normalized)" in english_autocorrect_block,
    "known exact English words remain protected": "index.exact(normalized) != null -> null" in english_autocorrect_block,
    "English ranking still sorts before dedupe": english.index(".sortedWith(candidateComparator)", english.index("val candidates = scored")) < english.index(".distinctBy { it.text.lowercase() }", english.index("val candidates = scored")),
    "Bangla uses centralized confidence policy": "PredictionAutocorrectPolicy.isConfidentBanglaTypo" in bangla,
    "old Bangla hard-coded safe typo constants removed": "SAFE_TYPO_AUTOCORRECT_MIN_LENGTH" not in bangla and "SAFE_TYPO_AUTOCORRECT_MARGIN" not in bangla,
    "Stage22 self-test covers confident multi-candidate typo": 'EnglishFixture("wdth", "with", "with")' in selftest,
    "Stage22 self-test covers ambiguous typo guard": 'EnglishFixture("shave", "save", null)' in selftest,
    "Stage22 self-test covers short typo guard": 'EnglishFixture("teh", "the", null)' in selftest,
    "Stage22 self-test preserves explicit alias": 'EnglishFixture("dont", "don\'t", "don\'t")' in selftest,
    "Stage22 self-test preserves English next-word": 'suggest("thank", 3)' in selftest and '== "you"' in selftest,
    "Stage22 self-test preserves Bangla ranking": "Bangla exact ranking must remain intact" in selftest and "Bangla safe alias must remain intact" in selftest,
    "Stage22 CI wraps Stage21": "stage21_ci_prediction_coldstart.sh" in ci and "base_stage21=PASS" in ci,
    "Stage22 CI validates explicit alias": 'assert_fixture "explicit_alias" "dont" "don\'t "' in ci,
    "Stage22 CI validates confident typo": 'assert_fixture "confident_multi_candidate_typo" "wdth" "with "' in ci,
    "Stage22 CI validates ambiguity guard": 'assert_fixture "ambiguous_typo_guard" "shave" "shave "' in ci,
    "Stage22 CI validates short-token guard": 'assert_fixture "short_typo_guard" "teh" "teh "' in ci,
    "Stage22 CI retains crash/ANR gate": "FATAL EXCEPTION|ANR in" in ci,
    "Stage22 CI notes controlled fixture privacy": "static_control_words" in ci,
    "workflow runs Stage22 verifier": "verify_typing_stage22.py" in workflow,
    "workflow runs Stage22 device wrapper": "stage22_ci_autocorrect_quality.sh" in workflow,
    "workflow archives Stage22 evidence": "stage22-api36-autocorrect-quality-report" in workflow and "stage22-api${{ matrix.api-level }}-autocorrect-quality-report" in workflow,
    "Stage22 documentation present": "Stage 22" in doc and "No real-device result is claimed" in doc,
    "README advertises Stage22 baseline": "Typing Core v2 Stage 22" in readme,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
if failed:
    print("Stage22 verification failed: " + ", ".join(failed), file=sys.stderr)
    sys.exit(1)
print(f"Stage22 static verification: {len(checks)}/{len(checks)} PASS")
