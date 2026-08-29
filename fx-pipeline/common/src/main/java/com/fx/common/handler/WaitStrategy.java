package com.fx.common.handler;

/**
 * {@code WaitStrategy} — pluggable idle behavior for an {@link AbstractEventLoop}
 * when its poll source has no event ready.
 *
 * <p>Maps to the LMAX Disruptor {@code WaitStrategy} concept: consumers delegate
 * to a single named strategy when caught up, rather than inlining the idle
 * behavior at every poll site. The Disruptor offers several trade-offs here
 * (BusySpin, Yielding, Sleeping, Blocking — lowest latency to lowest CPU usage);
 * this pipeline only implements {@link BusySpinWaitStrategy}, matching its
 * pinned-dedicated-core deployment model.
 *
 * <p>Implementations must be allocation-free and safe to invoke from the single
 * pinned event-loop thread only.
 */
public interface WaitStrategy {

    /**
     * Invoked when no event is currently available from the poll source.
     * Must not block indefinitely or perform I/O.
     */
    void idle();
}
