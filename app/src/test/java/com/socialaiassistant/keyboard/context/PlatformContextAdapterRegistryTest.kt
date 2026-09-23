package com.socialaiassistant.keyboard.context

object PlatformContextAdapterRegistryTest {
    @JvmStatic
    fun main(args: Array<String>) {
        classifiesSupportedPackages()
        distinguishesFacebookCommentsFromMessengerLikeSurfaces()
        filtersPlatformNoiseWithoutDroppingMessages()
        suppressesSupportedAppSearchAndCaptionSurfaces()
        rejectsUnknownAppsFailClosed()
        unknownAppsCannotProduceGenericSnapshot()
        recognizesSupportedPackagesExplicitly()
        println("PlatformContextAdapterRegistryTest PASS")
    }

    private fun classifiesSupportedPackages() {
        val registry = PlatformContextAdapterRegistry.default()
        check(registry.adapt(input("com.facebook.orca", composer = "Message" )).surface == ConversationSurface.INBOX)
        check(registry.adapt(input("com.whatsapp", composer = "Type a message")).surface == ConversationSurface.INBOX)
        check(registry.adapt(input("com.instagram.android", composer = "Add a comment…")).surface == ConversationSurface.COMMENT)
        check(registry.adapt(input("org.telegram.messenger", composer = "Message")).surface == ConversationSurface.INBOX)
    }

    private fun distinguishesFacebookCommentsFromMessengerLikeSurfaces() {
        val registry = PlatformContextAdapterRegistry.default()
        val comment = registry.adapt(input("com.facebook.katana", composer = "Write a comment"))
        val inbox = registry.adapt(input("com.facebook.katana", window = "Conversation with Rafi", composer = "Message"))
        check(comment.surface == ConversationSurface.COMMENT)
        check(inbox.surface == ConversationSurface.INBOX)
    }

    private fun filtersPlatformNoiseWithoutDroppingMessages() {
        val registry = PlatformContextAdapterRegistry.default()
        val result = registry.adapt(
            input(
                packageName = "com.facebook.orca",
                composer = "Message",
                nodes = listOf(
                    VisibleTextNode(0, "Active now"),
                    VisibleTextNode(1, "আজকে দেখা হবে?", senderHint = SenderClass.RECIPIENT),
                    VisibleTextNode(2, "Send"),
                    VisibleTextNode(3, "হ্যাঁ, বিকেলে।", senderHint = SenderClass.SELF),
                    VisibleTextNode(4, "Delivered")
                )
            )
        )
        check(result.nodes.mapNotNull { it.text } == listOf("আজকে দেখা হবে?", "হ্যাঁ, বিকেলে।"))
    }


    private fun suppressesSupportedAppSearchAndCaptionSurfaces() {
        val registry = PlatformContextAdapterRegistry.default()
        val whatsappSearch = registry.adapt(
            input(
                packageName = "com.whatsapp",
                composer = "Search chats",
                nodes = listOf(VisibleTextNode(0, "Recent chat row", senderHint = SenderClass.RECIPIENT))
            )
        )
        val instagramCaption = registry.adapt(
            input(
                packageName = "com.instagram.android",
                composer = "Write a caption",
                nodes = listOf(VisibleTextNode(0, "Photo preview"))
            )
        )
        check(whatsappSearch.surface == ConversationSurface.GENERAL)
        check(whatsappSearch.nodes.isEmpty())
        check(instagramCaption.surface == ConversationSurface.GENERAL)
        check(instagramCaption.nodes.isEmpty())
    }

    private fun rejectsUnknownAppsFailClosed() {
        val registry = PlatformContextAdapterRegistry.default()
        val result = registry.adapt(input("com.example.chat", composer = "Reply"))
        check(result.surface == ConversationSurface.GENERAL)
        check(result.conversationHint == null)
        check(result.nodes.isEmpty())
        check(result.confidenceBoost == 0f)
    }

    private fun unknownAppsCannotProduceGenericSnapshot() {
        val registry = PlatformContextAdapterRegistry.default()
        val platform = registry.adapt(input("com.example.chat", composer = "Reply"))
        val snapshot = GenericConversationAdapter().fromNodes(
            packageName = "com.example.chat",
            windowSignature = "chat window",
            conversationHint = platform.conversationHint,
            nodes = platform.nodes,
            composerHint = "Reply",
            surface = platform.surface,
            confidenceBoost = platform.confidenceBoost
        )
        check(snapshot == null)
    }

    private fun recognizesSupportedPackagesExplicitly() {
        val registry = PlatformContextAdapterRegistry.default()
        check(registry.supportsPackage("com.facebook.katana"))
        check(registry.supportsPackage("com.facebook.orca"))
        check(registry.supportsPackage("com.whatsapp"))
        check(registry.supportsPackage("com.whatsapp.w4b"))
        check(registry.supportsPackage("com.instagram.android"))
        check(registry.supportsPackage("org.telegram.messenger"))
        check(!registry.supportsPackage("com.example.chat"))
        check(!registry.supportsPackage(""))
    }

    private fun input(
        packageName: String,
        window: String = "chat window",
        conversation: String? = "Rafi",
        composer: String? = null,
        nodes: List<VisibleTextNode> = listOf(VisibleTextNode(0, "hello", senderHint = SenderClass.RECIPIENT))
    ) = PlatformContextInput(
        packageName = packageName,
        windowSignature = window,
        conversationHint = conversation,
        composerHint = composer,
        nodes = nodes
    )
}
