import com.socialaiassistant.keyboard.theme.*

fun main() {
    check(ThemePreset.builtIns.size == 8)
    check(ThemePreset.builtIns.map { it.id }.toSet().size == 8)
    ThemePreset.builtIns.forEach { check(it.isValid()) }
    val normalized = ThemePreset.socialAiNeon.copy(glowStrength = 999, keyCornerRadiusDp = -10f, keyGapDp = 99f, keyLabelScale = 9f).normalized()
    check(normalized.glowStrength == 100)
    check(normalized.keyCornerRadiusDp == 4f)
    check(normalized.keyGapDp == 12f)
    check(normalized.keyLabelScale == 1.35f)
    val bg = BackgroundPhotoConfig(true, "../bad.jpg", opacityPercent = 250, darkOverlayPercent = -7, blurAmount = 99).normalized()
    check(bg.localFileName == "bad.jpg")
    check(bg.opacityPercent == 100)
    check(bg.darkOverlayPercent == 0)
    check(bg.blurAmount == 30)
    println("theme core self-test: PASS")
}
