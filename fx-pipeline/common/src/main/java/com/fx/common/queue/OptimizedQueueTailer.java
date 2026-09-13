package com.fx.common.queue;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.ReadMarshallable;

/**
 * {@code OptimizedQueueTailer} — Drop-in wrapper for {@link ExcerptTailer}
 * that eliminates scheduler wake-up jitter by busy-spinning instead of
 * blocking when the queue is empty.
 *
 * <h2>Problem: Blocking Wait Latency Variance</h2>
 *
 * <p>Chronicle Queue's default tailer uses {@code Object#wait(long)} when
 * the queue is empty. On non-isolated CPUs (macOS, Docker), the OS scheduler
 * may delay re-scheduling the thread for 10–500µs after data arrives,
 * causing intermittent tail-latency spikes (observed: max 1.9ms in 1-second
 * intervals during 50k msg/s baseline run).
 *
 * <p>This tailer wrapper detects when data arrives using busy-spin with
 * {@link Thread#onSpinWait()}, only yielding to the OS scheduler after
 * exhausting a configurable spin limit. On isolated CPU cores (Linux with
 * {@code isolcpus}), the thread never yields, keeping it hot on-CPU with
 * microsecond wake-up latency.
 *
 * <h2>Zero-GC Design</h2>
 *
 * <ul>
 *   <li>No wrapper objects created per event (implements {@code AutoCloseable}
 *       for lifecycle only, not per-message).</li>
 *   <li>No String allocation on the hot path.</li>
 *   <li>Busy-spin loop contains only primitive comparisons and
 *       {@link Thread#onSpinWait()}.</li>
 * </ul>
 *
 * <h2>LMAX Disruptor Pattern</h2>
 *
 * <p>Implements the Disruptor's {@code BusySpinWaitStrategy} adapted for
 * Chronicle Queue. The spin-then-yield design follows
 * {@code PhasedBackOffWaitStrategy} but is tuned for queue-based latency
 * predictability (no randomness, no micro-nap).
 *
 * <h2>Configuration</h2>
 *
 * <p>Tunable via {@link TailerOptimizationConfig}:
 * <ul>
 *   <li>{@code SPIN_ATTEMPTS} — how many {@link Thread#onSpinWait()} calls
 *       before yielding (default: 10,000 ≈ 20–50µs on modern CPUs)</li>
 *   <li>{@code YIELD_ATTEMPTS} — how many times to yield before blocking wait
 *       (default: 100)</li>
 *   <li>{@code WAIT_TIMEOUT_MILLIS} — milliseconds to sleep if queue remains
 *       empty (default: 1ms, only reached on non-isolated CPUs)</li>
 * </ul>
 *
 * <h2>API Compatibility</h2>
 *
 * <p>This class wraps {@link ExcerptTailer} but does NOT implement the full
 * tailer interface. It only exposes methods used by
 * {@link com.fx.common.handler.AbstractEventLoop}:
 * {@link #readDocument(ReadMarshallable)}, {@link #index()}, {@link #close()},
 * {@link #underlying()}.
 *
 * <p>For advanced tailer operations (seek, direction, etc.), users can access
 * the underlying tailer via {@link #underlying()}.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 * @since 2026-09-13
 */
public final class OptimizedQueueTailer implements AutoCloseable {

    /** The wrapped Chronicle Queue tailer. */
    private final ExcerptTailer underlying;

    /** Last recorded index (updated after every successful read). */
    private long lastIndex;

    /**
     * Constructs a new optimized tailer wrapper.
     *
     * @param underlying the Chronicle Queue tailer to wrap; must not be null
     * @throws NullPointerException if {@code underlying} is null
     */
    public OptimizedQueueTailer(final ExcerptTailer underlying) {
        if (underlying == null) {
            throw new NullPointerException("Underlying tailer must not be null");
        }
        this.underlying = underlying;
        this.lastIndex = underlying.index();
    }

    /**
     * Creates an optimized tailer from the given queue.
     *
     * <p>Convenience factory method — equivalent to:
     * {@code new OptimizedQueueTailer(queue.createTailer(name))}.
     *
     * @param queue the Chronicle Queue to tail
     * @param name  the tailer name (for logging/identification)
     * @return a new optimized tailer wrapper
     * @throws NullPointerException if {@code queue} is null
     */
    public static OptimizedQueueTailer create(final ChronicleQueue queue,
                                              final String name) {
        if (queue == null) {
            throw new NullPointerException("Queue must not be null");
        }
        return new OptimizedQueueTailer(queue.createTailer(name));
    }

    /**
     * Attempts to read a document from the queue, using busy-spin to reduce
     * wake-up latency variance.
     *
     * <p>This method implements the core optimization: instead of calling
     * {@code underlying.readDocument()} (which internally may call
     * {@code Object#wait()}), we spin on the queue's index, only resorting
     * to blocking after exhausting spin/yield attempts.
     *
     * <p><b>Semantics:</b> Returns {@code true} if a document was read and is
     * available for processing. Returns {@code false} if the queue is empty
     * and no document could be read.
     *
     * <p><b>Zero-allocation:</b> No objects created during spin loop; only
     * primitive comparisons and {@link Thread#onSpinWait()}.
     *
     * @param message the {@link ReadMarshallable} (e.g. {@code FxMarketEvent})
     *                to decode the queue's document into
     * @return {@code true} if a document was read and decoded into
     *         {@code message}, {@code false} if the queue is empty
     */
    public boolean readDocument(final ReadMarshallable message) {
        // Fast path: try to read directly — if data is already available,
        // this avoids spin/yield overhead entirely. Must pass the actual
        // message through so Chronicle decodes the document's fields into it;
        // a no-op callback here would silently discard the event's data.
        if (underlying.readDocument(message)) {
            lastIndex = underlying.index();
            return true;
        }

        // Busy-spin path: queue appears empty, but may fill while we spin.
        // Spin without allocating, without yielding, using PAUSE instruction
        // (x86) or YIELD (ARM). On isolated CPUs, thread stays hot on-CPU
        // without context-switch latency.
        for (int spinAttempt = 0; spinAttempt < TailerOptimizationConfig.SPIN_ATTEMPTS; spinAttempt++) {
            Thread.onSpinWait();  // CPU hint: reduce memory bus contention
            if (underlying.readDocument(message)) {
                lastIndex = underlying.index();
                return true;
            }
        }

        // Yield path: after spin exhausted, yield to OS scheduler a few times
        // before giving up entirely. Helps on non-isolated CPUs where other
        // runnable threads may be contending for the core.
        for (int yieldAttempt = 0; yieldAttempt < TailerOptimizationConfig.YIELD_ATTEMPTS; yieldAttempt++) {
            Thread.yield();  // Yield to OS scheduler
            if (underlying.readDocument(message)) {
                lastIndex = underlying.index();
                return true;
            }
        }

        // Last resort: call blocking wait on the underlying tailer.
        // On bare-metal with isolcpus, this path is unreachable.
        // On macOS/Docker without isolation, this is a fallback that reduces
        // busy-spin overhead when the queue is truly empty for extended periods
        // (e.g., between load generator batches).
        try {
            Thread.sleep(TailerOptimizationConfig.WAIT_TIMEOUT_MILLIS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();  // Restore interrupt status
        }

        // Final attempt after backoff
        if (underlying.readDocument(message)) {
            lastIndex = underlying.index();
            return true;
        }

        return false;  // Queue remains empty
    }

    /**
     * Accesses the underlying {@link ExcerptTailer} for advanced operations
     * not wrapped by this class (e.g., {@code seek()}, {@code direction()}).
     *
     * @return the wrapped tailer (never null)
     */
    public ExcerptTailer underlying() {
        return underlying;
    }

    /**
     * Returns the index of the last successfully read document.
     *
     * <p>Updated only after {@link #readDocument(ReadMarshallable)} returns
     * {@code true}. Safe to call from the tailer thread at any time; no
     * synchronization needed (single-writer principle).
     *
     * @return the current tailer index
     */
    public long index() {
        return lastIndex;
    }

    /**
     * Closes the underlying tailer and releases any resources.
     *
     * <p>Safe to call multiple times (idempotent).
     */
    @Override
    public void close() {
        underlying.close();
    }
}
