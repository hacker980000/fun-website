#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks = []

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def check(name: str, condition: bool) -> None:
    checks.append((name, condition))

manifest = read("app/src/main/AndroidManifest.xml")
onboarding = read("app/src/main/java/com/socialaiassistant/keyboard/ThemeOnboardingActivity.kt")
repo = read("app/src/main/java/com/socialaiassistant/keyboard/theme/ThemeRepository.kt")
codec = read("app/src/main/java/com/socialaiassistant/keyboard/theme/ThemePreferencesCodec.kt")
strings = read("app/src/main/res/values/strings.xml")
layout = read("app/src/main/res/layout/activity_theme_onboarding.xml")
settings = read("app/src/main/java/com/socialaiassistant/keyboard/ThemeSettingsActivity.kt")

check("onboarding activity exists", "class ThemeOnboardingActivity" in onboarding)
check("onboarding is launcher", 'android:name=".ThemeOnboardingActivity"' in manifest and 'android.intent.category.LAUNCHER' in manifest)
check("main activity stays internal", 'android:name=".MainActivity"' in manifest and 'android:exported="false"' in manifest)
check("all four packs are generated from catalog", "ThemePack.entries.forEach" in onboarding)
check("first-run completion gate exists", "isInitialThemeSetupComplete" in onboarding and "INITIAL_THEME_SETUP_COMPLETE" in codec)
check("first-run selection persists atomically", "completeInitialThemeSetup" in repo and "store.edit" in repo)
check("complete package clears old surface overrides", "globalPack = pack" in repo and "perSurfaceOverrides = emptyMap()" in repo)
check("manual package switching uses global package API", "repository.applyGlobalPack(pack)" in settings)
check("onboarding explains later manual change", "theme_onboarding_change_later" in strings)
check("onboarding contains package container", "theme_onboarding_pack_container" in layout)
check("existing premium selection migrates as complete", "?: (globalPack != null)" in codec)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
print(f"\n{len(checks) - len(failed)}/{len(checks)} checks passed")
if failed:
    raise SystemExit(1)
