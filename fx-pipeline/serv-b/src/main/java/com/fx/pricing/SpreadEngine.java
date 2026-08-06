package com.fx.pricing;

import com.fx.common.event.FxMarketEvent;

/**
 * {@code SpreadEngine} — Zero-allocation FX spread application and price computation.
 *
 * <h2>FX Spread Mechanics</h2>
 * <p>
 * In FX trading, the spread is the difference between the bid (buy) and ask (sell)
 * price. The liquidity provider (LP) quotes a mid-price; the bank or dealer applies
 * a client-facing spread on top to earn revenue. For example:
 * <pre>
 *   Mid-price: 1.0850 (108500 scaled)
 *   Spread:    0.0002 (20 pips = 20 scaled units at 5-decimal precision)
 *   Bid price: 1.0848 (108480 scaled) — client buys at the ask
 *   Ask price: 1.0852 (108520 scaled) — client sells at the bid
 * </pre>
 *
 * <h2>Scaled Integer Arithmetic</h2>
 * <p>
 * All prices and spreads are stored as scaled {@code long} integers
 * (price × 100,000). This eliminates IEEE 754 floating-point rounding errors
 * that would otherwise accumulate across millions of trade calculations.
 * All arithmetic in this class is integer-only — zero floating-point operations.
 *
 * <h2>Tier-Based Spread Table</h2>
 * <p>
 * Spreads vary by client tier. Higher-tier clients receive tighter spreads
 * (lower cost). The spread table is stored as a primitive {@code long[]} indexed
 * by tier — a single array element lookup with perfect cache locality.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class SpreadEngine {

    // ── Tier-indexed spread table (scaled long values: pips × 10) ───────────
    // Tier 0: unknown — reject with PRICING_FAILED
    // Tier 1: RETAIL    — 3 pips spread = 30 scaled units
    // Tier 2: PRIME     — 1 pip spread  = 10 scaled units
    // Tier 3: INSTITUTIONAL — 0.5 pip   = 5 scaled units
    // Fits entirely in one cache line (4 longs = 32 bytes).
    private static final long[] SPREAD_BY_TIER = {
            -1L,  // Tier 0: invalid
            30L,  // Tier 1: RETAIL — 3 pips
            10L,  // Tier 2: PRIME  — 1 pip
            5L    // Tier 3: INSTITUTIONAL — 0.5 pip
    };

    /** Maximum tier index supported by the spread table. */
    private static final int MAX_TIER = 3;

    /**
     * Applies the appropriate spread to the event and populates
     * {@link FxMarketEvent#executedPriceScaled} and {@link FxMarketEvent#spreadScaled}.
     *
     * <p>This method is the entire hot path for serv-b's pricing logic.
     * Every operation is a primitive integer arithmetic instruction — no objects,
     * no floating-point, no method calls beyond simple array access.
     *
     * <p>Returns {@code true} if pricing succeeded; {@code false} if the event
     * should be marked {@code PRICING_FAILED} (e.g., unknown tier, negative price).
     *
     * @param event the FX market event to price; mutated in-place
     * @return {@code true} if the event was successfully priced
     */
    public boolean applySpread(final FxMarketEvent event) {
        // Validate tier is within the supported range.
        if (event.clientTier < 1 || event.clientTier > MAX_TIER) {
            return false;
        }

        // Validate base price is positive — negative or zero price is a data error.
        if (event.requestedPriceScaled <= 0L) {
            return false;
        }

        // Lookup the spread for this client's tier — O(1) array access.
        final long spread = SPREAD_BY_TIER[event.clientTier];

        // Mutate the event flyweight in-place:
        event.spreadScaled = spread;

        // Apply spread: Buy orders execute at ask (mid + half-spread).
        //               Sell orders execute at bid (mid - half-spread).
        // Side: +1 = BUY, -1 = SELL. Using multiplication avoids a branch.
        // The spread is divided by 2 for half-spread application.
        // Integer division: half-spread truncated (conservative, not generous).
        final long halfSpread = spread / 2L;
        event.executedPriceScaled = event.requestedPriceScaled
                + (event.side * halfSpread);

        return event.executedPriceScaled > 0L;
    }

    /**
     * Returns the spread (in scaled units) for a given client tier.
     *
     * @param tier the client tier (1–3)
     * @return spread in scaled units, or {@code -1L} for unknown tiers
     */
    public long spreadForTier(final int tier) {
        if (tier < 0 || tier >= SPREAD_BY_TIER.length) {
            return -1L;
        }
        return SPREAD_BY_TIER[tier];
    }
}
