package com.example.pccomponent.performance

import com.example.pccomponent.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

/**
 * Lightweight debug-only timing utility.
 *
 * Release builds execute the measured block directly and do not collect data.
 */
object PerformanceTrace {
    private const val REPORT_EVERY = 25
    private const val NANOS_PER_MILLI = 1_000_000.0

    private data class Stats(
        val calls: LongAdder = LongAdder(),
        val totalNanos: LongAdder = LongAdder(),
        val maxNanos: AtomicLong = AtomicLong()
    )

    private val stats = ConcurrentHashMap<String, Stats>()

    inline fun <T> measure(name: String, block: () -> T): T {
        if (!BuildConfig.DEBUG) return block()
        val started = System.nanoTime()
        return try {
            block()
        } finally {
            record(name, System.nanoTime() - started)
        }
    }

    fun snapshot(): Map<String, PerformanceSample> = stats.mapValues { (_, value) ->
        val calls = value.calls.sum()
        PerformanceSample(
            calls = calls,
            totalMillis = (value.totalNanos.sum() / NANOS_PER_MILLI).toLong(),
            averageMillis = if (calls == 0L) 0.0
            else value.totalNanos.sum().toDouble() / calls / NANOS_PER_MILLI,
            maxMillis = (value.maxNanos.get() / NANOS_PER_MILLI).toLong()
        )
    }

    fun reset() {
        stats.clear()
    }

    @PublishedApi
    internal fun record(name: String, elapsedNanos: Long) {
        val sample = stats.getOrPut(name) { Stats() }
        sample.calls.increment()
        sample.totalNanos.add(elapsedNanos)
        sample.maxNanos.updateAndGet { maxOf(it, elapsedNanos) }
        val calls = sample.calls.sum()
        if (calls % REPORT_EVERY == 0L) {
            val averageMillis = sample.totalNanos.sum().toDouble() / calls / NANOS_PER_MILLI
            println(
                "PerformanceTrace[$name]: calls=$calls, " +
                    "avgMs=${"%.2f".format(averageMillis)}, " +
                    "maxMs=${sample.maxNanos.get() / NANOS_PER_MILLI}"
            )
        }
    }
}

data class PerformanceSample(
    val calls: Long,
    val totalMillis: Long,
    val averageMillis: Double,
    val maxMillis: Long
)
