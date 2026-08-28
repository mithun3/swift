package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.risk.RiskMain;
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
 * Characterization tests for {@link RiskMain}, the serv-a entry point.
 *
 * <h2>Testing {@code main()} without a production refactor</h2>
 * <p>
 * As established for {@code GatewayMainTest} in serv-0, {@code main()} blocks the
 * calling thread indefinitely via {@code Thread.currentThread().join()}. This class
 * runs {@code main()} on a dedicated background thread and unblocks it deterministically
 * at teardown via {@link Thread#interrupt()}, which propagates as the declared
 * {@link InterruptedException} out of the blocking {@code join()} — no production code
 * is altered.
 *
 * <p><b>Deliberately NOT covered</b> (documented, not forced):
 * <ul>
 *   <li>The shutdown hook's {@code Runnable} body: only ever runs at real JVM exit,
 *       which cannot be safely triggered against a live Surefire fork mid-suite.</li>
 * </ul>
 *
 * @author FX Pipeline Team
 */
@DisplayName("RiskMain Characterization Tests")
class RiskMainTest {

    @TempDir
    Path tempDir;

    private Thread mainThread;
    private final AtomicReference<Throwable> uncaughtException = new AtomicReference<>();

    @BeforeEach
    void configureQueueOverrides() throws IOException {
        System.setProperty("fx.queue.queue-a.path", tempDir.resolve("queue-a").toString());
        System.setProperty("fx.queue.queue-b.path", tempDir.resolve("queue-b").toString());
        // Pre-create both queue directories: QueueFactory.create()'s "!exists() && !mkdirs()"
        // check is not atomic, so the test's writer thread and main()'s background thread
        // constructing their own ChronicleQueue instances against a fresh, not-yet-existing
        // temp directory can race on the same mkdirs() call and one loses (IllegalStateException:
        // "Failed to create queue directory"). Pre-creating avoids the race without touching
        // QueueFactory's production code.
        Files.createDirectories(tempDir.resolve("queue-a"));
        Files.createDirectories(tempDir.resolve("queue-b"));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (mainThread != null) {
            // Unblocks main()'s Thread.currentThread().join() without touching production code.
            mainThread.interrupt();
            mainThread.join(5_000L);
            assertFalse(mainThread.isAlive(), "risk-main-under-test thread must terminate after interrupt()");
        }
        System.clearProperty("fx.queue.queue-a.path");
        System.clearProperty("fx.queue.queue-b.path");
        System.clearProperty("fx.telemetry.enabled");
        System.clearProperty("fx.telemetry.log.path");
    }

    private Thread startMainInBackground() {
        final Thread t = new Thread(() -> {
            try {
                RiskMain.main(new String[0]);
            } catch (final InterruptedException expectedShutdownSignal) {
                // Expected: tearDown() interrupts this thread to unblock main()'s join().
            }
        }, "risk-main-under-test");
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

    private void writeToQueueA(final String queueAPath, final FxMarketEvent event) {
        try (ChronicleQueue writerQueue = QueueFactory.create(queueAPath);
             ExcerptAppender appender = writerQueue.createAppender()) {
            appender.writeDocument(event);
        }
    }

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException (non-instantiable utility/entry class)")
    void testPrivateConstructorThrowsUnsupportedOperationException() throws Exception {
        final Constructor<RiskMain> ctor = RiskMain.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        final InvocationTargetException wrapped = assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, wrapped.getCause());
    }

    @Test
    @DisplayName("main() with telemetry disabled starts the event loop and forwards an accepted event to queue-b")
    void testMainTelemetryDisabledProcessesEvent() throws Exception {
        System.setProperty("fx.telemetry.enabled", "false");

        mainThread = startMainInBackground();

        final String queueAPath = tempDir.resolve("queue-a").toString();
        final String queueBPath = tempDir.resolve("queue-b").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 21L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.clientId = 5L;
        outbound.notionalMinorUnits = 1_000_000_00L; // $1M — within every tier's limit and the per-order cap
        writeToQueueA(queueAPath, outbound);

        try (ChronicleQueue readerQueue = QueueFactory.create(queueBPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.ACCEPTED, result.eventStatus);
            assertEquals(21L, result.correlationId);
        }

        assertNull(uncaughtException.get(), "main() must not throw during telemetry-disabled startup");
    }

    @Test
    @DisplayName("main() with telemetry enabled constructs real TelemetryRecorders and continues without error")
    void testMainTelemetryEnabledConstructsRecorders() throws Exception {
        System.setProperty("fx.telemetry.enabled", "true");
        System.setProperty("fx.telemetry.log.path", tempDir.resolve("telemetry.hlog").toString());

        mainThread = startMainInBackground();

        final String queueAPath = tempDir.resolve("queue-a").toString();
        final String queueBPath = tempDir.resolve("queue-b").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 34L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.clientId = 9L;
        outbound.notionalMinorUnits = 1_000_000_00L;
        writeToQueueA(queueAPath, outbound);

        try (ChronicleQueue readerQueue = QueueFactory.create(queueBPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.ACCEPTED, result.eventStatus);
        }

        assertNull(uncaughtException.get(), "main() must not throw when telemetry is successfully initialised");
    }

    @Test
    @DisplayName("main() catches a telemetry init failure (unwritable log path) and continues without telemetry")
    void testMainTelemetryInitFailureIsCaughtAndLoopStillStarts() throws Exception {
        System.setProperty("fx.telemetry.enabled", "true");
        // Parent directories do not exist — PrintStream(File) inside TelemetryRecorder's
        // constructor throws FileNotFoundException, which RiskMain must catch and log.
        System.setProperty("fx.telemetry.log.path",
                tempDir.resolve("no/such/nested/dir/telemetry.hlog").toString());

        mainThread = startMainInBackground();

        final String queueAPath = tempDir.resolve("queue-a").toString();
        final String queueBPath = tempDir.resolve("queue-b").toString();

        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 47L;
        outbound.ingressNanoTime = System.nanoTime();
        outbound.clientId = 13L;
        outbound.notionalMinorUnits = 1_000_000_00L;
        writeToQueueA(queueAPath, outbound);

        // The loop must still start and process events even though telemetry failed to init.
        try (ChronicleQueue readerQueue = QueueFactory.create(queueBPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent result = new FxMarketEvent();
            awaitEvent(tailer, result, 5_000L);
            assertEquals(EventStatus.ACCEPTED, result.eventStatus);
        }

        assertNull(uncaughtException.get(),
                "the telemetry init failure must be caught inside main(), never propagate out");
    }
}
