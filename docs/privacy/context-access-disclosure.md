# AI Context Access & Privacy/Data Consent — v35.3.1

## AI Privacy/Data consent

Before any AI generation, Social AI Keyboard requires a separate saved **Privacy/Data consent**. AI generation remains blocked if this consent is off.

When you explicitly request AI assistance, the minimum context needed for that request may be processed outside the device:

- **Managed AI:** relevant request context is sent through the protected Social AI Assistant Pro Cloudflare Worker. Authentication, subscription, quota, version, device binding, cryptographic Device Proof, abuse protection and global Training Center rules are server-authoritative. Conversation text and generated replies are not intentionally stored as conversation history in D1.
- **Personal OpenRouter:** relevant request context is sent using the OpenRouter API key that you supplied. The key is encrypted locally with Android Keystore-backed protection.
- **Photo Caption:** only images you explicitly select are used. The app decodes, resizes and JPEG re-encodes selected images before transmission, so the original file's embedded EXIF/GPS metadata is not forwarded. Up to four images are accepted. No automatic posting occurs.
- **Personal AI Training:** managed-account training text is encrypted locally, isolated per logged-in account and sent only with AI generation requests. Server hard rules and global training remain higher authority.

## AI Context Access / Accessibility consent

**Social AI Keyboard can read visible conversation text to prepare reply suggestions.**

If you separately choose to enable **AI Context Access**, Android Accessibility is used only to read visible conversational text needed for supported reply/comment context. The Accessibility service is declared as a non-tool and does not request gesture capability. It does not click buttons, press Send/Post, or perform global actions.

Conversation history collected by the keyboard is kept on the device and persisted message bodies are encrypted. Sensitive fields such as passwords, PIN/OTP, payment/card and banking-sensitive fields hard-block AI context capture and persistence. Detection is privacy-first: the keyboard combines input-type semantics with bounded editor metadata and focused Accessibility password/security signals when available. Ambiguous unlabeled numeric fields fail closed because they can represent OTP/PIN/CVV inputs; clearly labeled benign numeric fields are treated as non-conversation surfaces.

Conversation-text extraction is fail-closed to the app packages covered by the built-in Facebook, Messenger, WhatsApp, Instagram and Telegram adapters (including the explicitly listed Lite/Business/Telegram-client package variants in the source allowlist). Unsupported app packages do not fall back to generic Accessibility conversation-text capture. Normal keyboard typing remains available there.

## Manual action boundary

- Managed AI generation is explicit-tap only; background context changes do not spend managed quota.
- Generated text is a draft. Insert/append/replace requires a user action.
- **Send/Post is always manual in the host app.**
- Preserve Draft defaults to ON: after an explicit Insert, the existing manual draft is retained and the AI draft is appended. The user may turn Preserve Draft off to replace the current draft on explicit Insert.

## Suggested consent controls

Privacy/Data checkbox:

> I understand and consent to the Privacy/Data processing described above for AI generation.

Accessibility/context checkbox:

> I understand and agree to AI Context Access for reply suggestions.

Do not open Android Accessibility settings until the user has affirmatively accepted the context-access disclosure.

## Bubble Flight visual geometry

When Bubble Key is enabled and Accessibility access is available, Bubble Flight may use the active editable field bounds only to position its visual animation. It does not persist field text or coordinates. Exact caret location, when supported by the host editor, comes from the IME cursor-anchor channel; Accessibility is only a bounds fallback. This visual geometry feature does not bypass or expand AI Context Access consent for conversation text, and it does not require a new draw-over-other-apps permission.

## Conversation identity and context isolation

Persistent conversation memory is used only when the supported app exposes a stable contact, group, or thread identity that can be bounded and normalized. A raw Android `windowId` / Activity class is not treated as a conversation identity because the same host window can be reused for different chats. If a stable thread identity is unavailable, Social AI Keyboard can use the current visible snapshot for the explicit AI request, but that snapshot is not merged into persistent conversation memory.

Visible rows are ordered using screen geometry when reliable bounds are available. Sender attribution is conservative: explicit incoming/outgoing UI semantics are preferred, clear side-alignment is only a fallback, and centered/full-width or otherwise ambiguous rows remain `UNKNOWN`. An `UNKNOWN` row is not promoted to a recipient message for reply intent or managed-AI payloads. This reduces accidental cross-speaker and group-chat attribution.
