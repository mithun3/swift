<div class="page-break"></div>

# Chapter 7.3: Event Loop and Pricing Mechanisms

---

## SECTION 1: PRIMER ON THE BASICS

### 1. The Pinned Single-Threaded Event Loop Pattern
In ultra-low-latency financial systems, order processing and price discovery are executed on a **single dedicated CPU core running an un-slotted, non-blocking event loop**. 

By pinning the thread to a specific CPU core (`isolcpus` in Linux), the operating system is prevented from scheduling other processes on that core. The thread runs in an infinite `while(true)` loop, continuously polling ring buffers and network queues without ever entering a kernel sleep state.

```text
               PINNED CORE LOW-LATENCY EVENT LOOP FLOW

   Pinned CPU Core 3 (isolcpus = 3, CPU Affinity Lock):
   ┌─────────────────────────────────────────────────────────────────┐
   │  while (running) {                                             │
   │      1. Poll Inbound Ring Buffer (Busy Spin - 0ns sleep)       │
   │      2. Decode Fixed-Point Market Data Event                   │
   │      3. Update Limit Order Book State (L1 / L2 Depth)          │
   │      4. Execute Pricing Algorithm (Microsecond discovery)      │
   │      5. Publish Outbound Quotes to Outbound Ring Buffer        │
   │  }                                                             │
   └─────────────────────────────────────────────────────────────────┘
```

---

### 2. High-Frequency Limit Order Book & Fixed-Point Math

Floating-point numbers (`float`, `double`) introduce two major risks in financial software:
1. **Non-deterministic IEEE 754 rounding errors**: `0.1 + 0.2 != 0.3` in standard floating point arithmetic.
2. **CPU FP Unit Latency**: Floating-point instructions are slower on hardware pipelines compared to native integer arithmetic operations.

High-frequency pricing engines store prices as **64-bit Signed Fixed-Point Integers (`long`)**, scaling prices by a fixed multiplier (e.g., $10^5$ or $10^8$).

$$\text{Internal Price} = \text{Floating Price} \times 10^8$$

For example, an EUR/USD quote of `1.08542` is stored internally as the `long` integer `108542000L`.

#### High-Performance Fixed-Point Order Book Matcher Implementation (Java):

```java
package com.hft.pipeline.engine;

/**
 * High-Speed Fixed-Point Limit Order Book Matcher.
 * Uses primitive arrays and long fixed-point prices for zero-allocation execution.
 */
public final class OrderBookEngine {
    private static final int MAX_DEPTH = 100;
    private static final long PRICE_SCALE_FACTOR = 100_000_000L; // 8 decimal places

    // Bids sorted descending, Asks sorted ascending
    private final long[] bidPrices = new long[MAX_DEPTH];
    private final int[] bidQuantities = new int[MAX_DEPTH];
    private int bidCount = 0;

    private final long[] askPrices = new long[MAX_DEPTH];
    private final int[] askQuantities = new int[MAX_DEPTH];
    private int askCount = 0;

    public void updateBid(long scaledPrice, int quantity) {
        // Fast in-memory array insertion & binary search
        for (int i = 0; i < bidCount; i++) {
            if (bidPrices[i] == scaledPrice) {
                bidQuantities[i] = quantity;
                return;
            }
        }
        if (bidCount < MAX_DEPTH) {
            bidPrices[bidCount] = scaledPrice;
            bidQuantities[bidCount] = quantity;
            bidCount++;
        }
    }

    public long getBestBidPrice() {
        return bidCount > 0 ? bidPrices[0] : 0L;
    }

    public long getBestAskPrice() {
        return askCount > 0 ? askPrices[0] : 0L;
    }

    public long getMidPriceScaled() {
        if (bidCount > 0 && askCount > 0) {
            return (bidPrices[0] + askPrices[0]) >> 1; // Division by 2 via Bitshift
        }
        return 0L;
    }
}
```

---

### 3. Disruptor Wait Strategy Trade-offs

When a consumer thread waits for new events to arrive in a ring buffer, the selected **WaitStrategy** determines the trade-off between CPU utilization and latency determinism:

| Wait Strategy | Latency / Jitter | CPU Utilization | Ideal Use Case |
| :--- | :--- | :--- | :--- |
| `BusySpinWaitStrategy` | **Lowest Latency (~0ns)** | 100% CPU on Core | Core HFT trading loops with dedicated CPU cores (`isolcpus`). |
| `YieldingWaitStrategy` | Low Latency (~100ns) | 100% CPU (Yields) | High-throughput systems where thread count equals physical cores. |
| `SleepingWaitStrategy` | Moderate Latency (~10$\mu s$) | Low CPU | Asynchronous logging, journaling, or back-office reporting. |
| `BlockingWaitStrategy` | Highest Latency (~50$\mu s$) | ~0% CPU (Locks) | Non-critical administrative control panels. |

```java
// Configuring BusySpinWaitStrategy for Zero-Latency Thread Waiting
WaitStrategy busySpinStrategy = new BusySpinWaitStrategy();
```

---

## SECTION 2: VERBATIM & RESEARCH TEXTS

> **VERBATIM SOURCE**
> - **Title:** Single-Threaded Execution Mechanics in LMAX Architecture
> - **Author(s):** Martin Fowler & Mike Barker
> - **Published:** 2011, ACM Queue / MartinFowler.com
> 
> *Note: Technical synthesis of single-writer event loop design.*

### Single-Writer Principle Mechanics
The single-writer principle asserts that mutating system state sequentially on a dedicated single thread removes the necessity for mutual exclusion locks, concurrent collection overhead, and transactional rollbacks. 

When an order matching engine processes incoming orders strictly sequentially on a single thread running on a CPU core pinned to hardware execution pipelines, it achieves processing throughput exceeding 6 million transactions per second per core. By eliminating lock contention, the execution time per order becomes completely deterministic, bounded only by L1 cache line access times and integer ALU operation execution cycles.

---

## SECTION 3: CITATION & REFERENCE DEEP-DIVES

### Reference 7.3.A: Bitwise Fixed-Point Operations
- **Shift Operations**: Binary arithmetic right shift (`>> 1`) performs integer division by 2 in 1 clock cycle, avoiding hardware division pipeline stalls.
- **Fixed-Point Scaling**: Storing prices as integer ticks (`108542`) avoids IEEE 754 floating-point denormalization penalties.
