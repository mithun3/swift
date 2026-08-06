package com.fx.risk;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.handler.AbstractEventLoop;
import com.fx.common.queue.QueueFactory;
import com.fx.common.queue.QueuePaths;
import net.openhft.chronicle.queue.ExcerptAppender;

/**
 * {@code RiskValidationEventLoop} — serv-a: Credit Check and Risk Validation Service.
 *
 * <h2>Responsibility</h2>
 * <p>
 * Tails {@code queue-a}, performs credit and tier validation on each
 * {@link FxMarketEvent} via {@link CreditCheckEngine}, mutates the event status
 * in-place (no object allocation), and appends the result to {@code queue-b}.
 *
 * <h2>Single-Writer Principle</h2>
 * <p>
 * This service is the sole writer to {@code queue-b}. The Chronicle Queue
 * {@link ExcerptAppender} is acquired once and reused for all writes — it is NOT
 * thread-safe and must never be shared.
 *
 * <h2>Garbage-Free Mutation</h2>
 * <p>
 * The event flyweight arriving from {@code queue-a} is mutated in-place:
 * {@code event.eventStatus} is set to either {@link EventStatus#ACCEPTED} or
 * {@link EventStatus#CREDIT_REJECTED}. All other fields from serv-0 are
 * preserved verbatim. No new {@link FxMarketEvent} is allocated — the same
 * flyweight object (populated by {@link AbstractEventLoop}) is written to queue-b.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class RiskValidationEventLoop extends AbstractEventLoop {

    /** CPU core for the risk validation thread. Core 1 is isolated from the gateway. */
    public static final int CPU_CORE = 1;

    /**
     * The credit check engine — stateless, allocation-free validation logic.
     * Pre-constructed at startup; its credit-limit arrays are read-only on the hot path.
     */
    private final CreditCheckEngine creditCheckEngine;

    /**
     * Constructs the risk validation event loop, connecting queue-a → queue-b.
     */
    public RiskValidationEventLoop() {
        super(
                "risk-a",
                QueueFactory.createWithOverride(QueuePaths.QUEUE_A, "queue-a"),
                QueueFactory.createWithOverride(QueuePaths.QUEUE_B, "queue-b"),
                new ErrorQueueWriter(QueuePaths.QUEUE_ERR),
                CPU_CORE
        );
        this.creditCheckEngine = new CreditCheckEngine();
    }

    /**
     * Processes a single FX market event from queue-a.
     *
     * <p>This is the hot path for serv-a. Every line of code in this method
     * must be allocation-free.
     *
     * <p><b>LMAX Principle: In-Place Mutation</b><br>
     * Rather than copying the event into a new object, we mutate the flyweight
     * fields that change at this stage ({@code clientTier}, {@code eventStatus})
     * and forward the same object. This is equivalent to the Disruptor pattern of
     * publishing a mutated ring-buffer slot to the next consumer.
     *
     * @param event      the mutable flyweight populated from queue-a
     * @param sequence   Chronicle Queue index of this excerpt
     * @param endOfBatch {@code true} if no further events are immediately queued
     * @param appender   pre-acquired appender for queue-b; the sole writer
     */
    @Override
    protected void handle(final FxMarketEvent event,
                           final long sequence,
                           final boolean endOfBatch,
                           final ExcerptAppender appender) {
        // Step 1: Resolve the client tier from the clientId.
        // The tier is stored in the event so downstream services can access it
        // without re-performing the lookup. This is a key pattern: compute once,
        // carry forward through the pipeline.
        event.clientTier = CreditCheckEngine.resolveTier(event.clientId);

        // Step 2: Perform credit check — zero-allocation validation.
        final boolean accepted = creditCheckEngine.validate(event);

        // Step 3: Mutate the event status in-place based on the validation result.
        // Using a ternary expression avoids an extra branch instruction on the fast path.
        event.eventStatus = accepted ? EventStatus.ACCEPTED : EventStatus.CREDIT_REJECTED;

        // Step 4: Write the mutated event to queue-b.
        // This is a single memory-mapped append — typically < 500 nanoseconds.
        // The appender serialises the flyweight's fields directly into the
        // off-heap Chronicle buffer. No intermediate byte[] is created.
        appender.writeDocument(event);

        // Note: We intentionally write BOTH accepted and credit-rejected events to
        // queue-b. This gives serv-c (persistence) a complete audit trail of all
        // orders, including rejections. serv-b will detect the CREDIT_REJECTED status
        // and skip pricing logic, forwarding the event directly to queue-c.
    }
}
