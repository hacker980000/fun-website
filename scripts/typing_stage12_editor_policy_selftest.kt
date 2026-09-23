import com.socialaiassistant.keyboard.ime.EditorSurfaceKind
import com.socialaiassistant.keyboard.ime.ImeEditorBehaviorPolicy
import com.socialaiassistant.keyboard.ime.KeyboardLayer

private var passed = 0
private fun checkCase(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    passed++
}

fun main() {
    val normal = ImeEditorBehaviorPolicy.typingPolicy(0x00000001)
    checkCase("normal text uses letters", ImeEditorBehaviorPolicy.preferredLayer(0x1) == KeyboardLayer.LETTERS)
    checkCase("number uses number layer", ImeEditorBehaviorPolicy.preferredLayer(0x2) == KeyboardLayer.NUMBERS)
    checkCase("phone uses number layer", ImeEditorBehaviorPolicy.preferredLayer(0x3) == KeyboardLayer.NUMBERS)
    checkCase("datetime uses number layer", ImeEditorBehaviorPolicy.preferredLayer(0x4) == KeyboardLayer.NUMBERS)
    checkCase("normal kind", normal.kind == EditorSurfaceKind.GENERIC_TEXT)
    checkCase("normal shows suggestions", normal.showSuggestions)
    checkCase("normal learns", normal.allowLearning)
    checkCase("normal does not force latin", !normal.forceLiteralLatin)
    checkCase("normal without flag does not autocorrect", !normal.allowAutocorrect)
    checkCase("normal smart hints", normal.allowSmartLanguageHints)

    val autoCorrect = ImeEditorBehaviorPolicy.typingPolicy(0x00008001)
    checkCase("autocorrect flag respected", autoCorrect.allowAutocorrect)

    val noSuggestions = ImeEditorBehaviorPolicy.typingPolicy(0x00088001)
    checkCase("no-suggestions kind", noSuggestions.kind == EditorSurfaceKind.NO_SUGGESTIONS)
    checkCase("no-suggestions hides candidates", !noSuggestions.showSuggestions)
    checkCase("no-suggestions overrides autocorrect", !noSuggestions.allowAutocorrect)
    checkCase("no-suggestions disables learning", !noSuggestions.allowLearning)
    checkCase("no-suggestions disables cross-language hints", !noSuggestions.allowSmartLanguageHints)

    val autoComplete = ImeEditorBehaviorPolicy.typingPolicy(0x00010001)
    checkCase("editor autocomplete kind", autoComplete.kind == EditorSurfaceKind.EDITOR_AUTOCOMPLETE)
    checkCase("editor autocomplete owns candidates", !autoComplete.showSuggestions)
    checkCase("editor autocomplete disables autocorrect", !autoComplete.allowAutocorrect)
    checkCase("editor autocomplete disables learning", !autoComplete.allowLearning)

    val email = ImeEditorBehaviorPolicy.typingPolicy(0x00000021)
    checkCase("email kind", email.kind == EditorSurfaceKind.EMAIL)
    checkCase("email is literal latin", email.forceLiteralLatin)
    checkCase("email no suggestions", !email.showSuggestions)
    checkCase("email no learning", !email.allowLearning)
    checkCase("email has at-sign quick key", "@" in email.quickKeys)
    checkCase("email has dot-com quick key", ".com" in email.quickKeys)

    val webEmail = ImeEditorBehaviorPolicy.typingPolicy(0x000000d1)
    checkCase("web email kind", webEmail.kind == EditorSurfaceKind.EMAIL)
    checkCase("web email literal latin", webEmail.forceLiteralLatin)

    val uri = ImeEditorBehaviorPolicy.typingPolicy(0x00000011)
    checkCase("uri kind", uri.kind == EditorSurfaceKind.URI)
    checkCase("uri is literal latin", uri.forceLiteralLatin)
    checkCase("uri no suggestions", !uri.showSuggestions)
    checkCase("uri has https quick key", "https://" in uri.quickKeys)
    checkCase("uri has www quick key", "www." in uri.quickKeys)

    val filter = ImeEditorBehaviorPolicy.typingPolicy(0x000000b1)
    checkCase("filter kind", filter.kind == EditorSurfaceKind.FILTER)
    checkCase("filter no suggestions", !filter.showSuggestions)
    checkCase("filter is not force-latin", !filter.forceLiteralLatin)

    val password = ImeEditorBehaviorPolicy.typingPolicy(0x00000081)
    checkCase("password kind", password.kind == EditorSurfaceKind.PASSWORD)
    checkCase("password no learning", !password.allowLearning)
    checkCase("password literal latin", password.forceLiteralLatin)

    val visiblePassword = ImeEditorBehaviorPolicy.typingPolicy(0x00000091)
    checkCase("visible password protected", visiblePassword.kind == EditorSurfaceKind.PASSWORD && !visiblePassword.showSuggestions)

    val webPassword = ImeEditorBehaviorPolicy.typingPolicy(0x000000e1)
    checkCase("web password protected", webPassword.kind == EditorSurfaceKind.PASSWORD && !webPassword.allowLearning)

    val nonText = ImeEditorBehaviorPolicy.typingPolicy(0x00000002)
    checkCase("non-text kind", nonText.kind == EditorSurfaceKind.NON_TEXT)
    checkCase("non-text no suggestion intelligence", !nonText.showSuggestions && !nonText.allowLearning && !nonText.allowAutocorrect)

    checkCase("search action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x3) == "Search")
    checkCase("send action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x4) == "Send")
    checkCase("done action label", ImeEditorBehaviorPolicy.enterKeyLabel(0x6) == "Done")

    println("TYPING STAGE 12 EDITOR POLICY SELF-TEST: PASS ($passed/46)")
}
