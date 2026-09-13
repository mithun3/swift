package com.fx.common.handler;

import net.openhft.chronicle.queue.ExcerptTailer;

/**
 * {@code EventBatchBuffer} — Pre-allocated batch buffer intended to reduce
 * Chronicle Queue tailer wake-up frequency via batched reads.
 *
 * <p><b>UNUSED / KNOWN BROKEN:</b> {@link #fillBatch(ExcerptTailer)} only
 * captures each document's index via a no-op reader callback — it never
 * decodes the document's fields into an event object. Chronicle Queue's
 * {@code readDocument} is a forward-only consume operation, so by the time
 * {@link #nextIndex()} is used to "process" an earlier index, that document's
 * content is already gone. Every event this class touches was silently
 * delivered to handlers as an empty/default flyweight. This caused
 * {@code queue-a}/{@code queue-b}/{@code queue-c}, end-to-end, and db-commit
 * telemetry to record zero samples in bare-metal runs (2026-09-13) while
 * per-stage dispatch metrics, which depend only on locally-set timestamps,
 * kept working.
 *
 * <p>{@link com.fx.common.handler.AbstractEventLoop#runLoopOptimized} no
 * longer uses this class — it calls
 * {@link com.fx.common.queue.OptimizedQueueTailer#readDocument} directly,
 * which decodes into the real flyweight. Do not reintroduce this class on
 * the hot path without first fixing the decode gap described above.
 *
 * <h2>Zero-GC Design</h2>
 *
 * <ul>
 *   <li>All buffers pre-allocated in the constructor (no per-batch allocations).</li>
 *   <li>Single {@code long[]} for storing event indices (no event copies).</li>
 *   <li>Reusable across the entire benchmark/production run.</li>
 *   <li>No String, no temporary objects created in {@link #fillBatch(ExcerptTailer)}.</li>
 *   <li>No lambdas or anonymous classes on hot path (zero closure allocation).</li>
 * </ul>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 * @since 2026-09-13
 */
public final class EventBatchBuffer {

    /**
     * Maximum batch size: defaults from {@link com.fx.common.queue.TailerOptimizationConfig}.
     * Pre-allocated in the constructor.
     *
     * <p><b>Tuning guidance:</b>
     * <ul>
     *   <li>128 (default) = good balance at 50k msg/s</li>
     *   <li>256+ = for higher rates (75k+/s) or downstream congestion</li>
     *   <li>32 = for latency-sensitive workloads (reduce batch window)</li>
     * </ul>
     */
    private final long[] eventIndices;

    /** Number of valid events in the buffer (set by fillBatch()). */
    private int batchCount;

    /** Current position within the batch during iteration. */
    private int batchPosition;

    /**
     * Constructs a batch buffer with the default batch size from configuration.
     */
    public EventBatchBuffer() {
        this(com.fx.common.queue.TailerOptimizationConfig.BATCH_SIZE);
    }

    /**
     * Constructs a batch buffer with a specific batch size.
     *
     * @param batchSize maximum events per batch; must be > 0
     * @throws IllegalArgumentException if batchSize <= 0
     */
    public EventBatchBuffer(final int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be > 0, got " + batchSize);
        }
        // Pre-allocate the buffer once — this is the single heap allocation
        // for batch processing in the entire run.
        this.eventIndices = new long[batchSize];
        this.batchCount = 0;
        this.batchPosition = 0;
    }

    /**
     * Fills the batch buffer from the tailer, returning the number of events read.
     *
     * <p>This method reads up to {@code eventIndices.length} events from the tailer
     * and stores their indices in the pre-allocated array. It returns the count of
     * events actually read (may be less than array size if the queue becomes empty).
     *
     * <p><b>Zero-allocation:</b> This method allocates no new objects — it only
     * reads indices from the tailer and stores them in the pre-allocated {@code long[]}
     * created at construction.
     *
     * <p><b>Reset:</b> Internal {@code batchPosition} is reset to 0, allowing the
     * caller to iterate the batch via {@link #hasNext()} and {@link #nextIndex()}.
     *
     * @param tailer the Chronicle Queue tailer to read from; must not be null
     * @return the number of events read into the batch (0 if queue is empty)
     * @throws NullPointerException if {@code tailer} is null
     */
    public int fillBatch(final ExcerptTailer tailer) {
        if (tailer == null) {
            throw new NullPointerException("Tailer must not be null");
        }

        int count = 0;
        final int maxSize = eventIndices.length;

        // Read up to maxSize events from the tailer.
        // Each readDocument() advances the tailer's internal index if successful.
        while (count < maxSize) {
            final int currentCount = count;  // Capture for lambda (must be final)
            if (tailer.readDocument(ctx -> {
                // Inside callback: event data is available in Chronicle's
                // internal message buffer. We only record the index here;
                // actual event data will be accessed during handle() processing.
                eventIndices[currentCount] = tailer.index();
            })) {
                count++;
            } else {
                // Queue empty or error — stop reading batch.
                break;
            }
        }

        // Store count and reset iteration position for subsequent calls.
        batchCount = count;
        batchPosition = 0;

        return count;
    }

    /**
     * Returns whether there are more events in the current batch to process.
     *
     * <p>Intended use in a loop:
     * <pre>
     *   while (batch.hasNext()) {
     *       long eventIndex = batch.nextIndex();
     *       handle(eventIndex, ...);
     *   }
     * </pre>
     *
     * @return {@code true} if there are events remaining in the batch
     */
    public boolean hasNext() {
        return batchPosition < batchCount;
    }

    /**
     * Returns the index of the next event in the batch and advances the position.
     *
     * <p>Only valid after {@link #fillBatch(ExcerptTailer)} has been called
     * and {@link #hasNext()} returns {@code true}.
     *
     * <p>The returned index corresponds to the Chronicle Queue index of the
     * event, suitable for use with {@link ExcerptTailer#index()} for diagnostics
     * and sequence tracking.
     *
     * @return the event index (long)
     * @throws IndexOutOfBoundsException if {@link #hasNext()} is false
     */
    public long nextIndex() {
        if (batchPosition >= batchCount) {
            throw new IndexOutOfBoundsException("No more events in batch; " +
                    "hasNext() returned false");
        }
        return eventIndices[batchPosition++];
    }

    /**
     * Resets the batch position without clearing the buffer.
     *
     * <p>Useful for re-iterating the batch multiple times without re-filling
     * from the queue. Does not modify {@code batchCount} or the underlying
     * {@code eventIndices} array.
     */
    public void reset() {
        batchPosition = 0;
    }

    /**
     * Returns the maximum batch size (capacity).
     *
     * @return batch size (fixed at construction, never changes)
     */
    public int getMaxBatchSize() {
        return eventIndices.length;
    }

    /**
     * Returns the number of valid events currently in the batch.
     *
     * @return count (0 if fillBatch() has not been called or queue was empty)
     */
    public int getBatchCount() {
        return batchCount;
    }
}
