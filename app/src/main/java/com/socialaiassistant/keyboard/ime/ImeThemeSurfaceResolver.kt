package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.theme.KeyboardThemeSurface

internal object ImeThemeSurfaceResolver {
    fun forMode(mode: KeyboardUiMode): KeyboardThemeSurface = when (mode.layer) {
        KeyboardLayer.NUMBERS -> KeyboardThemeSurface.NUMBER
        KeyboardLayer.SYMBOLS -> KeyboardThemeSurface.SYMBOL
        KeyboardLayer.LETTERS -> when (mode.language) {
            KeyboardLanguage.ENGLISH -> KeyboardThemeSurface.ENGLISH
            KeyboardLanguage.BANGLA -> when (mode.banglaMode) {
                BanglaInputMode.PHONETIC -> KeyboardThemeSurface.PHONETIC
                BanglaInputMode.BIJOY -> KeyboardThemeSurface.BIJOY
            }
        }
    }
}
