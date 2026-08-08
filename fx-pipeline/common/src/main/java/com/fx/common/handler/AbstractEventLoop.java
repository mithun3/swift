package com.fx.common.handler;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.FxMarketEvent;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code AbstractEventLoop} — Base class for all single-writer pipeline services.
 *
 * <h2>LMAX Single-Writer Principle</h2>
 * <p>
 * The LMAX Disruptor architecture mandates that each ring buffer (queue) has exactly
 * one producer thread. This class enforces that contract: the event loop runs on a
 * single dedicated platform thread, optionally pinned to a specific CPU core.
 *
 * <h2>BusySpin Wait Strategy</h2>
 * <p>
 * Rather than yielding or sleeping when the input queue is empty, the loop calls
 * {@link Thread#onSpinWait()} — a JVM hint (maps to PAUSE/YIELD CPU instruction)
 * that reduces power consumption and prevents the CPU from thrashing the memory
 * bus while spinning. This keeps the thread hot on-CPU with microsecond wake-up
 * latency, eliminating the 20–100µs context-switch penalty of OS-level blocking.
 *
 * <h2>Error Routing</h2>
 * <p>
 * If an exception escapes the handler, the event is forwarded to the error queue
 * via {@link ErrorQueueWriter}, the loop state is logged (without String allocation
 * on the hot path), and execution continues with the next event. The pipeline thread
 * is never terminated by a processing failure.
 *
 * <h2>Lifecycle</h2>
 * <pre>
 *   start() → [pinned thread] → eventLoop() → [busy-spin tail] → stop() → shutdown
 * </pre>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public abstract class AbstractEventLoop implements Runnable, AutoCloseable {

    /** Human-readable name for this event loop (used in thread naming and logs). */
    protected final String name;

    /**
     * The Chronicle Queue this loop reads events from.
     * Always consumed by a single tailer thread — never shared across threads.
     */
    protected final ChronicleQueue inputQueue;

    /**
     * The Chronicle Queue this loop writes processed events to.
     * May be {@code null} for terminal services (serv-c) that write to a DB sink.
     */
    protected final ChronicleQueue outputQueue;

    /**
     * Routes poisoned events off the critical path without throwing exceptions.
     * Backed by a separate low-priority Chronicle Queue.
     */
    protected final ErrorQueueWriter errorWriter;

    /**
     * Reusable flyweight event populated from the queue on each iteration.
     * Pre-allocated at construction — never replaced with a new instance.
     */
    protected final FxMarketEvent flyweight;

    /**
     * Volatile flag controlling the event loop lifecycle.
     * Using {@code AtomicBoolean} ensures the loop thread sees the stop signal
     * written by the shutdown thread without requiring synchronisation or locks.
     * A plain {@code volatile boolean} would also work here, but {@code AtomicBoolean}
     * exposes {@code compareAndSet} for more complex shutdown protocols if needed.
     */
    private final AtomicBoolean running;

    /**
     * CPU core to pin this event loop thread to. {@code -1} means no pinning.
     * On macOS (dev), affinity pinning is silently skipped by the native library.
     */
    private final int cpuCore;

    /** The platform thread executing this event loop. */
    private Thread eventLoopThread;

    /**
     * Constructs a new event loop for a pipeline service.
     *
     * @param name        human-readable service name, used in thread name
     * @param inputQueue  Chronicle Queue to tail events from; must not be null
     * @param outputQueue Chronicle Queue to append events to; may be null (terminal)
     * @param errorWriter writer for the error Chronicle Queue; must not be null
     * @param cpuCore     CPU core to pin to (0-indexed), or {@code -1} for no pinning
     */
    protected AbstractEventLoop(final String name,
                                 final ChronicleQueue inputQueue,
                                 final ChronicleQueue outputQueue,
                                 final ErrorQueueWriter errorWriter,
                                 final int cpuCore) {
        this.name        = name;
        this.inputQueue  = inputQueue;
        this.outputQueue = outputQueue;
        this.errorWriter = errorWriter;
        this.cpuCore     = cpuCore;
        this.running     = new AtomicBoolean(false);
        // Pre-allocate the flyweight ONCE at startup. This is the one and only
        // heap allocation for event data — all subsequent processing is done by
        // mutating this single instance in-place.
        this.flyweight   = new FxMarketEvent();
    }

    /**
     * Starts the event loop on a dedicated named platform thread.
     *
     * <p>This method returns immediately. The event loop runs asynchronously
     * on the spawned thread. Use {@link #stop()} to request a clean shutdown.
     *
     * <p><b>CPU pinning:</b> If {@code cpuCore >= 0}, an {@link AffinityThreadFactory}
     * is used to create the thread. The factory binds the thread to the specified core
     * using OS-native affinity calls (via JNA) the moment the thread starts.
     */
    public void start() {
        running.set(true);
        // Thread naming follows the pattern "fx-<service>-<core>" for easy identification
        // in thread dumps and monitoring tools (jstack, async-profiler, etc.).
        final String threadName = "fx-" + name + "-cpu" + cpuCore;

        // Create a standard platform thread regardless of pinning setting.
        // The actual CPU affinity binding is deferred to the start of run(), where
        // the OS-level thread identity exists. Binding inside the thread (via
        // AffinityLock.acquireLock) is the correct approach on both Linux and macOS.
        // AffinityThreadFactory with SAME_CORE incorrectly pins to the factory
        // thread's core, not to the configured cpuCore — hence this design.
        eventLoopThread = Thread.ofPlatform()
                .name(threadName)
                .unstarted(this);

        eventLoopThread.start();
    }

    /**
     * The event loop body — tails the input queue and dispatches to {@link #handle}.
     *
     * <p>This is called by the JVM when the platform thread starts (via {@link Runnable}).
     * It must never be called directly.
     */
    @Override
    public void run() {
        // CPU pinning: if a specific core is requested, acquire an AffinityLock
        // from inside the thread where the OS thread identity has been established.
        // This is the correct location — AffinityLock.acquireLock(core) calls
        // pthread_setaffinity_np (Linux) / thread_policy_set (macOS) on the
        // calling thread. On macOS, affinity is advisory; on Linux with isolcpus,
        // it is strict. A cpuCore of -1 disables pinning (used in tests and
        // environments without dedicated cores).
        final AffinityLock affinityLock = (cpuCore >= 0)
                ? AffinityLock.acquireLock(cpuCore)
                : null;

        // A single ExcerptAppender reused across all writes to the output queue.
        // ExcerptAppender is NOT thread-safe — but that is fine here because this
        // method runs on exactly one thread (the single-writer principle).
        // Chronicle Queue 2026.6: createAppender() is the correct API.
        final ExcerptAppender appender = (outputQueue != null)
                ? outputQueue.createAppender()
                : null;

        // ExcerptTailer reads events sequentially from the tail of the input queue.
        // It maintains its own read position (index), so no external index tracking needed.
        try (final ExcerptTailer tailer = inputQueue.createTailer(name)) {
            while (running.get()) {
                // Reset the flyweight before population to clear stale fields from
                // the previous iteration — prevents cross-event data contamination.
                flyweight.reset();

                // readDocument returns true if an event was available and read.
                // Returns false (without blocking) if the queue is currently empty.
                final boolean eventRead = tailer.readDocument(flyweight);

                if (eventRead) {
                    // Capture the current tailer index as the sequence number.
                    final long sequence = tailer.index();

                    // endOfBatch: attempt a non-blocking read to see if more data is ready.
                    // readingDocument(false) = non-blocking peek. If not present -> endOfBatch.
                    boolean endOfBatch = true;
                    try (final DocumentContext peeked = tailer.readingDocument(false)) {
                        if (peeked.isPresent()) {
                            endOfBatch = false;
                            peeked.rollbackOnClose(); // Prevent tailer index from advancing
                        }
                    }
                    try {
                        // Delegate to the concrete subclass for business logic.
                        handle(flyweight, sequence, endOfBatch, appender);
                    } catch (final Exception ex) {
                        // Route the poisoned event to the error queue rather than
                        // crashing the pipeline thread. The error writer is
                        // allocation-free and Chronicle-backed.
                        errorWriter.write(flyweight, name, ex.getMessage());
                        // Swallow — the loop continues with the next event.
                    }
                } else {
                    // No event available — busy-spin with a CPU hint.
                    // Thread.onSpinWait() maps to PAUSE (x86) / YIELD (ARM),
                    // reducing memory-bus contention while keeping the thread hot.
                    Thread.onSpinWait();
                }
            }
        } finally {
            // Release the CPU affinity lock before the thread exits,
            // returning the core to the system for potential reassignment.
            if (affinityLock != null) {
                affinityLock.release();
            }
            // Close appender if it was opened. This flushes any pending writes
            // and releases the memory-mapped segment handle.
            if (appender != null) {
                appender.close();
            }
        }
    }

    /**
     * Concrete handler method implemented by each pipeline service.
     *
     * <p>This is where all business logic lives. Receives the populated flyweight
     * and, if business logic succeeds, writes the mutated event to the output queue
     * via the provided {@code appender}.
     *
     * @param event      the mutable flyweight populated from the input queue
     * @param sequence   the Chronicle Queue index of this excerpt
     * @param endOfBatch {@code true} if no further events are immediately available
     * @param appender   the output queue appender; may be {@code null} for terminal services
     */
    protected abstract void handle(FxMarketEvent event,
                                   long sequence,
                                   boolean endOfBatch,
                                   ExcerptAppender appender);

    /**
     * Signals the event loop to stop after completing its current event.
     *
     * <p>This method is safe to call from any thread. The loop will exit cleanly
     * after the current {@link #handle} invocation completes.
     */
    public void stop() {
        running.set(false);
    }

    /**
     * Blocks the calling thread until the event loop thread terminates.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        if (eventLoopThread != null) {
            eventLoopThread.join();
        }
    }

    /**
     * {@link AutoCloseable} implementation — stops the loop and closes both queues.
     *
     * <p>Intended for use in try-with-resources in integration tests and main() methods.
     */
    @Override
    public void close() {
        stop();
        inputQueue.close();
        if (outputQueue != null) {
            outputQueue.close();
        }
        errorWriter.close();
    }

    /**
     * Returns {@code true} if the event loop is currently running.
     *
     * @return {@code true} if the loop thread is active
     */
    public boolean isRunning() {
        return running.get();
    }
}
