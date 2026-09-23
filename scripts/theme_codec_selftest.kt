import com.socialaiassistant.keyboard.theme.*

fun main() {
    val custom = ThemePreset.socialAiNeon.copy(
        id = "custom", displayName = "Custom", isBuiltIn = false,
        primaryNeon = 0xFFFF00FF.toInt(),
        glowStrength = 33,
        background = BackgroundPhotoConfig(true, "wall.jpg", BackgroundScope.KEYS_ONLY, BackgroundFit.FIT, 72, 44, 8)
    )
    val map = ThemePreferencesCodec.encode("custom", custom)
    val decoded = ThemePreferencesCodec.decode(map)
    check(decoded.activeThemeId == "custom")
    check(decoded.customTheme.primaryNeon == 0xFFFF00FF.toInt())
    check(decoded.customTheme.glowStrength == 33)
    check(decoded.customTheme.background.scope == BackgroundScope.KEYS_ONLY)
    check(decoded.customTheme.background.fit == BackgroundFit.FIT)
    check(decoded.customTheme.background.localFileName == "wall.jpg")

    val corrupt = ThemePreferencesCodec.decode(mapOf("active_theme_id" to "does_not_exist", "custom_glow_strength" to "500"))
    check(corrupt.activeThemeId == ThemePreset.socialAiNeon.id)
    check(corrupt.customTheme.glowStrength == 100)
    println("theme codec self-test: PASS")
}
