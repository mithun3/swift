# Chapter 3.7: Citation & Reference Deep-Dives for Module 3

This chapter provides standalone research profiles, mathematical formalisms, hardware memory fence mechanics, and lock-free data structure implementations for all major citations across Module 3.

---

## Deep-Dive 3.5.1: Carl Hewitt’s Original Actor Formalism (1973 vs 1985)

```
1973: Carl Hewitt, Peter Bishop, Richard Steiger (IJCAI '73)
  │   - Introduced universal modular actor formalism
  │   - Focused on AI knowledge representation and control structures
  │
  ├── 1977: Henry Baker & Carl Hewitt (LISP Conference)
  │   - Formalized Actor semantics in terms of Laws for Communicating Parallel Processes
  │
  └── 1985: Gul Agha (MIT PhD Thesis AITR-844)
      - Rigorous mathematical operational semantics for distributed actor systems
      - Minimal functional actor primitives (send, create, become)
```

### Theoretical Distinction: Hewitt vs. Agha
- **Hewitt & Bishop (1973)**: Conceived actors as generalized active software entities in Artificial Intelligence. Everything was an actor (numbers, functions, stack frames, environments). Communication was message-passing, but focused heavily on pattern matching and control structures.
- **Agha (1985)**: Stripped the actor model down to its pure concurrent computational essentials:
  1. **Actors have Mail Addresses** (uniquely identifying target locations).
  2. **Asynchronous Non-Blocking Send** (Sender never blocks, messages are buffered in mail queues).
  3. **Behavior Replacement (`become`)**: State mutation is modeled by an actor designating its replacement behavior for the next incoming message, maintaining pure mathematical functional state per message transition:
$$\text{Actor}(State_k) \xrightarrow{\text{Message}_m} \text{Actor}(State_{k+1}) + \text{NewActors} + \text{SentMessages}$$

---

## Deep-Dive 3.5.2: Lock-Free Memory Barriers & Cache-Line Padding

### The False Sharing Problem
In multi-core CPU architectures, memory is transferred between L3 cache and CPU L1/L2 caches in fixed **64-byte Cache Lines**.

When two threads executing on separate CPU cores write to independent variables that happen to reside on the same 64-byte cache line:

```
                  THE FALSE SHARING CACHE INVALIDATION CYCLE

   Core 1 (Thread A)                                 Core 2 (Thread B)
┌───────────────────────┐                         ┌───────────────────────┐
│ Writes to Variable A  │                         │ Writes to Variable B  │
└───────────┬───────────┘                         └───────────┬───────────┘
            │                                                 │
            ▼                                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 Shared 64-Byte Cache Line [A | B]                        │
├─────────────────────────────────────────────────────────────────────────┤
│ Core 1 invalidates Core 2's L1 cache line --> Core 2 re-fetches from L3 │
│ Core 2 invalidates Core 1's L1 cache line --> Core 1 re-fetches from L3 │
└─────────────────────────────────────────────────────────────────────────┘
```

Result: Massive bus-lock ping-ponging across CPU cores, degrading performance by 10x–100x.

#### Cache-Line Padding Implementation (Java & C++)

##### Java 8+ Implementation (`@Contended`)
```java
// Preventing False Sharing in Java
public class SequencePadded {
    // 56 bytes of padding (7 longs * 8 bytes) + 8 bytes value = 64 bytes
    public volatile long p1, p2, p3, p4, p5, p6, p7;
    public volatile long value = 0L;
    public volatile long p8, p9, p10, p11, p12, p13, p14;
}
```

##### C++20 Implementation (`alignas`)
```cpp
#include <atomic>
#include <new>

struct alignas(hardware_destructive_interference_size) PaddedAtomicSequence {
    std::atomic<int64_t> sequence{0};
};
```

---

## Deep-Dive 3.5.3: Memory Fences (LoadLoad, StoreStore, LoadStore, StoreLoad)

Hardware memory reordering forces low-latency lock-free data structures (like the LMAX Disruptor) to explicitly emit **Memory Barriers (Fences)**:

| Barrier Type | Description & Instruction |
| :--- | :--- |
| **LoadLoad** | Ensures all loads preceding the barrier complete before any subsequent loads execute. |
| **StoreStore** | Ensures all stores preceding the barrier are flushed to main memory/cache before subsequent stores execute. |
| **LoadStore** | Ensures all loads preceding the barrier complete before subsequent stores execute. |
| **StoreLoad** | Heavy hardware barrier (`mfence` on x86). Guarantees all preceding stores are visible to all processors before any subsequent loads execute. |

---

## Deep-Dive 3.5.4: Summary of Cited Works for Module 3

[12] G. A. Agha, "AITR-844 Actors Thesis," MIT, 1985. Available: https://dspace.mit.edu/handle/1721.1/6952
[13] M. Fowler, "The LMAX Architecture," MartinFowler.com, 2011. Available: https://martinfowler.com/articles/lmax.html
[14] M. Thompson et al., "Disruptor-1.0 Technical Paper," LMAX, 2011. Available: https://lmax-exchange.github.io/disruptor/files/Disruptor-1.0.pdf
[15] S. Warren, "A Question of Scale," LMAX Blog, 2023.
[16] Povoliashko et al., "First Impressions of Testing at LMAX," LMAX Blog, 2023.
[17] J. Byatt, "Why I Don't Do Work in Constructors," LMAX Blog, 2024.
[18] J. Byatt, "Coverage Can Only Show You What to Delete," LMAX Blog, 2023.
[19] LMAX Blog, "The Impossible NullPointerException," 2022. Available: https://www.lmax.com/blog/staff-blogs/2022/06/15/the-impossible-nullpointerexception/

**Supplementary Readings**
[S9] C. Hewitt, P. Bishop, and R. Steiger, "A Universal Modular ACTOR Formalism for Artificial Intelligence," IJCAI'73, 1973.
[S10] M. Barker, "Bad Concurrency: Flow Control in Aeron & I Heard a Rumour," bad-concurrency.blogspot.com, 2020.

**Subject Index Cross-References:**
- Actor Model ......... Ch 3.1, Ch 3.4
- Cache Line Padding .. Ch 2.4, Ch 3.2, Ch 3.4
- CAS (Compare-And-Swap) ........ Ch 3.2, Ch 3.4, Ch 2.4
- Disruptor ........... Ch 3.2, Ch 3.4
- Event Sourcing ...... Ch 3.2
- False Sharing ....... Ch 2.4, Ch 3.2, Ch 3.4
- LMAX Disruptor ...... Ch 3.2, Ch 3.4
- Mechanical Sympathy . Ch 3.2, Ch 3.4
- Memory Barriers ..... Ch 2.3, Ch 2.4, Ch 3.4
- Ring Buffer ......... Ch 3.2, Ch 3.4
- Single-Writer Principle ......... Ch 3.2, Ch 3.4
- TDD ................. Ch 5.1, Ch 3.3
- Volatile ............ Ch 2.3, Ch 2.4, Ch 3.4
