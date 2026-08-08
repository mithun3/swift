package com.fx.common.logging;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@code AsyncLogger} — LMAX-style garbage-free asynchronous logger.
 *
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
