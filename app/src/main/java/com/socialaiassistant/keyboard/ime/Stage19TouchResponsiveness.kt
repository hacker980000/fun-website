package com.socialaiassistant.keyboard.ime

import kotlin.math.max

/**
 * Small Android-free policies for Stage 19 touch responsiveness.
 *
 * These helpers intentionally never debounce text keys. They are for non-text
 * actions (toolbar, suggestion, AI actions), feedback cadence, and UI-frame
 * coalescing only.
 */
class RapidActionGate(
    private val defaultMinIntervalMs: Long = DEFAULT_ACTION_INTERVAL_MS,
    private val maxTrackedActions: Int = DEFAULT_MAX_TRACKED_ACTIONS
) {
    private val lastAcceptedMs = LinkedHashMap<String, Long>()

    fun allow(actionKey: String, nowMs: Long, minIntervalMs: Long = defaultMinIntervalMs): Boolean {
        val key = actionKey.take(MAX_ACTION_KEY_CHARS)
        val previous = lastAcceptedMs[key]
        val interval = minIntervalMs.coerceAtLeast(0L)
        if (previous != null && nowMs >= previous && nowMs - previous < interval) return false
        lastAcceptedMs[key] = nowMs
        trimIfNeeded()
        return true
    }

    fun clear() = lastAcceptedMs.clear()
    fun trackedCount(): Int = lastAcceptedMs.size

    private fun trimIfNeeded() {
        val maxSize = maxTrackedActions.coerceAtLeast(1)
        while (lastAcceptedMs.size > maxSize) {
            val first = lastAcceptedMs.entries.firstOrNull()?.key ?: return
            lastAcceptedMs.remove(first)
        }
    }

    companion object {
        const val DEFAULT_ACTION_INTERVAL_MS = 110L
        const val DEFAULT_MAX_TRACKED_ACTIONS = 32
        const val MAX_ACTION_KEY_CHARS = 96
    }
}

class FeedbackCadenceGate(
    private val hapticMinIntervalMs: Long = DEFAULT_HAPTIC_INTERVAL_MS,
    private val soundMinIntervalMs: Long = DEFAULT_SOUND_INTERVAL_MS
) {
    private var lastHapticMs = Long.MIN_VALUE
    private var lastSoundMs = Long.MIN_VALUE

    fun allowHaptic(nowMs: Long): Boolean {
        if (!allows(lastHapticMs, nowMs, hapticMinIntervalMs)) return false
        lastHapticMs = nowMs
        return true
    }

    fun allowSound(nowMs: Long): Boolean {
        if (!allows(lastSoundMs, nowMs, soundMinIntervalMs)) return false
        lastSoundMs = nowMs
        return true
    }

    fun clear() {
        lastHapticMs = Long.MIN_VALUE
        lastSoundMs = Long.MIN_VALUE
    }

    private fun allows(previousMs: Long, nowMs: Long, minimumMs: Long): Boolean {
        if (previousMs == Long.MIN_VALUE) return true
        if (nowMs < previousMs) return true // monotonic clock reset/test seam
        return nowMs - previousMs >= minimumMs.coerceAtLeast(0L)
    }

    companion object {
        const val DEFAULT_HAPTIC_INTERVAL_MS = 16L
        const val DEFAULT_SOUND_INTERVAL_MS = 20L
    }
}

/** Coalesces repeated UI requests until the next posted frame consumes them. */
class FrameWorkCoalescer {
    private var pending = false

    fun request(): Boolean {
        if (pending) return false
        pending = true
        return true
    }

    fun consume(): Boolean {
        val hadPending = pending
        pending = false
        return hadPending
    }

    fun cancel() {
        pending = false
    }

    fun isPending(): Boolean = pending
}

object TouchResponsivenessPolicy {
    const val MIN_ACTION_TOUCH_TARGET_DP = 48
    const val GLIDE_HIT_TOLERANCE_DP = 6

    /** Respect both our deliberate Glide threshold and the device touch slop. */
    fun glideStartThresholdPx(baseThresholdPx: Int, systemTouchSlopPx: Int): Int =
        max(baseThresholdPx.coerceAtLeast(1), systemTouchSlopPx.coerceAtLeast(0) + systemTouchSlopPx.coerceAtLeast(0) / 2)
}
