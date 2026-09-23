package com.socialaiassistant.keyboard.theme

enum class KeyboardThemeSurface(val storedId: String, val displayName: String) {
    ENGLISH("english", "English"),
    NUMBER("number", "Number"),
    SYMBOL("symbol", "Symbol"),
    BANGLA("bangla", "বাংলা"),
    PHONETIC("phonetic", "Phonetic"),
    BIJOY("bijoy", "Bijoy"),
    SETTINGS("settings", "Settings");

    companion object {
        fun fromStored(value: String?): KeyboardThemeSurface? = entries.firstOrNull { it.storedId == value }
    }
}
