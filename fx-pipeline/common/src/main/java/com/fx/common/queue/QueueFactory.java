package com.fx.common.queue;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.WireType;

import java.io.File;

/**
 * {@code QueueFactory} — Centralised factory for creating Chronicle Queue instances.
 *
 * <h2>Why a Factory?</h2>
 * <p>
 * Chronicle Queue configuration is non-trivial: roll cycles, wire types, block sizes,
 * and store file suppliers all impact throughput and latency. Centralising creation in
 * a factory ensures all queues in the pipeline share consistent configuration and that
 * tuning parameters are changed in one place.
 *
 * <h2>Wire Type Selection: {@code BINARY_LIGHT}</h2>
 * <p>
 * We use {@code WireType.BINARY_LIGHT} because:
 * <ul>
 *   <li>It is the most compact binary format — no field name metadata is stored,
 *       reducing the bytes written per event.</li>
 *   <li>It is the fastest serialisation path in Chronicle Wire for reading and writing.</li>
 *   <li>All consumers in this pipeline are compiled together and share the same
 *       {@link com.fx.common.event.FxMarketEvent} schema, so field-name metadata
 *       is unnecessary overhead.</li>
 * </ul>
 *
 * <h2>Block Size (128 MB)</h2>
 * <p>
 * The block size determines how much of the queue file is memory-mapped at one time.
 * At 10K events/sec × ~100 bytes/event, a 100-second 1M-event benchmark produces
 * ~100 MB of data per queue — fitting entirely within a single 128 MB segment.
 * <ul>
 *   <li>Fewer segments = fewer segment rolls during a benchmark run.</li>
 *   <li>Fewer rolls = fewer page-fault events (critical for Docker Desktop via VirtioFS,
 *       where each segment-roll page fault costs 1-50 ms instead of 5-20 µs).</li>
 *   <li>Doubled from the previous 64 MB default.</li>
 * </ul>
 *
 * <h2>Roll Cycle: {@code FAST_DAILY}</h2>
 * <p>
 * {@link RollCycles#FAST_DAILY} rolls queue files daily rather than sub-hourly.
 * For a benchmark run lasting 100-200 seconds, this means zero mid-run segment rolls —
 * eliminating the largest source of page-fault latency spikes in Docker environments.
 *
 * <h2>Segment Pre-Touching ({@link QueuePreToucher})</h2>
 * <p>
 * After construction, a {@link QueuePreToucher} daemon is started automatically.
 * It pre-faults the next mmap segment pages before the appender reaches them,
 * hiding the VirtioFS page-fault latency from the hot-path producer.
 *
 * <h2>Book/LMAX Mapping</h2>
 * <p>
 * The {@link ChronicleQueue} instances built here play the role of the Disruptor's
 * RingBuffer: a pre-allocated, durable, append-only backing store shared between
 * exactly one producer and its consumer(s).
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class QueueFactory {

    /**
     * Memory-mapped block size: 128 megabytes.
     *
     * <p>Doubled from the previous 64 MB to cover a full 1M-event benchmark run
     * (10K events/sec × 100s × ~100 bytes/event ≈ 100 MB) within a single segment,
     * eliminating mid-run segment rolls and their associated page-fault latency spikes.
     *
     * <p>Chronicle Queue requires {@code blockSize ≥ 4 × maxMessageSize}. At ~200 bytes
     * maximum per {@link com.fx.common.event.FxMarketEvent}, 128 MB provides a 640,000×
     * safety margin.
     *
     * <p><b>Consistency requirement:</b> all producers and consumers sharing a queue
     * path must use the same {@code blockSize}. This factory is the single source of
     * truth — all services construct queues through it.
     */
    private static final long BLOCK_SIZE_BYTES = 128L * 1024L * 1024L; // 128 MB

    private QueueFactory() {
        throw new UnsupportedOperationException("QueueFactory is a static factory class");
    }

    /**
     * Creates and returns a new {@link ChronicleQueue} for the given filesystem path.
     *
     * <p>The queue's store files ({@code .cq4}) will be created under {@code path}.
     * The directory is created if it does not exist.
     *
     * <p>A {@link QueuePreToucher} daemon is started automatically to pre-fault
     * the next segment's mmap pages before the appender reaches them, hiding
     * VirtioFS/page-fault latency from the hot-path producer.
     *
     * <p>The returned {@code ChronicleQueue} is an {@link AutoCloseable} resource.
     * Callers must close it (typically in a try-with-resources or shutdown hook) to
     * release the memory-mapped file handles and prevent resource leaks.
     *
     * @param path absolute filesystem path to the queue directory
     * @return a fully configured, ready-to-use {@link ChronicleQueue}
     * @throws IllegalArgumentException if {@code path} is null or empty
     */
    public static ChronicleQueue create(final String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Queue path must not be null or empty");
        }

        // Ensure the directory exists before Chronicle tries to create store files.
        // File.mkdirs() is idempotent — safe to call even if directory already exists.
        final File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Failed to create queue directory: " + path);
        }

        final SingleChronicleQueue queue = SingleChronicleQueueBuilder
                .binary(path)                        // Use binary (BINARY_LIGHT) wire format
                .blockSize(BLOCK_SIZE_BYTES)          // 128 MB mmap window per roll file
                .wireType(WireType.BINARY_LIGHT)      // Fastest, most compact format
                .rollCycle(RollCycles.FAST_DAILY)    // Daily roll — zero mid-benchmark rolls
                .build();

        // Start the segment pre-toucher daemon for this queue.
        // Runs at MIN_PRIORITY — does not compete with the event loop for CPU budget.
        // Eliminates 1-50 ms VirtioFS page-fault spikes at segment roll boundaries.
        QueuePreToucher.start(queue);

        return queue;
    }

    /**
     * Creates a Chronicle Queue at a path derived from a system property override.
     *
     * <p>The system property {@code fx.queue.<name>.path} takes precedence over
     * the default path. This allows ops teams to redirect queues to fast NVMe mounts
     * at runtime without recompiling: {@code -Dfx.queue.queue-a.path=/mnt/nvme/queue-a}.
     *
     * @param defaultPath the fallback path if no system property override is present
     * @param queueName   the short queue name used to build the property key (e.g., "queue-a")
     * @return a configured {@link ChronicleQueue}
     */
    public static ChronicleQueue createWithOverride(final String defaultPath,
                                                     final String queueName) {
        final String resolvedPath = System.getProperty(
                "fx.queue." + queueName + ".path", defaultPath);
        return create(resolvedPath);
    }
}


