# FX Pipeline Latency Investigation and Root-Cause Analysis

> **Status (2026-08-31):** RCA-2 through RCA-6 below have been resolved in code (verified against the
> current implementation — see the ✅ markers on each). RCA-1 (producer/consumer rate mismatch) remains
> an operational calibration concern rather than a code defect. A separate, previously-unrelated issue —
> a **sample-count discrepancy** between `serv-0` and every downstream stage — was investigated and fixed
> after this report was originally written; see [Section 9](#9-update--sample-count-discrepancy-between-serv-0-and-downstream-stages) for that RCA and fix.
> Benchmark runners now emit and embed a run manifest and create run-scoped artifact archives; environment
> comparisons must use matching manifest workload and mode fields.

## Executive Summary

The queue latencies are not inherently “bad” because the services are slow. The dominant issue is that the system is being fed faster than it can drain, and the measurement logic is then amplifying that backlog into very large queue wait times.

The key pattern is consistent across the traces:

- `serv-a`, `serv-b`, and `serv-c` each spend microseconds to low-millisecond amounts of time doing actual work.
- The large latency is overwhelmingly in the queue wait segments: `queue-a`, `queue-b`, and especially `queue-c`.
- That means the bottleneck is primarily upstream saturation and the persistence-backpressure chain, not the business logic itself.
- A secondary problem is measurement contamination from warm-up traffic and a non-trivial amount of dead overhead in the queue consumer loop.

This report documents the observed behavior, the underlying causes, the math behind the numbers, and a concrete fix plan aligned with zero-GC and mechanical sympathy principles.

---

## 1. What the metrics are actually showing

The latency files are stage-specific:

- `fx-latency-serv-0.hlog`: gateway decode and queue-a write
- `fx-latency-queue-a.hlog`: time spent waiting in queue-a before serv-a consumes it
- `fx-latency-serv-a.hlog`: serv-a processing time
- `fx-latency-queue-b.hlog`: queue-b wait time
- `fx-latency-serv-b.hlog`: serv-b processing time
- `fx-latency-queue-c.hlog`: queue-c wait time
- `fx-latency-serv-c.hlog`: serv-c ring-buffer + JDBC batch processing cost
- `fx-latency.hlog`: end-to-end latency

The service-side numbers are the calming signal. They are mostly sub-millisecond or microsecond scale:

- serv-a: effectively nanoseconds to microseconds
- serv-b: nanoseconds to low microseconds
- serv-c: usually low microseconds, but occasionally spikes as the ring buffer stalls

The large tail latency is in the queue segments, and especially in queue-a and queue-c.

---

## 2. The math behind the numbers

The queue wait times are not random; they follow a clear pattern of backlog growth.

For events sent at target rate $T$ and drained at effective sustainable rate $R_{eff}$, backlog growth is approximately linear:

$$
\text{wait}_i \approx \frac{i}{R_{eff}} - \frac{i}{T}
$$

where $i$ is event position in the queue.

This is the key reason why queue wait times rise from tens of milliseconds to multiple seconds as the backlog deepens.

### Example: 500k test on queue-a

Given the observed P50 in `fx-latency-queue-a.hlog` for the 500k run is about 1.276s, the median event sits behind roughly 250,000 earlier events.

If the producer is targeting 5,000,000 events/sec but the actual pipeline can only drain roughly 175,000–200,000 events/sec on the current host, then the backlog is continuously increasing and every event waits a long time before service entry.

This matches the observed behavior:

- service code is fast
- queue waits are huge
- the aggregated end-to-end latency therefore balloons

This is not a service bug; it is a pipeline saturation bug.

---

## 3. Observed evidence in the current report

### 3.1 500,000 trade run

The queue histograms show severe backlog:

- `fx-latency-queue-a.hlog`: P50 ~1.276s, P99 ~1.800s, Max ~1.813s
- `fx-latency-queue-b.hlog`: P50 ~165ms, P99 ~342ms
- `fx-latency-queue-c.hlog`: P50 ~5.9ms, P99 ~145ms, Max ~147ms

End-to-end:

- `fx-latency.hlog`: P50 ~1.478s, P99 ~2.058s, Max ~2.063s

This means the pipeline is operating under sustained backpressure and the backlog signal is not a rare spike.

### 3.2 2,000,000 trade run

The same pattern persists, but some of the numbers are misleading because they are contaminated by warm-up traffic:

- `fx-latency-queue-a.hlog`: P50 ~1.014s
- `fx-latency-queue-b.hlog`: P50 ~50ms
- `fx-latency-queue-c.hlog`: P50 ~3µs but P99 ~3.4s
- `fx-latency.hlog`: P50 ~1.056s, P99 ~5.184s, Max ~7.852s

This shows the pipeline is not recovering; it is being pushed further and further into a backlog state. The queue-c tail spikes are highly consistent with a ring-buffer stall at serv-c.

---

## 4. Root-cause analysis

### RCA-1: Producer-consumer rate mismatch is the primary cause

This is the dominant root cause.

The service logic is fast, but the producer is pushing more events than the pipeline can consume sustainably.

Evidence in code:

- [test/src/main/java/com/fx/test/LoadGenerator.java](test/src/main/java/com/fx/test/LoadGenerator.java)
- [serv-a/src/main/java/com/fx/risk/RiskValidationEventLoop.java](serv-a/src/main/java/com/fx/risk/RiskValidationEventLoop.java)
- [serv-b/src/main/java/com/fx/pricing/PricingEventLoop.java](serv-b/src/main/java/com/fx/pricing/PricingEventLoop.java)
- [serv-c/src/main/java/com/fx/persistence/PersistenceEventLoop.java](serv-c/src/main/java/com/fx/persistence/PersistenceEventLoop.java)

What the code shows:

- `serv-a` validates a single event in a fraction of a microsecond.
- `serv-b` applies spread logic in similarly tiny time.
- `serv-c` has a very fast path when the ring is healthy.

Yet the queue waits remain large. That indicates queue accumulation, not business logic cost.

Conclusion:

The pipeline is saturated by injection rate, and the queue backlog is performing the same function as a pressure accumulator.

---

### RCA-2: Warm-up traffic contaminated the later histogram runs — ✅ RESOLVED

The load generator was writing a large warm-up batch directly into the measured queue before the actual benchmark started.

Evidence:

- [test/src/main/java/com/fx/test/LoadGenerator.java](test/src/main/java/com/fx/test/LoadGenerator.java)

The earlier code was effectively doing this:

- generate 1,000,000 warm-up events
- write them directly to queue-a
- then start the measured run

That means a large chunk of historical backlog remained inside the queue before the measurement window started. Therefore the measured percentiles were being diluted by warm-up samples that were not representative of steady-state benchmark behavior.

This explains why a 2M run could appear better than a 500k run in some metrics even though the 2M run was actually under heavier cumulative pressure.

This is a measurement contamination bug, not a real improvement in throughput.

**Fix applied:** `LoadGenerator` now writes warm-up events to a separate, ephemeral warm-up
Chronicle Queue (not `queue-a`), and skips the warm-up phase entirely in TCP mode to avoid
flooding the gateway unpaced. The measured queue is never touched by warm-up traffic.

---

### RCA-3: The `endOfBatch` queue peek adds unnecessary overhead on every message — ✅ RESOLVED

There was a dead-cost pattern inside the generic event loop:

- [common/src/main/java/com/fx/common/handler/AbstractEventLoop.java](common/src/main/java/com/fx/common/handler/AbstractEventLoop.java)

The loop was doing a non-blocking peek into the next Chronicle document on every event:

- `tailer.readingDocument(false)`
- followed by `rollbackOnClose()`

This expensive pattern ran on every message, even though none of the actual pipeline handlers used `endOfBatch` for meaningful behavior.

This was not only unnecessary; it also reduced effective throughput at exactly the point where the queue was already near saturation.

Conclusion:

This dead-work loop was shaving throughput and making the backlog worse.

**Fix applied:** `AbstractEventLoop` no longer peeks the next document to compute `endOfBatch`.
It now passes `endOfBatch=true` unconditionally, since no handler used the false case.

---

### RCA-4: The ring buffer and H2 commit path were coupled together — ✅ RESOLVED

This was the main persistence-side bottleneck.

Evidence:

- [serv-c/src/main/java/com/fx/persistence/BatchPersistenceEngine.java](serv-c/src/main/java/com/fx/persistence/BatchPersistenceEngine.java)

The bad pattern was:

1. producer writes into ring buffer
2. db-writer fills a prepared batch
3. `executeBatch()` runs
4. `connection.commit()` runs
5. `readPointer` advances only after that commit completes

That means the ring was logically occupied for the full length of the commit, not just while the batch was being copied.

If commit latency grows to hundreds of milliseconds or several seconds, the ring fills and `accumulate()` stalls. Once stalled, the serv-c event loop stops draining queue-c and the queue-c wait times explode.

This matches the observed queue-c tail:

- P99 and Max spikes into the seconds
- service time remains minor
- queue-c delay is the real issue

**Fix applied:** `BatchPersistenceEngine` now advances `readPointer` immediately after the batch is
copied out of the ring, not after `connection.commit()` returns. The ring buffer was also enlarged
from 65,536 to 524,288 slots so it absorbs load-phase bursts independently of commit latency.

---

### RCA-5: H2 batch sizing was too large for the table growth pattern — ✅ RESOLVED

The batch size was too aggressive for the in-memory MVStore table under sustained insert pressure.

This is a balancing problem:

- too small batches => more frequent commits and more commit overhead
- too large batches => each commit becomes very heavy and can stall the ring for a long time

The old behavior was effectively in the “too large” zone once the DB grew to large tables. That created multi-second spikes, which then cascaded into queue-c backlog and end-to-end latency.

**Fix applied:** `MAX_BATCH` is now `8,192` (tuned down from an oversized value), combined with the
RCA-4 fix decoupling ring occupancy from commit duration — the batch size no longer determines how
long the ring is blocked.

---

### RCA-6: Schema design made persistence worse than necessary — ✅ RESOLVED

The table used a separate auto-increment `id` plus a correlation key.

The design was not aligned with the write pattern:

- the system is append-heavy
- the dominant key is already a monotonic correlation ID
- the auto-increment sequence introduces overhead and an extra B-tree path

This is small compared to the queue saturation, but it is still a real inefficiency at scale.

**Fix applied:** `fx_trades` now uses `correlation_id` directly as the `PRIMARY KEY` — no separate
auto-increment `id` column or extra B-tree path.

---

## 5. Why the earlier “bad” numbers were misleading

The reason the raw numbers look unstable is because they are measuring a queueing system under pressure, not a simple service call graph.

Three effects combine:

1. The producer is faster than the pipeline can drain.
2. The queue backlog accumulates linearly over time.
3. Measurement and warm-up design add extra samples to the same queue so the histogram is not isolated to the steady-state benchmark window.

This is why the tail numbers are not a clean service latency profile. They are a queue pressure profile.

---

## 6. Recommended fix plan

The fix must focus on queue discipline and persistence throughput first.

### Phase 1 — Remove measurement pollution — ✅ Implemented

- do not write warm-up events into the measured queue
- use a separate warm-up queue or a dedicated warm-up phase outside the benchmark path
- ensure `ingressNanoTime` corresponds to intended send time, not warm-up wall-clock time

### Phase 2 — Remove dead, per-event overhead — ✅ Implemented

- remove the non-blocking `readingDocument(false)` peek from the generic event loop
- do not compute `endOfBatch` if no handler uses it
- keep the loop minimal and deterministic

### Phase 3 — Separate ring occupancy from commit latency — ✅ Implemented

- free ring slots immediately after copying into the JDBC batch
- do not hold the slots until commit completes
- only advance the durable commit pointer after a successful commit

This is the key fix for queue-c tail spikes.

### Phase 4 — Tune batch size and H2 write profile — ✅ Implemented

- use a smaller atomic batch size so each commit completes quickly
- avoid oversized commits that create long stalls
- keep batch size large enough to amortize JDBC overhead, but not so large that commit latency becomes the dominant cost

### Phase 5 — Rework the table layout for append efficiency — ✅ Implemented

- use correlation ID as the primary key when it is naturally monotonic
- reduce extra indexing and sequence overhead
- keep the schema aligned with the write pattern

### Phase 6 — Calibrate the input rate to the sustainable throughput — ⚠️ Ongoing (operational, not a code fix)

- benchmark the pipeline at a lower rate first
- find the stable throughput limit for the host
- set load injection to a value below that limit with a safety margin

This is the direct way to eliminate queue-a backlog.

---

## 7. Best-practice engineering principles for the final design

The final design should preserve the following constraints:

- zero-GC hot path
- no per-event object allocation
- CPU affinity / thread isolation where host OS allows it
- single-writer ownership for each queue
- minimal queue peek overhead
- minimal DB commit time variance
- explicit calibration of production rate against hardware limits

This is a low-latency architecture, not a general-purpose web service. The optimization goal is stable tail latency, not maximum event rate at any cost.

---

## 8. Final conclusion

The system does not have a mysterious latency bug. The evidence is coherent:

- service logic is fast
- queue wait times dominate the observed latency
- queue-a is saturated by producer rate
- queue-c is stalled by ring-buffer / H2 commit interaction
- the benchmark was contaminated by warm-up events
- there is unnecessary overhead in the generic queue loop

The root cause is therefore a combination of:

1. feed rate exceeding sustainable drain rate
2. persistence backpressure coupling to ring fill
3. measurement contamination and unnecessary queue peeking

This is why the numbers vary so much and why the queue files are the real bottleneck signal.

---

## 9. Update — Sample-Count Discrepancy Between `serv-0` and Downstream Stages

### 9.1 Observed issue

A later benchmark run (`./scripts/run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 2000000`, TCP
mode) showed `fx-latency-serv-0.hlog` with a total sample count of **2,000,000**, while every other
histogram — `queue-a`, `queue-b`, `queue-c`, `serv-a`, `serv-b`, `serv-c`, and end-to-end
(`fx-latency.hlog`) — reported an *identical* total of **1,620,030**. This was not data corruption: no
FIX messages were lost, duplicated, or misdecoded.

### 9.2 Root causes

**RC-1 — Centralized telemetry recording.** `queue-a`, `serv-a`, `queue-b`, `serv-b`, `queue-c`,
`serv-c`, and e2e latencies were all recorded from a single call site inside
`PersistenceEventLoop.handle()` (serv-c). Every one of those histograms only received a sample once an
event reached the terminal stage — so all six were identical by construction, and none of them
reflected what each individual stage had actually processed.

**RC-2 — Non-draining shutdown.** `run_benchmark_suite.sh` used a fixed 5-second sleep before calling
`stop.sh`, which sent `SIGTERM` to all services. `AbstractEventLoop.stop()` only flipped a flag checked
at the top of the loop — it did not drain whatever backlog was still sitting in the input queue. Any
event not yet fully processed through `serv-c` when the timeout elapsed was abandoned mid-pipeline:
still on disk in `queue-a`/`queue-b`/`queue-c`, but never consumed, and therefore contributing zero
samples to any histogram.

`2,000,000 − 1,620,030 = 379,970` events (~19%) were still in flight when the pipeline was torn down —
consistent with the sustained backpressure documented in RCA-1 above.

### 9.3 Fixes implemented

**Phase 1 — Per-stage independent telemetry.**
[RiskValidationEventLoop](serv-a/src/main/java/com/fx/risk/RiskValidationEventLoop.java) and
[PricingEventLoop](serv-b/src/main/java/com/fx/pricing/PricingEventLoop.java) now record their own
queue-wait and processing-duration recorders at the end of their own `handle()` methods, instead of
relying on `serv-c` to record on their behalf.
[PersistenceEventLoop](serv-c/src/main/java/com/fx/persistence/PersistenceEventLoop.java) now only
records `queue-c`, `serv-c`, and end-to-end — the segment it can actually measure. Each `.hlog`'s
sample count now reflects events actually processed by that stage, independent of later stages.

**Phase 2 — Drain-then-stop shutdown.**
[AbstractEventLoop](common/src/main/java/com/fx/common/handler/AbstractEventLoop.java) now treats
`stop()` as "accept no new work", not "abandon in-flight work": after the main loop exits, it drains
any backlog already sitting in the input queue (bounded by `-Dfx.eventloop.drainTimeoutMillis`,
default 30s) and logs how many events were drained, or a warning if the timeout was hit.
[stop.sh](scripts/stop.sh) was rewritten to stop services one at a time, in strict producer-first
order (`serv-0 → serv-a → serv-b → serv-c → telemetry`), fully awaiting each exit before signalling
the next — required so a downstream stage never decides it has "finished draining" while its upstream
is still mid-flight. [start.sh](scripts/start.sh) now labels each PID so `stop.sh` can target them by
name. The fixed 5-second guess in `run_benchmark_suite.sh` was replaced with a short 1-second settle
buffer, since draining correctness now comes from the above rather than a timing guess.

**Phase 3 — Automatic reconciliation in the report.**
[generate_html_report.py](scripts/generate_html_report.py) now compares `serv-0`'s ingress count
against the terminal stage's completed count on every run and renders a callout directly in the HTML
report: a warning with the exact count/percentage unaccounted for if a gap remains, or a confirmation
that all events were accounted for. This replaced a set of hardcoded, stale "Issue/Fix" sections in the
same script that referenced numbers and a ring-buffer size from an old, unrelated run and no longer
matched the current (already-fixed) codebase.

### 9.4 Verification

All six per-stage/constructor-arity changes were validated with `mvn clean test-compile` and
`mvn test` (`BUILD SUCCESS`, all suites passing, including `PersistenceEventLoopTest` and
`FullPipelineIntegrationTest`). The reconciliation banner was smoke-tested against synthetic `.hgrm`
fixtures reproducing the 2,000,000 vs 1,620,030 case, confirming the warning renders with the correct
count and percentage, and that the confirmation banner renders when counts match.

---

## 10. Verification (original report)

I validated the project state by running the relevant compile/test command:

- `cd /Users/mithunselvan/swift/fx-pipeline && mvn clean test`

This completed successfully with `BUILD SUCCESS`, confirming the current project is stable after the documented fixes and validations.

---

## 11. Update — Docker Desktop CPU Self-Contention (2026-08-30)

### 11.1 Matched low-rate observation

The original Docker and macOS reports used different event counts and could not establish an
environmental root cause. The comparison was repeated through the same TCP pipeline at **10,000
messages/sec** and **10,000 events**. At this rate, producer saturation is not a credible explanation.

Docker Desktop on Apple Silicon reported an end-to-end P50 of **21.725 ms** and P99 of
**197.394 ms**. Native macOS reported a P50 of **19.295 µs** and P99 of **43.614 ms**. In Docker,
the service medians remained between **0.042 µs** and **2.583 µs**, while the three queue medians
were **5.943 ms**, **4.903 ms**, and **12.812 ms**. Queue residence therefore explains the end-to-end
median; the business handlers do not.

Increasing Docker Desktop to 12 CPUs and 16 GB did not materially change the result. The issue was
not a simple shortage of VM-wide CPU or memory capacity.

### 11.2 Controlled experiments

The one-second, 10,000-event run overemphasised startup effects, so the workload was extended to
**100,000 events at 10,000 messages/sec**.

| Configuration | E2E P50 | E2E P90 | E2E P99 | Max | Samples |
|---|---:|---:|---:|---:|---:|
| One vCPU per JVM (existing map) | 5.243 ms | 13.148 ms | 208.273 ms | 239.600 ms | 100,000 |
| Paired cpusets, run 1 | 55.487 µs | 3.248 ms | 47.907 ms | 107.086 ms | 100,000 |
| Paired cpusets, run 2 | 162.815 µs | 4.207 ms | 113.574 ms | 131.465 ms | 100,000 |
| Paired cpusets, run 3 | 107.327 µs | 3.815 ms | 82.444 ms | 114.229 ms | 100,000 |
| Paired cpusets, run 4 | 127.231 µs | 3.807 ms | 83.100 ms | 102.171 ms | 100,000 |
| Paired cpusets, run 5 | 361.983 µs | 4.207 ms | 87.753 ms | 123.339 ms | 100,000 |

Only the cpusets changed. Tracing, HdrHistogram telemetry, TCP mode, rate, event count, Java code,
and affinity setting remained unchanged. All eight histograms contained exactly 100,000 samples in
every sustained run.

An additional 10,000-event run omitted the standalone JSON trace stitcher. End-to-end P50 worsened
from **21.725 ms** to **57.049 ms**, so tracing was not identified as the cause and remains enabled by
default.

### 11.3 Root cause

The evidence supports **CPU self-contention created by the container CPU map**.

Each service JVM was restricted to one vCPU while its LMAX-style event loop continuously used
`BusySpinWaitStrategy`. The same vCPU also had to execute that JVM's JIT, ZGC, telemetry flusher,
asynchronous logger, and other support threads. `serv-c` additionally runs the independent database
writer. A cpuset restricts the entire container; it does not reserve that CPU exclusively for the
event-loop thread. Docker Desktop also runs through a Linux VM and cannot provide native Linux
`isolcpus` guarantees.

Busy-spin is correct when the event processor owns a dedicated physical core. Restricting the whole
JVM to that same single CPU prevents its housekeeping threads from making progress without
preempting the event loop. The resulting pauses appear as queue latency while the allocation-free
business handlers remain fast.

The following explanations were rejected or reduced in priority for this run:

- **Producer saturation:** rejected at 10,000 messages/sec.
- **Slow service logic:** rejected by sub-microsecond service medians.
- **Standalone JSON tracing:** rejected by the tracing-off A/B result.
- **Insufficient Docker Desktop resources:** rejected by the 12-CPU/16-GB rerun.
- **GC mode mismatch:** Docker lacked generational ZGC, but this can affect tails rather than explain
	the consistent queue-median improvement caused by cpusets.

### 11.4 Remediation implemented

No event-processing logic, wait strategy, timestamp, Chronicle Queue setting, telemetry
implementation, persistence batch, or ring-buffer protocol was changed.

- `run_docker_benchmark.sh` now selects a CPU profile. `auto` uses paired cpusets on macOS Docker
	Desktop (`0,5`, `1,6`, `2,7`, `3,8`, and `4,9`) and retains the original isolated-core map on
	Linux. `FX_CPU_PROFILE=desktop|isolated` and the individual `FX_*_CPUSET` variables remain explicit
	overrides.
- `FX_TRACE_ENABLED=false` provides a controlled tracing experiment, while tracing remains enabled
	by default.
- The Compose benchmark service no longer embeds a conflicting 500,000-message/sec, 5,000,000-event
	workload. `scripts/run_docker_benchmark.sh` is the canonical entry point.
- Docker now uses `-XX:+UseZGC -XX:+ZGenerational`, matching native Java 21 startup. Runtime flag
	inspection confirmed both options are enabled.

After rebuilding with the aligned JVM flags, the automatic Desktop profile completed another
100,000-event run with all samples reconciled: P50 **86.143 µs**, P90 **4.592 ms**, P99
**140.509 ms**, and Max **156.893 ms**. This remains variable at the tail, as expected on Docker
Desktop, but is materially better than the one-vCPU sustained baseline.

### 11.5 Operational guidance

Use the longer calibration for meaningful comparisons:

```bash
FX_SKIP_BUILD=true ./scripts/run_docker_benchmark.sh 10000 100000
```

The measured Desktop profile requires at least 10 CPUs in Docker Desktop because it uses CPU indices
0 through 9. The benchmark fails early with an actionable message when that allocation is unavailable.

Use `FX_CPU_PROFILE=isolated` only on a Linux host whose event CPUs are genuinely isolated. For
production latency acceptance, validate on native Linux with `isolcpus`, `nohz_full`, `rcu_nocbs`,
the performance governor, controlled C-states, and separate housekeeping CPUs. Docker Desktop is
suitable for functional checks and relative experiments, not deterministic tail-latency guarantees.

### 11.6 Run provenance and the current local/Docker comparison

The benchmark tooling now generates `run_manifest.json` for every orchestrated run and embeds it in
the HTML report. The manifest records the workload, transport, runtime, CPU/JVM configuration, Git
state, exact histogram files and timestamps, and per-stage sample counts. Local and Docker artifacts
are archived under `benchmark-runs/<run-id>/<environment>/`.

This was added after reviewing a Docker report with 100,000 samples at 150,000 messages/sec against a
native report with 1,000,000 samples at a higher transaction rate. Those reports are useful forensic
evidence but are **not a controlled environment comparison** because their workloads and measured
durations differ. They nevertheless show where Docker lost time: Docker queue-a P50 was 342.098 ms and
end-to-end P50 was also 342.098 ms, while native queue-a P50 was 5.543 microseconds and end-to-end P50
was 19.887 microseconds. The matching Docker queue-a/end-to-end median identifies backlog before
`serv-a`; sub-microsecond `serv-a` processing rules out risk logic as the cause.

Use a shared run ID and identical parameters for the controlled baseline:

```bash
FX_RUN_ID=baseline-10k ./scripts/run_local_benchmark.sh 10000 1000000
FX_RUN_ID=baseline-10k ./scripts/run_docker_benchmark.sh 10000 1000000
```

Only compare runs when target rate, message count, TCP/direct mode, tracing setting, source revision,
stage set, and reconciled sample counts agree. Prefer repeated 60–100 second runs and alternate the
environment order. The report surfaces compatibility evidence; automatic two-manifest comparison is
not implemented yet.
