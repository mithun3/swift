package com.fx.gateway;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.handler.AbstractEventLoop;
import com.fx.common.queue.QueueFactory;
import com.fx.common.queue.QueuePaths;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;

/**
 * {@code GatewayEventLoop} — serv-0: FIX Ingestion Gateway Event Loop.
 *
 * <h2>Responsibility</h2>
 * <p>
 * This is the pipeline entry point. It simulates reading raw FIX 4.4 messages
 * from a client connection (represented here as a pre-built byte array for
 * deterministic, no-TCP-dependency testing), decoding them, and appending them
 * to {@code queue-a} for downstream processing by serv-a.
 *
 * <h2>Processing Flow per Event</h2>
 * <ol>
 *   <li>Read next raw FIX byte buffer from the source.</li>
 *   <li>Reset the flyweight and the decode frame.</li>
 *   <li>Call {@link FixDecoder#decode} — zero-allocation byte parsing.</li>
 *   <li>If decode fails → write to error queue; skip to next message.</li>
 *   <li>Assign a {@link CorrelationIdGenerator#next() correlationId}.</li>
 *   <li>Stamp {@link System#nanoTime()} as {@code ingressNanoTime}.</li>
 *   <li>Populate remaining flyweight fields from the decode frame.</li>
 *   <li>Set {@code eventStatus = RECEIVED}.</li>
 *   <li>Append flyweight to {@code queue-a} via pre-acquired {@link ExcerptAppender}.</li>
 * </ol>
 *
 * <h2>FIX Source</h2>
 * <p>
 * In this implementation, the FIX source is a {@link FixMessageSource} that generates
 * deterministic synthetic FIX messages in a byte buffer. This allows full pipeline
 * testing without a live FIX counterparty or TCP socket. The interface is designed
 * so that a real TCP NIO socket reader can be plugged in without changing this class.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class GatewayEventLoop extends AbstractEventLoop {

    /** CPU core for the gateway thread. Core 0 handles FIX ingestion. */
    public static final int CPU_CORE = 0;

    /** The FIX byte-level parser — pre-allocated, stateful (holds temp buffers). */
    private final FixDecoder decoder;

    /** Reusable decode frame — populated by FixDecoder, transferred to flyweight. */
    private final FixDecoder.FxMessageFrame frame;

    /** Source of raw FIX byte messages — injectable for testability. */
    private final FixMessageSource messageSource;

    /** Generates monotonically increasing 64-bit correlation IDs. */
    private final CorrelationIdGenerator idGenerator;

    /**
     * Constructs the gateway event loop.
     *
     * <p>The output queue ({@code queue-a}) and error queue are opened here.
     * The input queue parameter inherited from {@link AbstractEventLoop} is not
     * used in the gateway (it reads from {@link FixMessageSource} instead of a
     * Chronicle Queue), but the abstract loop structure is preserved for lifecycle
     * uniformity across all services.
     *
     * @param messageSource the FIX byte message source
     * @param idGenerator   the correlation ID generator
     */
    public GatewayEventLoop(final FixMessageSource messageSource,
                             final CorrelationIdGenerator idGenerator) {
        super(
                "gateway",
                // The gateway reads from the FIX source, not a Chronicle Queue.
                // We still need a valid inputQueue for the AbstractEventLoop
                // constructor, so we open queue-a as both input reference and
                // output — the handle is only used for lifecycle (close on shutdown).
                QueueFactory.createWithOverride(QueuePaths.QUEUE_A, "queue-a"),
                QueueFactory.createWithOverride(QueuePaths.QUEUE_A, "queue-a"),
                new ErrorQueueWriter(QueuePaths.QUEUE_ERR),
                CPU_CORE
        );
        this.messageSource = messageSource;
        this.idGenerator   = idGenerator;
        this.decoder       = new FixDecoder();
        // Pre-allocate the decode frame once — reused across all messages.
        this.frame         = new FixDecoder.FxMessageFrame();
    }

    /**
     * Overrides the standard tail-and-dispatch loop from {@link AbstractEventLoop}.
     *
     * <p>The gateway does not tail a Chronicle Queue — it reads from the
     * {@link FixMessageSource} directly. We override {@link AbstractEventLoop#run()}
     * to provide a custom loop body while preserving the thread lifecycle management
     * (pinned platform thread, busy-spin, clean stop signal).
     */
    @Override
    public void run() {
        // Acquire a dedicated ExcerptAppender for queue-a.
        // ExcerptAppender is NOT thread-safe — we must never share it across threads.
        // This is safe here because we are the single writer for queue-a.
        try (final ExcerptAppender appender = outputQueue.createAppender()) {
            while (isRunning()) {
                // Poll the FIX message source for the next raw byte buffer.
                // Returns -1 if no message is currently available.
                final int bytesRead = messageSource.poll(messageSource.buffer(),
                        0, messageSource.buffer().length);

                if (bytesRead > 0) {
                    // A FIX message is available — process it.
                    processFixMessage(appender, bytesRead);
                } else {
                    // No data available — busy-spin with CPU hint.
                    // This avoids an OS context switch at the cost of one CPU core
                    // spinning continuously. Acceptable for a dedicated pinned core.
                    Thread.onSpinWait();
                }
            }
        }
    }

    /**
     * Processes a single FIX message from the source buffer.
     *
     * <p>This method contains the entire hot-path logic for serv-0. Every operation
     * is allocation-free: no new objects are created, no String conversions occur.
     *
     * @param appender  the queue-a appender to write the decoded event into
     * @param bytesRead the number of valid bytes in the source buffer
     */
    private void processFixMessage(final ExcerptAppender appender, final int bytesRead) {
        // Step 1: Reset the decode frame to clear any stale fields from the previous message.
        frame.reset();

        // Step 2: Invoke the zero-allocation FIX parser.
        final boolean decodeSuccess = decoder.decode(
                messageSource.buffer(), 0, bytesRead, frame);

        if (!decodeSuccess) {
            // Structural validation failed — route to error queue and skip.
            // We temporarily populate the flyweight with what we have so the
            // error writer can capture the correlation context.
            flyweight.reset();
            flyweight.eventStatus = EventStatus.VALIDATION_FAILED;
            errorWriter.write(flyweight, "gateway", "FIX decode failed");
            return;
        }

        // Step 3: Reset the main flyweight and populate all fields from the frame.
        flyweight.reset();

        // Step 4: Assign a globally unique, monotonically increasing correlation ID.
        // This is the primary key that traces this order across all pipeline stages.
        flyweight.correlationId = idGenerator.next();

        // Step 5: Stamp a high-resolution nanosecond ingress timestamp.
        // System.nanoTime() is a monotonic clock — no allocation, no calendar lookup.
        flyweight.ingressNanoTime = System.nanoTime();

        // Step 6: Transfer all decoded FIX fields into the flyweight.
        flyweight.fixMsgType          = frame.msgType;
        flyweight.fixSeqNum           = frame.seqNum;
        flyweight.clientId            = frame.clientId;
        flyweight.currencyPairCode    = frame.currencyPairCode;
        flyweight.side                = frame.side;
        flyweight.notionalMinorUnits  = frame.notionalMinorUnits;
        flyweight.requestedPriceScaled = frame.requestedPriceScaled;

        // Step 7: Set the initial processing status.
        flyweight.eventStatus = EventStatus.RECEIVED;

        // Step 8: Append to queue-a.
        // This is a memory-mapped write into the Chronicle Queue store file —
        // typically completing in < 1 microsecond with warm page cache.
        appender.writeDocument(flyweight);
    }

    /**
     * Not used by the gateway — handle() is part of the standard tail-dispatch
     * flow in AbstractEventLoop. The gateway overrides run() entirely.
     *
     * @param event      unused
     * @param sequence   unused
     * @param endOfBatch unused
     * @param appender   unused
     */
    @Override
    protected void handle(final FxMarketEvent event,
                           final long sequence,
                           final boolean endOfBatch,
                           final ExcerptAppender appender) {
        // Intentionally empty — gateway uses a custom run() loop.
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INNER INTERFACE: FIX MESSAGE SOURCE
    // Separating the source behind an interface enables:
    //   1. In tests: inject a synthetic deterministic source (no TCP).
    //   2. In production: inject an NIO selector-based TCP socket reader.
    // This follows the Dependency Inversion Principle without any framework.
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * {@code FixMessageSource} — Abstraction over FIX byte message delivery.
     *
     * <p>A {@code FixMessageSource} provides a pre-allocated byte buffer and a
     * {@link #poll} method that fills the buffer with the next FIX message bytes.
     * This design avoids byte[] allocation per-poll and makes the source
     * swappable (TCP socket, file replay, synthetic generator).
     */
    public interface FixMessageSource {

        /**
         * Returns the pre-allocated byte buffer for message storage.
         *
         * <p>The buffer is provided by the source implementation to allow
         * caller-side reuse without allocation. The gateway always reads from
         * {@code buffer[0..bytesRead-1}} after a successful {@link #poll}.
         *
         * @return the reusable byte buffer; never null
         */
        byte[] buffer();

        /**
         * Attempts to fill {@code buf} with the next FIX message starting at
         * {@code offset}, writing at most {@code maxLength} bytes.
         *
         * @param buf       destination buffer (same as returned by {@link #buffer()})
         * @param offset    start position in the buffer
         * @param maxLength maximum bytes to write
         * @return number of bytes written (> 0 if a message is available),
         *         or {@code -1} if no message is currently available
         */
        int poll(byte[] buf, int offset, int maxLength);

        /**
         * Returns {@code true} if the source has been exhausted (for finite sources
         * like file replay). For infinite sources (TCP socket), always returns {@code false}.
         *
         * @return {@code true} if no further messages will ever be produced
         */
        boolean isExhausted();
    }
}
