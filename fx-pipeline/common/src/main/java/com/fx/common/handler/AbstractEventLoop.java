package com.fx.common.handler;

import com.fx.common.error.ErrorQueueWriter;
import com.fx.common.event.FxMarketEvent;
import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;
import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;

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
 * <h2>endOfBatch Signal</h2>
 * <p>
 * The {@code endOfBatch} parameter passed to {@link #handle} is always {@code true}.
 * A previous implementation peeked at the next Chronicle Queue entry via
 * {@code readingDocument(false) + rollbackOnClose()} on every event to determine
 * whether more data was immediately available. Profiling showed this added 2–4 µs
 * of DocumentContext overhead per event (a 30–50% throughput reduction at 175K
 * events/sec) with no benefit — no current handler acts differently on
 * {@code endOfBatch=false}. The peek was removed; {@code handle()} always receives
 * {@code endOfBatch=true}.
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
 * <h2>Book/LMAX Disruptor Concept Mapping</h2>
 * <table border="1">
 * <caption>Disruptor concept to code mapping</caption>
 * <tr><th>Disruptor concept</th><th>This codebase</th></tr>
 * <tr><td>RingBuffer</td><td>{@link ChronicleQueue} (memory-mapped append-only log)</td></tr>
 * <tr><td>Sequencer / cursor</td><td>{@link ExcerptAppender} write position / {@link ExcerptTailer#index()}</td></tr>
 * <tr><td>SequenceBarrier</td><td>{@link ExcerptTailer} (gates what this consumer may read next)</td></tr>
 * <tr><td>EventProcessor</td><td>{@code AbstractEventLoop} (runs the poll/dispatch loop on one thread)</td></tr>
 * <tr><td>EventHandler</td><td>{@link #handle} (subclass business-logic hook)</td></tr>
 * <tr><td>EventFactory / Flyweight</td><td>{@link FxMarketEvent} (pre-allocated, reused in place)</td></tr>
 * <tr><td>WaitStrategy</td><td>{@link WaitStrategy} / {@link BusySpinWaitStrategy}</td></tr>
 * </table>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public abstract class AbstractEventLoop implements Runnable, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEventLoop.class);

    /**
     * Maximum time (ms) {@link #run()} will spend draining backlog already sitting in the
     * input queue after {@link #stop()} is called, before giving up and exiting anyway.
     *
     * <p>Without this drain phase, a service stopped while its input queue still has a
     * backlog (e.g. under sustained producer/consumer rate mismatch) would abandon those
     * events mid-pipeline: they would never be processed, and would silently disappear from
     * every downstream latency histogram with no error or log line. Override via
     * {@code -Dfx.eventloop.drainTimeoutMillis=<ms>}.
     */
    private static final long DRAIN_TIMEOUT_MILLIS =
            Long.getLong("fx.eventloop.drainTimeoutMillis", 30_000L);

        private static final boolean AFFINITY_ENABLED =
            Boolean.parseBoolean(System.getProperty("fx.affinity.enabled", "true"));

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
     * Idle behavior invoked when the poll source has no event ready.
     * Selected at startup via {@link WaitStrategy#fromSystemProperty()} so the
     * strategy can be changed per environment without recompilation:
     * <ul>
     *   <li>{@code -Dfx.waitstrategy=busyspin} (default) — {@link BusySpinWaitStrategy}
     *       for native Linux with {@code isolcpus}</li>
     *   <li>{@code -Dfx.waitstrategy=phased} — {@link PhasedBackOffWaitStrategy}
     *       for Docker / VM shared-CPU environments</li>
     * </ul>
     */
    protected final WaitStrategy waitStrategy;

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

    /** Unexpected failure that terminated the event-loop thread, if any. */
    private volatile Throwable terminationFailure;

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
        // Select the wait strategy based on the fx.waitstrategy system property.
        // BusySpinWaitStrategy (default) is optimal on native Linux with isolcpus.
        // PhasedBackOffWaitStrategy is optimal in Docker / VM shared-CPU environments.
        // Set -Dfx.waitstrategy=phased in docker-compose.yml service commands.
        this.waitStrategy = WaitStrategy.fromSystemProperty();
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
        AffinityLock affinityLock = null;
        ExcerptAppender appender = null;

        try {
            if (AFFINITY_ENABLED && cpuCore >= 0) {
                try {
                    affinityLock = AffinityLock.acquireLock(cpuCore);
                } catch (final RuntimeException ex) {
                    logger.warn("[" + name + "] CPU affinity unavailable; continuing unpinned: "
                            + ex.getMessage());
                }
            }

            // A single ExcerptAppender reused across all writes to the output queue.
            // ExcerptAppender is NOT thread-safe — but that is fine here because this
            // method runs on exactly one thread (the single-writer principle).
            appender = (outputQueue != null) ? outputQueue.createAppender() : null;

            // Delegate the poll/dispatch loop to the subclass hook below. Every
            // service shares CPU-affinity pinning and appender lifecycle from
            // this method uniformly; only the poll source and dispatch body
            // (Chronicle tailer vs. GatewayEventLoop's FixMessageSource) differ.
            runLoop(appender);
        } catch (final Throwable failure) {
            terminationFailure = failure;
            logger.error("[" + name + "] Event loop terminated unexpectedly.", failure);
        } finally {
            running.set(false);
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
     * Runs the busy-spin poll/dispatch loop until {@link #stop()} is signalled.
     *
     * <p>Default implementation: tails {@link #inputQueue} via Chronicle Queue's
     * {@link ExcerptTailer}, dispatching each event to {@link #handle}, then
     * drains any backlog left in the queue once stopped (see
     * {@link #drainRemainingBacklog}).
     *
     * <p>{@code GatewayEventLoop} overrides this method to poll a
     * {@code FixMessageSource} instead of tailing a Chronicle Queue — CPU
     * affinity pinning and the appender's open/close lifecycle in {@link #run()}
     * apply uniformly regardless of which poll source a subclass uses.
     *
     * @param appender the output-queue appender for this service; may be
     *                 {@code null} for terminal services with no output queue
     */
    protected void runLoop(final ExcerptAppender appender) {
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

                    // endOfBatch=true: the readingDocument(false)+rollbackOnClose() peek
                    // that previously computed this flag added 2-4 µs of Chronicle
                    // DocumentContext overhead per event and is not used by any handler.
                    // Passing true unconditionally removes that dead overhead entirely.
                    try {
                        // Delegate to the concrete subclass for business logic.
                        handle(flyweight, sequence, true, appender);
                    } catch (final Exception ex) {
                        // Route the poisoned event to the error queue rather than
                        // crashing the pipeline thread. The error writer is
                        // allocation-free and Chronicle-backed.
                        errorWriter.write(flyweight, name, ex.getMessage());
                        // Swallow — the loop continues with the next event.
                    }
                } else {
                    // No event available — defer to the configured WaitStrategy.
                    waitStrategy.idle();
                }
            }

            // Drain-then-stop: stop() only signals "accept no new work" — it must not abandon
            // events already sitting in the input queue. Continue processing whatever backlog
            // exists right now (bounded by DRAIN_TIMEOUT_MILLIS) so a service stopped mid-backlog
            // doesn't silently drop events from the pipeline and its telemetry.
            drainRemainingBacklog(tailer, appender);
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
     * Drains any backlog already sitting in the input queue after {@link #stop()} has been
     * signalled, instead of abandoning it mid-pipeline.
     *
     * <p>Bounded by {@link #DRAIN_TIMEOUT_MILLIS} so a queue that never quiesces (e.g. an
     * upstream producer that is still actively writing) cannot hang shutdown indefinitely.
     * Exits as soon as the tailer reports no more currently-available data — it does not wait
     * for future writes.
     *
     * @param tailer   the input queue tailer, still open at this point
     * @param appender the output queue appender to forward drained events to; may be {@code null}
     */
    private void drainRemainingBacklog(final ExcerptTailer tailer, final ExcerptAppender appender) {
        final long deadlineNanos = System.nanoTime() + (DRAIN_TIMEOUT_MILLIS * 1_000_000L);
        long drainedCount = 0L;

        while (System.nanoTime() < deadlineNanos) {
            flyweight.reset();
            final boolean eventRead = tailer.readDocument(flyweight);
            if (!eventRead) {
                // Tailer has caught up — no backlog remains. Drain complete.
                break;
            }
            drainedCount++;
            final long sequence = tailer.index();
            try {
                handle(flyweight, sequence, true, appender);
            } catch (final Exception ex) {
                errorWriter.write(flyweight, name, ex.getMessage());
            }
        }

        if (drainedCount > 0) {
            logger.info("[" + name + "] Drained backlog event(s) before shutdown: ", drainedCount);
        }
        if (System.nanoTime() >= deadlineNanos) {
            logger.warn("[" + name + "] Drain timeout (" + DRAIN_TIMEOUT_MILLIS
                    + "ms) reached — backlog may remain unprocessed in the input queue.");
        }
    }

    /**
     * Signals the event loop to stop accepting new work.
     *
     * <p>This method is safe to call from any thread. It does not abandon events already
     * sitting in the input queue — {@link #run()} continues draining any existing backlog
     * (see {@link #drainRemainingBacklog}) before the loop actually exits.
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

    /** Throws if this event loop stopped because of an unexpected failure. */
    public void throwIfTerminatedUnexpectedly() {
        if (terminationFailure != null) {
            throw new IllegalStateException(name + " event loop terminated unexpectedly",
                    terminationFailure);
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
