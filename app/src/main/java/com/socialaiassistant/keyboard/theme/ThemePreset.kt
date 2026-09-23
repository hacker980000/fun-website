package com.socialaiassistant.keyboard.theme

object ThemePreset {
    private const val WHITE = -0x1
    private const val SOFT_WHITE = -0x110001
    private const val MUTED = -0x666667

    val socialAiNeon = theme(
        id = "social_ai_neon", name = "Social AI Neon",
        root = 0xFF090D19.toInt(), panel = 0xE8171C2D.toInt(), toolbar = 0xE0111725.toInt(),
        key = 0xE51B2232.toInt(), special = 0xF0222940.toInt(), disabled = 0xAA171B27.toInt(),
        primary = 0xFF19D9FF.toInt(), secondary = 0xFF8A38FF.toInt(), ai = 0xFFC33CFF.toInt(), action = 0xFF1C8CFF.toInt()
    )

    val cyberBlue = theme(
        id = "cyber_blue", name = "Cyber Blue",
        root = 0xFF050A12.toInt(), panel = 0xE5111B2A.toInt(), toolbar = 0xE00B1320.toInt(),
        key = 0xE5172233.toInt(), special = 0xEE1D3047.toInt(), disabled = 0xAA101722.toInt(),
        primary = 0xFF00C8FF.toInt(), secondary = 0xFF2563FF.toInt(), ai = 0xFF4D7CFE.toInt(), action = 0xFF00A7FF.toInt()
    )

    val neonViolet = theme(
        id = "neon_violet", name = "Neon Violet",
        root = 0xFF100A16.toInt(), panel = 0xE5221630.toInt(), toolbar = 0xE0171022.toInt(),
        key = 0xE52A2033.toInt(), special = 0xEE352443.toInt(), disabled = 0xAA1D1822.toInt(),
        primary = 0xFFD846FF.toInt(), secondary = 0xFFFF4FD8.toInt(), ai = 0xFFB26CFF.toInt(), action = 0xFF8A5CFF.toInt()
    )

    val auroraCyan = theme(
        id = "aurora_cyan", name = "Aurora Cyan",
        root = 0xFF061416.toInt(), panel = 0xE50B262A.toInt(), toolbar = 0xE0081C20.toInt(),
        key = 0xE5112D31.toInt(), special = 0xEE173C3F.toInt(), disabled = 0xAA0C2022.toInt(),
        primary = 0xFF2AF6E8.toInt(), secondary = 0xFF2AD3FF.toInt(), ai = 0xFF5ED4FF.toInt(), action = 0xFF14B8D4.toInt()
    )

    val blackGold = theme(
        id = "black_gold", name = "Black & Gold",
        root = 0xFF050505.toInt(), panel = 0xE5161410.toInt(), toolbar = 0xE00D0C09.toInt(),
        key = 0xE51D1A13.toInt(), special = 0xEE282216.toInt(), disabled = 0xAA15130F.toInt(),
        primary = 0xFFFFC84A.toInt(), secondary = 0xFFFFE49B.toInt(), ai = 0xFFFFB629.toInt(), action = 0xFFDFAF36.toInt()
    )

    val crimsonPulse = theme(
        id = "crimson_pulse", name = "Crimson Pulse",
        root = 0xFF12070B.toInt(), panel = 0xE5281118.toInt(), toolbar = 0xE01B0C10.toInt(),
        key = 0xE52E171D.toInt(), special = 0xEE402029.toInt(), disabled = 0xAA201217.toInt(),
        primary = 0xFFFF315C.toInt(), secondary = 0xFFFF6C8A.toInt(), ai = 0xFFFF477A.toInt(), action = 0xFFE82C55.toInt()
    )

    val frostGlass = theme(
        id = "frost_glass", name = "Frost Glass",
        root = 0xFF0D1420.toInt(), panel = 0xD9273444.toInt(), toolbar = 0xD91C2837.toInt(),
        key = 0xCF344251.toInt(), special = 0xDD405164.toInt(), disabled = 0x99313A46.toInt(),
        primary = 0xFFB9EBFF.toInt(), secondary = 0xFF7DCFFF.toInt(), ai = 0xFFAAC8FF.toInt(), action = 0xFF6EC9FF.toInt(),
        glassOpacity = 72
    )

    val pureAmoled = theme(
        id = "pure_amoled", name = "Pure AMOLED",
        root = 0xFF000000.toInt(), panel = 0xF0080A0E.toInt(), toolbar = 0xF005070A.toInt(),
        key = 0xF00D1118.toInt(), special = 0xF0151C27.toInt(), disabled = 0xAA090C10.toInt(),
        primary = 0xFF25B8FF.toInt(), secondary = 0xFF6A79FF.toInt(), ai = 0xFF8A5CFF.toInt(), action = 0xFF1289FF.toInt(),
        glowStrength = 42, borderOpacity = 58, glassOpacity = 96
    )

    val builtIns: List<KeyboardTheme> = listOf(
        socialAiNeon, cyberBlue, neonViolet, auroraCyan, blackGold, crimsonPulse, frostGlass, pureAmoled
    )

    fun byId(id: String?): KeyboardTheme? = builtIns.firstOrNull { it.id == id }

    fun customFrom(base: KeyboardTheme = socialAiNeon): KeyboardTheme = base.asCustom()

    private fun theme(
        id: String,
        name: String,
        root: Int,
        panel: Int,
        toolbar: Int,
        key: Int,
        special: Int,
        disabled: Int,
        primary: Int,
        secondary: Int,
        ai: Int,
        action: Int,
        glowStrength: Int = 68,
        borderOpacity: Int = 72,
        glassOpacity: Int = 86
    ): KeyboardTheme = KeyboardTheme(
        id = id,
        displayName = name,
        isBuiltIn = true,
        rootBackground = root,
        panelSurface = panel,
        toolbarSurface = toolbar,
        keySurface = key,
        specialKeySurface = special,
        disabledSurface = disabled,
        textPrimary = WHITE,
        textSecondary = MUTED,
        keyLabel = SOFT_WHITE,
        specialKeyLabel = WHITE,
        disabledLabel = 0xFF737887.toInt(),
        primaryNeon = primary,
        secondaryNeon = secondary,
        aiNeon = ai,
        actionAccent = action,
        dangerAccent = 0xFFFF536F.toInt(),
        borderOpacity = borderOpacity,
        glowStrength = glowStrength,
        pressedGlowStrength = (glowStrength + 22).coerceAtMost(100),
        glassOpacity = glassOpacity
    ).normalized()
}
