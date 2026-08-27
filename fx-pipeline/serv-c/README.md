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

> [!TIP]
> **Easier Execution**: Rather than running this manually, use the `scripts/deploy.sh` script from the project root to automatically configure JVM arguments and start all services in the correct order. Use `scripts/test.sh` to diagnose OS-specific JVM properties if needed.

*Note: As a consumer, this service should generally be started **first** so that it establishes its tailer cursor at the end of the queue before producers start writing.*

## Viewing Database Entries
Since this service spins up an H2 TCP Server, you can view the persisted trades by connecting to the DB using any standard JDBC client (like DBeaver, DataGrip, or the H2 Console).
- **Driver:** H2
- **JDBC URL:** `jdbc:h2:tcp://localhost:9092/mem:fxdb`
- **Username:** `sa`
- **Password:** *(leave blank)*
- **Query:** `SELECT * FROM fx_trades;`

---

## Observed Telemetry & Known Issues

Metrics from `fx-latency-serv-c.hlog` and `fx-latency-queue-c.hlog` (1,221,641 samples):

| File | Measures | P50 | P99 | P99.99 | Max |
|---|---|---|---|---|---|
| `fx-latency-serv-c` | `handle()` duration: ring-buffer enqueue + JDBC async (`t3_exit − t3_entry`) | < 1 µs | 2 µs | **4.9 ms** | **586 ms** |
| `fx-latency-queue-c` | Queue-c wait: serv-b exit → serv-c entry (`t3 − t2_exit`) | **25.7 ms** | **4,995 ms** | **5,302 ms** | **5,302 ms** |

### Root Cause — Ring Buffer Saturation

`BatchPersistenceEngine.accumulate()` spin-waits when the async ring buffer is full:

```java
while (w - r >= RING_SIZE) {   // blocks the serv-c event-loop thread
    Thread.onSpinWait();
    r = readPointer;
}
```

At 1M events/sec a 65,536-slot ring absorbs only ~65 ms of burst. When H2 `executeBatch() + commit()` in the background `db-writer` thread exceeds ~65 ms, the ring fills and the serv-c hot-path thread stalls inside `accumulate()`. While stalled, serv-c cannot read queue-c, causing the P99 = 5 s queue-c cascade seen above.

The serv-c Max of 586 ms and P99.99 of 4.9 ms are the direct observation of this spin-wait inside `handle()`.

### Fixes Applied

| Constant / Setting | Before | After | Effect |
|---|---|---|---|
| `BatchPersistenceEngine.RING_SIZE` | 65,536 | **524,288** | ~524 ms burst absorption @ 1M evt/sec; 8× headroom |
| `BatchPersistenceEngine.MAX_BATCH` | 4,096 | **32,768** | Fewer H2 `commit()` calls per second; lower amortised overhead |
| Default JDBC URL | H2 1.x params attempted | **Reverted** | `LOG=0`/`UNDO_LOG=0` are unsupported in H2 2.x MVStore; using them breaks the JDBC connection at startup |

> [!NOTE]
> H2 2.3.x uses the MVStore engine and does not support the `LOG` or `UNDO_LOG` URL parameters from H2 1.x. An in-memory H2 database already keeps all data in RAM; no WAL-level URL tuning is available or needed.
