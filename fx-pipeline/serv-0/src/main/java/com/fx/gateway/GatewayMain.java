package com.fx.gateway;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;

/**
 * {@code GatewayMain} — Entry point for serv-0 (FIX Gateway).
 *
 * <h2>Startup Sequence</h2>
 * <ol>
 *   <li>Instantiate the {@link SyntheticFixSource} (or real FIX socket reader).</li>
 *   <li>Instantiate the {@link CorrelationIdGenerator}.</li>
 *   <li>Construct the {@link GatewayEventLoop} with injected dependencies.</li>
 *   <li>Register a JVM shutdown hook for clean teardown.</li>
 *   <li>Start the event loop (spawns the pinned platform thread).</li>
 *   <li>Block the main thread indefinitely via {@link Thread#sleep}.</li>
 * </ol>
 *
 * <h2>JVM Launch Arguments</h2>
 * <p>
 * This service must be started with the following JVM flags (see parent pom.xml):
 * <pre>
 *   --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED
 *   --add-exports=java.base/sun.nio.ch=ALL-UNNAMED
 *   --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
 *   --add-opens=java.base/java.lang=ALL-UNNAMED
 *   --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
 *   --add-opens=java.base/java.io=ALL-UNNAMED
 *   --add-opens=java.base/java.util=ALL-UNNAMED
 *   -XX:+UseZGC -XX:+ZGenerational
 *   -Xmx512m -Xms512m
 *   -XX:+AlwaysPreTouch
 *   -XX:+DisableExplicitGC
 * </pre>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class GatewayMain {

    private static final Logger logger = LoggerFactory.getLogger(GatewayMain.class);

    private GatewayMain() {
        throw new UnsupportedOperationException("Main class; not instantiable");
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (not used; configuration via system properties)
     * @throws InterruptedException if the main thread sleep is interrupted
     */
    public static void main(final String[] args) throws InterruptedException {
        logger.info("[serv-0] FX Gateway starting...");
        logger.info("[serv-0] Queue A path: " + QueuePaths.QUEUE_A);

        final String mode = System.getProperty("fx.gateway.mode", "synthetic");
        final GatewayEventLoop.FixMessageSource source;

        if ("tcp".equalsIgnoreCase(mode)) {
            final int port = Integer.getInteger("fx.gateway.port", 5001);
            source = new TcpFixSource(port);
        } else {
            // Synthetic source for demonstration
            final long messagesToGenerate = Long.getLong("fx.gateway.messages", 10_000_000L);
            source = new SyntheticFixSource(messagesToGenerate);
            logger.info("[serv-0] Mode: Synthetic (generating " + messagesToGenerate + " messages)");
        }

        final CorrelationIdGenerator idGen    = new CorrelationIdGenerator();
        final GatewayEventLoop loop           = new GatewayEventLoop(source, idGen);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            logger.info("[serv-0] Shutdown signal received. Stopping event loop...");
            loop.stop();
            try {
                loop.awaitTermination();
                loop.close();
                if (source instanceof AutoCloseable) {
                    ((AutoCloseable) source).close();
                }
            } catch (final Exception e) {
                Thread.currentThread().interrupt();
            }
            logger.info("[serv-0] Event loop stopped. Queue closed.");
        }));

        loop.start();
        logger.info("[serv-0] Event loop started on CPU core ", GatewayEventLoop.CPU_CORE);

        // Block the main thread indefinitely.
        Thread.currentThread().join();
    }
}
