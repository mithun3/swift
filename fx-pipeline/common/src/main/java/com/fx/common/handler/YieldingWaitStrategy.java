package com.fx.common.handler;

/**
 * {@code YieldingWaitStrategy} — a wait strategy that spins briefly and then
 * yields indefinitely.
 *
 * <p>This strategy is the LMAX Disruptor's recommended approach for environments
 * where threads share CPUs but still require sub-millisecond latencies (e.g.,
 * macOS local development or VMs).
 *
 * <p>It operates in two phases:
 * <ol>
 *   <li><b>Spin phase</b> (iterations 0 – {@link #SPIN_TRIES}): calls
 *       {@link Thread#onSpinWait()}. This absorbs micro-bursts of latency without
 *       context-switching.</li>
 *   <li><b>Yield phase</b>: continuously calls {@link Thread#yield()} indefinitely.
 *       Unlike {@link PhasedBackOffWaitStrategy}, this strategy <b>never parks</b>.
 *       This avoids the massive OS timer resolution penalties (30-50µs) associated
 *       with {@link java.util.concurrent.locks.LockSupport#parkNanos(long)}. At the
 *       same time, yielding prevents the OS scheduler from forcefully preempting the
 *       thread for 10-80ms (which happens with {@link BusySpinWaitStrategy} on shared CPUs).</li>
 * </ol>
 *
 * <p>Because this strategy holds mutable state ({@link #spinCount}), it is
 * <b>NOT a singleton</b>. Each {@link AbstractEventLoop} must hold its own instance.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 * @see BusySpinWaitStrategy
 * @see PhasedBackOffWaitStrategy
 */
public final class YieldingWaitStrategy implements WaitStrategy {

    /**
     * Number of {@link Thread#onSpinWait()} iterations before transitioning to yield.
     * 1000 iterations takes approximately 2µs on Apple Silicon, effectively absorbing
     * standard queue transmission jitter.
     */
    private static final int SPIN_TRIES = 1_000;

    /** Current position within the spin cycle. */
    private int spinCount;

    /** Package-private constructor — use {@link WaitStrategy#fromSystemProperty()}. */
    YieldingWaitStrategy() {
        this.spinCount = 0;
    }

    @Override
    public void reset() {
        this.spinCount = 0;
    }

    @Override
    public void idle() {
        if (spinCount < SPIN_TRIES) {
            Thread.onSpinWait();
            spinCount++;
        } else {
            // Indefinite yield: Cooperatively relinquish CPU to avoid scheduler
            // preemption punishment, but completely avoid the timer slack of parking.
            Thread.yield();
        }
    }
}
