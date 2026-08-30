# Docker Low-Latency Tuning Guide

To achieve ultra-low latency (sub-millisecond) with the dockerized FX Pipeline, you must configure the underlying Linux host OS. Docker relies on the host's kernel for CPU scheduling and network bridging.

## 1. Directory Prerequisites

Ensure the host directories for persistence and telemetry exist with proper permissions:
```bash
mkdir -p ./fx-data ./fx-telemetry
chmod 777 ./fx-data ./fx-telemetry
```

## 2. Kernel Boot Parameters (GRUB)

You must isolate CPUs to prevent the Linux scheduler from placing background tasks (or other Docker containers) on the cores we're using for the trading pipeline and load generator. We need to isolate cores 0, 1, 2, 3, and 4.

Open `/etc/default/grub` and modify `GRUB_CMDLINE_LINUX_DEFAULT`:
```text
GRUB_CMDLINE_LINUX_DEFAULT="quiet splash isolcpus=0,1,2,3,4 intel_idle.max_cstate=0 processor.max_cstate=0"
```
- `isolcpus=0,1,2,3,4`: Removes these cores from the general SMP balancing and scheduling algorithms.
- `intel_idle.max_cstate=0 processor.max_cstate=0`: Disables deep sleep states, ensuring the CPU never "spins down" and avoids the microsecond latency hit required to wake it back up.

Update GRUB and reboot:
```bash
sudo update-grub
sudo reboot
```

## 3. Disabling Hyperthreading (Optional but Recommended)

For true mechanical sympathy, you should disable hyperthreading (SMT) in the system BIOS, or at runtime:
```bash
echo off > /sys/devices/system/cpu/smt/control
```
This ensures that your pipeline threads have exclusive access to the physical core's L1/L2 caches without eviction from logical sibling threads.

## 4. Running the Pipeline

Once the host is tuned, you can launch the pipeline. The `docker-compose.yml` is already configured to request explicit CPU affinities via `cpuset` and `SYS_NICE` capabilities.

Docker owns CPU placement for container runs. JVM-level OpenHFT affinity is disabled inside the containers because combining it with `cpuset` can select a CPU outside the container's allowed mask and terminate the event-loop worker with `sched_setaffinity(...)=EINVAL`.

Build the image:
```bash
docker build -t fx-pipeline:latest .
```

Start the services without the benchmark profile:
```bash
docker compose up -d
```

Monitor logs:
```bash
docker compose logs -f
```

Verify that every service is running and that no event-loop exception occurred:
```bash
docker compose ps -a
docker compose logs --tail=100 serv-0 serv-a serv-b serv-c
```

## 5. Running a Benchmark

Use the Docker-native benchmark wrapper. It rebuilds the image, recreates the queue volume, waits for every event loop, runs the TCP load, stops services in producer-first order so queues drain and telemetry flushes, and generates the report.

For a macOS or Docker Desktop calibration run:
```bash
./scripts/run_docker_benchmark.sh 150000 100000
```

To reuse an image that was already built from the current sources:
```bash
FX_SKIP_BUILD=true ./scripts/run_docker_benchmark.sh 150000 100000
```

For the larger Linux-host run:
```bash
./scripts/run_docker_benchmark.sh 500000 5000000
```

The report is written to `fx-telemetry/latency_report.html`.

Do not use `docker compose --profile benchmark up`. It starts the benchmark alongside services without application-readiness ordering and remains attached to the long-running service containers. Do not invoke `run_benchmark_suite.sh` inside the benchmark container either: that script calls the bare-metal `stop.sh`, which manages `logs/services.pid` rather than Compose containers.

## 6. Cleanup

Stop containers and remove the named queue volume:
```bash
docker compose down --volumes --remove-orphans
```

This removes `fx-pipeline_fx-queues`. It does not remove the bind-mounted `fx-data` or `fx-telemetry` host directories. To clear those outputs too:
```bash
rm -rf ./fx-data/* ./fx-telemetry/*
```

If Docker reports that the volume is still in use, identify the remaining container before removing it:
```bash
docker ps -a --filter volume=fx-pipeline_fx-queues
docker rm -f <container-id>
docker volume rm fx-pipeline_fx-queues
```