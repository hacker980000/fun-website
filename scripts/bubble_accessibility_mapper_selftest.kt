import android.graphics.Rect
import com.socialaiassistant.keyboard.context.BubbleAccessibilityTargetMapper
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint

private var passed = 0
private fun checkCase(name: String, ok: Boolean) { check(ok) { "FAIL: $name" }; passed++; println("PASS: $name") }
fun main() {
    val bounds = Rect(100, 200, 400, 280)
    checkCase("ltr trailing edge", BubbleAccessibilityTargetMapper.pointInside(bounds, false, 16f) == BubbleFlightPoint(384f, 240f))
    checkCase("rtl trailing edge", BubbleAccessibilityTargetMapper.pointInside(bounds, true, 16f) == BubbleFlightPoint(116f, 240f))
    checkCase("zero bounds rejected", BubbleAccessibilityTargetMapper.pointInside(Rect(0,0,0,0), false, 16f) == null)
    val narrow = BubbleAccessibilityTargetMapper.pointInside(Rect(100,200,120,280), false, 16f)!!
    checkCase("narrow stays inside", narrow.x in 100f..120f)
    println("Stage 24.1 accessibility mapper selftest: $passed/4 PASS")
}
