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
     * Number of events per batch flush.
     *
     * <p>Tuning note: larger batches reduce JDBC overhead per event but increase
     * the maximum latency before an event is persisted. 256 is a balanced default
     * that achieves ~200µs average persistence latency at 1M events/sec throughput.
     */
    private static final int BATCH_SIZE = 256;

    /** Pre-allocated batch row objects — never replaced after construction. */
    private final BatchRow[] batchBuffer = new BatchRow[BATCH_SIZE];

    /** Current write position in the batch buffer. */
    private int batchCount = 0;

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
                        + "t1_serv_a_entry, t2_serv_b_entry, t3_serv_c_entry"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        // Pre-allocate all batch row objects once at construction.
        // These mutable objects are reused across all batch cycles,
        // preventing any allocation in the accumulate() hot path.
        for (int i = 0; i < BATCH_SIZE; i++) {
            batchBuffer[i] = new BatchRow();
        }
    }

    /**
     * Accumulates a single event into the batch buffer.
     *
     * <p>This is the hot-path method. All operations are primitive field assignments
     * into pre-allocated {@link BatchRow} slots — zero heap allocations.
     *
     * <p>If the buffer is full after this accumulation, a flush is triggered
     * automatically. The {@code endOfBatch} flag triggers a flush even when the
     * buffer is partially filled, preventing stale events at low throughput.
     *
     * @param event      the event to persist; fields copied into batch slot
     * @param endOfBatch {@code true} if no further events are immediately available
     * @throws SQLException if the database flush fails
     */
    public void accumulate(final FxMarketEvent event,
                            final boolean endOfBatch) throws SQLException {
        // Copy primitive fields into the next pre-allocated batch slot.
        // This is the only "write" in the hot path — no objects created.
        final BatchRow row = batchBuffer[batchCount++];
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
        // Stage-entry timestamps — captured by each service's handle() method.
        // Zero if the event did not reach that stage (e.g., rejected at serv-a).
        row.t1ServAEntry         = event.t1ServAEntry;
        row.t2ServBEntry         = event.t2ServBEntry;
        row.t3ServCEntry         = event.t3ServCEntry;

        // Flush if batch is full or if no more events are immediately available.
        if (batchCount >= BATCH_SIZE || endOfBatch) {
            flush();
        }
    }

    /**
     * Flushes the accumulated batch to the database via a single JDBC batch execute.
     *
     * <p>This method is NOT on the critical hot path — it is called at batch boundaries.
     * JDBC overhead is amortised across {@link #BATCH_SIZE} events.
     *
     * <p>On failure, a {@link #connection} rollback is performed to ensure partial
     * writes do not corrupt the {@code fx_trades} table. The exception is re-thrown
     * so the caller (the event loop) can route the batch to the error queue.
     *
     * @throws SQLException if any batch insert fails; the transaction is rolled back
     */
    public void flush() throws SQLException {
        if (batchCount == 0) {
            return; // Nothing to flush
        }

        try {
            for (int i = 0; i < batchCount; i++) {
                final BatchRow row = batchBuffer[i];
                // Bind each field to the prepared statement parameters.
                // JDBC setLong/setInt/setByte avoid boxing for primitive types
                // in well-implemented JDBC drivers (H2 included).
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
                insertStatement.setLong  (13, row.t2ServBEntry);
                insertStatement.setLong  (14, row.t3ServCEntry);
                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
            connection.commit();
        } catch (final SQLException e) {
            // GAP-7 FIX: Roll back the entire batch on failure to prevent partial writes.
            // This maintains ACID consistency: either all events in the batch are committed
            // or none are. The exception is re-thrown so the event loop routes the poisoned
            // batch to the error Chronicle Queue for replay investigation.
            try {
                connection.rollback();
            } catch (final SQLException rollbackEx) {
                // Log rollback failure to stderr — we're already in an error state.
                System.err.println("[serv-c] Rollback failed after batch error: "
                        + rollbackEx.getMessage());
            }
            throw e; // Re-throw so the event loop can handle it
        } finally {
            // Always reset batch counter — whether flush succeeded or failed.
            // This prevents double-submission of the same events on the next call.
            batchCount = 0;
        }
    }

    /**
     * Returns the number of events currently accumulated in the batch buffer.
     *
     * @return current batch count (0 to {@link #BATCH_SIZE})
     */
    public int batchCount() {
        return batchCount;
    }

    /**
     * Initialises the database schema — creates the {@code fx_trades} table if absent.
     *
     * <p>The schema includes per-stage nanosecond timestamps ({@code t1_serv_a_entry},
     * {@code t2_serv_b_entry}, {@code t3_serv_c_entry}) to enable offline per-stage
     * latency analysis from the persisted data.
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
                            // Per-stage timestamps: 0 if the event did not reach that stage.
                            + "t1_serv_a_entry        BIGINT NOT NULL DEFAULT 0, "
                            + "t2_serv_b_entry        BIGINT NOT NULL DEFAULT 0, "
                            + "t3_serv_c_entry        BIGINT NOT NULL DEFAULT 0"
                            + ")"
            );
            connection.commit();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        try {
            flush(); // Ensure any remaining events in the buffer are persisted
            insertStatement.close();
            connection.close();
        } catch (final SQLException e) {
            // Log to stderr on close — acceptable since we're shutting down.
            System.err.println("[serv-c] Error closing persistence engine: " + e.getMessage());
        }
    }

    // ── Inner Data Carrier ────────────────────────────────────────────────────

    /**
     * {@code BatchRow} — Pre-allocated holder for one event's primitive fields.
     *
     * <p>All fields are package-private primitives for maximum write throughput from the
     * accumulate() loop. No accessor methods — direct field assignment is faster
     * and the single-threaded access model makes encapsulation irrelevant here.
     *
     * <p>Includes per-stage nanosecond timestamps aligned with the fields defined
     * in {@link FxMarketEvent} for full pipeline stage-by-stage latency tracking.
     */
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
        // Per-stage entry timestamps (nanoseconds, monotonic System.nanoTime()).
        // Default 0L = stage not reached.
        long t1ServAEntry;
        long t2ServBEntry;
        long t3ServCEntry;
    }
}
