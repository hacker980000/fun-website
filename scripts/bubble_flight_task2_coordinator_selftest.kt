import com.socialaiassistant.keyboard.ime.*
import com.socialaiassistant.keyboard.theme.KeyboardTheme

private fun theme() = KeyboardTheme(
    id = "test", displayName = "Test", isBuiltIn = true,
    rootBackground = 0, panelSurface = 0, toolbarSurface = 0, keySurface = 0,
    specialKeySurface = 0, disabledSurface = 0, textPrimary = 0, textSecondary = 0,
    keyLabel = 0, specialKeyLabel = 0, disabledLabel = 0, primaryNeon = 0,
    secondaryNeon = 0, aiNeon = 0, actionAccent = 0, dangerAccent = 0
)

fun main() {
    val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)
    val prepared = PreparedBubbleFlight(BubbleFlightRequest(
        1L, "a", BubbleFlightPoint(100f, 900f), editor, null,
        BubbleKeyAnimationSpec("a", 470L, 68f, 11f, 32f, 82f, 0.52f), theme(), 1_000L
    ))
    val events = mutableListOf<String>()
    val coordinator = BubbleFlightTapCoordinator { events += "dispatch" }
    coordinator.commitThenDispatch(prepared) { events += "commit" }
    check(events == listOf("commit", "dispatch")) { "commit must happen before dispatch: $events" }
    events.clear()
    coordinator.commitThenDispatch(null) { events += "commit" }
    check(events == listOf("commit")) { "null prepared flight must still commit: $events" }
    println("Stage 24.1 Task 2 coordinator selftest: 2/2 PASS")
}
