#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
service = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt").read_text()
bangla = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/ImeTypingEngine.kt").read_text()
english = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/EnglishTypingEngine.kt").read_text()
cache = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/Stage18RenderWorkCache.kt").read_text()
ci_script = (root / "scripts/stage18_ci_render_latency.sh").read_text()
workflow = (root / ".github/workflows/android-build.yml").read_text()
checks = {
    "keyboard render gate": "KeyboardRenderGate" in service and "shouldRebuild(signature, rowsContainer.childCount)" in service,
    "suggestion memo": "suggestionMemo.getOrCompute(key)" in service,
    "suggestion bar coalescing": "SuggestionBarRenderGate" in service and "runtime_render_coalesced target=suggestions" in service,
    "keyboard coalescing marker": "runtime_render_coalesced target=keyboard" in service,
    "bangla revision": "fun suggestionRevision(): Long" in bangla and "bumpSuggestionRevision()" in bangla,
    "english revision": "fun suggestionRevision(): Long" in english and "bumpSuggestionRevision()" in english,
    "theme avoids unconditional dynamic rebuild": "applyTheme(root, rebuildDynamic = false)" in service,
    "bounded one-entry memo": "class SingleEntryMemo" in cache and "private var value: V? = null" in cache,
    "memory pressure clears suggestion memo": "suggestionMemo.clear()" in service,
    "stage18 ci evidence script": "base_stage17=PASS" in ci_script and "runtime_render_coalesced" in ci_script,
    "workflow retains stage18 evidence chain": ("stage18_ci_render_latency.sh" in workflow or "stage19_ci_touch_responsiveness.sh" in workflow) and ("stage18-api36-render-latency-report" in workflow or "stage19-api36-touch-responsiveness-report" in workflow),
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
if failed:
    print("Stage18 verification failed: " + ", ".join(failed), file=sys.stderr)
    sys.exit(1)
print(f"Stage18 static verification: {len(checks)}/{len(checks)} PASS")
