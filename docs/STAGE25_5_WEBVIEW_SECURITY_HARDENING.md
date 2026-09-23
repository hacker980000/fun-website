# Stage 25.5 — Managed Auth / Account Portal WebView Security Hardening

Date: 22 September 2026
Project: Social AI Keyboard v35.3.1
Baseline: Stage 25.4 Conversation Context Accuracy

## Goal

Harden the two credential-bearing WebViews used for managed login and the account/payment/device portal without changing the hosted backend API contract.

## Risks found in the Stage 25.4 baseline

1. `AuthActivity` and `PortalActivity` enabled third-party cookies.
2. Both WebViews enabled JavaScript/DOM storage without a shared hardened settings profile.
3. `PortalActivity` used a plain `WebViewClient`, so arbitrary top-level redirects could remain inside the credential WebView.
4. Auth callback matching checked scheme/host/path but did not centralize exact-origin/port/fragment validation.
5. Managed API logout cleared the bearer session but did not clear the independent WebView cookie/DOM-storage credential surface.
6. Login / Switch Account could inherit stale hosted WebView cookies/storage from a prior account.
7. The credential activities did not request `FLAG_SECURE`, leaving screenshots/recent-task previews available to the OS/user capture path.

## Changes

### 1. Shared secure WebView profile

Added `SecureWebViewSupport.kt` and applied it to both hosted WebViews.

The profile keeps first-party cookies, JavaScript, and DOM storage because the current hosted account UI requires them, but disables or constrains:

- third-party cookies;
- mixed HTTP content;
- file access;
- content-URI access;
- file-URL cross-origin access;
- JavaScript-created popup windows;
- multiple WebView windows;
- geolocation;
- autoplay without a user gesture;
- normal WebView cache reuse on these credential surfaces.

Safe Browsing is explicitly enabled. Web contents debugging follows `BuildConfig.DEBUG`, so release builds do not opt into WebView debugging.

No `addJavascriptInterface` bridge is registered.

### 2. Strict main-frame navigation policy

Added pure-Kotlin `WebNavigationPolicy.kt`.

Inside the embedded WebView, only the exact configured backend HTTPS origin is trusted. Origin comparison requires:

- HTTPS;
- exact host;
- default/equivalent HTTPS port;
- no URL user-info confusion.

External `https:`, `mailto:`, and `tel:` destinations are handed to another app rather than rendered with the credential WebView's cookie/DOM-storage context.

The following are blocked for top-level navigation:

- `http:`;
- `javascript:`;
- `data:`;
- `file:`;
- `content:`;
- `intent:` and other unapproved/custom schemes.

Same backend/callback host variants using the wrong port or wrong callback endpoint are blocked rather than treated as ordinary external links.

### 3. Exact auth callback gate

The Chromium HTTPS callback is recognized only by `AuthActivity` and must match the configured callback origin and path with the expected HTTPS port and no fragment. The existing random CSRF `state` remains mandatory, and blank authorization codes fail closed.

A malformed/mismatched callback clears hosted WebView session state before the activity closes.

### 4. Login / switch-account browser state isolation

Before loading the hosted login page, `AuthActivity` clears WebView cookies and WebStorage data. This prevents a previous hosted browser session from silently selecting or leaking into the next Login / Switch Account flow.

The existing managed API bearer session is replaced only after a successful authorization-code exchange.

### 5. Logout clears both credential surfaces

`SettingsCategoryActivity.logoutManaged()` now clears:

- the managed API bearer session through the existing backend logout path; and
- hosted WebView cookies plus WebStorage state through `SecureWebViewSupport.clearHostedSession()`.

This closes the previous gap where the native managed session could be logged out while the embedded portal cookie session remained locally available.

### 6. Screen-capture hardening

`AuthActivity` and `PortalActivity` now apply Android `FLAG_SECURE` to reduce credential/account/payment exposure through screenshots, screen recording, and recent-task previews.

### 7. WebView teardown

The shared teardown stops loading, clears navigation history/cache/form data, removes child views, and destroys the WebView.

## Automated verification

### Stage 25.5 targeted verifier

`python3 scripts/verify_webview_security_stage25_5.py`

Result: **28/28 PASS**

Coverage includes secure settings, exact-origin policy, callback restriction, external handoff, cookie/DOM cleanup, logout integration, `FLAG_SECURE`, non-exported activities, tests, and release/privacy documentation.

### Pure-Kotlin navigation policy self-test

`bash scripts/run_web_navigation_stage25_5_selftest.sh`

Result: **21/21 PASS**

Coverage includes:

- trusted backend paths;
- host lookalikes;
- URL user-info confusion;
- alternate ports;
- exact callback behavior;
- callback extra path/fragment rejection;
- external HTTPS/mail/tel handoff;
- cleartext/active-content/custom-scheme blocking;
- backslash/control-character URL rejection.

### Prior-stage targeted regressions

The following current contracts were rerun and passed:

- Stage 25.0 Conversation Memory: **13/13 PASS**
- Stage 25.1 Accessibility Privacy: **11/11 PASS**
- Stage 25.2 Clipboard Privacy: **18/18 PASS**
- Stage 25.3 Sensitive Fields: **24/24 PASS**
- Stage 25.4 Conversation Context: **27/27 PASS**
- Stage 24.3 Settings Navigation: **158/158 PASS**
- Stage 24.1 Bubble Flight static contract: **48/48 PASS**
- Stage 24.2 Optional Theme Bubble: **30/30 PASS**
- Release verifier: **PASS**

Relevant Stage 25.3/25.4/24.x Kotlin self-tests that completed in the isolated runs also passed, including the Stage 25.5 21/21 policy self-test.

### Shared verifier sweep

Stage 25.5: **43 PASS / 6 historical FAIL** across all `verify_*.py` scripts.

The six failures are the same pre-existing historical/stale verifier set from Stage 25.4:

- `verify_keyboard_bounded_update.py`
- `verify_numberpad_stage23_6.py`
- `verify_typing_stage23_3.py`
- `verify_typing_stage4.py`
- `verify_typing_stage7.py`
- `verify_typing_stage8.py`

Unexpected new failures: **0**

Stage 25.4 PASS -> Stage 25.5 FAIL regressions: **0**

The new Stage 25.5 verifier is an additional PASS.

## Build / device limitation

This execution environment has `kotlinc` but no Android SDK, `android.jar`, or system Gradle installation. The source package also follows the project's verified-wrapper bootstrap policy and does not bundle `gradle-wrapper.jar`. Therefore this stage does **not** claim a full `assembleDebug`, Android Lint, Robolectric suite, instrumentation run, or physical-device WebView execution.

Before Play release, run the Stage 25.5 device matrix on Android 10/14/16 and verify the real hosted login/payment flow, third-party payment/help links, cookie behavior, callback handling, and `FLAG_SECURE` behavior.
