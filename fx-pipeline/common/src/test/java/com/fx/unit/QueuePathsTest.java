package com.fx.unit;

import com.fx.common.queue.QueuePaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link QueuePaths} — the centralised registry of
 * Chronicle Queue filesystem paths shared by every pipeline service.
 *
 * @author FX Pipeline Team
 */
@DisplayName("QueuePaths Tests")
class QueuePathsTest {

    @Test
    @DisplayName("queue-a/b/c/err paths are all derived from BASE_DIR with the expected suffix")
    void testQueuePathsAreDerivedFromBaseDir() {
        assertEquals(QueuePaths.BASE_DIR + "/queue-a", QueuePaths.QUEUE_A);
        assertEquals(QueuePaths.BASE_DIR + "/queue-b", QueuePaths.QUEUE_B);
        assertEquals(QueuePaths.BASE_DIR + "/queue-c", QueuePaths.QUEUE_C);
        assertEquals(QueuePaths.BASE_DIR + "/queue-err", QueuePaths.QUEUE_ERR);
    }

    @Test
    @DisplayName("all four queue paths are distinct")
    void testQueuePathsAreDistinct() {
        final var paths = java.util.Set.of(
                QueuePaths.QUEUE_A, QueuePaths.QUEUE_B, QueuePaths.QUEUE_C, QueuePaths.QUEUE_ERR);
        assertEquals(4, paths.size(), "each pipeline queue must have a unique storage path");
    }

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException (constants-only class contract)")
    void testConstructorThrowsUnsupportedOperationException() throws Exception {
        final Constructor<QueuePaths> constructor = QueuePaths.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
    }
}
