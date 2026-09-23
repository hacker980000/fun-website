import com.socialaiassistant.keyboard.ime.BanglaInputMode
import com.socialaiassistant.keyboard.ime.KeyboardLanguage
import com.socialaiassistant.keyboard.ime.KeyboardLayer
import com.socialaiassistant.keyboard.ime.KeyboardRenderGate
import com.socialaiassistant.keyboard.ime.KeyboardRenderSignature
import com.socialaiassistant.keyboard.ime.KeyboardUiMode
import com.socialaiassistant.keyboard.ime.SingleEntryMemo
import com.socialaiassistant.keyboard.ime.SuggestionBarRenderGate
import com.socialaiassistant.keyboard.ime.SuggestionBarSignature

private var checks = 0

private fun checkThat(value: Boolean, message: String) {
    checks++
    check(value) { message }
}

fun main() {
    val mode = KeyboardUiMode(
        language = KeyboardLanguage.ENGLISH,
        banglaMode = BanglaInputMode.PHONETIC,
        layer = KeyboardLayer.LETTERS,
        shifted = false
    )
    val signature = KeyboardRenderSignature(
        mode = mode,
        showNumberRow = false,
        quickKeys = emptyList(),
        keyHeightDp = 50,
        oneHandedMode = "off",
        glideTyping = false,
        spacebarCursorControl = true,
        glideEligiblePolicy = true,
        keyGapBits = 3f.toBits(),
        enterLabel = "↵"
    )
    val keyboardGate = KeyboardRenderGate()
    checkThat(keyboardGate.shouldRebuild(signature, 0), "first keyboard render must rebuild")
    checkThat(!keyboardGate.shouldRebuild(signature, 4), "identical populated keyboard must coalesce")
    checkThat(keyboardGate.shouldRebuild(signature.copy(showNumberRow = true), 4), "layout setting change must rebuild")
    checkThat(!keyboardGate.shouldRebuild(signature.copy(showNumberRow = true), 5), "same changed signature should coalesce")
    checkThat(keyboardGate.shouldRebuild(signature.copy(showNumberRow = true, spacebarCursorControl = false), 5), "spacebar listener setting must rebuild")
    checkThat(keyboardGate.shouldRebuild(signature.copy(showNumberRow = true, spacebarCursorControl = false, glideEligiblePolicy = false), 5), "editor glide policy change must rebuild")
    keyboardGate.invalidate()
    checkThat(keyboardGate.shouldRebuild(signature.copy(showNumberRow = true), 5), "invalidated keyboard gate must rebuild")
    checkThat(keyboardGate.shouldRebuild(signature.copy(showNumberRow = true), 0), "missing child rows must force rebuild")

    var computes = 0
    val memo = SingleEntryMemo<String, List<String>>()
    val first = memo.getOrCompute("rev-1") { computes++; listOf("one", "two") }
    val second = memo.getOrCompute("rev-1") { computes++; listOf("bad") }
    checkThat(first == second, "same memo key must reuse result")
    checkThat(computes == 1, "same memo key must compute once")
    val third = memo.getOrCompute("rev-2") { computes++; listOf("three") }
    checkThat(third == listOf("three"), "new memo revision must recompute")
    checkThat(computes == 2, "new memo key must increment compute count")
    memo.clear()
    memo.getOrCompute("rev-2") { computes++; listOf("four") }
    checkThat(computes == 3, "cleared memo must recompute")

    val barGate = SuggestionBarRenderGate()
    val visible = SuggestionBarSignature(true, listOf("আমি|BANGLA|BANGLA|false", "আমার|BANGLA|BANGLA|false"))
    checkThat(barGate.shouldRebuild(visible, 0), "first suggestion render must build")
    checkThat(!barGate.shouldRebuild(visible, 2), "identical suggestion buttons must coalesce")
    checkThat(barGate.shouldRebuild(visible, 1), "child mismatch must repair suggestion bar")
    val changed = SuggestionBarSignature(true, listOf("আমি|BANGLA|BANGLA|false", "আমাকে|BANGLA|BANGLA|false"))
    checkThat(barGate.shouldRebuild(changed, 2), "candidate change must rebuild suggestion bar")
    val hidden = SuggestionBarSignature(false)
    checkThat(barGate.shouldRebuild(hidden, 2), "visible to hidden transition must rebuild")
    checkThat(!barGate.shouldRebuild(hidden, 0), "already-hidden empty bar must coalesce")
    barGate.invalidate()
    checkThat(barGate.shouldRebuild(hidden, 0), "invalidated suggestion gate must apply state again")

    println("Stage18 render/memo self-test: $checks/$checks PASS")
}
