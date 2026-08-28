package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.gateway.GatewayMain;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link GatewayMain}, the serv-0 entry point.
 *
 * <h2>Testing {@code main()} without a production refactor</h2>
 * <p>
 * {@code main()} blocks the calling thread indefinitely via {@code
 * Thread.currentThread().join()} and declares {@code throws InterruptedException}.
 * Rather than treating the whole method as untestable (as the {@code common}
 * module pilot did for its own {@code *Main} classes with a bare {@code
 * System.exit()} in the catch block), this class runs {@code main()} on a
 * dedicated background thread and unblocks it deterministically at teardown by
 * calling {@link Thread#interrupt()} on that thread — {@code join()} on a thread
 * interrupting itself throws {@link InterruptedException}, which propagates out
 * of {@code main()} exactly as declared. This exercises the full startup sequence
 * (mode selection, source/telemetry construction, event loop start, shutdown hook
 * registration) without ever changing {@code GatewayMain}'s production code.
 *
 * <p><b>Deliberately NOT covered</b> (documented, not forced — see session report):
 * <ul>
 *   <li>The shutdown hook's {@code Runnable} body: it only ever runs when the JVM
 *       actually terminates, which cannot be safely triggered against a live
 *       Surefire fork mid-suite.</li>
 *   <li>The telemetry-init failure catch branch is covered below by forcing a real
 *       {@code FileNotFoundException} (nonexistent parent directory) — no mocking
 *       needed.</li>
 * </ul>
 *
 * @author FX Pipeline Team
 */
@DisplayName("GatewayMain Characterization Tests")
class GatewayMainTest {

    @TempDir
    Path tempDir;

    private Thread mainThread;
    private final AtomicReference<Throwable> uncaughtException = new AtomicReference<>();

    @BeforeEach
    void configureQueueAOverride() {
        System.setProperty("fx.queue.queue-a.path", tempDir.resolve("queue-a").toString());
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (mainThread != null) {
            // Unblocks main()'s Thread.currentThread().join() without touching production code.
            mainThread.interrupt();
            mainThread.join(5_000L);
            assertFalse(mainThread.isAlive(), "gateway-main-under-test thread must terminate after interrupt()");
        }
        System.clearProperty("fx.queue.queue-a.path");
        System.clearProperty("fx.gateway.mode");
        System.clearProperty("fx.gateway.port");
        System.clearProperty("fx.gateway.messages");
        System.clearProperty("fx.telemetry.enabled");
        System.clearProperty("fx.telemetry.log.path");
    }

    private Thread startMainInBackground() {
        final Thread t = new Thread(() -> {
            try {
                GatewayMain.main(new String[0]);
            } catch (final InterruptedException expectedShutdownSignal) {
                // Expected: tearDown() interrupts this thread to unblock main()'s join().
            }
        }, "gateway-main-under-test");
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
                fail("No event appeared on queue-a within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException (non-instantiable utility/entry class)")
    void testPrivateConstructorThrowsUnsupportedOperationException() throws Exception {
        final Constructor<GatewayMain> ctor = GatewayMain.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        final InvocationTargetException wrapped = assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, wrapped.getCause());
    }

    @Test
    @DisplayName("main() in default (synthetic) mode starts the gateway and produces decoded events on queue-a")
    void testMainSyntheticModeStartsEventLoopAndProducesEvents() throws Exception {
        System.setProperty("fx.gateway.mode", "synthetic");
        System.setProperty("fx.gateway.messages", "1000");
        System.setProperty("fx.telemetry.enabled", "false");

        mainThread = startMainInBackground();

        final String queueAPath = tempDir.resolve("queue-a").toString();
        try (ChronicleQueue readerQueue = QueueFactory.create(queueAPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent event = new FxMarketEvent();
            awaitEvent(tailer, event, 5_000L);
            assertEquals(EventStatus.RECEIVED, event.eventStatus);
            assertNotEquals(0L, event.correlationId);
        }

        assertNull(uncaughtException.get(), "main() must not throw during synthetic-mode startup");
    }

    @Test
    @DisplayName("main() in tcp mode binds a TcpFixSource on the configured port without throwing")
    void testMainTcpModeStartsWithoutError() throws Exception {
        System.setProperty("fx.gateway.mode", "tcp");
        System.setProperty("fx.gateway.port", "0");
        System.setProperty("fx.telemetry.enabled", "false");

        mainThread = startMainInBackground();

        // If startup (mode selection, TcpFixSource bind, event loop start) completes
        // without throwing, the thread reaches the blocking join() and stays alive.
        final long deadlineNanos = System.nanoTime() + 5_000L * 1_000_000L;
        while (uncaughtException.get() == null && System.nanoTime() < deadlineNanos && !mainThread.isAlive()) {
            Thread.onSpinWait();
        }
        assertTrue(mainThread.isAlive(), "main() must have reached its blocking join() without throwing");
        assertNull(uncaughtException.get(), "main() must not throw during tcp-mode startup");
    }

    @Test
    @DisplayName("main() with telemetry enabled constructs a real TelemetryRecorder and continues without error")
    void testMainTelemetryEnabledConstructsRecorder() throws Exception {
        System.setProperty("fx.gateway.mode", "synthetic");
        System.setProperty("fx.gateway.messages", "1000");
        System.setProperty("fx.telemetry.enabled", "true");
        System.setProperty("fx.telemetry.log.path", tempDir.resolve("telemetry.hlog").toString());

        mainThread = startMainInBackground();

        final String queueAPath = tempDir.resolve("queue-a").toString();
        try (ChronicleQueue readerQueue = QueueFactory.create(queueAPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent event = new FxMarketEvent();
            awaitEvent(tailer, event, 5_000L);
            assertEquals(EventStatus.RECEIVED, event.eventStatus);
        }

        assertNull(uncaughtException.get(), "main() must not throw when telemetry is successfully initialised");
    }

    @Test
    @DisplayName("main() catches a telemetry init failure (unwritable log path) and continues without telemetry")
    void testMainTelemetryInitFailureIsCaughtAndGatewayStillStarts() throws Exception {
        System.setProperty("fx.gateway.mode", "synthetic");
        System.setProperty("fx.gateway.messages", "1000");
        System.setProperty("fx.telemetry.enabled", "true");
        // Parent directories do not exist — PrintStream(File) inside TelemetryRecorder's
        // constructor throws FileNotFoundException, which GatewayMain must catch and log.
        System.setProperty("fx.telemetry.log.path",
                tempDir.resolve("no/such/nested/dir/telemetry.hlog").toString());

        mainThread = startMainInBackground();

        // The gateway must still start and produce events even though telemetry failed to init.
        final String queueAPath = tempDir.resolve("queue-a").toString();
        try (ChronicleQueue readerQueue = QueueFactory.create(queueAPath);
             ExcerptTailer tailer = readerQueue.createTailer("test-reader")) {
            final FxMarketEvent event = new FxMarketEvent();
            awaitEvent(tailer, event, 5_000L);
            assertEquals(EventStatus.RECEIVED, event.eventStatus);
        }

        assertNull(uncaughtException.get(),
                "the telemetry init failure must be caught inside main(), never propagate out");
    }
}
