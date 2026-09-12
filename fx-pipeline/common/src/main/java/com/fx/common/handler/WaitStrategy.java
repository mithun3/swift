package com.fx.common.handler;

/**
 * {@code WaitStrategy} — pluggable idle behavior for an {@link AbstractEventLoop}
 * when its poll source has no event ready.
 *
 * <p>Maps to the LMAX Disruptor {@code WaitStrategy} concept: consumers delegate
 * to a single named strategy when caught up, rather than inlining the idle
 * behavior at every poll site. The Disruptor offers several trade-offs here
 * (BusySpin, Yielding, Sleeping, Blocking — lowest latency to lowest CPU usage).
 * This pipeline provides two implementations:
 * <ul>
 *   <li>{@link BusySpinWaitStrategy} — optimal on native Linux with {@code isolcpus};
 *       keeps the thread hot at 100% CPU consumption on a dedicated core.</li>
 *   <li>{@link PhasedBackOffWaitStrategy} — optimal in Docker/VM environments where
 *       the event loop shares its cpuset with ZGC, JIT, and background threads.
 *       Co-operatively yields to prevent CFS force-preemption of the event loop.</li>
 * </ul>
 *
 * <p>Implementations must be allocation-free and safe to invoke from the single
 * pinned event-loop thread only.
 *
 * <h2>Selection</h2>
 * <p>Use {@link #fromSystemProperty()} to select at runtime via
 * {@code -Dfx.waitstrategy=busyspin|phased} (default: {@code busyspin}).
 */
public interface WaitStrategy {

    /**
     * Invoked when no event is currently available from the poll source.
     * Must not block indefinitely or perform I/O.
     */
    void idle();

    /**
     * Resets the wait strategy to its initial state.
     * Invoked when an event is successfully read.
     */
    default void reset() {}

    /**
     * Returns the {@link WaitStrategy} selected by the {@code fx.waitstrategy}
     * system property.
     *
     * <table border="1" summary="Strategy property values">
     *   <tr><th>Property value</th><th>Strategy returned</th><th>Best for</th></tr>
     *   <tr><td>{@code busyspin} (default)</td><td>{@link BusySpinWaitStrategy#INSTANCE}</td>
     *       <td>Native Linux with {@code isolcpus}</td></tr>
     *   <tr><td>{@code yielding}</td><td>new {@link YieldingWaitStrategy}</td>
     *       <td>macOS native / Shared CPUs where low latency is required</td></tr>
     *   <tr><td>{@code phased}</td><td>new {@link PhasedBackOffWaitStrategy}</td>
     *       <td>Docker / VM / heavily overloaded CPU environments</td></tr>
     * </table>
     *
     * <p>Each call returns a separate {@link PhasedBackOffWaitStrategy} or
     * {@link YieldingWaitStrategy} instance when requested, because those strategies
     * hold mutable per-consumer state. {@link BusySpinWaitStrategy} is a
     * stateless singleton — returning the shared instance is safe.
     *
     * @return the configured {@link WaitStrategy}; never {@code null}
     * @throws IllegalArgumentException if the property value is unrecognised
     */
    static WaitStrategy fromSystemProperty() {
        final String strategy = System.getProperty("fx.waitstrategy", "busyspin");
        return switch (strategy) {
            case "busyspin" -> BusySpinWaitStrategy.INSTANCE;
            case "yielding" -> new YieldingWaitStrategy();
            case "phased"   -> new PhasedBackOffWaitStrategy();
            default         -> throw new IllegalArgumentException(
                    "Unknown fx.waitstrategy: '" + strategy
                            + "'. Expected 'busyspin', 'yielding', or 'phased'.");
        };
    }
}

