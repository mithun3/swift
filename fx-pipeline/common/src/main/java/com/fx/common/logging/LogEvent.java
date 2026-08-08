package com.fx.common.logging;

/**
 * {@code LogEvent} — A reusable flyweight for log entries.
 *
 * <p>Designed to be pooled and recycled. Holds primitive arguments and
 * constant string references to avoid allocation.
 */
public final class LogEvent {
    long timestamp;
    LogLevel level;
    CharSequence message;
    long longArg;
    Object objArg;
    Throwable throwable;
    String threadName;

    /**
     * Resets the event state before returning it to the pool.
     */
    public void reset() {
        timestamp = 0;
        level = null;
        message = null;
        longArg = 0;
        objArg = null;
        throwable = null;
        threadName = null;
    }
}
