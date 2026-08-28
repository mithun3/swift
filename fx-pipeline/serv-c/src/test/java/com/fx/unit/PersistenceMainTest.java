package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.queue.QueueFactory;
import com.fx.persistence.PersistenceEventLoop;
import com.fx.persistence.PersistenceMain;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link PersistenceMain}, the serv-c entry point.
 *
 * <h2>Testing {@code main()} without a production refactor</h2>
 * <p>
 * As established for {@code GatewayMainTest} (serv-0), {@code RiskMainTest} (serv-a), and
 * {@code PricingMainTest} (serv-b), {@code main()} blocks the calling thread indefinitely
 * via {@code Thread.currentThread().join()}. This class runs {@code main()} on a dedicated
 * background thread and unblocks it deterministically at teardown via {@link Thread#interrupt()}.
 *
 * <h2>Unique constraint for serv-c: only ONE {@code main()} invocation per JVM fork</h2>
 * <p>
 * Unlike the other three {@code *Main} classes, {@code PersistenceMain.main()} also starts
 * an H2 TCP server via {@code Server.createTcpServer(...).start()}, on a port controlled by
 * {@code -Dfx.persistence.h2.tcp.port} (default {@code 9092}; the property was added in
 * Phase 2 specifically to investigate unblocking multi-invocation testing). The returned
 * {@code Server} handle is a local variable inside {@code main()} — it is only ever stopped
 * from inside the JVM shutdown-hook {@code Runnable}, which does not run just because a
 * test's background thread is interrupted (shutdown hooks only run at real JVM exit).
 *
 * <p><b>Empirically confirmed (not just a port-9092 rebind conflict):</b> even with each test
 * using a distinct port, a <em>second</em> {@code Server.createTcpServer(...).start()} call
 * within the same JVM fork hangs indefinitely (verified with a 20-second bound) as long as an
 * earlier leaked server from a prior {@code main()} invocation is still running — the H2
 * {@code Server} start-up path evidently serialises on some JVM-global state that is only
 * released by {@code Server.stop()}, not by the calling thread being interrupted. This is a
 * deeper constraint than the hardcoded-port issue alone, and making the port configurable does
 * not unblock it. Reaching the {@code fx.telemetry.enabled=false} branch or the telemetry-init
 * -failure {@code catch (Exception)} branch would require either a second JVM fork per
 * scenario or exposing/stopping the {@code Server}/{@code PersistenceEventLoop} handles from
 * {@code main()} — both out of scope for a characterization-only pass. This class therefore
 * invokes {@code main()} at most ONCE (see {@link #testMainStartsEventLoopAndPersistsTelemetryEvent()}).
 *
 * <p><b>Deliberately NOT covered</b> (documented, not forced):
 * <ul>
 *   <li>The {@code fx.telemetry.enabled=false} branch and the telemetry-init-failure
 *       {@code catch (Exception)} branch — see above.</li>
 *   <li>The shutdown hook's {@code catch (InterruptedException)} body: never thrown on the
 *       normal path — {@code awaitTermination()} completes without interruption when the
 *       Surefire fork exits and all still-registered shutdown hooks run.</li>
 *   <li>{@code main()}'s own blocking {@code Thread.currentThread().join()} line and its
 *       closing brace: the only path through is via an injected {@code interrupt()}, never
 *       a normal return, so JaCoCo does not mark them covered even though they are reached —
 *       same documented deferred exception as {@code GatewayMain}/{@code RiskMain}/{@code PricingMain}.</li>
 * </ul>
 *
 * @author FX Pipeline Team
 */
@DisplayName("PersistenceMain Characterization Tests")
class PersistenceMainTest {

    @TempDir
    Path tempDir;

    private Thread mainThread;
    private final AtomicReference<Throwable> uncaughtException = new AtomicReference<>();

    @BeforeEach
    void configureQueueOverride() throws IOException {
        System.setProperty("fx.queue.queue-c.path", tempDir.resolve("queue-c").toString());
        // Pre-create the queue directory: QueueFactory.create()'s "!exists() && !mkdirs()"
        // check is not atomic, so the test's writer thread and main()'s background thread
        // constructing their own ChronicleQueue against a fresh, not-yet-existing temp
        // directory can race on the same mkdirs() call.
        Files.createDirectories(tempDir.resolve("queue-c"));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (mainThread != null) {
            mainThread.interrupt();
            mainThread.join(5_000L);
            assertFalse(mainThread.isAlive(), "persistence-main-under-test thread must terminate after interrupt()");
        }
        System.clearProperty("fx.queue.queue-c.path");
        System.clearProperty("fx.telemetry.enabled");
        System.clearProperty("fx.telemetry.log.path");
        System.clearProperty("fx.persistence.h2.tcp.port");
    }

    private Thread startMainInBackground() {
        final Thread t = new Thread(() -> {
            try {
                PersistenceMain.main(new String[0]);
            } catch (final InterruptedException expectedShutdownSignal) {
                // Expected: tearDown() interrupts this thread to unblock main()'s join().
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }, "persistence-main-under-test");
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thread, ex) -> uncaughtException.set(ex));
        t.start();
        return t;
    }

    private void writeToQueueC(final String queueCPath, final FxMarketEvent event) {
        try (ChronicleQueue writerQueue = QueueFactory.create(queueCPath);
             ExcerptAppender appender = writerQueue.createAppender()) {
            appender.writeDocument(event);
        }
    }

    /** Bounded poll for a persisted row — fails fast on regression instead of hanging the suite. */
    private long awaitRowCount(final String jdbcUrl, final long correlationId, final long timeoutMillis)
            throws SQLException {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (true) {
            try (Connection c = DriverManager.getConnection(jdbcUrl, "sa", "");
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT COUNT(*) FROM fx_trades WHERE correlation_id = " + correlationId)) {
                if (rs.next() && rs.getLong(1) > 0) {
                    return rs.getLong(1);
                }
            } catch (final SQLException tableNotYetCreated) {
                // BatchPersistenceEngine's initSchema() may not have run yet on the
                // background main() thread — transient during startup, keep polling.
            }
            if (System.nanoTime() > deadlineNanos) {
                fail("No row for correlationId=" + correlationId + " appeared within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException (non-instantiable utility/entry class)")
    void testPrivateConstructorThrowsUnsupportedOperationException() throws Exception {
        final Constructor<PersistenceMain> ctor = PersistenceMain.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        final InvocationTargetException wrapped = assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, wrapped.getCause());
    }

    @Test
    @DisplayName("main() starts the H2 server + event loop with telemetry enabled and persists a tailed event")
    void testMainStartsEventLoopAndPersistsTelemetryEvent() throws Exception {
        System.setProperty("fx.telemetry.enabled", "true");
        System.setProperty("fx.telemetry.log.path", tempDir.resolve("telemetry.hlog").toString());
        System.setProperty("fx.persistence.h2.tcp.port", "19092");

        mainThread = startMainInBackground();

        final String queueCPath = tempDir.resolve("queue-c").toString();

        // A unique correlationId — main() always persists to the fixed, class-load-resolved
        // PersistenceEventLoop.DEFAULT_JDBC_URL (a static final field, not safely overridable
        // per test class; read it directly rather than assuming an override "won" — see the
        // repo's QueuePaths.QUEUE_ERR lesson, same static-final-field gotcha applies here).
        final FxMarketEvent outbound = new FxMarketEvent();
        outbound.reset();
        outbound.correlationId = 900_001L;
        outbound.eventStatus = EventStatus.PRICED;
        outbound.clientTier = 2;
        outbound.notionalMinorUnits = 300_000L;
        writeToQueueC(queueCPath, outbound);

        assertEquals(1L, awaitRowCount(PersistenceEventLoop.DEFAULT_JDBC_URL, 900_001L, 5_000L),
                "main() must tail queue-c and persist the event to the database");

        assertNull(uncaughtException.get(), "main() must not throw during telemetry-enabled startup");
        assertEquals(3, PersistenceEventLoop.CPU_CORE, "CPU_CORE constant must remain 3 (isolated from other services)");
    }
}
