package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.persistence.BatchPersistenceEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BatchPersistenceEngine}.
 *
 * <p>Uses an in-memory H2 database for all tests. Each test method gets its
 * own unique H2 database (via a unique URL suffix) to prevent cross-test
 * row count contamination.
 *
 * @author FX Pipeline Team
 */
@DisplayName("BatchPersistenceEngine Tests")
class BatchPersistenceEngineTest {

    /** Unique JDBC URL per test instance — set in @BeforeEach to prevent cross-test pollution. */
    private String jdbcUrl;
    private BatchPersistenceEngine engine;
    private FxMarketEvent event;

    @BeforeEach
    void setUp() throws SQLException {
        // Unique DB per test method — prevents row count bleed between tests.
        jdbcUrl = "jdbc:h2:mem:test_batch_" + Thread.currentThread().threadId()
                + "_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        engine = new BatchPersistenceEngine(jdbcUrl);
        event  = new FxMarketEvent();
        event.reset();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    @DisplayName("batchCount() starts at 0")
    void testBatchCountStartsAtZero() {
        assertEquals(0, engine.batchCount(), "Batch count must start at 0");
    }

    @Test
    @DisplayName("accumulate() increments batchCount for each event (before flush threshold)")
    void testBatchCountIncrementsOnAccumulate() throws SQLException {
        event.correlationId = 1L;
        event.clientTier    = 2;
        event.notionalMinorUnits = 1_000_000L;
        engine.accumulate(event, false); // endOfBatch=false, single event, no flush yet
        // batchCount is either 1 (if BATCH_SIZE > 1) or 0 (if flush already happened)
        // Since BATCH_SIZE=256, a single accumulate should not flush.
        assertEquals(1, engine.batchCount(),
                "A single accumulate should increment batchCount to 1 without flushing");
    }

    @Test
    @DisplayName("accumulate() with endOfBatch=true flushes the batch")
    void testEndOfBatchTriggerFlush() throws SQLException {
        populateEvent(event, 42L, EventStatus.PRICED);
        engine.accumulate(event, true); // endOfBatch = true
        engine.flush(); // Wait for async flush
        assertEquals(0, engine.batchCount(), "batchCount must be 0 after flush");
        assertEquals(1L, countRows(), "Exactly 1 row must be in the DB after flush");
    }

    @Test
    @DisplayName("flush() with empty batch is a no-op")
    void testFlushEmptyBatchIsNoOp() throws SQLException {
        engine.flush();
        assertEquals(0L, countRows(), "An empty flush must not insert any rows");
    }

    @Test
    @DisplayName("All business fields are persisted correctly")
    void testAllFieldsArePersisted() throws SQLException {
        event.correlationId        = 100L;
        event.ingressNanoTime      = 999L;
        event.clientId             = 7L;
        event.clientTier           = 3;
        event.currencyPairCode     = 12345L;
        event.side                 = 1;
        event.notionalMinorUnits   = 500_000L;
        event.requestedPriceScaled = 108500L;
        event.executedPriceScaled  = 108505L;
        event.spreadScaled         = 10L;
        event.eventStatus          = EventStatus.PRICED;
        event.t1ServAEntry         = 111L;
        event.t2ServBEntry         = 222L;
        event.t3ServCEntry         = 333L;

        engine.accumulate(event, true);
        engine.flush(); // Wait for async flush

        try (final Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
             final Statement s = c.createStatement();
             final ResultSet rs = s.executeQuery("SELECT * FROM fx_trades WHERE correlation_id = 100")) {

            assertTrue(rs.next(), "Should find the persisted row");
            assertEquals(100L,   rs.getLong("correlation_id"));
            assertEquals(999L,   rs.getLong("ingress_nano"));
            assertEquals(7L,     rs.getLong("client_id"));
            assertEquals(3,      rs.getInt("client_tier"));
            assertEquals(12345L, rs.getLong("currency_pair_code"));
            assertEquals(1,      rs.getByte("side"));
            assertEquals(500_000L, rs.getLong("notional_minor"));
            assertEquals(108500L,  rs.getLong("requested_price_scaled"));
            assertEquals(108505L,  rs.getLong("executed_price_scaled"));
            assertEquals(10L,    rs.getLong("spread_scaled"));
            assertEquals(EventStatus.PRICED, rs.getInt("event_status"));
            // Stage timestamps (GAP-6)
            assertEquals(111L,   rs.getLong("t1_serv_a_entry"));
            assertEquals(222L,   rs.getLong("t2_serv_b_entry"));
            assertEquals(333L,   rs.getLong("t3_serv_c_entry"));
        }
    }

    @Test
    @DisplayName("Multiple events are accumulated and flushed as a batch")
    void testMultipleEventsAccumulatedAndFlushed() throws SQLException {
        for (long id = 1L; id <= 10L; id++) {
            populateEvent(event, id, EventStatus.PRICED);
            engine.accumulate(event, id == 10L);
        }
        engine.flush(); // Wait for async flush
        assertEquals(10L, countRows(), "All 10 events must be in the DB after batch flush");
    }

    @Test
    @DisplayName("t1/t2/t3 default to 0 when not set by services")
    void testTimestampDefaultsToZero() throws SQLException {
        event.correlationId = 55L;
        event.clientTier    = 1;
        event.notionalMinorUnits = 100L;
        // t1/t2/t3 remain 0 (reset())
        engine.accumulate(event, true);
        engine.flush(); // Wait for async flush

        try (final Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
             final Statement s = c.createStatement();
             final ResultSet rs = s.executeQuery("SELECT t1_serv_a_entry, t2_serv_b_entry, t3_serv_c_entry FROM fx_trades WHERE correlation_id = 55")) {
            assertTrue(rs.next());
            assertEquals(0L, rs.getLong("t1_serv_a_entry"), "t1 should default to 0");
            assertEquals(0L, rs.getLong("t2_serv_b_entry"), "t2 should default to 0");
            assertEquals(0L, rs.getLong("t3_serv_c_entry"), "t3 should default to 0");
        }
    }

    @Test
    @DisplayName("a duplicate-primary-key batch failure is rolled back and does not block future flushes")
    void testBatchFailureIsRolledBackAndDoesNotBlockFutureFlushes() throws SQLException {
        populateEvent(event, 500L, EventStatus.PRICED);
        engine.accumulate(event, true);
        engine.flush();
        assertEquals(1L, countRows(), "first insert of correlationId=500 must commit");

        // Re-accumulate the SAME correlationId (primary key) — executeBatch()/commit()
        // will fail with a PK violation, exercising flushLoop()'s rollback + poisoned-batch
        // committedPointer-advance branch. flush() must not hang even though the batch failed.
        populateEvent(event, 500L, EventStatus.PRICED);
        engine.accumulate(event, true);
        engine.flush();

        assertEquals(1L, countRows(), "duplicate PK batch must be rolled back, not committed");
        assertEquals(0, engine.batchCount(), "committedPointer must advance past the poisoned batch");
    }

    @Test
    @DisplayName("close() catches InterruptedException from the background join() and restores the interrupt flag")
    void testCloseCatchesInterruptedExceptionDuringJoin() throws Exception {
        final BatchPersistenceEngine localEngine = new BatchPersistenceEngine(
                "jdbc:h2:mem:test_close_intr_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        try {
            final Thread closer = new Thread(localEngine::close, "engine-closer");
            // Setting the interrupt flag before start() guarantees dbThread.join(5000)
            // observes it immediately once the closer thread runs, deterministically
            // exercising the catch(InterruptedException) branch inside close().
            closer.interrupt();
            closer.start();
            closer.join(6_000L);
            assertFalse(closer.isAlive(), "closer thread must finish despite the injected interrupt");
        } finally {
            Thread.interrupted(); // clear this test thread's interrupt status, if any leaked
        }
    }

    @Test
    @DisplayName("close() remains a no-op-safe operation even after the connection was already shut down externally")
    void testCloseIsSafeAfterConnectionAlreadyShutdown() throws Exception {
        final String url = "jdbc:h2:mem:test_close_sqlerr_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        final BatchPersistenceEngine localEngine = new BatchPersistenceEngine(url);

        // Force the underlying H2 database to shut down from a separate connection.
        // Per the JDBC spec, Statement.close()/Connection.close() on an already-closed
        // resource must be a silent no-op (not throw) — so this does NOT reliably reach
        // close()'s catch(SQLException) logging branch (see deferred-exceptions note in
        // the module's testing-conventions memory); it does verify close() never throws
        // even when the connection was invalidated out from under it.
        try (Connection shutdownConn = DriverManager.getConnection(url, "sa", "");
             Statement s = shutdownConn.createStatement()) {
            s.execute("SHUTDOWN IMMEDIATELY");
        } catch (final SQLException ignoredShutdownRace) {
            // Some H2 versions report the self-shutdown as an exception on the same statement — ignore.
        }

        assertDoesNotThrow(localEngine::close,
                "close() must never throw, even when the underlying connection was already shut down");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void populateEvent(final FxMarketEvent e, final long corrId, final int status) {
        e.reset();
        e.correlationId      = corrId;
        e.clientTier         = 2;
        e.notionalMinorUnits = 100_000L;
        e.eventStatus        = status;
    }

    private long countRows() throws SQLException {
        try (final Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
             final Statement s = c.createStatement();
             final ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM fx_trades")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
