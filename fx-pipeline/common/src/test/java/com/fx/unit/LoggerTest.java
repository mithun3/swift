package com.fx.unit;

import com.fx.common.logging.AsyncLogger;
import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import com.fx.common.logging.SyncLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    void testSyncLogger() {
        Logger logger = new SyncLogger("TestSync");
        logger.info("Test message");
        logger.info("Test arg", 42L);
        logger.info("Test object arg", (Object) "payload");
        logger.warn("Warning message");
        logger.warn("Warning with arg", 7L);
        logger.error("Error message", new RuntimeException("test exc"));
        logger.error("Bare error message");

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("INFO TestSync - Test message"));
        assertTrue(output.contains("INFO TestSync - Test arg 42"));
        assertTrue(output.contains("INFO TestSync - Test object arg payload"));
        assertTrue(output.contains("WARN TestSync - Warning message"));
        assertTrue(output.contains("WARN TestSync - Warning with arg 7"));
        assertTrue(output.contains("ERROR TestSync - Error message"));
        assertTrue(output.contains("ERROR TestSync - Bare error message"));
        assertTrue(output.contains("java.lang.RuntimeException: test exc"));
    }

    @Test
    void testAsyncLogger() throws InterruptedException {
        Logger logger = new AsyncLogger("TestAsync");
        logger.info("Async info message");
        logger.info("Async message with arg", 99L);
        logger.info("Async message with object", (Object) "async-payload");
        logger.warn("Async warn message");
        logger.warn("Async warn with arg", 5L);
        logger.error("Async error message", new RuntimeException("async exc"));
        logger.error("Bare async error message");

        // Wait for background processor to drain queue
        Thread.sleep(500);

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("INFO Async info message"));
        assertTrue(output.contains("INFO Async message with arg 99"));
        assertTrue(output.contains("INFO Async message with object async-payload"));
        assertTrue(output.contains("WARN Async warn message"));
        assertTrue(output.contains("WARN Async warn with arg 5"));
        assertTrue(output.contains("ERROR Async error message"));
        assertTrue(output.contains("ERROR Bare async error message"));
        assertTrue(output.contains("java.lang.RuntimeException: async exc"));
    }

    @Test
    void testLoggerFactory() {
        Logger defaultLogger = LoggerFactory.getLogger(LoggerTest.class);
        // By default, it should be AsyncLogger unless system property was set externally
        assertNotNull(defaultLogger);
    }
}
