package com.fx.common.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * {@code SyncLogger} — A synchronous logger that wraps System.out.println.
 *
 * <p>Useful for local development, debugging, or scenarios where blocking I/O
 * is acceptable (e.g., test suites).
 */
public final class SyncLogger implements Logger {
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());
            
    private final String name;

    public SyncLogger(String name) {
        this.name = name;
    }

    private void log(LogLevel level, CharSequence message, long longArg, Object objArg, Throwable t) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(formatter.format(Instant.now()))
          .append(" [").append(Thread.currentThread().getName()).append("] ")
          .append(level.name()).append(" ")
          .append(name).append(" - ")
          .append(message);
          
        if (longArg != 0) {
            sb.append(" ").append(longArg);
        }
        if (objArg != null) {
            sb.append(" ").append(objArg);
        }
        
        System.out.println(sb.toString());
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }

    @Override
    public void info(CharSequence message) {
        log(LogLevel.INFO, message, 0L, null, null);
    }

    @Override
    public void info(CharSequence message, long arg) {
        log(LogLevel.INFO, message, arg, null, null);
    }

    @Override
    public void info(CharSequence message, Object arg) {
        log(LogLevel.INFO, message, 0L, arg, null);
    }

    @Override
    public void warn(CharSequence message) {
        log(LogLevel.WARN, message, 0L, null, null);
    }

    @Override
    public void warn(CharSequence message, long arg) {
        log(LogLevel.WARN, message, arg, null, null);
    }

    @Override
    public void error(CharSequence message) {
        log(LogLevel.ERROR, message, 0L, null, null);
    }

    @Override
    public void error(CharSequence message, Throwable t) {
        log(LogLevel.ERROR, message, 0L, null, t);
    }
}
