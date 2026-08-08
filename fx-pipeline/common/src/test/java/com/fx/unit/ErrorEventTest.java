package com.fx.unit;

import com.fx.common.error.ErrorEvent;
import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ErrorEvent} flyweight DTO.
 *
 * <p>Validates {@link ErrorEvent#reset()} zeroing semantics and
 * {@link ErrorEvent#populateFrom(FxMarketEvent, String, String)} field transfer.
 *
 * @author FX Pipeline Team
 */
@DisplayName("ErrorEvent Tests")
class ErrorEventTest {

    private ErrorEvent errorEvent;
    private FxMarketEvent source;

    @BeforeEach
    void setUp() {
        errorEvent = new ErrorEvent();
        source = new FxMarketEvent();
        source.reset();
    }

    @Test
    @DisplayName("reset() should zero all fields")
    void testResetClearsAllFields() {
        // Arrange: pollute the fields
        errorEvent.correlationId      = 999L;
        errorEvent.errorNanoTime      = 12345L;
        errorEvent.serviceName        = "test-service";
        errorEvent.errorMessage       = "some error";
        errorEvent.eventStatusAtError = EventStatus.ERROR;

        // Act
        errorEvent.reset();

        // Assert
        assertEquals(0L,  errorEvent.correlationId,      "correlationId should be 0 after reset");
        assertEquals(0L,  errorEvent.errorNanoTime,      "errorNanoTime should be 0 after reset");
        assertEquals("",  errorEvent.serviceName,        "serviceName should be empty after reset");
        assertEquals("",  errorEvent.errorMessage,       "errorMessage should be empty after reset");
        assertEquals(0,   errorEvent.eventStatusAtError, "eventStatusAtError should be 0 after reset");
    }

    @Test
    @DisplayName("populateFrom() copies correlationId and eventStatus from source")
    void testPopulateFromCopiesSourceFields() {
        source.correlationId = 42L;
        source.eventStatus   = EventStatus.CREDIT_REJECTED;

        final long beforeNanos = System.nanoTime();
        errorEvent.populateFrom(source, "risk-a", "credit limit exceeded");
        final long afterNanos = System.nanoTime();

        assertEquals(42L,                       errorEvent.correlationId);
        assertEquals(EventStatus.CREDIT_REJECTED, errorEvent.eventStatusAtError);
        assertEquals("risk-a",                   errorEvent.serviceName);
        assertEquals("credit limit exceeded",    errorEvent.errorMessage);
        // errorNanoTime must be in the window of System.nanoTime() calls
        assertTrue(errorEvent.errorNanoTime >= beforeNanos
                        && errorEvent.errorNanoTime <= afterNanos,
                "errorNanoTime should be a valid nanosecond timestamp");
    }

    @Test
    @DisplayName("populateFrom() handles null errorMessage gracefully")
    void testPopulateFromNullErrorMessage() {
        source.correlationId = 1L;
        errorEvent.populateFrom(source, "gateway", null);

        assertEquals("unknown", errorEvent.errorMessage,
                "null errorMessage should be substituted with 'unknown'");
    }

    @Test
    @DisplayName("populateFrom() handles empty service name")
    void testPopulateFromEmptyServiceName() {
        source.correlationId = 1L;
        errorEvent.populateFrom(source, "", "some error");
        assertEquals("", errorEvent.serviceName);
    }
}
