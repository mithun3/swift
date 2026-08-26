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

Build the image:
```bash
docker build -t fx-pipeline:latest .
```

Start the services:
```bash
docker-compose up -d
```

Monitor logs:
```bash
docker-compose logs -f
```


# 1. Bring down the environment AND remove the full volumes (-v)
```bash
docker-compose down -v
```
# 2. Bring it back up fresh
```bash
docker-compose up -d
```
# 3. Run the benchmark again
```bash
docker-compose run --rm benchmark /app/scripts/run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 5000000 /tmp/fx-telemetry/fx-latency-queue-a.hlog /tmp/fx-telemetry/fx-latency-serv-a.hlog /tmp/fx-telemetry/fx-latency-serv-b.hlog /tmp/fx-telemetry/fx-latency.hlog
```