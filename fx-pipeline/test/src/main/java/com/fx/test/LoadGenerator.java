package com.fx.test;

import com.fx.common.event.FxMarketEvent;
import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueueFactory;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * {@code LoadGenerator} — Garbage-free, coordinated-omission-aware test generator.
 *
 * <p>Designed to inject events at a specific target rate (e.g., 500,000 msgs/sec).
 * Pacing is achieved via a busy-spin delay loop. To mitigate coordinated omission,
 * the {@code ingressNanoTime} is set to the <em>intended</em> send time, not the
 * actual send time, which correctly propagates any queueing delay into the measured
 * tail latency rather than hiding it.
 *
 * <p><b>Sustainable Rate Guidance (Direct Mode, macOS):</b>
 * Chronicle Queue on macOS with a regular filesystem sustains approximately
 * 175,000–300,000 events/sec end-to-end. Injecting at a higher rate than the
 * pipeline can consume creates a systematic queue-a backlog where every event
 * waits a linearly growing time before being processed. The coordinated-omission
 * correction then correctly reports multi-second P50 latency — not because the
 * pipeline is slow, but because the producer is too fast.
 * Recommended benchmark rates:
 * <ul>
 *   <li><b>macOS (dev)</b>: 150,000–200,000 events/sec</li>
 *   <li><b>Linux with {@code isolcpus}/tmpfs</b>: up to 1,000,000 events/sec</li>
 * </ul>
 * Run a calibration pass first: inject at 150K/sec, observe {@code fx-latency-queue-a}
 * P50. If P50 stays below 100µs, the pipeline is keeping up. Increase until P50
 * starts growing — that inflection point is the sustainable rate for your hardware.
 *
 * <p><b>Warmup (Direct Mode):</b>
 * A short JVM warmup phase ({@value #WARMUP_EVENTS} events) is written to a
 * <em>dedicated ephemeral queue</em> under {@code /tmp/fx-warmup-*}, completely
 * separate from the measured {@code queue-a}. This warms up the JIT and Chronicle
 * mmap code paths without creating a backlog of warmup events ahead of the measured
 * events in {@code queue-a}. The previous design wrote 1,000,000 warmup events
 * directly to {@code queue-a}; at 175K events/sec effective throughput those events
 * took ~5.7 seconds to drain, contaminating the histogram with warmup-event latency.
 *
 * <p><b>Benchmark Behavior (Direct Mode):</b>
 * LoadGenerator writes directly to queue-a, entirely bypassing the real serv-0
 * gateway to push maximum throughput. No gateway processing latency is recorded.
 *
 * <p><b>TCP Mode:</b>
 * If {@code -Dfx.load.mode=tcp} is provided, it connects to {@code serv-0} over TCP
 * (port 5001 by default) and injects raw FIX messages with zero object allocation
 * on the hot path, allowing the gateway to be exercised.
 *
 * <p><b>TCP Mode — Coordinated Omission Limitation:</b>
 * In TCP mode, {@code ingressNanoTime} is <em>not</em> set by the LoadGenerator.
 * Instead, {@code serv-0} ({@link com.fx.gateway.GatewayEventLoop}) stamps it at
 * FIX decode time. This means:
 * <ul>
 *   <li>Any time the message spent waiting in the OS TCP send/receive buffer is
 *       <em>excluded</em> from the measured latency.</li>
 *   <li>The coordinated-omission correction (intended-send-time propagation) used
 *       in direct mode cannot be applied.</li>
 *   <li>Measured e2e latency in TCP mode represents gateway-decode to persistence,
 *       not client-send to persistence.</li>
 * </ul>
 * This is an accepted trade-off for TCP mode benchmarking. Use direct mode for
 * coordinated-omission-correct downstream latency measurements.
 */
public final class LoadGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LoadGenerator.class);

    /**
     * Number of warmup events written to the ephemeral warmup queue.
     *
     * <p>100K events is sufficient for C2 JIT to compile all Chronicle Queue
     * appender hot paths. Written to a separate queue so they never enter queue-a
     * and cannot create a backlog ahead of the measured run.
     */
    private static final long WARMUP_EVENTS = 100_000L;

    public static void main(final String[] args) {
        if (args.length < 2) {
            logger.error("Usage: LoadGenerator <queue-path> <target-rate-per-sec> [message-count]");
            System.exit(1);
        }

        final String queuePath = args[0];
        final long targetRate = Long.parseLong(args[1]);
        // Default to -1 (infinite) if message count is not provided
        final long messageCount = args.length > 2 ? Long.parseLong(args[2]) : -1L;
        
        final String mode = System.getProperty("fx.load.mode", "direct");
        final boolean isTcp = "tcp".equalsIgnoreCase(mode);

        logger.info(String.format("Starting LoadGenerator (Mode: %s) to [%s] at %,d msgs/sec. Target count: %s", 
                mode.toUpperCase(), queuePath, targetRate, messageCount == -1 ? "Infinite" : String.format("%,d", messageCount)));

        try (AffinityLock lock = AffinityLock.acquireLock();
             ChronicleQueue queue = !isTcp ? QueueFactory.create(queuePath) : null;
             SocketChannel socketChannel = isTcp ? createSocketChannel() : null) {

            logger.info("Locked load generator to CPU: ", lock.cpuId());

            final ExcerptAppender appender = !isTcp ? queue.createAppender() : null;
            final FxMarketEvent flyweight = new FxMarketEvent();

            // Prepare constant fields (simulating a stream of EUR/USD orders)
            final byte[] eur = {'E', 'U', 'R'};
            final byte[] usd = {'U', 'S', 'D'};
            final long eurUsdCode = FxMarketEvent.CurrencyPairCodec.encode(eur, usd);
            
            // Prepare TCP payload
            String template = "8=FIX.4.4|35=D|49=CLIENT1|55=EUR/USD|54=1|38=100000|44=1.0850|34=0000000000|";
            template = template.replace('|', '\u0001');
            byte[] fixBytes = template.getBytes(StandardCharsets.US_ASCII);
            ByteBuffer tcpBuffer = ByteBuffer.wrap(fixBytes);
            int seqOffset = template.indexOf("34=") + 3;
            
            final long intervalNanos = TimeUnit.SECONDS.toNanos(1) / targetRate;
            long intendedSendTime = System.nanoTime();

            // Warmup phase: write to a dedicated ephemeral queue, NOT to the measured
            // queue-a.  The pipeline never sees these events, so no backlog accumulates
            // ahead of the measured run.  100K events is sufficient for C2 JIT to fully
            // optimise the Chronicle appender and flyweight serialisation hot paths.
            // Skipped for small tests (< WARMUP_EVENTS) to avoid warmup dominating runtime.
            if (messageCount == -1 || messageCount >= WARMUP_EVENTS) {
                if (!isTcp) {
                    final String warmupPath = "/tmp/fx-warmup-" + System.nanoTime();
                    logger.info(String.format("Warming up JVM (%,d iterations to %s)...",
                            WARMUP_EVENTS, warmupPath));
                    try (final ChronicleQueue warmupQueue = QueueFactory.create(warmupPath)) {
                        final ExcerptAppender warmupAppender = warmupQueue.createAppender();
                        for (long i = 0; i < WARMUP_EVENTS; i++) {
                            flyweight.reset();
                            flyweight.correlationId    = -i;
                            flyweight.ingressNanoTime  = System.nanoTime();
                            flyweight.currencyPairCode = eurUsdCode;
                            flyweight.side             = 1;
                            flyweight.notionalMinorUnits = 100_000_000L;
                            warmupAppender.writeDocument(flyweight);
                        }
                    }
                    logger.info("Warmup complete.");
                } else {
                    logger.info("Skipping JVM warmup in TCP mode to prevent flooding gateway unpaced...");
                }
            } else {
                logger.info("Skipping JVM warmup phase due to small target message count...");
            }

            // Drain pause: allow any pre-existing pipeline state to settle before
            // the measured run starts.  500 ms gives a 2.5x safety margin over the
            // ~200 ms it takes the pipeline to drain at 175K events/sec with an empty
            // queue-a (no warmup backlog in queue-a thanks to the ephemeral warmup queue).
            intendedSendTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);

            long count = 0;
            long lastPrintTime = System.nanoTime();
            // Main steady-state loop (zero allocation)
            while (true) {
                final long now = System.nanoTime();
                
                // Busy-spin until the intended send time arrives
                if (now >= intendedSendTime) {
                    ++count;
                    
                    if (isTcp) {
                        long seq = count;
                        for (int i = 9; i >= 0; i--) {
                            fixBytes[seqOffset + i] = (byte) ('0' + (seq % 10));
                            seq /= 10;
                        }
                        tcpBuffer.clear();
                        while (tcpBuffer.hasRemaining()) {
                            try {
                                socketChannel.write(tcpBuffer);
                            } catch (IOException e) {
                                logger.error("TCP write failed: " + e.getMessage());
                                break;
                            }
                        }
                    } else {
                        flyweight.reset();
                        flyweight.correlationId = count;
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
                    }
                    
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
        } catch (IOException e) {
            logger.error("Failed to acquire lock or open TCP connection: " + e.getMessage());
        }
    }

    private static SocketChannel createSocketChannel() throws IOException {
        int port = Integer.getInteger("fx.load.port", 5001);
        SocketChannel channel = SocketChannel.open(new InetSocketAddress("127.0.0.1", port));
        channel.configureBlocking(true); // Blocking writes to respect network backpressure
        return channel;
    }
}
