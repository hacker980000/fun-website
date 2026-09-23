#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "stage23_parse_device_evidence.py"
spec = importlib.util.spec_from_file_location("stage23_parser", MODULE_PATH)
parser = importlib.util.module_from_spec(spec)
assert spec and spec.loader
spec.loader.exec_module(parser)

checks = 0

def expect(condition, message):
    global checks
    if not condition:
        raise AssertionError(message)
    checks += 1
    print(f"PASS: {message}")

am = parser.parse_am_start("Status: ok\nThisTime: 120\nTotalTime: 140\nWaitTime: 150\n")
expect(am["ThisTime_ms"] == 120.0, "parse ThisTime")
expect(am["TotalTime_ms"] == 140.0, "parse TotalTime")
expect(am["WaitTime_ms"] == 150.0, "parse WaitTime")
expect(parser.parse_am_start("Status: ok\n")["TotalTime_ms"] is None, "missing am-start timing is tolerated")

cold = parser.parse_cold_milestones(
    "cold_start milestone=learning_models_ready elapsed_ms=11.5\n"
    "cold_start milestone=service_ready elapsed_ms=20\n"
    "cold_start milestone=first_input_view_ready elapsed_ms=31.25\n"
    "cold_start milestone=first_suggestion_ready elapsed_ms=44.75\n"
)
expect(cold["learning_models_ready"] == 11.5, "parse learning-model milestone")
expect(cold["service_ready"] == 20.0, "parse service milestone")
expect(cold["first_input_view_ready"] == 31.25, "parse input-view milestone")
expect(cold["first_suggestion_ready"] == 44.75, "parse first-suggestion milestone")

expect(parser.parse_total_pss_kb("TOTAL PSS: 45678 TOTAL RSS: 60000") == 45678, "parse modern TOTAL PSS")
expect(parser.parse_total_pss_kb(" TOTAL   12345  999  111") == 12345, "parse table TOTAL PSS fallback")
expect(parser.parse_total_pss_kb("no memory row") is None, "missing memory row is tolerated")

gfx = """---PROFILEDATA---
Flags,IntendedVsync,Vsync,OldestInputEvent,NewestInputEvent,HandleInputStart,AnimationStart,PerformTraversalsStart,DrawStart,FrameDeadline,FrameInterval,FrameStartTime,SyncQueued,SyncStart,IssueDrawCommandsStart,SwapBuffers,FrameCompleted,DequeueBufferDuration,QueueBufferDuration,GpuCompleted,SwapBuffersCompleted,DisplayPresentTime,CommandSubmissionCompleted
0,1000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1010000000,0,0,0,0,0,0
0,2000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2020000000,0,0,0,0,0,0
0,3000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3040000000,0,0,0,0,0,0
1,4000000000,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4100000000,0,0,0,0,0,0
---PROFILEDATA---
"""
g = parser.parse_gfx_framestats(gfx)
expect(g["frame_count"] == 3, "ignore nonzero-flag frame rows")
expect(g["frames_over_16_7ms"] == 2, "count 16.7ms frame proxy")
expect(g["frames_over_33_3ms"] == 1, "count 33.3ms frame proxy")
expect(g["frame_p50_ms"] == 20.0, "calculate frame median")
expect(g["frame_p95_ms"] == 40.0, "calculate frame p95")

with tempfile.TemporaryDirectory() as tmp:
    root = Path(tmp)
    root.joinpath("device.properties").write_text("api=30\nmodel=fixture\nlabel=selftest\n")
    for round_no, total, warm, pss in ((1, 140, 35, 40000), (2, 160, 45, 42000), (3, 150, 40, 41000)):
        root.joinpath(f"round-{round_no}-cold-am-start.txt").write_text(
            f"Status: ok\nThisTime: {total-10}\nTotalTime: {total}\nWaitTime: {total+5}\n"
        )
        root.joinpath(f"round-{round_no}-warm-am-start.txt").write_text(
            f"Status: ok\nThisTime: {warm}\nTotalTime: {warm}\nWaitTime: {warm+2}\n"
        )
        root.joinpath(f"round-{round_no}-logcat.txt").write_text(
            "cold_start milestone=learning_models_ready elapsed_ms=10\n"
            "cold_start milestone=service_ready elapsed_ms=20\n"
            "cold_start milestone=first_input_view_ready elapsed_ms=30\n"
            f"cold_start milestone=first_suggestion_ready elapsed_ms={50+round_no}\n"
        )
        root.joinpath(f"round-{round_no}-meminfo.txt").write_text(f"TOTAL PSS: {pss}\n")
        root.joinpath(f"round-{round_no}-gfxinfo.txt").write_text(gfx)
    parser.write_report(root)
    summary = json.loads(root.joinpath("baseline-summary.json").read_text())
    expect(root.joinpath("baseline-metrics.csv").exists(), "write baseline CSV")
    expect(root.joinpath("baseline-summary.txt").exists(), "write text summary")
    expect(summary["rounds"] == 3, "summary round count")
    expect(summary["cold_total_median_ms"] == 150.0, "summary cold-start median")
    expect(summary["warm_total_median_ms"] == 40.0, "summary warm-start median")
    expect(summary["total_pss_median_kb"] == 41000.0, "summary memory median")
    expect(summary["frame_count_total"] == 9, "summary frame count")
    expect("evidence_only" in summary["threshold_policy"], "summary does not invent tuning thresholds")
    expect("no_real_user_text" in summary["privacy"], "summary records privacy boundary")

print(f"Stage23 evidence parser self-test: {checks}/{checks} PASS")
