import com.socialaiassistant.keyboard.settingsui.SettingsCategoryId

fun main() {
    var passed = 0
    fun checkCase(name: String, ok: Boolean) {
        check(ok) { name }
        passed += 1
    }

    val expected = listOf(
        SettingsCategoryId.KEYBOARD_SETUP,
        SettingsCategoryId.LANGUAGE_INPUT,
        SettingsCategoryId.THEME_APPEARANCE,
        SettingsCategoryId.TYPING_SUGGESTIONS,
        SettingsCategoryId.AI_PRIVACY,
        SettingsCategoryId.CLIPBOARD,
        SettingsCategoryId.ACCOUNT_SUBSCRIPTION,
        SettingsCategoryId.HELP_ABOUT
    )

    checkCase("exactly eight canonical categories", SettingsCategoryId.canonical == expected)
    checkCase("wire parse theme", SettingsCategoryId.fromWireValue("theme_appearance") == SettingsCategoryId.THEME_APPEARANCE)
    checkCase("wire parse account", SettingsCategoryId.fromWireValue("account_subscription") == SettingsCategoryId.ACCOUNT_SUBSCRIPTION)
    checkCase("invalid wire is null", SettingsCategoryId.fromWireValue("missing") == null)
    checkCase("null wire is null", SettingsCategoryId.fromWireValue(null) == null)
    checkCase("legacy ai privacy maps", SettingsCategoryId.fromLegacySection("ai_privacy") == SettingsCategoryId.AI_PRIVACY)
    checkCase("unknown legacy section is null", SettingsCategoryId.fromLegacySection("typing") == null)
    checkCase("wire values are unique", SettingsCategoryId.entries.map { it.wireValue }.distinct().size == 8)

    println("Stage 24.3 settings category policy: $passed/8 PASS")
}
