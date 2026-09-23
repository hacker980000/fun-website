package com.socialaiassistant.keyboard.theme

data class DecodedThemePreferences(
    val activeThemeId: String,
    val customTheme: KeyboardTheme,
    val selectionState: ThemeSelectionState,
    val initialThemeSetupComplete: Boolean
)

object ThemePreferencesCodec {
    const val ACTIVE_THEME_ID = "active_theme_id"
    const val THEME_GLOBAL_PACK_ID = "theme_global_pack_id"
    const val THEME_SELECTION_SCHEMA_VERSION = "theme_selection_schema_version"
    const val INITIAL_THEME_SETUP_COMPLETE = "initial_theme_setup_complete"
    const val CURRENT_SELECTION_SCHEMA_VERSION = 1

    fun overrideKey(surface: KeyboardThemeSurface): String = "theme_override_${surface.storedId}"

    fun encode(activeThemeId: String, customTheme: KeyboardTheme): Map<String, String> {
        val t = customTheme.asCustom().normalized()
        val b = t.background.normalized()
        return linkedMapOf(
            ACTIVE_THEME_ID to normalizeActiveId(activeThemeId),
            "custom_root_background" to t.rootBackground.toString(),
            "custom_panel_surface" to t.panelSurface.toString(),
            "custom_toolbar_surface" to t.toolbarSurface.toString(),
            "custom_key_surface" to t.keySurface.toString(),
            "custom_special_key_surface" to t.specialKeySurface.toString(),
            "custom_disabled_surface" to t.disabledSurface.toString(),
            "custom_text_primary" to t.textPrimary.toString(),
            "custom_text_secondary" to t.textSecondary.toString(),
            "custom_key_label" to t.keyLabel.toString(),
            "custom_special_key_label" to t.specialKeyLabel.toString(),
            "custom_disabled_label" to t.disabledLabel.toString(),
            "custom_primary_neon" to t.primaryNeon.toString(),
            "custom_secondary_neon" to t.secondaryNeon.toString(),
            "custom_ai_neon" to t.aiNeon.toString(),
            "custom_action_accent" to t.actionAccent.toString(),
            "custom_danger_accent" to t.dangerAccent.toString(),
            "custom_key_corner_radius_dp" to t.keyCornerRadiusDp.toString(),
            "custom_panel_corner_radius_dp" to t.panelCornerRadiusDp.toString(),
            "custom_key_gap_dp" to t.keyGapDp.toString(),
            "custom_outer_padding_dp" to t.outerPaddingDp.toString(),
            "custom_key_label_scale" to t.keyLabelScale.toString(),
            "custom_toolbar_label_scale" to t.toolbarLabelScale.toString(),
            "custom_ai_title_scale" to t.aiTitleScale.toString(),
            "custom_ai_subtitle_scale" to t.aiSubtitleScale.toString(),
            "custom_border_opacity" to t.borderOpacity.toString(),
            "custom_glow_strength" to t.glowStrength.toString(),
            "custom_pressed_glow_strength" to t.pressedGlowStrength.toString(),
            "custom_glass_opacity" to t.glassOpacity.toString(),
            "custom_fill_style" to t.fillStyle.name,
            "custom_key_gradient_start" to t.keyGradientStart.toString(),
            "custom_key_gradient_end" to t.keyGradientEnd.toString(),
            "custom_special_gradient_start" to t.specialGradientStart.toString(),
            "custom_special_gradient_end" to t.specialGradientEnd.toString(),
            "custom_panel_gradient_start" to t.panelGradientStart.toString(),
            "custom_panel_gradient_end" to t.panelGradientEnd.toString(),
            "background_enabled" to b.enabled.toString(),
            "background_file_name" to b.localFileName,
            "background_scope" to b.scope.name,
            "background_fit" to b.fit.name,
            "background_opacity" to b.opacityPercent.toString(),
            "background_dim" to b.darkOverlayPercent.toString(),
            "background_blur" to b.blurAmount.toString()
        )
    }

    fun encodeSelection(state: ThemeSelectionState): Map<String, String> {
        val values = encode(state.legacyActiveThemeId, state.customTheme).toMutableMap()
        values[THEME_SELECTION_SCHEMA_VERSION] = CURRENT_SELECTION_SCHEMA_VERSION.toString()
        state.globalPack?.let { values[THEME_GLOBAL_PACK_ID] = it.storedId }
        state.perSurfaceOverrides.forEach { (surface, pack) ->
            values[overrideKey(surface)] = pack.storedId
        }
        return values
    }

    fun decode(values: Map<String, String>): DecodedThemePreferences {
        val base = ThemePreset.customFrom()
        val background = BackgroundPhotoConfig(
            enabled = values["background_enabled"]?.toBooleanStrictOrNull() ?: base.background.enabled,
            localFileName = values["background_file_name"] ?: base.background.localFileName,
            scope = BackgroundScope.fromStored(values["background_scope"]),
            fit = BackgroundFit.fromStored(values["background_fit"]),
            opacityPercent = int(values, "background_opacity", base.background.opacityPercent),
            darkOverlayPercent = int(values, "background_dim", base.background.darkOverlayPercent),
            blurAmount = int(values, "background_blur", base.background.blurAmount)
        ).normalized()

        val custom = base.copy(
            rootBackground = int(values, "custom_root_background", base.rootBackground),
            panelSurface = int(values, "custom_panel_surface", base.panelSurface),
            toolbarSurface = int(values, "custom_toolbar_surface", base.toolbarSurface),
            keySurface = int(values, "custom_key_surface", base.keySurface),
            specialKeySurface = int(values, "custom_special_key_surface", base.specialKeySurface),
            disabledSurface = int(values, "custom_disabled_surface", base.disabledSurface),
            textPrimary = int(values, "custom_text_primary", base.textPrimary),
            textSecondary = int(values, "custom_text_secondary", base.textSecondary),
            keyLabel = int(values, "custom_key_label", base.keyLabel),
            specialKeyLabel = int(values, "custom_special_key_label", base.specialKeyLabel),
            disabledLabel = int(values, "custom_disabled_label", base.disabledLabel),
            primaryNeon = int(values, "custom_primary_neon", base.primaryNeon),
            secondaryNeon = int(values, "custom_secondary_neon", base.secondaryNeon),
            aiNeon = int(values, "custom_ai_neon", base.aiNeon),
            actionAccent = int(values, "custom_action_accent", base.actionAccent),
            dangerAccent = int(values, "custom_danger_accent", base.dangerAccent),
            keyCornerRadiusDp = float(values, "custom_key_corner_radius_dp", base.keyCornerRadiusDp),
            panelCornerRadiusDp = float(values, "custom_panel_corner_radius_dp", base.panelCornerRadiusDp),
            keyGapDp = float(values, "custom_key_gap_dp", base.keyGapDp),
            outerPaddingDp = float(values, "custom_outer_padding_dp", base.outerPaddingDp),
            keyLabelScale = float(values, "custom_key_label_scale", base.keyLabelScale),
            toolbarLabelScale = float(values, "custom_toolbar_label_scale", base.toolbarLabelScale),
            aiTitleScale = float(values, "custom_ai_title_scale", base.aiTitleScale),
            aiSubtitleScale = float(values, "custom_ai_subtitle_scale", base.aiSubtitleScale),
            borderOpacity = int(values, "custom_border_opacity", base.borderOpacity),
            glowStrength = int(values, "custom_glow_strength", base.glowStrength),
            pressedGlowStrength = int(values, "custom_pressed_glow_strength", base.pressedGlowStrength),
            glassOpacity = int(values, "custom_glass_opacity", base.glassOpacity),
            fillStyle = fillStyle(values["custom_fill_style"], base.fillStyle),
            keyGradientStart = int(values, "custom_key_gradient_start", base.keyGradientStart),
            keyGradientEnd = int(values, "custom_key_gradient_end", base.keyGradientEnd),
            specialGradientStart = int(values, "custom_special_gradient_start", base.specialGradientStart),
            specialGradientEnd = int(values, "custom_special_gradient_end", base.specialGradientEnd),
            panelGradientStart = int(values, "custom_panel_gradient_start", base.panelGradientStart),
            panelGradientEnd = int(values, "custom_panel_gradient_end", base.panelGradientEnd),
            background = background
        ).asCustom().normalized()

        val activeThemeId = normalizeActiveId(values[ACTIVE_THEME_ID])
        val globalPack = ThemePack.fromStored(values[THEME_GLOBAL_PACK_ID])
        val overrides = KeyboardThemeSurface.entries.mapNotNull { surface ->
            ThemePack.fromStored(values[overrideKey(surface)])?.let { pack -> surface to pack }
        }.toMap()
        val initialThemeSetupComplete = values[INITIAL_THEME_SETUP_COMPLETE]?.toBooleanStrictOrNull()
            ?: (globalPack != null)
        val selectionState = ThemeSelectionState(
            globalPack = globalPack,
            perSurfaceOverrides = overrides,
            legacyActiveThemeId = activeThemeId,
            customTheme = custom
        )

        return DecodedThemePreferences(
            activeThemeId = activeThemeId,
            customTheme = custom,
            selectionState = selectionState,
            initialThemeSetupComplete = initialThemeSetupComplete
        )
    }

    fun resolveActive(decoded: DecodedThemePreferences): KeyboardTheme =
        if (decoded.activeThemeId == "custom") decoded.customTheme
        else ThemePreset.byId(decoded.activeThemeId) ?: ThemePreset.socialAiNeon

    private fun normalizeActiveId(value: String?): String = when {
        value == "custom" -> "custom"
        ThemePreset.byId(value) != null -> value!!
        else -> ThemePreset.socialAiNeon.id
    }

    private fun fillStyle(value: String?, fallback: ThemeFillStyle): ThemeFillStyle =
        ThemeFillStyle.entries.firstOrNull { it.name == value } ?: fallback

    private fun int(values: Map<String, String>, key: String, fallback: Int): Int =
        values[key]?.toIntOrNull() ?: fallback

    private fun float(values: Map<String, String>, key: String, fallback: Float): Float =
        values[key]?.toFloatOrNull() ?: fallback
}
