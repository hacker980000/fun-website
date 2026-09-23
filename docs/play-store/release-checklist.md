# Google Play Release Checklist

## Build and version

- [ ] `compileSdk = 36` and `targetSdk = 36` (Android 16 / API 36).
- [ ] Version metadata matches the intended upload (`versionCode = 350301`, `versionName = 35.3.1` in the current source) and is higher than the previous Play upload.
- [ ] GitHub CI passes `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- [ ] Manual release workflow produces a signed `app-release.aab` with the Play upload key.
- [ ] Enroll/confirm Play App Signing before production rollout.
- [ ] Install the CI debug APK on at least one real Android device before Play upload; run `scripts/stage13_adb_ime_smoke.sh <apk> apply` only on a dedicated test device.

## Accessibility policy

- [ ] Keep `android:isAccessibilityTool="false"`.
- [ ] Keep `android:canPerformGestures="false"`.
- [ ] Complete the Play Console AccessibilityService declaration.
- [ ] Select the accurate message/data category, including **Other in-app messages** when asked.
- [ ] Upload a review video showing full prominent disclosure, affirmative consent, Accessibility enablement, core context feature, user-controlled insertion/Send, and opt-out.
- [ ] Re-submit the declaration if Accessibility use changes.

## Privacy and Data safety

- [ ] Host `docs/play-store/privacy-policy.md` at a stable public HTTPS URL.
- [ ] Put the same privacy policy URL in Play Console.
- [ ] Keep the in-app **Privacy & data use** screen reachable in normal app usage.
- [ ] Complete the Play Console **Data safety** form using the final production provider/data flow.
- [ ] Verify the Data safety answer for Other in-app messages sent to OpenRouter/model providers.
- [ ] Verify the app listing's developer/privacy contact matches the hosted policy.
- [ ] Confirm no production secret or upload keystore is committed.

## Functional QA

- [ ] Keyboard enable/select flow works after fresh install.
- [ ] English, Bangla Phonetic, and Bijoy-style typing work.
- [ ] Personal typing learning survives an app/IME restart when enabled.
- [ ] Disabling Personal typing learning stops new learned records without disabling standard offline suggestions.
- [ ] Clear Learned Typing Data removes learned words, bigrams, and phonetic pairs.
- [ ] Password/PIN/OTP/payment/banking fields do not write Personal typing learning data.
- [ ] Emoji, explicit-tap clipboard, voice input, and settings shortcut work.
- [ ] Clipboard history is OFF by default after a fresh install.
- [ ] Enabling clipboard history persists only encrypted v2 data, bounded to 20 items, and entries expire after 24 hours.
- [ ] Disabling clipboard history immediately clears encrypted history and any legacy plaintext v1 history.
- [ ] Password/PIN/OTP/payment/banking fields and Android-sensitive clips do not display clipboard payloads/history.
- [ ] Facebook, Messenger, WhatsApp, Instagram, Telegram, and generic text fields behave as expected.
- [ ] Smart/Unique/Flirty, Rewrite, Translate, Grammar, Regenerate, Tone, and Custom Prompt behave as expected.
- [ ] Draft text is not overwritten without explicit user action.
- [ ] AI never auto-sends.
- [ ] Context Access can be disabled and later re-enabled through disclosure/consent.
- [ ] Clear local conversation history removes stored history without deleting the API key.

## Safety QA

- [ ] Password field blocks AI context and suggestions.
- [ ] PIN field blocks AI context and suggestions.
- [ ] OTP field blocks AI context and suggestions.
- [ ] Card/payment field blocks AI context and suggestions.
- [ ] Banking-sensitive field blocks AI context and suggestions.
- [ ] Unlabeled numeric field fails closed: AI, voice/deferred AI helpers, typing learning, and keyboard clipboard content are not exposed as if it were a normal conversation field.
- [ ] Explicit benign numeric labels such as Amount/Quantity/Age/Postal code remain non-conversation fields without being treated as credentials.
- [ ] Editor metadata-only fixtures block correctly: `fieldName=verificationCodeInput`, `autofill-like=smsOTPCode`, `autocomplete=one-time-code`, `creditCardSecurityCode`, and Bangla OTP/PIN labels.
- [ ] Accessibility `isPassword=true` or a stronger focused-field signal can only make the active IME session more restrictive; it never re-enables AI.
- [ ] Airplane/offline mode leaves normal keyboard typing usable.
- [ ] Invalid API key, insufficient credit, rate limit, timeout, and provider error fail safely.

## Store listing

- [ ] Final app icon, feature graphic, phone screenshots, short description, and full description are ready.
- [ ] Content rating questionnaire completed accurately.
- [ ] App access instructions describe how reviewers can test AI features with a test provider key if needed.
- [ ] Internal testing track completed before closed/open/production rollout.

## Stage 25.5 WebView security QA

- [ ] Managed Auth and Account/Payment Portal render only the configured backend HTTPS origin inside the WebView.
- [ ] Third-party cookies, mixed content, file/content URL access, JavaScript popup windows, and geolocation are disabled.
- [ ] JavaScript/DOM storage remain enabled only because the hosted account UI requires them; no `addJavascriptInterface` bridge is registered.
- [ ] External HTTPS/mail/tel links leave the credential WebView; cleartext and active-content/custom schemes are blocked.
- [ ] Exact auth callback host/path/default HTTPS port and CSRF `state` are required before authorization-code exchange.
- [ ] Login / Switch Account clears stale WebView cookies and DOM storage before starting a new hosted login.
- [ ] Managed Logout clears both the API bearer session and WebView cookies/DOM storage.
- [ ] Auth/Portal screenshots and recent-task previews are blocked by `FLAG_SECURE` on supported devices.
- [ ] Test same-host alternate-port, user-info, lookalike-host, callback-extra-path and callback-fragment cases; all must fail closed or leave the WebView according to policy.

## Stage 25.6 production network / release hardening

- [ ] Release build completes with R8 minification and resource shrinking enabled; archive `mapping.txt` with the release artifact for crash retracing.
- [ ] `lintRelease` passes before the signed Play AAB is accepted for upload.
- [ ] Verify Network Security Config rejects cleartext and trusts only platform/system CAs in non-debuggable release builds.
- [ ] Verify user-installed debug CA trust works only in debuggable builds and is ignored by release builds.
- [ ] Managed backend and OpenRouter native requests succeed only over HTTPS to the exact approved hosts on port 443.
- [ ] Simulated 3xx redirect from backend/OpenRouter is not followed automatically by the native client.
- [ ] Simulated transport failure does not automatically replay AI/account POST requests.
- [ ] Confirm release network timeouts fail safely and normal offline keyboard typing remains available.
- [ ] Confirm backend error handling does not retain arbitrary raw response bodies in exception details/logs.
- [ ] Run dependency updates as a separate controlled change with full Gradle unit/lint/release build verification; do not mix a major networking-library upgrade into this hardening patch without build coverage.
