#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]

def read(rel: str) -> str:
    return (root / rel).read_text(encoding="utf-8")

build = read("app/build.gradle.kts")
manifest = read("app/src/main/AndroidManifest.xml")
network_xml = read("app/src/main/res/xml/network_security_config.xml")
factory = read("app/src/main/java/com/socialaiassistant/keyboard/network/SecureHttpClientFactory.kt")
policy = read("app/src/main/java/com/socialaiassistant/keyboard/network/NetworkEndpointPolicy.kt")
application = read("app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt")
backend = read("app/src/main/java/com/socialaiassistant/keyboard/backend/SocialAiBackendClient.kt")
openrouter = read("app/src/main/java/com/socialaiassistant/keyboard/ai/OpenRouterGateway.kt")
proguard = read("app/proguard-rules.pro")
workflow = read(".github/workflows/android-build.yml")
props = read("gradle.properties")
unit_policy = read("app/src/test/java/com/socialaiassistant/keyboard/network/NetworkEndpointPolicyTest.kt")
unit_client = read("app/src/test/java/com/socialaiassistant/keyboard/network/SecureHttpClientFactoryTest.kt")

checks = []
def require(name: str, cond: bool):
    checks.append((name, bool(cond)))
    if not cond:
        raise SystemExit(f"FAIL: {name}")

# XML must be well-formed before string-level policy checks.
ET.fromstring(network_xml)
ET.fromstring(manifest)

require("release minification enabled", "isMinifyEnabled = true" in build)
require("release resource shrinking enabled", "isShrinkResources = true" in build)
require("release explicitly non-debuggable", "isDebuggable = false" in build)
require("optimized proguard baseline retained", 'getDefaultProguardFile("proguard-android-optimize.txt")' in build)
require("release lint is fail-closed", "abortOnError = true" in build and "checkReleaseBuilds = true" in build)
require("R8 full mode is not disabled", "android.enableR8.fullMode=false" not in props)
require("mapping keeps line numbers without source filenames", "-keepattributes SourceFile,LineNumberTable" in proguard and "-renamesourcefileattribute SourceFile" in proguard)
require("manifest references network security config", 'android:networkSecurityConfig="@xml/network_security_config"' in manifest)
require("network config blocks cleartext", 'cleartextTrafficPermitted="false"' in network_xml)
require("release trusts system roots", '<certificates src="system" />' in network_xml)
require("user CA is debug-only", "<debug-overrides>" in network_xml and '<certificates src="user" />' in network_xml)
require("application uses hardened shared client", "SecureHttpClientFactory.create()" in application and "OkHttpClient()" not in application)
require("native egress policy is exact-host HTTPS", all(x in policy for x in ['OPENROUTER_HOST = "openrouter.ai"', 'HTTPS_PORT = 443', 'scheme.equals("https"', 'port != HTTPS_PORT', 'allowedHosts']))
require("userinfo is blocked", 'username.isNotEmpty() || password.isNotEmpty()' in policy)
require("TLS-only connection spec is configured", "ConnectionSpec.MODERN_TLS" in factory)
require("connection/read/write/call timeouts are bounded", all(x in factory for x in ["connectTimeout(", "readTimeout(", "writeTimeout(", "callTimeout("]))
require("automatic transport retry is disabled", ".retryOnConnectionFailure(false)" in factory)
require("HTTP redirects are disabled", ".followRedirects(false)" in factory and ".followSslRedirects(false)" in factory)
require("destination interceptor is installed", ".addInterceptor" in factory and "NetworkEndpointPolicy.allows" in factory)
require("OpenRouter requests are no-store JSON", '.header("Accept", "application/json")' in openrouter and '.header("Cache-Control", "no-store")' in openrouter)
require("managed backend requests advertise JSON and no-store", '.header("Accept", "application/json")' in backend and '.header("Cache-Control", "no-store")' in backend)
require("arbitrary backend response body is not retained in exception details", "raw.take(2000)" not in backend and "BackendException(code, message, response.code)" in backend)
require("signed release runs release lint before optimized bundle", "lintRelease bundleRelease" in workflow)
require("network policy unit tests cover lookalikes ports cleartext and userinfo", all(x in unit_policy for x in ["cleartext_alt_ports_lookalikes_and_userinfo_are_blocked", "8443", "evil.example", "username = \"token\""]))
require("secure client unit test covers TLS timeout retry redirect policy", all(x in unit_client for x in ["ConnectionSpec.MODERN_TLS", "retryOnConnectionFailure", "followRedirects", "callTimeoutMillis"]))

print(f"Stage 25.6 production network/release verifier PASS ({len(checks)}/{len(checks)})")
for name, _ in checks:
    print(f"  PASS: {name}")
