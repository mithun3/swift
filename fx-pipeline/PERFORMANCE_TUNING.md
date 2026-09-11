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

**Fix:** This is an expected architectural limitation of running a busy-spin low-latency system on a general-purpose, non-isolated desktop OS. To mitigate (but not entirely eliminate) these spikes on macOS dev environments, the wait strategy for unpinned environments should be changed to yield to the OS:
```bash
# In config/profiles/local.env
FX_WAIT_STRATEGY="phased"
```
This reduces the scheduler's aggressiveness by yielding CPU time when the queue is empty, preventing the 100% CPU lockup that triggers severe preemption, at the cost of slightly higher base latencies. To achieve a perfectly flat latency profile out to P99.99+, you must execute the pipeline on a native Linux bare-metal server configured with strict CPU isolation.
