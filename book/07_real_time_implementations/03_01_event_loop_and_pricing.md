# Event Loop and Pricing Mechanisms

In distributed HFT pipelines, microservices don't use traditional thread pools or callback-based HTTP handlers. Instead, they employ single-threaded, busy-spin event loops that continuously tail memory-mapped queues.

## The Busy-Spin Event Loop

Consider `serv-b` (Pricing Engine). It reads from `queue-b` and writes to `queue-c`. It runs a `PricingEventLoop` on a dedicated, pinned CPU core.

```java
public static void main(final String[] args) throws InterruptedException {
    System.out.println("[serv-b] Pricing Engine starting...");
    System.out.println("[serv-b] Tailing queue-b: " + QueuePaths.QUEUE_B);
    System.out.println("[serv-b] Writing queue-c: " + QueuePaths.QUEUE_C);

    final PricingEventLoop loop = new PricingEventLoop();

    loop.start();
    System.out.println("[serv-b] Event loop started on CPU core " + PricingEventLoop.CPU_CORE);
    Thread.currentThread().join();
}
```

A busy-spin loop looks roughly like this inside:

```java
while (running) {
    if (tailer.readDocument(wire -> {
        // Read into our pre-allocated FxMarketEvent
        event.reset();
        event.readMarshallable(wire);
        
        // Process pricing logic in-place
        applySpread(event);
        
        // Write out to next queue
        appender.writeDocument(w -> event.writeMarshallable(w));
    })) {
        // Message processed, loop immediately
    } else {
        // Queue empty. Spin-wait strategy (e.g., Thread.onSpinWait())
        Thread.onSpinWait();
    }
}
```

## Integer Scaling for Pricing

Floating-point rounding errors (inherent in IEEE 754 `double` arithmetic) can be catastrophic in financial calculations. Instead, prices are stored as scaled integers (minor units).

For EUR/USD, where 1 pip = 0.0001, the price 1.0850 is scaled by 100,000 and stored as `108500L`. 

```java
/**
 * Client's requested execution price, encoded as minor units (e.g., pips × 10^5).
 *
 * <p>Stored as a {@code long} scaled integer.
 * This scale avoids all floating-point imprecision on the hot path.
 */
public long requestedPriceScaled;
```

This guarantees that applying spreads and calculating final execution prices involves simple, precise integer arithmetic, eliminating the need for `BigDecimal` (which creates garbage) while retaining absolute accuracy.
