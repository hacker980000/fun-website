# Google Play Data Safety Draft

This document is a conservative draft for the Play Console Data safety form. Re-check the final questions in Play Console immediately before submission because the form and provider relationships can change.

## Data handled by the app

### Other in-app messages / user content

- **Accessed locally:** yes, only after AI Context Access consent, for supported conversational surfaces.
- **Stored locally:** yes, conversation history is retained on the device with encrypted message bodies.
- **Transmitted off device:** only when an AI request needs relevant conversation text or the user's current draft.
- **Purpose:** app functionality — generate, rewrite, translate, grammar-fix, or style a user-requested response.
- **Provider in Personal API mode:** OpenRouter and the selected underlying model provider.
- **Sale of data:** data is **not sold** by Social AI Keyboard.

For a conservative Play Console answer, treat content sent to OpenRouter as collected off-device and review whether it counts as shared under the final provider agreement and Play's service-provider rules. Do not claim a service-provider exemption unless the final contract supports it.

### Personal typing learning

- Optional typing personalization stores bounded word counts, word-pair counts, and phonetic-pair counts in app-private local storage.
- This store is used only for offline suggestion/autocorrect/next-word ranking.
- It is not transmitted off device and is not mixed with AI training.
- Protected password/PIN/OTP/payment/banking fields do not write to this store.
- The user can disable new learning or clear the stored typing-learning data from Keyboard Preferences.

### Optional clipboard history

- Clipboard history is OFF by default.
- If enabled, at most 20 text items are stored only on-device in one AES-256-GCM encrypted payload protected by a dedicated Android Keystore key.
- Stored clipboard history expires after 24 hours.
- Password/PIN/OTP/payment/banking-sensitive fields and Android-sensitive clips are not exposed in the keyboard clipboard panel and are not written to history.
- Disabling the feature clears stored encrypted history; the Stage 25.2 upgrade also purges the legacy plaintext v1 clipboard store.
- Clipboard history is not intentionally transmitted off-device as clipboard history; text is sent to an AI provider only when separately included by the user in an AI request/draft flow described above.

### API key

- A Personal OpenRouter API key is entered by the user.
- It is stored locally using Android Keystore-backed encryption.
- It is transmitted only to OpenRouter as an Authorization credential when the user invokes AI functionality.
- It is never intentionally written to logs, analytics, crash reports, or source control.

### App activity / app identity

Package/surface information is processed locally to determine whether the focused field is a supported conversational context. The current implementation does not intentionally send installed-app inventory or unrelated browsing/activity history to the AI provider.

## Local retention and deletion

- Conversation history stays on the device until the user clears local history, clears app data, or uninstalls the app.
- The app provides **Privacy & data use → Clear local conversation history**.
- The user can remove the Personal OpenRouter API key from the main settings screen.
- The user can disable AI Context Access at any time.
- The user can disable Personal typing learning or clear learned typing data from Keyboard Preferences.
- Optional clipboard history expires after 24 hours and can be deleted immediately by disabling it or using the clear-history control.

## Security practices

- Message bodies in the Room database are encrypted with AES-GCM.
- Optional clipboard history uses a separate AES-256-GCM / Android Keystore key and is OFF by default.
- The personal API key uses a separate Android Keystore-backed secret store.
- Android backup is disabled.
- Cleartext network traffic is disabled.
- Password, PIN, OTP, payment/card, banking-sensitive, and protected fields are blocked from AI context processing. Detection uses input-type semantics plus bounded editor metadata and, when Accessibility is enabled, focused-node password/security signals. Ambiguous unlabeled numeric editors fail closed; clearly labeled benign numeric editors are treated as non-conversation surfaces.

## Managed account / hosted portal note

Managed AI can use a Social AI backend account for authentication, product entitlement, usage/quota, payment/device status, and server-side AI requests. Conversation history itself is not intentionally cloud-synced by this Android release. The hosted login/account portal uses a hardened embedded WebView for the configured backend HTTPS origin: third-party cookies and mixed content are disabled, external HTTPS/mail/tel links leave the credential WebView, unsafe schemes are blocked, and login/logout flows clear hosted WebView cookie/DOM state at the security boundaries described in the privacy policy. Re-check Play Console account-deletion and Data safety answers against the final production backend's account/deletion behavior before submission.
