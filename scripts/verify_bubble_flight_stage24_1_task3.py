from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ctx=ROOT/'app/src/main/java/com/socialaiassistant/keyboard/context'
checks=[]
def req(n,o): checks.append((n,bool(o)))
mapper=(ctx/'BubbleAccessibilityTargetMapper.kt').read_text() if (ctx/'BubbleAccessibilityTargetMapper.kt').exists() else ''
path=(ctx/'BubbleFlightPath.kt').read_text() if (ctx/'BubbleFlightPath.kt').exists() else ''
host=(ctx/'AccessibilityBubbleOverlayWindowHost.kt').read_text() if (ctx/'AccessibilityBubbleOverlayWindowHost.kt').exists() else ''
renderer=(ctx/'BubbleFlightOverlayRenderer.kt').read_text() if (ctx/'BubbleFlightOverlayRenderer.kt').exists() else ''
req('bounds mapper exists', bool(mapper))
req('bounds mapper validates dimensions', 'bounds.width() <= 0' in mapper and 'bounds.height() <= 0' in mapper)
req('bezier path exists', 'object BubbleFlightPath' in path and 'controlY' in path)
req('accessibility overlay type', 'TYPE_ACCESSIBILITY_OVERLAY' in host)
req('overlay non focusable', 'FLAG_NOT_FOCUSABLE' in host)
req('overlay non touchable', 'FLAG_NOT_TOUCHABLE' in host)
req('layout in screen', 'FLAG_LAYOUT_IN_SCREEN' in host)
req('no system alert permission reference', 'SYSTEM_ALERT_WINDOW' not in host and 'TYPE_APPLICATION_OVERLAY' not in host)
req('host catches security', 'SecurityException' in host and 'BadTokenException' in host and 'IllegalStateException' in host)
req('renderer bounded eight', 'MAX_SIMULTANEOUS_BUBBLES' in renderer)
req('renderer uses linked active map', 'LinkedHashMap<Long, ActiveFlight>' in renderer)
req('renderer pools views', 'idleBubbles' in renderer)
req('renderer value animator', 'ValueAnimator.ofFloat(0f, 1f)' in renderer)
req('renderer bezier', 'BubbleFlightPath.pointAt' in renderer)
req('renderer retarget gate', 'RETARGET_MAX_FRACTION' in renderer and 'retargeted' in renderer)
req('renderer cancel editor', 'fun cancelEditor' in renderer)
req('renderer idle detach', 'host.detach()' in renderer)
req('renderer theme aware', 'request.theme' in renderer and 'primaryNeon' in renderer)
for n,o in checks: print(('PASS' if o else 'FAIL')+': '+n)
print(f"Stage 24.1 Task 3 contract: {sum(o for _,o in checks)}/{len(checks)} PASS")
if not all(o for _,o in checks): raise SystemExit(1)
