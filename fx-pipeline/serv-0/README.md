# Gateway Service (`serv-0`)

The Client Gateway Service is the ingress point of the HFT FX Pipeline.

## Responsibilities & Flow
1. **Ingestion:** Reads FIX 4.4 messages. It supports two modes:
   - **Synthetic (Default):** Uses `SyntheticFixSource` to deterministically generate mock New Order Single (`MsgType=D`) payloads.
   - **TCP Client Mode:** Uses `TcpFixSource` to listen on a non-blocking `ServerSocketChannel` (port 5000), allowing real clients to connect and send FIX payloads.
2. **Decoding:** Uses `FixDecoder` to parse the raw byte buffer tag-by-tag. All parsing is string-free, reading ASCII characters directly into `long` or `byte` primitives.
3. **Enrichment:** 
   - Assigns a monotonically increasing `correlationId` using `CorrelationIdGenerator` (an `AtomicLong`).
   - Stamps an `ingressNanoTime` using `System.nanoTime()` for precision latency tracking.
4. **Dispatch:** Writes the enriched `FxMarketEvent` into **`queue-a`**.

## Threading
Pins itself to a dedicated CPU core (typically Core 0) using `AffinitySupport` to ensure it never relinquishes the CPU while busy-spinning for new FIX buffers.

## How to Run

Ensure the project is built via `mvn clean package` at the root directory.

Run the service using the required JVM arguments to support Chronicle Queue and ZGC:

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

# Synthetic Mode
java $JVM_OPTS -cp target/serv-0-1.0-SNAPSHOT.jar:target/dependency/* com.fx.gateway.GatewayMain

# TCP Client Mode (listens on port 5000)
java $JVM_OPTS -Dfx.gateway.mode=tcp -cp target/serv-0-1.0-SNAPSHOT.jar:target/dependency/* com.fx.gateway.GatewayMain
```
*(Make sure to run the downstream services `serv-c`, `serv-b`, and `serv-a` first so no messages are dropped.)*
