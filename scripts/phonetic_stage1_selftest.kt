import com.socialaiassistant.keyboard.ime.BanglaPhoneticComposer

fun main() {
    val cases = linkedMapOf(
        "ami" to "আমি",
        "bangla" to "বাংলা",
        "tumi" to "তুমি",
        "tomar" to "তোমার",
        "kemon" to "কেমন",
        "acho" to "আছো",
        "bhalobashi" to "ভালোবাসি",
        "korbo" to "করবো",
        "jacchi" to "যাচ্ছি",
        "dhonnobad" to "ধন্যবাদ",
        "bangladesh" to "বাংলাদেশ",
        "engineer" to "ইঞ্জিনিয়ার",
        "kalo" to "কালো",
        "pani" to "পানি",
        "T" to "ট",
        "t" to "ত"
    )
    for ((roman, expected) in cases) {
        val composer = BanglaPhoneticComposer()
        roman.forEach { composer.acceptLatin(it.toString()) }
        val actual = composer.flush()
        check(actual == expected) { "$roman -> $actual (expected $expected)" }
    }
    val composer = BanglaPhoneticComposer()
    "ami".forEach { composer.acceptLatin(it.toString()) }
    check(composer.backspace().composing == "আম")
    check(composer.romanBuffer() == "am")
    println("phonetic_stage1_selftest: ${cases.size + 2} checks PASS")
}
