package com.fx.common.logging;

/**
 * {@code LoggerFactory} — Factory for obtaining loggers.
 *
 * <p>Reads the system property `fx.logging.async` (defaults to "true")
 * to determine whether to provide the ultra-low-latency `AsyncLogger`
 * or the debugging fallback `SyncLogger`.
 */
public final class LoggerFactory {
    
    private static final boolean IS_ASYNC = 
        Boolean.parseBoolean(System.getProperty("fx.logging.async", "true"));

    private LoggerFactory() {}

    /**
     * Gets a logger for the given class name.
     */
    public static Logger getLogger(String name) {
        if (IS_ASYNC) {
            return new AsyncLogger(name);
        } else {
            return new SyncLogger(name);
        }
    }
    
    /**
     * Gets a logger for the given class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }
}
