package com.fx.common.handler;

import java.util.concurrent.locks.LockSupport;

/**
 * {@code PhasedBackOffWaitStrategy} — the LMAX Disruptor's recommended wait
 * strategy for event processors running in <b>shared-CPU environments</b>
 * (Docker containers, virtual machines, any host where the event loop thread
 * is not the sole tenant of a dedicated physical core).
 *
 * <h2>Three-Phase Idle Behaviour</h2>
 * <ol>
 *   <li><b>Spin phase</b> (iterations 0 – {@link #SPIN_TRIES}): calls
 *       {@link Thread#onSpinWait()} — maps to the ARM {@code YIELD} instruction
 *       on Apple Silicon and the x86 {@code PAUSE} instruction on Intel/AMD.
 *       Keeps the thread hot in the CPU pipeline with near-zero power overhead
 *       (~2 ns/iteration). Covers the 99% case where an event arrives within
 *       ~400 ns (200 iterations × ~2 ns each).</li>
 *   <li><b>Yield phase</b> (iterations {@link #SPIN_TRIES} – {@link #YIELD_LIMIT}):
 *       calls {@link Thread#yield()} — a co-operative scheduler hint that allows
 *       same-priority threads (ZGC concurrent GC, JIT compiler) to run without
 *       triggering a full park/unpark cycle. Covers the 0.9% case where the queue
 *       is empty for a few microseconds. Each yield costs ~1-2 µs but prevents
 *       CFS from forcibly preempting the event loop to run housekeeping threads.</li>
 *   <li><b>Park phase</b> (iterations ≥ {@link #YIELD_LIMIT}): calls
 *       {@link LockSupport#parkNanos(long)} for 1 µs — used only when the queue is
 *       genuinely empty. Completely releases the vCPU to the OS for rescheduling of
 *       ZGC/JIT/db-writer threads, at the cost of ~1-2 µs wake-up latency. The
 *       counter resets after parking so the spin phase is re-entered fresh on the
 *       next event.</li>
 * </ol>
 *
 * <h2>When to use this strategy vs {@link BusySpinWaitStrategy}</h2>
 * <ul>
 *   <li>Use {@link BusySpinWaitStrategy} on native Linux with {@code isolcpus} — the
 *       event loop owns a dedicated physical core exclusively, so co-operative yielding
 *       wastes cycles with no benefit.</li>
 *   <li>Use {@code PhasedBackOffWaitStrategy} in Docker (macOS, Proxmox VM) where
 *       the event loop shares its cpuset with ZGC, JIT, and background threads. The
 *       phased approach lets housekeeping threads run co-operatively, preventing CFS
 *       from force-preempting the event loop at unpredictable moments.</li>
 * </ul>
 *
 * <h2>Enabling via system property</h2>
 * <pre>
 *   # Docker / VM environments:
 *   -Dfx.waitstrategy=phased
 *
 *   # Native Linux with isolcpus (default):
 *   -Dfx.waitstrategy=busyspin
 * </pre>
 * See {@link WaitStrategy#fromSystemProperty()} for the factory method.
 *
 * <h2>Instance lifecycle</h2>
 * <p>This class holds mutable state ({@link #spinCount}) and therefore is
 * <b>NOT a singleton</b>. Each {@link AbstractEventLoop} must hold its own
 * instance. See {@link WaitStrategy#fromSystemProperty()} which returns a new
 * instance per call when {@code phased} is selected.
 *
 * <h2>GC-free guarantee</h2>
 * <p>The {@link #idle()} method contains no allocations. The {@link #spinCount}
 * field is a primitive {@code int} stored inline in the instance.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 * @see BusySpinWaitStrategy
 * @see WaitStrategy#fromSystemProperty()
 */
public final class PhasedBackOffWaitStrategy implements WaitStrategy {

    /**
     * Number of {@link Thread#onSpinWait()} iterations before transitioning to yield.
     *
     * <p>10000 iterations × ~2 ns/iter on ARM (Apple Silicon) = ~20 µs spin window.
     * At 50k TPS, inter-arrival time is 20µs. This ensures we don't fall back to
     * yielding/parking prematurely during sustained load.
     */
    private static final int SPIN_TRIES  = 10_000;

    /**
     * Cumulative iteration threshold before transitioning to park.
     *
     * <p>{@link Thread#yield()} is called for iterations
     * {@link #SPIN_TRIES} through {@code YIELD_LIMIT - 1}: 100 yield calls
     * each lasting ~1-2 µs on a lightly loaded system. In the worst-case idle
     * period, this covers ~100-200 µs of genuine queue emptiness before parking.
     */
    private static final int YIELD_LIMIT = SPIN_TRIES + 100;

    /**
     * Park duration when the queue is genuinely idle (nanoseconds).
     *
     * <p>1 µs allows the OS to reschedule ZGC/JIT/db-writer threads without
     * incurring the overhead of the full OS scheduler quantum (typically 1 ms).
     * After parking, the spin counter resets so the spin phase is re-entered
     * on the next poll cycle.
     */
    private static final long PARK_NANOS = 1_000L;

    /** Current position within the three-phase idle cycle. */
    private int spinCount;

    /** Package-private constructor — use {@link WaitStrategy#fromSystemProperty()}. */
    PhasedBackOffWaitStrategy() {
        this.spinCount = 0;
    }

    @Override
    public void reset() {
        this.spinCount = 0;
    }

    /**
     * Executes one idle iteration according to the current phase.
     *
     * <p>This method is called by {@link AbstractEventLoop#runLoop} exactly once
     * per iteration where {@code tailer.readDocument()} returns {@code false}.
     * It must not block indefinitely or perform I/O.
     */
    @Override
    public void idle() {
        if (spinCount < SPIN_TRIES) {
            // Phase 1: CPU PAUSE/YIELD hint — keeps thread hot at near-zero cost.
            Thread.onSpinWait();
            spinCount++;
        } else if (spinCount < YIELD_LIMIT) {
            // Phase 2: co-operative yield — lets ZGC/JIT run without forcing CFS
            // preemption of the event loop. The scheduler may return immediately if
            // no same-priority thread is runnable, keeping latency low.
            Thread.yield();
            spinCount++;
        } else {
            // Phase 3: park — queue is genuinely empty. Completely free the vCPU
            // for housekeeping threads. Wake-up latency ~1-2 µs is acceptable when
            // the pipeline has no events to process.
            LockSupport.parkNanos(PARK_NANOS);
            // Intentionally NOT resetting spinCount here. The state remains parked
            // to avoid oscillating between phase 1 and 3 on a truly empty queue.
            // AbstractEventLoop calls reset() to bring us back to Phase 1 when
            // an event successfully arrives.
        }
    }
}
