package com.socialaiassistant.keyboard.theme

private fun expect(name: String, value: Boolean) {
    check(value) { "FAIL: $name" }
    println("PASS: $name")
}

fun main() {
    val fresh = ThemePreferencesCodec.decode(emptyMap())
    expect("fresh install requires theme package selection", !fresh.initialThemeSetupComplete)
    expect("fresh install has no global package", fresh.selectionState.globalPack == null)

    val existingPremiumValues = ThemePreferencesCodec.encode(
        ThemePreset.socialAiNeon.id,
        ThemePreset.customFrom()
    ).toMutableMap().apply {
        put(ThemePreferencesCodec.THEME_GLOBAL_PACK_ID, ThemePack.GRADIENT_PRO.storedId)
    }
    val existingPremium = ThemePreferencesCodec.decode(existingPremiumValues)
    expect("existing premium package migrates as onboarded", existingPremium.initialThemeSetupComplete)

    val complete = fresh.selectionState.copy(
        globalPack = ThemePack.GLASS_MODERN,
        perSurfaceOverrides = emptyMap()
    )
    KeyboardThemeSurface.entries.forEach { surface ->
        val resolved = ThemeResolutionPolicy.resolveSurface(complete, surface)
        expect(
            "complete package resolves ${surface.name}",
            resolved.id == ThemeCatalog.surface(ThemePack.GLASS_MODERN, surface).id
        )
    }

    val encodedComplete = ThemePreferencesCodec.encodeSelection(complete).toMutableMap().apply {
        put(ThemePreferencesCodec.INITIAL_THEME_SETUP_COMPLETE, "true")
    }
    val decodedComplete = ThemePreferencesCodec.decode(encodedComplete)
    expect("explicit first-run completion persists", decodedComplete.initialThemeSetupComplete)
    expect("selected package persists", decodedComplete.selectionState.globalPack == ThemePack.GLASS_MODERN)
    expect("complete package has no surface overrides", decodedComplete.selectionState.perSurfaceOverrides.isEmpty())

    println("Stage23.4 first-run Theme Package self-test: PASS")
}
