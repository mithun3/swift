package com.fx.unit;

import com.fx.common.event.FxMarketEvent;
import com.fx.pricing.SpreadEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpreadEngine}.
 *
 * @author FX Pipeline Team
 */
@DisplayName("SpreadEngine Tests")
class SpreadEngineTest {

    private SpreadEngine engine;
    private FxMarketEvent event;

    @BeforeEach
    void setUp() {
        engine = new SpreadEngine();
        event  = new FxMarketEvent();
        event.reset();
    }

    @Test
    @DisplayName("RETAIL BUY: executed price = mid + half-spread")
    void testRetailBuySpread() {
        event.clientTier           = 1; // RETAIL — 30 scaled units spread
        event.requestedPriceScaled = 108500L;
        event.side                 = 1; // BUY

        final boolean success = engine.applySpread(event);

        assertTrue(success);
        assertEquals(30L,     event.spreadScaled,        "RETAIL spread should be 30 units");
        assertEquals(108515L, event.executedPriceScaled, "BUY executed = mid + half-spread = 108500 + 15 = 108515");
    }

    @Test
    @DisplayName("RETAIL SELL: executed price = mid - half-spread")
    void testRetailSellSpread() {
        event.clientTier           = 1; // RETAIL
        event.requestedPriceScaled = 108500L;
        event.side                 = -1; // SELL

        engine.applySpread(event);

        assertEquals(108485L, event.executedPriceScaled, "SELL executed = mid - half-spread = 108500 - 15 = 108485");
    }

    @Test
    @DisplayName("PRIME BUY: tighter spread of 10 units")
    void testPrimeBuySpread() {
        event.clientTier           = 2; // PRIME — 10 units spread
        event.requestedPriceScaled = 108500L;
        event.side                 = 1;

        engine.applySpread(event);

        assertEquals(10L,     event.spreadScaled);
        assertEquals(108505L, event.executedPriceScaled, "PRIME BUY = 108500 + 5 = 108505");
    }

    @Test
    @DisplayName("INSTITUTIONAL BUY: tightest spread of 5 units")
    void testInstitutionalBuySpread() {
        event.clientTier           = 3; // INSTITUTIONAL — 5 units
        event.requestedPriceScaled = 108500L;
        event.side                 = 1;

        engine.applySpread(event);

        assertEquals(5L,      event.spreadScaled);
        // halfSpread = 5 / 2 = 2 (integer division)
        assertEquals(108502L, event.executedPriceScaled, "INSTITUTIONAL BUY = 108500 + 2 = 108502");
    }

    @Test
    @DisplayName("Unknown tier (0) returns false")
    void testUnknownTierFails() {
        event.clientTier           = 0;
        event.requestedPriceScaled = 108500L;
        assertFalse(engine.applySpread(event), "Unknown tier should fail pricing");
    }

    @Test
    @DisplayName("Zero price returns false")
    void testZeroPriceFails() {
        event.clientTier           = 2;
        event.requestedPriceScaled = 0L;
        assertFalse(engine.applySpread(event), "Zero price should fail pricing");
    }

    @Test
    @DisplayName("Negative price returns false")
    void testNegativePriceFails() {
        event.clientTier           = 2;
        event.requestedPriceScaled = -100L;
        assertFalse(engine.applySpread(event), "Negative price should fail pricing");
    }

    @Test
    @DisplayName("spreadForTier() returns -1 for invalid tier")
    void testSpreadForInvalidTier() {
        assertEquals(-1L, engine.spreadForTier(-1));
        assertEquals(-1L, engine.spreadForTier(99));
    }

    @Test
    @DisplayName("spreadForTier() returns correct values for valid tiers")
    void testSpreadForValidTiers() {
        assertEquals(30L, engine.spreadForTier(1), "RETAIL spread");
        assertEquals(10L, engine.spreadForTier(2), "PRIME spread");
        assertEquals(5L,  engine.spreadForTier(3), "INSTITUTIONAL spread");
    }
}
