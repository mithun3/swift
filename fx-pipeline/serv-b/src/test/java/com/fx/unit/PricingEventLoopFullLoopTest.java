package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.common.telemetry.TelemetryRecorder;
import com.fx.pricing.PricingEventLoop;
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
 * Full end-to-end characterization tests for {@link PricingEventLoop}, the
 * Disruptor-analog "EventProcessor" that tails queue-b (produced by serv-a) and
 * appends to queue-c (consumed by serv-c). These tests run the real {@code handle()}
 * path via {@code run()} against real temp-dir Chronicle Queues, matching the
 * convention established by {@code GatewayEventLoopFullLoopTest} (serv-0) and
 * {@code RiskValidationEventLoopTest} (serv-a) — {@code fx.queue.queue-b.path} and
 * {@code fx.queue.queue-c.path} are resolved dynamically per call by
 * {@code QueueFactory.createWithOverride}, so they are safe to override per test.
 *
 * @author FX Pipeline Team
 */
@DisplayName("PricingEventLoop Full-Loop Characterization Tests")
class PricingEventLoopFullLoopTest {

    @TempDir
    Path tempDir;

    private PricingEventLoop loop;
    private TelemetryRecorder queueBRecorder;
    private TelemetryRecorder servBRecorder;

    @BeforeEach
    void configureQueueOverrides() {
        System.setProperty("fx.queue.queue-b.path", tempDir.resolve("queue-b").toString());
        System.setProperty("fx.queue.queue-c.path", tempDir.resolve("queue-c").toString());
    }

    @AfterEach
    void tearDown() {
        if (loop != null) {
            loop.close();
        }
        if (queueBRecorder != null) {
            queueBRecorder.close();
        }
        if (servBRecorder != null) {
            servBRecorder.close();
        }
        System.clearProperty("fx.queue.queue-b.path");
        System.clearProperty("fx.queue.queue-c.path");
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

    private void writeToQueueB(final String queueBPath, final FxMarketEvent event) {
        try (ChronicleQueue writerQueue = QueueFactory.create(queueBPath);
             ExcerptAppender appender = writerQueue.createAppender()) {
            appender.writeDocument(event);
        }
    }

    @Test
    @DisplayName("run() prices an ACCEPTED event with a valid tier and forwards PRICED status to queue-c")
    void testFullLoopPricesAcceptedOrder() throws Exception {
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");
        final String queueCPath = System.getProperty("fx.queue.queue-c.path");

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 200L;
        outbound.eventStatus = EventStatus.ACCEPTED;
        outbound.clientTier = 2; // PRIME — 10 scaled units spread
        outbound.requestedPriceScaled = 108500L;
        outbound.side = 1; // BUY
        outbound.t1ServAExit = System.nanoTime();
        writeToQueueB(queueBPath, outbound);

        loop = new PricingEventLoop();
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);

            assertEquals(EventStatus.PRICED, result.eventStatus, "in-tier accepted order must be priced");
            assertEquals(200L, result.correlationId, "correlationId must be forwarded unchanged");
            assertEquals(10L, result.spreadScaled, "PRIME tier spread must be applied");
            assertEquals(108505L, result.executedPriceScaled, "BUY executed = mid + half-spread = 108500 + 5");
            assertNotEquals(0L, result.t2ServBEntry, "t2ServBEntry must be stamped");
            assertNotEquals(0L, result.t2ServBExit, "t2ServBExit must be stamped");
            assertTrue(result.t2ServBExit >= result.t2ServBEntry, "exit timestamp must not precede entry timestamp");
        }

        loop.stop();
        loop.awaitTermination();
        assertFalse(loop.isRunning(), "loop must report not-running once its thread has terminated");
    }

    @Test
    @DisplayName("run() marks an ACCEPTED event with an unknown tier as PRICING_FAILED and still forwards it")
    void testFullLoopMarksUnknownTierPricingFailed() throws Exception {
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");
        final String queueCPath = System.getProperty("fx.queue.queue-c.path");

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 201L;
        outbound.eventStatus = EventStatus.ACCEPTED;
        outbound.clientTier = 0; // unknown tier
        outbound.requestedPriceScaled = 108500L;
        writeToQueueB(queueBPath, outbound);

        loop = new PricingEventLoop();
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);

            assertEquals(EventStatus.PRICING_FAILED, result.eventStatus, "unknown tier must fail pricing");
            assertEquals(201L, result.correlationId);
        }

        loop.stop();
        loop.awaitTermination();
    }

    @Test
    @DisplayName("run() forwards a CREDIT_REJECTED event unchanged, bypassing the spread engine")
    void testFullLoopBypassesPricingForCreditRejected() throws Exception {
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");
        final String queueCPath = System.getProperty("fx.queue.queue-c.path");

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 202L;
        outbound.eventStatus = EventStatus.CREDIT_REJECTED;
        outbound.clientTier = 1;
        outbound.requestedPriceScaled = 108500L;
        writeToQueueB(queueBPath, outbound);

        loop = new PricingEventLoop();
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);

            assertEquals(EventStatus.CREDIT_REJECTED, result.eventStatus, "status must remain unchanged");
            assertEquals(0L, result.executedPriceScaled, "spread engine must never run for a terminal failure");
            assertEquals(0L, result.spreadScaled, "spread engine must never run for a terminal failure");
        }

        loop.stop();
        loop.awaitTermination();
    }

    @Test
    @DisplayName("constructor with telemetry recorders records queue-b wait time and serv-b duration without error")
    void testFullLoopWithTelemetryRecordersDoesNotThrow() throws Exception {
        final String queueBPath = System.getProperty("fx.queue.queue-b.path");
        final String queueCPath = System.getProperty("fx.queue.queue-c.path");

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 203L;
        outbound.eventStatus = EventStatus.ACCEPTED;
        outbound.clientTier = 1;
        outbound.requestedPriceScaled = 108500L;
        outbound.t1ServAExit = System.nanoTime();
        writeToQueueB(queueBPath, outbound);

        queueBRecorder = new TelemetryRecorder(
                new File(tempDir.resolve("queue-b.hlog").toString()), 10_000_000_000L, 1_000L);
        servBRecorder = new TelemetryRecorder(
                new File(tempDir.resolve("serv-b.hlog").toString()), 10_000_000_000L, 1_000L);

        loop = new PricingEventLoop(queueBRecorder, servBRecorder);
        loop.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitNextEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.PRICED, result.eventStatus);
        }

        loop.stop();
        loop.awaitTermination();
    }
}
