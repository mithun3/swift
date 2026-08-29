package com.fx.common.handler;

/**
 * {@code BusySpinWaitStrategy} — the Disruptor's lowest-latency {@link WaitStrategy}:
 * spins on {@link Thread#onSpinWait()} instead of yielding, sleeping, or blocking.
 *
 * <p>Appropriate only for a thread pinned to a dedicated CPU core, since it keeps
 * that core at 100% utilization while idle. Stateless singleton — {@link #INSTANCE}
 * is the single pre-allocated reference shared by every event loop, so selecting
 * this strategy never allocates.
 */
public final class BusySpinWaitStrategy implements WaitStrategy {

    /** Shared singleton — stateless, so one instance serves every event loop. */
    public static final BusySpinWaitStrategy INSTANCE = new BusySpinWaitStrategy();

    private BusySpinWaitStrategy() {
    }

    @Override
    public void idle() {
        // PAUSE (x86) / YIELD (ARM) hint: reduces memory-bus contention while
        // keeping the thread hot, avoiding the context-switch cost of blocking.
        Thread.onSpinWait();
    }
}
