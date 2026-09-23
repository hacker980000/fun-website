
> **Android Studio build hotfix (2026-09-21):** The Stage 23 package includes a compile fix for a duplicate Kotlin helper signature in `SocialAiInputMethodService.kt`. The elapsed-duration helper is now named `recordDebugRuntimeElapsed(...)`; behavior is unchanged. See `docs/STAGE23_ANDROID_STUDIO_BUILD_FIX.md`.
# Social AI Keyboard

> **A–Z readiness hardening (2026-09-22):** the Stage 26.1 audit bounds encrypted conversation retention to the newest 200 messages per stable conversation, fixes lifecycle-cancellation swallowing, adds Android 12+ backup/device-transfer exclusions, makes theme-photo import cleanup cancellation-safe, aligns Windows/Unix wrapper bootstrap fallbacks, and adds a dedicated A–Z source gate. The current `verify*.py` sweep is 51/51 PASS. The archive still requires restoration of the checksum-verified `gradle-wrapper.jar` before local Gradle execution. Run `bash scripts/bootstrap_gradle_wrapper.sh` (or the PowerShell equivalent) and then `bash scripts/preflight_local_build.sh`. See `docs/STAGE26_1_A_TO_Z_READINESS_AUDIT.md`.

> **Port status (2026-09-18):** Static release verification and XML/Kotlin source preflight pass. Full Gradle unit-test/APK compilation could not be completed in this execution environment because the Gradle 9.6 distribution could not be downloaded from `services.gradle.org`; run the canonical Gradle commands below in Android Studio/CI before shipping.

Social AI Keyboard is the Android IME port of **Social AI Assistant Pro v35.3.1**, built on the existing Gboard-style keyboard project. The original keyboard behavior remains the baseline; v35.3.1 managed-account, protected-backend, caption, training, quota, and device-security behavior is added on top.

## Stage 24.1 — Bubble Flight To Caret

Bubble Key now captures the exact pressed-key screen center, commits text first, then visually flies the themed character bubble toward a fresh cursor-anchor target. The existing accessibility service supplies a matching editable-bounds fallback and a non-interactive accessibility overlay; if no trustworthy remote target is available, the effect stays local to the IME. The renderer remains capped at eight simultaneous bubbles, preserves sensitive/glide/non-letter gates, and adds no `SYSTEM_ALERT_WINDOW` permission. See `docs/STAGE24_1_BUBBLE_FLIGHT_TO_CARET.md`.

## Current implementation

The current source implements the foundation slice, multilingual keyboard/toolbar phase, the Phase 2B full manual AI panel, and Phase 2C multi-app context adapters plus AI style preferences:

- Android IME with offline English QWERTY typing, Shift, Backspace, Space, and Enter.
- Bangla input with Phonetic and Bijoy-style modes, language switching, number and symbol layers.
- **Typing Core v2 / Stages 1-7:** replaceable pure-Kotlin Bangla phonetic transliteration, a 1,144-entry indexed Bangla/Banglish lexicon, ranked top-3 candidates, conservative shorthand/personal autocorrect, two-word context prediction, bounded persistent Bangla learning, daily-use number-row/cursor/height/haptic/sound/clipboard controls, plus a 1,128-entry offline English suggestion/autocorrect engine with a 170-sentence next-word corpus and bounded persistent English learning. Smart Bangla ↔ English candidates require an explicit tap before the keyboard changes language. Full Avro parity and much larger production language models remain future typing work.
- A compact emoji panel, explicit-tap clipboard panel, system voice-recognition shortcut, and settings shortcut.
- Sensitive-field AI hard block for passwords, PIN/OTP, payment/card, and banking-sensitive input hints.
- Accessibility-based visible conversation context bridge with explicit consent gating and no click/gesture/Send automation.
- Local Room conversation history with AES-256-GCM encrypted message bodies.
- Ported extension language behavior for Bengali, Banglish, English/Latin inference, and other source scripts.
- Ported GENERAL/WITTY/FLIRT mode prompt rules and prompt-injection fencing.
- Keystore-protected Personal OpenRouter API key storage.
- Fast/Smart model routing with Gemini 2.5 Flash Lite / Gemini 2.5 Flash fallback order matching the extension.
- Personal OpenRouter mode retains automatic one-at-a-time Best Smart Reply generation with context fingerprints and a small in-memory reply cache; Managed AI generation is explicit-tap only to protect quota/privacy.
- Explicit tap-to-insert with manual-draft preservation. Sending always remains manual.
- Full keyboard AI panel with context-aware **Smart Reply/Comment**, **Unique/Witty Reply/Comment**, and **Flirty Reply/Comment** actions.
- Draft tools: **Rewrite**, **Translate** (target follows English/Bangla keyboard language), **Grammar Fix**, and **Regenerate**.
- Manual AI requests cancel stale in-flight manual requests so the newest action wins.
- Draft transformations can work in ordinary non-sensitive fields without conversation context; social actions require allowed conversation context.
- Package-specific context adapters for Facebook, Messenger, WhatsApp, Instagram, and Telegram improve comment-vs-inbox classification, UI-noise filtering, and conversation identity hints while preserving the generic fallback for unknown apps.
- AI panel labels now follow the detected surface (`Smart Comment` vs `Smart Reply`) rather than relying only on composer text.
- Tone presets are available from the keyboard AI panel: Auto, Friendly, Professional, Casual, Concise, and Warm.
- A saved custom AI instruction can be edited from the app settings screen; it is bounded and explicitly subordinate to privacy, security, safety, language-matching, and factual constraints.

Managed AI backend mode, account-scoped Personal AI Training, protected server generation, subscription/quota enforcement, cryptographic Device Proof, same-installation multi-account login, Funny Comment, Write Caption and user-selected Photo Caption are now integrated. Automatic browser-DOM media scraping is not applicable to an Android IME; Photo Caption instead uses explicit user-selected images. Full Avro parity, substantially larger production dictionaries/corpora, richer multilingual ranking, swipe typing, and long-history management polish remain future keyboard-specific work.

## Premium Neon Theme Engine

The approved **Social AI Neon** mockup is now the canonical keyboard visual baseline. The IME uses a token-based theme engine instead of hard-coded key/panel colors, while all typing and AI actions keep their existing behavior.

Built-in immutable presets:

- **Social AI Neon** — default deep navy/AMOLED glass, electric cyan/blue, and violet AI accents.
- **Cyber Blue**
- **Neon Violet**
- **Aurora Cyan**
- **Black & Gold**
- **Crimson Pulse**
- **Frost Glass**
- **Pure AMOLED**

`Customize Keyboard Theme` opens a live preview/editor. User edits are saved only to **Custom**, so shipped presets remain unchanged. Custom controls include root/panel/key colors, key labels, primary/secondary neon, AI accent, action/Enter accent, glow strength, glass opacity, key radius, key spacing, and key font scale. Critical key-label contrast is guarded by a readable fallback.

Manual theme photos are selected through the Android system document picker and can target exactly one scope: **Full Keyboard**, **Keys Only**, or **AI Panel Only**. Fit modes are **Fill / Fit / Center Crop**, with opacity, dark-overlay/dim, and blur controls. The selected image is decoded off the main thread, downsampled, re-encoded as JPEG into app-private storage (discarding the source EXIF/GPS metadata), cached for display, and never uploaded by the theme system. No storage/media permission was added.

Theme changes are observed live by the IME. Normal, special, Enter/action, AI, AI-action, and secondary-action controls receive distinct cached neon state drawables; the AI toolbar selected state and Smart Reply area follow the active theme. Photo decoding/blur does not run per keystroke and there is no continuous glow animation loop.

## Toolchain

- Android package: `com.socialaiassistant.keyboard`
- App name: `Social AI Keyboard`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 26`
- JDK 17
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- Kotlin 2.3.21

## Build

With JDK 17, Android SDK 36, and Gradle 9.6.0 installed, restore the verified wrapper JAR if it is not already present, then build:

```bash
bash scripts/bootstrap_gradle_wrapper.sh
python3 scripts/check_gradle_wrapper_completeness.py
./gradlew clean testDebugUnitTest assembleDebug
./gradlew lintDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The source pins the official Gradle 9.6.x wrapper JAR checksum and the Gradle 9.6.0 binary-distribution checksum. GitHub Actions installs Gradle 9.6.0, bootstraps the verified wrapper JAR, verifies it, and only then runs `./gradlew`. A local first build also needs network access unless the distribution/dependencies are already cached.

## First setup

1. Open Social AI Keyboard and choose one of the four complete keyboard Theme Packages.
2. Choose one of the four independent Settings Theme Packs (Clean Modern, Card Style, Premium, or Pro Style). A recommended visual match is highlighted, but the user can choose any pack and change it later.
3. Enable the keyboard and choose it as the current IME.
4. Read and accept the AI Context Access disclosure.
5. Enable the app's Accessibility service.
6. For Managed AI, use **Login / Register** and sign in through the hosted v35.3.1 Turnstile flow; subscription/quota/device policy remains server-authoritative. Personal OpenRouter remains optional for legacy draft tools.
7. Open a supported chat/comment composer. In Managed mode, generation happens only after an explicit AI-panel tap so quota is never spent merely because context changed.
8. Tap **AI** for Smart/Unique/Flirty/Funny plus Rewrite/Translate/Grammar actions.
9. Use **Write Caption / Photo Caption** for Romantic, Funny, Emotional, or user-selected Photo Caption. Photo Caption accepts up to 4 selected images and locally re-encodes them as JPEG before upload.
10. Save account-scoped Personal AI Training for the currently logged-in managed account; global server training and hard safety/language rules remain higher authority.
11. Review the generated result, tap Insert/Append/Replace explicitly, then press Send/Post yourself in the host app.

## Privacy boundary

- Conversation history remains local-only; persisted message bodies are encrypted and plaintext bodies are not stored in the Room schema.
- Managed AI sends only the context required for an explicit generation request through the protected Cloudflare Worker. The Worker enforces auth, subscription, quota, version, device binding, cryptographic Device Proof, abuse protection, and server-side Training Center rules.
- Managed Personal AI Training is encrypted locally per logged-in account and sent only with AI requests; it is not stored in D1.
- The optional Personal OpenRouter key is encrypted using a separate Android Keystore key alias.
- Photo Caption uses only images the user explicitly selects, re-encodes them locally to JPEG, removes original EXIF/GPS metadata from the transmitted copy, and never auto-posts.
- Accessibility context extraction is disabled until the in-app disclosure is accepted. Sensitive fields disable AI context capture, suggestions, conversation persistence, and personal typing learning.
- Optional Personal typing learning stores only bounded Bangla/English word and transition counts plus Bangla phonetic/context counts on this device; it can be disabled or cleared in Keyboard Preferences and is not uploaded or mixed with AI training.
- No code path performs Accessibility clicks, gestures, global actions, or autonomous Send/Post.

See `docs/privacy/context-access-disclosure.md`, `docs/testing/foundation-vertical-slice-checklist.md`, `docs/testing/multilingual-keyboard-toolbar-checklist.md`, `docs/testing/full-ai-panel-checklist.md`, and `docs/testing/multi-app-context-adapters-checklist.md`.

## Release hardening and canonical CI build

This beta source now includes a repeatable release contract, in-app **Privacy & data use** controls, local-history deletion, a GitHub Actions APK/AAB pipeline, and Play Console declaration/Data Safety drafts.

The canonical build path is `.github/workflows/android-build.yml`. It installs JDK 17, Android Platform 36, and Build Tools 36.0.0, then runs:

```bash
python3 scripts/verify_release_ready.py
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

A manual GitHub workflow run can create a signed Play upload AAB when the four upload-key repository secrets documented in `docs/release/github-build.md` are configured.

Run the static release contract locally at any time:

```bash
python3 scripts/verify_release_ready.py
```

Current port version: `35.3.1` (`versionCode = 350301`). This source targets API 36.

Release hardening does **not** mean Google Play approval or real-device validation has already occurred. Complete `docs/testing/release-device-matrix.md` and `docs/play-store/release-checklist.md` using the APK/AAB produced by CI before production submission.

## Typing Core v2 — Stage 9

Latest typing baseline: deeper Avro-compatible classic phonetic fallback plus QWERTY-aware Glide scoring. Stage 9 preserves the existing dictionary-first Bangla behavior, adds documented case-sensitive Avro mappings (`Ng/NG/ng`, `R/Rh`, `S/Sh`, `z/y/Y`, `rri`, `O/OU`, signs), and improves offline swipe ranking using keyboard-neighbor costs and ordered-path coverage. See `docs/typing/TYPING_CORE_V2_STAGE9.md`.

## Typing Core v2 Stage 11
Stage 11 hardens Android IME lifecycle behavior (editor-aware numeric layers/actions, composition cancellation on external cursor moves, safe view/rotation cleanup, bounded backspace repeat) and pins/verifies the Gradle 9.6.0 supply-chain contract. See `docs/typing/TYPING_CORE_V2_STAGE11.md`.


## Typing Core v2 Stage 13
Runtime/session hardening, deferred-result target locking, indexed Glide resolution, and real-device ADB smoke tooling are documented in `docs/typing/TYPING_CORE_V2_STAGE13.md`.

## Typing Core v2 Stage 14
Stage 14 adds insertion acknowledgement hardening, retry-safe deferred Voice/Caption delivery, a debug-only multi-editor IME validation harness, ADB crash/ANR/memory evidence collection, an APK metadata/hash CI gate, and coarse Glide/suggestion performance regression budgets. See `docs/typing/TYPING_CORE_V2_STAGE14.md` and `docs/STAGE14_DEVICE_VALIDATION.md`.

## Typing Core v2 Stage 15
Stage 15 adds executable CI/emulator validation across API 36 automatically and API 26/30/36 on manual matrix runs, with semantic IME key metadata and real key-tap E2E smoke coverage.

## Typing Core v2 Stage 16
Stage 16 removes per-word persistence from the typing hot path, guards stale InputConnection operations, snapshots Glide hit geometry once per gesture, throttles Glide haptics, and adds runtime stress evidence collection. See `docs/STAGE16_RUNTIME_PERFORMANCE_HARDENING.md`.

## Typing Core v2 Stage 17
Stage 17 makes Glide indexing lazy/releasable, drops dynamic panel views under UI-hidden memory pressure, adds privacy-safe rolling p50/p95 latency instrumentation, and extends CI with trim-memory resilience evidence. See `docs/STAGE17_MEMORY_LATENCY_HARDENING.md`.

## Typing Core v2 Stage 18
Stage 18 coalesces identical keyboard/suggestion renders and memoizes suggestion computation by engine revision to reduce startup and typing UI churn.

## Typing Core v2 Stage 19
Stage 19 hardens touch responsiveness: text keys remain un-debounced, non-text actions get bounded duplicate-tap guards, suggestion work is frame-coalesced, Glide respects touch slop/key-gap tolerance, and generated action targets are at least 48dp. See `docs/STAGE19_TOUCH_RESPONSIVENESS_HARDENING.md`.

## Typing Core v2 Stage 20
Stage 20 reduces repeat-open, orientation and low-memory churn by retaining safe populated key rows across normal hide/show, coalescing configuration work, reusing stable toolbar order/layout models, and bounding/coalescing local theme-background decode work. See `docs/STAGE20_RESIZE_MEMORY_HARDENING.md`.
## Typing Core v2 Stage 21
Stage 21 polishes deterministic English/Bangla prediction ranking, centralizes evidence-tunable ranking weights, fixes safe-alias visibility and score-before-dedupe ordering, and adds privacy-safe one-shot cold-start/prediction-rank debug evidence. CI now preserves the full Stage 20 chain and collects three cold-start rounds plus controlled English/Bangla prediction fixtures without logging user text. See `docs/STAGE21_PREDICTION_COLDSTART_TUNING.md`.


## Typing Core v2 Stage 22
Stage 22 adds confidence-aware English fuzzy autocorrect, keeps explicit aliases and exact-word protection unchanged, centralizes the existing Bangla safe-typo envelope, and adds reproducible offline/device regression gates for confident-vs-ambiguous typo behavior. It deliberately does not tune ranking weights or latency budgets from uncollected device evidence. See `docs/STAGE22_AUTOCORRECT_PREDICTION_QUALITY.md`.

## Typing Core v2 Stage 23
Stage 23 is the real-device/emulator evidence baseline stage. It does not change the Stage 22 prediction/autocorrect behavior. Instead it wraps the complete Stage 22 validation chain and records repeatable cold/warm launch timing, privacy-safe first-suggestion milestones, PSS memory, frame-timing evidence, process survival, and crash/ANR status. Automatic API 36 and manual API 26/30/36 CI paths now produce machine-readable Stage 23 baseline artifacts; physical-device runs use the same script. No latency/memory/frame threshold is invented before evidence is collected. See `docs/STAGE23_REAL_DEVICE_EVIDENCE_BASELINE.md`.

## Typing Core v2 Stage 23.2 — Premium Multi-Theme UI
Stage 23.2 adds four premium complete Theme Packs (Classic Dark, Glass Modern, Clean Light and Gradient Pro), seven catalog surfaces per pack (28 variants), per-menu overrides, surface-aware previews, preserved advanced/legacy appearance controls, and a manual-pack Reset flow. Toolbar, suggestions and AI/global utility chrome always follow the Global Theme Pack while active key surfaces may be customized independently. Existing legacy/custom theme and background data remain backward-compatible. See `docs/STAGE23_2_PREMIUM_MULTI_THEME_UI.md` and run `python3 scripts/verify_theme_stage23_2.py` for the static contract.

## Typing Core v2 Stage 23.3 — Bubble Key

Stage 23.3 adds the free optional **Bubble Key** typing effect. It is Off by default and can be enabled from Keyboard Preferences with Soft, Normal or Playful intensity. Eligible English/Phonetic/Bijoy alphabetic taps rise in a small theme-aware bubble and fade out; number/symbol/action keys do not. Started Glide gestures, sensitive password/OTP/payment fields and system-disabled animations suppress the effect. Rendering is visual-only, bounded to 8 simultaneous bubbles, and stays outside the text-commit/prediction critical path. See `docs/STAGE23_3_BUBBLE_KEY.md` and run `python3 scripts/verify_typing_stage23_3.py` plus the Stage 23.3 Kotlin self-test before packaging.

## Typing Core v2 Stage 23.4 — First-Run Theme Package Selection

Stage 23.4 makes the four premium Theme Packages part of first-run setup. A fresh install opens the Theme Package chooser before the main settings screen. Choosing Classic Dark, Glass Modern, Clean Light, or Gradient Pro applies that complete package across all catalog surfaces and clears older per-surface overrides. The choice is persisted so later launches go directly to the app. Users can switch the complete package later from **Change Theme Package / Customize**; selecting a complete package again applies it atomically across the keyboard. Existing installs that already have a premium global pack are treated as already onboarded. See `docs/STAGE23_4_FIRST_RUN_THEME_PACKAGE.md`, `docs/STAGE23_4_VERIFICATION_REPORT.md`, and run `python3 scripts/verify_theme_stage23_4.py`.

## Typing Core v2 Stage 23.5 — Reference Numeric Pad

Stage 23.5 replaces the previous flat number page with the user-approved calculator-style numeric pad: a four-key `+ - * /` operator rail on the left, a large 3x3 digit grid in the center, `% / space / backspace` on the right, and a full-width `ABC , !?# 0 = . Enter` bottom row. The layout is shared by Classic Dark, Glass Modern, Clean Light and Gradient Pro; only the resolved NUMBER surface styling changes with the selected theme package. See `docs/STAGE23_5_REFERENCE_NUMERIC_PAD.md` and run `python3 scripts/verify_numberpad_stage23_5.py` plus `scripts/numeric_pad_stage23_5_selftest.kt` before packaging.

## Typing Core v2 Stage 23.6 — Numeric Bottom Row Alignment

Stage 23.6 applies the final approved numeric-pad alignment correction to every complete Theme Package. The full-width `ABC , !?# 0 = . Enter` row is raised by 8dp, while its 9-unit weight geometry is explicitly fixed so the center of `0` remains exactly aligned with the center digit column (`2 / 5 / 8`). Classic Dark, Glass Modern, Clean Light and Gradient Pro continue to share one NUMBER geometry; only their visual styling differs. See `docs/STAGE23_6_NUMERIC_BOTTOM_ROW_ALIGNMENT.md` and run `python3 scripts/verify_numberpad_stage23_6.py` plus `python3 scripts/numeric_pad_stage23_6_geometry_selftest.py` before packaging.

## Typing Core v2 Stage 23.7 — Numeric No-Gap Bottom Row

Stage 23.7 removes the unwanted blank band beneath `7 8 9`. The dedicated NUMBER renderer now reserves exactly three center digit-row heights; the full-width `ABC , !?# 0 = . Enter` row follows immediately after `7 8 9`, with `0` still centered directly beneath `8`. The earlier Stage 23.6 fixed negative-margin lift is superseded. Classic Dark, Glass Modern, Clean Light and Gradient Pro continue to share the same geometry and behavior. See `docs/STAGE23_7_NUMERIC_NO_GAP_BOTTOM_ROW.md` and run `python3 scripts/verify_numberpad_stage23_7.py` plus `python3 scripts/numeric_pad_stage23_7_geometry_selftest.py` before packaging.


## Typing Core v2 Stage 23.8 — Alphabetic Number Row Control

Stage 23.8 makes the optional `1–0` row a persisted alphabetic-layout preference shared by English, Bangla Phonetic and Bijoy. When Number Row is Off, the row is removed from alphabetic layouts; when On, it appears above letters. Dedicated Number and Symbol surfaces remain unchanged, and all four keyboard Theme Packages share the same behavior. See `docs/STAGE23_8_ALPHABETIC_NUMBER_ROW.md`.

## Typing Core v2 Stage 23.9 — Premium Settings Theme Packs

Stage 23.9 adds a second, independent first-run theme step for the Settings panel. After the user chooses a keyboard Theme Package, they choose **Clean Modern, Card Style, Premium, or Pro Style** for Settings. The selected Settings theme styles the complete settings control center and adds a screenshot-inspired category dashboard while preserving all existing IDs, actions, AI/privacy controls and backend behavior. A keyboard-theme-aware recommendation is highlighted without forcing the choice. Settings themes can later be changed manually without altering the keyboard layout theme. See `docs/STAGE23_9_SETTINGS_THEME_PACKS.md` and run `python3 scripts/verify_settings_theme_stage23_9.py`.

## Typing Core v2 Stage 24.1 — Bubble Flight to Caret

Stage 24.1 makes Bubble Key originate at the exact pressed alphabetic key and, when a fresh safe target is available, fly across a non-touchable accessibility overlay to the current insertion caret after the character has already been committed. `CursorAnchorInfo` is preferred, editable bounds are a transient fallback, old-editor/stale targets are rejected, and unavailable cross-window rendering falls back to the key-local effect. Sensitive fields, Glide gestures, non-letter layers, system-disabled animations and the existing maximum of 8 simultaneous bubbles remain protected. No `SYSTEM_ALERT_WINDOW` permission is added. See `docs/STAGE24_1_BUBBLE_FLIGHT_TO_CARET.md` and `docs/STAGE24_1_VERIFICATION_REPORT.md`.
