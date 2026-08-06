package com.fx.unit;

import com.fx.common.event.FxMarketEvent;
import com.fx.risk.CreditCheckEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CreditCheckEngine}.
 *
 * @author FX Pipeline Team
 */
@DisplayName("CreditCheckEngine Tests")
class CreditCheckEngineTest {

    private CreditCheckEngine engine;
    private FxMarketEvent event;

    @BeforeEach
    void setUp() {
        engine = new CreditCheckEngine();
        event  = new FxMarketEvent();
        event.reset();
    }

    @Test
    @DisplayName("RETAIL tier (1) accepts order within limit")
    void testRetailTierAccepted() {
        event.clientTier       = 1; // RETAIL
        event.notionalMinorUnits = 5_000_000_00L; // $5M — within $10M limit
        assertTrue(engine.validate(event), "Should accept $5M order for RETAIL tier");
    }

    @Test
    @DisplayName("RETAIL tier (1) rejects order exceeding limit")
    void testRetailTierRejected() {
        event.clientTier       = 1;
        event.notionalMinorUnits = 20_000_000_00L; // $20M — exceeds $10M RETAIL limit
        assertFalse(engine.validate(event), "Should reject $20M for RETAIL tier");
    }

    @Test
    @DisplayName("INSTITUTIONAL tier (3) accepts large order")
    void testInstitutionalTierAccepted() {
        event.clientTier       = 3;
        event.notionalMinorUnits = 40_000_000_00L; // $40M — within $1B limit and $50M per-order cap
        assertTrue(engine.validate(event));
    }

    @Test
    @DisplayName("Unknown tier (0) is always rejected")
    void testUnknownTierRejected() {
        event.clientTier       = 0;
        event.notionalMinorUnits = 1_00L; // Even tiny amount
        assertFalse(engine.validate(event), "Tier 0 should always be rejected");
    }

    @Test
    @DisplayName("Tier above MAX_TIER is rejected")
    void testTierAboveMaxRejected() {
        event.clientTier       = 99;
        event.notionalMinorUnits = 1_00L;
        assertFalse(engine.validate(event), "Tier 99 should be rejected");
    }

    @Test
    @DisplayName("Zero notional is rejected")
    void testZeroNotionalRejected() {
        event.clientTier       = 2;
        event.notionalMinorUnits = 0L;
        assertFalse(engine.validate(event), "Zero notional should be rejected");
    }

    @Test
    @DisplayName("Negative notional is rejected")
    void testNegativeNotionalRejected() {
        event.clientTier       = 2;
        event.notionalMinorUnits = -1000L;
        assertFalse(engine.validate(event), "Negative notional should be rejected");
    }

    @Test
    @DisplayName("Order exceeding per-order cap is rejected regardless of tier")
    void testPerOrderCapEnforced() {
        event.clientTier       = 3; // INSTITUTIONAL — highest tier
        event.notionalMinorUnits = 60_000_000_00L; // $60M — exceeds $50M per-order cap
        assertFalse(engine.validate(event), "Per-order cap should override tier limit");
    }

    @ParameterizedTest(name = "Tier {0} credit limit should be {1}")
    @CsvSource({
            "0, 0",
            "1, 1000000000",
            "2, 10000000000",
            "3, 100000000000"
    })
    @DisplayName("creditLimitForTier() returns correct limits")
    void testCreditLimitForTier(final int tier, final long expectedLimit) {
        assertEquals(expectedLimit, engine.creditLimitForTier(tier));
    }

    @Test
    @DisplayName("creditLimitForTier() returns 0 for out-of-range tier")
    void testCreditLimitForOutOfRangeTier() {
        assertEquals(0L, engine.creditLimitForTier(-1));
        assertEquals(0L, engine.creditLimitForTier(100));
    }

    @Test
    @DisplayName("resolveTier() always returns value in [1, 3]")
    void testResolveTierBounds() {
        for (long clientId = -1000L; clientId <= 1000L; clientId++) {
            final int tier = CreditCheckEngine.resolveTier(clientId);
            assertTrue(tier >= 1 && tier <= 3,
                    "Tier must be in [1,3] for clientId=" + clientId + ", got " + tier);
        }
    }
}
