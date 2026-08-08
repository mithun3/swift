# Advanced HFT Patterns and Tuning

While the core architecture relies on memory-mapped queues and zero-allocation flyweights, the `fx-pipeline` implementation employs several advanced patterns to maintain strict microsecond latencies across error handling, logging, and observability.

## Hot Path Error Routing

In a typical enterprise application, exceptional states are handled by throwing JVM exceptions. In an HFT context, throwing an exception requires unwinding the stack and populating the stack trace, which is a very expensive operation that creates garbage and causes immediate latency spikes.

Instead, the pipeline uses **Zero-Allocation Error Routing**. 
Each service holds a reference to an `ErrorQueueWriter`. When a processing failure occurs (e.g., a validation failure), the service populates a pre-allocated `ErrorEvent` flyweight and writes it to a dedicated error Chronicle Queue. This allows the main event loop to continue processing subsequent events immediately without being interrupted by a JVM exception table lookup.

## Garbage-Free Asynchronous Logging

Standard logging frameworks (like logback or log4j) often allocate strings, varargs arrays, and wrapper objects in their hot path, which can trigger garbage collection.

To solve this, the pipeline uses an LMAX-style garbage-free `AsyncLogger`. 
- **Object Pooling:** A pool of `LogEvent` objects is pre-allocated.
- **Concurrent Queueing:** Log statements populate a pooled `LogEvent` and offer it to a `ManyToOneConcurrentArrayQueue` (provided by Agrona).
- **Background Processing:** A separate background thread (`LogProcessor`) reads from the queue, formats the text, and performs the actual I/O. This keeps all disk and string-formatting overhead completely off the critical path.

## Telemetry and Latency Recording (HdrHistogram)

Measuring latency accurately without perturbing the system requires specialized data structures. The pipeline uses **HdrHistogram** (`SingleWriterRecorder`) for concurrent, zero-allocation latency recording.

- The `TelemetryRecorder` allows the hot path thread to record nanosecond latency values using lock-free, allocation-free array updates.
- A background `TelemetryStitcher` thread periodically harvests the interval histograms and writes them to a `.hlog` file for later analysis. This separation ensures that metrics collection does not introduce jitter.

## Primitive Constants vs. Enums

Java `enum` values are object references. Using them for state machines (like event statuses) requires virtual dispatch (e.g., `enum.ordinal()`) or array lookups under the hood, and risks auto-boxing in certain scenarios.

The pipeline completely avoids `enum` for event states. Instead, `EventStatus` defines a registry of `static final int` constants (e.g., `RECEIVED = 0`, `ACCEPTED = 1`). 
- **CPU Registers:** Integers fit directly into CPU registers.
- **JIT Optimization:** The JIT compiler can emit highly optimal branch-table `switch` bytecodes.
- **Flyweight Storage:** They can be directly stored inside the `FxMarketEvent` flyweight as primitive fields, avoiding object graph pointer chasing.

## Chronicle Queue Tuning

The `QueueFactory` centrally configures Chronicle Queues with specific mechanical sympathy optimizations:
1. **WireType.BINARY_LIGHT:** Used because it is the most compact binary format. It strips out field name metadata, significantly reducing the bytes written per event and utilizing the fastest serialization path in Chronicle Wire.
2. **Block Size Tuning:** The block size is explicitly set to `64 MB`. This determines how much of the queue file is memory-mapped at one time. A larger block reduces the frequency of `mmap` remapping syscalls, which are a common source of latency jitter.
3. **Runtime Overrides:** The factory supports system property overrides (`fx.queue.<name>.path`), allowing ops teams to mount queues directly to fast NVMe drives or RAM disks (e.g., `/dev/shm`) in production without code changes.

## TCP FIX Ingestion

While synthetic data generation is used for benchmarking, the pipeline's gateway (`serv-0`) also supports a `TcpFixSource`. This handles real TCP socket ingestion, reading bytes directly from a socket channel into direct memory buffers before they are parsed by the `FixDecoder`—maintaining the zero-allocation constraint from the very edge of the network.
