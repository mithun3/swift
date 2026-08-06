package com.fx.common.error;

import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;

/**
 * {@code ErrorQueueWriter} — Non-blocking, zero-allocation error event router.
 *
 * <h2>Role in the Pipeline</h2>
 * <p>
 * Every pipeline service holds a reference to an {@code ErrorQueueWriter}. When
 * a processing failure occurs in {@code AbstractEventLoop.run()}, the service
 * calls {@link #write(FxMarketEvent, String, String)} instead of throwing an exception.
 * This keeps the main event loop thread alive and processing subsequent events
 * without incurring stack-unwinding cost or JVM exception table lookups.
 *
 * <h2>Single-Writer Guarantee</h2>
 * <p>
 * Each service instantiates its own {@code ErrorQueueWriter} with its own
 * {@link ExcerptAppender}. Chronicle Queue allows multiple producers to the same
 * queue file via its rolling store mechanism — appenders coordinate via the
 * memory-mapped file's header, not via Java locks.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class ErrorQueueWriter implements AutoCloseable {

    /** The error chronicle queue. Shared across all services pointing to the same path. */
    private final ChronicleQueue errorQueue;

    /** Pre-acquired appender — reused across all error writes, no per-write allocation. */
    private final ExcerptAppender appender;

    /**
     * Pre-allocated error event flyweight. Populated and written on each error — 
     * never replaced with a new instance after construction.
     */
    private final ErrorEvent errorEventFlyweight;

    /**
     * Constructs a new error queue writer backed by a Chronicle Queue at the given path.
     *
     * @param queuePath the filesystem path to the error Chronicle Queue directory
     */
    public ErrorQueueWriter(final String queuePath) {
        this.errorQueue          = QueueFactory.create(queuePath);
        this.appender            = this.errorQueue.createAppender();
        this.errorEventFlyweight = new ErrorEvent();
    }

    /**
     * Writes a failure event to the error Chronicle Queue.
     *
     * <p>This method is designed to be called from the catch block of the event loop.
     * It does not throw. It does not log synchronously. It populates the pre-allocated
     * {@link ErrorEvent} flyweight and appends it to the error queue via the pre-acquired
     * appender — a single memory-mapped write, typically completing in under 1 µs.
     *
     * @param source       the FX event that failed processing
     * @param serviceName  the name of the calling service
     * @param errorMessage a short description of the failure (may be null)
     */
    public void write(final FxMarketEvent source,
                      final String serviceName,
                      final String errorMessage) {
        // Reset the flyweight before populating to avoid stale field values
        // from the previous error write contaminating this event.
        errorEventFlyweight.reset();
        // Populate fields from the failed event — this is off the hot path
        // (errors are exceptional), so String assignment is acceptable here.
        errorEventFlyweight.populateFrom(source, serviceName, errorMessage);
        // Append to the error Chronicle Queue — off-heap memory-mapped write.
        appender.writeDocument(errorEventFlyweight);
    }

    /**
     * Closes the underlying appender and Chronicle Queue, releasing all
     * memory-mapped file handles and flushing any pending writes.
     */
    @Override
    public void close() {
        appender.close();
        errorQueue.close();
    }
}
