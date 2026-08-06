package com.fx.common.queue;

import net.openhft.chronicle.queue.ChronicleQueue;
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
 * <h2>Block Size</h2>
 * <p>
 * The block size (64 MB) determines how much of the queue file is memory-mapped at one
 * time. A larger block reduces the frequency of mmap remapping syscalls (which cause
 * latency spikes) but increases virtual address space consumption. 64 MB is a balanced
 * default for high-throughput FX pipelines.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class QueueFactory {

    /**
     * Memory-mapped block size: 64 megabytes.
     *
     * <p>Chronicle maps this many bytes from the store file into virtual address space.
     * Larger values reduce re-mapping frequency (good for latency stability) at the cost
     * of higher virtual memory usage. 64 MB is appropriate for a queue handling tens of
     * millions of small FX events per roll cycle.
     */
    private static final long BLOCK_SIZE_BYTES = 64L * 1024L * 1024L; // 64 MB

    private QueueFactory() {
        throw new UnsupportedOperationException("QueueFactory is a static factory class");
    }

    /**
     * Creates and returns a new {@link ChronicleQueue} for the given filesystem path.
     *
     * <p>The queue's store files ({@code .cq4}) will be created under {@code path}.
     * The directory is created if it does not exist.
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

        return SingleChronicleQueueBuilder
                .binary(path)                // Use binary (BINARY_LIGHT) wire format
                .blockSize(BLOCK_SIZE_BYTES) // 64 MB mmap window per roll file
                .wireType(WireType.BINARY_LIGHT) // Fastest, most compact format
                .build();
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
