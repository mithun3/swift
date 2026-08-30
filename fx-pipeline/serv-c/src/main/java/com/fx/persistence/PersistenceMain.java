package com.fx.persistence;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;
import com.fx.common.telemetry.TelemetryBootstrap;
import com.fx.common.telemetry.TelemetryRecorder;
import org.h2.tools.Server;

import java.io.File;
import java.sql.SQLException;

/**
 * {@code PersistenceMain} — Entry point for serv-c (Persistence and Egress Service).
 *
 * <h2>Startup Sequence</h2>
 * <ol>
 *   <li>Start the H2 TCP server for external JDBC inspection of in-flight trade data.</li>
 *   <li>Optionally construct a {@link TelemetryRecorder} backed by HdrHistogram to
 *       record end-to-end pipeline latencies without allocations on the hot path.</li>
 *   <li>Construct {@link PersistenceEventLoop} with the JDBC URL and optional recorder.</li>
 *   <li>Register a JVM shutdown hook to flush the batch buffer and close all resources.</li>
 *   <li>Start the event loop (spawns the pinned platform thread).</li>
 *   <li>Block the main thread indefinitely via {@link Thread#join}.</li>
 * </ol>
 *
 * <h2>Telemetry Configuration</h2>
 * <p>
 * Telemetry is enabled by default. Set system property
 * {@code fx.telemetry.enabled=false} to disable it (e.g., in tests where the
 * HdrHistogram log file path would conflict). The output log file path is
 * controlled by {@code fx.telemetry.log.path} (default: {@code /tmp/fx-latency.hlog}).
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class PersistenceMain {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceMain.class);

    /** Highest trackable latency for the serv-c telemetry histograms: 10 seconds in nanos. */
    private static final long TELEMETRY_HIGHEST_LATENCY_NANOS = 10_000_000_000L;

    /** Background flush interval for the serv-c telemetry recorders. */
    private static final long TELEMETRY_FLUSH_INTERVAL_MILLIS = 1_000L;

    /**
     * TCP port for the H2 external-inspection server. Override at runtime with
     * {@code -Dfx.persistence.h2.tcp.port=<port>} (e.g. to run multiple instances
     * side-by-side in tests without a fixed-port bind conflict). Default unchanged: 9092.
     */
    private static final String H2_TCP_PORT = System.getProperty("fx.persistence.h2.tcp.port", "9092");

    private PersistenceMain() {
        throw new UnsupportedOperationException("Main class — not instantiable");
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used; configuration via system properties)
     * @throws InterruptedException if the main thread join is interrupted
     * @throws SQLException         if the database or H2 server cannot be initialised
     */
    public static void main(final String[] args) throws InterruptedException, SQLException {
        logger.info("[serv-c] Persistence Service starting...");
        logger.info("[serv-c] Tailing queue-c: " + QueuePaths.QUEUE_C);
        logger.info("[serv-c] JDBC URL: " + PersistenceEventLoop.DEFAULT_JDBC_URL);

        // Start the H2 TCP server to allow external clients to connect to the in-memory database.
        // This enables real-time inspection of persisted trades without stopping the service.
        final Server h2Server = Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-tcpPort", H2_TCP_PORT).start();
        logger.info("[serv-c] H2 TCP Server started on port " + H2_TCP_PORT + ".");
        logger.info("[serv-c] External JDBC URL: jdbc:h2:tcp://localhost:" + H2_TCP_PORT + "/mem:fxdb");

        // ── Telemetry Recorder (GAP-5) ────────────────────────────────────────
        // The TelemetryRecorder uses HdrHistogram's SingleWriterRecorder to capture
        // end-to-end latencies (T3 - T0) on the hot path without any allocation.
        // A background daemon thread flushes interval histograms to an .hlog file
        // every second for offline visualisation via scripts/process_latency.sh.
        final boolean telemetryEnabled = TelemetryBootstrap.isEnabled();
        final String telemetryLogPath = TelemetryBootstrap.logPath();

        TelemetryRecorder e2eRecorder = null;
        TelemetryRecorder queueCRecorder = null;
        TelemetryRecorder servCRecorder = null;

        if (telemetryEnabled) {
            try {
                // highestTrackableValue: 10 seconds in nanoseconds (covers extreme outliers).
                // intervalMillis: flush histogram to disk every 1000ms.
                final String basePath = TelemetryBootstrap.basePath();
                
                e2eRecorder = new TelemetryRecorder(
                        new File(telemetryLogPath), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                queueCRecorder = new TelemetryRecorder(
                        new File(basePath + "-queue-c.hlog"), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                servCRecorder = new TelemetryRecorder(
                        new File(basePath + "-serv-c.hlog"), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                        
                logger.info("[serv-c] Telemetry enabled. Writing latency logs to: " + basePath + "*");
            } catch (final Exception e) {
                logger.warn("[serv-c] WARNING: Failed to init TelemetryRecorders: "
                        + e.getMessage() + " — continuing without telemetry.");
            }
        } else {
            logger.info("[serv-c] Telemetry disabled (fx.telemetry.enabled=false).");
        }

        // ── Event Loop Construction ───────────────────────────────────────────
        final PersistenceEventLoop loop = new PersistenceEventLoop(
                PersistenceEventLoop.DEFAULT_JDBC_URL, e2eRecorder, queueCRecorder, servCRecorder);

        // ── Shutdown Hook ─────────────────────────────────────────────────────
        final TelemetryRecorder finalE2e = e2eRecorder;
        final TelemetryRecorder finalQueueCRecorder = queueCRecorder;
        final TelemetryRecorder finalServCRecorder = servCRecorder;
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            logger.info("[serv-c] Shutdown signal received.");
            loop.stop();
            try {
                loop.awaitTermination();
                loop.close();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Close the TelemetryRecorders — flushes the final interval histograms to disk.
            if (finalE2e != null) finalE2e.close();
            if (finalQueueCRecorder != null) finalQueueCRecorder.close();
            if (finalServCRecorder != null) finalServCRecorder.close();
            
            if (finalE2e != null) {
                logger.info("[serv-c] Telemetry flushed to: " + telemetryLogPath + "*");
            }
            h2Server.stop();
            logger.info("[serv-c] Stopped. All batches flushed. H2 Server stopped.");
        }));

        loop.start();
        logger.info("[serv-c] Event loop started on CPU core ", PersistenceEventLoop.CPU_CORE);

        // Block the main thread indefinitely — the event loop runs on its own pinned thread.
        loop.awaitTermination();
        loop.throwIfTerminatedUnexpectedly();
    }
}
