package com.fx.unit;

import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.OptimizedQueueTailer;
import com.fx.common.queue.QueueFactory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for {@link OptimizedQueueTailer#readDocument(net.openhft.chronicle.wire.ReadMarshallable)}.
 *
 * <p>A prior implementation passed a no-op callback to the underlying tailer,
 * which consumed each document without decoding its fields — every event
 * delivered downstream was silently empty. This caused
 * {@code queue-a}/{@code queue-b}/{@code queue-c}, end-to-end, and db-commit
 * telemetry to record zero samples in production bare-metal runs, while
 * per-stage dispatch metrics (which depend only on locally-set timestamps)
 * appeared to work normally, masking the data loss. These tests pin the
 * fixed behaviour: business fields must be actually decoded into the target.
 */
@DisplayName("OptimizedQueueTailer Characterization Tests")
class OptimizedQueueTailerTest {

    @TempDir
    Path tempDir;

    private ChronicleQueue queue;

    @BeforeEach
    void setUp() {
        queue = QueueFactory.create(tempDir.resolve("queue-test").toString());
    }

    @AfterEach
    void tearDown() {
        if (queue != null) {
            queue.close();
        }
    }

    @Test
    @DisplayName("readDocument decodes the actual business fields, not just the index")
    void testReadDocumentDecodesEventFields() {
        try (final ExcerptAppender appender = queue.createAppender()) {
            final FxMarketEvent written = new FxMarketEvent();
            written.reset();
            written.correlationId = 424242L;
            written.ingressNanoTime = 123_456_789L;
            written.currencyPairCode = 76232755860292L;
            written.notionalMinorUnits = 1_000_000L;
            appender.writeDocument(written);
        }

        try (final OptimizedQueueTailer tailer = OptimizedQueueTailer.create(queue, "test-reader")) {
            final FxMarketEvent readInto = new FxMarketEvent();
            readInto.reset();

            final boolean eventRead = tailer.readDocument(readInto);

            assertTrue(eventRead, "a document was appended and must be read");
            assertEquals(424242L, readInto.correlationId,
                    "correlationId must be decoded from the queue, not left at its default");
            assertEquals(123_456_789L, readInto.ingressNanoTime,
                    "ingressNanoTime must be decoded from the queue, not left at its default");
            assertEquals(76232755860292L, readInto.currencyPairCode);
            assertEquals(1_000_000L, readInto.notionalMinorUnits);
        }
    }

    @Test
    @DisplayName("readDocument returns false and leaves the target untouched when the queue is empty")
    void testReadDocumentReturnsFalseOnEmptyQueue() {
        try (final OptimizedQueueTailer tailer = OptimizedQueueTailer.create(queue, "test-reader-empty")) {
            final FxMarketEvent readInto = new FxMarketEvent();
            readInto.reset();

            assertFalse(tailer.readDocument(readInto), "an empty queue must not report a successful read");
        }
    }

    @Test
    @DisplayName("Multiple sequential reads each decode their own distinct event")
    void testSequentialReadsDecodeDistinctEvents() {
        try (final ExcerptAppender appender = queue.createAppender()) {
            for (long i = 1; i <= 3; i++) {
                final FxMarketEvent written = new FxMarketEvent();
                written.reset();
                written.correlationId = i * 100;
                appender.writeDocument(written);
            }
        }

        try (final OptimizedQueueTailer tailer = OptimizedQueueTailer.create(queue, "test-reader-seq")) {
            final FxMarketEvent readInto = new FxMarketEvent();

            for (long i = 1; i <= 3; i++) {
                readInto.reset();
                assertTrue(tailer.readDocument(readInto));
                assertEquals(i * 100, readInto.correlationId,
                        "each read must decode the correct event, in order");
            }
        }
    }
}
