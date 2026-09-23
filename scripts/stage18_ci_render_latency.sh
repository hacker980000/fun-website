#!/usr/bin/env bash
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
OUT_DIR="${2:-stage18-render-latency-report}"
BASE_DIR="${OUT_DIR}/stage17-base"
mkdir -p "$OUT_DIR"

# Keep the complete Stage 17 functional/runtime/memory validation as the base gate.
bash scripts/stage17_ci_memory_latency.sh "$APK" "$BASE_DIR"

SOURCE_LOG="$BASE_DIR/logcat-runtime-filter.txt"
KEYBOARD_COALESCED="$(grep -c 'runtime_render_coalesced target=keyboard' "$SOURCE_LOG" 2>/dev/null || true)"
SUGGESTION_COALESCED="$(grep -c 'runtime_render_coalesced target=suggestions' "$SOURCE_LOG" 2>/dev/null || true)"
SUGGESTION_METRICS="$(grep -c 'runtime_metric metric=suggestion_compute' "$SOURCE_LOG" 2>/dev/null || true)"
KEYBOARD_METRICS="$(grep -c 'runtime_metric metric=keyboard_render' "$SOURCE_LOG" 2>/dev/null || true)"

{
  echo "stage=18"
  echo "base_stage17=PASS"
  echo "keyboard_render_coalesced_markers=$KEYBOARD_COALESCED"
  echo "suggestion_render_coalesced_markers=$SUGGESTION_COALESCED"
  echo "keyboard_render_metric_reports=$KEYBOARD_METRICS"
  echo "suggestion_compute_metric_reports=$SUGGESTION_METRICS"
  echo "cache_privacy_note=single_entry_in_memory_only_no_disk_or_backend_persistence"
  echo "evidence_note=coalesced_marker_counts_are_diagnostic_not_release_thresholds"
} > "$OUT_DIR/summary.txt"

# Preserve a small Stage 18 focused log artifact without duplicating all Stage 17 evidence.
grep -E 'runtime_render_coalesced|runtime_metric metric=(keyboard_render|suggestion_compute)' "$SOURCE_LOG" \
  > "$OUT_DIR/render-latency-markers.txt" || true

echo "PASS: Stage 18 render/suggestion latency evidence collected."
cat "$OUT_DIR/summary.txt"
