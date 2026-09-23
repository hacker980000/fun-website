package com.socialaiassistant.keyboard.theme

object ThemeCatalog {
    private data class Family(
        val root: Int,
        val panel: Int,
        val toolbar: Int,
        val key: Int,
        val special: Int,
        val text: Int,
        val secondaryText: Int,
        val primary: Int,
        val secondary: Int,
        val ai: Int,
        val action: Int,
        val fillStyle: ThemeFillStyle,
        val keyRadius: Float,
        val panelRadius: Float,
        val gap: Float,
        val glow: Int,
        val border: Int,
        val glass: Int,
        val keyGradientStart: Int = key,
        val keyGradientEnd: Int = key,
        val specialGradientStart: Int = special,
        val specialGradientEnd: Int = special
    )

    private data class SurfaceProfile(
        val radiusDelta: Float = 0f,
        val gapDelta: Float = 0f,
        val labelScale: Float = 1f,
        val accentMix: Float = 0f
    )

    private val families = mapOf(
        ThemePack.CLASSIC_DARK to Family(
            root = 0xFF0A0D12.toInt(), panel = 0xFF131821.toInt(), toolbar = 0xFF10151D.toInt(),
            key = 0xFF1A202A.toInt(), special = 0xFF262E3A.toInt(),
            text = 0xFFF7F9FC.toInt(), secondaryText = 0xFF9AA6B5.toInt(),
            primary = 0xFF4DA3FF.toInt(), secondary = 0xFF7C8B9D.toInt(), ai = 0xFF8B5CF6.toInt(), action = 0xFF2F8CFF.toInt(),
            fillStyle = ThemeFillStyle.SOLID, keyRadius = 8f, panelRadius = 12f, gap = 3f,
            glow = 24, border = 46, glass = 100
        ),
        ThemePack.GLASS_MODERN to Family(
            root = 0xFF07111F.toInt(), panel = 0xD21A2A3C.toInt(), toolbar = 0xD0122234.toInt(),
            key = 0xC8233548.toInt(), special = 0xD02B425A.toInt(),
            text = 0xFFF6FBFF.toInt(), secondaryText = 0xFFA8BDD0.toInt(),
            primary = 0xFF5CD6FF.toInt(), secondary = 0xFF62A8FF.toInt(), ai = 0xFFB67CFF.toInt(), action = 0xFF3B9EFF.toInt(),
            fillStyle = ThemeFillStyle.GLASS, keyRadius = 16f, panelRadius = 20f, gap = 5f,
            glow = 48, border = 58, glass = 72
        ),
        ThemePack.CLEAN_LIGHT to Family(
            root = 0xFFF4F7FB.toInt(), panel = 0xFFFFFFFF.toInt(), toolbar = 0xFFF0F4F8.toInt(),
            key = 0xFFFFFFFF.toInt(), special = 0xFFE8EEF6.toInt(),
            text = 0xFF172033.toInt(), secondaryText = 0xFF657185.toInt(),
            primary = 0xFF2F6FED.toInt(), secondary = 0xFF5B6B85.toInt(), ai = 0xFF7557D9.toInt(), action = 0xFF245EDB.toInt(),
            fillStyle = ThemeFillStyle.SOLID, keyRadius = 12f, panelRadius = 16f, gap = 4f,
            glow = 10, border = 24, glass = 100
        ),
        ThemePack.GRADIENT_PRO to Family(
            root = 0xFF090A18.toInt(), panel = 0xE3161830.toInt(), toolbar = 0xE0111327.toInt(),
            key = 0xE31B1D38.toInt(), special = 0xE72A2149.toInt(),
            text = 0xFFFFFFFF.toInt(), secondaryText = 0xFFB9B8D8.toInt(),
            primary = 0xFF6A5CFF.toInt(), secondary = 0xFF00C8FF.toInt(), ai = 0xFFE05CFF.toInt(), action = 0xFF3E8BFF.toInt(),
            fillStyle = ThemeFillStyle.GRADIENT, keyRadius = 18f, panelRadius = 22f, gap = 4f,
            glow = 70, border = 66, glass = 90,
            keyGradientStart = 0xFF312A65.toInt(), keyGradientEnd = 0xFF153A63.toInt(),
            specialGradientStart = 0xFF51306F.toInt(), specialGradientEnd = 0xFF18507B.toInt()
        )
    )

    private val profiles = mapOf(
        KeyboardThemeSurface.ENGLISH to SurfaceProfile(),
        KeyboardThemeSurface.NUMBER to SurfaceProfile(radiusDelta = 2f, gapDelta = 1f, labelScale = 1.06f),
        KeyboardThemeSurface.SYMBOL to SurfaceProfile(radiusDelta = -1f, labelScale = 0.96f),
        KeyboardThemeSurface.BANGLA to SurfaceProfile(radiusDelta = 1f, labelScale = 1.05f, accentMix = 0.08f),
        KeyboardThemeSurface.PHONETIC to SurfaceProfile(gapDelta = 0.5f, labelScale = 1.01f, accentMix = 0.05f),
        KeyboardThemeSurface.BIJOY to SurfaceProfile(radiusDelta = -2f, gapDelta = -0.5f, labelScale = 0.98f, accentMix = 0.10f),
        KeyboardThemeSurface.SETTINGS to SurfaceProfile(radiusDelta = 3f, gapDelta = 1f, labelScale = 1.00f, accentMix = 0.04f)
    )

    private val displayNames: Map<ThemePack, Map<KeyboardThemeSurface, String>> = mapOf(
        ThemePack.CLASSIC_DARK to mapOf(
            KeyboardThemeSurface.ENGLISH to "Classic Dark",
            KeyboardThemeSurface.NUMBER to "Classic Dark",
            KeyboardThemeSurface.SYMBOL to "Standard Dark",
            KeyboardThemeSurface.BANGLA to "Classic Dark",
            KeyboardThemeSurface.PHONETIC to "Dark Classic",
            KeyboardThemeSurface.BIJOY to "Classic Bijoy",
            KeyboardThemeSurface.SETTINGS to "Clean Modern"
        ),
        ThemePack.GLASS_MODERN to mapOf(
            KeyboardThemeSurface.ENGLISH to "Glass Modern",
            KeyboardThemeSurface.NUMBER to "Rounded Modern",
            KeyboardThemeSurface.SYMBOL to "Rounded Modern",
            KeyboardThemeSurface.BANGLA to "Rounded Modern",
            KeyboardThemeSurface.PHONETIC to "Rounded Modern",
            KeyboardThemeSurface.BIJOY to "Modern Bijoy",
            KeyboardThemeSurface.SETTINGS to "Card Style"
        ),
        ThemePack.CLEAN_LIGHT to mapOf(
            KeyboardThemeSurface.ENGLISH to "Clean Light",
            KeyboardThemeSurface.NUMBER to "Clean Light",
            KeyboardThemeSurface.SYMBOL to "Light Clean",
            KeyboardThemeSurface.BANGLA to "Clean Light",
            KeyboardThemeSurface.PHONETIC to "Light Clean",
            KeyboardThemeSurface.BIJOY to "Light Bijoy",
            KeyboardThemeSurface.SETTINGS to "Premium"
        ),
        ThemePack.GRADIENT_PRO to mapOf(
            KeyboardThemeSurface.ENGLISH to "Gradient Pro",
            KeyboardThemeSurface.NUMBER to "Stylish Pro",
            KeyboardThemeSurface.SYMBOL to "Pro Style",
            KeyboardThemeSurface.BANGLA to "Elegant Pro",
            KeyboardThemeSurface.PHONETIC to "Stylish Pro",
            KeyboardThemeSurface.BIJOY to "Pro Bijoy",
            KeyboardThemeSurface.SETTINGS to "Pro Style"
        )
    )

    fun globalChrome(pack: ThemePack): KeyboardTheme = build(pack, KeyboardThemeSurface.ENGLISH, chrome = true)

    fun surface(pack: ThemePack, surface: KeyboardThemeSurface): KeyboardTheme = build(pack, surface, chrome = false)

    fun allVariants(): List<KeyboardTheme> = ThemePack.entries.flatMap { pack ->
        KeyboardThemeSurface.entries.map { surface -> surface(pack, surface) }
    }

    private fun build(pack: ThemePack, surface: KeyboardThemeSurface, chrome: Boolean): KeyboardTheme {
        val family = requireNotNull(families[pack])
        val profile = requireNotNull(profiles[surface])
        val displayName = if (chrome) pack.displayName else requireNotNull(displayNames[pack]?.get(surface))
        val id = if (chrome) "premium_${pack.storedId}_chrome" else "premium_${pack.storedId}_${surface.storedId}"
        val primary = mix(family.primary, family.ai, profile.accentMix)
        val keySurface = mix(family.key, family.primary, profile.accentMix * 0.35f)
        val specialSurface = mix(family.special, family.secondary, profile.accentMix * 0.45f)
        val disabledSurface = mix(keySurface, family.root, 0.55f)
        val disabledLabel = mix(family.secondaryText, family.root, 0.25f)
        val panelGradientStart = mix(family.panel, family.primary, 0.12f)
        val panelGradientEnd = mix(family.panel, family.secondary, 0.10f)

        return KeyboardTheme(
            id = id,
            displayName = displayName,
            isBuiltIn = true,
            rootBackground = family.root,
            panelSurface = family.panel,
            toolbarSurface = family.toolbar,
            keySurface = keySurface,
            specialKeySurface = specialSurface,
            disabledSurface = disabledSurface,
            textPrimary = family.text,
            textSecondary = family.secondaryText,
            keyLabel = family.text,
            specialKeyLabel = family.text,
            disabledLabel = disabledLabel,
            primaryNeon = primary,
            secondaryNeon = family.secondary,
            aiNeon = family.ai,
            actionAccent = family.action,
            dangerAccent = 0xFFFF536F.toInt(),
            keyCornerRadiusDp = (family.keyRadius + if (chrome) 0f else profile.radiusDelta).coerceIn(4f, 22f),
            panelCornerRadiusDp = family.panelRadius.coerceIn(6f, 28f),
            keyGapDp = (family.gap + if (chrome) 0f else profile.gapDelta).coerceIn(0f, 12f),
            outerPaddingDp = if (surface == KeyboardThemeSurface.SETTINGS) 8f else 4f,
            keyLabelScale = if (chrome) 1f else profile.labelScale,
            toolbarLabelScale = 1f,
            aiTitleScale = 1f,
            aiSubtitleScale = 1f,
            borderOpacity = family.border,
            glowStrength = family.glow,
            pressedGlowStrength = (family.glow + 22).coerceAtMost(100),
            glassOpacity = family.glass,
            fillStyle = family.fillStyle,
            keyGradientStart = mix(family.keyGradientStart, family.primary, profile.accentMix),
            keyGradientEnd = mix(family.keyGradientEnd, family.secondary, profile.accentMix),
            specialGradientStart = mix(family.specialGradientStart, family.ai, profile.accentMix),
            specialGradientEnd = mix(family.specialGradientEnd, family.secondary, profile.accentMix),
            panelGradientStart = panelGradientStart,
            panelGradientEnd = panelGradientEnd
        ).normalized()
    }

    private fun mix(base: Int, accent: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val b = base ushr shift and 0xFF
            val c = accent ushr shift and 0xFF
            return (b + (c - b) * a).toInt().coerceIn(0, 255)
        }
        val alpha = base ushr 24 and 0xFF
        return (alpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
