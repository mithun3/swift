package com.fx.common.queue;

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;

/**
 * {@code QueuePreToucher} — background daemon that walks the next unmapped
 * mmap pages of a Chronicle Queue segment before the hot-path producer appender
 * reaches them, eliminating page-fault latency spikes at segment roll boundaries.
 *
 * <h2>Problem solved</h2>
 * <p>
 * Chronicle Queue pre-allocates 128 MB file segments (see {@link QueueFactory}).
 * On segment roll, the JVM must fault each new 4 KB OS page into the page cache
 * on first access. On native Linux, this takes ~5-20 µs per fault. In Docker Desktop
 * (macOS) via the VirtioFS/Apple Virtualization.framework boundary, the same fault
 * costs <b>1-50 ms</b> — the direct cause of P99/Max spikes in the benchmark.
 *
 * <h2>Solution</h2>
 * <p>
 * Since Chronicle Queue 2026.x moved its native Pretoucher to Enterprise-only (and the
 * open-source fallback triggers an infinite recursion bug), this daemon manually discovers
 * the latest {@code .cq4} file, maps it via {@link FileChannel#open}, and performs a
 * compare-and-set on one {@code int} per 4 KB page in a low-priority thread. When the
 * hot-path appender reaches those pages, they are already in the page cache.
 *
 * <h2>VarHandle byte-offset contract</h2>
 * <p>
 * {@link #INT_HANDLE} is created with
 * {@code MethodHandles.byteBufferViewVarHandle(int[].class, ...)}, which views the
 * {@link java.nio.ByteBuffer} as an {@code int[]}. In Java 9+, its index parameter is a
 * <b>raw byte offset</b>, <em>not</em> an element index. To touch the page starting at
 * byte offset {@code N}, the correct coordinate is {@code N}. This operation using
 * primitive types ensures zero-allocation (Zero GC) overhead.
 *
 * <h2>Resource management</h2>
 * <p>
 * {@link FileChannel#open} is used in preference to
 * {@code new RandomAccessFile(...).getChannel()} because {@code FileChannel.open}
 * is self-contained: {@code close()} releases its file descriptor directly.
 * The two-step approach requires closing <em>both</em> the channel and the
 * {@code RandomAccessFile}; omitting either leaks a file descriptor per segment roll.
 *
 * <h2>Crash-loop protection</h2>
 * <p>
 * If an unexpected exception escapes the inner pre-touch loop, {@code touchedPosition}
 * is reset to {@code 0}. Without this reset the daemon would re-enter immediately with
 * the same bad offset and spin at 10 ms/iteration making no forward progress.
 *
 * @author FX Pipeline Team
 * @version 1.0.2
 */
public final class QueuePreToucher {

    /** Interval between pre-touch passes when the segment is fully touched or no file exists. */
    private static final long PRETOUCHER_INTERVAL_MILLIS = 10L;

    /**
     * VarHandle for performing a compare-and-set on a {@link java.nio.ByteBuffer}
     * viewed as an {@code int[]}.
     *
     * <p><b>Byte-offset contract:</b> the index argument is a <em>raw byte offset</em>.
     * To access the int at byte offset {@code N}, pass {@code N} directly. This approach
     * adheres to Zero GC constraints as it avoids allocations during the compare-and-set.
     *
     * <p>The CAS with {@code expected=0, desired=0} forces a read-modify-write cycle that
     * faults the page into the OS page cache with write permissions, without modifying
     * any data already written by the Chronicle Queue appender.
     */
    private static final VarHandle INT_HANDLE =
            MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());

    private QueuePreToucher() {
        throw new UnsupportedOperationException("QueuePreToucher is a static utility class");
    }

    /**
     * Starts a background pre-touch daemon for the given Chronicle Queue.
     *
     * <p>The daemon runs at {@link Thread#MIN_PRIORITY} so it does not compete with
     * hot-path event loop threads for CPU budget. It is a daemon thread so the JVM
     * will not wait for it on shutdown.
     *
     * @param queue the Chronicle Queue whose segment files should be pre-touched
     */
    public static void start(final SingleChronicleQueue queue) {
        final File queueDir = new File(queue.fileAbsolutePath());

        Thread.ofPlatform()
                .name("pretoucher-" + queueDir.getName())
                .daemon(true)
                .priority(Thread.MIN_PRIORITY)
                .start(() -> runLoop(queueDir));
    }

    private static void runLoop(final File queueDir) {
        File currentFile = null;
        FileChannel channel = null;
        MappedByteBuffer buffer = null;
        long touchedPosition = 0L;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                final File latestFile = getLatestCq4File(queueDir);
                if (latestFile != null) {

                    // On segment roll, open the new file and map it for pre-touching.
                    if (!latestFile.equals(currentFile)) {
                        closeQuietly(channel);
                        currentFile = latestFile;
                        // FileChannel.open is self-contained: close() releases the fd.
                        // Prefer this to RandomAccessFile.getChannel(), which requires
                        // closing both the channel and the RandomAccessFile separately
                        // to avoid a file-descriptor leak on each segment roll.
                        channel = FileChannel.open(currentFile.toPath(),
                                StandardOpenOption.READ, StandardOpenOption.WRITE);
                        final long size = channel.size();
                        buffer = (size > 0)
                                ? channel.map(FileChannel.MapMode.READ_WRITE, 0, size)
                                : null;
                        touchedPosition = 0L;
                    }

                    if (buffer != null) {
                        preTouchNextChunk(buffer, touchedPosition);
                        // Advance past this chunk. Cap at capacity so the next
                        // iteration skips the inner loop once the segment is fully touched.
                        touchedPosition = Math.min(
                                touchedPosition + (4L * 1024L * 1024L),
                                buffer.capacity());
                    }
                }
                Thread.sleep(PRETOUCHER_INTERVAL_MILLIS);

            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                // An unexpected error (e.g., buffer unmapped during a roll) could leave
                // touchedPosition at an invalid offset. Reset to 0 so the next iteration
                // retries from the start of the current segment rather than spinning
                // forever on the same failing offset.
                touchedPosition = 0L;
            }
        }

        closeQuietly(channel);
    }

    /**
     * Pre-touches the next 4 MB chunk of the buffer, starting at {@code fromPosition}.
     *
     * <p>Performs one {@link VarHandle} compare-and-set per 4 KB page. The CAS uses
     * {@code expected=0, desired=0}: if the appender has already written non-zero data
     * to this page the CAS fails silently — the page fault still occurs on the attempt,
     * which is the goal.
     *
     * <p><b>Byte-offset calculation:</b> {@link #INT_HANDLE} accepts a raw byte offset.
     * The index passed is exactly the byte offset.
     *
     * @param buffer       the memory-mapped segment buffer
     * @param fromPosition byte offset at which to start this chunk
     */
    private static void preTouchNextChunk(final MappedByteBuffer buffer, final long fromPosition) {
        final int capacity = buffer.capacity();
        final long limit = Math.min(fromPosition + (4L * 1024L * 1024L), capacity);
        long pos = fromPosition;

        while (pos < limit) {
            // INT_HANDLE uses a raw byte offset.
            // A CAS operation using primitive int is zero-allocation (Zero GC).
            INT_HANDLE.compareAndSet(buffer, (int) pos, 0, 0);
            pos += 4096L; // advance by one OS page (4 KB)
        }
    }

    /**
     * Returns the most recently modified {@code .cq4} file in the queue directory,
     * or {@code null} if none exist.
     *
     * @param dir the Chronicle Queue data directory
     * @return the latest {@code .cq4} file, or {@code null}
     */
    private static File getLatestCq4File(final File dir) {
        final File[] files = dir.listFiles((d, name) -> name.endsWith(".cq4"));
        if (files == null || files.length == 0) {
            return null;
        }
        return Arrays.stream(files)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }

    /**
     * Closes the given {@link FileChannel} silently, ignoring any {@link IOException}.
     *
     * <p>Used in the pre-touch loop where a close failure is non-fatal: the OS will
     * reclaim the file descriptor when the object is garbage-collected.
     *
     * @param channel the channel to close; {@code null} is a no-op
     */
    private static void closeQuietly(final FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (final IOException ignored) {
                // Non-fatal — the OS will reclaim the fd at the next GC cycle.
            }
        }
    }
}
