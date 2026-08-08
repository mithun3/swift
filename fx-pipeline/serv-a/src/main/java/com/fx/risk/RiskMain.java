package com.fx.risk;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;

/**
 * {@code RiskMain} — Entry point for serv-a (Risk Validation Service).
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class RiskMain {

    private static final Logger logger = LoggerFactory.getLogger(RiskMain.class);

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

        final RiskValidationEventLoop loop = new RiskValidationEventLoop();

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            logger.info("[serv-a] Shutdown signal received.");
            loop.stop();
            try {
                loop.awaitTermination();
                loop.close();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.info("[serv-a] Stopped.");
        }));

        loop.start();
        logger.info("[serv-a] Event loop started on CPU core ", RiskValidationEventLoop.CPU_CORE);
        Thread.currentThread().join();
    }
}
