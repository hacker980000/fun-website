#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]

def read(rel):
    path = root / rel
    return path.read_text(encoding="utf-8") if path.exists() else ""

helper = read("app/src/main/java/com/socialaiassistant/keyboard/ime/Stage21PredictionProfiling.kt")
english = read("app/src/main/java/com/socialaiassistant/keyboard/ime/EnglishTypingEngine.kt")
bangla = read("app/src/main/java/com/socialaiassistant/keyboard/ime/OfflineBanglaSuggestionEngine.kt")
next_word = read("app/src/main/java/com/socialaiassistant/keyboard/ime/BanglaNextWordModel.kt")
service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
selftest = read("scripts/typing_stage21_prediction_coldstart_selftest.kt")
ci = read("scripts/stage21_ci_prediction_coldstart.sh")
workflow = read(".github/workflows/android-build.yml")
doc = read("docs/STAGE21_PREDICTION_COLDSTART_TUNING.md")

checks = {
    "central ranking tuning policy": "object PredictionRankingPolicy" in helper,
    "safe English alias gets ranking priority": "ENGLISH_ALIAS_BASE = 4_000" in helper and
        "PredictionRankingPolicy.ENGLISH_ALIAS_BASE + learning.wordBoost(alias)" in english,
    "English prefix completion penalty": "ENGLISH_PREFIX_COMPLETION_PENALTY" in helper and
        "PredictionRankingPolicy.completionPenalty" in english,
    "English typo length penalty": "ENGLISH_TYPO_LENGTH_DELTA_PENALTY" in helper and
        "kotlin.math.abs(entry.word.length - normalized.length)" in english,
    "English quality sort happens before dedupe": ".sortedWith(" in english and
        ".distinctBy { it.text.lowercase() }" in english and
        english.index(".sortedWith(", english.index("val candidates = scored")) < english.index(".distinctBy { it.text.lowercase() }", english.index("val candidates = scored")),
    "English deterministic source tie-break": "englishKindPriority" in helper and "PredictionRankingPolicy.englishKindPriority" in english,
    "Bangla deterministic source tie-break": "banglaKindPriority" in helper and "PredictionRankingPolicy.banglaKindPriority" in bangla,
    "English next-word weights centralized": "englishNextWordScore" in helper and "PredictionRankingPolicy.englishNextWordScore" in english,
    "Bangla next-word weights centralized": "banglaNextWordScore" in helper and "PredictionRankingPolicy.banglaNextWordScore" in next_word,
    "one-shot cold-start profiler": "class ColdStartProfiler" in helper and "if (!emitted.add(milestone)) return null" in helper,
    "cold-start profiler stores only timing/fixed milestones": "elapsedMicros" in helper and "typed text" in helper.lower(),
    "learning-model ready hook": "ColdStartMilestone.LEARNING_MODELS_READY" in service,
    "service ready hook": "ColdStartMilestone.SERVICE_READY" in service,
    "first input-view hook": "ColdStartMilestone.FIRST_INPUT_VIEW_READY" in service,
    "first suggestion hook": "ColdStartMilestone.FIRST_SUGGESTION_READY" in service,
    "prediction presentation evidence excludes text": "prediction_present language=" in service and "candidate.text" not in service.split("private fun recordStage21PredictionPresented", 1)[1].split("private fun recordStage21PredictionAccepted", 1)[0],
    "prediction acceptance evidence records rank only": "prediction_accept rank=" in service and "candidate.text" not in service.split("private fun recordStage21PredictionAccepted", 1)[1].split("private fun recordDebugRuntime", 1)[0],
    "Stage21 self-test covers alias and cold start": "safe alias should be the first visible candidate" in selftest and "cold-start milestones must be one-shot" in selftest,
    "Stage21 CI wraps Stage20": "base_stage20=PASS" in ci and "stage20_ci_resize_memory.sh" in ci,
    "Stage21 CI collects cold-start evidence": "cold_start milestone=" in ci and "cold-start-round" in ci,
    "Stage21 CI validates deterministic predictions": "english_thank_you_prediction=PASS" in ci and "bangla_ami_prediction=PASS" in ci,
    "Stage21 CI retains crash/ANR hard gate": "FATAL EXCEPTION|ANR in" in ci,
    "workflow runs Stage21 verifier": "verify_typing_stage21.py" in workflow,
    "workflow runs Stage21 device wrapper": "stage21_ci_prediction_coldstart.sh" in workflow,
    "Stage21 documentation present": "Stage 21" in doc and "No uncollected real-device result" in doc,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
if failed:
    print("Stage21 verification failed: " + ", ".join(failed), file=sys.stderr)
    sys.exit(1)
print(f"Stage21 static verification: {len(checks)}/{len(checks)} PASS")
