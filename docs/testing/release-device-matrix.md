# Release Device Test Matrix

Record device model, Android version, app build SHA/artifact, date, result, and notes for every row that you execute.

| Area | Android 8 / API 26 | Android 10 | Android 12 | Android 14 | Android 16 / API 36 |
| --- | --- | --- | --- | --- | --- |
| Install / launch | pending | pending | pending | pending | pending |
| Enable/select keyboard | pending | pending | pending | pending | pending |
| English typing | pending | pending | pending | pending | pending |
| Bangla Phonetic | pending | pending | pending | pending | pending |
| Bijoy-style layout | pending | pending | pending | pending | pending |
| Accessibility disclosure/consent | pending | pending | pending | pending | pending |
| Privacy screen / clear history | pending | pending | pending | pending | pending |
| Offline typing | pending | pending | pending | pending | pending |

## Supported-app context tests

Run each app on at least Android 14 and Android 16 where practical:

- Facebook: comment composer vs Messenger-style inbox surface classification.
- Messenger: visible conversation context and Smart Reply insertion.
- WhatsApp: visible chat context, manual insertion, manual Send.
- Instagram: DM context; unrelated Caption/Search fields must not be treated as chat.
- Telegram: visible chat context and reply insertion.
- Unsupported SMS/email/chat package: normal typing remains available, but AI conversation context capture stays fail-closed.

## AI action tests

For a non-sensitive conversational field verify:

1. Smart Reply/Comment.
2. Unique/Witty Reply/Comment.
3. Flirty Reply/Comment.
4. Rewrite current draft.
5. Translate current draft.
6. Grammar Fix.
7. Regenerate.
8. Tone preset cycle.
9. Saved Custom Prompt behavior.
10. Existing draft requires explicit Insert/Append/Replace behavior and is not silently overwritten.
11. The keyboard never presses Send.

## Sensitive-field regression tests

Use representative fields from real apps and test fixtures:

- password;
- PIN;
- OTP / one-time-code;
- card number / payment;
- banking-sensitive field;
- metadata-poor numeric field implemented as plain `TYPE_CLASS_NUMBER` with no hint/label (simulate OTP/PIN/CVV ambiguity);
- metadata-only security fields such as `verificationCodeInput`, `smsOTPCode`, `one-time-code`, and `creditCardSecurityCode`;
- Bangla labels such as `ওটিপি`, `বিকাশ পিন`, and `কার্ড নম্বর`;
- explicitly benign numeric fields such as Amount/Quantity/Age/Postal code.

Expected result: protected and metadata-poor ambiguous numeric cases block AI context/suggestions and do not persist conversation history. Explicitly benign numeric cases are treated as non-conversation surfaces rather than credentials, while normal keyboard typing remains available.

## Failure and recovery tests

- Enable airplane mode while the keyboard is open: normal typing continues; AI reports offline safely.
- Use an invalid OpenRouter API key: no crash; clear error shown.
- Simulate insufficient credits or rate limit: no crash; normal typing remains available.
- Rotate/restart the host app and keyboard: stale AI result must not overwrite the current draft.
- Restart the phone: keyboard can be selected again and settings persist.
- Disable Accessibility: context reading stops and normal typing still works.
- Clear local conversation history: reopen the same conversation and confirm prior stored history is gone.
- Remove API key: AI requests stop; offline keyboard features remain usable.

## Stage 15 automation coverage

CI now runs an API 36 end-to-end emulator smoke that taps the IME keys themselves and verifies English typing, Bangla phonetic commit, literal-Latin email policy, numeric-layer selection, orientation survival, and zero captured crash/ANR markers. Physical-device rows above remain release requirements rather than being marked complete by emulator evidence.

## Stage 25.4 conversation-context accuracy regression

On at least Android 14 and Android 16, exercise Messenger, WhatsApp and Telegram with one-to-one and group chats. Verify visual message chronology, conservative sender attribution, RTL behavior where available, duplicate-node suppression, newest-message budget priority, immediate stale-snapshot clearing on window transitions, and thread isolation when a stable conversation title is unavailable. Rapidly switch between two chats and confirm no stored history from one appears in the other.

## Stage 25.5 managed auth / portal WebView security

On at least Android 10, 14 and 16, verify the managed Login / Switch Account and Account / Payment / Device Portal flows:

- the hosted backend HTTPS origin stays inside the embedded WebView;
- an external HTTPS payment/help link opens outside the embedded WebView and does not inherit its first-party cookie/DOM-storage context;
- `http:`, `javascript:`, `data:`, `file:`, `content:` and `intent:` navigation attempts are blocked;
- a backend-host URL on a non-443 port is blocked rather than treated as trusted;
- the exact Chromium HTTPS auth callback is intercepted only during AuthActivity and a wrong state/code fails closed;
- third-party cookies remain disabled while first-party login cookies still allow the hosted flow to complete;
- mixed HTTP content does not render inside either credential WebView;
- Login / Switch Account starts with a clean hosted WebView cookie/DOM state;
- Managed Logout clears both the API bearer session and hosted WebView cookie/DOM state;
- screenshots/screen recording/recent-app previews are blocked for AuthActivity and PortalActivity by `FLAG_SECURE`;
- closing/reopening the portal and using Android Back does not escape the trusted-origin policy or crash the app.

## Stage 25.6 production network / optimized-release regression

Using a signed/minified release candidate (not only debug), validate at least Android 10, 14, and 16:

- managed login/account/AI and personal OpenRouter AI work over normal HTTPS;
- airplane mode, DNS failure, connect timeout, and read timeout fail safely while local typing remains usable;
- a controlled backend/OpenRouter test endpoint that returns an HTTP redirect is treated as a failure rather than followed to another host;
- release build does not trust a user-installed inspection CA, while a debuggable build may use the debug-only CA override for QA;
- R8/resource shrinking does not remove manifest Activities/Services, Room database access, settings screens, WebView flows, or IME resources;
- upload the release `mapping.txt` beside the signed AAB in internal release records and verify one obfuscated test stack trace can be retraced.
