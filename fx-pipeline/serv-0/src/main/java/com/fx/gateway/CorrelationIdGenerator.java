package com.fx.gateway;

import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code CorrelationIdGenerator} — Thread-safe, monotonically increasing 64-bit ID source.
 *
 * <h2>Why a Sequential {@code long} and not UUID?</h2>
 * <p>
 * A UUID is a 128-bit value typically represented as a 36-character {@link String}
 * ({@code "550e8400-e29b-41d4-a716-446655440000"}). Creating that string:
 * <ul>
 *   <li>Allocates a heap {@code String} object on every call.</li>
 *   <li>Performs random number generation (UUID v4) involving OS entropy reads.</li>
 *   <li>Produces a value that doesn't order naturally — binary searching or sorting
 *       a UUID set requires string comparison rather than integer comparison.</li>
 * </ul>
 *
 * <p>A monotonic {@code long} counter:
 * <ul>
 *   <li>Fits in a single CPU register (8 bytes).</li>
 *   <li>Increments via a single CAS (compare-and-swap) instruction —
 *       the {@link AtomicLong#incrementAndGet()} on x86 compiles to LOCK XADD.</li>
 *   <li>Provides natural total ordering for replay, gap detection, and debugging.</li>
 *   <li>Will not overflow for ~292 years at 1 billion IDs per second.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Although serv-0 is a single-threaded event loop, the generator is backed by
 * {@link AtomicLong} so that test code and diagnostic tools may call it from any
 * thread without data races. The cost is negligible — one CAS per call.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class CorrelationIdGenerator {

    /**
     * The underlying sequence counter.
     *
     * <p>Starts at 1 (not 0) so that the sentinel "unset" value of {@code 0L} in
     * {@link com.fx.common.event.FxMarketEvent#correlationId} is immediately
     * distinguishable from a legitimately generated ID.
     */
    private final AtomicLong sequence;

    /**
     * Constructs a new generator starting from sequence number 1.
     */
    public CorrelationIdGenerator() {
        // Start at 0; first call to next() returns 1.
        this.sequence = new AtomicLong(0L);
    }

    /**
     * Constructs a new generator starting from a given initial value.
     *
     * <p>Use this constructor for warm-restart scenarios where the system must
     * resume from a previously persisted sequence number.
     *
     * @param initialValue the value BEFORE the first generated ID; the first
     *                     call to {@link #next()} will return {@code initialValue + 1}
     */
    public CorrelationIdGenerator(final long initialValue) {
        this.sequence = new AtomicLong(initialValue);
    }

    /**
     * Returns the next unique correlation ID.
     *
     * <p>This is a lock-free, wait-free operation. The only hardware primitive
     * used is a single LOCK XADD (or equivalent on ARM64: LDXADD). There are
     * zero heap allocations and no object references involved.
     *
     * @return the next monotonically increasing {@code long} ID, always {@code >= 1}
     */
    public long next() {
        return sequence.incrementAndGet();
    }

    /**
     * Returns the last generated ID without advancing the sequence.
     *
     * <p>Useful for diagnostics and replay bookmarking. Not recommended for use
     * in the hot path due to the potential for races with concurrent calls to
     * {@link #next()} — though for single-threaded usage this is always consistent.
     *
     * @return the most recently generated correlation ID, or {@code 0L} if none yet
     */
    public long current() {
        return sequence.get();
    }

    /**
     * Resets the generator back to zero.
     *
     * <p><strong>WARNING:</strong> For use in tests and controlled restarts ONLY.
     * Calling this in a live production system will cause correlation ID collisions
     * and break traceability across the pipeline.
     */
    public void reset() {
        sequence.set(0L);
    }
}
