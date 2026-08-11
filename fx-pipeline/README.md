# HFT FX Pipeline

An ultra-low-latency Foreign Exchange (FX) Pipeline designed using pure **Java 21**, **Chronicle Queue**, and **LMAX Disruptor** architectural concepts. The system is built with zero-allocation, mechanical sympathy principles ensuring garbage-free execution in the critical path.

## System Architecture & Module Layout

The system consists of **6 runnable modules/processes** communicating sequentially via memory-mapped, zero-copy Chronicle Queues (`queue-a`, `queue-b`, `queue-c`).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│                           FX Ultra-Low-Latency Pipeline                                      │
│                                                                                              │
│  ┌──────────┐  FIX/TCP   ┌──────────┐  queue-a  ┌──────────┐  queue-b  ┌──────────┐        │
│  │  client  │──────────>│  serv-0  │──────────>│  serv-a  │──────────>│  serv-b  │        │
│  │ (FIX CLI)│           │ Gateway  │           │  Risk    │           │ Pricing  │        │
│  └──────────┘           └──────────┘           └──────────┘           └────┬─────┘        │
│                                                                             │ queue-c       │
│                                                                        ┌────▼─────┐        │
│                                                                        │  serv-c  │        │
│                                                                        │ Persist  │──>H2 DB│
│                                                                        └────┬─────┘        │
│                                                                             │ queue-c       │
│                                                                        ┌────▼─────┐        │
│                                                                        │  Stitcher│──>JSON │
│                                                                        └──────────┘        │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐   │
│  │  test (LoadGenerator + FullPipelineIntegrationTest)       queue-err (error events)   │   │
│  └──────────────────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Module | Role | Input | Output | CPU Core |
|---|---|---|---|---|
| `client` | FIX 4.4 NewOrderSingle CLI sender | TCP (FIX) | → `serv-0` | N/A |
| `serv-0` | FIX ingestion gateway, correlation ID stamping | FIX / Synthetic | `queue-a` | Core 0 |
| `serv-a` | Credit check & tier-based risk validation | `queue-a` | `queue-b` | Core 1 |
| `serv-b` | Spread application & price computation | `queue-b` | `queue-c` | Core 2 |
| `serv-c` | Batch H2 persistence & HdrHistogram telemetry | `queue-c` | H2 DB + `.hlog` | Core 3 |
| `telemetry` | Out-of-band JSON Trace Stitcher | `queue-c` | `traces.jsonl` | N/A |
| `test` | Load generator & full integration test harness | N/A | `queue-a` | Isolated |

### Pipeline Stage Details

1. **`serv-0` (Client Gateway):** Accepts FIX 4.4 NewOrderSingle messages via TCP or a deterministic `SyntheticFixSource`. Decodes fields byte-by-byte with zero String allocation, generates a monotonic Correlation ID, stamps `ingressNanoTime` (T0), and appends the `FxMarketEvent` flyweight to `queue-a`.
2. **`serv-a` (Risk Validation):** Tails `queue-a` on a busy-spin event loop. Resolves client tier from `clientId`, applies credit limits, stamps `t1ServAEntry` (T1), and writes the event (ACCEPTED or CREDIT_REJECTED) to `queue-b`.
3. **`serv-b` (Pricing Engine):** Tails `queue-b`, fast-forwards terminal failures (CREDIT_REJECTED, VALIDATION_FAILED) to `queue-c` without repricing, stamps `t2ServBEntry` (T2), applies tier-based FX spreads using integer arithmetic, and writes the priced event to `queue-c`.
4. **`serv-c` (Persistence Egress):** Tails `queue-c`, stamps `t3ServCEntry` (T3), records end-to-end latency (`T3-T0`) to an HdrHistogram `.hlog` file via `TelemetryRecorder`, and batch-inserts all fields (including T1/T2/T3) into the `fx_trades` H2 table.
5. **`client` (FIX Client):** A standalone TCP FIX client that sends a single NewOrderSingle message to the gateway port for live end-to-end verification.

---

## Technical Constraints
- **LMAX Philosophy:** Single-writer principle per queue, busy-spin wait strategies (`Thread.onSpinWait()`), memory-mapped IPC via Chronicle Queue.
- **Zero-Allocation:** Mutable `FxMarketEvent` flyweights, primitive arrays, no `java.util.stream` or `String` manipulation in the hot path.
- **Mechanical Sympathy:** CPU pinning via `AffinityLock.acquireLock(cpuCore)` inside each service thread, sequential cache-friendly access, 64 MB mmap blocks.
- **Per-Stage Telemetry:** T0 (ingress), T1 (serv-a entry), T2 (serv-b entry), T3 (serv-c entry) timestamps on every event. End-to-end latency written to HdrHistogram `.hlog` for tail-latency analysis.

---

## Building the Project

Ensure you have Java 21 and Maven installed.

```bash
scripts/build.sh
```

*(Alternatively: `mvn clean package dependency:copy-dependencies -DskipTests`)*

---

## Running the Pipeline

The easiest way to run the pipeline locally is the provided deployment script. It configures OS-specific JVM arguments (ZGC tuning, Chronicle module exports) and launches all four services in reverse order (consumers first):

```bash
scripts/deploy.sh
```

*Pressing `Ctrl+C` cleanly stops all background services.*

### Manual Execution (Alternative)

Open 5 separate terminals and run in this order:

**Terminal 1 (serv-c — Persistence):**
```bash
java $JVM_OPTS -cp serv-c/target/serv-c-1.0-SNAPSHOT.jar:serv-c/target/dependency/* com.fx.persistence.PersistenceMain
```

**Terminal 2 (Telemetry Stitcher):**
```bash
java $JVM_OPTS -cp common/target/common-1.0-SNAPSHOT.jar:common/target/dependency/* com.fx.common.telemetry.TelemetryMain
```

**Terminal 3 (serv-b — Pricing):**
```bash
java $JVM_OPTS -cp serv-b/target/serv-b-1.0-SNAPSHOT.jar:serv-b/target/dependency/* com.fx.pricing.PricingMain
```

**Terminal 4 (serv-a — Risk):**
```bash
java $JVM_OPTS -cp serv-a/target/serv-a-1.0-SNAPSHOT.jar:serv-a/target/dependency/* com.fx.risk.RiskMain
```

**Terminal 5 (serv-0 — Gateway):**
```bash
# Synthetic mode (default — generates messages internally):
java $JVM_OPTS -cp serv-0/target/serv-0-1.0-SNAPSHOT.jar:serv-0/target/dependency/* com.fx.gateway.GatewayMain

# TCP mode (waits for a real FIX client connection on port 5001):
java $JVM_OPTS -Dfx.gateway.mode=tcp -cp serv-0/target/serv-0-1.0-SNAPSHOT.jar:serv-0/target/dependency/* com.fx.gateway.GatewayMain
```

---

## Sending a Live FIX Message (`client` module)

Once the gateway (`serv-0`) is in TCP mode, inject a FIX 4.4 NewOrderSingle:

```bash
scripts/send_test_message.sh [HOST] [PORT]
```

*(Default: `localhost:5001`)*

This sends a single `35=D` (NewOrderSingle) message for EUR/USD, which flows through the full pipeline and is persisted to the H2 database.

---

## Viewing Database Entries

When `serv-c` is running, it starts an H2 TCP server. Connect with any JDBC client:

- **JDBC URL:** `jdbc:h2:tcp://localhost:9092/mem:fxdb`
- **User:** `sa` / **Password:** *(blank)*
- **Full schema query:**

```sql
SELECT
    correlation_id,
    ingress_nano,
    client_tier,
    executed_price_scaled,
    spread_scaled,
    event_status,
    t1_serv_a_entry - ingress_nano AS queue_a_latency_ns,
    t2_serv_b_entry - t1_serv_a_entry AS serv_a_latency_ns,
    t3_serv_c_entry - t2_serv_b_entry AS serv_b_latency_ns,
    t3_serv_c_entry - ingress_nano   AS end_to_end_ns
FROM fx_trades
ORDER BY correlation_id DESC
LIMIT 20;
```

---

## Distributed Log Tracing (Event Stitching)

The pipeline employs an **Out-of-band Telemetry Stitcher** to provide distributed tracing across microservices without inducing GC pressure or latency on the hot path. 

Because `FxMarketEvent` natively carries a `correlationId` (the Trace ID) and nanosecond timestamps at each pipeline stage (`t1`, `t2`, `t3`), tracing is inherently baked into the Event Sourced architecture.

The `TelemetryStitcher` (in the `common` module) runs on a non-critical background thread, tailing the terminal queue (`queue-c`) to construct standard JSON traces. 

Example output trace (`traces.jsonl`):
```json
{
  "traceId": "123456",
  "type": "FxMarketEvent",
  "totalLatencyNs": 3000,
  "spans": [
    {"service": "serv-0", "timestampNs": 1000},
    {"service": "serv-a", "timestampNs": 2000},
    {"service": "serv-b", "timestampNs": 3000},
    {"service": "serv-c", "timestampNs": 4000}
  ],
  "status": 4
}
```

These JSON lines can be seamlessly ingested by log aggregation tools (like Splunk, ELK, or Datadog) to stitch up and visualize the full event lifecycle, including precise intra-stage latencies, all while keeping the critical trading path completely garbage-free.

---

## Zero-Allocation Asynchronous Logging

Standard logging frameworks (like `System.out.println` or synchronous SLF4J) are fatal to ultra-low-latency applications due to thread synchronization, blocking disk I/O, and `String` allocations on the hot path (which trigger GC pauses).

This pipeline implements a custom **LMAX-Style GC-Free Asynchronous Logger** using pure Java and Agrona:
1. **Zero Allocation**: Log statements use primitive arguments and pre-allocated `LogEvent` flyweights from a lock-free object pool. No strings are concatenated on the hot path.
2. **Ring Buffer Offloading**: Log events are offered to a lock-free `ManyToOneConcurrentArrayQueue`. The caller thread returns immediately (sub-microsecond latency).
3. **Background Processing**: A dedicated `LogProcessor` thread tails the queue, formats the strings using a recycled `StringBuilder`, and writes to `System.out` or a file asynchronously.
4. **Fallback Mode**: For local debugging or tests, logging can be instantly toggled to synchronous mode via `-Dfx.logging.async=false`.

---

## Testing

### Unit & Integration Tests

```bash
# Run all unit tests across all modules
mvn test

# Run integration tests (spins up full pipeline in-process with 1000 events)
mvn verify -pl test -Dfailsafe.fork.count=1
```

### End-to-End Verification (1 Message)

```bash
# Build first
scripts/build.sh

# Start pipeline, send 1 message, then verify DB
scripts/deploy.sh &
scripts/run_load_generator.sh /tmp/fx-queues/queue-a 1 1
scripts/view_db.sh
```

---

## High-Throughput Benchmarking & Telemetry

The pipeline includes a coordinated-omission-aware load generator and an HdrHistogram telemetry recorder. See [`BENCHMARKING_ARCHITECTURE.md`](./BENCHMARKING_ARCHITECTURE.md) for full details.

```bash
# Run the full benchmark suite (Load generation -> Latency processing -> HTML report)
# Example: target rate 5M msgs/sec for 10M messages total
./scripts/run_benchmark_suite.sh /tmp/fx-queues/queue-a 5000000 10000000 /tmp/fx-latency*.hlog
```

---

## Troubleshooting

### No Messages Appearing in Database During Load Generation

Check that you are sending to the correct queue path. By default, `scripts/deploy.sh` starts services reading from `/tmp/fx-queues/`:

```bash
# Correct:
scripts/run_load_generator.sh /tmp/fx-queues/queue-a 1 1

# Incorrect (wrong base dir):
scripts/run_load_generator.sh /tmp/queue-a 1 1
```

### Diagnostic Scripts

For NIO or `SelectorProvider` issues (macOS vs Linux):

```bash
scripts/test.sh
```

This verifies which `SelectorProvider` is loaded in the JVM — important on macOS where `EPollSelectorProvider` is not available.
