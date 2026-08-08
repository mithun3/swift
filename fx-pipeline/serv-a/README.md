# Risk Validation Service (`serv-a`)

The Risk Validation Service acts as the first line of business logic after ingestion.

## Responsibilities & Flow
1. **Tail:** Uses a busy-spin `ExcerptTailer` to continuously read events from **`queue-a`**.
2. **Validate:** Passes the `FxMarketEvent` to the `CreditCheckEngine`. It verifies the `notionalMinorUnits` against client tier thresholds using ultra-fast, primitive-only branching logic.
3. **Mutate:** Mutates the flyweight event in-place (updates the `eventStatus` flag to either `ACCEPTED` or `CREDIT_REJECTED`).
4. **Dispatch:** Appends the mutated flyweight event directly into **`queue-b`**.

## Threading
Operates on a single thread pinned to a dedicated CPU core (e.g., Core 1). It is the exclusive writer to `queue-b`, adhering to the single-writer principle to eliminate contention locks.

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

java $JVM_OPTS -cp target/serv-a-1.0-SNAPSHOT.jar:target/dependency/* com.fx.risk.RiskMain
```

> [!TIP]
> **Easier Execution**: Rather than running this manually, use the `./deploy.sh` script from the project root to automatically configure JVM arguments and start all services in the correct order. Use `./test.sh` to diagnose OS-specific JVM properties if needed.
