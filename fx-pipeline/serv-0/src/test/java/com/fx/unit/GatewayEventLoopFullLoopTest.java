package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.error.ErrorEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.common.queue.QueuePaths;
import com.fx.gateway.CorrelationIdGenerator;
import com.fx.gateway.GatewayEventLoop;
import com.fx.gateway.SyntheticFixSource;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full end-to-end characterization tests for {@link GatewayEventLoop}, the
 * Disruptor-analog "EventProcessor" that owns queue-a (the Chronicle-Queue-backed
 * ring buffer between serv-0 and serv-a). Unlike {@link GatewayEventLoopTest}
 * (which exercises {@link com.fx.gateway.FixDecoder} in isolation via mocks),
 * these tests run the real {@code run()} loop on a real temp-dir Chronicle Queue,
 * matching the convention already used by {@code AbstractEventLoopTest} and
 * {@code FullPipelineIntegrationTest}.
 *
 * <p>{@code fx.queue.base.dir} is overridden once in {@code @BeforeAll}, before
 * {@code QueuePaths} is first loaded anywhere in this test JVM — this is the only
 * class in serv-0 that constructs a real {@link GatewayEventLoop}, so the
 * static-final {@code QueuePaths.QUEUE_ERR} path it resolves against is safely
 * redirected away from the real {@code /tmp/fx-queues} for the whole class.
 *
 * @author FX Pipeline Team
 */
@DisplayName("GatewayEventLoop Full-Loop Characterization Tests")
class GatewayEventLoopFullLoopTest {

    @TempDir
    static Path sharedBaseDir;

    @TempDir
    Path tempDir;

    private GatewayEventLoop gateway;

    @BeforeAll
    static void configureQueueBaseDir() {
        System.setProperty("fx.queue.base.dir", sharedBaseDir.toString());
    }

    @AfterAll
    static void clearQueueBaseDir() {
        System.clearProperty("fx.queue.base.dir");
    }

    @BeforeEach
    void configureQueueAOverride() {
        System.setProperty("fx.queue.queue-a.path", tempDir.resolve("queue-a").toString());
    }

    @AfterEach
    void tearDown() {
        if (gateway != null) {
            gateway.close();
        }
        System.clearProperty("fx.queue.queue-a.path");
    }

    /** Bounded poll — fails fast on regression instead of hanging the suite. */
    private static FxMarketEvent awaitNextEvent(final ExcerptTailer tailer,
                                                 final FxMarketEvent event,
                                                 final long timeoutMillis) {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (!tailer.readDocument(event)) {
            if (System.nanoTime() > deadlineNanos) {
                fail("No event appeared on queue-a within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
        return event;
    }

    @Test
    @DisplayName("run() decodes synthetic FIX messages and appends enriched events to queue-a in order")
    void testFullLoopDecodesAndAppendsToQueueA() throws Exception {
        final int messageCount = 5;
        final SyntheticFixSource source = new SyntheticFixSource(messageCount);
        final CorrelationIdGenerator idGenerator = new CorrelationIdGenerator();
        gateway = new GatewayEventLoop(source, idGenerator, null);

        final String queueAPath = System.getProperty("fx.queue.queue-a.path");
        gateway.start();

        try (ChronicleQueue readerQueue = QueueFactory.create(queueAPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {

            final FxMarketEvent event = new FxMarketEvent();
            long previousCorrelationId = 0L;
            long previousIngressNanoTime = 0L;
            long expectedClientId = 0L;
            long expectedCurrencyPairCode = 0L;

            for (int i = 1; i <= messageCount; i++) {
                awaitNextEvent(tailer, event, 5_000L);

                assertEquals('D', event.fixMsgType, "synthetic source always sends NewOrderSingle");
                assertEquals(i, event.fixSeqNum, "FIX seq num must match synthetic source's monotonic counter");
                assertEquals(1_000_000L, event.notionalMinorUnits, "synthetic source always sends OrderQty=1000000");
                assertEquals(108_500L, event.requestedPriceScaled, "synthetic source always sends Price=1.08500");
                assertEquals((byte) 1, event.side, "synthetic source always sends Side=1 (Buy)");
                assertEquals(EventStatus.RECEIVED, event.eventStatus, "gateway must stamp RECEIVED before appending");
                assertTrue(event.correlationId > previousCorrelationId,
                        "correlationId must be strictly increasing across events");
                assertTrue(event.ingressNanoTime >= previousIngressNanoTime,
                        "ingressNanoTime must be non-decreasing (System.nanoTime() is monotonic)");
                assertNotEquals(0L, event.clientId);
                assertNotEquals(0L, event.currencyPairCode);

                if (i == 1) {
                    expectedClientId = event.clientId;
                    expectedCurrencyPairCode = event.currencyPairCode;
                } else {
                    assertEquals(expectedClientId, event.clientId, "same CLIENT1 sender on every synthetic message");
                    assertEquals(expectedCurrencyPairCode, event.currencyPairCode, "same EUR/USD symbol on every synthetic message");
                }

                previousCorrelationId = event.correlationId;
                previousIngressNanoTime = event.ingressNanoTime;
            }
        }

        gateway.stop();
        gateway.awaitTermination();
        assertFalse(gateway.isRunning(), "loop must report not-running once its thread has terminated");
    }

    @Test
    @DisplayName("run() routes a structurally invalid FIX message to the error queue instead of crashing")
    void testFullLoopRoutesDecodeFailureToErrorQueue() throws Exception {
        // Missing mandatory tags (no Symbol=55, Side=54, OrderQty=38, Price=44) — same
        // shape FixDecoderTest already characterizes as a guaranteed decode failure.
        final byte[] SOH = {0x01};
        final String malformed = "35=D" + (char) SOH[0] + "34=1" + (char) SOH[0];
        final GatewayEventLoop.FixMessageSource singleShotSource = new GatewayEventLoop.FixMessageSource() {
            private final byte[] buf = new byte[256];
            private boolean served = false;

            @Override
            public byte[] buffer() {
                return buf;
            }

            @Override
            public int poll(final byte[] destination, final int offset, final int maxLength) {
                if (served) {
                    return -1;
                }
                served = true;
                final byte[] bytes = malformed.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                System.arraycopy(bytes, 0, destination, offset, bytes.length);
                return bytes.length;
            }

            @Override
            public boolean isExhausted() {
                return served;
            }
        };

        gateway = new GatewayEventLoop(singleShotSource, new CorrelationIdGenerator(), null);
        gateway.start();

        // Read QueuePaths.QUEUE_ERR directly rather than assuming sharedBaseDir "won" the
        // race to first-resolve QueuePaths' static-final BASE_DIR: Surefire test class
        // execution order within a shared fork is not guaranteed, so another test class
        // may have already fixed BASE_DIR to its own tempDir before this class's @BeforeAll ran.
        final String queueErrPath = QueuePaths.QUEUE_ERR;
        try (ChronicleQueue errorReaderQueue = QueueFactory.create(queueErrPath);
             ExcerptTailer errorTailer = errorReaderQueue.createTailer("test-error-reader")) {

            final ErrorEvent errorEvent = new ErrorEvent();
            final long deadlineNanos = System.nanoTime() + 5_000L * 1_000_000L;
            while (!errorTailer.readDocument(errorEvent)) {
                if (System.nanoTime() > deadlineNanos) {
                    fail("No event appeared on queue-err within 5000ms");
                }
                Thread.onSpinWait();
            }

            assertEquals("gateway", errorEvent.serviceName);
            assertEquals("FIX decode failed", errorEvent.errorMessage);
        }

        gateway.stop();
        gateway.awaitTermination();
    }
}
