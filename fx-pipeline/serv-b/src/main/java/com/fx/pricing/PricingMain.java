package com.fx.pricing;

import com.fx.common.queue.QueuePaths;

/**
 * {@code PricingMain} — Entry point for serv-b (Pricing Engine).
 * @author FX Pipeline Team
 */
public final class PricingMain {
    private PricingMain() { throw new UnsupportedOperationException("Main class"); }

    public static void main(final String[] args) throws InterruptedException {
        System.out.println("[serv-b] Pricing Engine starting...");
        System.out.println("[serv-b] Tailing queue-b: " + QueuePaths.QUEUE_B);
        System.out.println("[serv-b] Writing queue-c: " + QueuePaths.QUEUE_C);

        final PricingEventLoop loop = new PricingEventLoop();

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            System.out.println("[serv-b] Shutdown signal received.");
            loop.stop();
            try { loop.awaitTermination(); loop.close(); }
            catch (final InterruptedException e) { Thread.currentThread().interrupt(); }
            System.out.println("[serv-b] Stopped.");
        }));

        loop.start();
        System.out.println("[serv-b] Event loop started on CPU core " + PricingEventLoop.CPU_CORE);
        Thread.currentThread().join();
    }
}
