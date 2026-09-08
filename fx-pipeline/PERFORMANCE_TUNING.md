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
