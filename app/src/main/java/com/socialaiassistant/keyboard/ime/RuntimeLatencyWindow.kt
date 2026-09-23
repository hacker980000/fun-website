package com.socialaiassistant.keyboard.ime

/** Fixed metric names only; no typed text or editor content is recorded. */
enum class ImeRuntimeMetric {
    INPUT_VIEW_CREATE,
    KEYBOARD_RENDER,
    SUGGESTION_COMPUTE,
    GLIDE_RESOLVE,
    INPUT_CONNECTION
}

data class RuntimeLatencySnapshot(
    val metric: ImeRuntimeMetric,
    val sampleCount: Int,
    val p50Micros: Long,
    val p95Micros: Long,
    val maxMicros: Long,
    val overBudgetCount: Int
)

/**
 * Small in-memory rolling latency window for debug/device validation.
 *
 * The window stores durations only. It never stores typed text, package names,
 * editor hints, clipboard contents or AI/context data.
 */
class RuntimeLatencyWindow(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val reportEvery: Int = DEFAULT_REPORT_EVERY
) {
    private data class Bucket(
        val samplesMicros: LongArray,
        val overBudgetFlags: BooleanArray,
        var size: Int = 0,
        var cursor: Int = 0,
        var totalSamples: Long = 0,
        var overBudgetInWindow: Int = 0
    )

    private val buckets = ImeRuntimeMetric.values().associateWith {
        val boundedCapacity = capacity.coerceAtLeast(1)
        Bucket(LongArray(boundedCapacity), BooleanArray(boundedCapacity))
    }

    @Synchronized
    fun record(metric: ImeRuntimeMetric, elapsedNanos: Long, budgetNanos: Long): RuntimeLatencySnapshot? {
        val bucket = buckets.getValue(metric)
        val micros = (elapsedNanos.coerceAtLeast(0L) / 1_000L)
        val overBudget = elapsedNanos > budgetNanos
        if (bucket.size == bucket.samplesMicros.size && bucket.overBudgetFlags[bucket.cursor]) {
            bucket.overBudgetInWindow -= 1
        }
        bucket.samplesMicros[bucket.cursor] = micros
        bucket.overBudgetFlags[bucket.cursor] = overBudget
        if (overBudget) bucket.overBudgetInWindow += 1
        bucket.cursor = (bucket.cursor + 1) % bucket.samplesMicros.size
        if (bucket.size < bucket.samplesMicros.size) bucket.size += 1
        bucket.totalSamples += 1
        if (reportEvery <= 0 || bucket.totalSamples % reportEvery.toLong() != 0L) return null
        return snapshotLocked(metric, bucket)
    }

    @Synchronized
    fun snapshot(metric: ImeRuntimeMetric): RuntimeLatencySnapshot? {
        val bucket = buckets.getValue(metric)
        if (bucket.size == 0) return null
        return snapshotLocked(metric, bucket)
    }

    @Synchronized
    fun clear() {
        buckets.values.forEach { bucket ->
            bucket.samplesMicros.fill(0L)
            bucket.size = 0
            bucket.cursor = 0
            bucket.totalSamples = 0
            bucket.overBudgetInWindow = 0
            bucket.overBudgetFlags.fill(false)
        }
    }

    private fun snapshotLocked(metric: ImeRuntimeMetric, bucket: Bucket): RuntimeLatencySnapshot {
        val sorted = bucket.samplesMicros.copyOf(bucket.size).sortedArray()
        return RuntimeLatencySnapshot(
            metric = metric,
            sampleCount = bucket.size,
            p50Micros = percentile(sorted, 50),
            p95Micros = percentile(sorted, 95),
            maxMicros = sorted.lastOrNull() ?: 0L,
            overBudgetCount = bucket.overBudgetInWindow
        )
    }

    private fun percentile(sorted: LongArray, percent: Int): Long {
        if (sorted.isEmpty()) return 0L
        val index = (((sorted.size - 1) * percent.coerceIn(0, 100)) + 99) / 100
        return sorted[index.coerceIn(0, sorted.lastIndex)]
    }

    companion object {
        const val DEFAULT_CAPACITY = 64
        const val DEFAULT_REPORT_EVERY = 32
    }
}
