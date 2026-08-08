package com.fx.common.telemetry;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.SingleWriterRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code TelemetryRecorder} — Zero-allocation, concurrent latency recorder.
 *
 * <p>Uses {@link SingleWriterRecorder} to record latencies on the hot path without
 * locks or garbage creation. A background thread periodically harvests the interval
 * histograms and writes them to an HdrHistogram log ({@code .hlog}) file.
 */
public final class TelemetryRecorder implements AutoCloseable {

    private final SingleWriterRecorder recorder;
    private final Thread backgroundThread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final HistogramLogWriter logWriter;
    private final long intervalMillis;

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
        this.logWriter = new HistogramLogWriter(new PrintStream(logFile));
        
        // Output standard HdrHistogram log header
        this.logWriter.outputLogFormatVersion();
        this.logWriter.outputLegend();
        this.logWriter.outputBaseTime(System.currentTimeMillis());
        this.logWriter.outputStartTime(System.currentTimeMillis());

        this.backgroundThread = new Thread(this::flushLoop, "telemetry-flusher");
        this.backgroundThread.setDaemon(true);
        this.backgroundThread.start();
    }

    /**
     * Records a latency value. Must be called by a SINGLE thread (the hot path).
     *
     * @param value The value to record (e.g., latency in nanoseconds).
     */
    public void recordValue(final long value) {
        recorder.recordValue(value);
    }

    private void flushLoop() {
        Histogram intervalHistogram = null;
        while (running.get()) {
            try {
                Thread.sleep(intervalMillis);
                // getIntervalHistogram() safely swaps the underlying histogram structures
                // and returns the inactive one, populated with the latest interval's data.
                intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
                
                if (intervalHistogram.getTotalCount() > 0) {
                    final double startTimeSec = (System.currentTimeMillis() - intervalHistogram.getStartTimeStamp()) / 1000.0;
                    final double endTimeSec = startTimeSec + (intervalMillis / 1000.0);
                    // Write to the .hlog file (this allocates strings and does I/O, but it's on a background thread)
                    logWriter.outputIntervalHistogram(startTimeSec, endTimeSec, intervalHistogram);
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
        backgroundThread.interrupt();
        try {
            backgroundThread.join(2000);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Flush any remaining data
        Histogram intervalHistogram = recorder.getIntervalHistogram();
        if (intervalHistogram.getTotalCount() > 0) {
            final double startTimeSec = (System.currentTimeMillis() - intervalHistogram.getStartTimeStamp()) / 1000.0;
            final double endTimeSec = startTimeSec + (intervalMillis / 1000.0);
            logWriter.outputIntervalHistogram(startTimeSec, endTimeSec, intervalHistogram);
        }
    }
}
