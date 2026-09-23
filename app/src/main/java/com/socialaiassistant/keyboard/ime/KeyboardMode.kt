package com.socialaiassistant.keyboard.ime

enum class KeyboardLanguage {
    ENGLISH,
    BANGLA
}

enum class BanglaInputMode {
    PHONETIC,
    BIJOY
}

enum class KeyboardLayer {
    LETTERS,
    NUMBERS,
    SYMBOLS
}

data class KeyboardUiMode(
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val banglaMode: BanglaInputMode = BanglaInputMode.PHONETIC,
    val layer: KeyboardLayer = KeyboardLayer.LETTERS,
    val shifted: Boolean = false
) {
    fun reduce(action: KeyboardAction): KeyboardUiMode = when (action) {
        KeyboardAction.ToggleLanguage -> copy(
            language = if (language == KeyboardLanguage.ENGLISH) KeyboardLanguage.BANGLA else KeyboardLanguage.ENGLISH,
            layer = KeyboardLayer.LETTERS,
            shifted = false
        )
        KeyboardAction.ToggleBanglaMode -> copy(
            banglaMode = if (banglaMode == BanglaInputMode.PHONETIC) BanglaInputMode.BIJOY else BanglaInputMode.PHONETIC,
            layer = KeyboardLayer.LETTERS,
            shifted = false
        )
        KeyboardAction.ShowNumbers -> copy(layer = KeyboardLayer.NUMBERS, shifted = false)
        KeyboardAction.ShowSymbols -> copy(layer = KeyboardLayer.SYMBOLS, shifted = false)
        KeyboardAction.ShowLetters -> copy(layer = KeyboardLayer.LETTERS, shifted = false)
        KeyboardAction.Shift -> copy(shifted = !shifted)
        else -> this
    }
}
