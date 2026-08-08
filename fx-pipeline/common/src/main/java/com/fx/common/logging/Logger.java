package com.fx.common.logging;

/**
 * {@code Logger} — Standard interface for GC-free logging.
 *
 * <p>Implementations should ensure that these calls do not allocate objects
 * on the hot path (e.g., no String concatenation or auto-boxing).
 */
public interface Logger {
    void info(CharSequence message);
    void info(CharSequence message, long arg);
    void info(CharSequence message, Object arg);
    
    void warn(CharSequence message);
    void warn(CharSequence message, long arg);
    
    void error(CharSequence message);
    void error(CharSequence message, Throwable t);
}
