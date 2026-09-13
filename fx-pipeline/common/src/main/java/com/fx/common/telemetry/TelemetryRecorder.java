package com.fx.common.telemetry;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.SingleWriterRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * {@code TelemetryRecorder} — Zero-allocation, concurrent latency recorder.
 *
 * <p>Uses {@link SingleWriterRecorder} to record latencies on the hot path without
 * locks or garbage creation. A background thread periodically harvests the interval
 * histograms and writes them to an HdrHistogram log ({@code .hlog}) file.
 */
public final class TelemetryRecorder implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(TelemetryRecorder.class.getName());

    private final SingleWriterRecorder recorder;
    private final Thread backgroundThread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final long intervalMillis;
    private final File logFile;

    private HistogramLogWriter logWriter;
    /** Retained so we can explicitly flush and close the stream on shutdown. */
    private PrintStream printStream;

    /**
     * Creates a new TelemetryRecorder.
     *
     * @param logFile        The .hlog file to write to.
     * @param highestValue   The highest trackable latency (e.g., 10_000_000_000L for 10s in nanos).
     * @param intervalMillis The polling interval for the background thread.
     */
    public TelemetryRecorder(final File logFile, final long highestValue, final long intervalMillis) throws FileNotFoundException {
        // 3 significant digits provide ~0.1% accuracy.
        this.recorder = new SingleWriterRecorder(highestValue, 3);
        this.intervalMillis = intervalMillis;
        this.logFile = logFile;
        
        openStreamAndWriteHeader();

        if ("on_close".equals(TelemetryBootstrap.flushMode())) {
            this.backgroundThread = null;
        } else {
            this.backgroundThread = new Thread(this::flushLoop, "telemetry-flusher");
            this.backgroundThread.setDaemon(true);
            this.backgroundThread.start();
        }
    }
    
    private void openStreamAndWriteHeader() throws FileNotFoundException {
        this.printStream = new PrintStream(logFile);
        this.logWriter = new HistogramLogWriter(this.printStream);

        // Output standard HdrHistogram log header.
        // Restoring base time / start time headers to prevent NullPointerException
        // in HistogramLogProcessor (v2.2.2) when parsing log files that contain no intervals.
        this.logWriter.outputLogFormatVersion();
        this.logWriter.outputLegend();
        this.logWriter.outputBaseTime(System.currentTimeMillis());
        this.logWriter.outputStartTime(System.currentTimeMillis());
    }

    /**
     * Records a latency value. Must be called by a SINGLE thread (the hot path).
     *
     * @param value The value to record (e.g., latency in nanoseconds).
     */
    public void recordValue(final long value) {
        try {
            recorder.recordValue(value);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // Swallow telemetry exceptions gracefully to avoid disrupting
            // the main processing loop or causing false event rejections.
        }
    }

    private void flushLoop() {
        Histogram intervalHistogram = null;
        while (running.get()) {
            try {
                Thread.sleep(intervalMillis);
                
                // If the benchmark script deleted the file to clear warmup data, reopen it.
                if (!logFile.exists()) {
                    if (printStream != null) {
                        printStream.close();
                    }
                    openStreamAndWriteHeader();
                }

                // getIntervalHistogram() safely swaps the underlying histogram structures
                // and returns the inactive one, populated with the latest interval's data.
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                
                if (intervalHistogram.getTotalCount() > 0) {
                    // Use the single-argument overload — it reads startTimeStamp and
                    // endTimeStamp directly from the histogram's own internal fields,
                    // which are maintained correctly by SingleWriterRecorder.
                    // The previous 3-arg overload required computing a wall-clock offset
                    // from a base time, which produced near-zero values and collapsed
                    // all percentiles to 0.00 µs.
                    logWriter.outputIntervalHistogram(intervalHistogram);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (backgroundThread != null) {
            backgroundThread.interrupt();
            try {
                backgroundThread.join(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Flush the final partial interval — data recorded since the last background flush.
        // This is the critical step: without it, the last interval's samples are lost.
        final Histogram intervalHistogram = recorder.getIntervalHistogram();
        if (intervalHistogram.getTotalCount() > 0) {
            logWriter.outputIntervalHistogram(intervalHistogram);
        }

        // Explicitly flush and close the underlying PrintStream.
        // PrintStream is internally buffered — without this, any bytes not yet written
        // to the OS page cache will be silently lost when the JVM exits.
        logWriter.outputComment("TelemetryRecorder closed — all intervals flushed.");
        if (printStream != null) {
            printStream.flush();
            printStream.close();
        }
    }
}
