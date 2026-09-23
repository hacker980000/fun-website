package com.socialaiassistant.keyboard.ime

fun main() {
    var checks = 0
    val now = 20_000_000_000L
    val target = DeferredInsertionTarget(
        packageName = "com.example.chat",
        inputType = 0x00000001,
        fieldId = 77,
        hintText = "Message",
        createdAtNanos = now - 1_000_000_000L
    )
    fun decision(pkg: String?, input: Int?, field: Int?, hint: String?, blocked: Boolean = false, t: Long = now) =
        DeferredInsertionPolicy.decision(target, pkg, input, field, hint, blocked, t)
    check(decision("com.example.chat", 0x1, 77, "Message") == DeferredInsertionDecision.ACCEPT); checks++
    check(decision("com.socialaiassistant.keyboard", 0x1, 12, null) == DeferredInsertionDecision.WAIT); checks++
    check(decision(null, null, null, null) == DeferredInsertionDecision.WAIT); checks++
    check(decision("com.example.chat", 0x1, 78, "Message") == DeferredInsertionDecision.REJECT); checks++
    check(decision("com.example.chat", 0x1, 77, "Message", blocked = true) == DeferredInsertionDecision.REJECT); checks++
    check(decision("com.example.chat", 0x1, 77, "Message", t = now + DeferredInsertionPolicy.MAX_AGE_NANOS + 1) == DeferredInsertionDecision.REJECT); checks++

    val noId = DeferredInsertionTarget("com.example.chat", 0x00000021, 0, "Email", now - 1_000L)
    check(DeferredInsertionPolicy.decision(noId, "com.example.chat", 0x00000021, 0, "Email", false, now) == DeferredInsertionDecision.ACCEPT); checks++
    check(DeferredInsertionPolicy.decision(noId, "com.example.chat", 0x00000021, 0, "Other", false, now) == DeferredInsertionDecision.REJECT); checks++

    val english = linkedMapOf("hello" to 50, "help" to 40, "hero" to 30, "yellow" to 10, "below" to 10, "world" to 10)
    val bangla = linkedMapOf("ami" to "আমি", "amar" to "আমার", "alo" to "আলো", "tumi" to "তুমি", "valo" to "ভালো")
    val engine = GlideTypingEngine(english, bangla, emptyMap())
    check(engine.bestEnglish("helo")?.text == "hello"); checks++
    check(engine.bestEnglish("hlp")?.text == "help"); checks++
    check(engine.bestEnglish("yelow")?.text == "yellow"); checks++
    check(engine.resolveEnglish("helo").none { it.text == "yellow" }); checks++
    check(engine.bestBangla("ami")?.text == "আমি"); checks++
    check(engine.resolveBangla("ami").none { it.sourceRoman == "tumi" }); checks++

    println("TYPING STAGE 13 RUNTIME HARDENING SELF-TEST: PASS ($checks/14)")
}
