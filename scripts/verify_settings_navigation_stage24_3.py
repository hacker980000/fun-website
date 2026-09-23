#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
def read(rel):
    path = ROOT / rel
    return path.read_text() if path.exists() else ""

category = read("app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsCategoryId.kt")
renderer = read("app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeDashboardRenderer.kt")
checks = []
def check(name, ok): checks.append((name, bool(ok)))

for token in (
    "KEYBOARD_SETUP", "LANGUAGE_INPUT", "THEME_APPEARANCE", "TYPING_SUGGESTIONS",
    "AI_PRIVACY", "CLIPBOARD", "ACCOUNT_SUBSCRIPTION", "HELP_ABOUT"
):
    check(f"canonical category {token}", token in category)

check("dashboard uses canonical SettingsCategoryId", "SettingsCategoryId" in renderer and "enum class Category" not in renderer)
check("dashboard callback emits canonical ids", "onCategory: (SettingsCategoryId) -> Unit" in renderer)
for title in (
    "Keyboard Setup", "Language & Input", "Theme & Appearance", "Typing & Suggestions",
    "AI & Privacy", "Clipboard", "Account & Subscription", "Help & About"
):
    check(f"dashboard exposes {title}", title in renderer)

failed = [name for name, ok in checks if not ok]
for name, ok in checks: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"\nStage 24.3 settings navigation contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)

# Task 2 shell contract
activity = read("app/src/main/java/com/socialaiassistant/keyboard/SettingsCategoryActivity.kt")
manifest = read("app/src/main/AndroidManifest.xml")
styler = read("app/src/main/java/com/socialaiassistant/keyboard/settingsui/SettingsThemeStyler.kt")

check("category activity registered", '.SettingsCategoryActivity' in manifest and 'android:exported="false"' in manifest)
check("category intent factory exists", "fun createIntent(context: Context, category: SettingsCategoryId): Intent" in activity)
check("category parses stable wire value", "SettingsCategoryId.fromWireValue" in activity)
check("invalid category safely routes away", "openHubForInvalidCategory" in activity and "finish()" in activity)
check("back arrow uses normal back stack", "onBackPressedDispatcher.onBackPressed()" in activity)
check("category theme reapplies on resume", "override fun onResume()" in activity and "applySettingsTheme()" in activity)
check("styler has inner category entry point", "fun applyCategory(" in styler)
for layout_name in (
    "keyboard_setup", "language_input", "theme_appearance", "typing_suggestions",
    "ai_privacy", "clipboard", "account_subscription", "help_about"
):
    check(
        f"category layout exists {layout_name}",
        (ROOT / f"app/src/main/res/layout/settings_category_{layout_name}.xml").exists()
    )

failed = [name for name, ok in checks if not ok]
print("\nTask 2 additions:")
for name, ok in checks[18:]: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"Stage 24.3 cumulative contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)

# Task 3 hub-only contract
main = read("app/src/main/java/com/socialaiassistant/keyboard/MainActivity.kt")
hub = read("app/src/main/res/layout/activity_main.xml")
check("main launches category activity", "SettingsCategoryActivity.createIntent(this, category)" in main)
check("main has no settings smooth scroll", "smoothScrollTo" not in main)
check("main has no old section routing", "section_ai_privacy" not in main and "section_keyboard_preferences" not in main)
check("legacy ai deep link maps to canonical category", "SettingsCategoryId.fromLegacySection" in main and "SECTION_AI_PRIVACY" in main)
check("hub xml has dashboard container", "settings_theme_dashboard_container" in hub)
for old_id in (
    "section_ai_privacy", "section_theme_appearance", "section_keyboard_preferences",
    "section_language_input", "section_account_subscription", "section_help_support", "section_about"
):
    check(f"hub removed {old_id}", old_id not in hub)

failed = [name for name, ok in checks if not ok]
print("\nTask 3 additions:")
for name, ok in checks[33:]: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"Stage 24.3 cumulative contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)

# Task 4 ownership contract
keyboard_setup = read("app/src/main/res/layout/settings_category_keyboard_setup.xml")
language_input = read("app/src/main/res/layout/settings_category_language_input.xml")
clipboard = read("app/src/main/res/layout/settings_category_clipboard.xml")
for view_id in ("status_keyboard_enabled", "status_current_keyboard", "button_keyboard_settings", "button_choose_keyboard"):
    check(f"keyboard setup owns {view_id}", view_id in keyboard_setup and view_id not in hub)
for view_id in ("english_suggestions_checkbox", "english_autocorrect_checkbox", "smart_language_hints_checkbox"):
    check(f"language input owns {view_id}", view_id in language_input and view_id not in hub)
check("language input explains all four layouts", all(token in language_input for token in ("English", "বাংলা", "Phonetic", "Bijoy")))
for view_id in ("clipboard_history_checkbox", "button_clear_clipboard_history"):
    check(f"clipboard owns {view_id}", view_id in clipboard and view_id not in hub)
check("category activity binds keyboard setup", "bindKeyboardSetup" in activity and "Settings.ACTION_INPUT_METHOD_SETTINGS" in activity)
check("category activity binds language preferences", all(token in activity for token in ("setEnglishSuggestions", "setEnglishAutocorrect", "setSmartLanguageHints")))
check("category activity binds clipboard", "setClipboardHistory" in activity and "RecentClipboardStore(this).clear()" in activity)

failed = [name for name, ok in checks if not ok]
print("\nTask 4 additions:")
for name, ok in checks[45:]: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"Stage 24.3 cumulative contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)

# Task 5 appearance/typing ownership and state contract
theme_appearance = read("app/src/main/res/layout/settings_category_theme_appearance.xml")
typing_suggestions = read("app/src/main/res/layout/settings_category_typing_suggestions.xml")
for view_id in (
    "status_theme_name", "button_theme_settings", "status_settings_theme_category", "button_settings_theme",
    "number_row_checkbox", "key_boundary_checkbox", "bubble_key_checkbox", "status_bubble_key_intensity",
    "button_bubble_soft", "button_bubble_normal", "button_bubble_playful", "status_one_handed_mode",
    "button_one_hand_left", "button_one_hand_off", "button_one_hand_right", "status_keyboard_height",
    "button_height_compact", "button_height_normal", "button_height_tall"
):
    check(f"theme appearance owns {view_id}", view_id in theme_appearance and view_id not in hub)
for view_id in (
    "status_typing_learning", "personal_typing_learning_checkbox", "button_clear_typing_learning",
    "spacebar_cursor_checkbox", "glide_typing_checkbox", "status_toolbar_profile", "button_toolbar_balanced",
    "button_toolbar_ai_first", "button_toolbar_typing", "button_toolbar_minimal", "haptic_feedback_checkbox",
    "key_sound_checkbox"
):
    check(f"typing suggestions owns {view_id}", view_id in typing_suggestions and view_id not in hub)
check("category onResume reapplies theme before refresh", activity.find("applySettingsTheme()") < activity.find("refreshCategoryState()") and "override fun onResume()" in activity)
check("manual settings theme change supported", "SettingsThemeOnboardingActivity.EXTRA_MANUAL_CHANGE" in activity and "true" in activity)
check("key boundary uses per-pack repository", "currentKeyBoundaryEnabled(pack)" in activity and "setKeyBoundaryEnabled(pack, enabled)" in activity)
check("bubble uses per-pack repository", all(token in activity for token in ("currentBubbleAppearance(pack)", "setBubbleEnabled(pack, enabled)", "setBubbleIntensity(pack, intensity)")))
check("number row remains global", "settingsRepository.setShowNumberRow" in activity)
check("typing setters preserved", all(token in activity for token in ("setPersonalTypingLearning", "setSpacebarCursorControl", "setGlideTyping", "setToolbarProfile", "setHapticFeedback", "setKeySound")))

failed = [name for name, ok in checks if not ok]
print("\nTask 5 additions:")
for name, ok in checks[58:]: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"Stage 24.3 cumulative contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)

# Task 6 AI/account/help ownership contract
ai_privacy = read("app/src/main/res/layout/settings_category_ai_privacy.xml")
account_subscription = read("app/src/main/res/layout/settings_category_account_subscription.xml")
help_about = read("app/src/main/res/layout/settings_category_help_about.xml")
for view_id in (
    "ai_privacy_consent_checkbox", "preserve_draft_checkbox", "button_save_ai_privacy_consent", "status_ai_tone",
    "context_consent_checkbox", "button_context_access", "button_disable_context_access", "input_custom_instruction",
    "button_save_custom_instruction", "button_clear_custom_instruction", "input_personal_training",
    "button_save_personal_training", "button_clear_personal_training", "button_caption", "button_privacy_data"
):
    check(f"ai privacy owns {view_id}", view_id in ai_privacy and view_id not in hub)
for view_id in (
    "status_managed_account", "status_keyboard_product", "status_assistant_product", "status_usage", "status_gateway",
    "button_login_managed", "button_refresh_account", "button_account_portal", "button_use_managed", "button_logout_managed",
    "button_toggle_advanced_ai", "advanced_ai_container", "status_personal_api", "input_api_key", "button_save_api_key",
    "button_test_api_key", "button_clear_api_key", "button_use_personal"
):
    check(f"account owns {view_id}", view_id in account_subscription and view_id not in hub)
check("help owns whatsapp support", "button_whatsapp_support" in help_about and "button_whatsapp_support" not in hub)
for token in ("about_app_version", "developer_name", "developer_email", "developer_whatsapp", "developer_phone"):
    check(f"help about exposes {token}", token in help_about)
for token in (
    "saveAiPrivacyAndDraftSettings", "acceptDisclosureAndOpenAccessibility", "disableContextAccess",
    "saveCustomInstruction", "clearCustomInstruction", "savePersonalTraining", "clearPersonalTraining"
):
    check(f"AI behavior preserved {token}", token in activity)
for token in (
    "toggleAdvancedAi", "useManagedAi", "logoutManaged", "refreshManagedAccount", "saveApiKey", "testApiKey", "apiTestErrorMessage"
):
    check(f"account behavior preserved {token}", token in activity)
check("account uses existing secret store", "SecretStore" in activity and "secretStore" in activity)
check("account uses existing backend client", "app.backendClient" in activity and "app.managedSessionStore" in activity)
check("account gateway setter preserved", "settingsRepository.setGatewayMode" in activity)

failed = [name for name, ok in checks if not ok]
print("\nTask 6 additions:")
for name, ok in checks[95:]: print(("PASS" if ok else "FAIL") + ": " + name)
print(f"Stage 24.3 cumulative contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed: sys.exit(1)


# Task 7 final acceptance contract
# Parse enum declarations rather than relying on token presence so accidental extra
# categories or duplicate wire values fail the release gate.
enum_block_match = re.search(r"enum class SettingsCategoryId\(val wireValue: String\) \{(.*?)\n\s*companion object", category, re.S)
enum_entries = re.findall(r"^\s*([A-Z_]+)\(\"([^\"]+)\"\)[,;]?", enum_block_match.group(1) if enum_block_match else "", re.M)
check("exactly eight category ids", len(enum_entries) == 8)
check("category wire values are unique", len({wire for _, wire in enum_entries}) == 8)
check("renderer uses canonical items for all four packs", renderer.count("canonicalItems().forEachIndexed") == 4)

migrated_controls = (
    "status_keyboard_enabled", "status_current_keyboard", "button_keyboard_settings", "button_choose_keyboard",
    "english_suggestions_checkbox", "english_autocorrect_checkbox", "smart_language_hints_checkbox",
    "clipboard_history_checkbox", "button_clear_clipboard_history",
    "number_row_checkbox", "key_boundary_checkbox", "bubble_key_checkbox", "status_bubble_key_intensity",
    "personal_typing_learning_checkbox", "spacebar_cursor_checkbox", "glide_typing_checkbox",
    "ai_privacy_consent_checkbox", "context_consent_checkbox", "input_custom_instruction", "input_personal_training",
    "status_managed_account", "input_api_key", "button_whatsapp_support",
)
check("hub contains no migrated category controls", all(control not in hub for control in migrated_controls))
check(
    "legacy ai constants remain",
    'EXTRA_OPEN_SECTION = "social_ai_open_section"' in main and 'SECTION_AI_PRIVACY = "ai_privacy"' in main
)
check(
    "legacy ai route resolves AI_PRIVACY",
    'fromLegacySection' in main and '"ai_privacy" -> AI_PRIVACY' in category
)
check(
    "category activity introduces no raw preference keys",
    not any(token in activity for token in (
        "preferencesDataStore", "stringPreferencesKey", "booleanPreferencesKey", "intPreferencesKey",
        "longPreferencesKey", "floatPreferencesKey", "doublePreferencesKey", "getSharedPreferences(",
        "PreferenceDataStoreFactory", "DataStore<Preferences>"
    ))
)

failed = [name for name, ok in checks if not ok]
print("\nTask 7 final acceptance additions:")
for name, ok in checks[151:]:
    print(("PASS" if ok else "FAIL") + ": " + name)
print(f"Stage 24.3 FINAL contract: {len(checks)-len(failed)}/{len(checks)} PASS")
if failed:
    sys.exit(1)
