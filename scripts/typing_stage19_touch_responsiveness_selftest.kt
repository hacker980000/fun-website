import com.socialaiassistant.keyboard.ime.FeedbackCadenceGate
import com.socialaiassistant.keyboard.ime.FrameWorkCoalescer
import com.socialaiassistant.keyboard.ime.GlideHitBox
import com.socialaiassistant.keyboard.ime.GlideHitMap
import com.socialaiassistant.keyboard.ime.RapidActionGate
import com.socialaiassistant.keyboard.ime.TouchResponsivenessPolicy

private var checks = 0

private fun checkThat(value: Boolean, message: String) {
    checks++
    check(value) { message }
}

fun main() {
    val actions = RapidActionGate(defaultMinIntervalMs = 100L, maxTrackedActions = 3)
    checkThat(actions.allow("toolbar:language", 1_000L), "first action should pass")
    checkThat(!actions.allow("toolbar:language", 1_050L), "rapid duplicate action should be suppressed")
    checkThat(actions.allow("toolbar:language", 1_100L), "action should pass at debounce boundary")
    checkThat(actions.allow("toolbar:emoji", 1_101L), "different action should not be blocked")
    checkThat(actions.allow("toolbar:voice", 1_102L), "third action should pass")
    checkThat(actions.allow("toolbar:settings", 1_103L), "bounded gate should accept and trim")
    checkThat(actions.trackedCount() <= 3, "action tracking must remain bounded")
    checkThat(actions.allow("toolbar:settings", 900L), "clock rollback should recover instead of locking input")
    actions.clear()
    checkThat(actions.trackedCount() == 0, "clear should drop action history")

    val feedback = FeedbackCadenceGate(hapticMinIntervalMs = 16L, soundMinIntervalMs = 20L)
    checkThat(feedback.allowHaptic(100L), "first haptic should pass")
    checkThat(!feedback.allowHaptic(115L), "haptic under cadence should suppress")
    checkThat(feedback.allowHaptic(116L), "haptic at cadence boundary should pass")
    checkThat(feedback.allowSound(200L), "first sound should pass")
    checkThat(!feedback.allowSound(219L), "sound under cadence should suppress")
    checkThat(feedback.allowSound(220L), "sound at cadence boundary should pass")
    feedback.clear()
    checkThat(feedback.allowHaptic(0L), "cleared feedback gate should pass")

    val frame = FrameWorkCoalescer()
    checkThat(frame.request(), "first frame request should schedule")
    checkThat(!frame.request(), "duplicate frame request should coalesce")
    checkThat(frame.isPending(), "frame should report pending")
    checkThat(frame.consume(), "consume should report pending work")
    checkThat(!frame.isPending(), "consume should clear pending state")
    checkThat(frame.request(), "request after consume should schedule again")
    frame.cancel()
    checkThat(!frame.isPending(), "cancel should clear pending state")

    checkThat(TouchResponsivenessPolicy.glideStartThresholdPx(18, 8) == 18, "base Glide threshold should win")
    checkThat(TouchResponsivenessPolicy.glideStartThresholdPx(10, 12) == 18, "device touch slop should raise threshold")
    checkThat(TouchResponsivenessPolicy.MIN_ACTION_TOUCH_TARGET_DP >= 48, "action target should be at least 48dp")

    val hitMap = GlideHitMap(
        listOf(
            GlideHitBox('a', 0f, 0f, 40f, 40f),
            GlideHitBox('b', 50f, 0f, 90f, 40f)
        )
    )
    checkThat(hitMap.keyAt(20f, 20f) == 'a', "exact hit should resolve")
    checkThat(hitMap.keyAt(45f, 20f) == null, "visual gap should be dead without tolerance")
    checkThat(hitMap.keyAt(45f, 20f, 6f) == 'a', "small visual gap should resolve to nearest key")
    checkThat(hitMap.keyAt(47f, 20f, 6f) == 'b', "nearest key should win tolerance lookup")
    checkThat(hitMap.keyAt(45f, 20f, 4f) == null, "point outside tolerance should stay unresolved")
    checkThat(hitMap.keyAt(45f, 20f, 5f) == 'a', "equal-distance tie should be deterministic")
    checkThat(hitMap.size() == 2, "hit-map size should remain stable")

    println("Stage19 touch/responsiveness self-test: $checks/$checks PASS")
}
