# Common Module (`common`)

This module contains the foundational shared domain objects, handlers, and IPC configurations used across all microservices in the FX Pipeline.

## Key Components

### 1. `FxMarketEvent` (The Flyweight DTO)
The core data carrier of the pipeline. It is fully garbage-free and zero-allocation. 
- All fields are primitives (`long`, `byte`, `int`). 
- Strings (like currency pairs) are packed into 64-bit `long` fields using custom codecs (e.g., `CurrencyPairCodec`).
- Designed with cache-line sizes in mind (related properties grouped together to minimize cache misses).
- Implements `SelfDescribingMarshallable` for direct serialization to Chronicle Wire.
- Mutated in-place by calling `reset()` and repopulating instead of using the `new` keyword.

### 2. Event Loops (`AbstractEventLoop` & `EventLoopHandler`)
A standard scaffold for single-writer busy-spin event loops:
- Automatically pins the executing thread to an isolated CPU core using `net.openhft:affinity`.
- Abstracts Chronicle Queue reading (`ExcerptTailer`) and writing (`ExcerptAppender`).
- Handles graceful shutdown and lifecycle control.

### 3. Queue Definitions (`QueuePaths` & `QueueFactory`)
Defines the file paths and factory methods for instantiating the memory-mapped `.cq4` files used by Chronicle Queue (`queue-a`, `queue-b`, `queue-c`, `queue-err`). By default, these default to `/tmp/fx-queues/`.

## Flow Contribution
While `common` does not run as a standalone service, it enforces the LMAX principles (Zero-GC, Primitive Types, Fixed-Memory Footprint) that all other microservices depend on.
