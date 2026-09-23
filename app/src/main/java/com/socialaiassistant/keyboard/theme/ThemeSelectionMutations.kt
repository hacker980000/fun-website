package com.socialaiassistant.keyboard.theme

object ThemeSelectionMutations {
    fun withCustomAppearance(state: ThemeSelectionState, theme: KeyboardTheme): ThemeSelectionState =
        state.copy(
            globalPack = null,
            legacyActiveThemeId = "custom",
            customTheme = theme.asCustom().normalized()
        )
}
