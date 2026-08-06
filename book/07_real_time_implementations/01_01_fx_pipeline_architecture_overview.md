# 07. Real-Time Implementations: Case Study of an HFT FX Pipeline

In previous chapters, we discussed the LMAX Disruptor architecture, mechanical sympathy, and zero-allocation techniques. This chapter puts those concepts into practice by examining a real-world ultra-low-latency Foreign Exchange (FX) Pipeline designed using pure Java 21 and Chronicle Queue.

## System Architecture & Flow

The system consists of 4 microservices communicating sequentially via memory-mapped, zero-copy Chronicle Queues (`queue-a`, `queue-b`, `queue-c`).

```text
Synthetic FIX ──> [serv-0] ──queue-a──> [serv-a] ──queue-b──> [serv-b] ──queue-c──> [serv-c] ──> H2 DB
```

1. **serv-0 (Client Gateway):** Ingests incoming FIX messages, decodes them without string allocation, generates a monotonic Correlation ID, stamps an ingress nanosecond timestamp, and appends the `FxMarketEvent` flyweight to `queue-a`.
2. **serv-a (Risk Validation):** Tails `queue-a` using a busy-spin event loop. Performs credit and tier checks, mutates the event state in-place, and writes it to `queue-b`.
3. **serv-b (Pricing Matching):** Tails `queue-b`, applies FX spreads and normalizes pricing, updates the executed price, and writes it to `queue-c`.
4. **serv-c (Persistence Egress):** Tails `queue-c` and asynchronously batches writes into an in-memory H2 database.

## Technical Constraints

- **LMAX Philosophy:** Single-writer principle per queue, busy-spin wait strategies, memory-mapped IPC.
- **Zero-Allocation:** Mutable `FxMarketEvent` flyweights, primitive arrays, no `java.util.stream` or `String` manipulation in the hot path.
- **Mechanical Sympathy:** CPU pinning using `Java-Thread-Affinity`, sequential cache-friendly access, and cache-line padded data structures.

> [!TIP]
> This architecture demonstrates how replacing traditional in-memory queues with memory-mapped inter-process communication (IPC) can drastically reduce latency and garbage collection overhead.
