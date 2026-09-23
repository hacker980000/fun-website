import com.socialaiassistant.keyboard.ime.ImeEditorBehaviorPolicy
import com.socialaiassistant.keyboard.ime.KeyboardLayer

private var passed = 0
private fun checkCase(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    passed++
}

fun main() {
    checkCase("text fields start on letters", ImeEditorBehaviorPolicy.preferredLayer(0x1) == KeyboardLayer.LETTERS)
    checkCase("number fields start on numbers", ImeEditorBehaviorPolicy.preferredLayer(0x2) == KeyboardLayer.NUMBERS)
    checkCase("phone fields start on numbers", ImeEditorBehaviorPolicy.preferredLayer(0x3) == KeyboardLayer.NUMBERS)
    checkCase("datetime fields start on numbers", ImeEditorBehaviorPolicy.preferredLayer(0x4) == KeyboardLayer.NUMBERS)
    checkCase("variation bits do not break class masking", ImeEditorBehaviorPolicy.preferredLayer(0x00002002) == KeyboardLayer.NUMBERS)

    checkCase("go action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x2) == "Go")
    checkCase("search action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x3) == "Search")
    checkCase("send action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x4) == "Send")
    checkCase("next action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x5) == "Next")
    checkCase("done action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x6) == "Done")
    checkCase("previous action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x7) == "Previous")
    checkCase("no-enter-action forces newline label", ImeEditorBehaviorPolicy.enterKeyLabel(0x40000004) == "↵")

    checkCase(
        "normal composing callback is preserved",
        !ImeEditorBehaviorPolicy.shouldAbortCompositionForSelection(true, 8, 8, 4, 8)
    )
    checkCase(
        "cursor move away aborts local composition",
        ImeEditorBehaviorPolicy.shouldAbortCompositionForSelection(true, 5, 5, 4, 8)
    )
    checkCase(
        "range selection aborts local composition",
        ImeEditorBehaviorPolicy.shouldAbortCompositionForSelection(true, 4, 8, 4, 8)
    )
    checkCase(
        "missing candidate region aborts stale local composition",
        ImeEditorBehaviorPolicy.shouldAbortCompositionForSelection(true, 8, 8, -1, -1)
    )
    checkCase(
        "selection changes do not matter when not composing",
        !ImeEditorBehaviorPolicy.shouldAbortCompositionForSelection(false, 1, 2, -1, -1)
    )

    println("TYPING STAGE 11 IME LIFECYCLE SELF-TEST: PASS ($passed/17)")
}
