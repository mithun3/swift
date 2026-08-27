package com.fx.persistence;

import com.fx.common.event.FxMarketEvent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * {@code BatchPersistenceEngine} — Asynchronous batch write engine for serv-c.
 *
 * <h2>Batching Strategy</h2>
 * <p>
 * Individual JDBC INSERT calls incur significant per-row overhead: network round-trips
 * (even for embedded DBs), JDBC statement preparation, and transaction commit latency.
 * Batching amortises this cost across multiple rows.
 *
 * <p>This engine accumulates events into an internal pre-allocated array (the "batch
 * buffer") and flushes to the database when either:
 * <ol>
 *   <li>The batch reaches {@link #BATCH_SIZE} entries, or</li>
 *   <li>The {@code endOfBatch} flag signals no more events are immediately available
 *       (equivalent to a "drain" flush to prevent stale data in low-throughput periods).</li>
 * </ol>
 *
 * <h2>Pre-Allocated Batch Buffer</h2>
 * <p>
 * The batch buffer is a fixed-size array of {@link BatchRow} objects, all pre-allocated
 * at construction. On each event, the engine copies primitive fields from the flyweight
 * into the next available {@link BatchRow} slot. No new objects are created in the
 * hot path — only primitive field assignments.
 *
 * <h2>JDBC Prepared Statement Reuse</h2>
 * <p>
 * A single {@link PreparedStatement} is created at startup and reused across all
 * batches. Re-using a prepared statement avoids SQL parsing and query plan generation
 * on every insert.
 *
 * <h2>Transaction Safety</h2>
 * <p>
 * Manual transaction control is used ({@code autoCommit=false}). A full JDBC batch
 * execute is wrapped in a try-catch: on failure, {@code connection.rollback()} is
 * called to discard any partial writes, and the error is propagated to the caller
 * so the event loop can route it to the error queue.
 *
 * <h2>Per-Stage Timestamps</h2>
 * <p>
 * The {@code t1ServAEntry}, {@code t2ServBEntry}, and {@code t3ServCEntry} nanosecond
 * timestamps from {@link FxMarketEvent} are persisted alongside the business fields.
 * This allows post-hoc per-stage latency analysis directly from the database.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class BatchPersistenceEngine implements AutoCloseable {

    /**
     * Ring-buffer capacity: 524,288 slots (power of two, required for mask-based wrapping).
     *
     * <p>At 1M events/sec this ring absorbs ~524 ms of burst before the hot-path
     * {@link #accumulate} spin-wait fires.  The previous 65,536-slot ring only
     * covered ~65 ms, causing serv-c to stall whenever an H2 {@code executeBatch()}
     * took longer than that threshold &mdash; which in turn starved queue-c and
     * produced the observed P99 = 5 s queue-c wait times.
     *
     * <p>Memory footprint: 524,288 &times; ~128 bytes per {@link BatchRow} &asymp; 64 MB.
     * All slots are pre-allocated at construction &mdash; zero GC after startup.
     */
    private static final int RING_SIZE = 524_288;
    private static final int MASK = RING_SIZE - 1;

    /**
     * Maximum rows pulled into a single JDBC {@code executeBatch()} call.
     *
     * <p>Raised from 4,096 to 32,768 to reduce commit frequency and amortise
     * H2 transaction overhead over larger batches, lowering the average time
     * spent inside each {@code commit()} and keeping the ring from filling.
     */
    private static final int MAX_BATCH = 32_768;

    /** Pre-allocated batch row objects — never replaced after construction. */
    private final BatchRow[] ringBuffer = new BatchRow[RING_SIZE];

    /** Volatile pointer advanced by the event loop. */
    private volatile long writePointer = 0;

    /** Volatile pointer advanced by the background JDBC thread. */
    private volatile long readPointer = 0;

    /** Background thread for asynchronous database inserts. */
    private final Thread dbThread;
    
    /** Flag to signal the background thread to stop gracefully. */
    private volatile boolean running = true;

    /** JDBC connection to the H2 in-memory database. */
    private final Connection connection;

    /** Reusable prepared statement for batch INSERT. */
    private final PreparedStatement insertStatement;

    /**
     * Constructs the persistence engine with a JDBC connection to the given URL.
     *
     * @param jdbcUrl JDBC URL (e.g., {@code "jdbc:h2:mem:fxdb;DB_CLOSE_DELAY=-1"})
     * @throws SQLException if the connection or schema initialisation fails
     */
    public BatchPersistenceEngine(final String jdbcUrl) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        initSchema();
        this.insertStatement = connection.prepareStatement(
                "INSERT INTO fx_trades ("
                        + "correlation_id, ingress_nano, client_id, client_tier, "
                        + "currency_pair_code, side, notional_minor, "
                        + "requested_price_scaled, executed_price_scaled, "
                        + "spread_scaled, event_status, "
                        + "t1_serv_a_entry, t1_serv_a_exit, t2_serv_b_entry, t2_serv_b_exit, t3_serv_c_entry"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        // Pre-allocate all batch row objects once at construction.
        for (int i = 0; i < RING_SIZE; i++) {
            ringBuffer[i] = new BatchRow();
        }

        // Start the background JDBC writer thread
        this.dbThread = new Thread(this::flushLoop, "db-writer");
        this.dbThread.setDaemon(true);
        this.dbThread.start();
    }

    /**
     * Accumulates a single event into the asynchronous ring buffer.
     *
     * <p>This is the hot-path method. All operations are primitive field assignments
     * into pre-allocated {@link BatchRow} slots — zero heap allocations.
     * It only blocks (spin-waits) if the entire 65,536 element ring buffer is full.
     *
     * @param event      the event to persist; fields copied into batch slot
     * @param endOfBatch purely a hint, ignored in async design since the db-writer
     *                   dynamically batches available events.
     * @throws SQLException not thrown by async publish
     */
    public void accumulate(final FxMarketEvent event,
                            final boolean endOfBatch) throws SQLException {
        long w = writePointer;
        long r = readPointer;

        // Apply backpressure if the ring buffer is completely full
        while (w - r >= RING_SIZE) {
            Thread.onSpinWait();
            r = readPointer;
        }

        final BatchRow row = ringBuffer[(int) (w & MASK)];
        row.correlationId        = event.correlationId;
        row.ingressNanoTime      = event.ingressNanoTime;
        row.clientId             = event.clientId;
        row.clientTier           = event.clientTier;
        row.currencyPairCode     = event.currencyPairCode;
        row.side                 = event.side;
        row.notionalMinorUnits   = event.notionalMinorUnits;
        row.requestedPriceScaled = event.requestedPriceScaled;
        row.executedPriceScaled  = event.executedPriceScaled;
        row.spreadScaled         = event.spreadScaled;
        row.eventStatus          = event.eventStatus;
        row.t1ServAEntry         = event.t1ServAEntry;
        row.t1ServAExit          = event.t1ServAExit;
        row.t2ServBEntry         = event.t2ServBEntry;
        row.t2ServBExit          = event.t2ServBExit;
        row.t3ServCEntry         = event.t3ServCEntry;

        // Publish the event to the background thread
        writePointer = w + 1;
    }

    /**
     * The background thread loop that constantly reads from the ring buffer,
     * batches events up to MAX_BATCH, and executes JDBC inserts.
     */
    private void flushLoop() {
        while (running || readPointer < writePointer) {
            final long r = readPointer;
            final long w = writePointer;

            if (r == w) {
                if (!running) {
                    break;
                }
                Thread.onSpinWait();
                continue;
            }

            final long available = w - r;
            final int batchSize = (int) Math.min(available, MAX_BATCH);

            try {
                for (int i = 0; i < batchSize; i++) {
                    final BatchRow row = ringBuffer[(int) ((r + i) & MASK)];
                    insertStatement.setLong  (1,  row.correlationId);
                    insertStatement.setLong  (2,  row.ingressNanoTime);
                    insertStatement.setLong  (3,  row.clientId);
                    insertStatement.setInt   (4,  row.clientTier);
                    insertStatement.setLong  (5,  row.currencyPairCode);
                    insertStatement.setByte  (6,  row.side);
                    insertStatement.setLong  (7,  row.notionalMinorUnits);
                    insertStatement.setLong  (8,  row.requestedPriceScaled);
                    insertStatement.setLong  (9,  row.executedPriceScaled);
                    insertStatement.setLong  (10, row.spreadScaled);
                    insertStatement.setInt   (11, row.eventStatus);
                    insertStatement.setLong  (12, row.t1ServAEntry);
                    insertStatement.setLong  (13, row.t1ServAExit);
                    insertStatement.setLong  (14, row.t2ServBEntry);
                    insertStatement.setLong  (15, row.t2ServBExit);
                    insertStatement.setLong  (16, row.t3ServCEntry);
                    insertStatement.addBatch();
                }

                insertStatement.executeBatch();
                connection.commit();

                // Publish progress
                readPointer = r + batchSize;
            } catch (final SQLException e) {
                try {
                    connection.rollback();
                } catch (final SQLException rollbackEx) {
                    System.err.println("[db-writer] Rollback failed: " + rollbackEx.getMessage());
                }
                System.err.println("[db-writer] Batch insert failed: " + e.getMessage());
                
                // Advance read pointer to discard poisoned batch and prevent infinite crash loop
                readPointer = r + batchSize;
            }
        }
    }

    /**
     * Synchronously waits for the background thread to flush all pending events.
     * Useful for graceful shutdown and testing.
     *
     * @throws SQLException if the DB thread died
     */
    public void flush() throws SQLException {
        final long target = writePointer;
        while (readPointer < target && running) {
            Thread.onSpinWait();
            if (!dbThread.isAlive()) {
                throw new SQLException("DB Writer thread died prematurely");
            }
        }
    }

    /**
     * Returns the number of events currently waiting in the ring buffer.
     *
     * @return current pending count
     */
    public int batchCount() {
        return (int) (writePointer - readPointer);
    }

    /**
     * Initialises the database schema — creates the {@code fx_trades} table if absent.
     *
     * @throws SQLException if table creation fails
     */
    private void initSchema() throws SQLException {
        connection.setAutoCommit(false); // Manual transaction control for batch inserts
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS fx_trades ("
                            + "id                    BIGINT AUTO_INCREMENT PRIMARY KEY, "
                            + "correlation_id         BIGINT NOT NULL, "
                            + "ingress_nano           BIGINT NOT NULL, "
                            + "client_id              BIGINT NOT NULL, "
                            + "client_tier            INT NOT NULL, "
                            + "currency_pair_code     BIGINT NOT NULL, "
                            + "side                   TINYINT NOT NULL, "
                            + "notional_minor         BIGINT NOT NULL, "
                            + "requested_price_scaled BIGINT NOT NULL, "
                            + "executed_price_scaled  BIGINT NOT NULL, "
                            + "spread_scaled          BIGINT NOT NULL, "
                            + "event_status           INT NOT NULL, "
                            + "t1_serv_a_entry        BIGINT NOT NULL DEFAULT 0, "
                            + "t1_serv_a_exit         BIGINT NOT NULL DEFAULT 0, "
                            + "t2_serv_b_entry        BIGINT NOT NULL DEFAULT 0, "
                            + "t2_serv_b_exit         BIGINT NOT NULL DEFAULT 0, "
                            + "t3_serv_c_entry        BIGINT NOT NULL DEFAULT 0"
                            + ")"
            );
            connection.commit();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        running = false;
        try {
            dbThread.join(5000); // Wait up to 5 seconds for background thread to finish remaining writes
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            insertStatement.close();
            connection.close();
        } catch (final SQLException e) {
            System.err.println("[serv-c] Error closing persistence engine: " + e.getMessage());
        }
    }

    // ── Inner Data Carrier ────────────────────────────────────────────────────

    static final class BatchRow {
        long correlationId;
        long ingressNanoTime;
        long clientId;
        int  clientTier;
        long currencyPairCode;
        byte side;
        long notionalMinorUnits;
        long requestedPriceScaled;
        long executedPriceScaled;
        long spreadScaled;
        int  eventStatus;
        long t1ServAEntry;
        long t1ServAExit;
        long t2ServBEntry;
        long t2ServBExit;
        long t3ServCEntry;
    }
}
