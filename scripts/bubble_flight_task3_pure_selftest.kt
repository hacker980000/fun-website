import com.socialaiassistant.keyboard.context.BubbleFlightPath
import com.socialaiassistant.keyboard.ime.BubbleFlightPoint

private var passed = 0
private fun checkCase(name: String, ok: Boolean) { check(ok) { "FAIL: $name" }; passed++; println("PASS: $name") }
fun main() {
    val source = BubbleFlightPoint(100f, 900f)
    val target = BubbleFlightPoint(300f, 200f)
    checkCase("t0 source", BubbleFlightPath.pointAt(source, target, 80f, 0f) == source)
    checkCase("t1 target", BubbleFlightPath.pointAt(source, target, 80f, 1f) == target)
    val mid = BubbleFlightPath.pointAt(source, target, 80f, 0.5f)
    checkCase("midpoint arcs upward", mid.y < (source.y + target.y) / 2f)
    checkCase("short path finite", BubbleFlightPath.pointAt(source, BubbleFlightPoint(101f, 899f), 118f, 0.5f).isFinite())
    println("Stage 24.1 Task 3 pure selftest: $passed/4 PASS")
}
