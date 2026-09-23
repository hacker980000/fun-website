package com.socialaiassistant.keyboard.ime

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeferredFlushControllerTest {
    @Test
    fun frequent_requests_coalesce_to_one_persist() {
        val count = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val controller = DeferredFlushController("flush-test", 35L) {
            count.incrementAndGet()
            latch.countDown()
        }
        controller.request()
        controller.request()
        controller.request()
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(1, count.get())
        controller.closeAndFlush()
    }

    @Test
    fun close_cancels_pending_and_performs_final_flush() {
        val count = AtomicInteger(0)
        val controller = DeferredFlushController("flush-close-test", 5_000L) { count.incrementAndGet() }
        controller.request()
        controller.closeAndFlush()
        assertEquals(1, count.get())
    }
}
