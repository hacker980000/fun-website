package com.socialaiassistant.keyboard.theme

import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity

data class ThemeBubbleAppearance(
    val enabled: Boolean = false,
    val intensity: BubbleKeyIntensity = BubbleKeyIntensity.NORMAL
)

object ThemeBubbleAppearanceMutations {
    fun withEnabled(current: ThemeBubbleAppearance, enabled: Boolean): ThemeBubbleAppearance =
        current.copy(enabled = enabled)

    fun withIntensity(
        current: ThemeBubbleAppearance,
        intensity: BubbleKeyIntensity
    ): ThemeBubbleAppearance = current.copy(intensity = intensity)
}
