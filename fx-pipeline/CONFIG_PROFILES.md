# Configuration Profiles Reference

All benchmark runs are driven by a **configuration profile** — a `.env` file in
`config/profiles/` that declares every environment-specific variable in one place.
`run_benchmark.sh` loads the named profile with `set -a / source / set +a`, so every
variable is automatically exported to child processes (runners, report scripts, containers).

---

## 1. Profiles at a Glance

| Profile | `--profile` flag | Execution Mode | Target Environment |
|---|---|---|---|
| [`docker.env`](config/profiles/docker.env) | `docker` | Docker Compose | macOS Docker Desktop (dev / baseline comparison) |
| [`local.env`](config/profiles/local.env) | `local` | Native JVM | macOS native (fast iteration) |
| [`baremetal.env`](config/profiles/baremetal.env) | `baremetal` | Native JVM | Vultr Bare Metal, Rocky Linux 9 (production benchmark) |
| [`ec2.env`](config/profiles/ec2.env) | `ec2` | Native JVM | AWS EC2 dedicated / bare-metal, Rocky Linux 9 |

```bash
# Select a profile with --profile:
./scripts/run_benchmark.sh --profile docker    10000 1000000
./scripts/run_benchmark.sh --profile local     10000 1000000
./scripts/run_benchmark.sh --profile baremetal 10000 1000000
./scripts/run_benchmark.sh --profile ec2       10000 1000000
```

---

## 2. Variable Reference

Every profile defines the same set of variables. The table below describes each one,
its type, and how it is consumed.

### Identity

| Variable | Description | Example values |
|---|---|---|
| `FX_ENV_LABEL` | Human-readable label embedded in the Run ID and archive path. Override at runtime with `--env-label`. | `docker`, `local`, `baremetal_vultr`, `ec2` |
| `FX_EXECUTION_MODE` | Selects the runner script (`scripts/runners/${FX_EXECUTION_MODE}_runner.sh`). | `docker`, `native` |
| `FX_CPU_PROFILE` | Records the CPU pinning strategy in the run manifest. Also drives cpuset default-selection logic in `docker_runner.sh` when set to `auto`. | `host`, `isolated`, `desktop`, `auto` |

### HLog Paths

> [!IMPORTANT]
> The `docker` profile uses **two separate path variables** because the Docker bind mount
> creates a split between the container-side path and the host-side path. Native profiles
> set both to the same value.

| Variable | Description | `docker` | `local` | `baremetal` / `ec2` |
|---|---|---|---|---|
| `FX_HLOG_DIR` | **Container-side** (docker) or native path where services write `.hlog` files. Used by `docker_runner.sh` to clear histograms between warmup and measurement via `docker compose exec`. | `/tmp/fx-telemetry` | `/tmp` | `/tmp/fx-telemetry` |
| `FX_HOST_HLOG_DIR` | **Host-side** path where `.hlog` files are readable. In docker mode this is the bind-mount source (`./fx-telemetry` ↔ `/tmp/fx-telemetry`). `generate_benchmark_report.sh` reads from here and archives from here. | `fx-telemetry` | `/tmp` | `/tmp/fx-telemetry` |

**Why two variables in docker mode?**

`docker-compose.yml` bind-mounts `./fx-telemetry` on the host to `/tmp/fx-telemetry`
inside every container. Services write hlogs at `/tmp/fx-telemetry/fx-latency*.hlog`
(inside the container). After the run, the same files appear at `fx-telemetry/fx-latency*.hlog`
on the host. `generate_benchmark_report.sh` resolves the correct paths automatically:

```bash
# generate_benchmark_report.sh (simplified)
HOST_HLOG_DIR="${FX_HOST_HLOG_DIR:-$FX_HLOG_DIR}"      # host path
HLOG_FILES=( "${HOST_HLOG_DIR}"/fx-latency*.hlog )      # glob on host

# For docker: translate host paths → container paths before passing to docker compose run
DOCKER_HLOG_FILES+=( "${hlog/$HOST_HLOG_DIR//tmp/fx-telemetry}" )
```

### Queue

| Variable | Description | All profiles |
|---|---|---|
| `FX_QUEUE_DIR` | Root directory for Chronicle Queue segments. Must be on a fast filesystem (tmpfs / RAM disk). | `/tmp/fx-queues` |

### JVM

| Variable | Description | Notes |
|---|---|---|
| `FX_WAIT_STRATEGY` | Wait strategy passed to all services via `-Dfx.waitstrategy`. | `busyspin` for benchmarks; `phased` or `sleeping` for dev |
| `FX_JVM_OPTS_OVERRIDE` | Extra JVM flags appended to the base `JVM_OPTS` in `start.sh`. Overrides GC, heap, and THP settings. | THP (`-XX:+UseTransparentHugePages`) requires Linux; omit on macOS |

### CPU Pinning

Each variable maps to a `taskset -c <cpuset>` prefix in the launch command (native mode)
or to the `cpuset:` directive in `docker-compose.yml` (docker mode).

| Variable | Service | Core in all profiles |
|---|---|---|
| `FX_SERV_0_CPUSET` | Gateway (`serv-0`) | `1` |
| `FX_SERV_A_CPUSET` | Risk (`serv-a`) | `2` |
| `FX_SERV_B_CPUSET` | Pricing (`serv-b`) | `3` |
| `FX_SERV_C_CPUSET` | Persistence (`serv-c`) | `4` |
| `FX_BENCHMARK_CPUSET` | Load generator | `5` |
| `FX_TELEMETRY_CPUSET` | Telemetry stitcher | `""` (unconstrained) |

> [!NOTE]
> Core 0 is intentionally left free on all profiles. On Linux bare-metal it handles OS
> scheduler activity, SSH, and IRQs. On macOS Docker Desktop, `taskset` has no effect —
> these values are used for manifest recording only.

**`FX_CPU_PROFILE` values explained:**

| Value | Meaning | When used |
|---|---|---|
| `host` | All containers share all available CPUs. No per-service pinning defaults applied. | macOS Docker Desktop; macOS native (`local`) |
| `isolated` | One dedicated physical core per service. Best for SMT-disabled bare-metal. | `baremetal`, `ec2` |
| `desktop` | Each service gets a physical core **and** its HT sibling. For SMT-enabled consumer desktops (≥10 cores). | Not used by any current profile |
| `auto` | `docker_runner.sh` auto-selects `desktop` / `isolated` / `host` based on detected CPU count. | Set in profile to enable auto-detection |

### Timing

| Variable | Default | Description |
|---|---|---|
| `FX_WARMUP_SECONDS` | `20` | Duration of the unmeasured JIT warm-up phase at 10% of target rate. Set to `0` to skip. |
| `FX_STARTUP_TIMEOUT_SECONDS` | `30` | How long the runner waits for all services to report `"Event loop started"` before aborting. |
| `FX_STOP_TIMEOUT_SECONDS` | `40` | Per-service graceful shutdown timeout. Must exceed `fx.eventloop.drainTimeoutMillis` (default 30 s) to allow safe backlog drain. |

### Build

| Variable | Default | Description |
|---|---|---|
| `FX_SKIP_BUILD` | `false` | Docker only. Set to `true` to skip `docker build` and reuse the existing `fx-pipeline:latest` image. Useful for rapid re-runs without code changes. |

---

## 3. Override at Runtime

Any variable in a profile can be overridden at the shell level without editing the file:

```bash
# Skip the Docker image rebuild for a fast re-run
FX_SKIP_BUILD=true ./scripts/run_benchmark.sh --profile docker 10000 1000000

# Extend warmup to 60 s for a thorough JIT compilation phase
FX_WARMUP_SECONDS=60 ./scripts/run_benchmark.sh --profile baremetal 50000 5000000

# Use a custom run output directory
FX_RUN_OUTPUT_DIR=benchmark-runs/custom-label \
    ./scripts/run_benchmark.sh --profile local 10000 1000000

# Pair a local and docker run under the same ID for direct comparison
FX_RUN_ID=baseline-10k ./scripts/run_benchmark.sh --profile local  10000 1000000
FX_RUN_ID=baseline-10k ./scripts/run_benchmark.sh --profile docker 10000 1000000
```

---

## 4. Adding a New Profile

1. Copy the closest existing profile:
   ```bash
   cp config/profiles/baremetal.env config/profiles/myhostname.env
   ```

2. Update the identity variables:
   ```bash
   FX_ENV_LABEL="myhostname"
   FX_EXECUTION_MODE="native"   # or "docker"
   FX_CPU_PROFILE="isolated"    # or "host"
   ```

3. Adjust `FX_HLOG_DIR`, `FX_HOST_HLOG_DIR`, and cpuset values for the new environment.

4. Run:
   ```bash
   ./scripts/run_benchmark.sh --profile myhostname 10000 1000000
   ```

No code changes required — `run_benchmark.sh` discovers profiles dynamically from `config/profiles/*.env`.

---

## 5. How Profiles Flow Through the Pipeline

```
run_benchmark.sh
  └─ source config/profiles/<profile>.env   (set -a exports all vars)
       │
       ├─ source scripts/runners/${FX_EXECUTION_MODE}_runner.sh
       │    ├─ runner_start_services()      reads: FX_CPU_PROFILE, FX_*_CPUSET, FX_SKIP_BUILD
       │    ├─ runner_wait_ready()          reads: FX_STARTUP_TIMEOUT_SECONDS
       │    ├─ runner_warmup()              reads: FX_WARMUP_SECONDS, FX_HLOG_DIR
       │    ├─ runner_measure()             reads: FX_QUEUE_DIR
       │    └─ runner_stop_services()       reads: FX_STOP_TIMEOUT_SECONDS
       │
       └─ scripts/generate_benchmark_report.sh
            ├─ process_latency.sh          reads: FX_HOST_HLOG_DIR → HLOG_FILES glob
            ├─ generate_run_manifest.py    reads: FX_CPU_PROFILE, FX_*_CPUSET, FX_QUEUE_DIR
            ├─ generate_html_report.py     reads: HLOG_FILES
            └─ archive to benchmark-runs/$ENV_LABEL/$FX_RUN_ID/
```
