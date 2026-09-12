package com.fx.common.telemetry;

import org.HdrHistogram.EncodableHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * {@code IntervalHistogramExporter} — reads a benchmark {@code .hlog} file and exports
 * each recorded interval's percentiles to CSV, with elapsed-time offsets.
 *
 * <p>Aggregate {@code .hgrm} percentiles (produced by
 * {@code org.HdrHistogram.HistogramLogProcessor}, see {@code scripts/process_latency.sh})
 * answer "how bad did the tail get across the whole run", but cannot say *when* within a
 * 200s run a latency spike happened. This tool answers that: each output row is one
 * flush interval (one row per {@code TELEMETRY_FLUSH_INTERVAL_MILLIS}, typically 1s) with
 * its own percentiles and its elapsed-seconds-since-recording-started offset, so a spike
 * can be matched directly against a service's GC log (which uses the same "elapsed
 * seconds since JVM start" clock via the {@code uptime} `-Xlog` decorator) instead of
 * guessing from the aggregate distribution.
 *
 * <p>Deliberately does NOT use {@link Histogram#getStartTimeStamp()}/
 * {@link Histogram#getEndTimeStamp()} for this: {@code HistogramLogReader}'s
 * reconstruction of those absolute epoch-millis fields from the log's base/start-time
 * header was found to be unreliable (resolved to roughly double the real epoch time in
 * manual testing against archived benchmark logs), while the interval-to-interval
 * *duration* those same fields produce is correct. Elapsed time is accumulated instead
 * from each interval's own reliable duration, sidestepping the bad absolute value
 * entirely rather than depending on a library quirk that isn't fully understood.
 *
 * <p>Startup-only tool (not on the event-loop hot path); ordinary I/O and object
 * allocation here carry no zero-GC implications.
 */
public final class IntervalHistogramExporter {

    private IntervalHistogramExporter() {
        throw new UnsupportedOperationException("Utility class; not instantiable");
    }

    /**
     * Command-line entry point.
     *
     * @param args {@code <input.hlog> <output.csv>}
     */
    public static void main(final String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: IntervalHistogramExporter <input.hlog> <output.csv>");
            System.exit(1);
            return;
        }
        final int rowsWritten = export(new File(args[0]), new File(args[1]));
        System.out.println("Wrote " + rowsWritten + " interval row(s) to " + args[1]);
    }

    /**
     * Exports every interval histogram in {@code inputHlog} to {@code outputCsv}.
     *
     * <p>Tolerant of a truncated final entry (e.g. a force-killed service, see
     * {@code LATENCY_RCA.md}'s Docker findings) — whatever intervals were readable before
     * the failure are still written, rather than losing the whole file's data.
     *
     * @return the number of interval rows written
     */
    public static int export(final File inputHlog, final File outputCsv) throws IOException {
        int rowsWritten = 0;
        double elapsedSec = 0.0;
        try (HistogramLogReader reader = new HistogramLogReader(inputHlog.getPath());
             PrintWriter writer = new PrintWriter(new FileWriter(outputCsv))) {
            writer.println("interval_index,elapsed_start_sec,elapsed_end_sec,duration_sec,"
                    + "count,p50_ns,p90_ns,p95_ns,p99_ns,p999_ns,p9999_ns,max_ns");
            try {
                EncodableHistogram next;
                while ((next = reader.nextIntervalHistogram()) != null) {
                    if (!(next instanceof Histogram) || next.getEndTimeStamp() <= 0) {
                        continue;
                    }
                    final Histogram histogram = (Histogram) next;
                    if (histogram.getTotalCount() == 0) {
                        continue;
                    }
                    final double durationSec = (histogram.getEndTimeStamp() - histogram.getStartTimeStamp()) / 1000.0;
                    writeRow(writer, rowsWritten, elapsedSec, elapsedSec + durationSec, durationSec, histogram);
                    elapsedSec += durationSec;
                    rowsWritten++;
                }
            } catch (final RuntimeException truncated) {
                // A force-killed service (SIGKILL) can leave a partially-written final
                // interval; keep everything read so far instead of discarding the file.
                System.err.println("Warning: " + inputHlog + " ended early (" + truncated
                        + ") — exported " + rowsWritten + " interval(s) before the truncation.");
            }
        }
        return rowsWritten;
    }

    private static void writeRow(final PrintWriter writer, final int index, final double elapsedStartSec,
            final double elapsedEndSec, final double durationSec, final Histogram histogram) {
        writer.printf(
                "%d,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d%n",
                index,
                elapsedStartSec,
                elapsedEndSec,
                durationSec,
                histogram.getTotalCount(),
                histogram.getValueAtPercentile(50.0),
                histogram.getValueAtPercentile(90.0),
                histogram.getValueAtPercentile(95.0),
                histogram.getValueAtPercentile(99.0),
                histogram.getValueAtPercentile(99.9),
                histogram.getValueAtPercentile(99.99),
                histogram.getMaxValue());
    }
}
