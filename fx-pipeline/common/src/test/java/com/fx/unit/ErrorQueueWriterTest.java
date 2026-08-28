package com.fx.unit;

import com.fx.common.error.ErrorEvent;
import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link ErrorQueueWriter} — the non-blocking error
 * event router every {@code AbstractEventLoop} forwards poisoned events to.
 *
 * @author FX Pipeline Team
 */
@DisplayName("ErrorQueueWriter Tests")
class ErrorQueueWriterTest {

    @TempDir
    Path tempDir;

    private ErrorEvent readNext(final String queuePath) {
        try (final ChronicleQueue readerQueue = QueueFactory.create(queuePath);
             final ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final ErrorEvent errorEvent = new ErrorEvent();
            assertTrue(tailer.readDocument(errorEvent), "expected an entry in the error queue");
            return errorEvent;
        }
    }

    @Test
    @DisplayName("write() populates and appends an ErrorEvent with the source event's correlationId and status")
    void testWritePopulatesErrorEventFields() {
        final String path = tempDir.resolve("queue-err").toString();
        final FxMarketEvent source = new FxMarketEvent();
        source.reset();
        source.correlationId = 12345L;
        source.eventStatus = EventStatus.CREDIT_REJECTED;

        try (final ErrorQueueWriter writer = new ErrorQueueWriter(path)) {
            writer.write(source, "risk-a", "credit check failed");
        }

        final ErrorEvent read = readNext(path);
        assertEquals(12345L, read.correlationId);
        assertEquals("risk-a", read.serviceName);
        assertEquals("credit check failed", read.errorMessage);
        assertEquals(EventStatus.CREDIT_REJECTED, read.eventStatusAtError);
        assertTrue(read.errorNanoTime > 0L, "errorNanoTime must be stamped at write time");
    }

    @Test
    @DisplayName("write() falls back to \"unknown\" when errorMessage is null")
    void testWriteFallsBackToUnknownMessageWhenNull() {
        final String path = tempDir.resolve("queue-err").toString();
        final FxMarketEvent source = new FxMarketEvent();
        source.reset();
        source.correlationId = 7L;

        try (final ErrorQueueWriter writer = new ErrorQueueWriter(path)) {
            writer.write(source, "pricing-b", null);
        }

        assertEquals("unknown", readNext(path).errorMessage);
    }

    @Test
    @DisplayName("successive write() calls do not leak stale fields from the previous error event")
    void testSuccessiveWritesDoNotLeakStaleFields() {
        final String path = tempDir.resolve("queue-err").toString();
        final FxMarketEvent first = new FxMarketEvent();
        first.reset();
        first.correlationId = 1L;

        final FxMarketEvent second = new FxMarketEvent();
        second.reset();
        second.correlationId = 2L;

        try (final ErrorQueueWriter writer = new ErrorQueueWriter(path)) {
            writer.write(first, "gateway-0", "first failure");
            writer.write(second, "gateway-0", null);
        }

        try (final ChronicleQueue readerQueue = QueueFactory.create(path);
             final ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final ErrorEvent event1 = new ErrorEvent();
            final ErrorEvent event2 = new ErrorEvent();
            assertTrue(tailer.readDocument(event1));
            assertTrue(tailer.readDocument(event2));

            assertEquals("first failure", event1.errorMessage);
            assertEquals("unknown", event2.errorMessage,
                    "the flyweight reset() must clear the previous error's message before repopulating");
            assertEquals(2L, event2.correlationId);
        }
    }

    @Test
    @DisplayName("close() releases the appender and queue without throwing")
    void testCloseDoesNotThrow() {
        final String path = tempDir.resolve("queue-err").toString();
        final ErrorQueueWriter writer = new ErrorQueueWriter(path);
        assertDoesNotThrow(writer::close);
    }
}
