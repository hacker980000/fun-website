import com.socialaiassistant.keyboard.ime.BubbleFlightEditorToken
import com.socialaiassistant.keyboard.ime.BubbleFlightExactTargetRefresh
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint
import com.socialaiassistant.keyboard.ime.BubbleFlightTarget
import com.socialaiassistant.keyboard.ime.BubbleFlightTargetSource

fun main() {
    val editor = BubbleFlightEditorToken("org.example.chat", 7, 4L)
    val beforeCommit = BubbleFlightTarget(BubbleFlightPoint(100f, 200f), editor, 1_000L, BubbleFlightTargetSource.CURSOR_ANCHOR)
    val afterCommit = BubbleFlightTarget(BubbleFlightPoint(120f, 200f), editor, 1_010L, BubbleFlightTargetSource.CURSOR_ANCHOR)
    check(BubbleFlightExactTargetRefresh.choose(editor, beforeCommit, afterCommit) == afterCommit) {
        "newer post-commit cursor target must win before dispatch"
    }
    check(BubbleFlightExactTargetRefresh.choose(editor, afterCommit, beforeCommit) == afterCommit) {
        "older cursor target must not replace a newer prepared target"
    }
    val sameTickAfterCommit = afterCommit.copy(capturedAtUptimeMs = beforeCommit.capturedAtUptimeMs)
    check(BubbleFlightExactTargetRefresh.choose(editor, beforeCommit, sameTickAfterCommit) == sameTickAfterCommit) {
        "latest cursor state must win even when uptime millisecond is equal"
    }
    check(BubbleFlightExactTargetRefresh.choose(editor, beforeCommit, afterCommit.copy(editor = editor.copy(generation = 3L))) == beforeCommit) {
        "mismatched editor target must be rejected"
    }
    println("Stage 24.1 dispatch refresh selftest: 4/4 PASS")
}
