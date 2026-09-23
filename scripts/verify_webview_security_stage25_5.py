#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
auth = (root / 'app/src/main/java/com/socialaiassistant/keyboard/AuthActivity.kt').read_text()
portal = (root / 'app/src/main/java/com/socialaiassistant/keyboard/PortalActivity.kt').read_text()
support = (root / 'app/src/main/java/com/socialaiassistant/keyboard/SecureWebViewSupport.kt').read_text()
policy = (root / 'app/src/main/java/com/socialaiassistant/keyboard/backend/WebNavigationPolicy.kt').read_text()
settings = (root / 'app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt').read_text()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
unit = (root / 'app/src/test/java/com/socialaiassistant/keyboard/backend/WebNavigationPolicyTest.kt').read_text()
privacy = (root / 'docs/play-store/privacy-policy.md').read_text()
checklist = (root / 'docs/play-store/release-checklist.md').read_text()
matrix = (root / 'docs/testing/release-device-matrix.md').read_text()

checks = []
def require(name, condition):
    checks.append((name, bool(condition)))
    if not condition:
        raise SystemExit(f'FAIL: {name}')

require('shared secure WebView configuration is used by auth and portal', 'SecureWebViewSupport.configure(webView)' in auth and 'SecureWebViewSupport.configure(webView)' in portal)
require('third-party cookies are disabled', 'setAcceptThirdPartyCookies(webView, false)' in support and 'setAcceptThirdPartyCookies(webView, true)' not in auth + portal + support)
require('first-party cookies remain explicitly enabled', 'setAcceptCookie(true)' in support)
require('mixed content is never allowed', 'MIXED_CONTENT_NEVER_ALLOW' in support)
require('safe browsing is enabled', 'safeBrowsingEnabled = true' in support)
require('file and content access are disabled', 'allowFileAccess = false' in support and 'allowContentAccess = false' in support)
require('file-url cross-origin access is disabled', 'allowFileAccessFromFileURLs = false' in support and 'allowUniversalAccessFromFileURLs = false' in support)
require('javascript popups and multiple windows are disabled', 'javaScriptCanOpenWindowsAutomatically = false' in support and 'setSupportMultipleWindows(false)' in support)
require('geolocation is disabled and media needs gesture', 'setGeolocationEnabled(false)' in support and 'mediaPlaybackRequiresUserGesture = true' in support)
require('WebView cache is no-cache for hosted credential surfaces', 'cacheMode = WebSettings.LOAD_NO_CACHE' in support)
require('release WebView debugging is disabled via BuildConfig gate', 'setWebContentsDebuggingEnabled(BuildConfig.DEBUG)' in support)
require('no JavaScript bridge is registered', 'addJavascriptInterface' not in auth + portal + support)
require('strict navigation policy compares exact HTTPS origin', 'sameHttpsOrigin' in policy and 'normalizedHttpsPort' in policy and 'uri.userInfo != null' in policy)
require('unsafe schemes are blocked and only external https mail tel are handed off', '"https" -> Decision.OPEN_EXTERNAL' in policy and '"mailto", "tel" -> Decision.OPEN_EXTERNAL' in policy and 'else -> Decision.BLOCK' in policy)
require('callback endpoint is exact and auth-only', 'sameEndpoint(uri, authCallback)' in policy and 'allowAuthCallback' in policy and 'candidate.fragment == null' in policy)
require('same trusted/callback host with wrong origin variant is blocked', 'sameHost(uri, trustedOrigin) || sameHost(uri, authCallback)' in policy)
require('auth navigation uses strict policy and main-frame check', 'request.isForMainFrame' in auth and 'WebNavigationPolicy.classify(rawUrl, allowAuthCallback = true)' in auth)
require('portal navigation uses strict policy and main-frame check', 'request.isForMainFrame' in portal and 'WebNavigationPolicy.classify(rawUrl, allowAuthCallback = false)' in portal)
require('login switch starts from cleared browser state before loading auth page', auth.find('SecureWebViewSupport.clearHostedSession') < auth.find('loadAuthPage()') and 'Login / Switch Account starts from a clean browser credential surface' in auth)
require('failed or mismatched login clears browser state', auth.count('SecureWebViewSupport.clearHostedSession()') >= 3)
require('managed logout clears WebView credential state in addition to API session', 'app.backendClient.logout()' in settings and 'SecureWebViewSupport.clearHostedSession()' in settings)
require('cookie and DOM storage are both purged', 'removeAllCookies' in support and 'WebStorage.getInstance().deleteAllData()' in support)
require('auth and portal use FLAG_SECURE', 'FLAG_SECURE' in auth and 'FLAG_SECURE' in portal)
require('auth and portal activities remain non-exported', 'android:name=".AuthActivity"' in manifest and 'android:name=".PortalActivity"' in manifest and manifest.count('android:exported="false"') >= 8)
require('policy unit tests cover lookalikes schemes callback and alternate ports', all(x in unit for x in ['backend_lookalike_and_userinfo_urls_are_not_trusted', 'cleartext_and_active_content_schemes_are_blocked', 'auth_callback_is_exact_and_auth_only', 'alternate_ports_are_not_same_origin_or_callback']))
require('privacy policy documents managed WebView/browser security boundary', 'Embedded account and login pages' in privacy and 'third-party cookies are disabled' in privacy)
require('release checklist contains Stage 25.5 WebView security QA', 'Stage 25.5 WebView security' in checklist)
require('device matrix contains hosted auth/portal navigation tests', 'Stage 25.5 managed auth / portal WebView security' in matrix)

print(f'Stage 25.5 WebView security verifier PASS ({len(checks)}/{len(checks)})')
for name, _ in checks:
    print(f'  PASS: {name}')
