package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.safety.FieldSafety

fun main() {
    var calls = 0
    val controller = ExplicitAiTriggerController { calls++ }

    check(!controller.onToolbarTap(FieldSafety.ALLOW_AI, loading = false)) {
        "AI toolbar tap must open the panel only; it must not request generation"
    }
    check(calls == 0) { "AI toolbar tap unexpectedly triggered generation: $calls call(s)" }
    check(!controller.onToolbarTap(FieldSafety.ALLOW_AI, loading = true))
    check(!controller.onToolbarTap(FieldSafety.BLOCK_AI, loading = false))
    check(!controller.onToolbarTap(FieldSafety.NO_CONVERSATION, loading = false))

    println("EXPLICIT AI TOOLBAR SELFTEST: PASS")
}
