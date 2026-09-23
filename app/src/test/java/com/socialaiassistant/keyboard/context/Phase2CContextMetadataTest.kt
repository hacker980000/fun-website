package com.socialaiassistant.keyboard.context

object Phase2CContextMetadataTest {
    @JvmStatic
    fun main(args: Array<String>) {
        snapshotPreservesSurfaceAndConfidenceBoost()
        metadataBackfillsConversationHint()
        metadataIdentityWinsOverTransientEventDescription()
        windowSignatureIsNotConversationIdentity()
        transientEventDescriptionIsNotConversationIdentity()
        conversationKeysSeparateThreads()
        conversationKeysSeparateInboxFromComment()
        println("Phase2CContextMetadataTest PASS")
    }

    private fun snapshotPreservesSurfaceAndConfidenceBoost() {
        val adapter = GenericConversationAdapter(maxMessages = 10)
        val snapshot = adapter.fromNodes(
            packageName = "com.whatsapp",
            windowSignature = "1:window",
            conversationHint = "Rafi",
            nodes = listOf(
                VisibleTextNode(0, "hello", senderHint = SenderClass.RECIPIENT),
                VisibleTextNode(1, "hi", senderHint = SenderClass.SELF)
            ),
            composerHint = "Message",
            surface = ConversationSurface.INBOX,
            confidenceBoost = 0.25f,
            capturedAtMillis = 1L
        ) ?: error("snapshot expected")

        check(snapshot.surface == ConversationSurface.INBOX)
        check(snapshot.confidence >= 0.99f)
    }

    private fun metadataBackfillsConversationHint() {
        val nodes = listOf(
            VisibleTextNode(
                order = 0,
                text = "Rafi Ahmed",
                contentDescription = "Rafi Ahmed",
                viewIdResourceName = "com.whatsapp:id/conversation_contact_name",
                className = "android.widget.TextView"
            ),
            VisibleTextNode(1, "How are you?", senderHint = SenderClass.RECIPIENT)
        )
        val hint = ConversationHintResolver.resolve(
            explicitHint = "Conversation",
            windowSignature = "42:android.widget.FrameLayout",
            nodes = nodes
        )
        check(hint == "Rafi Ahmed")
    }

    private fun metadataIdentityWinsOverTransientEventDescription() {
        val nodes = listOf(
            VisibleTextNode(
                order = 0,
                text = "Family Group",
                contentDescription = "Family Group",
                viewIdResourceName = "com.whatsapp:id/conversation_title"
            )
        )
        val hint = ConversationHintResolver.resolve(
            explicitHint = "Back",
            windowSignature = "42:android.widget.FrameLayout",
            nodes = nodes
        )
        check(hint == "Family Group")
    }

    private fun windowSignatureIsNotConversationIdentity() {
        val hint = ConversationHintResolver.resolve(
            explicitHint = "Conversation",
            windowSignature = "42:android.widget.FrameLayout",
            nodes = emptyList()
        )
        check(hint == null)
    }

    private fun transientEventDescriptionIsNotConversationIdentity() {
        val hint = ConversationHintResolver.resolve(
            explicitHint = "Alice",
            windowSignature = "42:android.widget.FrameLayout",
            nodes = emptyList()
        )
        check(hint == null)
    }

    private fun conversationKeysSeparateThreads() {
        val factory = ConversationKeyFactory()
        val a = snapshot("Rafi")
        val b = snapshot("Sadia")
        check(factory.create(a) != factory.create(b))
        check(factory.create(a) == factory.create(a.copy(windowSignature = "99:new-window")))
    }


    private fun conversationKeysSeparateInboxFromComment() {
        val factory = ConversationKeyFactory()
        val inbox = snapshot("Rafi")
        val comment = inbox.copy(surface = ConversationSurface.COMMENT)
        check(factory.create(inbox) != factory.create(comment))
    }

    private fun snapshot(name: String) = ContextSnapshot(
        packageName = "com.whatsapp",
        windowSignature = "1:window",
        conversationHint = name,
        messages = listOf(ContextMessage(SenderClass.RECIPIENT, "hello")),
        latestRecipientMessage = "hello",
        composerHint = "Message",
        surface = ConversationSurface.INBOX,
        confidence = 1f,
        capturedAtMillis = 1L
    )
}
