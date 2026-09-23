import com.socialaiassistant.keyboard.ime.InsertResult
import com.socialaiassistant.keyboard.ime.ReplyInserter
import android.view.inputmethod.InputConnection

private class FakeConnection(
    initialBefore: String = "",
    initialAfter: String = "",
    private val commitOk: Boolean = true,
    private val selectOk: Boolean = true,
    private val throwRead: Boolean = false
) : InputConnection {
    var before = initialBefore
    var after = initialAfter
    var committed: String? = null
    var selectedAll = false

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        if (throwRead) error("dead editor")
        return before.takeLast(n)
    }
    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        if (throwRead) error("dead editor")
        return after.take(n)
    }
    override fun performContextMenuAction(id: Int): Boolean {
        selectedAll = selectOk
        if (selectOk) { before = ""; after = "" }
        return selectOk
    }
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (!commitOk) return false
        committed = text?.toString()
        before += committed.orEmpty()
        return true
    }
}

fun main() {
    val inserter = ReplyInserter()
    var checks = 0

    check(inserter.insert(null, "hello") == InsertResult.NoConnection); checks++
    check(inserter.insert(FakeConnection(), "   ") == InsertResult.EmptyReply); checks++

    val empty = FakeConnection()
    check(inserter.insert(empty, " hello ") == InsertResult.Inserted); checks++
    check(empty.committed == "hello"); checks++

    val draft = FakeConnection(initialBefore = "manual draft")
    check(inserter.insert(draft, "AI reply", replaceDraft = false) == InsertResult.DraftPresent); checks++
    check(draft.committed == null); checks++

    val selectFails = FakeConnection(initialBefore = "draft", selectOk = false)
    check(inserter.insert(selectFails, "AI", replaceDraft = true) == InsertResult.CommitFailed); checks++
    check(selectFails.committed == null); checks++

    val commitFails = FakeConnection(commitOk = false)
    check(inserter.insert(commitFails, "AI") == InsertResult.CommitFailed); checks++

    val deadEditor = FakeConnection(throwRead = true)
    check(inserter.insert(deadEditor, "AI") == InsertResult.CommitFailed); checks++

    val append = FakeConnection(initialBefore = "draft")
    check(inserter.appendAfterDraft(append, "AI") == InsertResult.Inserted); checks++
    check(append.selectedAll && append.committed == "draft\nAI"); checks++

    val anyway = FakeConnection(initialBefore = "draft")
    check(inserter.insertAnyway(anyway, "AI") == InsertResult.Inserted); checks++
    check(anyway.committed == " AI"); checks++

    println("TYPING STAGE 14 INSERTION ACK SELF-TEST: PASS ($checks/14)")
}
