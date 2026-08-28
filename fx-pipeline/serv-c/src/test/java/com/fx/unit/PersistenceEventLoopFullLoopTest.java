package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.common.telemetry.TelemetryRecorder;
import com.fx.persistence.PersistenceEventLoop;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full end-to-end characterization tests for {@link PersistenceEventLoop}, the terminal
 * Disruptor-analog "EventProcessor" that tails {@code queue-c} (produced by serv-b) and
 * persists every event to H2 via {@code BatchPersistenceEngine}. These tests run the real
 * {@code handle()} path via {@code run()} against a real temp-dir Chronicle Queue for
 * {@code queue-c} — {@code fx.queue.queue-c.path} is resolved dynamically per call by
 * {@code QueueFactory.createWithOverride}, so it is safe to override per test (unlike the
 * truly static-final {@code QueuePaths.QUEUE_ERR}, which this loop's error writer targets
 * but which the terminal-persist happy path never touches).
 *
 * <p>Unlike the pre-existing {@code PersistenceEventLoopTest} (which only exercises
 * {@link com.fx.persistence.BatchPersistenceEngine} directly, never the real event-loop
 * {@code handle()} dispatch), these tests drive events through the actual queue-c tailer.
 *
 * @author FX Pipeline Team
 */
@DisplayName("PersistenceEventLoop Full-Loop Characterization Tests")
class PersistenceEventLoopFullLoopTest {

    @TempDir
    Path tempDir;

    private PersistenceEventLoop loop;
    private String jdbcUrl;
    private TelemetryRecorder e2eRecorder;
    private TelemetryRecorder queueCRecorder;
    private TelemetryRecorder servCRecorder;

    @BeforeEach
    void configureQueueOverride() {
        System.setProperty("fx.queue.queue-c.path", tempDir.resolve("queue-c").toString());
        jdbcUrl = "jdbc:h2:mem:test_loop_fullloop_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
    }

    @AfterEach
    void tearDown() {
        if (loop != null) {
            loop.close();
        }
        if (e2eRecorder != null) {
            e2eRecorder.close();
        }
        if (queueCRecorder != null) {
            queueCRecorder.close();
        }
        if (servCRecorder != null) {
            servCRecorder.close();
        }
        System.clearProperty("fx.queue.queue-c.path");
    }

    private void writeToQueueC(final String queueCPath, final FxMarketEvent event) throws Exception {
        Files.createDirectories(Path.of(queueCPath));
        try (ChronicleQueue writerQueue = QueueFactory.create(queueCPath);
             ExcerptAppender appender = writerQueue.createAppender()) {
            appender.writeDocument(event);
        }
    }

    /** Bounded poll for a persisted row — fails fast on regression instead of hanging the suite. */
    private long awaitRowCount(final long correlationId, final long timeoutMillis) throws SQLException {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (true) {
            try (Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT COUNT(*) FROM fx_trades WHERE correlation_id = " + correlationId)) {
                if (rs.next() && rs.getLong(1) > 0) {
                    return rs.getLong(1);
                }
            }
            if (System.nanoTime() > deadlineNanos) {
                fail("No row for correlationId=" + correlationId + " appeared within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }

    @Test
    @DisplayName("run() persists a PRICED event tailed from queue-c to the database")
    void testFullLoopPersistsPricedEventToDatabase() throws Exception {
        final String queueCPath = tempDir.resolve("queue-c").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 400L;
        outbound.eventStatus = EventStatus.PRICED;
        outbound.clientTier = 2;
        outbound.notionalMinorUnits = 250_000L;
        outbound.t2ServBExit = System.nanoTime();
        writeToQueueC(queueCPath, outbound);

        loop = new PersistenceEventLoop(jdbcUrl);
        loop.start();

        assertEquals(1L, awaitRowCount(400L, 5_000L), "the tailed event must be persisted to fx_trades");

        loop.stop();
        loop.awaitTermination();
        assertFalse(loop.isRunning(), "loop must report not-running once its thread has terminated");
    }

    @Test
    @DisplayName("run() with telemetry recorders records queue-c wait time and serv-c/e2e latencies without error")
    void testFullLoopWithTelemetryRecordersDoesNotThrow() throws Exception {
        final String queueCPath = tempDir.resolve("queue-c").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 401L;
        outbound.eventStatus = EventStatus.PRICED;
        outbound.clientTier = 1;
        outbound.notionalMinorUnits = 100_000L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.t2ServBExit = System.nanoTime();
        writeToQueueC(queueCPath, outbound);

        e2eRecorder = new TelemetryRecorder(
                new File(tempDir.resolve("e2e.hlog").toString()), 10_000_000_000L, 1_000L);
        queueCRecorder = new TelemetryRecorder(
                new File(tempDir.resolve("queue-c.hlog").toString()), 10_000_000_000L, 1_000L);
        servCRecorder = new TelemetryRecorder(
                new File(tempDir.resolve("serv-c.hlog").toString()), 10_000_000_000L, 1_000L);

        loop = new PersistenceEventLoop(jdbcUrl, e2eRecorder, queueCRecorder, servCRecorder);
        loop.start();

        assertEquals(1L, awaitRowCount(401L, 5_000L),
                "the tailed event must still be persisted when telemetry recorders are attached");

        loop.stop();
        loop.awaitTermination();
    }

    @Test
    @DisplayName("single-arg constructor (no telemetry) starts a working loop that persists events")
    void testSingleArgConstructorPersistsEvents() throws Exception {
        final String queueCPath = tempDir.resolve("queue-c").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 402L;
        outbound.eventStatus = EventStatus.PRICED;
        outbound.clientTier = 3;
        outbound.notionalMinorUnits = 50_000L;
        writeToQueueC(queueCPath, outbound);

        // Exercises the single-arg constructor, which delegates to
        // PersistenceEventLoop(jdbcUrl, null, null, null).
        loop = new PersistenceEventLoop(jdbcUrl);
        loop.start();

        assertEquals(1L, awaitRowCount(402L, 5_000L));

        loop.stop();
        loop.awaitTermination();
    }
}
