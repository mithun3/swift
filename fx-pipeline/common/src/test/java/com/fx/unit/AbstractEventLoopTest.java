package com.fx.unit;

import com.fx.common.error.ErrorEvent;
import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.handler.AbstractEventLoop;
import com.fx.common.queue.QueueFactory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link AbstractEventLoop} — the base single-writer
 * event loop shared by every pipeline service (the Disruptor-analog "BatchEventProcessor"
 * of this codebase). These tests lock in current behaviour; they intentionally use
 * real (temp-dir) Chronicle Queues rather than mocks, since {@link ExcerptTailer} /
 * {@link ExcerptAppender} sequencing semantics are exactly what is under test.
 *
 * <p>CPU affinity is disabled ({@code cpuCore = -1}) for all cases, matching the
 * convention already used by {@code FullPipelineIntegrationTest} to stay portable
 * across CI/CD and non-Linux dev machines.
 *
 * @author FX Pipeline Team
 */
@DisplayName("AbstractEventLoop Characterization Tests")
class AbstractEventLoopTest {

    @TempDir
    Path tempDir;

    private ChronicleQueue inputQueue;
    private ChronicleQueue outputQueue;
    private String errorQueuePath;
    private ErrorQueueWriter errorWriter;
    private RecordingEventLoop loop;

    @BeforeEach
    void setUp() {
        inputQueue = QueueFactory.create(tempDir.resolve("queue-in").toString());
        outputQueue = QueueFactory.create(tempDir.resolve("queue-out").toString());
        errorQueuePath = tempDir.resolve("queue-err").toString();
        errorWriter = new ErrorQueueWriter(errorQueuePath);
    }

    @AfterEach
    void tearDown() {
        if (loop != null) {
            try {
                loop.close();
            } catch (final Exception alreadyClosedByTest) {
                // Some tests close the loop themselves to assert close() behaviour directly.
            }
        }
    }

    /** Minimal concrete subclass that records every {@code handle()} invocation for assertions. */
    private static final class RecordingEventLoop extends AbstractEventLoop {
        final List<Long> sequencesHandled = new CopyOnWriteArrayList<>();
        final List<FxMarketEvent> instancesSeen = new CopyOnWriteArrayList<>();
        final List<Boolean> endOfBatchSeen = new CopyOnWriteArrayList<>();
        volatile boolean throwOnNextEvent = false;

        RecordingEventLoop(final ChronicleQueue in, final ChronicleQueue out, final ErrorQueueWriter err) {
            super("test-loop", in, out, err, -1);
        }

        @Override
        protected void handle(final FxMarketEvent event, final long sequence,
                               final boolean endOfBatch, final ExcerptAppender appender) {
            if (throwOnNextEvent) {
                throwOnNextEvent = false;
                throw new RuntimeException("simulated handler failure");
            }
            instancesSeen.add(event);
            sequencesHandled.add(sequence);
            endOfBatchSeen.add(endOfBatch);
            if (appender != null) {
                event.eventStatus = EventStatus.ACCEPTED;
                appender.writeDocument(event);
            }
        }
    }

    private void appendEvent(final long correlationId) {
        try (final ExcerptAppender appender = inputQueue.createAppender()) {
            final FxMarketEvent event = new FxMarketEvent();
            event.reset();
            event.correlationId = correlationId;
            event.ingressNanoTime = System.nanoTime();
            appender.writeDocument(event);
        }
    }

    /** Bounded poll — fails fast on regression instead of hanging the suite like a raw Thread.sleep guess. */
    private static void awaitCondition(final BooleanSupplier condition, final long timeoutMillis) {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadlineNanos) {
                fail("Condition not met within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }

    @Test
    @DisplayName("isRunning() is false before start() and after stop()+termination")
    void testLifecycleFlags() throws InterruptedException {
        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        assertFalse(loop.isRunning(), "loop must not report running before start()");

        loop.start();
        awaitCondition(loop::isRunning, 2_000L);

        loop.stop();
        loop.awaitTermination();
        assertFalse(loop.isRunning(), "loop must report not-running once its thread has terminated");
    }

    @Test
    @DisplayName("Events are delivered to handle() in sequence order with endOfBatch always true")
    void testEventsHandledInOrderWithEndOfBatchTrue() throws InterruptedException {
        appendEvent(100L);
        appendEvent(200L);
        appendEvent(300L);

        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        loop.start();
        awaitCondition(() -> loop.sequencesHandled.size() >= 3, 5_000L);
        loop.stop();
        loop.awaitTermination();

        assertEquals(3, loop.sequencesHandled.size());
        assertTrue(loop.sequencesHandled.get(0) < loop.sequencesHandled.get(1));
        assertTrue(loop.sequencesHandled.get(1) < loop.sequencesHandled.get(2));
        assertTrue(loop.endOfBatchSeen.stream().allMatch(Boolean::booleanValue),
                "the readingDocument peek was removed for performance — endOfBatch is always true today");
    }

    @Test
    @DisplayName("The same flyweight instance is reused across events (zero-allocation guardrail)")
    void testFlyweightInstanceIsReusedAcrossEvents() throws InterruptedException {
        appendEvent(1L);
        appendEvent(2L);

        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        loop.start();
        awaitCondition(() -> loop.instancesSeen.size() >= 2, 5_000L);
        loop.stop();
        loop.awaitTermination();

        assertSame(loop.instancesSeen.get(0), loop.instancesSeen.get(1),
                "handle() must always receive the single pre-allocated flyweight instance, never a new one");
    }

    @Test
    @DisplayName("flyweight.reset() clears stale fields left by the previous event before the next read")
    void testFlyweightIsResetBetweenEvents() throws InterruptedException {
        appendEvent(999L);

        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        loop.start();
        awaitCondition(() -> loop.instancesSeen.size() >= 1, 5_000L);

        // Simulate a field a handler might set that isn't touched by the next event's payload.
        loop.instancesSeen.get(0).notionalMinorUnits = 555_555L;
        appendEvent(1000L);
        awaitCondition(() -> loop.instancesSeen.size() >= 2, 5_000L);

        loop.stop();
        loop.awaitTermination();

        assertEquals(0L, loop.instancesSeen.get(1).notionalMinorUnits,
                "reset() before each read must clear fields left over from the previous event");
    }

    @Test
    @DisplayName("An exception thrown from handle() is routed to the error queue and the loop keeps processing")
    void testHandlerExceptionRoutesToErrorQueueAndContinues() throws InterruptedException {
        appendEvent(42L);
        appendEvent(43L);

        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        loop.throwOnNextEvent = true;
        loop.start();
        awaitCondition(() -> loop.sequencesHandled.size() >= 1, 5_000L);
        loop.stop();
        loop.awaitTermination();

        // Only the second event reaches the recording list; the first was routed to the error queue instead.
        assertEquals(1, loop.sequencesHandled.size());

        try (final ChronicleQueue errorReaderQueue = QueueFactory.create(errorQueuePath);
             final ExcerptTailer errorTailer = errorReaderQueue.createTailer("test-error-reader")) {
            final ErrorEvent errorEvent = new ErrorEvent();
            assertTrue(errorTailer.readDocument(errorEvent), "error queue should contain the failed event");
            assertEquals(42L, errorEvent.correlationId, "the failed event's correlationId must be preserved");
            assertEquals("test-loop", errorEvent.serviceName);
        }
    }

    @Test
    @DisplayName("stop() drains backlog already sitting in the input queue instead of abandoning it")
    void testStopDrainsExistingBacklogBeforeExiting() throws InterruptedException {
        for (long i = 1; i <= 5; i++) {
            appendEvent(i);
        }

        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        loop.start();
        // Signal stop immediately — any backlog already in the queue must still be drained
        // (see AbstractEventLoop#drainRemainingBacklog), not silently abandoned.
        loop.stop();
        loop.awaitTermination();

        assertEquals(5, loop.sequencesHandled.size(),
                "all 5 pre-queued events must be processed even though stop() was called immediately");
    }

    @Test
    @DisplayName("close() stops the loop and releases the input/output/error queue resources")
    void testCloseStopsLoopAndClosesQueues() throws InterruptedException {
        loop = new RecordingEventLoop(inputQueue, outputQueue, errorWriter);
        loop.start();
        awaitCondition(loop::isRunning, 2_000L);

        assertDoesNotThrow(() -> loop.close());
        assertFalse(loop.isRunning(), "close() must stop the event loop thread");
    }
}
