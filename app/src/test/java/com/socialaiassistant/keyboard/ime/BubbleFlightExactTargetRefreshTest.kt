package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleFlightExactTargetRefreshTest {
    private val editor = BubbleFlightEditorToken("org.example.chat", 7, 4L)

    @Test
    fun newerPostCommitCursorWinsBeforeDispatch() {
        val before = target(100f, 1_000L)
        val after = target(120f, 1_010L)
        assertEquals(after, BubbleFlightExactTargetRefresh.choose(editor, before, after))
    }


    @Test
    fun sameTimestampStillUsesLatestCursorState() {
        val before = target(100f, 1_000L)
        val latest = target(120f, 1_000L)
        assertEquals(latest, BubbleFlightExactTargetRefresh.choose(editor, before, latest))
    }

    @Test
    fun mismatchedLatestCannotReplacePreparedTarget() {
        val before = target(100f, 1_000L)
        val other = target(120f, 1_010L).copy(editor = editor.copy(generation = 3L))
        assertEquals(before, BubbleFlightExactTargetRefresh.choose(editor, before, other))
    }

    private fun target(x: Float, time: Long) = BubbleFlightTarget(
        BubbleFlightPoint(x, 200f),
        editor,
        time,
        BubbleFlightTargetSource.CURSOR_ANCHOR
    )
}
