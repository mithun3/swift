# FX Pipeline Hardware & OS Tuning Checklist

To achieve accurate, repeatable sub-millisecond tail latencies on the JVM, you must configure the underlying OS and hardware to remove sources of jitter (context switches, CPU frequency scaling, page faults).

## 1. Boot Parameters (GRUB / kernel)
Edit `/etc/default/grub` and update `GRUB_CMDLINE_LINUX_DEFAULT`:
- `isolcpus=2-11`: Isolate cores 2 through 11 from the Linux scheduler. The OS will not schedule arbitrary user-space or kernel threads on these cores.
- `nohz_full=2-11`: Enable tickless kernel for these cores (stops the 1000Hz timer interrupt).
- `rcu_nocbs=2-11`: Move RCU callbacks away from isolated cores.
- `intel_idle.max_cstate=0 processor.max_cstate=0 idle=poll`: Disable deep sleep C-states to prevent wake-up latency penalties (which can be 10-100+ microseconds).

## 2. CPU Governor
Force the CPU to run at maximum frequency to avoid P-state transition jitter:
```bash
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
```

## 3. JVM Flags (Production & Benchmark)
The pipeline should be run with the following JVM flags (included in `scripts/deploy.sh` and `pom.xml`):
- `-XX:+UseZGC`: Z Garbage Collector for sub-millisecond pause times.
- `-XX:+AlwaysPreTouch`: Touch all allocated memory pages at startup to avoid page faults during runtime.
- `-XX:-UseBiasedLocking`: (If using JDK < 15) Disable biased locking which can cause safepoint pauses when revoking locks.
- `-XX:CompileThreshold=10000`: Force earlier C2 compilation (or use `-Xcomp` with care) to avoid JIT compilation on the hot path.
- `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints`: Improve async profiler visibility.

## 4. Running the Benchmark
1. Pin the `serv-test-gen` load generator to an isolated core (e.g., Core 2) using `AffinityLock`.
2. Pin `serv-0`, `serv-a`, `serv-b`, `serv-c` to Cores 3, 4, 5, 6 respectively.
3. Start the services.
4. Run the load generator at the desired rate.
5. Process the output `.hlog` files using the provided Python visualization script.
