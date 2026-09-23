from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks = []

def require(name, condition):
    checks.append((name, bool(condition)))

def read(rel):
    path = ROOT / rel
    return path.read_text() if path.exists() else ''

models = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightModels.kt')
bus = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightBus.kt')
resolver = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolver.kt')
exact_refresh = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightExactTargetRefresh.kt')
coordinator = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTapCoordinator.kt')
ime = read('app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt')
local_renderer = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyEffectRenderer.kt')
policy = read('app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt')
accessibility = read('app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt')
overlay_host = read('app/src/main/java/com/socialaiassistant/keyboard/context/AccessibilityBubbleOverlayWindowHost.kt')
overlay_renderer = read('app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRenderer.kt')
path_math = read('app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightPath.kt')
mapper = read('app/src/main/java/com/socialaiassistant/keyboard/context/BubbleAccessibilityTargetMapper.kt')
theme_pack = read('app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePack.kt')
manifest = read('app/src/main/AndroidManifest.xml')

for rel in (
    'app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightBus.kt',
    'app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolver.kt',
    'app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRenderer.kt',
):
    require(f'file exists: {Path(rel).name}', (ROOT / rel).exists())

require('flight request stores exact key source', 'val source: BubbleFlightPoint' in models)
require('flight request stores editor token', 'val editor: BubbleFlightEditorToken' in models)
require('target source distinguishes cursor and accessibility', 'CURSOR_ANCHOR' in models and 'ACCESSIBILITY_BOUNDS' in models)
require('resolver exact first then fallback', 'exact?.takeIf' in resolver and '?: fallback?.takeIf' in resolver)
require('resolver rejects editor mismatch', 'target.editor != editor' in resolver)
require('resolver rejects non-finite target', '!target.point.isFinite()' in resolver)
require('resolver bounds target age', 'age in 0L..maxAgeMs' in resolver)
require('dispatch refresh prefers newest cursor', 'capturedAtUptimeMs >' in exact_refresh and 'BubbleFlightTargetSource.CURSOR_ANCHOR' in exact_refresh)
require('IME refreshes exact target after commit before dispatch', 'BubbleFlightExactTargetRefresh.choose' in ime)

require('IME receives cursor anchor updates', 'override fun onUpdateCursorAnchorInfo' in ime)
require('IME requests cursor updates', 'requestCursorUpdates' in ime)
require('IME captures key screen position', 'getLocationOnScreen' in ime)
require('IME uses physical key center x', 'location[0] + view.width / 2f' in ime)
require('IME uses physical key center y', 'location[1] + view.height / 2f' in ime)
require('text commit precedes bubble dispatch coordinator', 'commitThenDispatch' in coordinator and 'commit()' in coordinator and 'BubbleFlightBus.dispatch' in ime)
require('IME has local fallback', 'showLocal' in ime and 'showLocal' in local_renderer)
require('IME retargets latest cursor', 'BubbleFlightBus.retarget' in ime)
require('IME cancels old editor flights', 'BubbleFlightBus.cancelEditor' in ime)

require('accessibility service implements flight sink', 'BubbleFlightSink' in accessibility and ': AccessibilityService(), BubbleFlightSink' in accessibility)
require('accessibility service registers bus', 'BubbleFlightBus.register(this)' in accessibility)
require('accessibility service unregisters bus', 'BubbleFlightBus.unregister(this)' in accessibility)
require('accessibility fallback uses editable bounds mapper', 'BubbleAccessibilityTargetMapper.pointInside' in accessibility)
require('accessibility fallback rejects password node', 'node.isPassword' in accessibility)
require('accessibility fallback does not retain AccessibilityNodeInfo field', 'AccessibilityNodeInfo?' not in accessibility)
require('exact target and fallback go through resolver', 'bubbleTargetResolver.resolve' in accessibility)

require('overlay uses accessibility window type', 'TYPE_ACCESSIBILITY_OVERLAY' in overlay_host)
require('overlay is not focusable', 'FLAG_NOT_FOCUSABLE' in overlay_host)
require('overlay is not touchable', 'FLAG_NOT_TOUCHABLE' in overlay_host)
require('overlay renderer uses bezier path', 'BubbleFlightPath.pointAt' in overlay_renderer and 'fun pointAt(' in path_math and '2f * oneMinus * clamped' in path_math)
require('overlay renderer supports one smooth retarget', 'retargeted' in overlay_renderer and 'elapsedFraction' in overlay_renderer)
require('overlay renderer bounded by policy', 'MAX_SIMULTANEOUS_BUBBLES' in overlay_renderer)
require('overlay renderer pools views', 'idleBubbles' in overlay_renderer and 'activeFlights' in overlay_renderer)

prod_sources = '\n'.join(
    p.read_text()
    for p in (ROOT / 'app/src/main').rglob('*')
    if p.is_file() and p.suffix in {'.kt', '.xml', '.java'}
)
require('no SYSTEM_ALERT_WINDOW permission', 'SYSTEM_ALERT_WINDOW' not in manifest and 'SYSTEM_ALERT_WINDOW' not in prod_sources)
require('no TYPE_APPLICATION_OVERLAY production window', 'TYPE_APPLICATION_OVERLAY' not in prod_sources)
require('max simultaneous bubbles stays eight', 'MAX_SIMULTANEOUS_BUBBLES = 8' in policy)
require('sensitive field gate remains', 'request.sensitiveField' in policy)
require('glide gesture gate remains', 'request.glideGesture' in policy)
require('non-letter layer gate remains', '!request.letterLayer' in policy)
require('alphabetic label gate remains', 'isAlphabeticKeyLabel(request.label)' in policy)
require('Soft Normal Playful flight curves exist', 'flightCurveDp = 56f' in policy and 'flightCurveDp = 82f' in policy and 'flightCurveDp = 118f' in policy)
require('flight end scales shrink', 'flightEndScale = 0.62f' in policy and 'flightEndScale = 0.52f' in policy and 'flightEndScale = 0.44f' in policy)

for pack in ('CLASSIC_DARK', 'GLASS_MODERN', 'CLEAN_LIGHT', 'GRADIENT_PRO'):
    require(f'theme pack {pack}', pack in theme_pack)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + ': ' + name)
print(f'Stage 24.1 Bubble Flight static contract: {len(checks)-len(failed)}/{len(checks)} PASS')
if failed:
    raise SystemExit(1)
