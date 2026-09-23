#!/usr/bin/env python3
"""Merge one or more Stage 23 baseline-summary.json files into a matrix index."""
from __future__ import annotations

import csv
import json
import sys
from pathlib import Path

FIELDS = (
    "label", "api", "android_release", "manufacturer", "model", "is_emulator",
    "rounds", "cold_total_median_ms", "first_suggestion_median_ms",
    "total_pss_median_kb", "frame_count_total", "frames_over_16_7ms_total",
    "frames_over_33_3ms_total", "frame_p95_max_ms",
)


def main(argv):
    if len(argv) != 3:
        print(f"usage: {argv[0]} <artifact-root> <output-dir>", file=sys.stderr)
        return 2
    source = Path(argv[1]).resolve()
    out = Path(argv[2]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    paths = sorted(source.rglob("baseline-summary.json"))
    if not paths:
        raise SystemExit("no Stage 23 baseline-summary.json files found")
    rows = []
    for path in paths:
        data = json.loads(path.read_text(encoding="utf-8"))
        device = data.get("device") or {}
        rows.append({
            "label": device.get("label", path.parent.name),
            "api": device.get("api", ""),
            "android_release": device.get("android_release", ""),
            "manufacturer": device.get("manufacturer", ""),
            "model": device.get("model", ""),
            "is_emulator": device.get("is_emulator", ""),
            "rounds": data.get("rounds", ""),
            "cold_total_median_ms": data.get("cold_total_median_ms", ""),
            "first_suggestion_median_ms": data.get("first_suggestion_median_ms", ""),
            "total_pss_median_kb": data.get("total_pss_median_kb", ""),
            "frame_count_total": data.get("frame_count_total", ""),
            "frames_over_16_7ms_total": data.get("frames_over_16_7ms_total", ""),
            "frames_over_33_3ms_total": data.get("frames_over_33_3ms_total", ""),
            "frame_p95_max_ms": data.get("frame_p95_max_ms", ""),
        })
    rows.sort(key=lambda row: (str(row["api"]), str(row["label"])))
    with (out / "stage23-device-matrix.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    (out / "stage23-device-matrix.json").write_text(
        json.dumps({
            "stage": 23,
            "devices": rows,
            "interpretation": "baseline evidence only; Stage 23 does not declare latency, memory, or frame winners/failures",
        }, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(f"Merged {len(rows)} Stage 23 device baseline(s).")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
