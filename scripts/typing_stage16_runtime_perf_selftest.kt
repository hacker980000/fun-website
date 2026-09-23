package com.socialaiassistant.keyboard.ime

import java.util.concurrent.atomic.AtomicInteger

fun main() {
    var checks = 0

    val map = GlideHitMap(
        listOf(
            GlideHitBox('a', 0f, 0f, 50f, 50f),
            GlideHitBox('s', 51f, 0f, 100f, 50f),
            GlideHitBox('z', 0f, 51f, 50f, 100f)
        )
    )
    check(map.size() == 3); checks++
    check(map.keyAt(25f, 25f) == 'a'); checks++
    check(map.keyAt(80f, 25f) == 's'); checks++
    check(map.keyAt(25f, 80f) == 'z'); checks++
    check(map.keyAt(150f, 150f) == null); checks++
    check(GlideHitMap.EMPTY.keyAt(1f, 1f) == null); checks++

    val persisted = AtomicInteger(0)
    val controller = DeferredFlushController(
        threadName = "stage16-selftest",
        defaultDelayMs = 45L,
        persistNow = { persisted.incrementAndGet() }
    )
    controller.request()
    Thread.sleep(12L)
    controller.request()
    Thread.sleep(12L)
    controller.request()
    Thread.sleep(110L)
    check(persisted.get() == 1); checks++

    controller.request(80L)
    controller.cancelPending()
    Thread.sleep(110L)
    check(persisted.get() == 1); checks++

    controller.request(5_000L)
    controller.closeAndFlush()
    check(persisted.get() == 2); checks++
    Thread.sleep(80L)
    check(persisted.get() == 2); checks++

    check(DeferredFlushController.DEFAULT_DELAY_MS in 500L..1_000L); checks++

    println("TYPING STAGE 16 RUNTIME/PERF SELF-TEST: PASS ($checks/11)")
}
