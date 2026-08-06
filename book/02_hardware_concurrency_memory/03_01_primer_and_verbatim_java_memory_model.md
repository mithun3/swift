<div class="page-break"></div>

## Chapter 2.3: Synchronization & The Java Memory Model (Doug Lea & William Pugh)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Why Multithreaded Memory Models Are Necessary
In a single-threaded execution context, compilers and CPU hardware aggressively reorder instructions and cache variable values in registers to maximize performance. As long as execution obeys **as-if-serial semantics** (the program produces the exact same results as if executed line-by-line in source order), these optimizations are completely invisible and safe.

In a multithreaded environment with shared memory, however, an optimizing compiler or CPU out-of-order pipeline can break code correctness in counter-intuitive ways:

```
               UNSYNCHRONIZED THREAD INTERACTION ANOMALY
               
   Thread 1 (Writer)                       Thread 2 (Reader)
   ------------------                      ------------------
   a = 1;                                  if (b == -1) {
   b = -1;                                     print(a); // Might print 0!
                                           }
```

Without a formal **Memory Model**, Thread 2 might observe `b == -1` while still reading `a == 0` due to:
1. Compiler statement reordering.
2. CPU instruction reordering.
3. CPU L1/L2 cache flushes occurring asynchronously.
4. Word tearing on 64-bit primitives (`long` and `double`).

#### 2. The Three Pillar Guarantees of a Memory Model

```
                    THE THREE MEMORY MODEL PILLARS
                    
┌───────────────────────┬───────────────────────┬───────────────────────┐
│       ATOMICIY        │       VISIBILITY      │       ORDERING        │
├───────────────────────┼───────────────────────┼───────────────────────┤
│ Which operations are  │ Under what conditions │ When operations appear│
│ indivisible (e.g. 32- │ field writes by one   │ in program order to   │
│ bit reads/writes vs   │ thread are guaranteed │ other thread          │
│ 64-bit word tearing). │ visible to another.   │ (Happens-Before).     │
└───────────────────────┴───────────────────────┴───────────────────────┘
```

#### 3. Happens-Before Consistency & Memory Barriers
A **Memory Model** specifies a formal contract between programmers and language runtimes (JVM, C++11 runtime):
- **Locks & Synchronization**: Releasing a lock (`synchronized` block exit) forces a flush of all written variables from local working memory to main memory. Acquiring a lock forces a cache invalidation and reload from main memory.
- **Volatile Variables**: Writing to a `volatile` variable establishes a strict **Happens-Before** edge to subsequent reads of that same variable by any thread, suppressing instruction reordering via **Memory Barriers (Fences)**.

#### 4. The Double-Checked Locking (DCL) Problem
A classic example of memory model failure is the Double-Checked Locking singleton pattern. In Java pre-1.5, developers tried to avoid the overhead of `synchronized` on every access by checking if the instance was null, then synchronizing, and checking again:

```java
// BROKEN Double-Checked Locking (Java pre-1.5)
public class Singleton {
    private static Singleton instance;
    public static Singleton getInstance() {
        if (instance == null) { // First check (no lock)
            synchronized(Singleton.class) {
                if (instance == null) { // Second check (with lock)
                    instance = new Singleton(); // VULNERABILITY HERE
                }
            }
        }
        return instance;
    }
}
```
**Why it fails:** The line `instance = new Singleton()` is not atomic. It involves: (1) allocate memory, (2) run constructor, (3) assign memory reference to `instance`. The JVM or CPU can reorder (2) and (3). Thus, thread A could assign the reference before the constructor finishes. Thread B sees `instance != null` (first check), returns it, and accesses uninitialized fields!

The fix in Java 5+ (JSR-133) relies on the strengthened `volatile` keyword, which guarantees a Happens-Before edge:
```java
// FIXED Double-Checked Locking (Java 5+)
public class Singleton {
    private static volatile Singleton instance; // volatile prevents reordering
    public static Singleton getInstance() {
        Singleton localRef = instance;
        if (localRef == null) {
            synchronized(Singleton.class) {
                localRef = instance;
                if (localRef == null) {
                    instance = localRef = new Singleton();
                }
            }
        }
        return localRef;
    }
}
```

#### Python Implementation
Python relies on the Global Interpreter Lock (GIL), but still requires explicit locking to prevent race conditions during initialization:
```python
# Python — Thread-safe Singleton
import threading

class Singleton:
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None: # First check
            with cls._lock:       # Acquire lock
                if cls._instance is None: # Second check
                    cls._instance = super(Singleton, cls).__new__(cls)
        return cls._instance
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

#### Synchronization and the Java Memory Model (1996–1999)
*By Doug Lea (Excerpts from Concurrent Programming in Java: Design Principles and Patterns)*

Consider the tiny class, defined without any synchronization:

```java
final class SetCheck {
    private int a = 0;
    private long b = 0;

    void set() {
        a = 1;
        b = -1;
    }

    boolean check() {
        return ((b == 0) || (b == -1 && a == 1)); 
    }
}
```

In a purely sequential language, the method `check` could never return `false`. This holds even though compilers, run-time systems, and hardware might process this code in a way that you might not intuitively expect. For example, any of the following might apply to the execution of method `set`:

- The compiler may rearrange the order of the statements, so `b` may be assigned before `a`. If the method is inlined, the compiler may further rearrange the orders with respect to yet other statements.
- The processor may rearrange the execution order of machine instructions corresponding to the statements, or even execute them at the same time.
- The memory system (as governed by cache control units) may rearrange the order in which writes are committed to memory cells corresponding to the variables. These writes may overlap with other computations and memory actions.
- The compiler, processor, and/or memory system may interleave the machine-level effects of the two statements. For example on a 32-bit machine, the high-order word of `b` may be written first, followed by the write to `a`, followed by the write to the low-order word of `b`.
- The compiler, processor, and/or memory system may cause the memory cells representing the variables not to be updated until sometime after (if ever) a subsequent check is called, but instead to maintain the corresponding values (for example in CPU registers) in such a way that the code still has the intended effect.

In a sequential language, none of this can matter so long as program execution obeys as-if-serial semantics. Sequential programs cannot depend on the internal processing details of statements within simple code blocks, so they are free to be manipulated in all these ways. This provides essential flexibility for compilers and machines. Exploitation of such opportunities (via pipelined superscalar CPUs, multilevel caches, load/store balancing, interprocedural register allocation, and so on) is responsible for a significant amount of the massive improvements in execution speed seen in computing over the past decade. The as-if-serial property of these manipulations shields sequential programmers from needing to know if or how they take place. Programmers who never create their own threads are almost never impacted by these issues.

Things are different in concurrent programming. Here, it is entirely possible for `check` to be called in one thread while `set` is being executed in another, in which case the check might be "spying" on the optimized execution of `set`. And if any of the above manipulations occur, it is possible for `check` to return `false`. For example, `check` could read a value for the long `b` that is neither `0` nor `-1`, but instead a half-written in-between value. Also, out-of-order execution of the statements in `set` may cause `check` to read `b` as `-1` but then read `a` as still `0`.

In other words, not only may concurrent executions be interleaved, but they may also be reordered and otherwise manipulated in an optimized form that bears little resemblance to their source code. As compiler and run-time technology matures and multiprocessors become more prevalent, such phenomena become more common. They can lead to surprising results for programmers with backgrounds in sequential programming who have never been exposed to the underlying execution properties of allegedly sequential code. This can be the source of subtle concurrent programming errors.

In almost all cases, there is an obvious, simple way to avoid contemplation of all the complexities arising in concurrent programs due to optimized execution mechanics: Use synchronization. For example, if both methods in class `SetCheck` are declared as `synchronized`, then you can be sure that no internal processing details can affect the intended outcome of this code.

#### The Three Key Issues

##### Atomicity
Accesses and updates to the memory cells corresponding to fields of any type except `long` or `double` are guaranteed to be atomic. This includes fields serving as references to other objects. Additionally, atomicity extends to `volatile long` and `double`. (Even though non-volatile longs and doubles are not guaranteed atomic, they are of course allowed to be.)

Atomicity guarantees ensure that when a non-long/double field is used in an expression, you will obtain either its initial value or some value that was written by some thread, but not some jumble of bits resulting from two or more threads both trying to write values at the same time. However, atomicity alone does not guarantee that you will get the value most recently written by any thread.

##### Visibility
Changes to fields made by one thread are guaranteed to be visible to other threads only under the following conditions:

- A writing thread releases a synchronization lock and a reading thread subsequently acquires that same synchronization lock.
- In essence, releasing a lock forces a flush of all writes from working memory employed by the thread, and acquiring a lock forces a (re)load of the values of accessible fields.
- If a field is declared as `volatile`, any value written to it is flushed and made visible by the writer thread before the writer thread performs any further memory operation. Reader threads must reload the values of volatile fields upon each access.

##### Ordering
Ordering rules fall under two cases: within-thread and between-thread:
- From the point of view of the thread performing the actions in a method, instructions proceed in the normal as-if-serial manner.
- From the point of view of other threads that might be "spying" on this thread by concurrently running unsynchronized methods, almost anything can happen.

---

> **📄 VERBATIM SOURCE**
> **Title:** Semantics of Multithreaded Java
> **Author(s):** Jeremy Manson and William Pugh
> **Published:** January 11, 2002
> **Source type:** Academic paper
> **Original URL:** https://www.cs.umd.edu/~pugh/java/memoryModel/semantics.pdf
> **DOI:** 10.1145/1040305.1040336
> **Repository:** N/A
> **Note:** The text below is reproduced verbatim — exact word-for-word —
> for educational study. All rights remain with the original author(s)
> and publisher(s).

#### Abstract
Java has integrated multithreading to a far greater extent than most programming languages. It is also one of the only languages that specifies and requires safety guarantees for improperly synchronized programs. It turns out that understanding these issues is far more subtle and difficult than was previously thought. The existing specification makes guarantees that prohibit standard and proposed compiler optimizations; it also omits guarantees that are necessary for safe execution of much existing code. Some guarantees that are made (e.g., type safety) raise tricky implementation issues when running unsynchronized code on SMPs with weak memory models.

This paper reviews those issues. It proposes a new semantics for Java that allows for aggressive compiler optimization and addresses the safety and multithreading issues.

#### 1 Introduction
Java has integrated multithreading to a far greater extent than most programming languages. One desired goal of Java is to be able to execute untrusted programs safely. To do this, we need to make safety guarantees for unsynchronized as well as synchronized programs. Even potentially malicious programs must have safety guarantees.

Pugh showed that the existing specification of the semantics of Java's memory model has serious problems. However, the solutions proposed in the first paper were naıve and incomplete. The issue is far more subtle than anyone had anticipated.

Many of the issues raised in this paper have been discussed on a mailing list dedicated to the Java Memory Model. There is a rough consensus on the solutions to these issues, and the answers proposed here are similar to those proposed in another paper (by other authors) that arose out of those discussions. However, the details and the way in which those solutions are formalized are different.

---


