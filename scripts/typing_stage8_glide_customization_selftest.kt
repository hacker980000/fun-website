import com.socialaiassistant.keyboard.ime.GlideTypingEngine
import com.socialaiassistant.keyboard.settings.OneHandedMode
import com.socialaiassistant.keyboard.settings.ToolbarProfile

fun main() {
    var checks = 0
    val engine = GlideTypingEngine()

    check(engine.bestEnglish("helo")?.text == "hello")
    checks++
    check(engine.bestEnglish("mesage")?.text == "message")
    checks++
    check(engine.resolveEnglish("thnks", 3).any { it.text == "thanks" })
    checks++

    check(engine.bestBangla("ami")?.text == "আমি")
    checks++
    check(engine.bestBangla("valo")?.text == "ভালো")
    checks++
    check(engine.resolveBangla("kmn", 3).any { it.text == "কেমন" })
    checks++

    check(engine.bestEnglish("to") == null)
    checks++
    check(OneHandedMode.fromSetting("left") == OneHandedMode.LEFT)
    checks++
    check(OneHandedMode.fromSetting("unknown") == OneHandedMode.OFF)
    checks++
    check(ToolbarProfile.fromSetting("ai_first") == ToolbarProfile.AI_FIRST)
    checks++
    check(ToolbarProfile.fromSetting(null) == ToolbarProfile.BALANCED)
    checks++

    println("typing_stage8_glide_customization_selftest: $checks checks PASS")
}
