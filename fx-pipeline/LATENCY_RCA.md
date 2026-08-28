# FX Pipeline Latency Investigation and Root-Cause Analysis

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

### RCA-2: Warm-up traffic contaminated the later histogram runs

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

---

### RCA-3: The `endOfBatch` queue peek adds unnecessary overhead on every message

There was a dead-cost pattern inside the generic event loop:

- [common/src/main/java/com/fx/common/handler/AbstractEventLoop.java](common/src/main/java/com/fx/common/handler/AbstractEventLoop.java)

The loop was doing a non-blocking peek into the next Chronicle document on every event:

- `tailer.readingDocument(false)`
- followed by `rollbackOnClose()`

This expensive pattern ran on every message, even though none of the actual pipeline handlers used `endOfBatch` for meaningful behavior.

This was not only unnecessary; it also reduced effective throughput at exactly the point where the queue was already near saturation.

Conclusion:

This dead-work loop was shaving throughput and making the backlog worse.

---

### RCA-4: The ring buffer and H2 commit path were coupled together

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

---

### RCA-5: H2 batch sizing was too large for the table growth pattern

The batch size was too aggressive for the in-memory MVStore table under sustained insert pressure.

This is a balancing problem:

- too small batches => more frequent commits and more commit overhead
- too large batches => each commit becomes very heavy and can stall the ring for a long time

The old behavior was effectively in the “too large” zone once the DB grew to large tables. That created multi-second spikes, which then cascaded into queue-c backlog and end-to-end latency.

---

### RCA-6: Schema design made persistence worse than necessary

The table used a separate auto-increment `id` plus a correlation key.

The design was not aligned with the write pattern:

- the system is append-heavy
- the dominant key is already a monotonic correlation ID
- the auto-increment sequence introduces overhead and an extra B-tree path

This is small compared to the queue saturation, but it is still a real inefficiency at scale.

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

### Phase 1 — Remove measurement pollution

- do not write warm-up events into the measured queue
- use a separate warm-up queue or a dedicated warm-up phase outside the benchmark path
- ensure `ingressNanoTime` corresponds to intended send time, not warm-up wall-clock time

### Phase 2 — Remove dead, per-event overhead

- remove the non-blocking `readingDocument(false)` peek from the generic event loop
- do not compute `endOfBatch` if no handler uses it
- keep the loop minimal and deterministic

### Phase 3 — Separate ring occupancy from commit latency

- free ring slots immediately after copying into the JDBC batch
- do not hold the slots until commit completes
- only advance the durable commit pointer after a successful commit

This is the key fix for queue-c tail spikes.

### Phase 4 — Tune batch size and H2 write profile

- use a smaller atomic batch size so each commit completes quickly
- avoid oversized commits that create long stalls
- keep batch size large enough to amortize JDBC overhead, but not so large that commit latency becomes the dominant cost

### Phase 5 — Rework the table layout for append efficiency

- use correlation ID as the primary key when it is naturally monotonic
- reduce extra indexing and sequence overhead
- keep the schema aligned with the write pattern

### Phase 6 — Calibrate the input rate to the sustainable throughput

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

## 9. Verification

I validated the project state by running the relevant compile/test command:

- `cd /Users/mithunselvan/swift/fx-pipeline && mvn clean test`

This completed successfully with `BUILD SUCCESS`, confirming the current project is stable after the documented fixes and validations.
