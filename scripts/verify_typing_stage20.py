#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
service = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt").read_text()
helper = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/Stage20ResizeMemoryHardening.kt").read_text()
bg = (root / "app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManager.kt").read_text()
workflow = (root / ".github/workflows/android-build.yml").read_text()
ci_path = root / "scripts/stage20_ci_resize_memory.sh"
ci = ci_path.read_text() if ci_path.exists() else ""

finish_view = service.split("override fun onFinishInputView", 1)[1].split("override fun onUpdateSelection", 1)[0]

checks = {
    "toolbar reorder gate helper": "class ToolbarOrderGate" in helper and "ToolbarOrderSignature" in helper,
    "toolbar uses stable-order gate": "toolbarOrderGate.shouldReorder" in service and "runtime_render_coalesced target=toolbar" in service,
    "normal hide/show keeps populated rows": "keyboardRenderGate.invalidate()" not in finish_view and "glideKeyTargets.clear()" not in finish_view,
    "new input view invalidates row/background/toolbar gates": service.count("toolbarOrderGate.invalidate()") >= 2 and service.count("backgroundRenderGate.invalidate()") >= 2,
    "configuration work is frame-coalesced": "configurationFrameCoalescer" in service and "postOnAnimation(configurationRenderRunnable)" in service,
    "configuration pending work cancels on lifecycle": service.count("cancelPendingConfigurationRender()") >= 2,
    "background request gate helper": "class BackgroundRenderGate" in helper and "LOAD_KEEP_VISIBLE" in helper,
    "background decode target buckets": "object BackgroundDecodePolicy" in helper and "DEFAULT_BUCKET_PX = 64" in helper,
    "low-ram background caps": "LOW_RAM_MAX_WIDTH = 1024" in helper and "LOW_RAM_MAX_HEIGHT = 640" in helper,
    "IME uses low-ram device signal": "isLowRamDevice" in service and "BackgroundDecodePolicy.target" in service,
    "same background request coalesces": "runtime_render_coalesced target=background" in service,
    "resize keeps existing background visible": "BackgroundRenderDecision.LOAD_KEEP_VISIBLE -> Unit" in service,
    "background cache explicit trim hook": "fun clearMemoryCache()" in bg and service.count("clearMemoryCache()") >= 3,
    "UI-hidden background is actually detached": "releaseDisplayedThemeBackgroundForMemoryPressure(\"ui_hidden\")" in service,
    "bounded immutable layout cache": "BoundedValueCache<KeyboardLayoutRequest, KeyboardLayout>" in service and "MAX_LAYOUT_CACHE_ENTRIES = 10" in service,
    "layout cache participates in render": "keyboardLayoutCache.getOrPut(layoutRequest)" in service,
    "layout cache clears under memory pressure": "keyboardLayoutCache.clear()" in service and "resource=layout_background_cache" in service,
    "stage20 CI wrapper": "base_stage19=PASS" in ci and "orientation_burst_process_survival=PASS" in ci,
    "workflow runs stage20 verifier": "verify_typing_stage20.py" in workflow,
    "workflow runs stage20 device wrapper": "stage20_ci_resize_memory.sh" in workflow,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
if failed:
    print("Stage20 verification failed: " + ", ".join(failed), file=sys.stderr)
    sys.exit(1)
print(f"Stage20 static verification: {len(checks)}/{len(checks)} PASS")
