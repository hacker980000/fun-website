import com.socialaiassistant.keyboard.ime.*

fun main() {
    var checks = 0
    fun checkCase(name: String, condition: Boolean) {
        check(condition) { name }
        checks++
    }

    val emailKeys = listOf("@", ".", "_", "-", ".com")
    val en = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.ENGLISH),
        showNumberRow = false,
        quickKeys = emailKeys
    )
    checkCase("email quick row inserted", en.rows.first().map { it.label } == emailKeys)

    val bn = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC),
        showNumberRow = true,
        quickKeys = emailKeys
    )
    checkCase("number row remains first", bn.rows.first().map { it.label }.first() == "1")
    checkCase("quick row follows number row", bn.rows[1].map { it.label } == emailKeys)

    val numbers = KeyboardLayout.forMode(
        KeyboardUiMode(layer = KeyboardLayer.NUMBERS),
        showNumberRow = true,
        quickKeys = emailKeys
    )
    checkCase("number layer does not duplicate quick row", numbers.rows.none { row -> row.map { it.label } == emailKeys })

    val uriKeys = listOf("https://", "www.", ".com", "/", ".")
    val uri = KeyboardLayout.forMode(KeyboardUiMode(), quickKeys = uriKeys)
    checkCase("long quick key gets direct text action", (uri.rows.first().first().action as KeyboardAction.Text).value == "https://")
    checkCase("quick keys cap at six", KeyboardLayout.forMode(KeyboardUiMode(), quickKeys = listOf("1","2","3","4","5","6","7")).rows.first().size == 6)

    println("TYPING STAGE 12 LAYOUT SELF-TEST: PASS ($checks/6)")
}
