#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")

def req(condition, message):
    if not condition:
        errors.append(message)

service = read("app/src/main/java/com/socialaiassistant/keyboard/ime/SocialAiInputMethodService.kt")
policy = read("app/src/main/java/com/socialaiassistant/keyboard/ime/ImeEditorBehaviorPolicy.kt")
workflow = read(".github/workflows/android-build.yml")
props = read("gradle/wrapper/gradle-wrapper.properties")
bootstrap = read("scripts/bootstrap_gradle_wrapper.sh")
doc = read("docs/typing/TYPING_CORE_V2_STAGE11.md")

for token in [
    "override fun onStartInputView", "override fun onFinishInputView", "override fun onUpdateSelection",
    "override fun onConfigurationChanged", "override fun onEvaluateFullscreenMode(): Boolean = false",
    "stopBackspaceRepeat()", "sendDefaultEditorAction(true)", "ImeEditorBehaviorPolicy.preferredLayer",
    "ImeEditorBehaviorPolicy.enterKeyLabel", "finishComposingText()"
]:
    req(token in service, f"IME lifecycle hardening missing: {token}")

for token in ["preferredLayer", "enterKeyLabel", "shouldAbortCompositionForSelection"]:
    req(token in policy, f"editor behavior policy missing: {token}")

req("distributionSha256Sum=bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01" in props,
    "Gradle distribution checksum pin missing")
req("497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7" in bootstrap,
    "official Gradle 9.6 wrapper checksum missing from bootstrap")
for token in ["gradle-version: '9.6.0'", "bootstrap_gradle_wrapper.sh", "check_gradle_wrapper_completeness.py"]:
    req(token in workflow, f"CI wrapper bootstrap missing: {token}")
req("IME lifecycle hardening" in doc and "Gradle" in doc, "Stage 11 documentation incomplete")

if errors:
    print("TYPING STAGE 11 VERIFICATION: FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("TYPING STAGE 11 VERIFICATION: PASS")
print("- editor-aware layer/action-label policy wired")
print("- composition/selection/input-view/rotation lifecycle guards wired")
print("- verified Gradle wrapper bootstrap + distribution checksum pin wired")
