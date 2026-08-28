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
The flags actually applied differ by script/module — there is no `scripts/deploy.sh`
(that script does not exist in this repo). What's really configured today:
- **`scripts/start.sh`** (launches the 4 pipeline services): `-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m -XX:+AlwaysPreTouch -XX:+DisableExplicitGC`.
- **`pom.xml`** (Surefire/Failsafe test JVMs): `-XX:+UseZGC -Xmx512m -Xms512m -XX:+AlwaysPreTouch` (plus Chronicle `--add-exports`/`--add-opens`; Surefire additionally sets `-XX:+DisableExplicitGC`).
- **`scripts/run_load_generator.sh`**: `-XX:+UseZGC -XX:+AlwaysPreTouch -XX:CompileThreshold=10000 -Xmx2G -Xms2G`.

Notes on flags not currently used anywhere in this repo:
- `-XX:-UseBiasedLocking` — biased locking was removed entirely in Java 21 (JEP 374); this project targets Java 21, so omit this flag rather than pass it (it would fail JVM startup with `-XX:+UnlockExperimentalVMOptions` needed, or simply be rejected as unrecognized).
- `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` — useful for async-profiler runs but not wired into any script here; add them manually to your own profiling invocation if needed.

## 4. Running the Benchmark
1. Start the pipeline: `./scripts/start.sh`
2. Pin the `LoadGenerator` to an isolated core — handled automatically via `AffinityLock` inside the JVM.
3. Run the load generator through serv-0 (TCP mode, default):
   ```bash
   ./scripts/run_load_generator.sh /tmp/fx-queues/queue-a 5000000 10000000
   ```
   Or bypass serv-0 for downstream-only measurement (direct mode):
   ```bash
   ./scripts/run_load_generator.sh /tmp/fx-queues/queue-a 5000000 10000000 --direct
   ```
4. Stop the pipeline to flush all telemetry buffers: `./scripts/stop.sh`
5. Process the output `.hlog` files using the provided Python visualization script:
   ```bash
   ./scripts/process_latency.sh /tmp/fx-latency*.hlog
   python3 scripts/generate_html_report.py /tmp/fx-latency*.hlog
   ```

Or use the full orchestrated suite (handles steps 3–5 automatically):
```bash
./scripts/run_benchmark_suite.sh /tmp/fx-queues/queue-a 5000000 10000000          # --tcp default
./scripts/run_benchmark_suite.sh /tmp/fx-queues/queue-a 5000000 10000000 --direct  # downstream only
```
