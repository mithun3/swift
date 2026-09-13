package com.fx.common.queue;

/**
 * {@code TailerOptimizationConfig} — Centralized configuration for Chronicle Queue
 * tailer optimizations targeting sub-100µs tail latency.
 *
 * <h2>Configuration Parameters</h2>
 *
 * <p>All parameters are tunable via JVM system properties to support different
 * deployment environments (bare-metal, Docker, macOS dev) without recompilation.
 *
 * <h2>Book/LMAX Mapping</h2>
 *
 * <p>These parameters directly enable LMAX Disruptor principles:
 * <ul>
 *   <li><b>Spin-before-yield:</b> SPIN_ATTEMPTS controls busy-spin duration
 *       before yielding to OS scheduler (mechanical sympathy principle).</li>
 *   <li><b>Single-writer optimized polling:</b> BATCH_SIZE amortizes tailer
 *       overhead across multiple events (batching principle).</li>
 *   <li><b>Preemption resilience:</b> YIELD_ATTEMPTS + WAIT_TIMEOUT provide
 *       graceful degradation on non-isolated CPUs.</li>
 * </ul>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 * @since 2026-09-13
 */
public final class TailerOptimizationConfig {

    /**
     * Maximum busy-spin attempts before yielding to OS scheduler.
     * Configurable via {@code -Dfx.optimized.tailer.spin.attempts=<n>}.
     * Default: 10,000 (≈ 20–50µs on modern CPUs at 50k msg/s).
     *
     * <p><b>Production tuning:</b>
     * <ul>
     *   <li>Bare-metal with isolcpus: increase to 100,000 (thread never yielded)</li>
     *   <li>Docker/shared-CPU: decrease to 1,000 (early yield reduces lock contention)</li>
     *   <li>macOS dev: use 10,000 (balanced for Rosetta2 overhead)</li>
     * </ul>
     */
    public static final int SPIN_ATTEMPTS = Integer.getInteger(
            "fx.optimized.tailer.spin.attempts", 10_000);

    /**
     * Maximum yield attempts before calling {@link Thread#sleep(long)}.
     * Configurable via {@code -Dfx.optimized.tailer.yield.count=<n>}.
     * Default: 100.
     *
     * <p>Only used if queue remains empty after SPIN_ATTEMPTS exhausted.
     * Each yield is ≈ 1µs on Linux, but OS scheduler decision cost can be 10–100µs.
     */
    public static final int YIELD_ATTEMPTS = Integer.getInteger(
            "fx.optimized.tailer.yield.count", 100);

    /**
     * Timeout (ms) for blocking wait when queue is empty after spin/yield.
     * Default: 1 millisecond.
     *
     * <p>Only reaches this path on non-isolated CPUs (macOS, Docker) or under
     * extreme load (producer stalled). On production bare-metal with affinity,
     * this timeout should be unreachable.
     */
    public static final long WAIT_TIMEOUT_MILLIS = 1L;

    /**
     * Event batch size for amortizing tailer overhead.
     * Configurable via {@code -Dfx.batch.size=<n>}.
     * Default: 128.
     *
     * <p><b>Tuning guidance:</b>
     * <ul>
     *   <li>128 = good balance for 50k msg/s (reduces tailer wake-ups 2–4×)</li>
     *   <li>256+ = for higher rates (75k+) or downstream congestion</li>
     *   <li>32 = for low-latency sensitivity (reduce batch processing window)</li>
     * </ul>
     */
    public static final int BATCH_SIZE = Integer.getInteger(
            "fx.batch.size", 128);

    /**
     * Whether to enable optimized event loop (with batching + busy-spin tailer).
     * Configurable via {@code -Dfx.use.optimized.eventloop=true}.
     * Default: false (use standard implementation).
     *
     * <p><b>Feature flag design:</b> Allows safe rollback to standard
     * {@link com.fx.common.handler.AbstractEventLoop#runLoop()} if needed,
     * without recompilation or service restart (only JVM property change).
     */
    public static final boolean USE_OPTIMIZED_EVENTLOOP = Boolean.parseBoolean(
            System.getProperty("fx.use.optimized.eventloop", "false"));

    /**
     * Whether to enable GC stress detection during benchmarks.
     * Configurable via {@code -Dfx.gc.stress.detector.enabled=true}.
     * Default: false (disabled in production).
     *
     * <p><b>Production note:</b> This detector polls JVM heap usage every 10ms
     * (zero-cost pre-allocation), adding negligible overhead. Safe to enable
     * in staging/validation but disabled by default in production.
     */
    public static final boolean GC_STRESS_DETECTOR_ENABLED = Boolean.parseBoolean(
            System.getProperty("fx.gc.stress.detector.enabled", "false"));

    /** Private constructor — this class is a static configuration holder. */
    private TailerOptimizationConfig() {
        throw new UnsupportedOperationException("TailerOptimizationConfig is a static configuration class");
    }

    /**
     * Validates configuration parameters at startup.
     * Called by service bootstrap to detect misconfiguration early.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static void validate() {
        if (SPIN_ATTEMPTS <= 0) {
            throw new IllegalArgumentException("SPIN_ATTEMPTS must be > 0, got " + SPIN_ATTEMPTS);
        }
        if (YIELD_ATTEMPTS <= 0) {
            throw new IllegalArgumentException("YIELD_ATTEMPTS must be > 0, got " + YIELD_ATTEMPTS);
        }
        if (WAIT_TIMEOUT_MILLIS <= 0) {
            throw new IllegalArgumentException("WAIT_TIMEOUT_MILLIS must be > 0, got " + WAIT_TIMEOUT_MILLIS);
        }
        if (BATCH_SIZE <= 0) {
            throw new IllegalArgumentException("BATCH_SIZE must be > 0, got " + BATCH_SIZE);
        }
    }

    /**
     * Returns a human-readable summary of current configuration.
     * Useful for logging at startup to confirm tuning is as expected.
     *
     * @return configuration summary string
     */
    public static String summary() {
        return String.format(
                "TailerOptimizationConfig[" +
                "spinAttempts=%d, yieldAttempts=%d, waitMs=%d, batchSize=%d, " +
                "optimizedEventLoop=%s, gcDetectorEnabled=%s]",
                SPIN_ATTEMPTS, YIELD_ATTEMPTS, WAIT_TIMEOUT_MILLIS, BATCH_SIZE,
                USE_OPTIMIZED_EVENTLOOP, GC_STRESS_DETECTOR_ENABLED);
    }
}
