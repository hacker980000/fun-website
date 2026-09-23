import com.socialaiassistant.keyboard.ime.ClipboardTextPolicy

fun main() {
    check(ClipboardTextPolicy.shouldExposeContent(fieldSensitive = false, clipMarkedSensitive = false))
    check(!ClipboardTextPolicy.shouldExposeContent(fieldSensitive = true, clipMarkedSensitive = false))
    check(!ClipboardTextPolicy.shouldExposeContent(fieldSensitive = false, clipMarkedSensitive = true))
    check(!ClipboardTextPolicy.shouldExposeContent(fieldSensitive = true, clipMarkedSensitive = true))

    val sanitized = ClipboardTextPolicy.sanitize(listOf(" one ", "", "one", "two"))
    check(sanitized == listOf("one", "two"))
    check(ClipboardTextPolicy.label("a".repeat(80)).endsWith("…"))

    println("Stage 25.2 clipboard privacy self-test PASS")
}
