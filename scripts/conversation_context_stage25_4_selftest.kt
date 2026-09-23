import com.socialaiassistant.keyboard.context.*

private var checks = 0
private fun expect(name: String, condition: Boolean) {
    check(condition) { "FAIL: $name" }
    checks++
}

private fun node(
    order: Int,
    text: String,
    sender: SenderClass = SenderClass.UNKNOWN,
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null,
    id: String? = null
) = VisibleTextNode(
    order = order,
    text = text,
    senderHint = sender,
    viewIdResourceName = id,
    screenLeft = left,
    screenTop = top,
    screenRight = right,
    screenBottom = bottom
)

private fun snapshot(hint: String?, capturedAt: Long = 1L) = ContextSnapshot(
    packageName = "com.whatsapp",
    windowSignature = "42:android.widget.FrameLayout",
    conversationHint = hint,
    messages = listOf(ContextMessage(SenderClass.RECIPIENT, "hello")),
    latestRecipientMessage = "hello",
    composerHint = "Message",
    surface = ConversationSurface.INBOX,
    confidence = 1f,
    capturedAtMillis = capturedAt
)

fun main() {
    expect("semantic outgoing wins", ConversationSenderClassifier.infer(
        node(0, "mine", left = 10, top = 10, right = 250, bottom = 60, id = "id/outgoing_message_text"), 1000, false
    ) == SenderClass.SELF)
    expect("semantic incoming wins", ConversationSenderClassifier.infer(
        node(0, "theirs", left = 700, top = 10, right = 980, bottom = 60, id = "id/incoming_message_text"), 1000, false
    ) == SenderClass.RECIPIENT)
    expect("ltr left is recipient", ConversationSenderClassifier.infer(
        node(0, "left", left = 20, top = 10, right = 300, bottom = 60), 1000, false
    ) == SenderClass.RECIPIENT)
    expect("ltr right is self", ConversationSenderClassifier.infer(
        node(0, "right", left = 700, top = 10, right = 980, bottom = 60), 1000, false
    ) == SenderClass.SELF)
    expect("center is unknown", ConversationSenderClassifier.infer(
        node(0, "system", left = 350, top = 10, right = 650, bottom = 60), 1000, false
    ) == SenderClass.UNKNOWN)
    expect("full width is unknown", ConversationSenderClassifier.infer(
        node(0, "system", left = 50, top = 10, right = 950, bottom = 60), 1000, false
    ) == SenderClass.UNKNOWN)
    expect("rtl mirrors sides", ConversationSenderClassifier.infer(
        node(0, "self rtl", left = 20, top = 10, right = 300, bottom = 60), 1000, true
    ) == SenderClass.SELF)

    val ordered = ConversationNodeOrdering.chronological(listOf(
        node(0, "new", SenderClass.RECIPIENT, 20, 500, 300, 550),
        node(1, "old", SenderClass.RECIPIENT, 20, 100, 300, 150),
        node(2, "mid", SenderClass.SELF, 700, 300, 980, 350)
    ))
    expect("geometry restores visual chronology", ordered.map { it.text } == listOf("old", "mid", "new"))

    val dup = node(0, "duplicate", SenderClass.RECIPIENT, 20, 100, 300, 150)
    val deduped = ConversationNodeOrdering.chronological(listOf(dup, dup.copy(order = 1, screenLeft = 21, screenTop = 101)))
    expect("mirrored rows dedupe", deduped.size == 1)

    val adapter = GenericConversationAdapter(maxMessages = 10, maxTotalChars = 12, maxMessageChars = 50)
    val bounded = adapter.fromNodes(
        "com.whatsapp", "window", "Rafi",
        listOf(
            node(0, "old-old-old", SenderClass.RECIPIENT),
            node(1, "recent-one", SenderClass.SELF),
            node(2, "latest", SenderClass.RECIPIENT)
        ),
        "Message",
        ConversationSurface.INBOX,
        capturedAtMillis = 1L
    ) ?: error("snapshot expected")
    expect("budget keeps latest row", bounded.messages.last().text == "latest")
    expect("budget drops oldest before latest", bounded.messages.none { it.text == "old-old-old" })

    val identityNodes = listOf(
        VisibleTextNode(0, "Family Group", viewIdResourceName = "com.whatsapp:id/conversation_title")
    )
    expect("toolbar identity wins over event description", ConversationHintResolver.resolve(
        explicitHint = "Back", windowSignature = "42:android.widget.FrameLayout", nodes = identityNodes
    ) == "Family Group")
    expect("window signature is not identity", ConversationHintResolver.resolve(
        explicitHint = "Conversation", windowSignature = "42:android.widget.FrameLayout", nodes = emptyList()
    ) == null)
    expect("event description alone is not persisted identity", ConversationHintResolver.resolve(
        explicitHint = "Alice", windowSignature = "42:android.widget.FrameLayout", nodes = emptyList()
    ) == null)
    expect("per-message username is not a thread identity", ConversationHintResolver.resolve(
        explicitHint = null,
        windowSignature = "42:android.widget.FrameLayout",
        nodes = listOf(VisibleTextNode(0, "Alice", viewIdResourceName = "com.example:id/username"))
    ) == null)

    val factory = ConversationKeyFactory()
    expect("stable hint stays same across captures", factory.create(snapshot("Rafi", 1L)) == factory.create(snapshot("Rafi", 2L)))
    expect("missing hint is not stable", !factory.hasStableIdentity(snapshot(null, 1L)))
    expect("missing hint gets capture scoped key", factory.create(snapshot(null, 1L)) != factory.create(snapshot(null, 2L)))

    println("Stage 25.4 conversation-context self-test PASS ($checks/$checks)")
}
