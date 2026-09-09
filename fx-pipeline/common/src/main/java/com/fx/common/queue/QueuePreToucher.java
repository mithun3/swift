package com.fx.common.queue;

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
 * the latest {@code .cq4} file, maps it via {@link FileChannel#map}, and reads a single
 * byte per 4 KB page in a low-priority thread. When the hot-path appender reaches those
 * pages, they are already in the page cache.
 *
 * @author FX Pipeline Team
 * @version 1.0.1
 */
public final class QueuePreToucher {

    private static final long PRETOUCHER_INTERVAL_MILLIS = 10L;
    private static final VarHandle INT_HANDLE = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());

    private QueuePreToucher() {
        throw new UnsupportedOperationException("QueuePreToucher is a static factory");
    }

    public static void start(final SingleChronicleQueue queue) {
        final File queueDir = new File(queue.fileAbsolutePath());

        final Thread daemon = Thread.ofPlatform()
                .name("pretoucher-" + queueDir.getName())
                .daemon(true)
                .priority(Thread.MIN_PRIORITY)
                .unstarted(() -> runLoop(queueDir));

        daemon.start();
    }

    private static void runLoop(final File queueDir) {
        File currentFile = null;
        FileChannel channel = null;
        MappedByteBuffer buffer = null;
        long touchedPosition = 0;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                final File latestFile = getLatestCq4File(queueDir);
                if (latestFile != null) {
                    if (currentFile == null || !currentFile.equals(latestFile)) {
                        if (channel != null) {
                            try { channel.close(); } catch (Exception ignored) {}
                        }
                        currentFile = latestFile;
                        // Map the new segment file
                        final RandomAccessFile raf = new RandomAccessFile(currentFile, "rw");
                        channel = raf.getChannel();
                        final long size = channel.size();
                        if (size > 0) {
                            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
                        }
                        touchedPosition = 0;
                    }

                    if (buffer != null) {
                        final int capacity = buffer.capacity();
                        // Pre-touch next 4 MB chunk per iteration (1000 pages of 4 KB)
                        final long targetPosition = Math.min(touchedPosition + (4 * 1024 * 1024), capacity);
                        while (touchedPosition < targetPosition) {
                            // Write one byte via CAS to fault the page into the OS page cache with WRITE permissions.
                            // CAS ensures we only write if the appender hasn't reached here yet (memory is 0),
                            // preventing data corruption if the hot-path appender overtakes the pretoucher.
                            INT_HANDLE.compareAndSet(buffer, (int) touchedPosition, 0, 0);
                            touchedPosition += 4096; // advance by one 4KB page
                        }
                    }
                }
                Thread.sleep(PRETOUCHER_INTERVAL_MILLIS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final Exception e) {
                // File might be locked or unmapped temporarily, ignore and retry next cycle
            }
        }

        if (channel != null) {
            try { channel.close(); } catch (Exception ignored) {}
        }
    }

    private static File getLatestCq4File(final File dir) {
        final File[] files = dir.listFiles((d, name) -> name.endsWith(".cq4"));
        if (files == null || files.length == 0) {
            return null;
        }
        return Arrays.stream(files)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }
}
