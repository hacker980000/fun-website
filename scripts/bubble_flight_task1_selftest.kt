import com.socialaiassistant.keyboard.ime.*
import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity
import com.socialaiassistant.keyboard.theme.KeyboardTheme

private var passed = 0
private fun checkCase(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    passed++
    println("PASS: $name")
}

private class Sink(private val accept: Boolean) : BubbleFlightSink {
    val requests = mutableListOf<BubbleFlightRequest>()
    val retargets = mutableListOf<Pair<Long, BubbleFlightTarget>>()
    override fun submit(request: BubbleFlightRequest): Boolean { requests += request; return accept }
    override fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean { retargets += flightId to target; return accept }
    override fun cancelEditor(editor: BubbleFlightEditorToken) = Unit
    override fun cancelAll() = Unit
}

fun main() {
    val resolver = BubbleFlightTargetResolver(600L, 900L)
    val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)
    val exact = BubbleFlightTarget(BubbleFlightPoint(300f, 200f), editor, 1000L, BubbleFlightTargetSource.CURSOR_ANCHOR)
    val fallback = BubbleFlightTarget(BubbleFlightPoint(250f, 210f), editor, 1050L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS)
    checkCase("exact wins", resolver.resolve(editor, exact, fallback, 1200L) == exact)
    checkCase("fallback after stale exact", resolver.resolve(editor, exact.copy(capturedAtUptimeMs = 100L), fallback.copy(capturedAtUptimeMs = 900L), 1100L) == fallback.copy(capturedAtUptimeMs = 900L))
    checkCase("generation mismatch rejected", resolver.resolve(editor, exact.copy(editor = editor.copy(generation = 2L)), null, 1100L) == null)
    checkCase("non-finite rejected", resolver.resolve(editor, exact.copy(point = BubbleFlightPoint(Float.NaN, 1f)), null, 1100L) == null)

    BubbleFlightBus.clearForTest()
    checkCase("no sink is unhandled", !BubbleFlightBus.dispatch(dummyRequest(editor, exact)))
    val sink = Sink(true)
    BubbleFlightBus.register(sink)
    checkCase("sink handles", BubbleFlightBus.dispatch(dummyRequest(editor, exact)))
    checkCase("sink receives", sink.requests.size == 1)
    checkCase("retarget forwards", BubbleFlightBus.retarget(1L, exact) && sink.retargets.single() == (1L to exact))
    BubbleFlightBus.unregister(sink)
    checkCase("unregister works", !BubbleFlightBus.dispatch(dummyRequest(editor, exact).copy(id = 2L)))

    val base = BubbleKeyRequest(true, "a", false, false, true, true, BubbleKeyIntensity.NORMAL)
    val soft = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.SOFT))!!
    val normal = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.NORMAL))!!
    val playful = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.PLAYFUL))!!
    checkCase("curve increases", soft.flightCurveDp < normal.flightCurveDp && normal.flightCurveDp < playful.flightCurveDp)
    checkCase("end scale decreases", soft.flightEndScale > normal.flightEndScale && normal.flightEndScale > playful.flightEndScale)
    checkCase("pool cap eight", BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES == 8)
    println("Stage 24.1 Task 1 selftest: $passed/12 PASS")
}

private fun dummyRequest(editor: BubbleFlightEditorToken, exact: BubbleFlightTarget): BubbleFlightRequest = BubbleFlightRequest(
    id = 1L,
    label = "a",
    source = BubbleFlightPoint(100f, 900f),
    editor = editor,
    exactTarget = exact,
    spec = BubbleKeyAnimationSpec("a", 470L, 68f, 11f, 32f, 82f, 0.52f),
    theme = KeyboardTheme(
        id = "test", displayName = "Test", isBuiltIn = true,
        rootBackground = 0, panelSurface = 0, toolbarSurface = 0, keySurface = 0,
        specialKeySurface = 0, disabledSurface = 0, textPrimary = 0, textSecondary = 0,
        keyLabel = 0, specialKeyLabel = 0, disabledLabel = 0, primaryNeon = 0,
        secondaryNeon = 0, aiNeon = 0, actionAccent = 0, dangerAccent = 0
    ),
    createdAtUptimeMs = 1_000L
)
