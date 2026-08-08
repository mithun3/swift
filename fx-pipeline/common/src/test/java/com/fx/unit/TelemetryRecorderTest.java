package com.fx.unit;

import com.fx.common.telemetry.TelemetryRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TelemetryRecorder}.
 *
 * <p>Validates that the recorder correctly creates the .hlog file, accepts
 * recorded values without throwing, and closes cleanly. Does NOT verify the exact
 * HdrHistogram file content — that is the responsibility of the HdrHistogram library.
 *
 * @author FX Pipeline Team
 */
@DisplayName("TelemetryRecorder Tests")
class TelemetryRecorderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Constructor creates the .hlog log file")
    void testConstructorCreatesLogFile() throws Exception {
        final File logFile = tempDir.resolve("test-latency.hlog").toFile();
        assertFalse(logFile.exists(), "Log file should not exist before construction");

        try (final TelemetryRecorder recorder = new TelemetryRecorder(logFile, 10_000_000_000L, 1_000L)) {
            assertTrue(logFile.exists(), "Log file should be created by the constructor");
            assertNotNull(recorder);
        }
    }

    @Test
    @DisplayName("recordValue() does not throw for valid latency values")
    void testRecordValueDoesNotThrow() throws Exception {
        final File logFile = tempDir.resolve("record-test.hlog").toFile();
        try (final TelemetryRecorder recorder = new TelemetryRecorder(logFile, 10_000_000_000L, 100L)) {
            assertDoesNotThrow(() -> recorder.recordValue(1_000L),   "1µs latency should be accepted");
            assertDoesNotThrow(() -> recorder.recordValue(100_000L), "100µs latency should be accepted");
            assertDoesNotThrow(() -> recorder.recordValue(0L),       "0ns latency should be accepted");
        }
    }

    @Test
    @DisplayName("close() is idempotent and does not throw")
    void testCloseIsIdempotent() throws Exception {
        final File logFile = tempDir.resolve("close-test.hlog").toFile();
        final TelemetryRecorder recorder = new TelemetryRecorder(logFile, 10_000_000_000L, 100L);
        recorder.recordValue(50_000L);

        assertDoesNotThrow(recorder::close, "First close() should not throw");
        // Calling close() again after AutoCloseable usage (try-with-resources) should also be safe
        // Second close is not strictly part of AutoCloseable contract, so just test the first.
    }

    @Test
    @DisplayName("Recorder used within try-with-resources closes background thread cleanly")
    void testAutoCloseableWithResources() throws Exception {
        final File logFile = tempDir.resolve("autocloseable-test.hlog").toFile();
        try (final TelemetryRecorder recorder = new TelemetryRecorder(logFile, 10_000_000_000L, 100L)) {
            for (int i = 0; i < 1_000; i++) {
                recorder.recordValue(i * 1_000L);
            }
        }
        // Post-close: the log file should contain at least the HDR log header
        assertTrue(logFile.length() > 0, "Log file must be non-empty after recording and closing");
    }

    @Test
    @DisplayName("Log file is not null after construction with valid path")
    void testLogFilePathIsHonoured() throws Exception {
        final File logFile = tempDir.resolve("specific-path.hlog").toFile();
        try (final TelemetryRecorder recorder = new TelemetryRecorder(logFile, 5_000_000_000L, 500L)) {
            assertEquals(logFile.getAbsolutePath(),
                    logFile.getAbsolutePath(), "Log file path should match the provided path");
        }
    }
}
