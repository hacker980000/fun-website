package com.socialaiassistant.keyboard.ime

import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.socialaiassistant.keyboard.R
import com.socialaiassistant.keyboard.ai.ReplyState
import com.socialaiassistant.keyboard.theme.KeyboardTheme
import com.socialaiassistant.keyboard.theme.ThemeRenderer

sealed interface SmartReplyUiState {
    data object Hidden : SmartReplyUiState
    data class Loading(val message: String = "Thinking…") : SmartReplyUiState
    data class Ready(val reply: String) : SmartReplyUiState
    data class Status(val message: String) : SmartReplyUiState
}

class SmartReplyController(
    private val root: View,
    private val onReplyTap: (String) -> InsertResult,
    private val onInsertAnyway: (String) -> Unit,
    private val onReplace: (String) -> Unit
) {
    private val container: View = root.findViewById(R.id.smart_reply_container)
    private val progress: ProgressBar = root.findViewById(R.id.smart_reply_progress)
    private val text: TextView = root.findViewById(R.id.smart_reply_text)
    private val insertAnyway: Button = root.findViewById(R.id.smart_reply_insert_anyway)
    private val replace: Button = root.findViewById(R.id.smart_reply_replace)
    private var currentReply: String? = null

    init {
        text.setOnClickListener {
            val reply = currentReply ?: return@setOnClickListener
            if (onReplyTap(reply) == InsertResult.DraftPresent) showDraftChoices(reply)
        }
        insertAnyway.setOnClickListener { currentReply?.let(onInsertAnyway) }
        replace.setOnClickListener { currentReply?.let(onReplace) }
    }

    fun applyTheme(theme: KeyboardTheme, renderer: ThemeRenderer) {
        renderer.styleSmartReply(root, theme)
    }

    fun render(state: ReplyState) {
        render(
            when (state) {
                ReplyState.Hidden -> SmartReplyUiState.Hidden
                ReplyState.WaitingForContext -> SmartReplyUiState.Hidden
                ReplyState.Loading -> SmartReplyUiState.Loading("Preparing Best Reply…")
                is ReplyState.Ready -> SmartReplyUiState.Ready(state.reply)
                ReplyState.Offline -> SmartReplyUiState.Status("AI Offline — normal typing still works")
                ReplyState.NeedsApiKey -> SmartReplyUiState.Status("Add your OpenRouter API key in Social AI Keyboard settings")
                is ReplyState.Error -> SmartReplyUiState.Status(state.message)
            }
        )
    }

    fun render(state: SmartReplyUiState) {
        insertAnyway.visibility = View.GONE
        replace.visibility = View.GONE
        when (state) {
            SmartReplyUiState.Hidden -> {
                currentReply = null
                container.visibility = View.GONE
            }
            is SmartReplyUiState.Loading -> {
                currentReply = null
                container.visibility = View.VISIBLE
                progress.visibility = View.VISIBLE
                text.text = state.message
                text.isClickable = false
            }
            is SmartReplyUiState.Ready -> {
                currentReply = state.reply
                container.visibility = View.VISIBLE
                progress.visibility = View.GONE
                text.text = state.reply
                text.isClickable = true
            }
            is SmartReplyUiState.Status -> {
                currentReply = null
                container.visibility = View.VISIBLE
                progress.visibility = View.GONE
                text.text = state.message
                text.isClickable = false
            }
        }
    }

    private fun showDraftChoices(reply: String) {
        currentReply = reply
        progress.visibility = View.GONE
        text.text = "Draft detected — keep it or replace it explicitly."
        insertAnyway.visibility = View.VISIBLE
        replace.visibility = View.VISIBLE
    }
}
