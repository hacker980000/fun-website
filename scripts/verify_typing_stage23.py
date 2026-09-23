#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]

def read(rel):
    path = root / rel
    return path.read_text(encoding="utf-8") if path.exists() else ""

ci = read("scripts/stage23_ci_real_device_baseline.sh")
parser = read("scripts/stage23_parse_device_evidence.py")
merge = read("scripts/stage23_merge_device_evidence.py")
selftest = read("scripts/stage23_evidence_parser_selftest.py")
workflow = read(".github/workflows/android-build.yml")
doc = read("docs/STAGE23_REAL_DEVICE_EVIDENCE_BASELINE.md")
readme = read("README.md")

checks = {
    "Stage23 device wrapper exists": "stage22_ci_autocorrect_quality.sh" in ci,
    "Stage23 preserves Stage22 hard chain": "base_stage22=PASS" in ci,
    "Stage23 defaults to five evidence rounds": 'ROUNDS="${STAGE23_ROUNDS:-5}"' in ci,
    "Stage23 bounds configurable rounds": "ROUNDS >= 3 && ROUNDS <= 20" in ci,
    "Stage23 collects API metadata": "ro.build.version.sdk" in ci and "ro.build.version.release" in ci,
    "Stage23 collects model metadata": "ro.product.manufacturer" in ci and "ro.product.model" in ci,
    "Stage23 avoids device serial evidence": "device_serial_collected=NO" in ci,
    "Stage23 cold starts each round": 'am force-stop "$PACKAGE"' in ci and 'round-${round}-cold-am-start.txt' in ci,
    "Stage23 captures warm relaunch": "KEYCODE_HOME" in ci and 'round-${round}-warm-am-start.txt' in ci,
    "Stage23 keeps fixed prediction fixture": "controlled 'thank -> you'" in ci and 'grep -Fxq "you"' in ci,
    "Stage23 requires all cold milestones": "learning_models_ready service_ready first_input_view_ready first_suggestion_ready" in ci,
    "Stage23 collects meminfo": 'dumpsys meminfo "$PACKAGE"' in ci,
    "Stage23 collects gfx framestats": 'dumpsys gfxinfo "$PACKAGE" framestats' in ci,
    "Stage23 keeps crash/ANR hard gate": "FATAL EXCEPTION|ANR in" in ci,
    "Stage23 invokes evidence parser": "stage23_parse_device_evidence.py" in ci,
    "Stage23 has no hard latency budget": "hard_latency_budget=NONE" in ci,
    "Stage23 has no hard memory budget": "hard_memory_budget=NONE" in ci,
    "Stage23 has no hard frame budget": "hard_frame_budget=NONE" in ci,
    "parser reads am-start timings": "parse_am_start" in parser and "TotalTime" in parser and "WaitTime" in parser,
    "parser reads cold-start markers": "parse_cold_milestones" in parser and "first_suggestion_ready" in parser,
    "parser reads total PSS": "parse_total_pss_kb" in parser and "TOTAL PSS" in parser,
    "parser reads framestats": "parse_gfx_framestats" in parser and "FrameCompleted" in parser,
    "parser writes normalized CSV": "baseline-metrics.csv" in parser,
    "parser writes JSON summary": "baseline-summary.json" in parser,
    "parser labels thresholds evidence-only": "evidence_only_no_latency_memory_or_frame_budget_is_hard_gated_at_stage23" in parser,
    "parser self-test covers summary": "Stage23 evidence parser self-test" in selftest and "summary cold-start median" in selftest,
    "matrix merger exists": "stage23-device-matrix.csv" in merge and "baseline-summary.json" in merge,
    "matrix merger avoids winner claim": "does not declare latency, memory, or frame winners/failures" in merge,
    "workflow runs Stage23 verifier": "verify_typing_stage23.py" in workflow,
    "workflow runs Stage23 parser self-test": "stage23_evidence_parser_selftest.py" in workflow,
    "workflow runs Stage23 API36 wrapper": "stage23-smoke-api36-real-device-baseline-report" in workflow and "stage23_ci_real_device_baseline.sh" in workflow,
    "workflow runs Stage23 API matrix wrapper": "stage23-api${{ matrix.api-level }}-real-device-baseline-report" in workflow,
    "workflow merges manual matrix evidence": "stage23_merge_device_evidence.py" in workflow and "stage23-device-matrix-summary" in workflow,
    "Stage23 documentation explains evidence boundary": "no real-device timing, memory, or frame result is claimed" in doc,
    "Stage23 documentation explains physical-device procedure": "STAGE23_DEVICE_LABEL=physical-lowend" in doc,
    "README advertises Stage23 baseline": "Typing Core v2 Stage 23" in readme,
}

failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
if failed:
    print("Stage23 verification failed: " + ", ".join(failed), file=sys.stderr)
    sys.exit(1)
print(f"Stage23 static verification: {len(checks)}/{len(checks)} PASS")
