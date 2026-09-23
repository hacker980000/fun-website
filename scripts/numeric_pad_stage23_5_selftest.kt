import com.socialaiassistant.keyboard.ime.*

fun main() {
    var checks = 0
    fun expect(name: String, condition: Boolean) {
        check(condition) { name }
        checks++
    }

    val pad = KeyboardLayout.numericPad(KeyboardUiMode(layer = KeyboardLayer.NUMBERS))

    expect("left operator rail", pad.leftRail.map { it.label } == listOf("+", "-", "*", "/"))
    expect("three digit rows", pad.digitRows.map { row -> row.map { it.label } } == listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    ))
    expect("right action rail", pad.rightRail.map { it.label } == listOf("%", "␣", "⌫"))
    expect("right rail space action", pad.rightRail[1].action == KeyboardAction.Space)
    expect("right rail backspace action", pad.rightRail[2].action == KeyboardAction.Backspace)
    expect("bottom row labels", pad.bottomRow.map { it.label } == listOf("ABC", ",", "!?#", "0", "=", ".", "↵"))
    expect("ABC returns to letters", pad.bottomRow.first().action == KeyboardAction.ShowLetters)
    expect("symbol switch", pad.bottomRow[2].action == KeyboardAction.ShowSymbols)
    expect("zero commits zero", (pad.bottomRow[3].action as? KeyboardAction.Text)?.value == "0")
    expect("enter action", pad.bottomRow.last().action == KeyboardAction.Enter)
    expect("bottom row width units", kotlin.math.abs(pad.bottomRow.sumOf { it.weight.toDouble() } - 9.0) < 0.0001)

    val fallback = KeyboardLayout.forMode(KeyboardUiMode(layer = KeyboardLayer.NUMBERS)).rows.flatten()
    expect("fallback exposes slash", fallback.any { (it.action as? KeyboardAction.Text)?.value == "/" })
    expect("fallback exposes digits", (1..9).all { digit -> fallback.any { it.label == digit.toString() } })

    println("STAGE 23.5 NUMERIC PAD SELF-TEST: PASS ($checks/13)")
}
