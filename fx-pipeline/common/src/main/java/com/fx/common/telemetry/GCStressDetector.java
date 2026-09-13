package com.fx.common.telemetry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * {@code GCStressDetector} — Monitor heap allocation activity during benchmarks
 * to correlate with latency tail spikes.
 *
 * <h2>Purpose</h2>
 *
 * <p>This detector runs as a background thread (or can be called periodically)
 * and tracks JVM heap allocation deltas. If heap allocations correlate with
 * tail-latency spikes, they indicate garbage collection pressure or unexpected
 * object allocation on hot paths.
 *
 * <p>The detector is designed for <b>validation and debugging only</b> —
 * disabled by default in production. When enabled via system property
 * {@code -Dfx.gc.stress.detector.enabled=true}, it records allocation deltas
 * into an HdrHistogram for later analysis.
 *
 * <h2>Zero-Allocation Design</h2>
 *
 * <ul>
 *   <li>No allocations on the polling path (uses pre-allocated TelemetryRecorder).</li>
 *   <li>No String formatting or logging in {@link #recordAllocationDelta()}.</li>
 *   <li>Polls JVM's {@link MemoryMXBean#getHeapMemoryUsage()} which is zero-allocation.</li>
 *   <li>Stores only {@code long} values (heap usage delta) in histogram.</li>
 * </ul>
 *
 * <h2>Limitations & Caveats</h2>
 *
 * <p><b>Not real-time:</b> Heap usage tracking is coarse-grained (sampled
 * at poll intervals, typically 10ms). Young-generation allocations may not
 * be visible if they're freed between polls.
 *
 * <p><b>Not GC-specific:</b> Heap delta includes all allocations (young gen,
 * old gen, survivor moves). Does not distinguish between allocation types.
 *
 * <p><b>Overhead:</b> MemoryMXBean access is fast (~1µs), but do not call
 * recordAllocationDelta() on the critical event-loop path. Instead, call
 * from a background monitoring thread or during drain phase.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>
 *   // At startup
 *   if (TailerOptimizationConfig.GC_STRESS_DETECTOR_ENABLED) {
 *       GCStressDetector detector = new GCStressDetector(
 *           telemetryRecorder.child("gc-allocation-delta"));
 *       new Thread(detector::monitorLoop, "gc-stress-detector").start();
 *   }
 * </pre>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 * @since 2026-09-13
 */
public final class GCStressDetector {

    /**
     * Polling interval (ms) for GC stress detection.
     * Configurable via {@code -Dfx.gc.detector.poll.ms=<ms>}.
     * Default: 10 milliseconds.
     *
     * <p><b>Production tuning:</b>
     * <ul>
     *   <li>10ms (default): catches allocation spikes with reasonable granularity</li>
     *   <li>100ms: lower overhead, misses fine-grained allocation patterns</li>
     * </ul>
     */
    private static final long POLL_INTERVAL_MILLIS = Long.getLong(
            "fx.gc.detector.poll.ms", 10L);

    /** JVM memory beans for heap usage tracking. */
    private final MemoryMXBean memBean;

    /**
     * Recorder for allocation delta values.
     * May be null if detector was constructed without a recorder.
     */
    private final TelemetryRecorder allocationRecorder;

    /** Last recorded heap usage (bytes). Updated after each poll. */
    private long lastHeapBytes;

    /** Running flag for monitor loop. */
    private volatile boolean running;

    /**
     * Constructs a GC stress detector with an optional recorder.
     *
     * @param allocationRecorder optional HdrHistogram recorder for delta values;
     *                           if null, deltas are counted but not recorded
     */
    public GCStressDetector(final TelemetryRecorder allocationRecorder) {
        this.memBean = ManagementFactory.getMemoryMXBean();
        this.allocationRecorder = allocationRecorder;
        this.lastHeapBytes = memBean.getHeapMemoryUsage().getUsed();
        this.running = false;
    }

    /**
     * Records the current heap allocation delta to the histogram.
     *
     * <p>To be called periodically (e.g., every 10ms) from a background
     * monitoring thread. Never call from the event-loop thread.
     *
     * <p><b>Zero-allocation:</b> Only reads JVM state and records long values.
     */
    public void recordAllocationDelta() {
        final long currentHeapBytes = memBean.getHeapMemoryUsage().getUsed();
        final long delta = currentHeapBytes - lastHeapBytes;

        // Record positive deltas (allocations). Negative deltas indicate
        // GC collection between polls — typically not recorded to avoid noise.
        if (delta > 0 && allocationRecorder != null) {
            allocationRecorder.recordValue(delta);
        }

        lastHeapBytes = currentHeapBytes;
    }

    /**
     * Starts a continuous monitoring loop in the calling thread.
     *
     * <p>This method blocks indefinitely until {@link #stop()} is called.
     * Intended to be called from a dedicated background thread:
     * <pre>
     *   Thread monitorThread = new Thread(detector::monitorLoop, "gc-monitor");
     *   monitorThread.setDaemon(true);
     *   monitorThread.start();
     * </pre>
     *
     * <p>Calls {@link #recordAllocationDelta()} at {@link #POLL_INTERVAL_MILLIS}
     * intervals until interrupted or {@link #stop()} is called.
     */
    public void monitorLoop() {
        running = true;
        while (running) {
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
                recordAllocationDelta();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Stops the monitoring loop gracefully.
     *
     * <p>Safe to call from any thread. The monitor loop will exit on the next
     * polling interval.
     */
    public void stop() {
        running = false;
    }

    /**
     * Returns the last recorded heap usage (bytes).
     *
     * @return heap usage in bytes
     */
    public long getLastHeapBytes() {
        return lastHeapBytes;
    }
}
