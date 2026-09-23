# Social AI Keyboard Privacy Policy

**Effective date:** 22 September 2026

Social AI Keyboard is published by the developer or organization identified on its Google Play listing. The privacy contact is the developer contact shown on that same listing.

## What the app does

Social AI Keyboard is an Android input method with optional AI-assisted reply and writing tools. Normal keyboard typing can work without AI Context Access.

## AI Context Access

If you choose to enable AI Context Access, the app uses Android Accessibility to read visible conversation text in supported conversational fields. This helps the keyboard understand the current conversation and prepare reply suggestions.

The app is not an accessibility tool. The Accessibility service is configured as read-only for this purpose: it does not perform gestures, does not press buttons, and does not send messages for you.

## Information stored on your device

The app may keep local conversation history so later suggestions can use prior context. Message bodies stored in the local Room database are encrypted. Conversation history is local to the device in this release; Social AI Keyboard does not provide conversation-history cloud sync.

Your Personal OpenRouter API key, if you provide one, is stored locally using Android Keystore-backed encryption.

### Personal typing learning

If Personal typing learning is enabled, the keyboard keeps bounded word counts, word-pair counts, and phonetic-pair counts on this device to personalize offline suggestions and next-word prediction. This typing-learning store is not uploaded, is not merged into AI training, and is not written for password, PIN, OTP, payment/card, banking-sensitive, or other protected fields. You can disable new learning or clear the stored typing-learning data from Keyboard Preferences.

### Optional clipboard history

Recent clipboard history is OFF by default. If you enable it, the app stores at most 20 recent text items locally in an AES-256-GCM encrypted payload protected by a dedicated Android Keystore key. Stored clipboard history automatically expires after 24 hours. Clipboard content is hidden in password, PIN, OTP, payment/card, banking-sensitive, and other protected fields, and content marked sensitive by Android is not exposed in the keyboard clipboard panel or saved to history. Disabling clipboard history clears the stored encrypted history. Upgrading to Stage 25.2 also deletes the older plaintext clipboard-history store rather than migrating it.

## Information sent to an AI provider

When you request an AI feature, the relevant conversation text or draft needed to perform that request may be sent to your selected AI provider. In Personal API mode, requests are sent using your OpenRouter API key and may be processed by OpenRouter and the selected underlying model provider.

Do not enter information you do not want processed by the selected provider. Provider handling is also governed by the provider's own terms and privacy practices.

## Sensitive fields

Social AI Keyboard blocks AI context reading, AI suggestions, and conversation persistence for password, PIN, OTP, payment/card, banking-sensitive, and other protected input fields detected by the keyboard's safety rules. Stage 25.3 evaluates multiple editor metadata signals (for example hint text, field name, labels, private IME options, bounded security-relevant editor extras, Android input-type semantics, and Accessibility password/focused-field signals when available). Because plain numeric editors are commonly used for OTP, PIN, and CVV entry without reliable metadata, an unlabeled or ambiguous numeric editor is treated conservatively as protected. Explicitly described non-secret numeric fields such as amount, quantity, age, or postal code are treated as non-conversation fields rather than secret fields.

## User control

You can at any time:

- disable AI Context Access from the Social AI Keyboard app;
- clear local conversation history from **Privacy & data use**;
- disable Personal typing learning or clear learned typing data from **Keyboard Preferences**;
- disable encrypted Clipboard History or clear it from **Clipboard** settings;
- remove your Personal OpenRouter API key;
- stop using the AI features while continuing to use normal keyboard typing;
- clear the app's data or uninstall the app to remove app-local data.

Generated text is inserted only after your action. Social AI Keyboard never presses Send for you.

## Retention

Local conversation history remains on the device until you clear it, clear app data, or uninstall the app. Optional clipboard history, if enabled, expires automatically after 24 hours and can be cleared sooner by disabling it or using the clear-history control. The app does not intentionally maintain a separate cloud copy of conversation or clipboard history in this release.

## Security

The app disables Android backup for its application data, disables cleartext network traffic, encrypts stored conversation message bodies and optional clipboard history, and protects the Personal OpenRouter API key with Android Keystore-backed encryption.

Embedded account and login pages are restricted to the configured Social AI backend HTTPS origin. First-party cookies may be used for the hosted account flow, but third-party cookies are disabled. Mixed HTTP content, file/content URL access, geolocation, JavaScript popup windows, and unsafe/custom main-frame schemes are blocked. External HTTPS/mail/tel links are handed to another app rather than being rendered with the credential WebView's cookie/DOM-storage context. Login / Switch Account clears stale hosted WebView cookies and DOM storage before a new login, and Managed Logout clears the API session plus hosted WebView cookies/DOM storage. The Auth and Account/Payment Portal activities also request Android `FLAG_SECURE` to reduce screenshots, screen recording, and recent-task preview exposure.

No security method can guarantee absolute protection, but the app is designed to minimize unnecessary collection and storage.

## Sale of data

Social AI Keyboard does **not sell** personal data.

## Changes

If a later release changes how data is accessed, stored, shared, or synced, this policy and the in-app disclosure must be updated before that behavior is released.
