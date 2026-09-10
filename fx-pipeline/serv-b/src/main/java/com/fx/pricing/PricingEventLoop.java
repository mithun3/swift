package com.fx.pricing;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.handler.AbstractEventLoop;
import com.fx.common.queue.QueueFactory;
import com.fx.common.queue.QueuePaths;
import com.fx.common.telemetry.StageMetrics;
import com.fx.common.telemetry.TelemetryRecorder;
import net.openhft.chronicle.queue.ExcerptAppender;

/**
 * {@code PricingEventLoop} — serv-b: FX Spread Application and Pricing Engine.
 *
 * <h2>Responsibility</h2>
 * <p>
 * Tails {@code queue-b}, applies FX spread pricing for ACCEPTED events via
 * {@link SpreadEngine}, and appends the execution report to {@code queue-c}.
 * CREDIT_REJECTED events are forwarded to queue-c unchanged for full audit persistence.
 *
 * <h2>Fast-Path Branching</h2>
 * <p>
 * When an event arrives with {@code eventStatus == CREDIT_REJECTED}, the pricing
 * engine skips all computation and forwards the event directly. This branch is
 * branch-predictor friendly because rejected events are statistically rare —
 * the CPU's branch predictor will learn to favour the ACCEPTED path.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class PricingEventLoop extends AbstractEventLoop {

    /**
     * CPU core for the pricing thread.
     *
     * <p>Resolved from the system property {@code fx.serv-b.cpucore} at class load
     * (see {@link AbstractEventLoop#resolveCpuCore}), defaulting to {@code 2} when
     * no property is set. Set {@code -Dfx.serv-b.cpucore=3} in {@code baremetal.env}
     * to align with {@code FX_SERV_B_CPUSET="3"} and ensure AffinityLock pins to
     * the same core that {@code taskset} already assigned.
     */
    public static final int CPU_CORE = resolveCpuCore("serv-b", 2);

    /** Stateless spread computation engine — pre-allocated once. */
    private final SpreadEngine spreadEngine;

    /** Optional zero-allocation latency recorder for queue-b wait time (T2Entry - T1Exit). */
    private final TelemetryRecorder queueBRecorder;

    /** Optional zero-allocation latency recorder for serv-b duration (T2Exit - T2Entry). */
    private final TelemetryRecorder servBRecorder;

    /**
     * Constructs the pricing event loop, connecting queue-b → queue-c, with no telemetry.
     */
    public PricingEventLoop() {
        this(null, null);
    }

    /**
     * Constructs the pricing event loop with optional per-stage telemetry recording.
     *
     * <p>Recording independently at this stage (rather than downstream at serv-c) ensures the
     * sample count reflects events actually processed by serv-b, regardless of whether they
     * later complete the rest of the pipeline before the benchmark run ends.
     *
     * @param queueBRecorder optional HdrHistogram recorder for queue-b wait time
     * @param servBRecorder  optional HdrHistogram recorder for serv-b processing duration
     */
    public PricingEventLoop(final TelemetryRecorder queueBRecorder,
                             final TelemetryRecorder servBRecorder) {
        super(
                "pricing-b",
                QueueFactory.createWithOverride(QueuePaths.QUEUE_B, "queue-b"),
                QueueFactory.createWithOverride(QueuePaths.QUEUE_C, "queue-c"),
                new ErrorQueueWriter(QueuePaths.QUEUE_ERR),
                CPU_CORE
        );
        this.spreadEngine = new SpreadEngine();
        this.queueBRecorder = queueBRecorder;
        this.servBRecorder = servBRecorder;
    }

    /**
     * Prices a single FX market event from queue-b and writes to queue-c.
     *
     * <p>Allocation-free. No objects created. Single-writer to queue-c.
     *
     * @param event      the flyweight event from queue-b
     * @param sequence   Chronicle Queue index
     * @param endOfBatch batch boundary hint
     * @param appender   sole writer to queue-c
     */
    @Override
    protected void handle(final FxMarketEvent event,
                           final long sequence,
                           final boolean endOfBatch,
                           final ExcerptAppender appender) {
        // Stage-entry timestamp — T2 in the per-stage latency chain.
        // Captured before any branch evaluation to measure true queue-to-handler latency.
        event.t2ServBEntry = System.nanoTime();

        // Fast-path: if the event was already rejected upstream, skip pricing entirely.
        // Forward the event to queue-c so serv-c can persist the rejection record.
        if (EventStatus.isTerminalFailure(event.eventStatus)) {
            event.t2ServBExit = System.nanoTime();
            appender.writeDocument(event);
            recordTelemetry(event);
            return;
        }

        // Apply tier-based spread to compute the execution price.
        final boolean pricingSucceeded = spreadEngine.applySpread(event);

        // Update event status based on pricing outcome.
        event.eventStatus = pricingSucceeded ? EventStatus.PRICED : EventStatus.PRICING_FAILED;

        // Forward to queue-c regardless of pricing outcome — for full audit trail.
        event.t2ServBExit = System.nanoTime();
        appender.writeDocument(event);
        recordTelemetry(event);
    }

    /**
     * Recorded here (not downstream at serv-c) so the sample count reflects events
     * actually processed by serv-b, independent of later pipeline stages.
     *
     * @param event the event just forwarded to queue-c
     */
    private void recordTelemetry(final FxMarketEvent event) {
        StageMetrics.record(queueBRecorder, event.t1ServAExit, event.t2ServBEntry);
        StageMetrics.record(servBRecorder, event.t2ServBEntry, event.t2ServBExit);
    }
}
