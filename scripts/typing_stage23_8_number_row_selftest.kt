import com.socialaiassistant.keyboard.ime.*

fun main() {
    var checks = 0
    fun expect(name: String, condition: Boolean) {
        check(condition) { name }
        checks++
    }

    val expectedNumberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val alphabeticModes = listOf(
        "English" to KeyboardUiMode(language = KeyboardLanguage.ENGLISH, layer = KeyboardLayer.LETTERS),
        "English shifted" to KeyboardUiMode(language = KeyboardLanguage.ENGLISH, layer = KeyboardLayer.LETTERS, shifted = true),
        "Bangla Phonetic" to KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC, layer = KeyboardLayer.LETTERS),
        "Bangla Phonetic shifted" to KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC, layer = KeyboardLayer.LETTERS, shifted = true),
        "Bangla Bijoy" to KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY, layer = KeyboardLayer.LETTERS),
        "Bangla Bijoy shifted" to KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY, layer = KeyboardLayer.LETTERS, shifted = true)
    )

    alphabeticModes.forEach { (name, mode) ->
        val enabled = KeyboardLayout.forMode(mode, showNumberRow = true)
        expect("$name number row ON", enabled.rows.first().map { it.label } == expectedNumberRow)

        val disabled = KeyboardLayout.forMode(mode, showNumberRow = false)
        expect("$name number row OFF", disabled.rows.none { row -> row.map { it.label } == expectedNumberRow })
    }

    val quickKeys = listOf("@", ".", "_", "-", ".com")
    val withQuickKeys = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC),
        showNumberRow = true,
        quickKeys = quickKeys
    )
    expect("number row stays above editor quick row", withQuickKeys.rows[0].map { it.label } == expectedNumberRow)
    expect("editor quick row follows optional number row", withQuickKeys.rows[1].map { it.label } == quickKeys)

    val withoutNumberWithQuickKeys = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.ENGLISH),
        showNumberRow = false,
        quickKeys = quickKeys
    )
    expect("quick row becomes first when number row is OFF", withoutNumberWithQuickKeys.rows[0].map { it.label } == quickKeys)

    val numbers = KeyboardLayout.forMode(
        KeyboardUiMode(layer = KeyboardLayer.NUMBERS),
        showNumberRow = true
    )
    expect("dedicated number pad never gets duplicate 1-0 row", numbers.rows.none { row -> row.map { it.label } == expectedNumberRow })

    val symbols = KeyboardLayout.forMode(
        KeyboardUiMode(layer = KeyboardLayer.SYMBOLS),
        showNumberRow = true
    )
    expect("symbol layer never gets optional 1-0 row", symbols.rows.none { row -> row.map { it.label } == expectedNumberRow })

    expect(
        "policy explicitly allows English letters",
        AlphabeticNumberRowPolicy.shouldShow(KeyboardUiMode(language = KeyboardLanguage.ENGLISH), true)
    )
    expect(
        "policy explicitly allows Bangla Phonetic letters",
        AlphabeticNumberRowPolicy.shouldShow(KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC), true)
    )
    expect(
        "policy explicitly allows Bangla Bijoy letters",
        AlphabeticNumberRowPolicy.shouldShow(KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY), true)
    )
    expect(
        "policy blocks dedicated number layer",
        !AlphabeticNumberRowPolicy.shouldShow(KeyboardUiMode(layer = KeyboardLayer.NUMBERS), true)
    )
    expect(
        "policy blocks all letters when setting is OFF",
        !AlphabeticNumberRowPolicy.shouldShow(KeyboardUiMode(language = KeyboardLanguage.ENGLISH), false)
    )

    println("STAGE 23.8 ALPHABETIC NUMBER ROW SELF-TEST: PASS ($checks/$checks)")
}
