package com.fx.common.handler;

import com.fx.common.event.FxMarketEvent;

/**
 * {@code EventLoopHandler} — Single-Writer Event Handler Interface.
 *
 * <h2>LMAX Disruptor Analogy</h2>
 * <p>
 * In the LMAX Disruptor, an {@code EventHandler<T>} receives pre-allocated event
 * objects from the ring buffer one at a time on a single dedicated thread. This
 * interface mirrors that contract for our Chronicle Queue-backed pipeline:
 *
 * <ul>
 *   <li>Each service implements exactly one {@code EventLoopHandler}.</li>
 *   <li>The handler is always invoked from the same pinned thread — guaranteeing
 *       the single-writer principle: no synchronisation, no locks, no CAS loops.</li>
 *   <li>The {@code FxMarketEvent} passed to {@link #onEvent} is the reused flyweight
 *       instance — the handler mutates it in-place and writes it to the output queue.</li>
 * </ul>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Implementations MUST NOT allocate heap objects in {@link #onEvent}.</li>
 *   <li>Implementations MUST NOT throw checked exceptions from {@link #onEvent}.
 *       On unrecoverable errors, route the event to the error queue and return.</li>
 *   <li>Implementations MUST complete {@link #onEvent} without blocking I/O or locks.</li>
 * </ul>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
@FunctionalInterface
public interface EventLoopHandler {

    /**
     * Processes a single {@link FxMarketEvent} read from an input Chronicle Queue.
     *
     * <p>This method is the entire hot path for a pipeline stage. Every nanosecond
     * spent here is latency added to the end-to-end trade lifecycle. The implementation
     * must be:
     * <ul>
     *   <li><b>Allocation-free:</b> No {@code new} keyword, no autoboxing,
     *       no {@code String} formatting, no stream operations.</li>
     *   <li><b>Lock-free:</b> Single-threaded access is guaranteed by the pinned
     *       event loop — do not introduce any synchronised blocks or volatile writes
     *       beyond what the queue infrastructure already manages.</li>
     *   <li><b>Non-blocking:</b> Never call blocking I/O (file, socket, database)
     *       directly from this method. Delegate to asynchronous mechanisms.</li>
     * </ul>
     *
     * @param event      the mutable flyweight event populated from the input queue;
     *                   the handler is permitted and expected to mutate its fields
     * @param sequence   the Chronicle Queue index (monotonic excerpt sequence number)
     *                   of this event — useful for gap detection and replay
     * @param endOfBatch {@code true} if this is the last event currently available
     *                   in the queue's batch (equivalent to the Disruptor's
     *                   {@code endOfBatch} flag); use to trigger batch flushes
     */
    void onEvent(FxMarketEvent event, long sequence, boolean endOfBatch);
}
