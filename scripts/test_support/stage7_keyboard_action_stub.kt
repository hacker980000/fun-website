package com.socialaiassistant.keyboard.ime

sealed interface KeyboardAction {
    data class Text(val value: String) : KeyboardAction
    data object Backspace : KeyboardAction
    data object Enter : KeyboardAction
    data object Space : KeyboardAction
    data object Shift : KeyboardAction
    data object ToggleLanguage : KeyboardAction
    data object ToggleBanglaMode : KeyboardAction
    data object ShowNumbers : KeyboardAction
    data object ShowSymbols : KeyboardAction
    data object ShowLetters : KeyboardAction
    data object OpenEmoji : KeyboardAction
    data object OpenClipboard : KeyboardAction
    data object OpenAiPanel : KeyboardAction
    data object OpenVoice : KeyboardAction
    data object OpenSettings : KeyboardAction
}
