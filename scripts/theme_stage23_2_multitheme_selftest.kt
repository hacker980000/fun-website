package com.socialaiassistant.keyboard.theme

private fun expect(name: String, value: Boolean) {
    check(value) { "FAIL: $name" }
    println("PASS: $name")
}

fun main() {
    expect("four premium packs", ThemePack.entries.size == 4)
    expect("seven theme surfaces", KeyboardThemeSurface.entries.size == 7)
    val variants = ThemeCatalog.allVariants()
    expect("28 catalog variants", variants.size == 28)
    expect("28 unique variant ids", variants.map { it.id }.toSet().size == 28)
    expect("all variants valid", variants.all { it.isValid() })

    val legacyCustom = ThemePreset.customFrom().copy(
        primaryNeon = 0xFF123456.toInt(),
        background = BackgroundPhotoConfig(enabled = true, localFileName = "keep.jpg")
    )
    val legacyDecoded = ThemePreferencesCodec.decode(ThemePreferencesCodec.encode("custom", legacyCustom))
    expect("legacy decode has no forced global pack", legacyDecoded.selectionState.globalPack == null)
    expect("legacy custom color preserved", legacyDecoded.selectionState.customTheme.primaryNeon == 0xFF123456.toInt())
    expect("legacy background preserved", legacyDecoded.selectionState.customTheme.background.localFileName == "keep.jpg")

    val premium = legacyDecoded.selectionState.copy(
        globalPack = ThemePack.GLASS_MODERN,
        perSurfaceOverrides = mapOf(
            KeyboardThemeSurface.ENGLISH to ThemePack.CLEAN_LIGHT,
            KeyboardThemeSurface.BIJOY to ThemePack.GRADIENT_PRO
        )
    )
    val premiumDecoded = ThemePreferencesCodec.decode(ThemePreferencesCodec.encodeSelection(premium))
    expect("premium global pack round trip", premiumDecoded.selectionState.globalPack == ThemePack.GLASS_MODERN)
    expect("premium override round trip", premiumDecoded.selectionState.overrideFor(KeyboardThemeSurface.BIJOY) == ThemePack.GRADIENT_PRO)

    val cleared = premium.copy(perSurfaceOverrides = emptyMap())
    val clearedValues = ThemePreferencesCodec.encodeSelection(cleared)
    expect("cleared overrides are not encoded", KeyboardThemeSurface.entries.none { ThemePreferencesCodec.overrideKey(it) in clearedValues })

    val mixedState = ThemeSelectionState(
        globalPack = ThemePack.GLASS_MODERN,
        perSurfaceOverrides = mapOf(KeyboardThemeSurface.ENGLISH to ThemePack.CLEAN_LIGHT),
        legacyActiveThemeId = ThemePreset.socialAiNeon.id,
        customTheme = ThemePreset.customFrom()
    )
    val customMixed = ThemeSelectionMutations.withCustomAppearance(
        mixedState,
        ThemePreset.customFrom().copy(primaryNeon = 0xFF123456.toInt())
    )
    expect(
        "custom appearance keeps per-surface overrides",
        customMixed.perSurfaceOverrides[KeyboardThemeSurface.ENGLISH] == ThemePack.CLEAN_LIGHT
    )
    expect(
        "custom appearance enters custom global mode",
        customMixed.globalPack == null && customMixed.legacyActiveThemeId == "custom"
    )

    val invalid = ThemePreferencesCodec.encode(ThemePreset.blackGold.id, ThemePreset.customFrom()).toMutableMap().apply {
        put(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID, "invalid")
        put(ThemePreferencesCodec.overrideKey(KeyboardThemeSurface.ENGLISH), "invalid")
    }
    val invalidDecoded = ThemePreferencesCodec.decode(invalid)
    expect("invalid global pack safely ignored", invalidDecoded.selectionState.globalPack == null)
    expect("invalid override safely ignored", invalidDecoded.selectionState.perSurfaceOverrides.isEmpty())
    expect("invalid premium data preserves legacy active", invalidDecoded.selectionState.legacyActiveThemeId == ThemePreset.blackGold.id)

    println("Stage23.2 multi-theme Android-free self-test: PASS")
}
