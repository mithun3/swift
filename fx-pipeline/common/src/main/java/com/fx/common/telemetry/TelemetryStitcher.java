package com.fx.common.telemetry;

import com.fx.common.event.FxMarketEvent;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code TelemetryStitcher} — Out-of-band Distributed Trace Aggregator.
 *
 * <p>Tails the terminal queue (queue-c) and error queue asynchronously. 
 * Extracts the latency timestamps (t1, t2, t3) from the {@link FxMarketEvent} 
 * and formats them into a standard JSON trace log.
 * 
 * <p>This runs on a non-critical background thread, ensuring absolutely zero 
 * GC allocation or thread contention impacts the hot path.
 */
public final class TelemetryStitcher implements AutoCloseable {

    private final ChronicleQueue sourceQueue;
    private final ExcerptTailer tailer;
    private final PrintWriter logWriter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread tailerThread;
    
    // We pre-allocate a single flyweight for the tailer to read into
    private final FxMarketEvent eventFlyweight = new FxMarketEvent();

    /**
     * @param queuePath Path to the queue to tail (e.g. queue-c)
     * @param logPath Path to the output JSON log file
     * @throws IOException if log file cannot be created
     */
    public TelemetryStitcher(final String queuePath, final String logPath) throws IOException {
        this.sourceQueue = net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
                .binary(queuePath)
                .build();
        this.tailer = this.sourceQueue.createTailer("telemetry-tailer");
        
        // Ensure log directory exists
        Files.createDirectories(Paths.get(logPath).getParent());
        
        // Auto-flushing print writer for JSON lines
        this.logWriter = new PrintWriter(new FileWriter(logPath, true), true);
        
        this.tailerThread = new Thread(this::tailLoop, "telemetry-stitcher");
        this.tailerThread.setDaemon(true);
        this.tailerThread.start();
    }

    private void tailLoop() {
        while (running.get()) {
            eventFlyweight.reset();
            final boolean read = tailer.readDocument(eventFlyweight);
            if (read) {
                // Generate and write JSON trace
                writeTrace(eventFlyweight);
            } else {
                // Backoff when idle
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void writeTrace(FxMarketEvent event) {
        // Simple JSON formatting for the distributed trace.
        // This allocates Strings, but it is acceptable because it's completely out-of-band.
        
        long totalLatency = 0;
        if (event.t3ServCEntry > 0 && event.ingressNanoTime > 0) {
            totalLatency = event.t3ServCEntry - event.ingressNanoTime;
        }

        String json = String.format(
            "{\"traceId\":\"%d\", \"type\":\"FxMarketEvent\", \"totalLatencyNs\":%d, " +
            "\"spans\":[" +
                "{\"service\":\"serv-0\", \"timestampNs\":%d}, " +
                "{\"service\":\"serv-a\", \"timestampNs\":%d}, " +
                "{\"service\":\"serv-b\", \"timestampNs\":%d}, " +
                "{\"service\":\"serv-c\", \"timestampNs\":%d}" +
            "], " +
            "\"status\":%d}",
            event.correlationId,
            totalLatency,
            event.ingressNanoTime,
            event.t1ServAEntry,
            event.t2ServBEntry,
            event.t3ServCEntry,
            event.eventStatus
        );
        logWriter.println(json);
    }

    @Override
    public void close() {
        running.set(false);
        tailerThread.interrupt();
        try {
            tailerThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sourceQueue.close();
        logWriter.close();
    }
}
