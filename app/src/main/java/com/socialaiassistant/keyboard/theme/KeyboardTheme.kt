package com.socialaiassistant.keyboard.theme

enum class ThemeFillStyle { SOLID, GLASS, GRADIENT }

enum class BackgroundScope {
    FULL_KEYBOARD,
    KEYS_ONLY,
    AI_PANEL_ONLY;

    companion object {
        fun fromStored(value: String?): BackgroundScope = entries.firstOrNull { it.name == value } ?: FULL_KEYBOARD
    }
}

enum class BackgroundFit {
    FILL,
    FIT,
    CENTER_CROP;

    companion object {
        fun fromStored(value: String?): BackgroundFit = entries.firstOrNull { it.name == value } ?: CENTER_CROP
    }
}

data class BackgroundPhotoConfig(
    val enabled: Boolean = false,
    val localFileName: String = "",
    val scope: BackgroundScope = BackgroundScope.FULL_KEYBOARD,
    val fit: BackgroundFit = BackgroundFit.CENTER_CROP,
    val opacityPercent: Int = 100,
    val darkOverlayPercent: Int = 35,
    val blurAmount: Int = 0
) {
    fun normalized(): BackgroundPhotoConfig {
        val safeName = localFileName.substringAfterLast('/').substringAfterLast('\\').trim().take(120)
        return copy(
            enabled = enabled && safeName.isNotEmpty(),
            localFileName = safeName,
            opacityPercent = opacityPercent.coerceIn(0, 100),
            darkOverlayPercent = darkOverlayPercent.coerceIn(0, 90),
            blurAmount = blurAmount.coerceIn(0, 30)
        )
    }
}

data class KeyboardTheme(
    val id: String,
    val displayName: String,
    val isBuiltIn: Boolean,
    val rootBackground: Int,
    val panelSurface: Int,
    val toolbarSurface: Int,
    val keySurface: Int,
    val specialKeySurface: Int,
    val disabledSurface: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val keyLabel: Int,
    val specialKeyLabel: Int,
    val disabledLabel: Int,
    val primaryNeon: Int,
    val secondaryNeon: Int,
    val aiNeon: Int,
    val actionAccent: Int,
    val dangerAccent: Int,
    val keyCornerRadiusDp: Float = 10f,
    val panelCornerRadiusDp: Float = 14f,
    val keyGapDp: Float = 3f,
    val outerPaddingDp: Float = 4f,
    val keyLabelScale: Float = 1f,
    val toolbarLabelScale: Float = 1f,
    val aiTitleScale: Float = 1f,
    val aiSubtitleScale: Float = 1f,
    val borderOpacity: Int = 72,
    val glowStrength: Int = 68,
    val pressedGlowStrength: Int = 92,
    val glassOpacity: Int = 86,
    val fillStyle: ThemeFillStyle = ThemeFillStyle.SOLID,
    val keyGradientStart: Int = keySurface,
    val keyGradientEnd: Int = keySurface,
    val specialGradientStart: Int = specialKeySurface,
    val specialGradientEnd: Int = specialKeySurface,
    val panelGradientStart: Int = panelSurface,
    val panelGradientEnd: Int = panelSurface,
    val background: BackgroundPhotoConfig = BackgroundPhotoConfig()
) {
    fun normalized(): KeyboardTheme = copy(
        id = id.trim().ifBlank { "custom" }.take(60),
        displayName = displayName.trim().ifBlank { "Custom" }.take(80),
        keyCornerRadiusDp = keyCornerRadiusDp.coerceIn(4f, 22f),
        panelCornerRadiusDp = panelCornerRadiusDp.coerceIn(6f, 28f),
        keyGapDp = keyGapDp.coerceIn(0f, 12f),
        outerPaddingDp = outerPaddingDp.coerceIn(0f, 16f),
        keyLabelScale = keyLabelScale.coerceIn(0.80f, 1.35f),
        toolbarLabelScale = toolbarLabelScale.coerceIn(0.80f, 1.35f),
        aiTitleScale = aiTitleScale.coerceIn(0.80f, 1.35f),
        aiSubtitleScale = aiSubtitleScale.coerceIn(0.75f, 1.30f),
        borderOpacity = borderOpacity.coerceIn(0, 100),
        glowStrength = glowStrength.coerceIn(0, 100),
        pressedGlowStrength = pressedGlowStrength.coerceIn(0, 100),
        glassOpacity = glassOpacity.coerceIn(25, 100),
        background = background.normalized()
    )

    fun isValid(): Boolean {
        val n = normalized()
        return n.id.isNotBlank() && n.displayName.isNotBlank() &&
            n.keyCornerRadiusDp in 4f..22f && n.panelCornerRadiusDp in 6f..28f &&
            n.keyGapDp in 0f..12f && n.outerPaddingDp in 0f..16f &&
            n.keyLabelScale in 0.80f..1.35f && n.toolbarLabelScale in 0.80f..1.35f &&
            n.borderOpacity in 0..100 && n.glowStrength in 0..100 && n.glassOpacity in 25..100
    }

    fun asCustom(): KeyboardTheme = normalized().copy(id = "custom", displayName = "Custom", isBuiltIn = false)

    fun visualSignature(): Int = listOf(
        rootBackground, panelSurface, toolbarSurface, keySurface, specialKeySurface,
        textPrimary, keyLabel, primaryNeon, secondaryNeon, aiNeon, actionAccent,
        keyCornerRadiusDp.toBits(), panelCornerRadiusDp.toBits(), keyGapDp.toBits(),
        borderOpacity, glowStrength, pressedGlowStrength, glassOpacity,
        fillStyle.ordinal, keyGradientStart, keyGradientEnd,
        specialGradientStart, specialGradientEnd, panelGradientStart, panelGradientEnd
    ).fold(17) { acc, value -> 31 * acc + value }
}
