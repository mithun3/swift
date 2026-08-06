package com.fx.pricing;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.handler.AbstractEventLoop;
import com.fx.common.queue.QueueFactory;
import com.fx.common.queue.QueuePaths;
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

    /** CPU core for the pricing thread — isolated from serv-0 (core 0) and serv-a (core 1). */
    public static final int CPU_CORE = 2;

    /** Stateless spread computation engine — pre-allocated once. */
    private final SpreadEngine spreadEngine;

    /**
     * Constructs the pricing event loop, connecting queue-b → queue-c.
     */
    public PricingEventLoop() {
        super(
                "pricing-b",
                QueueFactory.createWithOverride(QueuePaths.QUEUE_B, "queue-b"),
                QueueFactory.createWithOverride(QueuePaths.QUEUE_C, "queue-c"),
                new ErrorQueueWriter(QueuePaths.QUEUE_ERR),
                CPU_CORE
        );
        this.spreadEngine = new SpreadEngine();
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
        // Fast-path: if the event was already rejected upstream, skip pricing entirely.
        // Forward the event to queue-c so serv-c can persist the rejection record.
        if (EventStatus.isTerminalFailure(event.eventStatus)) {
            appender.writeDocument(event);
            return;
        }

        // Apply tier-based spread to compute the execution price.
        final boolean pricingSucceeded = spreadEngine.applySpread(event);

        // Update event status based on pricing outcome.
        event.eventStatus = pricingSucceeded ? EventStatus.PRICED : EventStatus.PRICING_FAILED;

        // Forward to queue-c regardless of pricing outcome — for full audit trail.
        appender.writeDocument(event);
    }
}
