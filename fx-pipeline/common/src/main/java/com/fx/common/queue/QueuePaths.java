package com.fx.common.queue;

/**
 * {@code QueuePaths} — Centralised registry of Chronicle Queue filesystem paths.
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Keeping all queue paths in a single constants class achieves two goals:
 * <ol>
 *   <li><b>Single source of truth:</b> Changing a queue's storage path requires
 *       editing exactly one file. There is no risk of a producer and consumer
 *       pointing at different directories due to copy-paste drift.</li>
 *   <li><b>Zero-allocation access:</b> {@code static final String} constants are
 *       interned by the JVM class loader and shared from the string pool. No new
 *       String is created each time a path is referenced — the same object
 *       reference is returned every time.</li>
 * </ol>
 *
 * <h2>Storage Strategy</h2>
 * <p>
 * Queues are stored under {@code /tmp/fx-queues/} by default, which maps to a
 * tmpfs RAM-backed filesystem on Linux (if configured). On macOS (development),
 * this falls through to the regular filesystem — still fast enough for testing.
 * In production, mount {@code /tmp/fx-queues} on a RAM disk or a dedicated NVMe
 * namespace to maximise sequential I/O throughput.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class QueuePaths {

    /** Base directory containing all Chronicle Queue data files. */
    public static final String BASE_DIR = System.getProperty(
            "fx.queue.base.dir", "/tmp/fx-queues");

    /**
     * queue-a: FIX Gateway (serv-0) → Risk Validation (serv-a).
     * Contains raw-decoded, validated FX market events in RECEIVED status.
     */
    public static final String QUEUE_A = BASE_DIR + "/queue-a";

    /**
     * queue-b: Risk Validation (serv-a) → Pricing Engine (serv-b).
     * Contains credit-checked events in ACCEPTED or CREDIT_REJECTED status.
     */
    public static final String QUEUE_B = BASE_DIR + "/queue-b";

    /**
     * queue-c: Pricing Engine (serv-b) → Persistence (serv-c).
     * Contains fully priced execution reports in PRICED status.
     */
    public static final String QUEUE_C = BASE_DIR + "/queue-c";

    /**
     * queue-err: Error routing queue — written by any service encountering an
     * unrecoverable event. Drained by a separate low-priority non-pinned thread.
     * Using a dedicated error queue avoids polluting the main processing queues
     * and prevents a single bad event from blocking the pipeline.
     */
    public static final String QUEUE_ERR = BASE_DIR + "/queue-err";

    private QueuePaths() {
        throw new UnsupportedOperationException("QueuePaths is a constants class");
    }
}
