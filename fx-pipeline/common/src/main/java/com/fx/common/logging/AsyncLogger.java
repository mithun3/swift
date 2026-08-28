package com.fx.common.logging;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code AsyncLogger} — LMAX-style garbage-free asynchronous logger.
 *
 * <h2>Thread Ownership</h2>
 * <p>
 * {@code LOG_QUEUE}, {@code EVENT_POOL}, and the background {@link LogProcessor}
 * thread are {@code static} — shared by <b>every</b> {@code AsyncLogger} instance
 * in the JVM, not one per instance. Any number of hot-path threads may call
 * {@code info}/{@code warn}/{@code error} concurrently (each enqueue is a single
 * lock-free offer to {@link ManyToOneConcurrentArrayQueue}); exactly one
 * background thread ({@code AsyncLogProcessor}) drains the queue and performs
 * the actual formatting/I/O. This is a multi-producer, single-consumer analogue
 * of the pipeline's single-writer event loops — many producers, one consumer.
 *
 * <h2>Zero-GC Invariant</h2>
 * <p>
 * {@link #acquireEvent()} pulls a pre-allocated {@link LogEvent} from
 * {@code EVENT_POOL} (pre-filled with {@code QUEUE_CAPACITY} instances at class
 * load) instead of calling {@code new}. The one exception is the pool-exhaustion
 * fallback in {@link #acquireEvent()}, which allocates a {@code new LogEvent()}
 * only if the pool is empty — an extreme-load edge case, not the steady-state
 * path. If the shared queue itself is full, {@link #enqueue} drops the log entry
 * (returns the event to the pool) rather than blocking the caller's hot path.
 * <p>
 * Uses Agrona's {@link ManyToOneConcurrentArrayQueue} to buffer log events
 * off the hot path. A background thread processes the events.
 */
public final class AsyncLogger implements Logger {
    private static final int QUEUE_CAPACITY = 65536;

    // Shared queue for all async loggers in the JVM
    private static final ManyToOneConcurrentArrayQueue<LogEvent> LOG_QUEUE = new ManyToOneConcurrentArrayQueue<>(
            QUEUE_CAPACITY);

    // Object pool for LogEvents (lock-free concurrent queue)
    private static final ManyToOneConcurrentArrayQueue<LogEvent> EVENT_POOL = new ManyToOneConcurrentArrayQueue<>(
            QUEUE_CAPACITY);

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static LogProcessor processor;
    private static Thread processorThread;

    @SuppressWarnings("unused")
    private final String name;

    static {
        // Pre-fill the object pool
        for (int i = 0; i < QUEUE_CAPACITY; i++) {
            EVENT_POOL.offer(new LogEvent());
        }
    }

    public AsyncLogger(String name) {
        this.name = name;
        if (initialized.compareAndSet(false, true)) {
            startProcessor();
        }
    }

    private static void startProcessor() {
        processor = new LogProcessor(LOG_QUEUE, EVENT_POOL);
        processorThread = new Thread(processor, "AsyncLogProcessor");
        processorThread.setDaemon(true);
        processorThread.start();

        // Add shutdown hook to flush logs
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            processor.stop();
            try {
                processorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "AsyncLogProcessor-Shutdown"));
    }

    private LogEvent acquireEvent() {
        LogEvent event = EVENT_POOL.poll();
        if (event == null) {
            // Fallback if pool is exhausted (should be rare if sized correctly)
            return new LogEvent();
        }
        return event;
    }

    private void enqueue(LogLevel level, CharSequence message, long longArg, Object objArg, Throwable t) {
        LogEvent event = acquireEvent();
        event.timestamp = System.currentTimeMillis();
        event.level = level;
        event.message = message;
        event.longArg = longArg;
        event.objArg = objArg;
        event.throwable = t;
        event.threadName = Thread.currentThread().getName();

        if (!LOG_QUEUE.offer(event)) {
            // If the logging queue is full, we drop the log to save the hot path.
            // Alternatively, we could block or fall back to SyncLogger.
            // For ultra-low latency, dropping or writing a "dropped" metric is better.
            event.reset();
            EVENT_POOL.offer(event);
        }
    }

    @Override
    public void info(CharSequence message) {
        enqueue(LogLevel.INFO, message, 0L, null, null);
    }

    @Override
    public void info(CharSequence message, long arg) {
        enqueue(LogLevel.INFO, message, arg, null, null);
    }

    @Override
    public void info(CharSequence message, Object arg) {
        enqueue(LogLevel.INFO, message, 0L, arg, null);
    }

    @Override
    public void warn(CharSequence message) {
        enqueue(LogLevel.WARN, message, 0L, null, null);
    }

    @Override
    public void warn(CharSequence message, long arg) {
        enqueue(LogLevel.WARN, message, arg, null, null);
    }

    @Override
    public void error(CharSequence message) {
        enqueue(LogLevel.ERROR, message, 0L, null, null);
    }

    @Override
    public void error(CharSequence message, Throwable t) {
        enqueue(LogLevel.ERROR, message, 0L, null, t);
    }
}
