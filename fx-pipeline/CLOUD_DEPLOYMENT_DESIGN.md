# Cloud Deployment & Tuning Design Considerations

This document records the architectural and infrastructural decisions for running the ultra-low-latency FX Pipeline benchmark on cloud infrastructure.

## 1. Infrastructure Choice: True Bare Metal

**Decision:** We mandate the use of true bare-metal servers (e.g., Vultr Bare Metal, Hetzner Dedicated) rather than standard virtualized cloud instances (e.g., standard AWS EC2, GCP Compute Engine).

**Rationale:**
- **Hypervisor Jitter:** Virtual machines share physical CPU cores with other tenants or the hypervisor itself. Even with dedicated vCPUs, the hypervisor can still preempt the VM to service hypervisor-level interrupts or manage host memory. In an HFT pipeline where we are measuring sub-millisecond tail latencies, a 1-millisecond hypervisor pause ruins the 99.99th percentile metric.
- **Cost-Efficiency:** While AWS offers `c6i.metal` bare-metal instances, they are prohibitively expensive for iterative benchmark testing. Providers like Vultr (hourly API provisioning) and Hetzner (cheap monthly dedications) provide the exact same mechanical isolation at a fraction of the cost.

## 2. Operating System: Rocky Linux 9

**Decision:** The deployment standardizes on **Rocky Linux 9** (a 1:1 RHEL clone) rather than Debian/Ubuntu.

**Rationale:**
- **Enterprise Tooling (`tuned`):** Achieving sub-millisecond determinism requires strict CPU isolation. On Ubuntu, this requires manual and often fragile GRUB parameter edits (`isolcpus`, `nohz_full`, IRQ affinity routing). Rocky Linux includes the enterprise `tuned` daemon natively. By enabling the `cpu-partitioning` profile, Rocky Linux automatically ensures that all OS noise, kernel threads, and hardware interrupts are kept strictly away from the isolated trading cores.
- **Industry Standard:** RHEL-based distributions are the absolute standard in institutional finance. Furthermore, if the pipeline later implements hardware kernel bypass (e.g., Solarflare OpenOnload or Mellanox VMA), those proprietary drivers are traditionally certified for RHEL-based kernels first.

## 3. Network Stack vs. Shared Memory (IPC)

**Decision:** We are explicitly *not* tuning the Linux network stack (e.g., kernel bypass, TCP `TCP_NODELAY`, DPDK) for the benchmark phase.

**Rationale:**
- The pipeline utilizes an internal `LoadGenerator` running on the exact same physical node. 
- Because all communication between the load generator and the pipeline services (`serv-0`, `serv-a`, `serv-b`, `serv-c`) occurs via Chronicle Queue memory-mapped files (`tmpfs`), the packets never traverse the Linux network stack.
- This allows us to focus entirely on CPU scheduling, memory locality, and disk I/O (persistence) tuning without the compounding variables of TCP stack jitter. Network tuning will become relevant only when an external load generator is introduced.

## 4. Hardware Sympathy Tuning Summary

When deploying to the selected bare metal node, the following configurations are mandated via the provisioning script:
1. **Hyperthreading (SMT) Disabled:** Prevents background tasks from polluting the L1/L2 caches of the trading cores.
2. **C-States Disabled:** `intel_idle.max_cstate=0` prevents the CPU from entering deep sleep modes, avoiding the microsecond wake-up penalty.
3. **CPU Isolation:** Cores 1-5 are completely isolated from the OS scheduler, leaving Core 0 to handle SSH, Docker daemon, and system interrupts.
