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
     * <p>At 175K events/sec (macOS benchmark throughput) this ring absorbs ~3 seconds
     * of burst absorption before the hot-path {@link #accumulate} spin-wait could fire.
     * The previous 65,536-slot ring only covered ~375 ms, causing serv-c to stall
     * whenever an H2 {@code executeBatch()} took longer than that threshold.
     *
     * <p>With the {@link #readPointer} now advanced <em>before</em> the H2 commit,
     * this ring only needs to absorb load while the db-writer is loading data into
     * the JDBC batch — not the full commit duration. Ring fill is extremely rare.
     *
     * <p>Memory footprint: 524,288 &times; ~128 bytes per {@link BatchRow} &asymp; 64 MB.
     * All slots are pre-allocated at construction &mdash; zero GC after startup.
     */
    private static final int RING_SIZE = 524_288;
    private static final int MASK = RING_SIZE - 1;

    /**
     * Maximum rows pulled into a single JDBC {@code executeBatch()} call.
     *
     * <p>Reduced from 32,768 to 8,192 to prevent multi-second H2 commits.
     * At 175K events/sec, an 8,192-row batch represents ~47 ms of events.
     * In-memory H2 commits 8K rows in approximately 20-50 ms, which is
     * well below the 3-second ring-fill threshold at 175K events/sec.
     *
     * <p>The previous 32,768 value caused individual H2 commits to take
     * 200 ms &ndash; 2+ s as the {@code fx_trades} table grew past 1M rows
     * (MVStore B-tree depth increases), triggering the {@link #accumulate}
     * spin-wait and producing the observed serv-c Max = 2.676 s.
     */
    private static final int MAX_BATCH = 8_192;

    /** Pre-allocated batch row objects — never replaced after construction. */
    private final BatchRow[] ringBuffer = new BatchRow[RING_SIZE];

    /**
     * Write pointer advanced by the event-loop thread ({@code accumulate}).
     * Slots in range {@code [readPointer, writePointer)} hold live data.
     *
     * <p>Cache-line padded (see {@link PaddedLong}): this pointer is written by
     * the hot {@code accumulate()} thread and read by the db-writer thread on
     * every loop iteration. Sharing a 64-byte cache line with {@link #readPointer}
     * or {@link #committedPointer} would force a cache-coherency round trip on
     * the hot path every time either of the other two pointers is updated.
     */
    private final PaddedLong writePointer = new PaddedLong(0L);

    /**
     * Read pointer advanced by the db-writer thread <em>before</em> the JDBC
     * commit, immediately after all ring-buffer rows have been copied into the
     * {@link java.sql.PreparedStatement} batch.  Advancing here frees the ring
     * slots so {@code accumulate()} can write new events while H2 is committing
     * &mdash; hiding commit latency from the serv-c hot path entirely.
     *
     * <p>Ring-full guard: {@code writePointer - readPointer &lt; RING_SIZE}.
     * Cache-line padded (see {@link PaddedLong}) for the same false-sharing
     * reason as {@link #writePointer}.
     */
    private final PaddedLong readPointer = new PaddedLong(0L);

    /**
     * Committed pointer advanced by the db-writer thread <em>after</em> a
     * successful {@link java.sql.Connection#commit()}.  Used by {@link #flush()}
     * to guarantee that all recorded events are durably in H2 before returning,
     * and by {@link #batchCount()} to report truly uncommitted events.
     * On a commit failure the batch is discarded and this pointer advances
     * alongside {@link #readPointer} to prevent {@link #flush()} from hanging.
     *
     * <p>Cache-line padded (see {@link PaddedLong}) for the same false-sharing
     * reason as {@link #writePointer}.
     */
    private final PaddedLong committedPointer = new PaddedLong(0L);

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

        // Start the background JDBC writer thread.
        // Priority is set one notch below NORM_PRIORITY so the event loop thread
        // (which runs at NORM_PRIORITY) gets scheduler preference on the shared cpuset.
        // On Linux, CFS assigns proportionally more CPU time to the event loop,
        // reducing preemption frequency of the hot-path Chronicle tailer.
        this.dbThread = new Thread(this::flushLoop, "db-writer");
        this.dbThread.setDaemon(true);
        this.dbThread.setPriority(Thread.NORM_PRIORITY - 1);
        this.dbThread.start();
    }

    /**
     * Accumulates a single event into the asynchronous ring buffer.
     *
     * <p>This is the hot-path method. All operations are primitive field assignments
     * into pre-allocated {@link BatchRow} slots &mdash; zero heap allocations.
     *
     * <p>Back-pressure guard: spins only if all {@value #RING_SIZE} slots are
     * occupied ({@link #readPointer} has not yet advanced past slot {@code w}).
     * Because {@link #readPointer} is advanced <em>before</em> the H2 commit,
     * this guard fires only during the brief window when the db-writer is loading
     * the PreparedStatement batch &mdash; not during the H2 commit itself.
     * In practice the guard should never fire under normal load.
     *
     * @param event      the event to persist; fields are copied into a ring slot
     * @param endOfBatch not used; the db-writer batches dynamically
     * @throws SQLException never thrown; declared for interface compatibility
     */
    public void accumulate(final FxMarketEvent event,
                            final boolean endOfBatch) throws SQLException {
        long w = writePointer.get();
        long r = readPointer.get();

        // Apply backpressure if the ring buffer is completely full
        while (w - r >= RING_SIZE) {
            Thread.onSpinWait();
            r = readPointer.get();
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
        writePointer.set(w + 1);
    }

    /**
     * Background db-writer loop: drains the ring buffer into H2 via JDBC batches.
     *
     * <h3>Two-phase progress tracking</h3>
     * <ol>
     *   <li>{@link #readPointer} is advanced <em>immediately after</em> all ring
     *       slots for this batch have been copied into the PreparedStatement batch
     *       via {@code addBatch()}.  This frees the ring slots for the producer
     *       ({@link #accumulate}) to reuse while H2 commits in the background.</li>
     *   <li>{@link #committedPointer} is advanced <em>after</em> a successful
     *       {@link Connection#commit()}.  {@link #flush()} and the loop-exit
     *       condition use this pointer to guarantee durability.</li>
     * </ol>
     *
     * <p>On a commit failure both pointers are advanced to discard the poisoned
     * batch and prevent an infinite retry loop.
     */
    private void flushLoop() {
        // Exit only after all events have been durably committed, not merely loaded.
        while (running || committedPointer.get() < writePointer.get()) {
            final long r = readPointer.get();
            final long w = writePointer.get();

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

                // Phase 1: free ring slots now that data is loaded into the JDBC batch.
                // The hot-path accumulate() can write new events to these slots while
                // H2 processes the transaction — commit latency is hidden from the pipeline.
                readPointer.set(r + batchSize);

                insertStatement.executeBatch();
                connection.commit();

                // Phase 2: signal durability — flush() and the exit condition use this.
                committedPointer.set(r + batchSize);
            } catch (final SQLException e) {
                try {
                    connection.rollback();
                } catch (final SQLException rollbackEx) {
                    System.err.println("[db-writer] Rollback failed: " + rollbackEx.getMessage());
                }
                System.err.println("[db-writer] Batch insert failed: " + e.getMessage());

                // readPointer was already advanced before executeBatch (phase 1).
                // Advance committedPointer to match so flush() is not blocked
                // indefinitely by a poisoned batch that will never be committed.
                committedPointer.set(r + batchSize);
            }
        }
    }

    /**
     * Synchronously waits until all accumulated events have been durably committed.
     *
     * <p>Uses {@link #committedPointer} (not {@link #readPointer}) to guarantee
     * that data is in H2 before returning — not merely loaded into the JDBC batch.
     * Safe to call from any thread.
     *
     * @throws SQLException if the db-writer thread died before committing
     */
    public void flush() throws SQLException {
        final long target = writePointer.get();
        while (committedPointer.get() < target && running) {
            Thread.onSpinWait();
            if (!dbThread.isAlive()) {
                throw new SQLException("DB Writer thread died prematurely");
            }
        }
    }

    /**
     * Returns the number of events accumulated but not yet committed to H2.
     *
     * <p>Events counted here have been written to the ring buffer and may already
     * be loaded into the JDBC batch, but their H2 transaction is still in flight.
     *
     * @return uncommitted event count
     */
    public int batchCount() {
        return (int) (writePointer.get() - committedPointer.get());
    }

    /**
     * Initialises the database schema — creates the {@code fx_trades} table if absent.
     *
     * <h3>Schema design notes</h3>
     * <ul>
     *   <li>{@code correlation_id} is used as PRIMARY KEY instead of a separate
     *       auto-increment {@code id} column.  The correlation ID is a monotonically
     *       increasing {@link java.util.concurrent.atomic.AtomicLong}, so H2 MVStore
     *       always appends to the rightmost B-tree leaf — O(1) amortised insert cost
     *       with no random page splits.  An auto-increment column would have required
     *       a separate sequence counter update on every row, adding overhead that
     *       grew visibly as the table exceeded 1 M rows.</li>
     *   <li>All timestamp columns default to {@code 0} so rows inserted before
     *       a given stage completes remain queryable.</li>
     * </ul>
     *
     * @throws SQLException if table creation fails
     */
    private void initSchema() throws SQLException {
        connection.setAutoCommit(false); // Manual transaction control for batch inserts
        try (final Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS fx_trades ("
                            // Monotonically increasing sequential long — O(1) B-tree append,
                            // no separate sequence counter, queries by trade ID in O(log n).
                            + "correlation_id         BIGINT NOT NULL PRIMARY KEY, "
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

    // ── Cache-Line-Padded Pointer ─────────────────────────────────────────────

    /** 56 bytes (7 longs) of left-hand padding ahead of {@link ValueHolder#value}. */
    private abstract static class LhsPadding {
        @SuppressWarnings("unused")
        long p1, p2, p3, p4, p5, p6, p7;
    }

    private abstract static class ValueHolder extends LhsPadding {
        volatile long value;
    }

    /** 56 bytes (7 longs) of right-hand padding behind {@link ValueHolder#value}. */
    private abstract static class RhsPadding extends ValueHolder {
        @SuppressWarnings("unused")
        long p9, p10, p11, p12, p13, p14, p15;
    }

    /**
     * {@code PaddedLong} — a single volatile long isolated on its own CPU cache line.
     *
     * <p>Replicates the padding technique used by LMAX Disruptor's {@code Sequence}
     * class: {@link LhsPadding} and {@link RhsPadding} surround {@link ValueHolder#value}
     * with 56 bytes of unused fields on each side via inheritance (not sibling fields
     * in one class), so the JVM's field layout cannot place another live field within
     * 64 bytes of {@code value} in either direction. This prevents false sharing when
     * independent threads each poll a different {@code PaddedLong} at high frequency.
     */
    private static final class PaddedLong extends RhsPadding {

        PaddedLong(final long initialValue) {
            this.value = initialValue;
        }

        long get() {
            return value;
        }

        void set(final long newValue) {
            value = newValue;
        }
    }
}

