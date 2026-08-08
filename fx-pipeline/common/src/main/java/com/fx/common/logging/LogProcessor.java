package com.fx.common.logging;

import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * {@code LogProcessor} — The background thread that drains the async log queue.
 *
 * <p>This thread takes events off the lock-free queue, formats them using a
 * pre-allocated StringBuilder, writes them to standard out, and returns the
 * event flyweight back to the object pool.
 */
public final class LogProcessor implements Runnable {
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private final ManyToOneConcurrentArrayQueue<LogEvent> logQueue;
    private final ManyToOneConcurrentArrayQueue<LogEvent> eventPool;
    private final StringBuilder sb;
    private volatile boolean running = true;

    public LogProcessor(ManyToOneConcurrentArrayQueue<LogEvent> logQueue,
                        ManyToOneConcurrentArrayQueue<LogEvent> eventPool) {
        this.logQueue = logQueue;
        this.eventPool = eventPool;
        this.sb = new StringBuilder(256);
    }

    @Override
    public void run() {
        while (running || !logQueue.isEmpty()) {
            LogEvent event = logQueue.poll();
            if (event != null) {
                formatAndPrint(event);
                event.reset();
                eventPool.offer(event);
            } else {
                // Backoff strategy (busy spin -> yield -> park)
                Thread.yield(); // Simple yield, keeping thread active but cooperative
            }
        }
    }
    
    public void stop() {
        this.running = false;
    }

    private void formatAndPrint(LogEvent event) {
        sb.setLength(0); // Reset without allocating
        
        sb.append(formatter.format(Instant.ofEpochMilli(event.timestamp)))
          .append(" [").append(event.threadName).append("] ")
          .append(event.level.name()).append(" ")
          .append(event.message);
          
        if (event.longArg != 0) {
            sb.append(" ").append(event.longArg);
        }
        if (event.objArg != null) {
            sb.append(" ").append(event.objArg);
        }
        
        // Output asynchronously
        System.out.println(sb.toString());
        
        if (event.throwable != null) {
            event.throwable.printStackTrace(System.out);
        }
    }
}
