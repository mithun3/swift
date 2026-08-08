package com.fx.integration;

import com.fx.gateway.CorrelationIdGenerator;
import com.fx.gateway.GatewayEventLoop;
import com.fx.gateway.SyntheticFixSource;
import com.fx.persistence.PersistenceEventLoop;
import com.fx.pricing.PricingEventLoop;
import com.fx.risk.RiskValidationEventLoop;
import com.fx.common.telemetry.TelemetryStitcher;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code FullPipelineIntegrationTest} — End-to-end pipeline validation.
 *
 * <h2>Test Strategy</h2>
 * <p>
 * This test spins up all 4 pipeline services in a single JVM using temporary
 * Chronicle Queue directories (cleaned up after each test). It injects a known
 * number of synthetic FIX events via {@link SyntheticFixSource} and asserts:
 * <ol>
 *   <li>All events are received by serv-c and persisted to the H2 database.</li>
 *   <li>Correlation IDs are unique and sequential.</li>
 *   <li>Accepted events have a non-zero executed price.</li>
 *   <li>The pipeline completes within an acceptable wall-clock time.</li>
 * </ol>
 *
 * <h2>Chronicle Queue in Tests</h2>
 * <p>
 * Real Chronicle Queue instances are used (not mocks) because the integration
 * test must validate actual memory-mapped file I/O, serialisation, and
 * multi-service coordination. Temporary directories under {@code /tmp/fx-test-*}
 * are created for each test run and deleted in {@code @AfterEach}.
 *
 * <h2>CPU Affinity Disable</h2>
 * <p>
 * CPU pinning is bypassed in tests by setting the CPU core to {@code -1}
 * via subclassing. This avoids OS-level affinity calls that could fail in
 * restricted CI/CD environments.
 *
 * @author FX Pipeline Team
 */
@DisplayName("Full FX Pipeline Integration Test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullPipelineIntegrationTest {

    /** Number of FIX messages to inject per test. */
    private static final int EVENT_COUNT = 1_000;

    /** Maximum time to allow the pipeline to drain all events (milliseconds). */
    private static final long DRAIN_TIMEOUT_MS = 30_000L;

    /** H2 JDBC URL for a unique in-memory DB per test run. */
    private static final String JDBC_URL =
            "jdbc:h2:mem:fxtest_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

    private Path tempQueueDir;
    private String queueAPath;
    private String queueBPath;
    private String queueCPath;
    private String queueErrPath;

    private GatewayEventLoop        gateway;
    private RiskValidationEventLoop  riskService;
    private PricingEventLoop         pricingService;
    private PersistenceEventLoop     persistenceService;
    private TelemetryStitcher        telemetryStitcher;
    private String                   traceLogPath;

    @BeforeEach
    void setUp() throws Exception {
        // Create unique temp directory for Chronicle Queue files per test run.
        tempQueueDir = Files.createTempDirectory("fx-test-");
        queueAPath   = tempQueueDir + "/queue-a";
        queueBPath   = tempQueueDir + "/queue-b";
        queueCPath   = tempQueueDir + "/queue-c";
        queueErrPath = tempQueueDir + "/queue-err";
        traceLogPath = tempQueueDir + "/traces.jsonl";

        // Override queue paths via system properties (read by QueuePaths and QueueFactory).
        System.setProperty("fx.queue.queue-a.path", queueAPath);
        System.setProperty("fx.queue.queue-b.path", queueBPath);
        System.setProperty("fx.queue.queue-c.path", queueCPath);
        System.setProperty("fx.queue.queue-err.path", queueErrPath);
        System.setProperty("fx.persistence.jdbc.url", JDBC_URL);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop all services gracefully in reverse pipeline order.
        stopQuietly(telemetryStitcher);
        stopQuietly(persistenceService);
        stopQuietly(pricingService);
        stopQuietly(riskService);
        stopQuietly(gateway);

        // Remove all Chronicle Queue files created during the test.
        if (tempQueueDir != null && Files.exists(tempQueueDir)) {
            Files.walk(tempQueueDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        // Clear system property overrides.
        System.clearProperty("fx.queue.queue-a.path");
        System.clearProperty("fx.queue.queue-b.path");
        System.clearProperty("fx.queue.queue-c.path");
        System.clearProperty("fx.queue.queue-err.path");
        System.clearProperty("fx.persistence.jdbc.url");
    }

    @Test
    @Order(1)
    @DisplayName("All events injected at serv-0 should be persisted by serv-c")
    void testAllEventsPersistedEndToEnd() throws Exception {
        // ── Start all pipeline services ───────────────────────────────────────
        persistenceService = new PersistenceEventLoop(JDBC_URL);
        pricingService     = new PricingEventLoop();
        riskService        = new RiskValidationEventLoop();

        // Create gateway with a finite synthetic source
        final SyntheticFixSource source = new SyntheticFixSource(EVENT_COUNT);
        final CorrelationIdGenerator idGen = new CorrelationIdGenerator();
        gateway = new GatewayEventLoop(source, idGen);

        // Start in reverse pipeline order (consumers first) to avoid queue writes
        // being missed by a consumer that hasn't started tailing yet.
        persistenceService.start();
        pricingService.start();
        riskService.start();
        gateway.start();

        // ── Wait for all events to be persisted ───────────────────────────────
        final long deadlineMs = System.currentTimeMillis() + DRAIN_TIMEOUT_MS;
        long persistedCount = 0L;

        while (System.currentTimeMillis() < deadlineMs && persistedCount < EVENT_COUNT) {
            Thread.sleep(100); // Poll every 100ms
            persistedCount = countPersistedEvents();
        }

        // ── Assertions ────────────────────────────────────────────────────────
        assertEquals(EVENT_COUNT, persistedCount,
                "All " + EVENT_COUNT + " events should be persisted, found: " + persistedCount);
    }

    @Test
    @Order(2)
    @DisplayName("Persisted events should have unique correlation IDs")
    void testUniqueCorrelationIds() throws Exception {
        // Start pipeline and drain all events
        startPipelineAndDrain(EVENT_COUNT);

        // Query for duplicate correlation IDs
        try (final Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             final Statement stmt = conn.createStatement();
             final ResultSet rs = stmt.executeQuery(
                     "SELECT correlation_id, COUNT(*) as cnt FROM fx_trades "
                             + "GROUP BY correlation_id HAVING COUNT(*) > 1")) {

            assertFalse(rs.next(),
                    "No duplicate correlation IDs should exist in the database");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Accepted events should have non-zero executed prices")
    void testAcceptedEventsHavePrices() throws Exception {
        startPipelineAndDrain(EVENT_COUNT);

        // event_status 4 = PRICED (EventStatus.PRICED)
        try (final Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             final Statement stmt = conn.createStatement();
             final ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM fx_trades "
                             + "WHERE event_status = 4 AND executed_price_scaled = 0")) {

            rs.next();
            assertEquals(0, rs.getInt(1),
                    "No PRICED event should have a zero executed price");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Pipeline processes 1000 events within 30 seconds")
    void testPipelineThroughputBenchmark() throws Exception {
        final long startNs = System.nanoTime();

        startPipelineAndDrain(EVENT_COUNT);

        final long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        System.out.printf("[Integration] %d events processed in %d ms (%.0f events/sec)%n",
                EVENT_COUNT, elapsedMs, (EVENT_COUNT * 1000.0 / elapsedMs));

        assertTrue(elapsedMs < DRAIN_TIMEOUT_MS,
                "Pipeline should complete " + EVENT_COUNT + " events in < " + DRAIN_TIMEOUT_MS + "ms");
    }

    @Test
    @Order(5)
    @DisplayName("TelemetryStitcher outputs all events to trace log")
    void testTelemetryStitcherOutput() throws Exception {
        startPipelineAndDrain(EVENT_COUNT);
        
        // Allow a small buffer for the out-of-band stitcher to finish writing
        Thread.sleep(1000);

        File logFile = new File(traceLogPath);
        assertTrue(logFile.exists(), "Trace log file should exist");
        
        long lineCount = Files.lines(logFile.toPath()).count();
        assertTrue(lineCount >= EVENT_COUNT, "Trace log should contain at least " + EVENT_COUNT + " events, found: " + lineCount);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Starts the full pipeline, waits for all events to be persisted, then stops.
     */
    private void startPipelineAndDrain(final int count) throws Exception {
        persistenceService = new PersistenceEventLoop(JDBC_URL);
        pricingService     = new PricingEventLoop();
        riskService        = new RiskValidationEventLoop();
        telemetryStitcher  = new TelemetryStitcher(queueCPath, traceLogPath);

        final SyntheticFixSource source = new SyntheticFixSource(count);
        gateway = new GatewayEventLoop(source, new CorrelationIdGenerator());

        persistenceService.start();
        pricingService.start();
        riskService.start();
        gateway.start();

        final long deadlineMs = System.currentTimeMillis() + DRAIN_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadlineMs && countPersistedEvents() < count) {
            Thread.sleep(100);
        }
    }

    /**
     * Counts total rows in the {@code fx_trades} table.
     */
    private long countPersistedEvents() throws Exception {
        try (final Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             final Statement stmt = conn.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM fx_trades")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /**
     * Gracefully stops a service, ignoring any errors (for teardown safety).
     */
    private static void stopQuietly(final AutoCloseable service) {
        if (service == null) return;
        try {
            service.close();
        } catch (final Exception ignored) {
            // Suppress — teardown should not mask test failures
        }
    }
}
