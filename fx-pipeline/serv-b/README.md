# Pricing Matching Service (`serv-b`)

The core algorithmic logic of the FX Pipeline sits in the Pricing Matching Service.

## Responsibilities & Flow
1. **Tail:** Uses a busy-spin `ExcerptTailer` to continuously read events from **`queue-b`**.
2. **Process:** If the event status is `ACCEPTED`, it passes the `FxMarketEvent` to the `SpreadEngine`.
3. **Execute:** The `SpreadEngine` calculates bid/ask spreads via pip-scaled longs (`long` arithmetic, eliminating IEEE 754 float inaccuracies) and determines the `executedPriceScaled`. 
4. **Mutate:** Sets the `eventStatus` to `PRICED` and populates the `executedPriceScaled` in the flyweight event.
5. **Dispatch:** Appends the mutated flyweight event into **`queue-c`**.

## Threading
Operates on a single thread pinned to a dedicated CPU core (e.g., Core 2). It is the exclusive writer to `queue-c`.

## How to Run

Ensure the project is built via `mvn clean package` at the root directory.

Run the service using the required JVM arguments:

```bash
# -Djava.nio.channels.spi.SelectorProvider=...EPollSelectorProvider is Linux-only
# (scripts/start.sh sets it conditionally via `uname` — omit it on macOS/other OSes).
export JVM_OPTS="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC"

java $JVM_OPTS -cp target/serv-b-1.0.0-SNAPSHOT.jar:target/dependency/* com.fx.pricing.PricingMain
```

> [!TIP]
> **Easier Execution**: Rather than running this manually, use the `scripts/start.sh` script from the project root to automatically configure JVM arguments and start all services in the correct order. Use `scripts/test.sh` to diagnose OS-specific JVM properties if needed.

---

## Observed Telemetry

Metrics from `fx-latency-serv-b.hlog` and `fx-latency-queue-b.hlog` (1,221,641 samples):

| File | Measures | P50 | P99 | Max |
|---|---|---|---|---|
| `fx-latency-serv-b` | Spread-engine duration (`t2_exit − t2_entry`) | < 1 µs | < 1 µs | 10.7 ms |
| `fx-latency-queue-b` | Queue-b wait: serv-a exit → serv-b entry (`t2 − t1_exit`) | 7.6 ms | 57.9 ms | 93.1 ms |

**serv-b's own processing is excellent** — spread application using fixed-point `long` arithmetic completes in nanoseconds with zero allocation.

**Queue-b shows moderate latency** (P50 = 7.6 ms, P99 = 57.9 ms). This is a downstream consequence of the queue-a backlog; events arrive at queue-b in bursts after serv-a drains its own backlog. The 10.7 ms serv-b Max corresponds to a sporadic JIT safepoint or OS preemption during the benchmark warm-up window.

The serv-b Max of 10.7 ms will decrease on Linux with strict CPU isolation and a fully JIT-warmed JVM.
