package com.socialaiassistant.keyboard.ime

data class PhoneticResult(
    val committed: String = "",
    val composing: String = ""
)

class BanglaPhoneticComposer(
    private val transliterator: BanglaPhoneticTransliterator = HybridBanglaPhoneticTransliterator()
) {
    private val buffer = StringBuilder()

    fun reset() {
        buffer.clear()
    }

    fun acceptLatin(token: String): PhoneticResult {
        if (token.isEmpty()) return PhoneticResult(composing = render())
        buffer.append(token)
        return PhoneticResult(composing = render())
    }

    fun backspace(): PhoneticResult {
        if (buffer.isNotEmpty()) buffer.deleteCharAt(buffer.lastIndex)
        return PhoneticResult(composing = render())
    }

    fun flush(): String {
        val output = render()
        buffer.clear()
        return output
    }

    fun isComposing(): Boolean = buffer.isNotEmpty()

    fun romanBuffer(): String = buffer.toString()

    fun renderedBuffer(): String = render()

    private fun render(): String {
        val raw = buffer.toString()
        if (raw.isEmpty()) return ""
        return transliterator.transliterate(raw)
    }
}
