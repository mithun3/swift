package com.fx.risk;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;
import com.fx.common.telemetry.TelemetryBootstrap;
import com.fx.common.telemetry.TelemetryRecorder;

import java.io.File;

/**
 * {@code RiskMain} — Entry point for serv-a (Risk Validation Service).
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class RiskMain {

    private static final Logger logger = LoggerFactory.getLogger(RiskMain.class);

    /** Highest trackable latency for the serv-a telemetry histograms: 10 seconds in nanos. */
    private static final long TELEMETRY_HIGHEST_LATENCY_NANOS = 10_000_000_000L;

    /** Background flush interval for the serv-a telemetry recorders. */
    private static final long TELEMETRY_FLUSH_INTERVAL_MILLIS = 1_000L;

    private RiskMain() {
        throw new UnsupportedOperationException("Main class; not instantiable");
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used)
     * @throws InterruptedException if the main thread is interrupted
     */
    public static void main(final String[] args) throws InterruptedException {
        logger.info("[serv-a] Risk Validation Service starting...");
        logger.info("[serv-a] Tailing queue-a: " + QueuePaths.QUEUE_A);
        logger.info("[serv-a] Writing queue-b: " + QueuePaths.QUEUE_B);

        final boolean telemetryEnabled = TelemetryBootstrap.isEnabled();

        TelemetryRecorder queueARecorder = null;
        TelemetryRecorder servARecorder = null;
        if (telemetryEnabled) {
            try {
                final String basePath = TelemetryBootstrap.basePath();
                queueARecorder = new TelemetryRecorder(
                        new File(basePath + "-queue-a.hlog"), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                servARecorder = new TelemetryRecorder(
                        new File(basePath + "-serv-a.hlog"), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                logger.info("[serv-a] Telemetry enabled. Writing latency logs to: " + basePath + "-{queue-a,serv-a}.hlog");
            } catch (final Exception e) {
                logger.warn("[serv-a] WARNING: Failed to init TelemetryRecorders: "
                        + e.getMessage() + " — continuing without telemetry.");
            }
        }

        final RiskValidationEventLoop loop = new RiskValidationEventLoop(queueARecorder, servARecorder);

        final TelemetryRecorder finalQueueA = queueARecorder;
        final TelemetryRecorder finalServA = servARecorder;
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            logger.info("[serv-a] Shutdown signal received.");
            loop.stop();
            try {
                loop.awaitTermination();
                loop.close();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (finalQueueA != null) finalQueueA.close();
            if (finalServA != null) finalServA.close();
            logger.info("[serv-a] Stopped.");
        }));

        loop.start();
        logger.info("[serv-a] Event loop started on CPU core ", RiskValidationEventLoop.CPU_CORE);
        Thread.currentThread().join();
    }
}
