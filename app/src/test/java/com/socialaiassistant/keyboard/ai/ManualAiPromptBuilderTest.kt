package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.context.SenderClass
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualAiPromptBuilderTest {
    private val builder = ManualAiPromptBuilder(ExtensionPromptBuilder())

    @Test
    fun smart_uses_general_social_reply_prompt() {
        val bundle = builder.build(socialRequest(ManualAiAction.SMART, InteractionType.INBOX))
        assertTrue(bundle.system.contains("MODE: GENERAL"))
    }

    @Test
    fun witty_uses_witty_social_reply_prompt() {
        val bundle = builder.build(socialRequest(ManualAiAction.WITTY, InteractionType.INBOX))
        assertTrue(bundle.system.contains("MODE: WITTY"))
    }

    @Test
    fun flirty_maps_to_message_or_comment_mode() {
        val inbox = builder.build(socialRequest(ManualAiAction.FLIRTY, InteractionType.INBOX))
        val comment = builder.build(socialRequest(ManualAiAction.FLIRTY, InteractionType.COMMENT))
        assertTrue(inbox.system.contains("MODE: FLIRT_MSG"))
        assertTrue(comment.user.contains("FLIRT_CMT"))
    }


    @Test
    fun flirty_inbox_prompt_contains_compact_video_style_without_copying_examples() {
        val bundle = builder.build(
            ManualAiRequest(
                action = ManualAiAction.FLIRTY,
                interactionType = InteractionType.INBOX,
                conversationIntent = ConversationAiIntent.REPLY,
                messages = listOf(PromptMessage(SenderClass.RECIPIENT, "tumi ajke eto chup keno")),
                latestRecipientMessage = "tumi ajke eto chup keno"
            )
        )
        val system = bundle.system.lowercase()
        assertTrue(system.contains("context") && system.contains("playful"))
        assertTrue(system.contains("soft flirt") || system.contains("wordplay"))
        assertTrue(system.contains("continuation"))
        assertTrue(system.contains("do not copy") || system.contains("never copy"))
        assertTrue(system.contains("opener") && system.contains("punchline"))
    }

    @Test
    fun flirty_inbox_prompt_deescalates_on_boundary_signals() {
        val bundle = builder.build(
            ManualAiRequest(
                action = ManualAiAction.FLIRTY,
                interactionType = InteractionType.INBOX,
                conversationIntent = ConversationAiIntent.REPLY,
                messages = listOf(PromptMessage(SenderClass.RECIPIENT, "please normal kotha bolo, flirt korona")),
                latestRecipientMessage = "please normal kotha bolo, flirt korona"
            )
        )
        val system = bundle.system.lowercase()
        assertTrue(system.contains("stop") || system.contains("disinterest"))
        assertTrue(system.contains("friend-only") || system.contains("normal chat"))
        assertTrue(system.contains("busy") || system.contains("reply-later"))
        assertTrue(system.contains("de-escalate") || system.contains("no further romantic"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rewrite_requires_non_empty_draft() {
        builder.build(ManualAiRequest(action = ManualAiAction.REWRITE, draftText = ""))
    }

    @Test
    fun translate_names_target_language_and_preserves_meaning() {
        val bundle = builder.build(
            ManualAiRequest(
                action = ManualAiAction.TRANSLATE,
                draftText = "আমি ভালো আছি",
                translateTargetLanguage = "English"
            )
        )
        assertTrue(bundle.system.contains("Translate the draft into English"))
        assertTrue(bundle.system.contains("Preserve the original meaning"))
    }

    @Test
    fun grammar_fix_preserves_language_tone_and_meaning() {
        val bundle = builder.build(
            ManualAiRequest(
                action = ManualAiAction.GRAMMAR_FIX,
                draftText = "I has a plan"
            )
        )
        assertTrue(bundle.system.contains("preserve its meaning, tone, and language", ignoreCase = true))
    }

    private fun socialRequest(action: ManualAiAction, type: InteractionType) = ManualAiRequest(
        action = action,
        interactionType = type,
        messages = listOf(PromptMessage(SenderClass.RECIPIENT, "How are you?")),
        latestRecipientMessage = "How are you?"
    )
}
