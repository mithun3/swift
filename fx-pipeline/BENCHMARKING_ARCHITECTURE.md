# High-Throughput Benchmarking & Telemetry Architecture

When building ultra-low-latency, high-throughput systems like the FX Pipeline, traditional benchmarking and monitoring techniques often fail. Standard logging frameworks allocate strings, garbage collection pauses distort results, and naive load generators fall victim to *coordinated omission*.

This document details the architecture of our benchmarking and telemetry framework, designed to measure sub-millisecond tail latencies at millions of messages per second with zero allocations on the hot path.

---

## 1. The Coordinated Omission Problem

In many performance tests, a load generator loop looks like this:

```java
long start = System.nanoTime();
send(message);
long end = System.nanoTime();
recordLatency(end - start);
```

If the system experiences a 100-millisecond GC pause, the sender also pauses. The messages that *should* have been sent during those 100ms are delayed, but when the JVM wakes up, the sender resumes and only measures the next message it successfully sends — the stall delay is completely hidden. This is **coordinated omission**.

### The Solution: Paced Sending and Intended Timestamps

Our `LoadGenerator` (in the `test` Maven module) mitigates this by pacing itself to a fixed target throughput and calculating the *intended* send time for every message in advance:

```java
// From test/src/main/java/com/fx/test/LoadGenerator.java
long intervalNanos = TimeUnit.SECONDS.toNanos(1) / targetRate;
long intendedSendTime = System.nanoTime();

while (true) {
    long now = System.nanoTime();
    if (now >= intendedSendTime) {
        flyweight.reset();
        flyweight.correlationId = ++count;
        // COORDINATED OMISSION MITIGATION:
        // Record intendedSendTime rather than 'now'. If the JVM paused or
        // we fell behind, this correctly propagates the stall delay through
        // the pipeline as measured latency at serv-c.
        flyweight.ingressNanoTime = intendedSendTime;
        flyweight.currencyPairCode = eurUsdCode;
        flyweight.side = 1;
        flyweight.notionalMinorUnits = 100_000_000L;
        flyweight.clientTier = 2;
        flyweight.clientId = 9999L;
        appender.writeDocument(flyweight);

        intendedSendTime += intervalNanos;
    }
}
```

---

## 2. Zero-Allocation Telemetry with HdrHistogram

Traditional telemetry tools (Prometheus clients, standard loggers) often allocate objects or acquire locks on the hot path. Any allocation causes CPU cache eviction and eventually triggers a minor GC pause.

To solve this, we employ two techniques:
1. **HdrHistogram** for latency recording.
2. **LMAX-style Asynchronous Logger** for diagnostic logs.

### Hot-Path Recording (serv-c)

In `PersistenceEventLoop.handle()`, we record the end-to-end latency at the earliest possible moment — before the batch accumulation:

```java
// From serv-c/src/main/java/com/fx/persistence/PersistenceEventLoop.java
event.t3ServCEntry = System.nanoTime(); // T3: serv-c entry timestamp

// End-to-end pipeline latency = T3 (persistence entry) - T0 (FIX ingress)
if (telemetryRecorder != null) {
    telemetryRecorder.recordValue(event.t3ServCEntry - event.ingressNanoTime);
}
```

`SingleWriterRecorder.recordValue()` is wait-free and allocation-free — it increments a long counter in a pre-allocated histogram array, with no locks and no garbage.

### Hot-Path Logging (All Services)

Standard `System.out.println` calls block on I/O and allocate strings. We have replaced all hot-path logging with a custom **GC-Free Asynchronous Logger**:
- Threads grab a pre-allocated `LogEvent` from a lock-free pool.
- The event is populated with static strings and primitives and offered to an Agrona `ManyToOneConcurrentArrayQueue`.
- A background `LogProcessor` thread formats the string and performs the actual I/O, completely isolating the event loop from disk latency.

### Background Harvesting (Cold Path)

A background daemon thread (inside `TelemetryRecorder`) wakes up every second, cleanly swaps the active histogram array using `getIntervalHistogram()`, and flushes the data to an `.hlog` file:

```java
// From common/src/main/java/com/fx/common/telemetry/TelemetryRecorder.java
Histogram intervalHistogram = recorder.getIntervalHistogram(intervalHistogram);
if (intervalHistogram.getTotalCount() > 0) {
    logWriter.outputIntervalHistogram(startTimeSec, endTimeSec, intervalHistogram);
}
```

The background thread allocates freely (String, I/O) — this is acceptable because it runs on a non-critical, non-isolated CPU core and never touches the hot-path thread.

### Enabling Telemetry

Telemetry is enabled by default in `PersistenceMain`. Control it via system properties:

```bash
# Disable telemetry (e.g., in CI)
-Dfx.telemetry.enabled=false

# Custom log file path
-Dfx.telemetry.log.path=/mnt/fast-disk/fx-latency.hlog
```

---

## 3. Per-Stage Timestamp Tracking (T0–T3)

To identify bottlenecks in a multi-stage pipeline, we need to know exactly how much time an event spent in each queue and each service.

We stamp primitive `long` fields directly onto the `FxMarketEvent` flyweight at the entry of each service's `handle()` method:

| Timestamp Field | Set In | Meaning |
|---|---|---|
| `ingressNanoTime` (T0) | `serv-0` `GatewayEventLoop` | FIX message decoded and written to `queue-a` |
| `t1ServAEntry` (T1) | `serv-a` `RiskValidationEventLoop` | Event read from `queue-a`, risk check begins |
| `t2ServBEntry` (T2) | `serv-b` `PricingEventLoop` | Event read from `queue-b`, spread engine begins |
| `t3ServCEntry` (T3) | `serv-c` `PersistenceEventLoop` | Event read from `queue-c`, DB write begins |

Because these are primitive fields within a single pre-allocated flyweight object, tracking stage-by-stage latency costs zero garbage and zero pointer indirection.

### Querying Per-Stage Latencies from H2

All four timestamps are persisted to the `fx_trades` table. Query stage-by-stage latencies directly from the DB:

```sql
SELECT
    correlation_id,
    (t1_serv_a_entry - ingress_nano)    AS queue_a_wait_ns,   -- Time in queue-a
    (t2_serv_b_entry - t1_serv_a_entry) AS serv_a_duration_ns, -- serv-a processing
    (t3_serv_c_entry - t2_serv_b_entry) AS serv_b_duration_ns, -- serv-b processing
    (t3_serv_c_entry - ingress_nano)    AS end_to_end_ns
FROM fx_trades
ORDER BY end_to_end_ns DESC
LIMIT 10;
```

---

## 4. Out-of-Band Distributed Tracing

In typical architectures, Distributed Tracing (e.g., OpenTelemetry, Zipkin) is integrated directly into the application threads using SDKs or asynchronous loggers. This approach is fatal to ultra-low latency constraints because these libraries allocate objects (Strings, Span contexts, Builder objects) and introduce lock contention on internal buffers.

To achieve distributed tracing without breaking Mechanical Sympathy, we employ **Out-of-band Telemetry Stitching** (Event Sourcing telemetry).

### TelemetryStitcher

Because every event already carries its Trace ID (`correlationId`) and latency boundaries (`ingressNanoTime`, `t1`, `t2`, `t3`), tracing is essentially fully implemented natively inside the queue.

The `TelemetryStitcher` is an isolated background process that tails the terminal queue (`queue-c`). It extracts these timing fields into a pre-allocated flyweight, formats them into a standard Distributed Trace JSON format, and writes them to a log file (`traces.jsonl`). 

This architecture guarantees:
1. **Zero GC overhead on the hot path**: The stitcher runs entirely on a non-critical background thread.
2. **Standard Compatibility**: The generated JSON logs can be effortlessly ingested by APM platforms like Datadog or ELK.
3. **No Lock Contention**: Chronicle Queue `Tailer` instances are lock-free and isolated from the `Appender`.

---

## 5. Visualizing the "Hockey Stick"

The `.hlog` outputs from `TelemetryRecorder` are HdrHistogram log files. Use the provided utility script to process the percentiles and generate Percentile vs. Latency charts for all pipeline stages:

```bash
./scripts/process_latency.sh /tmp/fx-latency.hlog /tmp/fx-latency-queue-a.hlog /tmp/fx-latency-serv-a.hlog /tmp/fx-latency-serv-b.hlog
```

In high-throughput systems, latency is usually stable up to the 99th percentile, after which it spikes exponentially (the "hockey stick" curve). By using a logarithmic X-axis for percentiles (90%, 99%, 99.9%, 99.99%), the script clearly visualises the exact tail latencies where the system begins to saturate.

---

## 6. Running the Benchmark

```bash
# 1. Build all modules
scripts/build.sh

# 2. Start the pipeline (all 4 services)
scripts/deploy.sh

# 3. Run the load generator at 5M msgs/sec (infinite loop, coordinated-omission-aware)
scripts/run_load_generator.sh /tmp/fx-queues/queue-a 5000000

# 4. Wait 60 seconds, then stop the pipeline (Ctrl+C or kill the PIDs)

# 5. Process and visualise the latency histograms for all stages
./scripts/process_latency.sh /tmp/fx-latency*.hlog
```

---

## 7. Hardware Sympathy Tuning

Software architecture alone cannot guarantee sub-millisecond latencies. The OS and hardware must be configured to cooperate:

1. **CPU Isolation (`isolcpus`):** Critical threads (the 4 services and the load generator) must be pinned to isolated cores where the Linux scheduler is forbidden from running other tasks.
2. **Tickless Kernel (`nohz_full`):** Disables the 1000Hz OS timer interrupt on isolated cores, preventing the CPU from being interrupted every 1ms.
3. **C-State Disabling:** Deep sleep states are disabled (`idle=poll`) to prevent the 10-100 microsecond penalty incurred when a CPU wakes up from a low-power state.
4. **CPU Governor:** Set to `performance` to prevent P-state transitions during the benchmark window.

*(See `BENCHMARK_TUNING.md` for the exact kernel boot parameters and JVM flags.)*
