package com.fx.pricing;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;

/**
 * {@code PricingMain} — Entry point for serv-b (Pricing Engine).
 * @author FX Pipeline Team
 */
public final class PricingMain {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingMain.class);
    
    private PricingMain() { throw new UnsupportedOperationException("Main class"); }

    public static void main(final String[] args) throws InterruptedException {
        logger.info("[serv-b] Pricing Engine starting...");
        logger.info("[serv-b] Tailing queue-b: " + QueuePaths.QUEUE_B);
        logger.info("[serv-b] Writing queue-c: " + QueuePaths.QUEUE_C);

        final PricingEventLoop loop = new PricingEventLoop();

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            logger.info("[serv-b] Shutdown signal received.");
            loop.stop();
            try { loop.awaitTermination(); loop.close(); }
            catch (final InterruptedException e) { Thread.currentThread().interrupt(); }
            logger.info("[serv-b] Stopped.");
        }));

        loop.start();
        logger.info("[serv-b] Event loop started on CPU core ", PricingEventLoop.CPU_CORE);
        Thread.currentThread().join();
    }
}
