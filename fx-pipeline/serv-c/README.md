# Persistence Egress Service (`serv-c`)

The terminal point of the critical path, responsible for durable logging and state saving.

## Responsibilities & Flow
1. **Tail:** Uses a busy-spin `ExcerptTailer` to continuously read events from **`queue-c`**.
2. **Batch & Persist:** Utilizes `BatchPersistenceEngine` to accumulate processed events in-memory.
3. **Database Write:** Uses a JDBC connection to an in-memory H2 database to execute high-speed, non-blocking batch `INSERT` statements to durably log `correlationId`, `executedPriceScaled`, `eventStatus`, etc.
4. **Database Querying (External):** Starts an H2 TCP Server on port `9092` at startup. External JDBC clients can connect to `jdbc:h2:tcp://localhost:9092/mem:fxdb` to query the `fx_trades` table live.
5. **Egress:** (Conceptual phase) Marks the event as `PERSISTED` and would theoretically emit TCP ACK notifications back to the client.

## Threading
Operates on a single thread pinned to a dedicated CPU core (e.g., Core 3).

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

java $JVM_OPTS -cp target/serv-c-1.0-SNAPSHOT.jar:target/dependency/* com.fx.persistence.PersistenceMain
```

*Note: As a consumer, this service should generally be started **first** so that it establishes its tailer cursor at the end of the queue before producers start writing.*
