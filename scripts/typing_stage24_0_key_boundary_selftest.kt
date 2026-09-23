import com.socialaiassistant.keyboard.ime.*

fun main() {
    var checks = 0
    fun checkCase(name: String, condition: Boolean) {
        check(condition) { name }
        checks++
    }

    val english = KeyboardLayout.forMode(KeyboardUiMode(language = KeyboardLanguage.ENGLISH))
    val englishLetters = english.rows.flatten().filter { it.action is KeyboardAction.Text && it.output?.all(Char::isLetter) == true }
    checkCase("english letter keys are alphabetic visual role", englishLetters.isNotEmpty() && englishLetters.all { it.visualRole == KeyVisualRole.ALPHABETIC })

    val phonetic = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.PHONETIC)
    )
    val phoneticQ = phonetic.rows.flatten().first { it.label == "q" }
    checkCase("phonetic latin keys are alphabetic visual role", phoneticQ.visualRole == KeyVisualRole.ALPHABETIC)

    val bijoy = KeyboardLayout.forMode(
        KeyboardUiMode(language = KeyboardLanguage.BANGLA, banglaMode = BanglaInputMode.BIJOY)
    )
    val bijoyMark = bijoy.rows.flatten().first { it.label == "ৌ" }
    checkCase("bijoy combining-mark key is alphabetic visual role", bijoyMark.visualRole == KeyVisualRole.ALPHABETIC)

    val withNumbers = KeyboardLayout.forMode(KeyboardUiMode(), showNumberRow = true)
    val digitOne = withNumbers.rows.first().first { it.label == "1" }
    checkCase("optional number row stays standard visual role", digitOne.visualRole == KeyVisualRole.STANDARD)

    val shift = english.rows.flatten().first { it.action == KeyboardAction.Shift }
    val backspace = english.rows.flatten().first { it.action == KeyboardAction.Backspace }
    checkCase("special keys stay standard visual role", shift.visualRole == KeyVisualRole.STANDARD && backspace.visualRole == KeyVisualRole.STANDARD)

    val numeric = KeyboardLayout.numericPad(KeyboardUiMode(layer = KeyboardLayer.NUMBERS))
    checkCase("dedicated numeric pad stays standard visual role", numeric.digitRows.flatten().all { it.visualRole == KeyVisualRole.STANDARD })

    val signature = KeyboardRenderSignature(
        mode = KeyboardUiMode(),
        showNumberRow = false,
        keyBoundaryEnabled = false,
        quickKeys = emptyList(),
        keyHeightDp = 50,
        oneHandedMode = "off",
        glideTyping = false,
        spacebarCursorControl = true,
        glideEligiblePolicy = true,
        keyGapBits = 0,
        enterLabel = "↵"
    )
    val gate = KeyboardRenderGate()
    checkCase("first boundary signature renders", gate.shouldRebuild(signature, 0))
    checkCase("boundary preference change rebuilds", gate.shouldRebuild(signature.copy(keyBoundaryEnabled = true), 4))

    println("STAGE 24.0 KEY BOUNDARY SELF-TEST: PASS ($checks/$checks)")
}
