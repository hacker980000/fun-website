# Social AI Keyboard - Project Context Handoff

Date: 2026-09-18
Current Android port version: 35.3.1
Package: `com.socialaiassistant.keyboard`

## Purpose

This file is the carry-forward context for future work on the project previously discussed as **Keyboard / Social AI Keyboard**. Future keyboard development should use this document plus the current source as the baseline instead of restarting from the old browser-extension assumptions.

## Historical source snapshots recovered

The following source packages were recovered from the account Library and are included in the consolidated archive:

1. `Social_AI_Keyboard_Foundation_Source_2026-09-13.zip`
2. `Social_AI_Keyboard_Phase2A_Source_2026-09-13.zip`
3. `Social_AI_Keyboard_Phase2B_Source_2026-09-13.zip`
4. `Social_AI_Keyboard_Phase2C_Source_2026-09-14.zip`
5. `Social_AI_Keyboard_ReleaseReady_Source_2026-09-14.zip`
6. `Social_AI_Keyboard_BuildReady_v2_2026-09-14.zip`

The v35.3.1 port is based on `Social_AI_Keyboard_BuildReady_v2_2026-09-14.zip`.

## Locked keyboard product decisions

- This is a real Android IME, not a floating browser-style assistant.
- The keyboard has two product modes: **Normal Keyboard** and **AI Assistant**.
- English and Bangla typing remain first-class. Bangla includes Phonetic/Avro-like and Bijoy-style layouts.
- Core typing must remain usable offline and must never depend on AI availability.
- Supported conversation surfaces include Facebook, Messenger, WhatsApp, Instagram, Telegram and a generic fallback usable in other text/chat apps.
- Visible conversation context is obtained through the existing read-only Accessibility bridge only after explicit in-app consent.
- Password, PIN/OTP, payment/card and other sensitive input fields hard-block AI context capture and persistence.
- AI output is always a draft/suggestion. Insertion is explicit. **Send/Post is always manual.**
- No Accessibility click, gesture, global action, autonomous send, or autonomous post may be added.
- Existing stable keyboard UI/typing behavior should be preserved; upgrades should be additive unless a concrete bug requires change.

## Pre-v35.3.1 keyboard baseline

The BuildReady baseline already contains:

- Android IME, English QWERTY, shift/backspace/space/enter.
- Bangla Phonetic and Bijoy-style input; number/symbol layers.
- Emoji, explicit-tap clipboard, system voice input shortcut and settings shortcut.
- Local Room conversation history with encrypted message bodies.
- Context adapters for Facebook, Messenger, WhatsApp, Instagram and Telegram plus generic fallback.
- Smart, Unique/Witty and Flirty Reply/Comment.
- Rewrite, Translate, Grammar Fix and Regenerate.
- Tone presets and bounded Custom Instruction.
- Personal OpenRouter mode with Android-Keystore-protected API key.
- Explicit insert/append/replace; never automatic Send/Post.

## Social AI Assistant Pro v35.3.1 reference

Reference package recovered and inspected:

- Owner/Admin package: latest internal runtime/release metadata is `35.3.1` even though the locked outer filename remains `v35.0.0`.
- Customer package: locked outer filename remains `Social_AI_Assistant_Pro_v35.0.0_CUSTOMER_FINAL.zip`.
- Backend: `https://super-boat-4aba.madigitalstudio2018.workers.dev`
- D1 database: `social-ai-assistant-pro-db`
- Latest migration: `0009_multi_account_same_device.sql`

Important server-authoritative v35.3.1 behavior preserved by this Android port:

- Hosted Turnstile authentication and one-time authorization-code exchange.
- Single-flight login behavior on the Android side.
- One Account -> One Active Device.
- Multiple different subscribed accounts may use the same installation after logout/login.
- Existing account transfer keeps session revocation and the 24-hour self-service transfer policy.
- ECDSA P-256 cryptographic Device Proof with signed AI requests and replay-resistant request IDs.
- Active subscription requirement.
- Daily and subscription-cycle quota enforcement (currently server defaults 200/day and 4000/cycle).
- Server abuse/security gates and version gate.
- Global AI Training Center remains server-side and higher authority than Personal Training.
- Personal AI Training is optional, account-scoped, stored locally, and sent only with AI requests.
- Funny Comment.
- Write Caption: Romantic, Funny and Emotional.
- Photo Caption with up to 4 explicit user-selected images.
- Caption output remains a short draft and never auto-posts.

## v35.3.1 Android-port design decisions

- **Managed AI** uses the existing Cloudflare Worker; no production OpenRouter secret is placed in the APK.
- AI Privacy/Data consent is persisted separately from Accessibility consent and is required before AI generation.
- **Personal OpenRouter** remains as an optional legacy mode/fallback for Rewrite, Translate and Grammar tools not directly exposed by the current managed server AI endpoint.
- Managed social AI does not auto-generate on background context changes. A user must tap an AI action. This prevents invisible quota consumption and matches v35.3.1 explicit-action semantics.
- Managed Personal AI Training is encrypted locally and isolated by managed account ID. Switching accounts on the same installation does not mix training text.
- Photo Caption uses Android's document picker and does not request broad storage access. Selected images are sampled/resized, JPEG re-encoded, and capped before transmission; original EXIF/GPS metadata is not forwarded.
- Caption results enter the IME only after an explicit **Insert** action.
- Existing local conversation history remains local; it is used only to assemble explicit AI requests.

## Known platform difference

The browser extension can inspect supported website DOM media previews. An Android IME cannot safely or reliably read arbitrary host-app images without adding much broader capture permissions. Therefore automatic browser-DOM media scraping is **not copied**. The Android equivalent is explicit user-selected Photo Caption/media input. This is intentional and preserves the keyboard privacy boundary.

## Current verification state

- `python3 scripts/verify_release_ready.py` -> PASS.
- XML parsing/string-reference/Kotlin delimiter static preflight -> PASS.
- Full Gradle unit tests / APK compilation could not run in the current execution environment because the Gradle 9.6 distribution could not be downloaded from `services.gradle.org` and no compatible local Gradle cache was present.
- Before shipping, run the canonical Android Studio/CI build and real-device matrix.

## Standing development rule

Use the current v35.3.1 port source as the working baseline for all new Keyboard discussions in this chat. Do not regress Normal Keyboard typing, Bangla/English input, privacy gates, read-only Accessibility behavior, manual Send/Post, account/device security, or existing AI actions.
