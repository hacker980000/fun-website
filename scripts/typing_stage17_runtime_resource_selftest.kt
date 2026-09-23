package com.socialaiassistant.keyboard.ime

import java.util.concurrent.atomic.AtomicInteger

fun main() {
    var checks = 0

    val builds = AtomicInteger(0)
    val resource = ResettableLazyResource {
        builds.incrementAndGet()
        "engine-${builds.get()}"
    }
    check(!resource.isInitialized()); checks++
    val first = resource.get()
    check(first == "engine-1"); checks++
    check(resource.isInitialized()); checks++
    check(resource.get() === first); checks++
    check(builds.get() == 1); checks++
    check(resource.release()); checks++
    check(!resource.isInitialized()); checks++
    check(!resource.release()); checks++
    check(resource.get() == "engine-2"); checks++
    check(builds.get() == 2); checks++

    val latency = RuntimeLatencyWindow(capacity = 4, reportEvery = 4)
    check(latency.snapshot(ImeRuntimeMetric.SUGGESTION_COMPUTE) == null); checks++
    check(latency.record(ImeRuntimeMetric.SUGGESTION_COMPUTE, 1_000_000L, 4_000_000L) == null); checks++
    check(latency.record(ImeRuntimeMetric.SUGGESTION_COMPUTE, 2_000_000L, 4_000_000L) == null); checks++
    check(latency.record(ImeRuntimeMetric.SUGGESTION_COMPUTE, 3_000_000L, 4_000_000L) == null); checks++
    val report = requireNotNull(latency.record(ImeRuntimeMetric.SUGGESTION_COMPUTE, 8_000_000L, 4_000_000L))
    checks++
    check(report.sampleCount == 4); checks++
    check(report.p50Micros == 3_000L); checks++
    check(report.p95Micros == 8_000L); checks++
    check(report.maxMicros == 8_000L); checks++
    check(report.overBudgetCount == 1); checks++

    latency.record(ImeRuntimeMetric.SUGGESTION_COMPUTE, 500_000L, 4_000_000L)
    val rolling = latency.snapshot(ImeRuntimeMetric.SUGGESTION_COMPUTE)!!
    check(rolling.sampleCount == 4); checks++
    check(rolling.maxMicros == 8_000L); checks++
    latency.clear()
    check(latency.snapshot(ImeRuntimeMetric.SUGGESTION_COMPUTE) == null); checks++

    println("TYPING STAGE 17 RUNTIME RESOURCE SELF-TEST: PASS ($checks/23)")
}
