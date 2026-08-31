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

Our `LoadGenerator` (in the `test` Maven module) mitigates this by pacing itself to a fixed target throughput and calculating the *intended* send time for every message in advance.

The load generator supports two delivery modes, selected via a flag:

| Mode | Flag | Description |
|---|---|---|
| **TCP** (default) | `--tcp` | Connects to serv-0 on `:5001` and injects raw FIX bytes. The full gateway path (decode → correlationId → ingress timestamp) is exercised. |
| **Direct** | `--direct` | Writes a pre-built `FxMarketEvent` flyweight directly to `queue-a`, bypassing serv-0 entirely. Useful for isolating downstream service latency. |

In **direct mode**, `ingressNanoTime` is set to the *intended* send time (not actual), correctly propagating any stall delay through the pipeline as measured latency at serv-c.

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
        flyweight.ingressNanoTime = intendedSendTime; // direct mode
        flyweight.currencyPairCode = eurUsdCode;
        flyweight.side = 1;
        flyweight.notionalMinorUnits = 100_000_000L;
        flyweight.clientTier = 2;
        flyweight.clientId = 9999L;
        appender.writeDocument(flyweight); // direct mode: write to queue-a
        // In TCP mode: write raw FIX bytes to serv-0 via SocketChannel instead

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
    // Single-argument overload: reads start/end timestamps from the histogram's
    // own internal fields. An earlier 3-arg overload that computed a wall-clock
    // offset from a base time produced near-zero values and collapsed all
    // percentiles to 0.00 µs — replaced with this overload.
    logWriter.outputIntervalHistogram(intervalHistogram);
}
```

The background thread allocates freely (String, I/O) — this is acceptable because it runs on a non-critical, non-isolated CPU core and never touches the hot-path thread.

### Enabling Telemetry

Telemetry is enabled by default in every pipeline service main (`GatewayMain`,
`RiskMain`, `PricingMain`, `PersistenceMain`) — each stamps and records its own
per-stage latency independently. Control it via system properties:

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

To generate a consolidated, human-readable **`latency_report.html`** file, use the separate Python script. A manifest is optional for legacy/manual runs, but recommended because it preserves provenance in the report:
```bash
python3 scripts/generate_html_report.py \
    --manifest /tmp/run_manifest.json \
    /tmp/fx-latency*.hlog
```

### Run provenance

`generate_run_manifest.py` writes a JSON record derived from both the invocation and the
generated artifacts. It includes:

- run ID and UTC timestamp, Git SHA, and dirty-worktree state
- environment, OS, architecture, JDK, CPU count/profile/cpusets, and JVM options
- transport mode, target rate, message count, expected duration, and observed launcher duration
- queue path plus the exact `.hlog` paths, modification times, and `.hgrm` sample counts

`generate_html_report.py --manifest <path>` renders these fields in a **Run Configuration**
card. This makes provenance visible for individual runs.

### Historical Comparison & Streamlit Dashboard

To interactively compare multiple historical runs and overlay their latency distributions across different environments (e.g., `local` vs `docker`), use the built-in Streamlit dashboard:

```bash
# Install dependencies (only needed once)
pip3 install -r reporting/requirements.txt

# Start the dashboard
streamlit run reporting/app.py
```

The dashboard automatically discovers all archived runs in `benchmark-runs/`, calculates statistical deltas for P50/P90/P99/Max, and renders interactive Plotly charts overlaying the HdrHistogram curves. It is the recommended tool for tracking performance regressions and containerization overhead over time.

The report also reconciles gateway ingress (`serv-0`) against terminal completion
(`serv-c`). A green banner confirms that all events were accounted for. A warning means
the pipeline did not fully drain, while a terminal count greater than ingress usually means
stale histogram files were mixed into the input list.

---

## 6. Running the Benchmark

### Orchestrated (recommended)

```bash
# 1. Build all modules
scripts/build.sh

# 2. Run a clean local TCP benchmark. Defaults are 10K msg/s and 1M messages.
./scripts/run_local_benchmark.sh 10000 1000000

# 3. Run Docker with the exact same workload for an environment comparison.
./scripts/run_docker_benchmark.sh 10000 1000000
```

`run_local_benchmark.sh` rejects an existing `logs/services.pid`, starts from the cleanup
performed by `start.sh`, waits until all four event-loop logs report readiness, and delegates
the measured run to `run_benchmark_suite.sh`. Both local and Docker flows stop services in
producer-first order, process exactly eight stage histograms, generate a manifest and report,
and archive all artifacts under `benchmark-runs/<run-id>/<environment>/`.

Set `FX_RUN_ID` to pair runs under one identifier, or set `FX_RUN_OUTPUT_DIR` to choose an
archive directory explicitly. `run_benchmark_suite.sh` remains available for an already-running
pipeline and for `--direct` downstream-only measurements.

### Standalone (manual steps)

```bash
# 3a. Run load generator in TCP mode (default — routes through serv-0)
scripts/run_load_generator.sh /tmp/fx-queues/queue-a 5000000 10000000

# 3a. Run load generator in direct mode (bypasses serv-0)
scripts/run_load_generator.sh /tmp/fx-queues/queue-a 5000000 10000000 --direct

# 3b. Stop services to flush telemetry
scripts/stop.sh

# 3c. Process latencies
./scripts/process_latency.sh /tmp/fx-latency*.hlog

# 3d. Generate HTML report
python3 scripts/generate_html_report.py /tmp/fx-latency*.hlog
```

---

## 7. Hardware Sympathy Tuning

Software architecture alone cannot guarantee sub-millisecond latencies. The OS and hardware must be configured to cooperate:

1. **CPU Isolation (`isolcpus`):** Critical threads (the 4 services and the load generator) must be pinned to isolated cores where the Linux scheduler is forbidden from running other tasks.
2. **Tickless Kernel (`nohz_full`):** Disables the 1000Hz OS timer interrupt on isolated cores, preventing the CPU from being interrupted every 1ms.
3. **C-State Disabling:** Deep sleep states are disabled (`idle=poll`) to prevent the 10-100 microsecond penalty incurred when a CPU wakes up from a low-power state.
4. **CPU Governor:** Set to `performance` to prevent P-state transitions during the benchmark window.

*(See `BENCHMARK_TUNING.md` for the exact kernel boot parameters and JVM flags.)*

---

## 8. Benchmark Run Analysis & Identified Issues

### 8.1 Run Configuration

| Parameter | Value |
|---|---|
| Mode | Direct (LoadGenerator → queue-a, bypassing serv-0) |
| Target injection rate | 5,000,000 events/sec |
| Total messages sent | 2,000,000 |
| Total messages persisted | 1,221,641 |
| Environment | macOS / Docker (advisory CPU affinity only) |

### 8.2 Observed Metrics

All values below are in milliseconds. Adaptive units (µs / ms / s) are used in the HTML report.

| File | Samples | P50 | P90 | P99 | P99.99 | Max |
|---|---|---|---|---|---|---|
| `fx-latency-serv-0` | 2,000,000 | 0.001 ms | 0.002 ms | 0.006 ms | 0.090 ms | 26 ms |
| `fx-latency-queue-a` | 1,221,641 | **733 ms** | **1,050 ms** | **1,125 ms** | **1,128 ms** | **1,128 ms** |
| `fx-latency-serv-a` | 1,221,641 | < 0.001 ms | < 0.001 ms | < 0.001 ms | 0.001 ms | 0.137 ms |
| `fx-latency-queue-b` | 1,221,641 | 7.6 ms | 20.4 ms | 57.9 ms | 93.1 ms | 93.1 ms |
| `fx-latency-serv-b` | 1,221,641 | < 0.001 ms | < 0.001 ms | < 0.001 ms | 0.001 ms | 10.7 ms |
| `fx-latency-queue-c` | 1,221,641 | **25.7 ms** | **1,536 ms** | **4,995 ms** | **5,302 ms** | **5,302 ms** |
| `fx-latency-serv-c` | 1,221,641 | < 0.001 ms | < 0.001 ms | 0.002 ms | 4.887 ms | **586 ms** |

### 8.3 Math Validation

Values are stored in HdrHistogram `.hgrm` files as **nanoseconds** (the unit recorded by `System.nanoTime()` differences). The previous HTML report divided by `1,000,000` to display milliseconds — this arithmetic is correct, but millisecond display masks sub-microsecond values (e.g. serv-a P50 appeared as `0.000 ms` rather than `< 1 µs`).

The updated `generate_html_report.py` stores raw nanoseconds internally and applies adaptive unit selection at render time:

```
value_ns <   1,000,000  (<  1,000 µs) → display in µs
value_ns < 1,000,000,000  (< 1,000 ms) → display in ms
value_ns ≥ 1,000,000,000              → display in s
```

### 8.4 Root-Cause Analysis

#### Issue 1 — Queue-A: Producer-Consumer Rate Mismatch (P50 = 733 ms)

**Pattern:** Flat distribution — P50 (733 ms) through Max (1,128 ms) spans only ~400 ms. This is not a rare spike; it is a steady-state queue backlog.

**Mechanism:**

1. serv-0 stamps `ingressNanoTime` (T0) and writes to queue-a in ~1 µs.
2. The load generator paced at 5M events/sec → ~5,000 events every millisecond.
3. The downstream pipeline sustained only ~1–2M events/sec on this hardware.
4. A growing backlog accumulated in queue-a. Each event's measured `T1 − T0` includes all the time other events ahead of it took to be processed.
5. At a 733 ms P50, approximately 733K events were ahead of any median-ranked event.

**The ~778K missing events** (2,000,000 sent − 1,221,641 persisted) were still in-flight or written to queue-a when the benchmark was terminated — confirming the backlog was never fully drained.

**Contributing factors:**

| Factor | Detail |
|---|---|
| macOS advisory affinity | `AffinityLock.acquireLock()` calls `thread_policy_set(THREAD_AFFINITY_POLICY)` — a *hint* only. The macOS scheduler can preempt serv-a for tens to hundreds of milliseconds at any time. |
| Chronicle Queue page faults | When the 64 MB store file rolls to a new segment the first access triggers a kernel page fault. On HDD: up to 500 ms. On SSD: 10–50 ms. Pre-warming eliminates this. |
| JIT warm-up safepoints | C2 compilation of hot methods triggers JVM-global safepoints of 200–500 ms in the first few seconds of a benchmark, contaminating early histogram buckets. |

**Recommended fixes:**

1. Calibrate the load generator rate to ≤ 80% of the measured sustainable pipeline throughput (measure first, then drive).
2. Run benchmarks on Linux with `isolcpus` + `nohz_full` + `rcu_nocbs` (see `BENCHMARK_TUNING.md`) for strict CPU isolation.
3. Pre-warm queue-a with a dummy write at startup to pre-fault the first mmap segment.

#### Issue 2 — Queue-C: Ring Buffer Saturation (P50 = 25 ms, P99 = 5 s)

**Pattern:** Bimodal distribution — fast 50% of the time (P50 = 25 ms), catastrophically slow 1% of the time (P99 = 5 s).

**Mechanism:**

```java
// BatchPersistenceEngine.accumulate() — hot-path spin-wait
while (w - r >= RING_SIZE) {   // blocks serv-c event loop when ring is full
    Thread.onSpinWait();
    r = readPointer;
}
```

1. `BatchPersistenceEngine.accumulate()` writes to a pre-allocated async ring buffer.
2. A background `db-writer` thread drains the ring via JDBC `executeBatch() + commit()`.
3. At 1M events/sec a 65,536-slot ring absorbed only ~65 ms of burst.
4. Any H2 commit taking longer than ~65 ms filled the ring and stalled `accumulate()`.
5. While stalled, serv-c could not read from queue-c → queue-c depth grew to seconds.

**Direct evidence:**

| Observation | Interpretation |
|---|---|
| serv-c P99.99 = 4.887 ms | `handle()` spent up to 4.9 ms spinning inside `accumulate()` |
| serv-c Max = 586 ms | One H2 commit stall kept serv-c blocked for 586 ms |
| queue-c Max = 5,302 ms | During those 586 ms, ~586K events piled up in queue-c |

#### Issue 3 — serv-C Tail (Max = 586 ms): Same Root as Issue 2

The 586 ms serv-c Max is the measured duration of the spin-wait inside `accumulate()`. This is not a separate issue — it is the direct in-process observation of the ring-buffer stall that causes the queue-c cascade.

### 8.5 Fixes Applied

| Component | Setting | Before | After | Impact |
|---|---|---|---|---|
| `BatchPersistenceEngine` | `RING_SIZE` | 65,536 | **524,288** | ~524 ms burst absorption @ 1M evt/sec; 8× more headroom |
| `BatchPersistenceEngine` | `MAX_BATCH` | 4,096 | **32,768** | Fewer `commit()` calls per second; lower amortised H2 overhead |
| `PersistenceEventLoop` | Default JDBC URL | H2 1.x params attempted | **Reverted** | `LOG=0`/`UNDO_LOG=0` unsupported in H2 2.x MVStore; broke JDBC connection at startup |
| `generate_html_report.py` | Unit display | Fixed `ms` column | Adaptive µs / ms / s with severity heat-map | Correct precision for sub-µs values; embedded RCA + fix plan in HTML |

> **H2 2.x note:** `LOG=0` and `UNDO_LOG=0` were H2 1.x-only URL parameters. H2 2.3.x (MVStore engine) does not support them — supplying them causes serv-c's JDBC connection to fail at startup, which prevents all per-stage telemetry from recording (all `.hlog` files show 249 bytes / header-only). The correct URL for H2 2.x is `jdbc:h2:mem:fxdb;DB_CLOSE_DELAY=-1;MODE=MySQL`.
