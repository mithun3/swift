package com.fx.unit;

import com.fx.common.event.FxMarketEvent;
import com.fx.common.telemetry.TelemetryStitcher;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelemetryStitcher Tests")
class TelemetryStitcherTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Stitcher reads from queue and outputs valid JSON trace")
    void testTelemetryStitching() throws Exception {
        final File queueDir = tempDir.resolve("test-queue").toFile();
        final File logFile = tempDir.resolve("traces.jsonl").toFile();

        // 1. Write a dummy FxMarketEvent to the queue
        try (ChronicleQueue queue = net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
                .binary(queueDir)
                .build()) {
            
            ExcerptAppender appender = queue.createAppender();
            FxMarketEvent event = new FxMarketEvent();
            event.reset();
            event.correlationId = 123456L;
            event.ingressNanoTime = 1000L;
            event.t1ServAEntry = 2000L;
            event.t2ServBEntry = 3000L;
            event.t3ServCEntry = 4000L;
            event.eventStatus = 4; // PRICED or COMPLETED

            appender.writeDocument(event);
        }

        // 2. Start the TelemetryStitcher
        try (TelemetryStitcher stitcher = new TelemetryStitcher(queueDir.getAbsolutePath(), logFile.getAbsolutePath())) {
            // Give the background thread time to read the queue and write the log
            Thread.sleep(500);
        }

        // 3. Verify the JSON log file
        assertTrue(logFile.exists(), "Trace log file should exist");
        List<String> lines = Files.readAllLines(logFile.toPath());
        
        assertFalse(lines.isEmpty(), "Trace log should not be empty");
        String json = lines.get(0);
        System.out.println("JSON OUTPUT: " + json);

        assertTrue(json.contains("\"traceId\":\"123456\""), "JSON should contain correct traceId");
        assertTrue(json.contains("\"totalLatencyNs\":3000"), "JSON should contain correct totalLatencyNs (4000 - 1000)");
        assertTrue(json.contains("\"timestampNs\":1000"), "JSON should contain ingress timestamp");
        assertTrue(json.contains("\"timestampNs\":2000"), "JSON should contain serv-a timestamp");
        assertTrue(json.contains("\"timestampNs\":3000"), "JSON should contain serv-b timestamp");
        assertTrue(json.contains("\"timestampNs\":4000"), "JSON should contain serv-c timestamp");
    }
}
