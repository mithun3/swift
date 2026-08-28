package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.common.telemetry.TelemetryRecorder;
import com.fx.risk.CreditCheckEngine;
import com.fx.risk.RiskValidationEventLoop;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full end-to-end characterization tests for {@link RiskValidationEventLoop}, the
 * Disruptor-analog "EventProcessor" that tails queue-a (produced by serv-0) and
 * appends to queue-b (consumed by serv-b). These tests run the real {@code run()}
 * loop against real temp-dir Chronicle Queues, matching the convention established
 * by {@code GatewayEventLoopFullLoopTest} in serv-0 — {@code fx.queue.queue-a.path}
 * and {@code fx.queue.queue-b.path} are resolved dynamically per call by
 * {@code QueueFactory.createWithOverride}, so they are safe to override per test
 * (unlike the truly static-final {@code QueuePaths.QUEUE_ERR}).
 *
 * @author FX Pipeline Team
 */
@DisplayName("RiskValidationEventLoop Full-Loop Characterization Tests")
class RiskValidationEventLoopTest {

    @TempDir
    Path tempDir;

    private RiskValidationEventLoop loop;
    private TelemetryRecorder queueARecorder;
    private TelemetryRecorder servARecorder;

    @BeforeEach
    void configureQueueOverrides() {
        System.setProperty("fx.queue.queue-a.path", tempDir.resolve("queue-a").toString());
        System.setProperty("fx.queue.queue-b.path", tempDir.resolve("queue-b").toString());
    }

    @AfterEach
    void tearDown() {
        if (loop != null) {
            loop.close();
        }
        if (queueARecorder != null) {
            queueARecorder.close();
        }
        if (servARecorder != null) {
            servARecorder.close();
        }
        System.clearProperty("fx.queue.queue-a.path");
        System.clearProperty("fx.queue.queue-b.path");
    }

    /** Bounded poll — fails fast on regression instead of hanging the suite. */
    private static void awaitNextEvent(final ExcerptTailer tailer,
                                        final FxMarketEvent event,
                                        final long timeoutMillis) {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (!tailer.readDocument(event)) {
            if (System.nanoTime() > deadlineNanos) {
                fail("No event appeared within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }

    private void writeToQueueA(final String queueAPath, final FxMarketEvent event) {
        try (ChronicleQueue writerQueue = QueueFactory.create(queueAPath);
             ExcerptAppender appender = writerQueue.createAppender()) {
            appender.writeDocument(event);
        }
    }

    @Test
    @DisplayName("run() accepts an in-limit order and forwards ACCEPTED status + resolved tier to queue-b")
    void testFullLoopAcceptsInLimitOrder() throws Exception {
        final String queueAPath = System.getProperty("fx.queue.queue-a.path");
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");

        final long clientId = 7L; // resolveTier(7) is deterministic and within [1,3]
        final int expectedTier = CreditCheckEngine.resolveTier(clientId);

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 99L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.clientId = clientId;
        outbound.notionalMinorUnits = 1_000_000_00L; // $1M — within every tier's limit and the per-order cap
        writeToQueueA(queueAPath, outbound);

        loop = new RiskValidationEventLoop();
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueBPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);

            assertEquals(EventStatus.ACCEPTED, result.eventStatus, "in-limit order must be accepted");
            assertEquals(expectedTier, result.clientTier, "clientTier must be resolved from clientId");
            assertEquals(99L, result.correlationId, "correlationId must be forwarded unchanged");
            assertNotEquals(0L, result.t1ServAEntry, "t1ServAEntry must be stamped");
            assertNotEquals(0L, result.t1ServAExit, "t1ServAExit must be stamped");
            assertTrue(result.t1ServAExit >= result.t1ServAEntry, "exit timestamp must not precede entry timestamp");
        }

        loop.stop();
        loop.awaitTermination();
        assertFalse(loop.isRunning(), "loop must report not-running once its thread has terminated");
    }

    @Test
    @DisplayName("run() rejects an order exceeding the per-order notional cap and forwards CREDIT_REJECTED status")
    void testFullLoopRejectsOverCapOrder() throws Exception {
        final String queueAPath = System.getProperty("fx.queue.queue-a.path");
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");

        final long clientId = 11L;
        final int expectedTier = CreditCheckEngine.resolveTier(clientId);

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 55L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.clientId = clientId;
        outbound.notionalMinorUnits = 60_000_000_00L; // $60M — exceeds the $50M per-order cap regardless of tier
        writeToQueueA(queueAPath, outbound);

        loop = new RiskValidationEventLoop();
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueBPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);

            assertEquals(EventStatus.CREDIT_REJECTED, result.eventStatus, "over-cap order must be credit-rejected");
            assertEquals(expectedTier, result.clientTier, "clientTier is still resolved even on rejection");
        }

        loop.stop();
        loop.awaitTermination();
    }

    @Test
    @DisplayName("constructor with telemetry recorders records queue-a wait time and serv-a duration without error")
    void testFullLoopWithTelemetryRecordersDoesNotThrow() throws Exception {
        final String queueAPath = System.getProperty("fx.queue.queue-a.path");
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 1L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.clientId = 3L;
        outbound.notionalMinorUnits = 1_000_000_00L;
        writeToQueueA(queueAPath, outbound);

        queueARecorder = new TelemetryRecorder(
                new File(tempDir.resolve("queue-a.hlog").toString()), 10_000_000_000L, 1_000L);
        servARecorder = new TelemetryRecorder(
                new File(tempDir.resolve("serv-a.hlog").toString()), 10_000_000_000L, 1_000L);

        loop = new RiskValidationEventLoop(queueARecorder, servARecorder);
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueBPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.ACCEPTED, result.eventStatus);
        }

        loop.stop();
        loop.awaitTermination();
    }
}
