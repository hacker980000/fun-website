package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.SenderClass

private fun promptFor(text: String): String {
    return ExtensionPromptBuilder().build(
        PromptRequest(
            type = InteractionType.COMMENT,
            mode = AiMode.GENERAL,
            messages = listOf(PromptMessage(SenderClass.RECIPIENT, text)),
            latestRecipientMessage = text,
            postText = text
        )
    ).system
}

fun main() {
    val banglish = promptFor("tumi kemon acho")
    check(banglish.contains("COMMENT LANGUAGE: BANGLISH")) { banglish }

    val bengali = promptFor("তুমি কেমন আছো")
    check(bengali.contains("COMMENT LANGUAGE: BENGALI")) { bengali }

    val english = promptFor("How are you today?")
    check(english.contains("COMMENT LANGUAGE: ENGLISH")) { english }

    println("COMMENT LANGUAGE PROMPT SELFTEST: PASS")
}
