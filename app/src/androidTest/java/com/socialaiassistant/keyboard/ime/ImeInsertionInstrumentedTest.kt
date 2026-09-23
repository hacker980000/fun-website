package com.socialaiassistant.keyboard.ime

import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Selection
import android.view.inputmethod.BaseInputConnection
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImeInsertionInstrumentedTest {
    @Test
    fun explicit_tap_path_inserts_into_empty_composer() {
        val connection = editableConnection("")
        val result = ReplyInserter().insert(connection, "Hello!", replaceDraft = false)
        assertEquals(InsertResult.Inserted, result)
        assertEquals("Hello!", connection.text.toString())
    }

    @Test
    fun non_empty_draft_is_preserved_by_default() {
        val connection = editableConnection("my draft")
        val result = ReplyInserter().insert(connection, "AI reply", replaceDraft = false)
        assertEquals(InsertResult.DraftPresent, result)
        assertEquals("my draft", connection.text.toString())
    }

    @Test
    fun phonetic_engine_commits_bangla_word_before_space() {
        val connection = editableConnection("")
        val engine = ImeTypingEngine()
        val mode = KeyboardUiMode(
            language = KeyboardLanguage.BANGLA,
            banglaMode = BanglaInputMode.PHONETIC
        )

        "ami".forEach { char ->
            val mutation = engine.onText(mode, char.toString())
            connection.setComposingText(mutation.composingText.orEmpty(), 1)
        }
        connection.setComposingText(engine.flush(), 1)
        connection.finishComposingText()
        connection.commitText(" ", 1)

        assertEquals("আমি ", connection.text.toString())
    }


    @Test
    fun failed_commit_is_reported_instead_of_false_success() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val connection = FailingCommitConnection(context)
        val result = ReplyInserter().insert(connection, "Hello!", replaceDraft = false)
        assertEquals(InsertResult.CommitFailed, result)
    }

    @Test
    fun failed_select_all_does_not_append_over_existing_draft() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val connection = FailingSelectAllConnection(context, "my draft")
        val result = ReplyInserter().insert(connection, "AI reply", replaceDraft = true)
        assertEquals(InsertResult.CommitFailed, result)
        assertEquals("my draft", connection.text.toString())
    }

    private fun editableConnection(initial: String): RecordingInputConnection {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return RecordingInputConnection(context, initial)
    }

    private open class RecordingInputConnection(context: Context, initial: String) :
        BaseInputConnection(EditText(context), true) {
        val text = SpannableStringBuilder(initial)

        init {
            Selection.setSelection(text, text.length)
        }

        override fun getEditable(): Editable = text
    }

    private class FailingCommitConnection(context: Context) : RecordingInputConnection(context, "") {
        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = false
    }

    private class FailingSelectAllConnection(context: Context, initial: String) :
        RecordingInputConnection(context, initial) {
        override fun performContextMenuAction(id: Int): Boolean = false
    }
}
