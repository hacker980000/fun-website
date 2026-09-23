import com.socialaiassistant.keyboard.ime.BubbleFlightEditorToken
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint
import com.socialaiassistant.keyboard.ime.BubbleFlightTarget
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetResolver
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetSource
import com.socialaiassistant.keyboard.ime.BubbleKeyPolicy
import com.socialaiassistant.keyboard.ime.BubbleKeyRequest
import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity

private var passed = 0
private fun checkCase(name: String, condition: Boolean) {
    if (!condition) error("FAIL: $name")
    passed++
    println("PASS: $name")
}

fun main() {
    val editor = BubbleFlightEditorToken("org.example.chat", 7, 3L)
    val resolver = BubbleFlightTargetResolver(exactMaxAgeMs = 600L, fallbackMaxAgeMs = 900L)
    val exact = BubbleFlightTarget(BubbleFlightPoint(300f, 200f), editor, 1000L, BubbleFlightTargetSource.CURSOR_ANCHOR)
    val fallback = BubbleFlightTarget(BubbleFlightPoint(250f, 210f), editor, 1050L, BubbleFlightTargetSource.ACCESSIBILITY_BOUNDS)
    checkCase("exact target priority", resolver.resolve(editor, exact, fallback, 1200L) == exact)
    checkCase("stale exact falls back", resolver.resolve(editor, exact.copy(capturedAtUptimeMs = 1L), fallback, 1200L) == fallback)
    checkCase("generation mismatch rejected", resolver.resolve(editor, exact.copy(editor = editor.copy(generation = 2L)), null, 1200L) == null)
    checkCase("no target invents nothing", resolver.resolve(editor, null, null, 1200L) == null)
    checkCase("max bubbles remains eight", BubbleKeyPolicy.MAX_SIMULTANEOUS_BUBBLES == 8)

    val base = BubbleKeyRequest(true, "a", false, false, true, true, BubbleKeyIntensity.NORMAL)
    val soft = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.SOFT))!!
    val normal = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.NORMAL))!!
    val playful = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.PLAYFUL))!!
    checkCase("curve grows with intensity", soft.flightCurveDp < normal.flightCurveDp && normal.flightCurveDp < playful.flightCurveDp)
    checkCase("end scale shrinks with intensity", soft.flightEndScale > normal.flightEndScale && normal.flightEndScale > playful.flightEndScale)
    checkCase("duration grows with intensity", soft.durationMs < normal.durationMs && normal.durationMs < playful.durationMs)

    println("Stage 24.1 Bubble Flight policy: $passed/8 PASS")
}
