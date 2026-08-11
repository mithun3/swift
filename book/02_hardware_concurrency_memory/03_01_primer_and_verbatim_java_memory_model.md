<div class="page-break"></div>

# Chapter 2.3: Synchronization & The Java Memory Model (Doug Lea & William Pugh)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. Why Multithreaded Memory Models Are Necessary
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

### 2. The Three Pillar Guarantees of a Memory Model

```
                    THE THREE MEMORY MODEL PILLARS
                    
┌───────────────────────┬───────────────────────┬───────────────────────┐
│       ATOMICITY       │       VISIBILITY      │       ORDERING        │
├───────────────────────┼───────────────────────┼───────────────────────┤
│ Which operations are  │ Under what conditions │ When operations appear│
│ indivisible (e.g. 32- │ field writes by one   │ in program order to   │
│ bit reads/writes vs   │ thread are guaranteed │ other thread          │
│ 64-bit word tearing). │ visible to another.   │ (Happens-Before).     │
└───────────────────────┴───────────────────────┴───────────────────────┘
```

### 3. Happens-Before Consistency & Memory Barriers
A **Memory Model** specifies a formal contract between programmers and language runtimes (JVM, C++11 runtime):
- **Locks & Synchronization**: Releasing a lock (`synchronized` block exit) forces a flush of all written variables from local working memory to main memory. Acquiring a lock forces a cache invalidation and reload from main memory.
- **Volatile Variables**: Writing to a `volatile` variable establishes a strict **Happens-Before** edge to subsequent reads of that same variable by any thread, suppressing instruction reordering via **Memory Barriers (Fences)**.

### 4. The Double-Checked Locking (DCL) Problem
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

## SECTION 2: VERBATIM RESEARCH PAPER

### Synchronization and the Java Memory Model (1996–1999)
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

> **VERBATIM SOURCE**
> - **Title:** Semantics of Multithreaded Java
> - **Author(s):** Jeremy Manson and William Pugh
> - **Published:** January 11, 2002
> - **Source type:** Academic paper
> - **Original URL:** https://www.cs.umd.edu/~pugh/java/memoryModel/semantics.pdf
> - **DOI:** 10.1145/1040305.1040336
> - **Repository:** N/A
> 
> *Note: The text below is reproduced verbatim — exact word-for-word —*
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


#### 2 Memory Models

Almost all of the work in the area of memory models has been done on processor memory models. Programming language memory models differ in some important ways.

First, most programming languages offer some safety guarantees. An example of this sort of guarantee is type safety. These guarantees must be absolute: there must not be a way for a programmer to circumvent them.

Second, the run-time environment for a high level language contains many hidden data structures and fields that are not directly visible to a programmer (for example, the pointer to a virtual method table). A data race resulting in the reading of an unexpected value for one of these hidden fields could be impossible to debug and lead to substantial violations of the semantics of the high level language.

Third, some processors have special instructions for performing synchronization and memory barriers. In a programming language, some variables have special properties (e.g., volatile or final), but there is usually no way to indicate that a particular write should have special memory semantics.

Finally, it is impossible to ignore the impact of compilers and the transformations they perform. Many standard compiler transformations violate the rules of existing processor memory models [Pug00b].

##### 2.1 Terms and Definitions

In this paper, we concern ourselves with the semantics of the Java virtual machine [LY99]. While defining a semantics for Java source programs is important, there are many issues that arise only in the JVM that also need to be resolved. Informally, the semantics of Java source programs is understood to be defined by their straightforward translation into classfiles, and then by interpreting the classfiles using the JVM semantics.

A variable refers to a static variable of a loaded class, a field of an allocated object, or element of an allocated array. The system must maintain the following properties with regards to variables and the memory manager:

- It must be impossible for any thread to see a variable before it has been initialized to the default value for the type of the variable.
- The fact that a garbage collection may relocate a variable to a new memory location is immaterial and invisible to the semantics.
- The fact that two variables may be stored in adjacent bytes (e.g., in a byte array) is immaterial. Two variables can be simultaneously updated by different threads without needing to use synchronization to account for the fact that they are "adjacent". Any word-tearing must be invisible to the programmer.

#### 3 Proposed Informal Semantics

The proposed informal semantics are very similar to lazy release consistency [CZ92, GLL +90]. A formal operational semantics is provided in Section 8. All Java objects act as monitors that support reentrant locks. For simplicity, we treat the monitor associated with each Java object as a separate variable. The only actions that can be performed on the monitor are Lock and Unlock actions. A Lock action by a thread blocks until the thread can obtain an exclusive lock on the monitor.

The actions on individual monitors and volatile fields are executed in a sequentially consistent manner (i.e., there must exist a single, global, total execution order over these actions that is consistent with the order in which the actions occur in their original threads). Actions on volatile fields are always immediately visible to other threads, and do not need to be guarded by synchronization.

If two threads access a normal variable, and one of those accesses is a write, then the program should be synchronized so that the first access is visible to the second access. When a thread T1 acquires a lock on/enters a monitor m that was previously held by another thread T2, all actions that were visible to T2 at the time it released the lock on m become visible to T1.

If thread T1 starts thread T2, then all actions visible to T1 at the time it starts T2 become visible to T2 before T2 starts. Similarly, if T1 joins with T2 (waits for T2 to terminate), then all accesses visible to T2 when T2 terminates are visible to T1 after the join completes.

When a thread T1 reads a volatile field v that was previously written by a thread T2, all actions that were visible to T2 at the time T2 wrote to v become visible to T1. This is a strengthening of volatile over the existing semantics. The existing semantics make it very difficult to use volatile fields to communicate between threads, because you cannot use a signal received via a read of a volatile field to guarantee that writes to non-volatile fields are visible. With this change, many broken synchronization idioms (e.g., double-checked locking [Pug00a]) can be fixed by declaring a single field volatile.

There are two reasons that a value written to a variable might not be available to be read after it becomes visible to a thread. First, another write to that variable in the same thread can overwrite the first value. Second, additional synchronization can provide a new value for the variable in the ways described above. Between the time the write becomes visible and the time the thread no longer can read that value from that variable, the write is said to be eligible to be read.

When programs are not properly synchronized, very surprising behaviors are allowed. There are additional rules associated with final fields (Section 5) and finalizers (Section 6).

#### 4 Safety guarantees

Java allows untrusted code to be executed in a sandbox with limited access rights. The set of actions allowed in a sandbox can be customized and depends upon interaction with a security manager, but the ability to execute code in this manner is essential. In a language that allows casts between pointers and integers, or in a language without garbage collection, any such guarantee is impossible. Even for code that is written by someone you trust not to act maliciously, safety guarantees are important: they limit the possible effects of an error. Safety guarantees need to be enforced regardless of whether a program contains a synchronization error or data race. In this section, we go over the implementation issues involved in enforcing certain virtual machine safety guarantees, and in the issues in writing libraries that promise higher level safety guarantees.

##### 4.1 VM Safety guarantees

Consider execution of the code on the left of Figure 1a on a multiprocessor with a weak memory model (all of the ri variables are intended to be registers that do not require memory references). Can this result in r2 = -1? For this to happen, the write to p must precede the read of p, and the read of *r1 must precede the write to y. It is easy to see how this could happen if the MemBar (Memory Barrier) instruction were not present. A MemBar instruction usually requires that actions that have been initiated are completed before any further actions can be taken.

If a compiler or the processor tries to reorder the statements in Thread 1 (leading to r2 = -1), then a MemBar would prevent that reordering. Given that the instructions in thread 1 cannot be reordered, you might think that the data dependence in thread 2 would prohibit seeing r2 = -1. You'd be wrong. The Alpha memory model allows the result r2 = -1. Existing implementations of the Alpha do not actually reorder the instructions. However, some Alpha processors can fulfill the r2 = *r1 instruction out of a stale cache line, which has the same effect. Future implementations may use value prediction to allow the instructions to be executed out of order.

Stronger memory orders, such as TSO (Total Store Order), PSO (Partial Store Order) and RMO (Relaxed Memory Order) would not allow this reordering. Sun's SPARC chip typically runs in TSO mode, and Sun's new MAJC chip implements RMO. Intel's IA-64 memory model does not allow r2 = -1; the IA-32 has no memory barrier instructions or formal memory model (the implementation changes from chip to chip), but many knowledgeable experts have claimed that no IA-32 implementation would allow the result r2 = -1 (assuming an appropriate ordering instruction was used instead of the memory barrier).

Now consider Figure 1b. This is very similar to Figure 1a, except that y is replaced by heap allocated memory for a new instance of Point. What happens if, when Thread 2 reads Foo.p, it sees the address written by Thread 1, but it doesn't see the writes performed by Thread 1 to initialize the instance? When thread 2 reads r2.x, it could see whatever was in that memory location before it was allocated from the heap. If that memory was uninitialized before allocation, an arbitrary value could be read. This would obviously be a violation of Java semantics. If r2.x were a reference/pointer, then seeing a garbage value would violate type safety and make any kind of security/safety guarantee impossible.

One solution to this problem is allocate objects out of memory that all threads know to have been zeroed (perhaps at GC time). This would mean that if we see an early/stale value for r2.x, we see a zero or null value. This is type safe, and happens to be the default value the field is initialized with before the constructor is executed.

Now consider Figure 1c. When thread 2 dispatches hashCode(), it needs to read the virtual method table of the object referenced by r2. If we use the idea suggested previously of allocating objects out of prezeroed memory, then the repercussions of seeing a stale value for the vptr are limited to a segmentation fault when attempting to load a method address out of the virtual method table. Other operations such as arraylength, instanceOf and checkCast could also load header fields and behave anomalously.

But consider what happens if the creation of the Bar object by Thread 1 is the very first time Bar has been referenced. This forces the loading and initialization of class Bar. Then not only might thread 2 see a stale value in the instance of Bar, it could also see a stale value in any of the data structures or code loaded for class Bar. What makes this particularly tricky is that thread 2 has no indication that it might be about to execute code of a class that has just been loaded.

##### 4.1.1 Proposed VM Safety Guarantees

Synchronization errors can only cause surprising or unexpected values to be returned from a read action (i.e., a read of a field or array element). Other actions, such as getting the length of an array, performing a checked cast or invoking a virtual method behave normally. They cannot throw any exceptions or errors because of a data race, cause the VM to crash or be corrupted, or behave in any other way not allowed by the semantics.

Values returned by read actions must be both typesafe and "not out of thin air". To say that a value must be "not out of thin air" means that it must be a value written previously to that variable by some thread. For example, Figure 9 must not be able to produce any result other than i == j == 0; for example, the value 42 cannot be assigned to i and j as if by "magic". The exception to this is that incorrectly synchronized reads of non-volatile longs and doubles are not required to respect the "not out of thin air" rule (see Section 8.8 for details).

**Figure 1: Surprising results from weak memory models**

**(a)**
```
Initially p=&x; x=1; y=-1;

Thread 1         Thread 2
y = 2            r1 = p
MemBar           r2 = *r1
p = &y
```
*Could result in r2 = -1*

**(b)**
```
Initially Foo.p = new Point(1,2);

Thread 1                     Thread 2
r1 = new Point(3,4)          r2 = Foo.p
MemBar                       r3 = r2.x
Foo.p = r1
```
*Could result in r3 = 0 or garbage*

**(c)**
```
Initially Foo.o = "Hello";

Thread 1                     Thread 2
r1 = new Bar(3,4)            r2 = Foo.o
MemBar                       r3 = r2.hashCode()
Foo.o = r1
```
*Could result in almost anything*

##### 4.2 Library Safety guarantees

Many programmers assume that immutable objects (objects that do not change once they are constructed) do not need to be synchronized. This is only true for programs that are otherwise correctly synchronized. However, if a reference to an immutable object is passed between threads without correct synchronization, then synchronization within the methods of the object is needed to ensure that the object actually appears to be immutable.

The motivating example is the `java.lang.String` class. This class is typically implemented using a length, offset, and reference to an array of characters. All of these are immutable (including the contents of the array), although in existing implementations are not declared final. The problem occurs if thread 1 creates a String object S, and then passes a reference to S to thread 2 without using synchronization. When thread 2 reads the fields of S, those reads are improperly synchronized and can see the default values for the fields of S. Later reads by thread 2 can then see the values set by thread 1.

As an example of how this can affect a program, it is possible to show that a String that is supposed to be immutable can appear to change from "/tmp" to "/usr". Consider an implementation of StringBuffer whose substring method creates a string using the StringBuffer's character array. It only creates a new array for the new String if the StringBuffer is changed. We create a String using `new StringBuffer("/usr/tmp").substring(4);`. This will produce a string with an offset field of 4 and a length of 4. If thread 2 incorrectly sees an offset with the default value of 0, it will think the string represents "/usr" rather than "/tmp".

This behavior can only occur on systems with weak memory models, such as an Alpha SMP. Under the existing semantics, the only way to prohibit this behavior is to make all of the methods and constructors of the String class synchronized. This solution would incur a substantial performance penalty. The impact of this is compounded by the fact that the synchronization is not necessary on all platforms, and even then is only required when the code contains a data race. If an object contains mutable data fields, then synchronization is required to protect the class against attack via data race. For objects with immutable data fields, we propose allowing the class to be defended by use of final fields.

#### 5 Guarantees for Final fields

Final fields must be assigned exactly once in the constructor for the class that defines them. The existing Java memory model contains no discussion of final fields. In fact, at each synchronization point, final fields need to be reloaded from memory just like normal fields. We propose additional semantics for final fields. These semantics will allow more aggressive optimizations of 

```java
class ReloadFinal extends Thread {
    final int x;
    ReloadFinal() {
        synchronized(this) {
            start();
            sleep(10);
            x = 42;
        }
    }
    public void run() {
        int i, j;
        i = x;
        synchronized(this) {
            j = x;
        }
        System.out.println(i + ", " + j); // j must be 42, even if i is 0
    }
}
```

**Figure 2: Final fields must be reloaded under existing semantics**

data races, defensive programming may require considering that a user of your code may deliberately introduce a data race, and that there is little or nothing you can do to prevent it.

##### 5.2 Final fields of objects that escape their constructors

Figure 2 shows an example of where the existing specification requires final fields to be reloaded. In this example, the object being constructed is made visible to another thread before the final field is assigned. That thread reads the final field, waits to be signaled that the constructor has assigned the final field, and then reads the final field again. The current specification guarantees that even if the first read of tmp1.x in foo sees 0, the second read will see 42.

The (informal) rule for final fields is that you must ensure that the constructor for a object has completed before another thread is allowed to load a reference to that object. These are called "properly constructed" final fields. We will deal with the semantics of properly constructed final fields first, and then come to the semantics of improperly constructed final fields.

##### 5.3 Informal semantics of final fields

The formal detailed semantics for final fields are given in Section 8.7. For now, we just describe the informal semantics of final fields that are constructed properly. The first part of the semantics of final fields is:

**F1** When a final field is read, the value read is the value assigned in the constructor.

Consider the scenario postulated at the bottom of Figure 3. The question is: which of the variables i1 - i7 are guaranteed to see the value 42? F1 alone guarantees that i1 is 42. However, that rule isn't sufficient to make Strings absolutely immutable. Strings contain a reference to an array of characters; the contents of that array must be seen to be immutable in order for the String to be immutable. Unfortunately, there is no way to declare the contents of an array as final in Java. Even if you could, it would mean that you couldn't reuse the mutable character buffer from a StringBuffer in constructing a String. To use final fields to make Strings immutable requires that when we read a final reference to an array, we see both the correct reference to the array and the correct contents of the array. Enforcing this should guarantee that i2 is 42.

For i3, the relevant question is: do the contents of the array need to be set before the final field is set (i.e, i3 might not be 42), or merely before the constructor completes (i3 must be 42)? Although this point is debatable, we believe that a requirement for objects to be completely initialized before they are assigned to final fields would often be ignored or incorrectly performed. Thus, we recommend that the semantics only require that such objects be initialized before the constructor completes.

Since i4 is very similar to i2, it should clearly be 42. What about i5? It is reading the same location as i4. However, simple compiler optimizations would simply reuse the value loaded for j as the value of i5. Similarly, a processor using the Sparc RMO memory model would only require a memory barrier at the end of the constructor to guarantee that i4 is 42. However, ensuring that i5 is 42 under RMO would require a memory barrier by the reading thread. For these reasons, we recommend that the semantics not require that i5 be 42.

All of the examples to this point have dealt with references to arrays. However, it would be very confusing if these semantics applied only to array elements and not to object fields. Thus, the semantics should require that i6 is 42. We need to decide if these special semantics apply only to the fields/elements of the object/array directly referenced, or if it applies to those referenced indirectly. If the semantics apply to indirectly referenced fields/elements, then i7 must be 42. We believe making the semantics apply only to directly referenced fields would be difficult to program correctly, so we recommend that i7 be required to be 42.

```java
class FinalTest {
    public static FinalTest ft;
    public static int[] x = new int[1];
    public final int a;
    public final int[] b, c, d;
    public final Point p;
    public final int[][] e;

    public FinalTest(int i) {
        a = i;
        int[] tmp = new int[1];
        tmp[0] = i;
        b = tmp;
        c = new int[1];
        c[0] = i;
        FinalTest.x[0] = i;
        d = FinalTest.x;
        p = new Point();
        p.x = i;
        e = new int[1][1];
        e[0][0] = i;
    }

    static void foo() {
        int[] myX = FinalTest.x;
        int j = myX[0];
        FinalTest f1 = ft;
        if (f1 == null) return;

        // Guaranteed to see value set in constructor?
        int i1 = f1.a;       // yes
        int i2 = f1.b[0];    // yes
        int i3 = f1.c[0];    // yes
        int i4 = f1.d[0];    // yes
        int i5 = myX[0];     // no
        int i6 = f1.p.x;     // yes
        int i7 = f1.e[0][0]; // yes

        // use j, i1 ... i7
    }
}

// Thread 1:
// FinalTest.ft = new FinalTest(42);

// Thread 2:
// FinalTest.foo();
```

**Figure 3: Subtle points of the revised semantics of final 6**

To formalize this idea, we say that a read r2 is derived from a read r1 if:
- r2 is a read of a field or element of an address that was returned by r1, or
- there exists a read r3 such that r3 is derived from r1 and r2 is derived from r3.

Thus, the additional semantics for final fields are:

**F2** Assume thread T1 assigns a value to a final field f of object X defined in class C. Assume that T1 does not allow any other thread to load a reference to X until after the C constructor for X has terminated. Thread T2 then reads field f of X. Any writes done by T1 before the class C constructor for object X terminates are guaranteed to be ordered before and visible to any reads done by T2 that are derived from the read of f.

##### 5.4 Improperly Constructed Final Fields

Conditions [F1] and [F2] suffice if the object which contains the final field is not made visible to another thread before its constructor ends. Additional semantics are needed to describe the behavior of a program that allows references to objects to escape their constructor.

The basic question of what should be read from a final field which is improperly constructed is a simple one. In order to maintain not-out-of-thin-air safety, it is necessary that the value read out of such a final field is either the default value for its type, or the value written to it in its constructor.

Figure 4 demonstrates some of the issues with improperly synchronized final fields. The variables proper and improper refer to the same object. proper points to the correctly constructed version of the object, because the reference was written to it after the constructor completed. improper is not guaranteed to point to the correctly constructed version of the object, because it was set before the object was fully constructed.

When thread 1 reads the improperly constructed reference into i, and tries to reference i.x through that reference, we cannot make the guarantee that the constructor has finished. The resulting value of i1 may be either a reference to the point or the default value for that field (which is null). If i1 is not null, and we then try to read i1.x, should we be forced to see the correctly constructed value of 42? After all, the write to improper occurred after the write of 42; one line of reasoning would suggest that if you can see the write to improper, you should be able to see the write to improper.x. This is not the case, however. The write to improper can be reordered to before the write to improper.x. Therefore, i2 can have either the value 42 or the value 0.

Because we have guaranteed that p will not be null, the reads from p should return the correctly constructed values for the fields. This is discussed in section 5.3. Now we come to i3 and i4. It is not unreasonable, initially, to believe that i3 and i4 should have the correct values in them. After all, we have just ensured that the thread has seen that object; it has been referenced through p. However, the compiler could reuse the values of i1 and i2 for i3 and i4 through common subexpression elimination. The values for i3 and i4 must therefore remain the same as those of i1 and i2.

##### 5.5 Final Static Fields

Final static fields must be initialized by the class initializer for the class in which they are defined. The semantics for class initialization guarantee that any thread that reads a static field sees all the results of the execution of the class initialization. Note that final static fields do not have to be reloaded at synchronization points.

Under certain complicated circumstances involving circularities in class initialization, it is possible for a thread to access the static variables of a class before the static initializer for that class has started. Under such situations, a thread which accesses a final static field before it has been set sees the default value for the field. This does not otherwise affect the nature or property of the field (any other threads that read the static field will see the final value set in the class initializer). No special semantics or memory barriers are required to observe this behavior; the standard memory barriers required for class initialization ensure it.

##### 5.6 Native code changing final fields

JNI allows native code to change final fields. To allow optimization (and sane understanding) of final fields, that ability will be prohibited. Attempting to use JNI to change a final field should throw an immediate exception.

```java
class Improper {
    public final Point p;
    public static Improper proper;
    public static Improper improper;

    public Improper(int i) {
        p = new Point();
        p.x = i;
        improper = this;
    }

    static void foo() {
        Improper p = proper;
        Improper i = improper;
        if (p == null) return;

        // Possible Results
        Improper i1 = i; // reference to point or null
        int i2 = i.x;    // 42 or 0
        Improper p1 = p; // reference to point
        int p2 = p.x;    // 42
        Improper i3 = i; // reference to point or null
        int i4 = i.x;    // 42 or 0
    }
}

// Thread 1:
// Improper.proper = new Improper(42);

// Thread 2:
// Improper.foo();
```

**Figure 4: Improperly Constructed Final Fields 8**

##### 5.6.1 Write Protected Fields

System.in, System.out, and System.err are final static fields that are changed by the methods System.setIn, System.setOut and System.setErr. This is done by having the methods call native code that modifies the final fields. We need to create a special rule to handle this situation.

These fields should have been accessed via getter methods (e.g., System.getIn()). However, it would be impossible to make that change now. If we simply made the fields non-final, then untrusted code could change the fields, which would also be a serious problem (functions such as System.setIn have to get permission from the security manager).

The (ugly) solution for this is to create a new kind of field, write protected, and declare these three fields (and only these fields) as write protected. They would be treated as normal variables, except that the JVM would reject any bytecode that attempts to modify them. In particular, they need to be reloaded at synchronization points.

#### 6 Guarantees for Finalizers

When an object is no longer reachable, the finalize() method (i.e., the finalizer) for the object may be invoked. The finalizer is typically run in a separate finalizer thread, although there may be more than one such thread. The loss of the last reference to an object acts as an asynchronous signal to another thread to invoke the finalizer.

In many cases, finalizers should be synchronized, because the finalizers of an unreachable but connected set of objects can be invoked simultaneously by different threads. However, in practice finalizers are often not synchronized. To naive users, it seems counter-intuitive to synchronize finalizers.

Why is it hard to make guarantees? Consider the code in Figure 5. If foo() is invoked, an object is created and then made unreachable. What is guaranteed about the reads in the finalizer? An aggressive compiler and garbage collector may realize that after the assignment to ft.y, all references to the object are dead and thus the object is unreachable. If garbage collection and finalization were performed immediately, the write to FinalizerTest.z would not have been performed and would not be visible. But if the compiler reorders the assignments to FinalizerTest.x and ft.y, the same would hold for FinalizerTest.x. However, the object referenced

```java
class FinalizerTest {
    static int x = 0;
    int y = 0;
    static int z = 0;

    protected void finalize() {
        int i = FinalizerTest.x;
        int j = y;
        int k = FinalizerTest.z;
        // use i, j and k
    }

    public static void foo() {
        FinalizerTest ft = new FinalizerTest();
        FinalizerTest.x = 1;
        ft.y = 1;
        FinalizerTest.z = 1;
        ft = null;
    }
}
```

**Figure 5: Subtle issues involving finalization**

by ft is clearly reachable at least until the assignment to ft.y is performed. So the guarantee that can be reasonably made is that all memory accesses to the fields of an object X during normal execution are ordered before all memory accesses to the fields of X performed during the invocation of the finalizer for X. Furthermore, all memory accesses visible to the constructing thread at the time it completes the construction of X are visible to the finalizer for X.

For a uniprocessor garbage collector, or a multiprocessor garbage collector that performs a global memory barrier (a memory barrier on all processors) as part of garbage collection, this guarantee should be free. For a garbage collector that doesn't "stop the world", things are a little trickier. When an object with a finalizer becomes unreachable, it must be put into special queue of unreachable objects. The next time a global memory barrier is performed, all of the objects in the unreachable queue get moved to a finalizable queue, and it now becomes safe to run their finalizer. There are a number of situations that will cause global memory barriers (such as class initialization), and they can also be performed periodically or when the queue of unreachable objects grows too large.

```java
// Thread 1:
while (true) {
    synchronized (o) {
        // does not call Thread.yield() or Thread.sleep()
    }
}

// Thread 2:
synchronized (o) {
    // does nothing.
}
```

**Figure 6: Fairness**

#### 7 Fairness Guarantees

Without a fairness guarantee for virtual machines, it is possible for a running thread to be capable of making progress and never do so. Java currently has no official fairness guarantee, although, in practice, most JVMs do provide it to some extent. An example of a potential weak fairness guarantee would be one that states that if a thread is infinitely often allowed to make progress, it would eventually do so.

An example of how this issue can impact a program can be seen in Figure 6. Without a fairness guarantee, it is perfectly legal for a compiler to move the while loop inside the synchronized block; Thread 2 will be blocked forever. Any potential fairness guarantee would be inextricably linked to the threading model for a given virtual machine.

A threading model that only switches threads when Thread.yield() is called will never allow Thread 2 to execute. A fairness guarantee would make this sort of implementation, which is used in a number of JVMs, illegal; it would force Thread 2 to be scheduled. Because this kind of implementation is often desirable, our proposed specification does not include a fairness guarantee. The flip side of this issue is the fact that library calls like Thread.yield() and Thread.sleep() are given no meaningful semantics by the Java API. The question of whether they should have one is outside the scope of this discussion, which centers on VM issues, not API changes.
