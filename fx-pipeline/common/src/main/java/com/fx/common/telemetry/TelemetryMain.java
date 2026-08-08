package com.fx.common.telemetry;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.queue.QueuePaths;

import java.io.File;
import java.io.IOException;

/**
 * {@code TelemetryMain} — Entry point to run the TelemetryStitcher as a standalone JVM process.
 *
 * <p>By running this in a separate JVM, we guarantee that the telemetry extraction, 
 * JSON formatting, and disk I/O do not interfere with the garbage collection or 
 * CPU cache of the main trading pipeline JVMs.
 */
public class TelemetryMain {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryMain.class);

    public static void main(String[] args) {
        logger.info("Starting Out-of-band Telemetry Stitcher...");

        final String queuePath = System.getProperty("fx.queue.queue-c.path", QueuePaths.QUEUE_C);
        
        // Output log will be written to the current working directory by default
        final String logPath = System.getProperty("fx.telemetry.trace.log", "logs/traces.jsonl");

        final File logDir = new File(logPath).getParentFile();
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs();
        }

        logger.info("Tailing Queue: " + queuePath);
        logger.info("Output JSON Log: " + logPath);

        try (TelemetryStitcher stitcher = new TelemetryStitcher(queuePath, logPath)) {
            
            // Add a shutdown hook to cleanly close the stitcher
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down TelemetryStitcher...");
                stitcher.close();
            }));

            // Keep the main thread alive while the background tailer thread runs
            Thread.currentThread().join();
            
        } catch (IOException | InterruptedException e) {
            logger.error("TelemetryStitcher failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
