package com.socialaiassistant.keyboard.ime

import android.R
import android.view.inputmethod.InputConnection

enum class InsertResult {
    Inserted,
    DraftPresent,
    EmptyReply,
    NoConnection,
    CommitFailed
}

sealed interface KeyboardAction {
    data class Text(val value: String) : KeyboardAction
    data object Backspace : KeyboardAction
    data object Shift : KeyboardAction
    data object Enter : KeyboardAction
    data object Space : KeyboardAction
    data object ToggleLanguage : KeyboardAction
    data object ToggleBanglaMode : KeyboardAction
    data object ShowNumbers : KeyboardAction
    data object ShowSymbols : KeyboardAction
    data object ShowLetters : KeyboardAction
    data object OpenEmoji : KeyboardAction
    data object OpenClipboard : KeyboardAction
    data object OpenVoice : KeyboardAction
    data object OpenSettings : KeyboardAction
    data object OpenAiPanel : KeyboardAction
}

class ReplyInserter {
    fun insert(
        inputConnection: InputConnection?,
        reply: String,
        replaceDraft: Boolean = false
    ): InsertResult {
        val connection = inputConnection ?: return InsertResult.NoConnection
        val cleanReply = reply.trim()
        if (cleanReply.isEmpty()) return InsertResult.EmptyReply

        val before = safeTextBeforeCursor(connection, MAX_DRAFT_SCAN) ?: return InsertResult.CommitFailed
        val after = safeTextAfterCursor(connection, MAX_DRAFT_SCAN) ?: return InsertResult.CommitFailed
        val hasDraft = (before + after).isNotBlank()

        if (hasDraft && !replaceDraft) return InsertResult.DraftPresent

        if (hasDraft && replaceDraft && !safeSelectAll(connection)) {
            return InsertResult.CommitFailed
        }
        return if (safeCommitText(connection, cleanReply)) InsertResult.Inserted else InsertResult.CommitFailed
    }

    fun appendAfterDraft(inputConnection: InputConnection?, reply: String): InsertResult {
        val connection = inputConnection ?: return InsertResult.NoConnection
        val cleanReply = reply.trim()
        if (cleanReply.isEmpty()) return InsertResult.EmptyReply
        val before = safeTextBeforeCursor(connection, MAX_DRAFT_SCAN) ?: return InsertResult.CommitFailed
        val after = safeTextAfterCursor(connection, MAX_DRAFT_SCAN) ?: return InsertResult.CommitFailed
        val manual = (before + after).trim()
        val desired = if (manual.isBlank()) cleanReply else "$manual\n$cleanReply"
        if (manual.isNotBlank() && !safeSelectAll(connection)) return InsertResult.CommitFailed
        return if (safeCommitText(connection, desired)) InsertResult.Inserted else InsertResult.CommitFailed
    }

    fun insertAnyway(inputConnection: InputConnection?, reply: String): InsertResult {
        val connection = inputConnection ?: return InsertResult.NoConnection
        val cleanReply = reply.trim()
        if (cleanReply.isEmpty()) return InsertResult.EmptyReply
        val before = safeTextBeforeCursor(connection, 1) ?: return InsertResult.CommitFailed
        val prefix = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        return if (safeCommitText(connection, prefix + cleanReply)) InsertResult.Inserted else InsertResult.CommitFailed
    }

    private fun safeTextBeforeCursor(connection: InputConnection, maxChars: Int): String? =
        runCatching { connection.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty() }.getOrNull()

    private fun safeTextAfterCursor(connection: InputConnection, maxChars: Int): String? =
        runCatching { connection.getTextAfterCursor(maxChars, 0)?.toString().orEmpty() }.getOrNull()

    private fun safeSelectAll(connection: InputConnection): Boolean =
        runCatching { connection.performContextMenuAction(R.id.selectAll) }.getOrDefault(false)

    private fun safeCommitText(connection: InputConnection, text: CharSequence): Boolean =
        runCatching { connection.commitText(text, 1) }.getOrDefault(false)

    private companion object {
        const val MAX_DRAFT_SCAN = 8_000
    }
}
