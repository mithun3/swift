package com.fx.unit;

import com.fx.common.queue.QueueFactory;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QueueFactory}.
 *
 * <p>Validates null/empty path rejection and successful queue creation.
 * Uses JUnit 5's {@code @TempDir} for automatic cleanup of queue store files.
 *
 * @author FX Pipeline Team
 */
@DisplayName("QueueFactory Tests")
class QueueFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("create() returns a non-null ChronicleQueue for a valid path")
    void testCreateReturnsQueueForValidPath() {
        final String path = tempDir.resolve("test-queue").toString();
        try (final ChronicleQueue queue = QueueFactory.create(path)) {
            assertNotNull(queue, "Queue must not be null for a valid path");
        }
    }

    @Test
    @DisplayName("create() creates the directory if it does not exist")
    void testCreateMakesDirectory() {
        final String path = tempDir.resolve("new-dir/test-queue").toString();
        try (final ChronicleQueue queue = QueueFactory.create(path)) {
            assertNotNull(queue);
            assertTrue(new java.io.File(path).exists(), "Queue directory should have been created");
        }
    }

    @Test
    @DisplayName("create() throws IllegalArgumentException for null path")
    void testCreateThrowsForNullPath() {
        assertThrows(IllegalArgumentException.class,
                () -> QueueFactory.create(null),
                "null path should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("create() throws IllegalArgumentException for empty path")
    void testCreateThrowsForEmptyPath() {
        assertThrows(IllegalArgumentException.class,
                () -> QueueFactory.create(""),
                "empty path should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("createWithOverride() uses system property when set")
    void testCreateWithOverrideUsesSystemProperty() {
        final String overridePath = tempDir.resolve("overridden-queue").toString();
        System.setProperty("fx.queue.test-q.path", overridePath);
        try (final ChronicleQueue queue = QueueFactory.createWithOverride("/invalid/default", "test-q")) {
            assertNotNull(queue, "Queue should use the system property override path");
            assertTrue(new java.io.File(overridePath).exists());
        } finally {
            System.clearProperty("fx.queue.test-q.path");
        }
    }

    @Test
    @DisplayName("createWithOverride() falls back to defaultPath if no property set")
    void testCreateWithOverrideFallsBackToDefault() {
        System.clearProperty("fx.queue.no-prop-q.path");
        final String defaultPath = tempDir.resolve("default-queue").toString();
        try (final ChronicleQueue queue = QueueFactory.createWithOverride(defaultPath, "no-prop-q")) {
            assertNotNull(queue);
            assertTrue(new java.io.File(defaultPath).exists());
        }
    }
}
