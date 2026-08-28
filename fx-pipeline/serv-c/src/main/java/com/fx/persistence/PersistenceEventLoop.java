package com.fx.persistence;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.handler.AbstractEventLoop;
import com.fx.common.queue.QueueFactory;
import com.fx.common.queue.QueuePaths;
import com.fx.common.telemetry.StageMetrics;
import com.fx.common.telemetry.TelemetryRecorder;
import net.openhft.chronicle.queue.ExcerptAppender;

import java.sql.SQLException;

/**
 * {@code PersistenceEventLoop} — serv-c: Terminal Persistence and Egress Service.
 *
 * <h2>Responsibility</h2>
 * <p>
 * Tails {@code queue-c} and persists every event (including rejections and failures)
 * to the {@code fx_trades} H2 database table via {@link BatchPersistenceEngine}.
 * This is the terminal stage — no output queue is written.
 *
 * <h2>Batch Persistence Strategy</h2>
 * <p>
 * Events are accumulated in a pre-allocated batch buffer and flushed in bulk.
 * The {@code endOfBatch} hint from Chronicle Queue's tailer is used to trigger
 * early flushes during periods of low traffic — preventing events from sitting
 * unbatched in the buffer for extended periods.
 *
 * <h2>No Output Queue</h2>
 * <p>
 * {@code serv-c} is the terminal consumer — it has no output Chronicle Queue.
 * The {@code outputQueue} parameter in {@link AbstractEventLoop} is {@code null}.
 * The {@link ExcerptAppender} passed to {@link #handle} will therefore be {@code null},
 * and this implementation never calls it.
 *
 * <h2>End-to-End Latency Telemetry</h2>
 * <p>
 * If a {@link TelemetryRecorder} is injected, this service records the end-to-end
 * pipeline latency ({@code System.nanoTime() - event.ingressNanoTime}) for every
 * event via {@link TelemetryRecorder#recordValue(long)}. The recorder is
 * zero-allocation on the recording side (uses {@code SingleWriterRecorder} internally)
 * and flushes histograms to an {@code .hlog} file on a background thread.
 *
 * <p>If no recorder is provided ({@code null}), telemetry is silently skipped.
 * This keeps the hot path branch-predictor friendly: the null check compiles
 * to a single CMP instruction with near-zero cost.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class PersistenceEventLoop extends AbstractEventLoop {

    /** CPU core for the persistence thread. Core 3 is isolated from all other services. */
    public static final int CPU_CORE = 3;

    /**
     * Default JDBC URL for the H2 2.x in-memory database (MVStore engine).
     *
     * <p>H2 2.x removed the {@code LOG} and {@code UNDO_LOG} URL parameters that
     * existed in H2 1.x — supplying them causes the connection to fail.  The MVStore
     * engine manages its own write-ahead strategy internally; an in-memory database
     * already keeps all data off-disk, so no additional URL-level tuning is needed.
     *
     * <p>Override at runtime with {@code -Dfx.persistence.jdbc.url=<url>}.
     */
    public static final String DEFAULT_JDBC_URL =
            System.getProperty("fx.persistence.jdbc.url",
                    "jdbc:h2:mem:fxdb;DB_CLOSE_DELAY=-1;MODE=MySQL");

    /** The batch database writer — pre-allocated, stateful, AutoCloseable. */
    private final BatchPersistenceEngine persistenceEngine;

    /**
     * Optional zero-allocation latency recorder for end-to-end latency (T3 - T0).
     */
    private final TelemetryRecorder e2eRecorder;

    /**
     * Optional zero-allocation latency recorder for queue-c wait time.
     */
    private final TelemetryRecorder queueCRecorder;

    /**
     * Optional zero-allocation latency recorder for serv-c duration.
     */
    private final TelemetryRecorder servCRecorder;

    /**
     * Constructs the persistence event loop with no telemetry recording.
     *
     * <p>Equivalent to {@code PersistenceEventLoop(jdbcUrl, null, null, null)}.
     *
     * @param jdbcUrl JDBC URL for the database sink
     * @throws SQLException if the database connection cannot be established
     */
    public PersistenceEventLoop(final String jdbcUrl) throws SQLException {
        this(jdbcUrl, null, null, null);
    }

    /**
     * Constructs the persistence event loop with optional telemetry recording.
     *
     * <p>Queue-a/serv-a and queue-b/serv-b latencies are recorded independently by serv-a
     * and serv-b themselves (at the point each stage actually processes the event) rather
     * than here, so that per-stage sample counts reflect events actually processed by that
     * stage instead of only events that reach this terminal stage.
     *
     * @param jdbcUrl        JDBC URL for the database sink
     * @param e2eRecorder    optional HdrHistogram recorder for end-to-end latency
     * @param queueCRecorder optional HdrHistogram recorder for queue-c latency
     * @param servCRecorder  optional HdrHistogram recorder for serv-c latency
     * @throws SQLException if the database connection cannot be established
     */
    public PersistenceEventLoop(final String jdbcUrl,
                                 final TelemetryRecorder e2eRecorder,
                                 final TelemetryRecorder queueCRecorder,
                                 final TelemetryRecorder servCRecorder) throws SQLException {
        super(
                "persist-c",
                QueueFactory.createWithOverride(QueuePaths.QUEUE_C, "queue-c"),
                null, // Terminal service — no output Chronicle Queue
                new ErrorQueueWriter(QueuePaths.QUEUE_ERR),
                CPU_CORE
        );
        this.persistenceEngine  = new BatchPersistenceEngine(jdbcUrl);
        this.e2eRecorder        = e2eRecorder;
        this.queueCRecorder     = queueCRecorder;
        this.servCRecorder      = servCRecorder;
    }

    /**
     * Accumulates the event into the batch buffer, records telemetry, and flushes when appropriate.
     *
     * <p>This is the terminal hot path. The only operations here are:
     * <ol>
     *   <li>T3 stage-entry timestamp capture — marks when serv-c received the event.</li>
     *   <li>End-to-end latency recording via {@link TelemetryRecorder} (if present).</li>
     *   <li>Primitive field copy into the batch slot (in {@link BatchPersistenceEngine#accumulate}).</li>
     *   <li>Conditional batch flush (triggered by buffer-full or endOfBatch).</li>
     * </ol>
     *
     * <p>All operations are allocation-free: no {@code new}, no boxing, no String formatting.
     *
     * @param event      the fully processed FX market event
     * @param sequence   Chronicle Queue index
     * @param endOfBatch {@code true} if no more events are immediately available
     * @param appender   always {@code null} for serv-c (no output queue)
     */
    @Override
    protected void handle(final FxMarketEvent event,
                           final long sequence,
                           final boolean endOfBatch,
                           final ExcerptAppender appender) {
        // T3: Stage-entry timestamp — the precise nanosecond serv-c received this event.
        // Must be captured before any other operation to measure true queue-to-handler latency
        // for the queue-c → serv-c segment.
        event.t3ServCEntry = System.nanoTime();

        // End-to-end pipeline latency = time from FIX ingress (T0) to persistence entry (T3).
        // Recorded via SingleWriterRecorder — zero-allocation, wait-free.
        // Queue-a/serv-a and queue-b/serv-b are recorded upstream by serv-a and serv-b
        // themselves — see RiskValidationEventLoop and PricingEventLoop.
        StageMetrics.record(queueCRecorder, event.t2ServBExit, event.t3ServCEntry);

        try {
            // Accumulate the event — zero-allocation primitive copy into batch buffer.
            // Flush is triggered internally when buffer is full or endOfBatch is true.
            persistenceEngine.accumulate(event, endOfBatch);
        } catch (final SQLException e) {
            // Database write failure — route to error queue.
            // The persistence loop continues with the next event; the failed event
            // is captured in queue-err for diagnosis and potential replay.
            errorWriter.write(event, "persist-c", e.getMessage());
        }

        long t3ServCExit = System.nanoTime();
        StageMetrics.record(servCRecorder, event.t3ServCEntry, t3ServCExit);
        StageMetrics.record(e2eRecorder, event.ingressNanoTime, t3ServCExit);
    }

    /**
     * Extends the base {@link #close()} to also flush and close the persistence engine.
     *
     * <p>This ensures any events remaining in the batch buffer are written to the
     * database before the JVM exits, preventing data loss on clean shutdown.
     */
    @Override
    public void close() {
        super.close(); // Stops the loop and closes Chronicle queues
        persistenceEngine.close(); // Flushes remaining batch and closes JDBC connection
    }
}
