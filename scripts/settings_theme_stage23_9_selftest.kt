import com.socialaiassistant.keyboard.settingsui.SettingsThemePack
import com.socialaiassistant.keyboard.theme.ThemePack

fun main() {
    val expected = mapOf(
        ThemePack.CLASSIC_DARK to SettingsThemePack.CLEAN_MODERN,
        ThemePack.GLASS_MODERN to SettingsThemePack.CARD_STYLE,
        ThemePack.CLEAN_LIGHT to SettingsThemePack.PREMIUM,
        ThemePack.GRADIENT_PRO to SettingsThemePack.PRO_STYLE
    )
    var passed = 0
    expected.forEach { (keyboard, settings) ->
        check(SettingsThemePack.recommendedFor(keyboard) == settings)
        println("PASS - $keyboard -> $settings")
        passed++
    }
    SettingsThemePack.entries.forEach { pack ->
        check(SettingsThemePack.fromStored(pack.storedId) == pack)
        println("PASS - stored id round trip ${pack.storedId}")
        passed++
    }
    check(SettingsThemePack.fromStored("missing") == null)
    println("PASS - unknown stored id")
    passed++
    println("$passed/$passed PASS")
}
