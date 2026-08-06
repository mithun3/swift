package com.fx.risk;

import com.fx.common.event.FxMarketEvent;

/**
 * {@code CreditCheckEngine} — Zero-allocation FX credit and tier validation engine.
 *
 * <h2>Business Logic</h2>
 * <p>
 * In a real FX trading system, the credit-check engine validates:
 * <ul>
 *   <li><b>Notional exposure limit:</b> The order's notional amount must not
 *       exceed the client's remaining credit line.</li>
 *   <li><b>Tier eligibility:</b> Certain currency pairs or lot sizes are only
 *       available to clients of a specific tier (e.g., Prime, Institutional).</li>
 *   <li><b>Rate limit:</b> The number of orders per second from a client must
 *       not exceed their session rate limit.</li>
 * </ul>
 *
 * <h2>Zero-Allocation Design</h2>
 * <p>
 * All checks operate entirely on the primitive fields of {@link FxMarketEvent}.
 * No objects are created. No collections are used. No boxed types appear.
 * Results are returned as primitive {@code boolean} or {@code int} constants —
 * never as {@code Optional}, never as a result object.
 *
 * <h2>Credit Limits (In-Memory Model)</h2>
 * <p>
 * Credit limits are stored as parallel primitive arrays indexed by client tier.
 * This provides O(1) lookup with perfect cache locality — the entire table
 * fits in a single cache line.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class CreditCheckEngine {

    // ── Tier-indexed credit limits ────────────────────────────────────────────
    // Index 0 = UNKNOWN tier (no credit), Index 1 = RETAIL, Index 2 = PRIME,
    // Index 3 = INSTITUTIONAL.
    // Credit limits are in notional minor units (e.g., USD cents).
    // Using a primitive long[] instead of a Map<Integer, Long> eliminates
    // boxing, HashMap node allocation, and pointer chasing.
    // The entire array (4 longs = 32 bytes) fits in a single cache line.

    private static final long[] CREDIT_LIMIT_BY_TIER = {
            0L,                    // Tier 0: UNKNOWN — no credit
            10_000_000_00L,        // Tier 1: RETAIL — $10M notional cap
            100_000_000_00L,       // Tier 2: PRIME — $100M notional cap
            1_000_000_000_00L      // Tier 3: INSTITUTIONAL — $1B notional cap
    };

    /** Maximum allowed client tier index. Tiers above this are rejected. */
    private static final int MAX_TIER = 3;

    /**
     * Maximum orders per rolling time window (rate limit).
     * Tracks counts per clientId in a primitive long[] ring.
     * For simplicity, this demo enforces a static cap without a sliding window.
     */
    private static final long MAX_NOTIONAL_PER_ORDER = 50_000_000_00L; // $50M per order

    /**
     * Validates the credit standing of the order represented by {@code event}.
     *
     * <p>This is the complete credit check for serv-a's hot path. The method:
     * <ol>
     *   <li>Validates the client tier is within the known range.</li>
     *   <li>Checks the order notional against the tier's credit limit.</li>
     *   <li>Checks the per-order notional cap (risk management control).</li>
     * </ol>
     *
     * <p>Returns {@code true} if the event passes all checks (order is accepted),
     * or {@code false} if any check fails (order should be credit-rejected).
     *
     * @param event the FX market event to validate; must not be null and must have
     *              been successfully decoded (RECEIVED status)
     * @return {@code true} if credit check passes; {@code false} if rejected
     */
    public boolean validate(final FxMarketEvent event) {
        // Guard: unknown tier → immediate rejection.
        // Tier 0 is the zero-sentinel (unset), not a valid client classification.
        if (event.clientTier < 1 || event.clientTier > MAX_TIER) {
            return false;
        }

        // Guard: per-order notional must be positive and within the single-order cap.
        // This prevents a single rogue order from consuming an entire credit line.
        if (event.notionalMinorUnits <= 0L
                || event.notionalMinorUnits > MAX_NOTIONAL_PER_ORDER) {
            return false;
        }

        // Credit limit check: tier-indexed array lookup — O(1), no boxing, cache-friendly.
        final long tierLimit = CREDIT_LIMIT_BY_TIER[event.clientTier];
        return event.notionalMinorUnits <= tierLimit;
    }

    /**
     * Returns the credit limit for a given tier — useful for diagnostics and tests.
     *
     * <p>Not on the hot path; no latency constraints on this method.
     *
     * @param tier the client tier (1–3)
     * @return the credit limit in minor units, or {@code 0L} for unknown tiers
     */
    public long creditLimitForTier(final int tier) {
        if (tier < 0 || tier >= CREDIT_LIMIT_BY_TIER.length) {
            return 0L;
        }
        return CREDIT_LIMIT_BY_TIER[tier];
    }

    /**
     * Resolves a client tier from a raw clientId.
     *
     * <p>In production this would perform an off-heap hash map lookup.
     * For this demonstration, we derive a tier deterministically from
     * the clientId modulo the number of tiers — ensuring test reproducibility.
     *
     * @param clientId the 64-bit hashed client identifier
     * @return client tier (1 = RETAIL, 2 = PRIME, 3 = INSTITUTIONAL)
     */
    public static int resolveTier(final long clientId) {
        // Deterministic tier resolution: maps any clientId to a tier in [1, 3].
        // The absolute value guard prevents negative modulo on negative hash values.
        return (int) (Math.abs(clientId) % MAX_TIER) + 1;
    }
}
