package com.fx.pricing;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;
import com.fx.common.telemetry.TelemetryBootstrap;
import com.fx.common.telemetry.TelemetryRecorder;

import java.io.File;

/**
 * {@code PricingMain} — Entry point for serv-b (Pricing Engine).
 * @author FX Pipeline Team
 */
public final class PricingMain {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingMain.class);

    /** Highest trackable latency for the serv-b telemetry histograms: 10 seconds in nanos. */
    private static final long TELEMETRY_HIGHEST_LATENCY_NANOS = 10_000_000_000L;

    /** Background flush interval for the serv-b telemetry recorders. */
    private static final long TELEMETRY_FLUSH_INTERVAL_MILLIS = 1_000L;

    private PricingMain() { throw new UnsupportedOperationException("Main class"); }

    public static void main(final String[] args) throws InterruptedException {
        logger.info("[serv-b] Pricing Engine starting...");
        logger.info("[serv-b] Tailing queue-b: " + QueuePaths.QUEUE_B);
        logger.info("[serv-b] Writing queue-c: " + QueuePaths.QUEUE_C);

        final boolean telemetryEnabled = TelemetryBootstrap.isEnabled();

        TelemetryRecorder queueBRecorder = null;
        TelemetryRecorder servBRecorder = null;
        if (telemetryEnabled) {
            try {
                final String basePath = TelemetryBootstrap.basePath();
                queueBRecorder = new TelemetryRecorder(
                        new File(basePath + "-queue-b.hlog"), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                servBRecorder = new TelemetryRecorder(
                        new File(basePath + "-serv-b.hlog"), TELEMETRY_HIGHEST_LATENCY_NANOS, TELEMETRY_FLUSH_INTERVAL_MILLIS);
                logger.info("[serv-b] Telemetry enabled. Writing latency logs to: " + basePath + "-{queue-b,serv-b}.hlog");
            } catch (final Exception e) {
                logger.warn("[serv-b] WARNING: Failed to init TelemetryRecorders: "
                        + e.getMessage() + " — continuing without telemetry.");
            }
        }

        final PricingEventLoop loop = new PricingEventLoop(queueBRecorder, servBRecorder);

        final TelemetryRecorder finalQueueB = queueBRecorder;
        final TelemetryRecorder finalServB = servBRecorder;
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            logger.info("[serv-b] Shutdown signal received.");
            loop.stop();
            try { loop.awaitTermination(); loop.close(); }
            catch (final InterruptedException e) { Thread.currentThread().interrupt(); }
            if (finalQueueB != null) finalQueueB.close();
            if (finalServB != null) finalServB.close();
            logger.info("[serv-b] Stopped.");
        }));

        loop.start();
        logger.info("[serv-b] Event loop started on CPU core ", PricingEventLoop.CPU_CORE);
        Thread.currentThread().join();
    }
}
