package com.fx.unit;

import com.fx.gateway.CorrelationIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CorrelationIdGenerator}.
 *
 * @author FX Pipeline Team
 */
@DisplayName("CorrelationIdGenerator Tests")
class CorrelationIdGeneratorTest {

    @Test
    @DisplayName("next() starts at 1 with default constructor")
    void testFirstIdIsOne() {
        final CorrelationIdGenerator gen = new CorrelationIdGenerator();
        assertEquals(1L, gen.next(), "First generated ID should be 1");
    }

    @Test
    @DisplayName("next() returns monotonically increasing values")
    void testMonotonicallyIncreasing() {
        final CorrelationIdGenerator gen = new CorrelationIdGenerator();
        long previous = gen.next();
        for (int i = 0; i < 1_000; i++) {
            final long current = gen.next();
            assertTrue(current > previous, "Each ID must be strictly greater than the previous");
            previous = current;
        }
    }

    @Test
    @DisplayName("Initialising from a custom value resumes from next")
    void testCustomInitialValue() {
        final CorrelationIdGenerator gen = new CorrelationIdGenerator(1_000_000L);
        assertEquals(1_000_001L, gen.next(), "Should resume from initialValue + 1");
    }

    @Test
    @DisplayName("current() returns last generated without advancing")
    void testCurrentDoesNotAdvance() {
        final CorrelationIdGenerator gen = new CorrelationIdGenerator();
        gen.next(); // ID = 1
        gen.next(); // ID = 2
        assertEquals(2L, gen.current());
        assertEquals(2L, gen.current()); // Still 2
        assertEquals(3L, gen.next());    // Now advances
    }

    @Test
    @DisplayName("reset() sets sequence back to 0")
    void testReset() {
        final CorrelationIdGenerator gen = new CorrelationIdGenerator();
        gen.next();
        gen.next();
        gen.reset();
        assertEquals(1L, gen.next(), "After reset, first ID should be 1 again");
    }

    @Test
    @DisplayName("No duplicate IDs under concurrent access")
    void testNoDuplicatesUnderConcurrency() throws InterruptedException {
        final CorrelationIdGenerator gen = new CorrelationIdGenerator();
        final int threadCount = 4;
        final int idsPerThread = 10_000;
        final long[] allIds = new long[threadCount * idsPerThread];
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicBoolean failed = new AtomicBoolean(false);

        for (int t = 0; t < threadCount; t++) {
            final int offset = t * idsPerThread;
            Thread.ofPlatform().start(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        allIds[offset + i] = gen.next();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Verify all IDs are unique by checking no two are equal
        final java.util.HashSet<Long> unique = new java.util.HashSet<>(allIds.length);
        for (final long id : allIds) {
            assertTrue(unique.add(id), "Duplicate ID detected: " + id);
        }
    }
}
