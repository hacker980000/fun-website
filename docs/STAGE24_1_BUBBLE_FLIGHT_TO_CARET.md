# Stage 24.1 — Bubble Flight to Caret

Stage 24.1 changes Bubble Key from a keyboard-local rise/fade effect into a source-to-destination flight when Android exposes a safe destination. The typing path remains authoritative: the character is committed first and the animation is dispatched afterward, so animation availability never blocks or delays text input.

## User-visible flow

1. The IME captures the exact on-screen center of the pressed alphabetic key.
2. The key action commits text immediately through the existing typing engine.
3. A `BubbleFlightRequest` is dispatched with the key center, editor/session token, intensity motion spec, and current keyboard theme.
4. The target resolver prefers a fresh `CursorAnchorInfo` insertion marker for the same editor session.
5. If an exact cursor target is unavailable, the accessibility service may use a fresh bounds-only point inside the active editable control for the same editor session.
6. A non-touchable, non-focusable `TYPE_ACCESSIBILITY_OVERLAY` renders a quadratic Bezier flight from the physical key to the target.
7. Near the destination the bubble shrinks and fades so it visually merges with the just-inserted character/caret.

At dispatch time the IME re-checks the latest cursor anchor so an update delivered during the commit can replace the pre-commit target immediately. The animation may also retarget once near the start of a flight when an even newer cursor anchor arrives afterward. Retargeting continues from the bubble's current screen position rather than jumping back to the key.

## Fallbacks

- **Accessibility service unavailable / no cross-window sink:** Bubble Key falls back to the existing keyboard-local effect, starting from the exact pressed-key center.
- **No fresh target:** The cross-window flight is not invented. The IME uses the local fallback instead.
- **Stale or different editor target:** Rejected by editor generation/field/package matching and freshness limits.
- **Editor changes while bubbles are active:** Flights for the old editor are cancelled so they cannot jump into the new field.

## Safety and privacy

The existing Bubble Key gates remain: the feature is optional, system-disabled animations are respected, started Glide gestures are excluded, only eligible alphabetic letter-layer taps are accepted, and sensitive/password/OTP/payment fields are suppressed by the existing safety policy. The accessibility fallback stores only a transient screen point, editor token and capture time; it does not persist editable text or an `AccessibilityNodeInfo`.

Stage 24.1 does **not** request `SYSTEM_ALERT_WINDOW` and does not use `TYPE_APPLICATION_OVERLAY`. It reuses the existing accessibility service and `TYPE_ACCESSIBILITY_OVERLAY`. Conversation-content collection remains governed separately by the existing AI Context Access consent path.

## Performance bounds

`BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES` remains 8. Both the local renderer and cross-window renderer pool/reuse bubble views and recycle the oldest active flight when necessary. Soft, Normal and Playful use the same source/destination rules but different duration, curve and end-scale values.

## Verification

Run the Android-free contract checks:

```bash
python3 scripts/verify_typing_stage24_1.py
bash scripts/run_typing_stage24_1_bubble_flight_selftest.sh
python3 scripts/verify_typing_stage23_3.py
```

When the verified Gradle wrapper and Android SDK are available, also run:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug assembleDebug
```

The physical-device acceptance checklist and current execution status are in `docs/STAGE24_1_VERIFICATION_REPORT.md`.
