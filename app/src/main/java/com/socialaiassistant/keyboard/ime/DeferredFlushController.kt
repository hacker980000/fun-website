package com.socialaiassistant.keyboard.ime

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Coalesces frequent learning-store flush requests off the IME hot path.
 * Direct flush() on the backing store remains synchronous for deterministic tests,
 * explicit clear/export operations and final lifecycle shutdown.
 */
internal class DeferredFlushController(
    threadName: String,
    private val defaultDelayMs: Long = DEFAULT_DELAY_MS,
    private val persistNow: () -> Unit
) {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, threadName).apply { isDaemon = true }
    }
    private val lock = Any()
    private var pending: ScheduledFuture<*>? = null
    private var generation: Long = 0L
    @Volatile private var closed = false

    fun request(delayMs: Long = defaultDelayMs) {
        if (closed) return
        synchronized(lock) {
            if (closed) return
            generation += 1L
            val token = generation
            pending?.cancel(false)
            pending = executor.schedule({
                try {
                    persistNow()
                } finally {
                    synchronized(lock) {
                        if (generation == token) pending = null
                    }
                }
            }, delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
        }
    }

    fun cancelPending() {
        synchronized(lock) {
            generation += 1L
            pending?.cancel(false)
            pending = null
        }
    }

    fun closeAndFlush() {
        synchronized(lock) {
            if (closed) return
            closed = true
            generation += 1L
            pending?.cancel(false)
            pending = null
        }
        persistNow()
        executor.shutdown()
    }

    companion object {
        const val DEFAULT_DELAY_MS = 650L
    }
}
