import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity
import com.socialaiassistant.keyboard.theme.ThemeBubbleAppearance
import com.socialaiassistant.keyboard.theme.ThemeBubbleAppearanceMutations

fun main() {
    var passed = 0
    fun checkCase(name: String, condition: Boolean) {
        check(condition) { name }
        passed += 1
    }

    val defaults = ThemeBubbleAppearance()
    checkCase("default off", !defaults.enabled)
    checkCase("default normal", defaults.intensity == BubbleKeyIntensity.NORMAL)

    val playful = ThemeBubbleAppearanceMutations.withIntensity(defaults, BubbleKeyIntensity.PLAYFUL)
    checkCase("style can change", playful.intensity == BubbleKeyIntensity.PLAYFUL)
    checkCase("style change preserves disabled", !playful.enabled)

    val enabled = ThemeBubbleAppearanceMutations.withEnabled(playful, true)
    checkCase("enabled can change", enabled.enabled)
    checkCase("enabled change preserves style", enabled.intensity == BubbleKeyIntensity.PLAYFUL)

    val disabledAgain = ThemeBubbleAppearanceMutations.withEnabled(enabled, false)
    checkCase("disabled can change", !disabledAgain.enabled)
    checkCase("disable preserves style", disabledAgain.intensity == BubbleKeyIntensity.PLAYFUL)

    println("Stage 24.2 theme bubble policy: $passed/8 PASS")
}
