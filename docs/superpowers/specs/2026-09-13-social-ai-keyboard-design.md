# Social AI Keyboard Android Design

## Goal

Convert the existing **Social AI Assistant Pro v34.1.1** Chrome extension into a Google-Play-oriented Android keyboard whose AI assistant works wherever the user types. The keyboard must detect conversational context, reuse the extension's reply intelligence, prepare one best reply automatically, insert only when the user taps it, and leave final Send under user control.

## Source of Truth

The original extension is preserved unchanged under `legacy-extension/`:

- `background.js` — OpenRouter client, model routing/fallback, language intelligence, prompt construction, inbox memory, response parsing, error normalization.
- `content.js` — surface detection, sender inference, message collection, conversation keys, draft preservation, media/post context extraction, reply insertion, observer/performance logic.
- `popup.html`, `popup.js`, `popup.css` — user settings, API key validation, model mode, media analysis, recipient history size, privacy consent, memory clearing.
- `manifest.json`, `content.css`, `privacy.html` — extension integration, UI and disclosures.

The Android implementation ports behavior rather than embedding the browser DOM code.

## Product Decisions Locked in Conversation

- Product is a **dual-mode keyboard**: normal keyboard mode + AI assistant mode.
- AI should work in any supported Android app, not only Facebook/Messenger/WhatsApp.
- Conversation context uses **visible recent messages + local cached/full conversation history**.
- Opening a conversational field automatically requests **one Best Smart Reply**; Smart/Witty/Flirty and other actions remain available manually.
- AI access supports both **Personal API** (user OpenRouter key) and **Managed AI** (owner backend) modes.
- Conversation history is **local-only**; no cloud sync.
- As much conversation history as can be safely collected is retained locally.
- Distribution target is **Google Play Store**.
- After one global consent flow, conversational context reading is active across supported apps, except sensitive fields which are always hard-blocked.
- Final insertion requires a user tap. Final Send always requires the user.

## Platform Constraints

- Android application package: `com.socialaiassistant.keyboard`.
- App name: `Social AI Keyboard`.
- `compileSdk = 36`, `targetSdk = 36`; new Google Play apps submitted after 31 August 2026 must target Android 16 / API 36 or higher.
- `minSdk = 26` (Android 8.0) for the initial release.
- JDK 17, Android Gradle Plugin 9.4.0, Gradle 9.6.0, Kotlin 2.3.21.
- Normal typing must remain functional offline and must never wait on an AI/network operation.
- Password, PIN, OTP, payment/card, banking-sensitive and secure system fields must disable AI context capture, AI suggestions, and conversation persistence.
- Accessibility is used only for the user-facing core feature of context-aware reply assistance, with prominent disclosure and affirmative consent.
- No autonomous send/click behavior.

## Architecture

### 1. IME Keyboard Layer

`SocialAiInputMethodService` owns the keyboard window and the active `InputConnection`.

Responsibilities:

- English and Bangla typing.
- Avro/phonetic and Bijoy-style Bangla modes.
- Backspace, shift, enter, number/symbol layers.
- Suggestion/AI bar.
- Emoji, clipboard, voice shortcut and settings entry.
- Insert a generated reply only after an explicit user tap.
- Preserve existing draft text unless the user explicitly chooses a replace/rewrite action.

Typing events are synchronous and local. AI state updates are observed asynchronously and must not block key handling.

### 2. Context Bridge

`SocialAiAccessibilityService` observes supported windows and text nodes. It publishes normalized snapshots rather than exposing `AccessibilityNodeInfo` objects outside the service.

A `ContextAdapter` chain handles package-specific extraction where useful and falls back to `GenericConversationAdapter` for unknown apps.

Each normalized snapshot contains:

- app package name
- screen/window signature
- conversation identity hint
- ordered messages with sender class (`SELF`, `RECIPIENT`, `UNKNOWN`)
- latest recipient message
- visible composer hint
- timestamps when available
- confidence score

The service never reads or persists sensitive fields and never performs Send.

### 3. Safety Gate

`SensitiveFieldPolicy` combines:

- `EditorInfo.inputType`
- autofill/input hints
- package/category deny rules for explicitly sensitive surfaces
- accessibility node password flags

Result is `ALLOW_AI`, `BLOCK_AI`, or `NO_CONVERSATION`.

A blocked field immediately clears any visible AI suggestion and suppresses context storage.

### 4. Conversation Identity and Local Memory

`ConversationKeyFactory` derives a stable local key from package name plus the strongest available conversation/contact/thread identity. It must not depend on raw message text alone.

A Room database stores conversation metadata and all collected messages. Message bodies are encrypted before persistence with AES-256-GCM; the wrapping key lives in Android Keystore. No conversation content is synced to cloud.

Deduplication uses package + conversation key + sender + normalized text + local time bucket/hash so repeatedly exposed accessibility nodes do not create duplicate history rows.

### 5. Ported AI Intelligence

Android ports the behaviors in `legacy-extension/background.js`:

- `cleanString`
- mode normalization: `GENERAL`, `FLIRT_MSG`, `FLIRT_CMT`, `WITTY`
- Bengali/Banglish/English/other-language inference
- latest-recipient-message language priority
- default-new-conversation language behavior
- Fast/Smart model sets
- system/user prompt policy and prompt-injection resistance
- structured JSON result parsing with plain-text fallback
- OpenRouter error normalization
- model fallback
- custom knowledge/preferences

The extension's Fast/Smart mapping is preserved initially:

- Fast primary: `google/gemini-2.5-flash-lite`
- Fast fallback: `google/gemini-2.5-flash`
- Smart primary: `google/gemini-2.5-flash`
- Smart fallback: `google/gemini-2.5-flash-lite`

### 6. AI Router

`AiGateway` has two implementations:

- `OpenRouterGateway` — personal-key mode. The OpenRouter key is stored encrypted using Android Keystore and is never written to logs or conversation DB.
- `ManagedAiGateway` — owner backend mode. Provider credentials are server-side only. This is implemented as a separate backend/release phase because the owner backend endpoint and authentication contract do not exist in the extension source.

`AiRouter` selects the configured gateway and applies timeout/fallback rules. The initial Android vertical slice ships a fully working Personal API path and a disabled Managed AI selector that clearly explains it requires the managed-service release; it must not silently pretend to work.

### 7. Automatic Best Reply State Machine

For an allowed conversational editor:

1. IME session begins.
2. Context bridge supplies a snapshot.
3. Conversation history merges and deduplicates.
4. If the context fingerprint matches the last successful generation, reuse the cached suggestion.
5. Otherwise debounce briefly, then request `GENERAL` Best Smart Reply.
6. Show loading state without blocking typing.
7. Show one reply in the smart bar.
8. Tap inserts the reply at the current editor, preserving non-empty manual draft unless the user explicitly confirms replacement.
9. Send remains entirely user-controlled.

If the user starts typing while generation is running, generation may finish but must not overwrite or auto-insert anything.

### 8. Performance Model

- No global polling loop.
- Accessibility processing is event-driven and debounced.
- No full-tree traversal unless the current event/window plausibly contains an editable conversation surface.
- Accessibility node data is normalized and released quickly.
- Database/network work runs off the IME main thread.
- One in-flight generation per conversation; stale requests are cancelled/ignored.
- Conversation fingerprint caching prevents repeated analysis when nothing changed.
- Keyboard drawing and key dispatch never wait for database/network calls.

### 9. Settings and Onboarding

First-run flow:

1. Welcome.
2. Prominent privacy/context disclosure.
3. Enable keyboard.
4. Select Social AI Keyboard as current IME.
5. Enable AI Context Access / Accessibility.
6. Choose Personal API or Managed AI.
7. For Personal API, enter and test OpenRouter key.
8. Ready.

Settings include:

- English/Bangla languages
- Avro/phonetic vs Bijoy-style Bangla
- theme, key size, sound/haptic
- Fast/Smart model mode
- Personal/Managed AI mode
- OpenRouter key + test action
- custom knowledge/preferences
- preserve draft
- context/media toggles
- conversation memory viewer/clear action
- accessibility status
- privacy disclosure

### 10. Extension Feature Mapping

| Extension behavior | Android equivalent |
|---|---|
| DOM surface detection | IME `EditorInfo` + Accessibility context adapters |
| Facebook/Messenger/WhatsApp anchors | package-specific accessibility adapters |
| comment/inbox distinction | context classifier (`COMMENT`, `MESSAGE`, `GENERAL`) |
| sender-aware message collection | accessibility row/node normalization + sender inference |
| 10/30 initial recipient analysis | imported as a configurable context budget, while full encrypted history remains local |
| cached inbox memory | Room conversation history + context fingerprints |
| language matching | Kotlin port of extension language intelligence |
| Smart/Witty/Flirty modes | keyboard AI panel actions |
| draft preservation | `InputConnection` text inspection + no-autoinsert rule |
| image/video context | separate media-analysis phase using Play-policy-reviewed Android capture mechanisms |
| OpenRouter key protection | Android Keystore encrypted secret storage |
| model fallback | `AiRouter` retry/fallback chain |
| privacy consent | onboarding disclosure + settings audit trail |

## Error Handling

- No internet: show `AI Offline`; typing continues.
- Invalid API key: show a settings action; never expose the key.
- Rate limit/credit failure: show normalized extension-equivalent message; keep last cached reply if still valid.
- Model timeout: cancel after 22 seconds; typing continues.
- Accessibility disabled: normal keyboard works; AI context status explains what is missing.
- Context unavailable: manual AI tools can operate on selected/current draft text without conversation history.
- Empty/malformed model reply: parse fallback; if still empty, show Regenerate.
- Database/encryption failure: do not persist plaintext; keep current session in memory only and surface a privacy-safe diagnostic.

## Testing Strategy

- JVM unit tests for language detection, mode normalization, prompt building, conversation keying, dedupe, sensitive-field policy, API error normalization and response parsing.
- Instrumentation tests for IME text insertion, manual draft preservation, blocked password fields and keyboard mode switching.
- Accessibility service tests with synthetic node/snapshot builders for generic and package-specific adapters.
- Room tests for encrypted-at-rest repository behavior and dedupe.
- MockWebServer/HTTP tests for OpenRouter request/timeout/fallback semantics.
- Manual device matrix for Facebook, Messenger, WhatsApp, Instagram, Telegram, SMS and at least one unknown chat app.
- Performance checks: keystroke latency independent from AI request, no repeated generation for unchanged context, no continuous polling.

## Delivery Decomposition

The product is too large for one safe implementation plan. It is split into independently testable sub-projects:

1. **Foundation Vertical Slice** — Android project, functional IME, safety gate, accessibility snapshot bridge, encrypted local history, ported language/prompt logic, Personal OpenRouter path, automatic Best Reply and tap-to-insert.
2. **Full Keyboard Experience** — production English/Bangla layouts, Avro + Bijoy engines, symbols, emoji, clipboard, voice shortcut, themes, haptic/sound and polished AI panel.
3. **Multi-App Context Adapters** — hardened adapters for Facebook, Messenger, WhatsApp, Instagram, Telegram, SMS/email plus generic fallback, sender/contact identity improvements and long-history management.
4. **Extension Parity: Media + Managed AI** — safe image/video context analysis, owner backend/API, managed authentication/quotas and Personal→Managed fallback policy.
5. **Play Store Release Hardening** — disclosure screens, data-safety documentation, accessibility declaration evidence, instrumentation/device QA, performance/battery profiling, signing and release bundle.

The first implementation plan covers sub-project 1 so that development begins with a real working keyboard rather than a large untestable code drop.
