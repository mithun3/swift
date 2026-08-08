package com.fx.test;

import com.fx.common.event.FxMarketEvent;
import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueueFactory;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * {@code LoadGenerator} — Garbage-free, coordinated-omission-aware test generator.
 *
 * <p>Designed to flood the 'queue-a' (or act as serv-0 ingress) at a specific
 * target rate (e.g., 5,000,000 msgs/sec). Pacing is achieved via a busy-spin
 * delay loop. To mitigate coordinated omission, the {@code ingressNanoTime} is
 * set to the <em>intended</em> send time, not the actual send time, which correctly
 * pushes any queueing delay into the measured tail latency.
 */
public final class LoadGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LoadGenerator.class);

    public static void main(final String[] args) {
        if (args.length < 2) {
            logger.error("Usage: LoadGenerator <queue-path> <target-rate-per-sec> [message-count]");
            System.exit(1);
        }

        final String queuePath = args[0];
        final long targetRate = Long.parseLong(args[1]);
        // Default to -1 (infinite) if message count is not provided
        final long messageCount = args.length > 2 ? Long.parseLong(args[2]) : -1L;
        
        logger.info(String.format("Starting LoadGenerator to [%s] at %,d msgs/sec. Target count: %s", 
                queuePath, targetRate, messageCount == -1 ? "Infinite" : String.format("%,d", messageCount)));

        try (AffinityLock lock = AffinityLock.acquireLock();
             ChronicleQueue queue = QueueFactory.create(queuePath)) {

            logger.info("Locked load generator to CPU: ", lock.cpuId());

            final ExcerptAppender appender = queue.createAppender();
            final FxMarketEvent flyweight = new FxMarketEvent();

            // Prepare constant fields (simulating a stream of EUR/USD orders)
            final byte[] eur = {'E', 'U', 'R'};
            final byte[] usd = {'U', 'S', 'D'};
            final long eurUsdCode = FxMarketEvent.CurrencyPairCodec.encode(eur, usd);
            
            final long intervalNanos = TimeUnit.SECONDS.toNanos(1) / targetRate;
            long intendedSendTime = System.nanoTime();

            // Warmup phase (1 million iterations unpaced, skipped for small tests)
            final long warmupIterations = (messageCount != -1 && messageCount < 1_000_000) ? 0 : 1_000_000;
            if (warmupIterations > 0) {
                logger.info(String.format("Warming up JVM (%,d iterations)...", warmupIterations));
                for (long i = 0; i < warmupIterations; i++) {
                    flyweight.reset();
                    flyweight.correlationId = -i; // Negative ID to mark as warmup
                    flyweight.ingressNanoTime = System.nanoTime();
                    flyweight.currencyPairCode = eurUsdCode;
                    flyweight.side = 1;
                    flyweight.notionalMinorUnits = 100_000_000L;
                    appender.writeDocument(flyweight);
                }
                logger.info("Warmup complete. Starting main load test...");
            } else {
                logger.info("Skipping JVM warmup phase due to small target message count...");
            }
            
            // Re-sync intended send time after warmup
            intendedSendTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);

            long count = 0;
            long lastPrintTime = System.nanoTime();
            // Main steady-state loop (zero allocation)
            while (true) {
                final long now = System.nanoTime();
                
                // Busy-spin until the intended send time arrives
                if (now >= intendedSendTime) {
                    flyweight.reset();
                    flyweight.correlationId = ++count;
                    // COORDINATED OMISSION MITIGATION:
                    // Record intendedSendTime rather than 'now'. If the JVM paused or
                    // we fell behind, this correctly propagates the stall delay through the pipeline.
                    flyweight.ingressNanoTime = intendedSendTime;
                    flyweight.currencyPairCode = eurUsdCode;
                    flyweight.side = 1;
                    flyweight.notionalMinorUnits = 100_000_000L;
                    flyweight.clientTier = 2;
                    flyweight.clientId = 9999L;

                    appender.writeDocument(flyweight);
                    
                    // Exit condition for finite runs
                    if (messageCount != -1 && count >= messageCount) {
                        logger.info(String.format("Reached target message count of %,d. Exiting.", messageCount));
                        break;
                    }

                    // Advance to next schedule tick
                    intendedSendTime += intervalNanos;
                    
                    if (now - lastPrintTime >= 1_000_000_000L) { // Every second
                        logger.info(String.format("Sent %,d messages...", count));
                        lastPrintTime = now;
                    }
                } else {
                    // Slight yield if we're far ahead, else tight spin
                    if (intendedSendTime - now > 1000) {
                        LockSupport.parkNanos(100); // 100ns sleep
                    }
                }
            }
        }
    }
}
