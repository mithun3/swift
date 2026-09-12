# FX Pipeline Deployment and Performance Tuning Guide

To achieve accurate, repeatable sub-millisecond tail latencies on the JVM, you must configure the underlying OS and hardware to remove sources of jitter (context switches, CPU frequency scaling, page faults). This guide details the tuning required for native (bare-metal) environments as well as Docker.

## 1. Cloud Deployment & Bare-Metal Constraints

**Infrastructure Choice: True Bare Metal**
We mandate the use of true bare-metal servers (e.g., Vultr Bare Metal, Hetzner Dedicated) rather than standard virtualized cloud instances.
- **Hypervisor Jitter:** Virtual machines share physical CPU cores with other tenants or the hypervisor itself. A 1-millisecond hypervisor pause ruins the 99.99th percentile metric.
- **Cost-Efficiency:** Bare-metal providers (hourly or monthly dedications) provide the exact same mechanical isolation as expensive cloud instances at a fraction of the cost.

**Operating System: Rocky Linux 9**
The deployment standardizes on **Rocky Linux 9** (a 1:1 RHEL clone).
- **Enterprise Tooling (`tuned`):** Achieving sub-millisecond determinism requires strict CPU isolation. Rocky Linux includes the enterprise `tuned` daemon natively. By enabling the `cpu-partitioning` profile, Rocky Linux automatically ensures that all OS noise, kernel threads, and hardware interrupts are kept strictly away from the isolated trading cores.

**Hardware Sympathy Summary:**
1. **Hyperthreading (SMT) Disabled:** Prevents background tasks from polluting the L1/L2 caches of the trading cores.
2. **C-States Disabled:** Prevents the CPU from entering deep sleep modes, avoiding the microsecond wake-up penalty.
3. **CPU Isolation:** Trading cores are isolated from the OS scheduler, leaving Core 0 to handle SSH, Docker daemon, and system interrupts.

## 2. Hardware & OS Tuning Checklist (Native Linux)

### Boot Parameters (GRUB / kernel)
Edit `/etc/default/grub` and update `GRUB_CMDLINE_LINUX_DEFAULT`:
- `isolcpus=2-11`: Isolate cores 2 through 11 from the Linux scheduler.
- `nohz_full=2-11`: Enable tickless kernel for these cores (stops the 1000Hz timer interrupt).
- `rcu_nocbs=2-11`: Move RCU callbacks away from isolated cores.
- `intel_idle.max_cstate=0 processor.max_cstate=0 idle=poll`: Disable deep sleep C-states.

### CPU Governor
Force the CPU to run at maximum frequency to avoid P-state transition jitter:
```bash
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
```

## 3. Docker Low-Latency Tuning

If running under Docker, the Linux host must still be tuned as above because Docker relies on the host's kernel for CPU scheduling.

### Directory Prerequisites
Ensure the host directories for persistence and telemetry exist with proper permissions:
```bash
mkdir -p ./fx-data ./fx-telemetry
chmod 777 ./fx-data ./fx-telemetry
```

### CPU Isolation
Isolate cores 0, 1, 2, 3, and 4 in `/etc/default/grub`:
```text
GRUB_CMDLINE_LINUX_DEFAULT="quiet splash isolcpus=0,1,2,3,4 intel_idle.max_cstate=0 processor.max_cstate=0"
```
Disable hyperthreading at runtime if not disabled in BIOS:
```bash
echo off > /sys/devices/system/cpu/smt/control
```

The `docker-compose.yml` is already configured to request explicit CPU affinities via `cpuset`. JVM-level OpenHFT affinity is automatically disabled inside the containers to avoid `EINVAL` exceptions.

## 4. JVM Flags

The flags actually applied differ by script/module:
- **`scripts/start.sh`** (launches native pipeline services): `-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m -XX:+AlwaysPreTouch -XX:+DisableExplicitGC`.
- **`pom.xml`** (Surefire/Failsafe test JVMs): `-XX:+UseZGC -Xmx512m -Xms512m -XX:+AlwaysPreTouch`.
- **`scripts/run_load_generator.sh`**: `-XX:+UseZGC -XX:+AlwaysPreTouch -XX:CompileThreshold=10000 -Xmx2G -Xms2G`.

## 5. Running the Benchmarks

All benchmark executions are unified under `scripts/run_benchmark.sh`. It automatically handles environment differences by loading configuration profiles from `config/profiles/*.env`.

> [!NOTE]
> See [`CONFIG_PROFILES.md`](CONFIG_PROFILES.md) for a complete reference of every profile variable,
> the docker two-path hlog design, CPU profile semantics, and runtime override examples.

### Local Native Benchmark
Use the local profile for repeatable native (macOS/Linux) measurements:
```bash
./scripts/run_benchmark.sh --profile local 10000 1000000
```
This script starts from clean queue/telemetry state, waits for event-loop readiness, runs the workload, drains services producer-first, and archives the manifest and report.

### Docker Benchmark
For a Docker comparison, use the docker profile with the exact same workload:
```bash
./scripts/run_benchmark.sh --profile docker 10000 1000000
```
*(To bypass image rebuilds, prefix with `FX_SKIP_BUILD=true`)*

### Bare-Metal Benchmark
Use the baremetal profile when testing on remote dedicated hardware:
```bash
./scripts/run_benchmark.sh --profile baremetal 10000 1000000
```

### Advanced Usage

You can explicitly pair local/Docker results under a single identifier using `FX_RUN_ID`:
```bash
FX_RUN_ID=baseline-10k ./scripts/run_benchmark.sh --profile local 10000 1000000
FX_RUN_ID=baseline-10k ./scripts/run_benchmark.sh --profile docker 10000 1000000
```

For downstream-only testing (bypassing the `serv-0` gateway), use the `--direct` flag:
```bash
./scripts/run_benchmark.sh --profile local 5000000 10000000 --direct
```

## 6. Docker Cleanup

When finished with Docker tests, stop containers and remove the named queue volume to prevent stale IPC files:
```bash
docker compose down --volumes --remove-orphans
```
This removes the `fx-pipeline_fx-queues` volume. It does not remove the bind-mounted `fx-data` or `fx-telemetry` directories, which you can clear manually:
```bash
rm -rf ./fx-data/* ./fx-telemetry/*
```

## 7. Pre-flight Verification Checklist (Baremetal)

Run these commands **on the Vultr server before every benchmark** to confirm all OS tuning is active. Any failure should be remediated via `scripts/setup_baremetal_os.sh` before proceeding.

| Check | Command | Expected output |
|---|---|---|
| GRUB params applied | `cat /proc/cmdline \| grep isolcpus` | `isolcpus=1-5` |
| SMT disabled | `cat /sys/devices/system/cpu/smt/active` | `0` |
| CPU cores isolated | `cat /sys/devices/system/cpu/isolated` | `1-5` |
| Timer ticks disabled | `cat /sys/devices/system/cpu/nohz_full` | `1-5` |
| THP set to madvise | `cat /sys/kernel/mm/transparent_hugepage/enabled` | `[madvise]` |
| CPU governor | `cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor` | `performance` |
| Logical core count | `nproc` | `4` (not 8, SMT disabled) |
| Queue on tmpfs | `stat -f -c '%T' /dev/shm/fx-queues` | `tmpfs` |

Quick one-liner to verify all at once:
```bash
echo "SMT=$(cat /sys/devices/system/cpu/smt/active) isolated=$(cat /sys/devices/system/cpu/isolated) thp=$(cat /sys/kernel/mm/transparent_hugepage/enabled | grep -o '\w*' | head -1) gov=$(cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor) nproc=$(nproc)"
# Expected: SMT=0 isolated=1-5 thp=madvise gov=performance nproc=4
```

## 8. Known Application-Level Pitfalls

### 8.1. THP + QueuePreToucher Interaction (Fixed in v1.0.2)

**Symptom:** Baremetal queue-a P50 is ~200µs instead of <5µs. Bimodal latency distribution. Max latency of 10–38ms.

**Root cause (two compounding bugs):**

1. **`QueuePreToucher` VarHandle byte-offset bug (fixed):** Initially, it was believed that `byteBufferViewVarHandle(int[].class, ...)` required an *int-element index* (`byteOffset / Integer.BYTES`). However, in Java 9+ it actually requires a **raw byte offset**. The previous code divided the offset by 4, causing the pretoucher to hit the same 4KB page multiple times and only ever reach byte offset 32 MB before the loop terminated. This left 93.8% (96 MB) of the 128 MB segment completely unfaulted.

2. **`-XX:+UseTransparentHugePages` in Linux profiles (removed):** Rocky Linux 9 defaults system THP to `always`. As the hot-path producer faulted each unfaulted page, `khugepaged` acquired `mmap_lock` (write) to coalesce 4 KB pages to 2 MB huge pages, stalling hot-path threads for 1–50 ms per coalescing event.

**Fix:** `QueuePreToucher` v1.0.2 corrects the VarHandle byte offset formula (adhering to zero GC and Disruptor principles by offloading page faults), fixes the `RandomAccessFile` fd leak, and adds a crash-loop guard. `-XX:+UseTransparentHugePages` is removed from all Linux profiles. `setup_baremetal_os.sh` sets system THP to `madvise`.

**Gotcha: Why wasn't this caught at 10,000 TPS?**
In earlier tests at 10,000 TPS, a 1,000,000 message payload generated total data volumes that fit entirely within the pre-touched 32 MB boundary. The bug remained completely hidden because the hot-path producer never advanced into the unfaulted regions. At higher throughputs (25,000 or 50,000 TPS) or larger volumes, the producer breached the 32 MB mark, suddenly exposing the 96 MB of unfaulted pages and triggering massive latency spikes.

**Why `local.env` intentionally omits `-XX:+UseTransparentHugePages`:** macOS does not support THP; the flag is silently ignored. The local profile uses a native HFS+ RAM disk where page faults cost ~100–500 ns regardless — making the pretoucher a no-op. This is the correct configuration and must not be changed.

**Cross-environment validation:** Mac Docker (Linux VM with THP + broken pretoucher) showed P50 = 4,026,367 ns — **1,638× slower than Mac native** (2,459 ns). This confirms the bugs alone, without any OS configuration difference, account for most of the observed latency gap.

### 8.2. taskset vs isolcpus Conflict

**Symptom:** End-to-end latency stuck in the multi-millisecond range (~2.8ms) on bare-metal Linux despite `isolcpus` being configured correctly.

**Root cause:** Launching the JVM via `taskset -c 0,X` to allow background threads on Core 0 and the event loop on Core X overrides the `isolcpus` kernel parameter. The OS scheduler treats Core X as explicitly permitted for that process, and will dynamically migrate heavy background threads (like GC or the JDBC `db-writer`) onto the isolated Core X to balance load. These background threads then preempt the hot-path event loop, causing millisecond-scale latency spikes.

Attempting to fix this by using `taskset -c 0` also fails, because OpenHFT `AffinityLock` natively respects the process's allowed CPU mask; it will silently fail to acquire Core X (reporting "CPU affinity unavailable") and leave the event loop stranded on Core 0 alongside the background threads.

**Fix:** Do **not** use `taskset` for JVMs on bare-metal systems. Launch the JVM without CPU restrictions. Because the isolated cores are marked as `isolcpus`, the Linux scheduler will automatically restrict all naturally spawned JVM threads to the housekeeping core (Core 0). Inside the Java code, `AffinityLock.acquireLock(X)` will use a native JNI `sched_setaffinity` system call to pull *only* the single event-loop thread over to the isolated core, achieving perfect mechanical sympathy.

### 8.3. macOS Scheduler Preemption (Hockey Stick Spikes)

**Symptom:** On macOS (`local` profile), latency is extremely good out to the 99th percentile (e.g., 66µs at 50k TPS), but suffers sudden, massive multi-millisecond spikes at P99.9, P99.99, and Max (e.g., 5.7ms to 89ms).

**Root cause:** LMAX Disruptor principles dictate that a `busyspin` wait strategy must only be used when the hot-path thread can be strictly pinned to an isolated core. Because macOS fundamentally does not support thread pinning (`taskset` or `isolcpus`), OpenHFT `AffinityLock` falls back to an unpinned state. 

Since your unpinned services are configured to busy-spin, they consume 100% of their respective CPU cores. The macOS scheduler responds to this heavy compute load by occasionally preempting the threads to run background processes (ZGC, Telemetry, system daemons) or migrating them between Performance (P) and Efficiency (E) cores. A typical OS context switch takes 5–10 milliseconds. At 50,000 TPS, a 5ms preemption stall delays ~250 messages. Since this only happens occasionally, it exclusively affects the extreme tail percentiles (P99.9+), resulting in the classic "bimodal" or "hockey stick" latency curve.

**Historical Fix & Timer Slack Pitfall (`phased`, original `SPIN_TRIES=200`):** Initially, to mitigate these spikes, the wait strategy was changed to `phased` (which spins, yields, then parks via `LockSupport.parkNanos(1000)`). While this successfully brought the 89ms preemption Max down to ~7ms, it completely destroyed the baseline P50 latency (inflating it from 7µs to ~38µs). The root cause is OS timer granularity: calling `parkNanos(1000)` on macOS results in an actual sleep of 30–50µs. `SPIN_TRIES` was later retuned to `10_000` (see `PhasedBackOffWaitStrategy`) specifically to fix this P50 regression — see the verification note below for how that retuned version actually behaves.

**Modern Fix (`yielding`):** The correct LMAX Disruptor architectural approach for shared-CPU, non-isolated environments is to cooperatively yield *without ever parking*.
```bash
# In config/profiles/local.env
FX_WAIT_STRATEGY="yielding"
```
The `yielding` strategy spins for 1,000 iterations to absorb micro-bursts, and then continuously invokes `Thread.yield()` indefinitely. `Thread.yield()` cooperatively relinquishes the CPU to other threads, reducing (see below — not eliminating) forceful OS-scheduler preemption of the pipeline, while completely avoiding the 30–50µs timer slack of `parkNanos`. Real bare-metal (`isolcpus`) does eliminate *this specific* macOS-scheduler-preemption mechanism — but it is not a "perfectly flat P99.99+" guarantee on its own; see §8.4 for a separate, unrelated capacity ceiling that appears at the same 50k msg/s rate on genuine bare-metal.

### 8.4. Bare-Metal `queue-c` / H2 Capacity Ceiling at 50k msg/s (distinct from §8.3 — not a wait-strategy or scheduler issue)

**Symptom:** On real Vultr bare-metal (`baremetal_vultr`, AMD EPYC 4345P, verified `isolcpus`/`cpu_profile=isolated`, `busyspin`), sustained 50k msg/s runs reproduced (3 of 3 runs, both 1M and 10M message counts) a severe tail: e2e P99.9 ranging **134ms–325ms**, Max **323ms–495ms** — far worse than anything seen on macOS local. At 10,000 msg/s the same environment is clean (P50 ~3.7µs, no comparable blowup on native bare-metal; a milder version was also seen on Vultr-hosted Docker at 10k msg/s, so the effect is not exclusive to the highest rate tested).

**Root cause (stage-isolated and code-grounded, not just inferred from aggregates):** per-stage Max values pin this precisely to `queue-c`: in every affected run, `queue-c`'s own Max is essentially identical to the e2e Max (e.g. one run: queue-c=348,651,519ns vs e2e=348,651,519ns — exact match), while `queue-a`/`queue-b` stay in the microsecond-to-low-millisecond range and `serv-a`/`serv-b`/`serv-c`'s own per-event handler dispatch time stays sub-microsecond to low-microsecond (serv-c Max ≤ ~34µs in every run). This means the delay is not in any service's own processing — it is entirely in the wait between `serv-b` appending to `queue-c` and `serv-c`'s tailer calling `handle()` for it.

`BatchPersistenceEngine`'s own Javadoc documents the exact mechanism this matches: its 524,288-slot ring buffer exists specifically to let the `db-writer` thread's H2 `executeBatch()`/`commit()` run in the background without blocking `accumulate()` (called from serv-c's hot path) — but `accumulate()` still spin-waits if the ring is ever completely full (`writePointer - readPointer >= RING_SIZE`), and that spin-wait blocks the same thread that calls `tailer.readDocument()`, so a ring-full stall inflates `t3ServCEntry` (and therefore measured `queue-c` latency) for every event still queued behind it. The class's own history already documents H2 MVStore commit time growing as the `fx_trades` table grows (`MAX_BATCH` was previously reduced from 32,768 to 8,192 for exactly this reason, after commits were observed taking 200ms–2s+ once the table passed ~1M rows). The 10M-message runs (which grow the table furthest) show a worse Max (323–495ms) than the 1M-message run (25.5ms) — consistent with commit time continuing to grow with table size even under the already-reduced `MAX_BATCH=8,192`, eventually catching up with the ring's absorption capacity.

**This is a different mechanism from §8.3, not a variant of it**: it reproduces on genuine `isolcpus` hardware where OS-scheduler preemption of a busy-spin thread cannot happen, it is unrelated to `WaitStrategy` choice (all runs used `busyspin`, the unchanged bare-metal default), and it originates at `queue-c`/persistence, not `queue-a`/ingress.

**Confidence level — plausible and code-consistent, not yet instrumented live:** this analysis is based on historical run archives (2026-09-09/09-10) that predate this repo's GC-log and `IntervalHistogramExporter` tooling (added 2026-09-12), so there is no direct GC-pause or ring-occupancy measurement confirming the exact moment/cause the way the macOS `serv-c` GC correlation was checked. The stage-isolation evidence (queue-c Max = e2e Max, every other stage negligible) and the code-level mechanism are strong, but a live re-run on the same bare-metal host with current tooling would be needed to call this fully confirmed rather than well-evidenced.

**No fix has been applied.** Candidate directions (all unvalidated, listed only for reference, none implemented): increasing `RING_SIZE` further, reducing `MAX_BATCH` further, partitioning/archiving `fx_trades` to bound B-tree depth, or moving off in-memory H2 for sustained high-throughput persistence — every one of these needs its own before/after A/B at 50k msg/s on real bare-metal before being called a fix, per this repo's own evidence bar.

**Update (2026-09-13):** the original runs above were flagged by the user as having used
the wrong profile; two corrected re-runs (on different, 6-core hosts — Intel Xeon
E-2286G and AMD EPYC 4245P, vs. the original 8-core AMD EPYC 4345P) reproduce the
`queue-c` finding again (e2e Max up to 652.7ms, the worst seen yet), but *also* show
`queue-a`/`serv-a` developing their own large-scale tail (up to 158ms Max) — not present
on the original 8-core host. `serv-a`'s own dispatch time stays tiny in these runs, same
shape as the `queue-c` mechanism, but `RiskValidationEventLoop` has no ring-buffer
backpressure component to explain it structurally the way `BatchPersistenceEngine` does.
Whether this is a second real mechanism the wrong profile had been masking, or an
artifact of less spare CPU capacity on the newer 6-core hosts (services already pin
cores 0-4; an 8-core host has 2 fully spare cores beyond that, a 6-core host has none),
is unresolved — see `LATENCY_RCA.md`'s "Correction (2026-09-13)" section for full data.

**Verification note (2026-09-12, 13 runs at 50k msg/s / 10M messages on current `HEAD` — see `LATENCY_RCA.md` for full data):** the claims above about `yielding` need correcting. Measured `yielding` P50 across 6 repetitions is consistently **~25µs, not ~7µs** — it does not "retain" the busy-spin P50, it pays a real, ~3.3x median cost. It also does **not** eliminate the preemption spikes, only reduces their frequency: 1 of 6 runs still hit a 26.3ms P99.9. Separately, the retuned `phased` (`SPIN_TRIES=10_000`) *does* measure a low P50 (~7.6µs, matching `busyspin`) as intended, but this is because at 50k msg/s (~20µs inter-arrival time) it rarely reaches its yield/park phase at all — it behaves almost identically to `busyspin` under sustained load, including inheriting a similar-or-worse preemption-spike frequency (2 of 4 runs ≥10ms P99.9 in the same test). `busyspin` itself was also re-tested directly at this scale (3 runs) and shows the same intermittent, queue-a-first stall signature (1 of 3 borderline/elevated). **None of the three strategies eliminates the underlying macOS scheduler-preemption risk; they only trade median latency against how often it is observed, by controlling how long the thread monopolizes the CPU before voluntarily yielding it.** `yielding` remains the best-evidenced `local` default of the three (lowest observed elevated-tail rate), not because it is fastest, but because it yields soonest.
