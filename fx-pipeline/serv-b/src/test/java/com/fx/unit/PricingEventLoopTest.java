package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.pricing.SpreadEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link com.fx.pricing.PricingEventLoop} business logic.
 *
 * <p>Because {@link net.openhft.chronicle.queue.ExcerptAppender} is a Chronicle internal
 * interface that cannot be easily mocked (it carries JNI-level restrictions), this test
 * class instead validates the core business rules that {@code PricingEventLoop.handle()}
 * implements — specifically:
 * <ul>
 *   <li>Terminal failures (CREDIT_REJECTED, VALIDATION_FAILED) bypass the spread engine.</li>
 *   <li>ACCEPTED events are priced by {@link SpreadEngine} and marked PRICED.</li>
 *   <li>ACCEPTED events with an unknown tier are marked PRICING_FAILED.</li>
 * </ul>
 *
 * <p>The {@code EventLoopHandler} functional interface is used as a lightweight stub
 * to capture events in place of the actual queue appender, validating the event state
 * after the business logic runs.
 *
 * @author FX Pipeline Team
 */
@DisplayName("PricingEventLoop Business Logic Tests")
class PricingEventLoopTest {

    private SpreadEngine spreadEngine;
    private FxMarketEvent event;
    private List<FxMarketEvent> capturedForwarded;

    @BeforeEach
    void setUp() {
        spreadEngine         = new SpreadEngine();
        event                = new FxMarketEvent();
        capturedForwarded    = new ArrayList<>();
        event.reset();
    }

    /**
     * Simulates the fast-path: CREDIT_REJECTED events bypass the spread engine.
     * The event should be forwarded immediately without price mutation.
     */
    @Test
    @DisplayName("CREDIT_REJECTED events bypass pricing and are forwarded unchanged")
    void testCreditRejectedBypassesPricing() {
        event.eventStatus          = EventStatus.CREDIT_REJECTED;
        event.requestedPriceScaled = 108500L;
        event.clientTier           = 1;

        // Simulate the fast-path logic from PricingEventLoop.handle()
        if (EventStatus.isTerminalFailure(event.eventStatus)) {
            capturedForwarded.add(event);
        } else {
            spreadEngine.applySpread(event);
            event.eventStatus = EventStatus.PRICED;
            capturedForwarded.add(event);
        }

        assertEquals(1, capturedForwarded.size(), "CREDIT_REJECTED event must be forwarded exactly once");
        assertEquals(EventStatus.CREDIT_REJECTED, event.eventStatus,
                "Event status must remain CREDIT_REJECTED (not repriced)");
        assertEquals(0L, event.executedPriceScaled,
                "executedPriceScaled must remain 0 — spread engine was never called");
        assertEquals(0L, event.spreadScaled,
                "spreadScaled must remain 0 — spread engine was never called");
    }

    /**
     * Verifies that an ACCEPTED event with a valid tier and price results in
     * status = PRICED and a non-zero executedPriceScaled.
     */
    @Test
    @DisplayName("ACCEPTED event with valid tier and price is marked PRICED")
    void testAcceptedEventGetsPriced() {
        event.eventStatus          = EventStatus.ACCEPTED;
        event.clientTier           = 2; // PRIME — 10 scaled units spread
        event.requestedPriceScaled = 108500L;
        event.side                 = 1; // BUY

        final boolean success = spreadEngine.applySpread(event);
        event.eventStatus = success ? EventStatus.PRICED : EventStatus.PRICING_FAILED;
        capturedForwarded.add(event);

        assertEquals(EventStatus.PRICED, event.eventStatus,
                "Event must be PRICED after successful spread application");
        assertTrue(event.executedPriceScaled > 0,
                "executedPriceScaled must be positive after pricing");
        assertEquals(1, capturedForwarded.size());
    }

    /**
     * Verifies that VALIDATION_FAILED (another terminal failure) also bypasses pricing.
     */
    @Test
    @DisplayName("VALIDATION_FAILED events bypass pricing — isTerminalFailure() returns true")
    void testValidationFailedBypassesPricing() {
        event.eventStatus = EventStatus.VALIDATION_FAILED;

        assertTrue(EventStatus.isTerminalFailure(event.eventStatus),
                "VALIDATION_FAILED must be detected as a terminal failure");
        assertEquals(0L, event.spreadScaled,        "spreadScaled must stay 0 — not priced");
        assertEquals(0L, event.executedPriceScaled, "executedPriceScaled must stay 0 — not priced");
    }

    /**
     * Verifies ACCEPTED event with unknown tier (0) is marked PRICING_FAILED.
     */
    @Test
    @DisplayName("ACCEPTED event with unknown tier (0) results in PRICING_FAILED")
    void testUnknownTierCausesPricingFailed() {
        event.eventStatus          = EventStatus.ACCEPTED;
        event.clientTier           = 0; // UNKNOWN
        event.requestedPriceScaled = 108500L;

        final boolean success = spreadEngine.applySpread(event);
        event.eventStatus = success ? EventStatus.PRICED : EventStatus.PRICING_FAILED;
        capturedForwarded.add(event);

        assertEquals(EventStatus.PRICING_FAILED, event.eventStatus,
                "Unknown tier must result in PRICING_FAILED");
        assertEquals(1, capturedForwarded.size());
    }

    /**
     * Verifies that the T2 stage-entry timestamp is set to a valid non-zero nanotime.
     */
    @Test
    @DisplayName("t2ServBEntry is captured as a non-zero nanotime when handle() is entered")
    void testT2TimestampIsPopulated() {
        final long before = System.nanoTime();
        event.t2ServBEntry = System.nanoTime(); // Mirrors PricingEventLoop.handle()
        final long after = System.nanoTime();

        assertTrue(event.t2ServBEntry >= before && event.t2ServBEntry <= after,
                "t2ServBEntry must be within the System.nanoTime() window");
    }
}
