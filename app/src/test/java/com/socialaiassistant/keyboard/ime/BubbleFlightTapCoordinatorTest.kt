package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.theme.KeyboardTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleFlightTapCoordinatorTest {
    @Test fun commitRunsBeforeFlightDispatch() {
        val events = mutableListOf<String>()
        val coordinator = BubbleFlightTapCoordinator { events += "dispatch" }
        coordinator.commitThenDispatch(preparedFlight()) { events += "commit" }
        assertEquals(listOf("commit", "dispatch"), events)
    }

    @Test fun nullPreparedFlightStillCommits() {
        val events = mutableListOf<String>()
        val coordinator = BubbleFlightTapCoordinator { events += "dispatch" }
        coordinator.commitThenDispatch(null) { events += "commit" }
        assertEquals(listOf("commit"), events)
    }

    private fun preparedFlight(): PreparedBubbleFlight {
        val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)
        return PreparedBubbleFlight(
            BubbleFlightRequest(
                1L, "a", BubbleFlightPoint(100f, 900f), editor, null,
                BubbleKeyAnimationSpec("a", 470L, 68f, 11f, 32f, 82f, 0.52f),
                sampleTheme(), 1_000L
            )
        )
    }

    private fun sampleTheme() = KeyboardTheme(
        id = "test", displayName = "Test", isBuiltIn = true,
        rootBackground = 0, panelSurface = 0, toolbarSurface = 0, keySurface = 0,
        specialKeySurface = 0, disabledSurface = 0, textPrimary = 0, textSecondary = 0,
        keyLabel = 0, specialKeyLabel = 0, disabledLabel = 0, primaryNeon = 0,
        secondaryNeon = 0, aiNeon = 0, actionAccent = 0, dangerAccent = 0
    )
}
