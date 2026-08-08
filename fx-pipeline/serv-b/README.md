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
export JVM_OPTS="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC \
-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider"

java $JVM_OPTS -cp target/serv-b-1.0-SNAPSHOT.jar:target/dependency/* com.fx.pricing.PricingMain
```

> [!TIP]
> **Easier Execution**: Rather than running this manually, use the `scripts/deploy.sh` script from the project root to automatically configure JVM arguments and start all services in the correct order. Use `scripts/test.sh` to diagnose OS-specific JVM properties if needed.
