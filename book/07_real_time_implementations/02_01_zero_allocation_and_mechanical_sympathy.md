# Zero-Allocation and Mechanical Sympathy in Practice

A cornerstone of High-Frequency Trading (HFT) architectures is the avoidance of memory allocations in the critical path. The JVM's Garbage Collector (GC), even modern variants like ZGC, introduces non-deterministic pauses that are unacceptable when measuring latency in microseconds or nanoseconds.

## The Flyweight Pattern in HFT Context

In the LMAX Disruptor architecture, a single mutable event object is pre-allocated at startup and reused for every message. This eliminates the GC pressure that would result from allocating millions of new DTOs per second.

```java
public final class FxMarketEvent extends SelfDescribingMarshallable {

    public long correlationId;
    public long ingressNanoTime;
    public long currencyPairCode;
    public byte side;
    public long notionalMinorUnits;

    // Flyweight reset method
    public void reset() {
        correlationId        = 0L;
        ingressNanoTime      = 0L;
        // ... (reset other primitives)
    }
}
```

The flyweight is filled with new values before being written to the Chronicle Queue, and then populated from the queue into a reused instance on the consumer side.

## Cache-Line Alignment

A modern CPU cache line is 64 bytes. In `FxMarketEvent`, related primitive fields are grouped together so that a single cache-line fetch pulls in all the data an event handler needs in one shot, avoiding expensive cache misses.

The fields are ordered by access frequency and logical group:
1. **Identity group** (`correlationId`, `ingressNanoTime`) — needed by every service for correlation and latency measurement.
2. **FX instrument group** (`currencyPair`, `side`, `notional`) — needed for all business logic.
3. **Pricing group** (`requestedPrice`, `executedPrice`, `spread`) — written by serv-b, read by serv-c.

## Avoiding String Allocations

Currency pairs (e.g., "EUR/USD") are normally represented as Strings. In an HFT pipeline, they are encoded as a compact `long`, packing two 3-letter ISO 4217 currency codes into 6 bytes.

```java
// Packing 'EUR' and 'USD' into a single long without String allocations
long encoded = 0L;
// Pack base currency into bits 40–16 (3 bytes, high side)
for (int i = 0; i < 3; i++) {
    encoded |= ((long) (baseCurrency[i] & 0xFF)) << ((5 - i) * 8);
}
// Pack quote currency into bits 23–0 (3 bytes, low side)
for (int i = 0; i < 3; i++) {
    encoded |= ((long) (quoteCurrency[i] & 0xFF)) << ((2 - i) * 8);
}
```

## JVM Tuning for Mechanical Sympathy

To maximize performance, specific JVM arguments are used to configure ZGC, ensure memory pages are pre-touched, and disable explicit GC calls:

```bash
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC
```

> [!NOTE]
> Thread affinity is also critical. By pinning the event loops of `serv-a` and `serv-b` to specific CPU cores, we prevent the OS from migrating the thread, keeping L1/L2 caches hot and avoiding costly context switches.
