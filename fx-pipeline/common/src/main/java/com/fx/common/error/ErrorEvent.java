package com.fx.common.error;

import com.fx.common.event.FxMarketEvent;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * {@code ErrorEvent} — Flyweight DTO for the error Chronicle Queue.
 *
 * <h2>Purpose</h2>
 * <p>
 * When a poisoned event is detected in any pipeline stage, the main processing
 * thread must not throw an exception (which would unwind the stack and disrupt
 * the hot path) nor log synchronously (which would incur I/O latency). Instead,
 * it writes a compact {@code ErrorEvent} to the dedicated {@code queue-err}
 * Chronicle Queue, which is drained by a separate low-priority thread.
 *
 * <h2>Zero-Allocation Design</h2>
 * <p>
 * Like {@link FxMarketEvent}, this is a mutable flyweight extended from
 * {@link SelfDescribingMarshallable}. The error writer pre-allocates one
 * instance and reuses it. Only the {@code errorMessage} field involves a
 * {@link String} — but error events are by definition off the hot path
 * (they represent exceptional conditions, not normal flow).
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class ErrorEvent extends SelfDescribingMarshallable {

    /** Correlation ID of the event that caused the error (copied from {@link FxMarketEvent}). */
    public long correlationId;

    /** Nanos timestamp at which the error was detected. */
    public long errorNanoTime;

    /** Short ASCII name of the service that generated this error. */
    public String serviceName;

    /** Human-readable error description — only populated off the hot path. */
    public String errorMessage;

    /** The {@link com.fx.common.event.EventStatus} value at the time of the error. */
    public int eventStatusAtError;

    /**
     * Resets all fields to sentinel/zero values for flyweight reuse.
     */
    public void reset() {
        correlationId     = 0L;
        errorNanoTime     = 0L;
        serviceName       = "";
        errorMessage      = "";
        eventStatusAtError = 0;
    }

    /**
     * Populates this error event from a failed FX event and its context.
     *
     * <p>This method is NOT on the critical hot path — it is only called when
     * an exception has already occurred, so String assignments are acceptable.
     *
     * @param source       the event that failed processing
     * @param serviceName  the name of the service that detected the error
     * @param errorMessage a description of the failure
     */
    public void populateFrom(final FxMarketEvent source,
                              final String serviceName,
                              final String errorMessage) {
        this.correlationId      = source.correlationId;
        this.errorNanoTime      = System.nanoTime();
        this.serviceName        = serviceName;
        this.errorMessage       = (errorMessage != null) ? errorMessage : "unknown";
        this.eventStatusAtError = source.eventStatus;
    }
}
