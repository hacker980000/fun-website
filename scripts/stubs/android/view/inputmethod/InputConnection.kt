package android.view.inputmethod
interface InputConnection {
    fun getTextBeforeCursor(n: Int, flags: Int): CharSequence?
    fun getTextAfterCursor(n: Int, flags: Int): CharSequence?
    fun performContextMenuAction(id: Int): Boolean
    fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean
}
