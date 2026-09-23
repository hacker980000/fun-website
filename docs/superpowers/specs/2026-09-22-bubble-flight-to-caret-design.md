# Stage 24.1 — Bubble Flight From Pressed Key to Typed Text

## Goal

When the user taps an eligible alphabetic key, the character must be committed to the active editor immediately as it is today. At the same moment, a themed bubble for that exact character must originate from the physical key that was tapped, travel across the screen, and visually converge on the text insertion point where the committed character appears. The animation is visual-only and must never delay or gate typing.

## User-visible behavior

1. User presses an eligible alphabetic key.
2. The character is committed to the editor immediately.
3. The bubble starts at the center of the exact pressed key, not from a keyboard corner or fixed origin.
4. The bubble follows a smooth curved flight path toward the editor insertion marker/caret.
5. As it approaches the caret, it shrinks and fades so that it appears to merge into the newly inserted character.
6. Rapid typing may create several simultaneous flights, but the renderer remains bounded.
7. The existing Soft, Normal, and Playful intensity modes control duration, curve amplitude, bubble size, and end-scale while preserving the same start and destination rules.
8. The effect remains disabled for sensitive/password/OTP/payment fields, glide input, disabled animations, and non-letter layers according to the existing BubbleKeyPolicy safety gates.

## Architecture

### 1. IME-side source and cursor tracking

`SocialAiInputMethodService` remains responsible for the typing event and exact key origin.

- The key view provides the source point using `getLocationOnScreen()` plus its center.
- The IME requests cursor-anchor updates from the active `InputConnection`.
- `onUpdateCursorAnchorInfo(CursorAnchorInfo)` stores the latest insertion-marker location transformed into screen coordinates.
- Text commit and animation dispatch are separate. Commit happens first/independently; animation failure never changes committed text.

### 2. Target resolver

Add a small `BubbleFlightTargetResolver` with a strict priority order:

1. **Exact cursor anchor** — latest valid `CursorAnchorInfo` insertion marker for the active editor/session.
2. **Editable-node fallback** — if the target app does not provide cursor-anchor data, use the active editable `AccessibilityNodeInfo` bounds and selection/text direction to choose a conservative target point inside the textbox.
3. **No trustworthy target** — do not invent a remote destination. Use a short local fallback that rises from the pressed key and fades near the IME top edge.

This avoids visibly flying to the wrong place.

### 3. Cross-window flight transport

The existing `BubbleKeyEffectRenderer` only draws inside the IME view, so it cannot visually cross into another app's editor. Stage 24.1 introduces a screen-space flight request and a renderer owned by `SocialAiAccessibilityService`.

A lightweight in-process `BubbleFlightBus` carries immutable flight requests:

- label
- source screen X/Y
- destination screen X/Y
- animation spec/intensity
- theme snapshot needed for bubble styling
- editor/session token or generation id
- timestamp

The accessibility service hosts a non-focusable, non-touchable `TYPE_ACCESSIBILITY_OVERLAY` used only while flights are active. No `SYSTEM_ALERT_WINDOW` permission is introduced.

### 4. Overlay renderer

Add `BubbleFlightOverlayRenderer` to the accessibility service.

- Uses screen coordinates.
- Reuses a bounded pool of `TextView` bubbles.
- Maximum simultaneous bubbles remains capped at 8.
- Uses a quadratic/cubic Bezier path instead of a fixed translation.
- Start scale is near the key size; end scale is reduced so the bubble appears to merge into text.
- Alpha stays high during most of the flight, then fades primarily during the final segment.
- Overlay is removed/hidden when no bubbles remain.
- Overlay never takes focus or touch input.

### 5. Timing and destination freshness

The text commit is immediate. The flight uses the freshest target available:

- If a valid cursor anchor already exists, dispatch immediately.
- If the editor reports a newer cursor anchor immediately after commit, the active flight may retarget once during an early, bounded retarget window so the destination corresponds to the newly inserted character.
- Retargeting is limited to one update and only during the first part of the flight to prevent jitter.
- Old anchors are rejected by session/generation and age checks.

This supports the user's desired illusion: the typed character appears in the textbox immediately, and the bubble visually catches up and merges at that exact location.

## Cursor-anchor integration

On editor start/input-view start, request cursor monitoring from the active `InputConnection`.

For API 33+ request the insertion-marker filter when supported. For older supported Android versions use the compatible cursor-update request.

In `onUpdateCursorAnchorInfo`:

- reject NaN/invalid insertion-marker coordinates;
- transform `(horizontal, top/baseline/bottom)` through `CursorAnchorInfo.matrix` into screen coordinates;
- use the vertical midpoint (or baseline-biased point) as the bubble target;
- store the point with package/editor generation and timestamp.

## Accessibility fallback

Extend `SocialAiAccessibilityService` only enough to track the active editable node bounds needed by Bubble Flight. This data is kept transiently in memory and is not persisted.

The fallback target should prefer the caret-facing edge of the field based on text/selection direction when determinable; otherwise it should use a safe point inside the editable bounds. This is explicitly a fallback, not the primary path.

## Safety and privacy

- Existing sensitive-field policy remains authoritative.
- No bubble flight request is emitted for blocked fields.
- No text content is stored by the flight system; only the already-known key label and transient coordinates are needed.
- Coordinate/target state is cleared when the editor changes, IME hides, accessibility disconnects, or session safety changes.
- The accessibility overlay is non-interactive and visual-only.

## Theme behavior

The same geometry/flight rules apply to all keyboard theme packages. Each flight uses the active keyboard theme's visual tokens (surface, neon/accent, gradient/glass treatment, label color) so Classic Dark, Glass Modern, Clean Light, and Gradient Pro remain visually consistent.

## Failure behavior

- Accessibility service unavailable: use the existing/local IME fallback only; typing remains unaffected.
- Cursor updates unsupported by target app: use accessibility editable-bounds fallback if trustworthy.
- Neither target source is available: local rise/fade only; never fly to an arbitrary screen location.
- Overlay creation/render failure: drop the visual effect and continue typing normally.

## Files expected to change

- `app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt`
- `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyEffectRenderer.kt` (reduced to local fallback / source-side concerns)
- `app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt`
- new `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightBus.kt`
- new `app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleFlightTargetResolver.kt`
- new `app/src/main/java/com/socialaiassistant/keyboard/context/BubbleFlightOverlayRenderer.kt`
- related unit/self-tests and Stage 24.1 verification docs/scripts

## Tests / acceptance criteria

1. Pressed key center is the exact animation source in screen coordinates.
2. Valid `CursorAnchorInfo` produces the exact destination after matrix transformation.
3. Text commit does not wait for overlay/cursor data.
4. A post-commit cursor update can retarget at most once inside the retarget window.
5. Accessibility editable bounds are used only when exact cursor data is unavailable.
6. Stale/mismatched targets are rejected.
7. Sensitive fields emit no flight.
8. Glide input emits no flight.
9. Non-letter layers emit no flight.
10. Max simultaneous flights is 8 and pooled views are recycled.
11. Overlay is non-focusable and non-touchable and is removed/hidden when idle.
12. All four keyboard themes render the same motion geometry with theme-specific visuals.
13. Existing Bubble Soft/Normal/Playful settings still control motion character.
14. Existing typing, suggestion, number-row, numeric-pad, theme, and settings-theme regressions remain green.
15. Real-device test confirms at least one common editor (e.g. Messages/WhatsApp-like text field) visibly shows key-origin-to-caret flight.

## Non-goals

- The bubble will not physically become part of the target app's text rendering; the merge is a visual animation ending at the caret/character position.
- No new draw-over-other-apps permission.
- No change to committed text semantics, autocorrect, composition, or prediction behavior.
