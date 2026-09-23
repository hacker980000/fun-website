#!/usr/bin/env python3
"""Parse Stage 23 ADB evidence into stable machine-readable baseline files.

This parser intentionally works only with fixed timing/resource metadata. It does not
read editor text, suggestion text, clipboard data, conversation context, or package
content from user apps.
"""
from __future__ import annotations

import csv
import json
import math
import re
import statistics
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

AM_KEYS = ("ThisTime", "TotalTime", "WaitTime")
MILESTONES = (
    "learning_models_ready",
    "service_ready",
    "first_input_view_ready",
    "first_suggestion_ready",
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""


def parse_am_start(text: str) -> Dict[str, Optional[float]]:
    out: Dict[str, Optional[float]] = {f"{key}_ms": None for key in AM_KEYS}
    for key in AM_KEYS:
        m = re.search(rf"(?m)^\s*{re.escape(key)}:\s*(\d+)\s*$", text)
        if m:
            out[f"{key}_ms"] = float(m.group(1))
    return out


def parse_cold_milestones(text: str) -> Dict[str, Optional[float]]:
    out: Dict[str, Optional[float]] = {name: None for name in MILESTONES}
    pattern = re.compile(r"cold_start milestone=([a-z_]+) elapsed_ms=([0-9.]+)")
    for name, value in pattern.findall(text):
        if name in out:
            out[name] = float(value)
    return out


def parse_total_pss_kb(text: str) -> Optional[int]:
    patterns = (
        r"(?m)^\s*TOTAL PSS:\s*(\d+)",
        r"(?m)^\s*TOTAL\s+(\d+)\s+\d+\s+\d+",
    )
    for pattern in patterns:
        m = re.search(pattern, text)
        if m:
            return int(m.group(1))
    return None


def parse_gfx_framestats(text: str) -> Dict[str, Optional[float]]:
    valid_durations_ms: List[float] = []
    in_profile = False
    header: Optional[List[str]] = None
    for raw in text.splitlines():
        line = raw.strip()
        if line == "---PROFILEDATA---":
            in_profile = not in_profile
            if in_profile:
                header = None
            continue
        if not in_profile or not line:
            continue
        if line.startswith("Flags,"):
            header = [item.strip() for item in line.split(",")]
            continue
        if header is None or "," not in line:
            continue
        values = [item.strip() for item in line.split(",")]
        if len(values) < len(header):
            continue
        row = dict(zip(header, values))
        try:
            if int(row.get("Flags", "1")) != 0:
                continue
            intended = int(row["IntendedVsync"])
            completed = int(row["FrameCompleted"])
        except (KeyError, TypeError, ValueError):
            continue
        if intended <= 0 or completed <= intended:
            continue
        duration_ms = (completed - intended) / 1_000_000.0
        # Ignore impossible/invalid outliers produced by partially initialized rows.
        if 0.0 < duration_ms < 10_000.0:
            valid_durations_ms.append(duration_ms)

    if not valid_durations_ms:
        return {
            "frame_count": 0,
            "frame_p50_ms": None,
            "frame_p95_ms": None,
            "frames_over_16_7ms": 0,
            "frames_over_33_3ms": 0,
        }

    ordered = sorted(valid_durations_ms)
    p95_index = max(0, math.ceil(len(ordered) * 0.95) - 1)
    return {
        "frame_count": len(ordered),
        "frame_p50_ms": float(statistics.median(ordered)),
        "frame_p95_ms": float(ordered[p95_index]),
        "frames_over_16_7ms": sum(v > 16.7 for v in ordered),
        "frames_over_33_3ms": sum(v > 33.3 for v in ordered),
    }


def _round_numbers(root: Path) -> List[int]:
    values = []
    for path in root.glob("round-*-cold-am-start.txt"):
        m = re.search(r"round-(\d+)-cold-am-start\.txt$", path.name)
        if m:
            values.append(int(m.group(1)))
    return sorted(set(values))


def _median(values: Iterable[Optional[float]]) -> Optional[float]:
    clean = [float(v) for v in values if v is not None]
    return float(statistics.median(clean)) if clean else None


def _maximum(values: Iterable[Optional[float]]) -> Optional[float]:
    clean = [float(v) for v in values if v is not None]
    return max(clean) if clean else None


def build_report(root: Path) -> Tuple[List[Dict[str, object]], Dict[str, object]]:
    rows: List[Dict[str, object]] = []
    for round_no in _round_numbers(root):
        cold = parse_am_start(_read(root / f"round-{round_no}-cold-am-start.txt"))
        warm = parse_am_start(_read(root / f"round-{round_no}-warm-am-start.txt"))
        milestones = parse_cold_milestones(_read(root / f"round-{round_no}-logcat.txt"))
        pss = parse_total_pss_kb(_read(root / f"round-{round_no}-meminfo.txt"))
        gfx = parse_gfx_framestats(_read(root / f"round-{round_no}-gfxinfo.txt"))
        rows.append({
            "round": round_no,
            "cold_this_ms": cold["ThisTime_ms"],
            "cold_total_ms": cold["TotalTime_ms"],
            "cold_wait_ms": cold["WaitTime_ms"],
            "warm_this_ms": warm["ThisTime_ms"],
            "warm_total_ms": warm["TotalTime_ms"],
            "warm_wait_ms": warm["WaitTime_ms"],
            "learning_models_ready_ms": milestones["learning_models_ready"],
            "service_ready_ms": milestones["service_ready"],
            "first_input_view_ready_ms": milestones["first_input_view_ready"],
            "first_suggestion_ready_ms": milestones["first_suggestion_ready"],
            "total_pss_kb": pss,
            **gfx,
        })

    metadata: Dict[str, str] = {}
    for line in _read(root / "device.properties").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            metadata[key.strip()] = value.strip()

    summary: Dict[str, object] = {
        "stage": 23,
        "rounds": len(rows),
        "device": metadata,
        "cold_total_median_ms": _median(row["cold_total_ms"] for row in rows),
        "cold_total_max_ms": _maximum(row["cold_total_ms"] for row in rows),
        "warm_total_median_ms": _median(row["warm_total_ms"] for row in rows),
        "first_suggestion_median_ms": _median(row["first_suggestion_ready_ms"] for row in rows),
        "first_suggestion_max_ms": _maximum(row["first_suggestion_ready_ms"] for row in rows),
        "total_pss_median_kb": _median(row["total_pss_kb"] for row in rows),
        "total_pss_max_kb": _maximum(row["total_pss_kb"] for row in rows),
        "frame_count_total": int(sum(int(row["frame_count"] or 0) for row in rows)),
        "frames_over_16_7ms_total": int(sum(int(row["frames_over_16_7ms"] or 0) for row in rows)),
        "frames_over_33_3ms_total": int(sum(int(row["frames_over_33_3ms"] or 0) for row in rows)),
        "frame_p95_max_ms": _maximum(row["frame_p95_ms"] for row in rows),
        "threshold_policy": "evidence_only_no_latency_memory_or_frame_budget_is_hard_gated_at_stage23",
        "privacy": "fixed_control_fixtures_and_resource_timing_only_no_real_user_text",
    }
    return rows, summary


def write_report(root: Path) -> None:
    rows, summary = build_report(root)
    if not rows:
        raise SystemExit("no Stage 23 round evidence found")

    columns = list(rows[0].keys())
    with (root / "baseline-metrics.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)

    (root / "baseline-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    with (root / "baseline-summary.txt").open("w", encoding="utf-8") as handle:
        handle.write("stage=23\n")
        handle.write(f"rounds={summary['rounds']}\n")
        for key in (
            "cold_total_median_ms", "cold_total_max_ms", "warm_total_median_ms",
            "first_suggestion_median_ms", "first_suggestion_max_ms",
            "total_pss_median_kb", "total_pss_max_kb", "frame_count_total",
            "frames_over_16_7ms_total", "frames_over_33_3ms_total", "frame_p95_max_ms",
        ):
            handle.write(f"{key}={summary[key]}\n")
        handle.write(f"threshold_policy={summary['threshold_policy']}\n")
        handle.write(f"privacy={summary['privacy']}\n")


def main(argv: List[str]) -> int:
    if len(argv) != 2:
        print(f"usage: {argv[0]} <stage23-report-dir>", file=sys.stderr)
        return 2
    write_report(Path(argv[1]).resolve())
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
