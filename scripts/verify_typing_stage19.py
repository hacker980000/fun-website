#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
service = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt").read_text()
policy = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/Stage19TouchResponsiveness.kt").read_text()
hitmap = (root / "app/src/main/java/com/socialaiassistant/keyboard/ime/GlideHitMap.kt").read_text()
layout = (root / "app/src/main/res/layout/ime_keyboard.xml").read_text()
workflow = (root / ".github/workflows/android-build.yml").read_text()
ci = (root / "scripts/stage19_ci_touch_responsiveness.sh").read_text() if (root / "scripts/stage19_ci_touch_responsiveness.sh").exists() else ""

checks = {
    "non-text action debounce helper": "class RapidActionGate" in policy and "DEFAULT_ACTION_INTERVAL_MS" in policy,
    "text keys are not routed through action gate": 'is KeyboardAction.Text -> {' in service and 'rapidActionGate.allow("text' not in service,
    "toolbar duplicate-tap guard": 'rapidActionGate.allow("toolbar:$id"' in service,
    "suggestion duplicate-tap guard": 'rapidActionGate.allow("suggestion"' in service,
    "panel duplicate-tap guard": 'rapidActionGate.allow("panel:$label"' in service,
    "feedback cadence guard": "FeedbackCadenceGate" in service and "allowHaptic" in service and "allowSound" in service,
    "Glide haptic starts from down-event cadence": "lastHapticEventTimeMs = event.eventTime" in service,
    "frame-coalesced suggestion render": "FrameWorkCoalescer" in service and "postOnAnimation(suggestionRenderRunnable)" in service,
    "pending suggestion work canceled on lifecycle": service.count("cancelPendingSuggestionRender()") >= 5,
    "system touch slop affects Glide start": "ViewConfiguration.get(this).scaledTouchSlop" in service and "glideStartThresholdPx" in service,
    "Glide key-gap tolerance": "GLIDE_HIT_TOLERANCE_DP" in service and "distanceSquaredTo" in hitmap,
    "48dp generated suggestion/actions": "MIN_ACTION_TOUCH_TARGET_DP" in service and "const val MIN_ACTION_TOUCH_TARGET_DP = 48" in policy,
    "48dp XML toolbar/action targets": layout.count('android:layout_height="48dp"') >= 8,
    "literal Latin duplicate flush removed": service.count("flushActiveComposition(autocorrect = false)\n                        flushActiveComposition(autocorrect = false)") == 0,
    "stage19 CI wrapper": "base_stage18=PASS" in ci and "rapid_toolbar_double_tap=PASS" in ci,
    "workflow runs stage19": "stage19_ci_touch_responsiveness.sh" in workflow,
}
failed = [name for name, ok in checks.items() if not ok]
for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
if failed:
    print("Stage19 verification failed: " + ", ".join(failed), file=sys.stderr)
    sys.exit(1)
print(f"Stage19 static verification: {len(checks)}/{len(checks)} PASS")
