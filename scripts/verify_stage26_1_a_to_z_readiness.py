#!/usr/bin/env python3
"""Stage 26.1 source-level A-Z readiness invariants.

This complements Android lint/unit/instrumentation builds. It intentionally checks only
properties that can be proven from source without an Android SDK/device.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
warnings: list[str] = []


def read(rel: str) -> str:
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8", errors="ignore")


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


manifest = read("app/src/main/AndroidManifest.xml")
build = read("app/build.gradle.kts")
network_security = read("app/src/main/res/xml/network_security_config.xml")
data_rules = read("app/src/main/res/xml/data_extraction_rules.xml")
webview = read("app/src/main/java/com/socialaiassistant/keyboard/SecureWebViewSupport.kt")
http = read("app/src/main/java/com/socialaiassistant/keyboard/network/SecureHttpClientFactory.kt")
dao = read("app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationDao.kt")
repo = read("app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationRepository.kt")
clip = read("app/src/main/java/com/socialaiassistant/keyboard/ime/RecentClipboardStore.kt")
access = read("app/src/main/java/com/socialaiassistant/keyboard/context/SocialAiAccessibilityService.kt")
background = read("app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeBackgroundManager.kt")
backend = read("app/src/main/java/com/socialaiassistant/keyboard/backend/BackendConfig.kt")
ps_bootstrap = read("scripts/bootstrap_gradle_wrapper.ps1")
sh_bootstrap = read("scripts/bootstrap_gradle_wrapper.sh")

# Build/release baseline.
require("compileSdk = 36" in build and "targetSdk = 36" in build, "compile/target SDK 36 required")
require("JavaVersion.VERSION_17" in build, "Java 17 contract required")
require("isMinifyEnabled = true" in build and "isShrinkResources = true" in build, "release R8/resource shrink required")
require("abortOnError = true" in build and "checkReleaseBuilds = true" in build, "lint must gate release")

# Android exposure / data protection.
permissions = set(re.findall(r'<uses-permission\s+android:name="([^"]+)"', manifest))
require(permissions <= {"android.permission.INTERNET"}, f"unexpected production permission(s): {sorted(permissions)}")
require('android:allowBackup="false"' in manifest, "backup must be disabled")
require('android:dataExtractionRules="@xml/data_extraction_rules"' in manifest, "Android 12+ extraction rules required")
require("<cloud-backup" in data_rules and "<device-transfer>" in data_rules, "backup and device-transfer rules both required")
for domain in ("root", "file", "database", "sharedpref", "external"):
    require(data_rules.count(f'<exclude domain="{domain}" path="." />') == 2, f"{domain} must be excluded from cloud + device transfer")

# Web/network hardening.
for dangerous in ("addJavascriptInterface", "MIXED_CONTENT_ALWAYS_ALLOW", "allowUniversalAccessFromFileURLs = true"):
    require(dangerous not in webview, f"dangerous WebView setting/API present: {dangerous}")
require("allowFileAccess = false" in webview and "allowContentAccess = false" in webview, "WebView local content access must stay disabled")
require("MIXED_CONTENT_NEVER_ALLOW" in webview and "safeBrowsingEnabled = true" in webview, "WebView HTTPS/safe-browsing hardening missing")
require("BuildConfig.DEBUG" in webview and "setWebContentsDebuggingEnabled" in webview, "WebView debugging must be debug-only")
require("ConnectionSpec.MODERN_TLS" in http and "followRedirects(false)" in http, "HTTP client TLS/redirect policy missing")
require("retryOnConnectionFailure(false)" in http and "NetworkEndpointPolicy.allows" in http, "HTTP replay/allowlist policy missing")
require('android:usesCleartextTraffic="false"' in manifest and 'cleartextTrafficPermitted="false"' in network_security, "cleartext must stay disabled")

# Bounded privacy/performance state.
require("MAX_PERSISTED_MESSAGES_PER_CONVERSATION = 200" in repo, "conversation persistence cap missing")
require("recentMessagesFor" in dao and "LIMIT :limit" in dao, "conversation reads must be bounded")
require("pruneMessages" in dao and "LIMIT :keep" in dao and "dao.pruneMessages" in repo, "conversation retention pruning missing")
require("MAX_ITEMS = 20" in clip and "MAX_CHARS = 4_000" in clip and "RETENTION_MILLIS = 24L" in clip, "clipboard bounds/retention changed unexpectedly")
require("MAX_NODES = 250" in access and "MAX_DEPTH = 8" in access, "accessibility traversal bounds missing")
require("LruCache<String, Bitmap>(16 * 1024 * 1024)" in background, "theme background bitmap cache must stay bounded")
require("catch (error: CancellationException)" in background and "tempFile?.delete()" in background and "outputFile?.delete()" in background, "background import cancellation cleanup missing")

# Lifecycle cancellation: suspend/UI coroutines must not convert cancellation into user errors.
for rel in (
    "app/src/main/java/com/socialaiassistant/keyboard/AuthActivity.kt",
    "app/src/main/java/com/socialaiassistant/keyboard/CaptionActivity.kt",
    "app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt",
    "app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt",
    "app/src/main/java/com/socialaiassistant/keyboard/ThemeOnboardingActivity.kt",
    "app/src/main/java/com/socialaiassistant/keyboard/SettingsThemeOnboardingActivity.kt",
):
    text = read(rel)
    require("CancellationException" in text, f"explicit coroutine cancellation handling missing: {rel}")

# Wrapper bootstraps must verify exactly the expected Gradle 9.6 wrapper jar and have local fallback parity.
expected_sha = "497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
for name, text in (("bash", sh_bootstrap), ("PowerShell", ps_bootstrap)):
    require(expected_sha in text and "9.6.0" in text, f"{name} wrapper bootstrap checksum/version pin missing")
    require("gradle" in text.lower() and "wrapper" in text.lower(), f"{name} wrapper bootstrap fallback missing")

# Parse all production XML as a cheap structural gate.
for p in [ROOT / "app/src/main/AndroidManifest.xml", *sorted((ROOT / "app/src/main/res").rglob("*.xml"))]:
    try:
        ET.parse(p)
    except Exception as exc:
        errors.append(f"XML parse failed: {p.relative_to(ROOT)}: {exc}")

# External integration warning: this host looks like a browser-extension identity callback and
# cannot be proven valid from Android source alone. Do not fail CI without backend evidence.
if "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.chromiumapp.org" in backend:
    warnings.append("ANDROID_AUTH_CALLBACK is the existing chromiumapp.org identity callback; verify the hosted backend still accepts it before release")

if errors:
    print("STAGE 26.1 A-Z READINESS: FAIL")
    for item in errors:
        print(f"- {item}")
    for item in warnings:
        print(f"WARN: {item}")
    sys.exit(1)

print("STAGE 26.1 A-Z READINESS: PASS")
print("- build/release source contract hardened")
print("- production exposure, WebView, network, backup rules checked")
print("- conversation/clipboard/accessibility/background bounds checked")
print("- lifecycle cancellation + wrapper bootstrap parity checked")
for item in warnings:
    print(f"WARN: {item}")
