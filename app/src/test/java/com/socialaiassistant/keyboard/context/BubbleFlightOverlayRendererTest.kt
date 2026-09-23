package com.socialaiassistant.keyboard.context

import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.ime.BubbleFlightEditorToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BubbleFlightOverlayRendererTest {
    @Test fun cancelUnknownEditorDoesNotCreateViews() {
        val root = FrameLayout(ApplicationProvider.getApplicationContext())
        val host = FakeHost(root)
        val renderer = BubbleFlightOverlayRenderer(host)
        renderer.cancelEditor(BubbleFlightEditorToken("x", 1, 1L))
        assertEquals(0, root.childCount)
        assertFalse(host.attached)
    }

    private class FakeHost(override val root: FrameLayout) : BubbleOverlayHost {
        var attached = false
        override fun ensureAttached(): Boolean { attached = true; return true }
        override fun detach() { attached = false }
    }
}
