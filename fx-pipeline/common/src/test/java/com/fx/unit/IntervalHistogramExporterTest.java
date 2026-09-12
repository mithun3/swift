package com.fx.unit;

import com.fx.common.telemetry.IntervalHistogramExporter;
import com.fx.common.telemetry.TelemetryRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link IntervalHistogramExporter}.
 *
 * <p>Uses a real {@link TelemetryRecorder}-written {@code .hlog} file (not a hand-crafted
 * one) so the test exercises the exact log format the benchmark harness produces.
 *
 * @author FX Pipeline Team
 */
@DisplayName("IntervalHistogramExporter Tests")
class IntervalHistogramExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("export() writes one CSV row per non-empty flushed interval, with a header")
    void testExportWritesOneRowPerInterval() throws Exception {
        final File logFile = tempDir.resolve("interval-export.hlog").toFile();
        try (TelemetryRecorder recorder = new TelemetryRecorder(logFile, 10_000_000_000L, 50L)) {
            // Two intervals' worth of recordings, spaced across the 50ms flush boundary.
            recorder.recordValue(1_000L);
            recorder.recordValue(2_000L);
            Thread.sleep(120);
            recorder.recordValue(3_000L);
            Thread.sleep(120);
        }

        final File csvFile = tempDir.resolve("interval-export.csv").toFile();
        final int rowsWritten = IntervalHistogramExporter.export(logFile, csvFile);

        assertTrue(rowsWritten >= 1, "Expected at least one non-empty interval to be exported");

        final List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals("interval_index,elapsed_start_sec,elapsed_end_sec,duration_sec,"
                + "count,p50_ns,p90_ns,p95_ns,p99_ns,p999_ns,p9999_ns,max_ns", lines.get(0));
        assertEquals(rowsWritten + 1, lines.size(), "One header row plus one row per interval");

        final String[] firstDataRow = lines.get(1).split(",", -1);
        assertEquals("0", firstDataRow[0], "First data row must be interval_index 0");
        assertEquals(0.0, Double.parseDouble(firstDataRow[1]), 0.001, "First interval starts at elapsed 0");
        assertTrue(Double.parseDouble(firstDataRow[2]) > 0, "elapsed_end_sec must be after elapsed_start_sec");
        assertTrue(Long.parseLong(firstDataRow[4]) > 0, "count must be positive for a non-empty interval");
    }

    @Test
    @DisplayName("export() returns zero rows for a log file with no recorded values")
    void testExportHandlesEmptyRecorder() throws Exception {
        final File logFile = tempDir.resolve("empty.hlog").toFile();
        try (TelemetryRecorder recorder = new TelemetryRecorder(logFile, 10_000_000_000L, 50L)) {
            // No recordValue() calls — every flushed interval has zero count and is skipped.
            Thread.sleep(60);
        }

        final File csvFile = tempDir.resolve("empty.csv").toFile();
        final int rowsWritten = IntervalHistogramExporter.export(logFile, csvFile);

        assertEquals(0, rowsWritten);
        assertEquals(1, Files.readAllLines(csvFile.toPath()).size(), "Header-only CSV expected");
    }
}
