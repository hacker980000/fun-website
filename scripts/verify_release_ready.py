#!/usr/bin/env python3
from pathlib import Path
import re
import sys

from verify_training_parity import check_training_parity
from verify_shared_backend_config import check_shared_backend_config

ROOT = Path(__file__).resolve().parents[1]
errors = []


def require(cond: bool, message: str):
    if not cond:
        errors.append(message)


def text(path: str) -> str:
    p = ROOT / path
    require(p.exists(), f"missing: {path}")
    return p.read_text(encoding="utf-8") if p.exists() else ""

build = text("app/build.gradle.kts")
manifest = text("app/src/main/AndroidManifest.xml")
accessibility = text("app/src/main/res/xml/accessibility_service_config.xml")
wrapper = text("gradle/wrapper/gradle-wrapper.properties")

require(re.search(r"compileSdk\s*=\s*36\b", build) is not None, "compileSdk must be 36")
require(re.search(r"targetSdk\s*=\s*36\b", build) is not None, "targetSdk must be 36")
vc = re.search(r"versionCode\s*=\s*(\d+)", build)
require(vc is not None and int(vc.group(1)) >= 2, "versionCode must be >= 2")
vn = re.search(r'versionName\s*=\s*"([^"]+)"', build)
require(vn is not None and vn.group(1) == "35.3.1", "versionName must be 35.3.1")
require("gradle-9.6.0-bin.zip" in wrapper, "Gradle wrapper must use 9.6.0")
require("distributionSha256Sum=bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01" in wrapper, "Gradle 9.6.0 distribution checksum must be pinned")

# AGP 9 built-in Kotlin compatibility
root_build = text("build.gradle.kts")
versions_toml = text("gradle/libs.versions.toml")
require("kotlin.android" not in root_build and "kotlin-android" not in versions_toml, "AGP 9 built-in Kotlin requires removing kotlin-android plugin")
require("kotlin.kapt" not in root_build and "kotlin-kapt" not in versions_toml, "AGP 9 built-in Kotlin requires migrating kotlin-kapt")
require("legacy-kapt" in versions_toml and "legacy.kapt" in root_build, "AGP 9 build must declare com.android.legacy-kapt")
require("kotlinOptions" not in build, "android.kotlinOptions is incompatible with Kotlin 2.3/AGP 9 migration")
gradle_props = text("gradle.properties")
require("android.builtInKotlin=false" not in gradle_props, "AGP 9 migrated build must not disable built-in Kotlin")
require("android.newDsl=false" not in gradle_props, "AGP 9 migrated build must not disable the new DSL")
require('android:usesCleartextTraffic="false"' in manifest, "cleartext traffic must be disabled")
require('android:networkSecurityConfig="@xml/network_security_config"' in manifest, "Network Security Config must be attached to the application")
network_security = text("app/src/main/res/xml/network_security_config.xml")
require('cleartextTrafficPermitted="false"' in network_security, "Network Security Config must reject cleartext")
require('<certificates src="system" />' in network_security, "release trust anchors must use system CAs")
require('<debug-overrides>' in network_security and '<certificates src="user" />' in network_security, "user-installed CA trust must remain debug-only")
require("isMinifyEnabled = true" in build, "release must enable R8 minification")
require("isShrinkResources = true" in build, "release must enable resource shrinking")
require("isDebuggable = false" in build, "release must be non-debuggable")
require('getDefaultProguardFile("proguard-android-optimize.txt")' in build, "release must use the optimized default R8 rules")
require("android.enableR8.fullMode=false" not in gradle_props, "R8 full mode must not be disabled")
require('android:allowBackup="false"' in manifest, "backup must be disabled")
require('android:dataExtractionRules="@xml/data_extraction_rules"' in manifest, "Android 12+ data extraction rules must be attached")
data_extraction = text("app/src/main/res/xml/data_extraction_rules.xml")
for domain in ["root", "file", "database", "sharedpref", "external"]:
    require(data_extraction.count(f'<exclude domain="{domain}" path="." />') == 2, f"data extraction rules must exclude {domain} from backup and device transfer")
require("<cloud-backup" in data_extraction and "<device-transfer>" in data_extraction, "data extraction rules must cover cloud backup and device transfer")
require('android:isAccessibilityTool="false"' in accessibility, "Accessibility service must not claim to be an accessibility tool")
require('android:canPerformGestures="false"' in accessibility, "Accessibility service gestures must remain disabled")

permissions = re.findall(r'<uses-permission\s+android:name="([^"]+)"', manifest)
require(set(permissions) <= {"android.permission.INTERNET"}, f"unexpected manifest permissions: {permissions}")

tracked_roots = [ROOT / "app" / "src" / "main", ROOT / ".github", ROOT / "gradle.properties", ROOT / "app" / "build.gradle.kts"]
secret_hits = []
for base in tracked_roots:
    if not base.exists():
        continue
    candidates = [base] if base.is_file() else list(base.rglob("*"))
    for p in candidates:
        if not p.is_file() or p.suffix.lower() in {".png", ".jpg", ".jpeg", ".gif", ".jar", ".zip", ".apk", ".aab"}:
            continue
        try:
            data = p.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            continue
        if re.search(r"sk-or-v1-[A-Za-z0-9_-]{32,}", data):
            secret_hits.append(str(p.relative_to(ROOT)))
require(not secret_hits, f"possible OpenRouter secret found in: {secret_hits}")

for parity_error in check_training_parity(require_release_integration=False):
    errors.append(f"training parity: {parity_error}")

for backend_error in check_shared_backend_config(require_release_integration=False):
    errors.append(f"shared backend config: {backend_error}")


conversation_dao = text("app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationDao.kt")
conversation_repo = text("app/src/main/java/com/socialaiassistant/keyboard/memory/ConversationRepository.kt")
require("recentMessagesFor" in conversation_dao and "LIMIT :limit" in conversation_dao, "conversation history reads must be bounded")
require("pruneMessages" in conversation_dao and "LIMIT :keep" in conversation_dao, "conversation storage must prune old rows")
require("MAX_PERSISTED_MESSAGES_PER_CONVERSATION = 200" in conversation_repo, "conversation retention cap must remain explicit")
require("dao.pruneMessages" in conversation_repo, "conversation repository must enforce retention after merges")

network_factory = text("app/src/main/java/com/socialaiassistant/keyboard/network/SecureHttpClientFactory.kt")
network_policy = text("app/src/main/java/com/socialaiassistant/keyboard/network/NetworkEndpointPolicy.kt")
application_source = text("app/src/main/java/com/socialaiassistant/keyboard/SocialAiApplication.kt")
require("SecureHttpClientFactory.create()" in application_source and "OkHttpClient()" not in application_source, "production app must use the hardened shared OkHttp client")
require("ConnectionSpec.MODERN_TLS" in network_factory, "native HTTP client must be TLS-only")
require("retryOnConnectionFailure(false)" in network_factory, "native HTTP client must not automatically replay failed POST requests")
require("followRedirects(false)" in network_factory and "followSslRedirects(false)" in network_factory, "native HTTP redirects must fail closed")
require("NetworkEndpointPolicy.allows" in network_factory, "native HTTP client must enforce endpoint allowlist")
require('OPENROUTER_HOST = "openrouter.ai"' in network_policy and "HTTPS_PORT = 443" in network_policy, "native egress allowlist must include only expected HTTPS origins")

# Lightweight Android source/resource preflight before Gradle compilation.
resource_ids = set()
for xml_path in (ROOT / "app/src/main/res").rglob("*.xml"):
    xml_text = xml_path.read_text(encoding="utf-8", errors="ignore")
    resource_ids.update(re.findall(r'android:id="@\+id/([A-Za-z0-9_]+)"', xml_text))
missing_resource_ids = []
for kotlin_path in (ROOT / "app/src/main/java").rglob("*.kt"):
    kotlin_text = kotlin_path.read_text(encoding="utf-8", errors="ignore")
    if "import android.R" in kotlin_text:
        continue
    for ref in re.findall(r'\bR\.id\.([A-Za-z0-9_]+)', kotlin_text):
        if ref not in resource_ids:
            missing_resource_ids.append(f"{kotlin_path.relative_to(ROOT)} -> R.id.{ref}")
require(not missing_resource_ids, f"missing app resource ids: {missing_resource_ids}")

manifest_classes = re.findall(r'android:name="(\.[A-Za-z0-9_$.]+|com\.socialaiassistant\.keyboard[A-Za-z0-9_$.]+)"', manifest)
missing_manifest_classes = []
for class_name in manifest_classes:
    fqcn = f"com.socialaiassistant.keyboard{class_name}" if class_name.startswith(".") else class_name
    source_path = ROOT / "app/src/main/java" / Path(*fqcn.split("."))
    source_path = source_path.with_suffix(".kt")
    if not source_path.exists():
        missing_manifest_classes.append(str(source_path.relative_to(ROOT)))
require(not missing_manifest_classes, f"manifest component source missing: {missing_manifest_classes}")

privacy_activity = ROOT / "app/src/main/java/com/socialaiassistant/keyboard/PrivacyActivity.kt"
privacy_layout = ROOT / "app/src/main/res/layout/activity_privacy.xml"
ai_privacy_layout = text("app/src/main/res/layout/settings_category_ai_privacy.xml")
settings_category_activity = text("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
strings = text("app/src/main/res/values/strings.xml")
require(privacy_activity.exists(), "PrivacyActivity must exist")
require(privacy_layout.exists(), "activity_privacy.xml must exist")
require('.PrivacyActivity' in manifest, "PrivacyActivity must be declared in manifest")
require('button_privacy_data' in ai_privacy_layout, "AI & Privacy screen must expose Privacy & Data button")
require('PrivacyActivity::class.java' in settings_category_activity, "AI & Privacy screen must open PrivacyActivity")
require('clear_local_history' in strings, "privacy screen must offer local history deletion")
require('setAccessibilityDisclosureAccepted(true)' in settings_category_activity and 'Settings.ACTION_ACCESSIBILITY_SETTINGS' in settings_category_activity, "disclosure consent must gate Accessibility settings")

workflow_path = ROOT / ".github/workflows/android-build.yml"
workflow = workflow_path.read_text(encoding="utf-8") if workflow_path.exists() else ""
require(workflow_path.exists(), "Android GitHub build workflow must exist")
for required in [
    "actions/checkout@v4",
    "actions/setup-java@v4",
    "android-actions/setup-android@v3",
    "gradle/actions/setup-gradle@v4",
    "gradle-version: '9.6.0'",
    "bootstrap_gradle_wrapper.sh",
    "check_gradle_wrapper_completeness.py",
    "actions/upload-artifact@v4",
    "platforms;android-36",
    "build-tools;36.0.0",
    "testDebugUnitTest",
    "lintDebug",
    "assembleDebug",
    "app-debug.apk",
    "workflow_dispatch",
    "PLAY_UPLOAD_KEYSTORE_B64",
    "lintRelease",
    "bundleRelease",
    "app-release.aab",
    "ReactiveCircus/android-emulator-runner@v2.38.0",
    "stage17_ci_memory_latency.sh",
    "connectedDebugAndroidTest",
    "actions/download-artifact@v4",
]:
    require(required in workflow, f"CI workflow missing invariant: {required}")


stage16_runtime = text("scripts/stage16_ci_runtime_stress.sh")
require("stage15_ci_emulator_validation.sh" in stage16_runtime, "Stage 16 runtime stress must retain Stage 15 functional E2E smoke")
stage17_runtime = text("scripts/stage17_ci_memory_latency.sh")
require("stage16_ci_runtime_stress.sh" in stage17_runtime, "Stage 17 memory/latency validation must retain Stage 16 runtime stress")

play_docs = {
    "docs/play-store/accessibility-declaration.md": [
        "not an accessibility tool",
        "visible conversation",
        "does not perform gestures",
        "never presses Send",
        "prominent disclosure",
    ],
    "docs/play-store/data-safety-draft.md": [
        "Other in-app messages",
        "OpenRouter",
        "not sold",
    ],
    "docs/play-store/privacy-policy.md": [
        "local",
        "encrypted",
        "OpenRouter",
        "clear",
    ],
    "docs/play-store/release-checklist.md": [
        "API 36",
        "Accessibility",
        "Data safety",
    ],
    "docs/testing/release-device-matrix.md": [
        "Android 16",
        "Messenger",
        "WhatsApp",
        "OTP",
        "airplane",
    ],
}
for rel, terms in play_docs.items():
    content = text(rel)
    for term in terms:
        require(term.lower() in content.lower(), f"{rel} missing required content: {term}")

if errors:
    print("RELEASE VERIFICATION: FAIL")
    for item in errors:
        print(f"- {item}")
    sys.exit(1)

print("RELEASE VERIFICATION: PASS")
print("- API 36 target/compile contract present")
print("- Social AI Assistant Pro v35.3.1 release contract present")
print("- Gradle 9.6.0 distribution checksum + verified-wrapper bootstrap contract present")
print("- cleartext/backup disabled")
print("- Accessibility declared non-tool and no gestures")
print("- manifest permissions constrained")
print("- no likely OpenRouter secret found")
