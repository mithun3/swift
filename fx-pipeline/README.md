# HFT FX Pipeline

An ultra-low-latency Foreign Exchange (FX) Pipeline designed using pure Java 21, Chronicle Queue, and LMAX Disruptor architectural concepts. The system is built with zero-allocation, mechanical sympathy principles ensuring garbage-free execution in the critical path.

## System Architecture & Flow

The system consists of 4 microservices communicating sequentially via memory-mapped, zero-copy Chronicle Queues (`queue-a`, `queue-b`, `queue-c`).

```text
Synthetic FIX ──> [serv-0] ──queue-a──> [serv-a] ──queue-b──> [serv-b] ──queue-c──> [serv-c] ──> H2 DB
```

1. **serv-0 (Client Gateway):** Ingests incoming FIX messages, decodes them without string allocation, generates a monotonic Correlation ID, stamps an ingress nanosecond timestamp, and appends the `FxMarketEvent` flyweight to `queue-a`.
2. **serv-a (Risk Validation):** Tails `queue-a` using a busy-spin event loop. Performs credit and tier checks, mutates the event state in-place, and writes it to `queue-b`.
3. **serv-b (Pricing Matching):** Tails `queue-b`, applies FX spreads and normalizes pricing, updates the executed price, and writes it to `queue-c`.
4. **serv-c (Persistence Egress):** Tails `queue-c` and asynchronously batches writes into an in-memory H2 database.

## Technical Constraints
- **LMAX Philosophy:** Single-writer principle per queue, busy-spin wait strategies, memory-mapped IPC.
- **Zero-Allocation:** Mutable `FxMarketEvent` flyweights, primitive arrays, no `java.util.stream` or `String` manipulation in the hot path.
- **Mechanical Sympathy:** CPU pinning using `Java-Thread-Affinity`, sequential cache-friendly access, and cache-line padded data structures.

## Building the Project

Ensure you have Java 21 and Maven installed.

```bash
mvn clean package
```

## Running the Pipeline

To run the pipeline locally, you must start the services in **reverse order** (consumers first) so that no messages are missed before tailers attach to the queues.

The JVM requires specific launch arguments to allow Chronicle Queue direct memory access and to tune the Z Garbage Collector (ZGC).

### Required JVM Arguments
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
```

### Execution Commands

Open 4 separate terminals and execute the following commands in order from root:

**Terminal 1 (serv-c):**
```bash
java $JVM_OPTS -cp serv-c/target/serv-c-1.0-SNAPSHOT.jar:serv-c/target/dependency/* com.fx.persistence.PersistenceMain
```
*(Alternatively via maven: `mvn exec:java -pl serv-c -Dexec.mainClass="com.fx.persistence.PersistenceMain" -Dexec.args="$JVM_OPTS"`)*

**Terminal 2 (serv-b):**
```bash
java $JVM_OPTS -cp serv-b/target/serv-b-1.0-SNAPSHOT.jar:serv-b/target/dependency/* com.fx.pricing.PricingMain
```

**Terminal 3 (serv-a):**
```bash
java $JVM_OPTS -cp serv-a/target/serv-a-1.0-SNAPSHOT.jar:serv-a/target/dependency/* com.fx.risk.RiskMain
```

**Terminal 4 (serv-0):**
```bash
# To run in synthetic mode (default):
java $JVM_OPTS -cp serv-0/target/serv-0-1.0-SNAPSHOT.jar:serv-0/target/dependency/* com.fx.gateway.GatewayMain

# OR to run in TCP mode (to accept real client connections):
java $JVM_OPTS -Dfx.gateway.mode=tcp -cp serv-0/target/serv-0-1.0-SNAPSHOT.jar:serv-0/target/dependency/* com.fx.gateway.GatewayMain
```

### Sending a Live FIX Message
Once `serv-0` is running in TCP mode, you can use the standalone client application to inject a live FIX message. Open a 5th terminal:

```bash
java -cp client/target/client-1.0-SNAPSHOT.jar com.fx.client.FixClientMain
```

### Viewing Database Entries
When `serv-c` is running, it spins up an H2 TCP Server. You can view the persisted trades by connecting to the DB using any standard JDBC client (like DBeaver, DataGrip, or the H2 Console).
- **JDBC URL**: `jdbc:h2:tcp://localhost:9092/mem:fxdb`
- **User**: `sa`
- **Password**: *(leave blank)*
- **Query**: `SELECT * FROM fx_trades;`

### Testing End-to-End
To run the automated integration tests that spin up all pipelines using temporary queues and assert full 1000-message delivery:

```bash
mvn verify -pl test -Dfailsafe.fork.count=1
```
