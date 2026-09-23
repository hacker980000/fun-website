# Typing Core v2 Stage 23.3 — Bubble Key

Stage 23.3 adds the approved optional **Bubble Key** typing effect on top of the Stage 23.2 Premium Multi-Theme UI baseline.

## Product behavior

- **Free and optional.** Bubble Key has no subscription/entitlement gate.
- **Default: Off.** Existing and fresh installs do not start floating key animations unless the user enables them.
- Settings path: **Keyboard Preferences → Bubble Key (Free)**.
- Motion profiles: **Soft / Normal / Playful**. The selected profile is persisted even while Bubble Key is off.
- Only alphabetic visual keys are eligible: one-character English/Latin letters and bounded Bengali/Bijoy glyph clusters/signs on the letters layer.
- Number, symbol, quick-word, Space, Enter, Backspace, toolbar, suggestion and panel actions never create bubbles.
- A normal tap still creates a bubble when Glide typing is enabled; once a Glide gesture actually starts dragging, Bubble Key is suppressed for that gesture.
- Password, PIN/OTP, card/payment/banking and other fields classified by the existing sensitive-field policy never create a bubble, even if the setting is on.
- Android system-disabled animations are respected with `ValueAnimator.areAnimatorsEnabled()`.

## Rendering / performance design

`BubbleKeyPolicy` is Android-free and decides whether an effect is allowed before the renderer is touched. `BubbleKeyEffectRenderer` is visual-only and never participates in text commit, prediction, learning or InputConnection operations.

The renderer:

- uses the active **key-surface theme** for bubble fill, accent and label styling;
- keeps at most **8 simultaneous bubbles**;
- reuses a small bounded TextView pool instead of retaining an unbounded view list;
- alternates a small left/right drift and fades the bubble while it rises;
- clears active effects when the input view finishes, when Bubble Key is disabled, and when the IME service is destroyed.

Motion profiles:

| Profile | Duration | Rise | Drift | Bubble size |
|---|---:|---:|---:|---:|
| Soft | 380 ms | 52 dp | 7 dp | 28 dp |
| Normal | 470 ms | 68 dp | 11 dp | 32 dp |
| Playful | 560 ms | 86 dp | 16 dp | 36 dp |

## Isolation guarantees

Stage 23.3 does not modify AI generation, backend/API routing, context extraction, safety policy, Stage 21 prediction profiling, Stage 22 confidence policy, theme-pack resolution, or the locked Stage 19 rule that normal text keys are never debounce-gated.

## Android Studio / physical-device validation

After building in Android Studio, validate these cases:

1. Bubble Key off → no bubbles while rapid typing.
2. Enable Bubble Key → English letter taps bubble; numbers/symbols/Space/Backspace do not.
3. Switch Soft / Normal / Playful → motion visibly changes without changing typed text.
4. Phonetic tap → Latin key bubble appears; resulting Bangla composing behavior remains unchanged.
5. Bijoy tap → visible Bangla/Bijoy key bubble appears.
6. Enable Glide → ordinary taps still bubble; a real swipe/glide gesture does not emit per-key bubbles.
7. Open password, OTP and payment-style validation fields → no bubble appears.
8. Switch all four premium Theme Packs → bubble styling follows the active key-surface theme.
9. Rapidly tap 40+ letters → no crash/ANR and no accumulating overlay views after animations finish.
10. Disable Bubble Key while bubbles are active → active bubbles are cleared immediately.

## Local verification commands

```bash
kotlinc \
  app/src/main/java/com/socialaiassistant/keyboard/settings/KeyboardCustomization.kt \
  app/src/main/java/com/socialaiassistant/keyboard/ime/BubbleKeyPolicy.kt \
  scripts/typing_stage23_3_bubble_key_selftest.kt \
  -include-runtime -d /tmp/stage23_3_bubble_key_selftest.jar
java -jar /tmp/stage23_3_bubble_key_selftest.jar
python3 scripts/verify_typing_stage23_3.py
```
