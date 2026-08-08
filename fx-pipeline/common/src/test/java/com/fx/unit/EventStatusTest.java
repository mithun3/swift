package com.fx.unit;

import com.fx.common.event.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EventStatus} — the integer-constant status registry.
 *
 * <p>Validates constant values, the {@link EventStatus#isTerminalFailure(int)} predicate,
 * and the {@link EventStatus#toLabel(int)} diagnostic string converter.
 *
 * @author FX Pipeline Team
 */
@DisplayName("EventStatus Tests")
class EventStatusTest {

    // ── Constant value tests ────────────────────────────────────────────────

    @Test
    @DisplayName("RECEIVED should be 0 (initial sentinel)")
    void testReceivedIsZero() {
        assertEquals(0, EventStatus.RECEIVED, "RECEIVED must be 0 — zero-sentinel for uninitialised status");
    }

    @Test
    @DisplayName("All constants must be unique")
    void testAllConstantsAreUnique() {
        final int[] all = {
                EventStatus.RECEIVED,
                EventStatus.ACCEPTED,
                EventStatus.CREDIT_REJECTED,
                EventStatus.VALIDATION_FAILED,
                EventStatus.PRICED,
                EventStatus.PRICING_FAILED,
                EventStatus.PERSISTED,
                EventStatus.ERROR
        };
        final java.util.Set<Integer> unique = new java.util.HashSet<>();
        for (final int s : all) {
            assertTrue(unique.add(s), "Duplicate status value detected: " + s);
        }
    }

    // ── isTerminalFailure() tests ───────────────────────────────────────────

    @ParameterizedTest(name = "isTerminalFailure({0}) should be true")
    @ValueSource(ints = {
            EventStatus.CREDIT_REJECTED,
            EventStatus.VALIDATION_FAILED,
            EventStatus.PRICING_FAILED,
            EventStatus.ERROR
    })
    @DisplayName("isTerminalFailure returns true for all failure statuses")
    void testIsTerminalFailureForFailures(final int status) {
        assertTrue(EventStatus.isTerminalFailure(status),
                "Status " + status + " should be a terminal failure");
    }

    @ParameterizedTest(name = "isTerminalFailure({0}) should be false")
    @ValueSource(ints = {
            EventStatus.RECEIVED,
            EventStatus.ACCEPTED,
            EventStatus.PRICED,
            EventStatus.PERSISTED
    })
    @DisplayName("isTerminalFailure returns false for non-failure statuses")
    void testIsTerminalFailureForSuccesses(final int status) {
        assertFalse(EventStatus.isTerminalFailure(status),
                "Status " + status + " should NOT be a terminal failure");
    }

    @Test
    @DisplayName("isTerminalFailure returns false for unknown high value")
    void testIsTerminalFailureForUnknown() {
        assertFalse(EventStatus.isTerminalFailure(99));
    }

    // ── toLabel() tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("toLabel() returns correct string for each known status")
    void testToLabelKnownStatuses() {
        assertEquals("RECEIVED",          EventStatus.toLabel(EventStatus.RECEIVED));
        assertEquals("ACCEPTED",          EventStatus.toLabel(EventStatus.ACCEPTED));
        assertEquals("CREDIT_REJECTED",   EventStatus.toLabel(EventStatus.CREDIT_REJECTED));
        assertEquals("VALIDATION_FAILED", EventStatus.toLabel(EventStatus.VALIDATION_FAILED));
        assertEquals("PRICED",            EventStatus.toLabel(EventStatus.PRICED));
        assertEquals("PRICING_FAILED",    EventStatus.toLabel(EventStatus.PRICING_FAILED));
        assertEquals("PERSISTED",         EventStatus.toLabel(EventStatus.PERSISTED));
        assertEquals("ERROR",             EventStatus.toLabel(EventStatus.ERROR));
    }

    @Test
    @DisplayName("toLabel() returns UNKNOWN(N) for unrecognised status")
    void testToLabelUnknownStatus() {
        final String label = EventStatus.toLabel(42);
        assertTrue(label.startsWith("UNKNOWN("), "Unknown status label should start with UNKNOWN(");
        assertTrue(label.contains("42"), "Unknown status label should contain the numeric value");
    }

    // ── Constructor protection ──────────────────────────────────────────────

    @Test
    @DisplayName("EventStatus cannot be instantiated")
    void testCannotInstantiate() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final var ctor = EventStatus.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            try {
                ctor.newInstance();
            } catch (final java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
