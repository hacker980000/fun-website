package com.socialaiassistant.keyboard.ime

import android.graphics.Matrix
import android.view.inputmethod.CursorAnchorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BubbleCursorAnchorMapperTest {
    @Test fun mapsInsertionMarkerThroughCursorMatrix() {
        val matrix = Matrix().apply {
            setScale(2f, 2f)
            postTranslate(40f, 60f)
        }
        val info = CursorAnchorInfo.Builder()
            .setInsertionMarkerLocation(10f, 20f, 24f, 28f, 0)
            .setMatrix(matrix)
            .build()
        assertEquals(BubbleFlightPoint(60f, 108f), BubbleCursorAnchorMapper.map(info))
    }

    @Test fun nanInsertionMarkerReturnsNull() {
        val info = CursorAnchorInfo.Builder()
            .setInsertionMarkerLocation(Float.NaN, 20f, 24f, 28f, 0)
            .build()
        assertNull(BubbleCursorAnchorMapper.map(info))
    }
}
