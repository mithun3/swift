package com.fx.common.telemetry;

/**
 * {@code StageMetrics} — zero-allocation helper for recording per-stage pipeline latency.
 *
 * <p>Every event-loop stage records two intervals: time spent waiting on its input
 * queue, and time spent inside its own {@code handle()} call. This helper centralises
 * the repeated null-check-then-{@code recordValue} pattern that was previously
 * duplicated inline in serv-a, serv-b, and serv-c, so each service's hot path has one
 * call site per interval instead of a four-line block.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class StageMetrics {

    private StageMetrics() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Records {@code toNanos - fromNanos} on {@code recorder} if present.
     *
     * <p>Allocation-free: a single subtraction and a null check, identical in cost
     * to the inlined pattern this replaces. {@code recorder} may be {@code null}
     * when telemetry is disabled — the branch is a single, branch-predictor-friendly
     * comparison.
     *
     * @param recorder  the optional latency recorder; {@code null} skips recording
     * @param fromNanos the earlier {@code System.nanoTime()} sample
     * @param toNanos   the later {@code System.nanoTime()} sample
     */
    public static void record(final TelemetryRecorder recorder, final long fromNanos, final long toNanos) {
        if (recorder != null) {
            recorder.recordValue(toNanos - fromNanos);
        }
    }
}
