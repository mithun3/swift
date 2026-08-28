package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.pricing.PricingEventLoop;
import com.fx.pricing.PricingMain;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link PricingMain}, the serv-b entry point.
 *
 * <h2>Testing {@code main()} without a production refactor</h2>
 * <p>
 * As established for {@code GatewayMainTest} (serv-0) and {@code RiskMainTest}
 * (serv-a), {@code main()} blocks the calling thread indefinitely via
 * {@code Thread.currentThread().join()}. This class runs {@code main()} on a
 * dedicated background thread and unblocks it deterministically at teardown via
 * {@link Thread#interrupt()}, which propagates as the declared
 * {@link InterruptedException} out of the blocking {@code join()} — no production
 * code is altered.
 *
 * <p><b>Deliberately NOT covered</b> (documented, not forced):
 * <ul>
 *   <li>The shutdown hook's {@code catch (InterruptedException)} body: never thrown
 *       on the normal path — {@code awaitTermination()} completes without interruption
 *       when the fork exits and all still-registered shutdown hooks run.</li>
 * </ul>
 *
 * @author FX Pipeline Team
 */
@DisplayName("PricingMain Characterization Tests")
class PricingMainTest {

    @TempDir
    Path tempDir;

    private Thread mainThread;
    private final AtomicReference<Throwable> uncaughtException = new AtomicReference<>();

    @BeforeEach
    void configureQueueOverrides() throws IOException {
        System.setProperty("fx.queue.queue-b.path", tempDir.resolve("queue-b").toString());
        System.setProperty("fx.queue.queue-c.path", tempDir.resolve("queue-c").toString());
        // Pre-create both queue directories: QueueFactory.create()'s "!exists() && !mkdirs()"
        // check is not atomic, so the test's writer thread and main()'s background thread
        // constructing their own ChronicleQueue instances against a fresh, not-yet-existing
        // temp directory can race on the same mkdirs() call and one loses (IllegalStateException:
        // "Failed to create queue directory"). Pre-creating avoids the race without touching
        // QueueFactory's production code.
        Files.createDirectories(tempDir.resolve("queue-b"));
        Files.createDirectories(tempDir.resolve("queue-c"));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (mainThread != null) {
            // Unblocks main()'s Thread.currentThread().join() without touching production code.
            mainThread.interrupt();
            mainThread.join(5_000L);
            assertFalse(mainThread.isAlive(), "pricing-main-under-test thread must terminate after interrupt()");
        }
        System.clearProperty("fx.queue.queue-b.path");
        System.clearProperty("fx.queue.queue-c.path");
        System.clearProperty("fx.telemetry.enabled");
        System.clearProperty("fx.telemetry.log.path");
    }

    private Thread startMainInBackground() {
        final Thread t = new Thread(() -> {
            try {
                PricingMain.main(new String[0]);
            } catch (final InterruptedException expectedShutdownSignal) {
                // Expected: tearDown() interrupts this thread to unblock main()'s join().
            }
        }, "pricing-main-under-test");
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thread, ex) -> uncaughtException.set(ex));
        t.start();
        return t;
    }

    /** Bounded poll — fails fast on regression instead of hanging the suite. */
    private static void awaitEvent(final ExcerptTailer tailer, final FxMarketEvent event, final long timeoutMillis) {
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
    @DisplayName("private constructor throws UnsupportedOperationException (non-instantiable utility/entry class)")
    void testPrivateConstructorThrowsUnsupportedOperationException() throws Exception {
        final Constructor<PricingMain> ctor = PricingMain.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        final InvocationTargetException wrapped = assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, wrapped.getCause());
    }

    @Test
    @DisplayName("main() with telemetry disabled starts the event loop and prices an accepted event to queue-c")
    void testMainTelemetryDisabledProcessesEvent() throws Exception {
        System.setProperty("fx.telemetry.enabled", "false");

        mainThread = startMainInBackground();

        final String queueBPath = tempDir.resolve("queue-b").toString();
        final String queueCPath = tempDir.resolve("queue-c").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 301L;
        outbound.eventStatus = EventStatus.ACCEPTED;
        outbound.clientTier = 2;
        outbound.requestedPriceScaled = 108500L;
        outbound.side = 1;
        writeToQueueB(queueBPath, outbound);

        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.PRICED, result.eventStatus);
            assertEquals(301L, result.correlationId);
        }

        assertNull(uncaughtException.get(), "main() must not throw during telemetry-disabled startup");
        assertEquals(PricingEventLoop.CPU_CORE, 2, "CPU_CORE constant must remain 2 (isolated from serv-0/serv-a)");
    }

    @Test
    @DisplayName("main() with telemetry enabled constructs real TelemetryRecorders and continues without error")
    void testMainTelemetryEnabledConstructsRecorders() throws Exception {
        System.setProperty("fx.telemetry.enabled", "true");
        System.setProperty("fx.telemetry.log.path", tempDir.resolve("telemetry.hlog").toString());

        mainThread = startMainInBackground();

        final String queueBPath = tempDir.resolve("queue-b").toString();
        final String queueCPath = tempDir.resolve("queue-c").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 302L;
        outbound.eventStatus = EventStatus.ACCEPTED;
        outbound.clientTier = 1;
        outbound.requestedPriceScaled = 108500L;
        writeToQueueB(queueBPath, outbound);

        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.PRICED, result.eventStatus);
        }

        assertNull(uncaughtException.get(), "main() must not throw when telemetry is successfully initialised");
    }

    @Test
    @DisplayName("main() catches a telemetry init failure (unwritable log path) and continues without telemetry")
    void testMainTelemetryInitFailureIsCaughtAndLoopStillStarts() throws Exception {
        System.setProperty("fx.telemetry.enabled", "true");
        // Parent directories do not exist — PrintStream(File) inside TelemetryRecorder's
        // constructor throws FileNotFoundException, which PricingMain must catch and log.
        System.setProperty("fx.telemetry.log.path",
                tempDir.resolve("no/such/nested/dir/telemetry.hlog").toString());

        mainThread = startMainInBackground();

        final String queueBPath = tempDir.resolve("queue-b").toString();
        final String queueCPath = tempDir.resolve("queue-c").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 303L;
        outbound.eventStatus = EventStatus.ACCEPTED;
        outbound.clientTier = 3;
        outbound.requestedPriceScaled = 108500L;
        writeToQueueB(queueBPath, outbound);

        // The loop must still start and process events even though telemetry failed to init.
        try (ChronicleQueue readerQueue = QueueFactory.create(queueCPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.PRICED, result.eventStatus);
        }

        assertNull(uncaughtException.get(),
                "the telemetry init failure must be caught inside main(), never propagate out");
    }
}
