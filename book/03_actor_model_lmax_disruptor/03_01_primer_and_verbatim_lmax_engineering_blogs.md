<div class="page-break"></div>

# Chapter 3.3: LMAX Technology Blog Lessons — Scale, Testing & Code Hygiene

---

## SECTION 1: PRIMER ON THE BASICS

### 1. Real-World Engineering Practices at LMAX Exchange
While architectural patterns (such as Event Sourcing and the Disruptor) provide high-throughput theoretical frameworks, maintaining an ultra-low-latency financial platform requires rigorous day-to-day engineering hygiene and testing discipline.

The LMAX Technology Blog series explores four core operational and software engineering lessons:

```
               THE LMAX PRACTICAL ENGINEERING HYGIENE
               
  ┌─────────────────────────────────────────────────────────────┐
  │ 1. Numerical Scale & Precision (A Question of Scale)        │
  │    - BigDecimal scale anomalies & precision pitfall         │
  ├─────────────────────────────────────────────────────────────┤
  │ 2. Pair Testing & Test-First Culture                        │
  │    - Developers & testers pairing daily                     │
  ├─────────────────────────────────────────────────────────────┤
  │ 3. Side-Effect Free Constructors                            │
  │    - Constructors perform field assignment ONLY             │
  │    - Zero I/O, Zero allocation in constructor bodies        │
  ├─────────────────────────────────────────────────────────────┤
  │ 4. Code Coverage Realities                                  │
  │    - Coverage doesn't prove correctness; it finds dead code │
  └─────────────────────────────────────────────────────────────┘
```

### 2. Numerical Scale & Precision Anomalies in Managed Runtimes
In high-frequency financial platforms, numbers representing contract sizes and prices must avoid floating-point rounding errors (such as `0.1 + 0.2 = 0.30000000000000004`). Languages like Java provide `BigDecimal` for exact arbitrary-precision arithmetic.

However, `BigDecimal` carries subtle traps:
- `10` and `10.0` have different **scales** (`0` vs `1`).
- `.equals()` returns `false` when comparing `new BigDecimal("10")` and `new BigDecimal("10.0")` because scale is part of object equality!
- Operations like `.stripTrailingZeros()` can result in **negative scales** (e.g., `100` stripped becomes `1E+2` with scale `-2`), causing unexpected results when combined with division and rounding modes.

### 3. Side-Effect Free Constructors & Testability
Constructors that open files, perform network I/O, or instantiate complex dependent objects make unit testing nearly impossible. 

By restricting constructors to **assignment and nothing else**, classes become instantly testable with lightweight mocks or in-memory streams, enforcing clean Dependency Injection.

---

<div class="page-break"></div>

## SECTION 2: VERBATIM RESEARCH PAPERS

<div class="scholarly-text">

### Article 1: A Question of Scale (May 2023)

> **VERBATIM SOURCE**
> - **Title:** A Question of Scale
> - **Author(s):** Simon Warren
> - **Published:** May 2023, LMAX Technology Blog
> - **Source type:** Engineering Blog
> - **Original URL:** https://technology.lmax.com/posts/a-question-of-scale/
> 
> *Note: The following text is reproduced verbatim — exact word-for-word.*

During testing, it was noticed that occasionally an order placed over Broker FIX received no acknowledgement. This was strange, not least because we hadn’t actually made any changes to the message flow around the system.

We traced the message through until we reached a block of code that looked something like this:

```java
class OrderManager {
    public void implicitClosePosition(final PlaceOrderInstruction instruction) {
        final @UnitQty long availableToClose = calculateQuantityToCloseOnInstrumentPosition(instruction);
        if (availableToClose != 0) {
            final OrderContext orderContext = this.orderContextFactory.from(instruction);
            orderContext.applyInflightClose(availableToClose);
            orderContext.onPlaceInstruction(instruction);
        }
    }
}
```

It turned out that `availableToClose` was in fact `0`, despite the order quantity being `10`.

Deep in the utility methods, we found:

```java
public static BigDecimal roundToMultipleOf(final BigDecimal value, final BigDecimal increment) {
    BigDecimal multiples = value.divide(increment, RoundingMode.FLOOR);
    multiples = multiples.setScale(0, RoundingMode.FLOOR);
    return multiples.multiply(increment).setScale(increment.scale());
}
```

When dividing `10` by `10` where the `increment` had a negative scale of `-1` (produced by `.stripTrailingZeros()`), `10 / 10` resulted in `0` when rounded to the nearest 10 with `RoundingMode.FLOOR`!

The fix was updating the order book configuration so that `contract size` and `quantity increment` maintained a scale of $\ge 0$.

---

#### Article 2: First Impressions of Testing at LMAX (December 2023)

> **VERBATIM SOURCE**
> - **Title:** First Impressions of Testing at LMAX
> - **Author(s):** Yuliia Povoliashko, Hans Sharda, and Stewart Atkinson
> - **Published:** December 2023, LMAX Technology Blog
> - **Source type:** Engineering Blog
> - **Original URL:** https://technology.lmax.com/posts/first-impressions-of-testing-at-lmax/
> 
> *Note: The following text is reproduced verbatim — exact word-for-word.*

One aspect that stood out to me is the company’s pair programming approach. Tester-developer pairing is a common practice at LMAX, with testers contributing to code and developers writing tests. This approach fosters excellent knowledge sharing and collaboration.

Another unique aspect of working at LMAX is the practice of writing tests first, followed by the code. The result of this approach has compounded over the years: leaving a set of tests that act both as a safety net allowing for fast agile development, and as documentation for how every aspect of the exchange works.

---

#### Article 3: Why I Don't Do Work in Constructors (September 2024)

> **VERBATIM SOURCE**
> - **Title:** Why I Don't Do Work in Constructors
> - **Author(s):** James Byatt
> - **Published:** September 2024, LMAX Technology Blog
> - **Source type:** Engineering Blog
> - **Original URL:** https://technology.lmax.com/posts/why-i-dont-do-work-in-constructors/
> 
> *Note: The following text is reproduced verbatim — exact word-for-word.*

In Java, the only thing I’ll do in a constructor is assign a parameter to a field. Sounds strict? Let me explain.

The canonical anti-pattern:

```java
// ANTI-PATTERN
class Thingy {
    private final FileInputStream inputStream;
    public Thingy() throws FileNotFoundException {
        this.inputStream = new FileInputStream("/tmp/my-very-special-file");
    }
}
```

It does a few things I don’t like:
- It opens a `FileInputStream` in the constructor.
- It hard codes where that `FileInputStream` comes from.
- The field declaration is for a `FileInputStream` rather than an `InputStream`.

Ideally that class would look like this:

```java
// CLEAN DESIGN
final class Thingy implements AutoCloseable {
    private final InputStream inputStream;
    
    Thingy(InputStream inputStream) {
        this.inputStream = inputStream; // Assignment ONLY!
    }

    public static Thingy openThingy() throws FileNotFoundException {
        return new Thingy(new FileInputStream("/tmp/my-very-special-file"));
    }
}
```

---

#### Article 4: Coverage Can Only Show You What to Delete (May 2023)

> **VERBATIM SOURCE**
> - **Title:** Coverage Can Only Show You What to Delete
> - **Author(s):** James Byatt
> - **Published:** May 2023, LMAX Technology Blog
> - **Source type:** Engineering Blog
> - **Original URL:** https://technology.lmax.com/posts/coverage-can-only-show-you-what-to-delete/
> 
> *Note: The following text is reproduced verbatim — exact word-for-word.*

The best thing code coverage can tell you is that code is unused, and should therefore be deleted.

```java
public static void doThing(String input) {
    if (input.length() > 3) {
        doThingOne(input);
    }
    if (input.length() < 7) {
        doThingTwo(input);
    }
}
```

I can cover this function with two tests ("bananarama" and "oo") and trivially achieve 100% coverage. That’s not great, because there is a code path we haven’t covered: "moose" - which fires *both* `if` branches. This brings me down solidly in the "coverage is necessary, but not sufficient" crowd.

When using TDD we’re only supposed to write code in response to a failing test. If we haven’t got 100% coverage, we broke the rules, and wrote code the test didn’t need. What should we do in this case? **Delete the uncovered code!** It doesn’t have an excuse to exist.

---

<div class="page-break"></div>

#### Article 5: The Impossible NullPointerException (June 2022)
*By James Byatt (LMAX Technology Blog)*

> **VERBATIM SOURCE**
> - **Title:** The Impossible NullPointerException
> - **Author(s):** James Byatt
> - **Published:** June 2022, LMAX Technology Blog
> - **Source type:** Engineering Blog
> - **Original URL:** https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
> 
> *Note: The following text is reproduced verbatim — exact word-for-word.*

Our new production exchange recently produced an impossible looking `NullPointerException`. At the same time, we saw another application in the same deployment throw an `OutOfMemoryError`. Both problems turned out to have the same root cause. This post tells the story of how we found that out.

### The Problem

Or problems, plural, in this case. We’ve added a new exchange. To bring it into service, it needs to join an upstream ‘global’ service that maintains data that’s shared across multiple exchanges. To do so, that global service needs to send our new exchange quite a lot of data. One of the larger datasets that we need to synchronise is `customer`.

Our ﬁrst attempt to do this did not go well. Here’s the ﬁrst few elements of a stack trace that we thought was, well, exceptional.

```java
Caused by: java.lang.NullPointerException
       at CustomerDao.upsert(CustomerDao.java:345)
       at CustomerManager.upsertCustomer(CustomerManager.java:123)
       at CustomerManager.upsert(CustomerManager.java:100)
```

Why is that so impossible? We’ll need a crash course in how our messaging system works to see why. First though - let’s have a look at that line, and work our way up…

Here’s `CustomerDao.java:345`:

```java
customerParams.addValue("principalRealm", changedBy.getRealm());
```

From which we guess that `changedBy` is null. That particular object has type `Identity` and gets passed down from `CustomerManager::upsert`.

```java
public void upsert(
       final String requestId,
       final Customer customer,
       final Identity changedBy,
       final @Milliseconds long changeTimestamp,
       final long bookingCustomerId) {
```

Now. How did this function call get here? Well, it was proxied over from another (remote) service via multicast. `CustomerManager` implements a messaging interface that contains that method signature. With a bit of magic, for a topic like that, we generate an implementation that transforms the method call into a multicast packet, and another piece of code that can ‘invoke’ that packet on an instance of that interface in another application entirely.

This is very handy, because we can see who calls that method on that interface via IntelliJ’s “ﬁnd usages”, and quickly ﬁnd the culprit who has passed in a null `Identity`. There are six callers, but we know that only one of them is called by our sync process, so we trace where the `Identity` instance is constructed by that caller, and suddenly, we’re very confused, because in the database reading code, we ﬁnd this:

```java
return new Identity(
       getByAlphaChar(rs.getString("principal_realm")),
       rs.getString("principal_domain"), 
       rs.getString("principal_username"));
```

We double and triple check our work, but no. The caller’s got no way of passing null in. And yet… `NullPointerException`. What have we missed? Well, we guess there must be something wrong in the magic that marshals those objects across the network. Unfortunately, that magic, in this case, is a slightly larger area than usual…

### The other problem

We haven’t been entirely honest. The vast majority of the messaging at LMAX happens as described - a method call transported across a multicast bus between two applications. In this particular case though, the story is more complicated, because global and the new exchange do not live in the same multicast network.

How do we get around this? We use a bridge. The bridge application has two parts - a process in the exchange, and one in global. Each process proxies a subset of the multicast trafﬁc from its end down a single, shared TCP link to the other end, which then faithfully copies that trafﬁc back out onto the multicast bus at the other end unchanged. The global end is slightly more special because it maintains a connection per exchange (it sends anything it hears to every exchange), but otherwise the processes are very similar. We call these processes `wan-tunnel-local` (deployed in the exchange) and `wan-tunnel-global` (deployed in global).

Our real message path is actually more like:
`global-admin-svc -> multicast -> wan-tunnel-global -> tcp link -> wan-tunnel-local -> multicast -> account-svc`

With this extra knowledge, we can reveal part two of the issue: at around the same time we see the `NullPointerException` in `account-svc`, `wan-tunnel-local` `OutOfMemoryError`s.

Even more notably, only one `wan-tunnel-local` (out of ﬁve) OOMed, and only one `account-svc` NPE'd. And yes, they were in the same exchange.

Where should we go next? There are some big clues in there. Not least the presence of our old enemyfriend, `ByteBuffer`.

### Oh, ByteBuffer, we have not missed you

```java
java.lang.OutOfMemoryError: Direct buffer memory 
 at java.base/java.nio.Bits.reserveMemory(Bits.java:175)
 at java.base/java.nio.DirectByteBuffer.<init>(DirectByteBuffer.java:118)
 at java.base/java.nio.ByteBuffer.allocateDirect(ByteBuffer.java:317)
 at TunnelInbound.readNext(TunnelInbound.java:110)
 at TunnelNetworkLoop.onRead(TunnelNetworkLoop.java:85)
 at TunnelNetworkLoop.doSelect(TunnelNetworkLoop.java:70)
 at TunnelNetworkLoop.doSelect(TunnelNetworkLoop.java:55)
 at TunnelAcceptor.run(TunnelAcceptor.java:139)
 at java.base/java.lang.Thread.run(Thread.java:829)
```

We look very closely at `wan-tunnel-global`, and work out that it is deﬁnitely copying the same multicast packets to each TCP connection handler’s work queue. This means we’ve either got a bug in the code to copy the work queue to the TCP socket in this process, or a bug in the code that reads and translates the other end of the connection in `wan-tunnel-local`.

Unfortunately, that code is full of `ByteBuffer`s. It also seems unnecessarily convoluted to our eyes. That’s an easy criticism to make when looking at code for the ﬁrst time when you know a bug lives in it, so we should reserve judgement. Walking through it and running the unit tests doesn’t immediately ﬁnd a problem at either end. Unsurprising, we suppose - if the tests exercised the bug, they’d be failing, and this bit of software has worked without error for several years. We write more unit tests, and they continue to tell us everything is ﬁne.

We’re at that funny sort of impasse where it’s tempting to start blaming cosmic rays for corrupting a packet. That usually means it’s time to try a reproduction at a different scale; we’ve been going small - can we go large? Handily, yes.

### An Example In Staging

We handily have virtualized versions of our exchanges available, using a subset of cleansed production data. In the case of our bug we have the whole (cleansed) data set, so we should be able to invoke absolute the same code path that triggered this in live.

We get staging into an appropriate state to do this, push the sync button, and…it works. The data syncs correctly.

Ok, so, it’s intermittent. We work out how to easily repeat the experiment and run it few tens of times across lunch. It works correctly every time. That’s odd - we tried this once in live, and it failed the ﬁrst time. This is either a spectacularly unlikely coincidence, or our reproduction is insufﬁciently accurate. We reassure ourselves that computers are deterministic and non-malicious, and wonder if we need to simulate the exact network conditions too; the TCP link between global and the exchange often goes over a VPN, and sometimes over a long enough distance to generate 100ms+ of RTT.

Do we ﬁnally have an excuse to get `tc`/`netem` out? Yes we do.

### We’re in control of the trafﬁc now

We make our way to a `wan-tunnel-local` host in staging, and crack our ﬁngers in preparation for typing arcane demands into the black screen of tiny letters. We believe we’re adding 150ms of delay (with a 20ms standard deviation) and rate limiting (32kbit/s with bursts up to 1mbit/s) to all trafﬁc on `eth55`.

```bash
# throttle bandwidth:
sudo tc qdisc add dev eth55 root handle 1: tbf rate 32kbit burst 1mbit
# and delay packets (both commands need to be used)
sudo tc qdisc add dev eth55 parent 1:1 handle 10: netem delay 150ms 20ms
```

Whether our understanding is correct or not swiftly becomes irrelevant - the very ﬁrst time we attempt this with both the rate limit and the delay, we see our `wan-tunnel-local` happily throw our `OOME` with the stack we’re after. We visit the logs for the local `account-svc` and ﬁnd a familiar `NPE`, too. Aha. Not cosmic rays after all. The reproducer seems pretty reliable, too - our ﬁrst three tries all generate the desired result.

Now we’re back on the science train, we can gather some more data. Attaching a debugger to `account-svc` gets us a step further - we’re trying to deserialize an `Identity` (in fact probably most of the objects in the second half of an RPC) out of a giant array of zeroed bytes.

We take some packet captures at the `wan-tunnel-local`. I forget how to use wireshark ﬁlters and instead get it to export the socket’s data as a ﬁle, then search for contiguous zeroes in it. Oh. Yes. There they are.

We take a hop upstream - we assume the `wan-tunnel-global` must have sent a packet with zeroes in, but did it receive any? Our packet captures suggest it did not receive any big arrays of `0`, and it deﬁnitely sent one.

We already looked at this code once and didn’t ﬁnd anything, but perhaps, armed with this new knowledge, something else might jump out?

#### Back in the Wan Tunnel

No, no it doesn’t. But it does change which bits of the code we look at. We’re only hitting a problem when we try to send a reasonably sized chunk of data down a constricted pipe. How does this code cope with that? Does it block, or drop, or what?

Reminder: we’re still in the global end of the wan-tunnel, doing a send to each downstream local tunnel. The channel is a standard `java.nio.channels.GatheringByteChannel` representing the socket with a local tunnel at the other end.

This method sends some data messages (there are also ‘command’ messages like acknowledgements and heartbeats) to a given channel. It is full of comments. That’s not usually a good sign…

```java
   private final GatheringByteChannel channel; 
   private ByteBuffer dataBackLog = ByteBuffer.allocateDirect(1024);
   private final ByteBuffer commandBacklog = ByteBuffer.allocateDirect(1024);

   public void writeMessages(ByteBuffer buffer) 
   {
       int messageCount = validateCompleteMessages(buffer);
       drain();
       if (!hasPendingMessages() && !hasPendingCommands())
       {
           // nothing pending anywhere; just try
           channel.write(buffer);
           // Only if we get a partial write do 
           // the remain message data fits in the backlog.
           // Give this only happens when the backlog is empty,
           // the total buffer length should not exceed the
           // message seen + a little bit to round up.
           resizeDataBacklogToFitPartialWrite(buffer);
       }
       if (buffer.hasRemaining()) // partial write
       {
           // this copies this buffer into the backlog
           addToBacklog(dataBackLog, buffer, messageCount);
           // and then we stick it in the queue.
           appendToDrainQueue(dataBackLog);
       }
       if (hasPendingCommands())
       {
           appendToDrainQueue(commandBacklog);
           drain();
       }
       outboundCounters.increment(TunnelMessageTypes.DATA, messageCount);
   }

   private static void addToBacklog(final ByteBuffer backlog, final ByteBuffer buffer, int messageCount)
   {
       backlog.compact();
       if (backlog.remaining() < buffer.remaining())
       {
           throw new DataLossException(backlog.remaining(), buffer.remaining());
       }
       backlog.put(buffer);
       backlog.flip();
   }
```

What is all this? Well. This implementation tries really hard to keep data and command messages apart (separate `dataBacklog` and `commandBacklog` ﬁelds) - why? It also refuses to store partial messages in its internal buffers (trust us on this one) - this feels sensible. It appears to make a token attempt to resize its internal buffer should it see a large enough message, but…only if there’s no data already pending? We just don’t get that at all.

What we’re looking for in here is buffer fullness, probably in either of those two backlog ﬁelds. How does `addToBacklog` work?

OK - so here’s the answer - if we get a full buffer, we throw a well named `DataLossException`, and we just drop the data on the ﬂoor. That’s probably sensible - when the underlying bytes are retranslated into application level messages, they have sequence numbers on them, and if there’s a gap, the receiver can request a resend of what is missing (this is one of the usages of the commands in the tunnel protocol).

We check how the `DataLossException` gets propagated and that looks alright.

So this method is ﬁne then. Right? We thought so - there’s even a test that checks for the `DataLossException`, and it passes, and a quick bit of debugging shows its passing for the right reason.

`ByteBuffer` veterans may be crying into their coffee at this point, because NO, that method is NOT alright.

### The missing ﬂip

Let’s look at that code once more, but slower. We’ll add some comments inline, to help.

```java
   private static void addToBacklog(final ByteBuffer backlog, final ByteBuffer buffer, int messageCount)
   {
       // move the content of backlog to the front
       // the position is set to the first byte 
       // after a compact, this buffer is in _write_ mode
       backlog.compact();
       if (backlog.remaining() < buffer.remaining())
       {             
           throw new DataLossException(backlog.remaining(), buffer.remaining());
       }
       // Given we're in write mode, add the buffer
       backlog.put(buffer);
       // Flip the buffer back into read mode so it can be
       // copied into `channel` by the next invocation
       backlog.flip();
   }
```

Can you see it now? It took the CTO sitting down next to me and pointing at it questioningly for my temporary `ByteBuffer` blindness to wear off, so don’t feel bad if you didn’t.

In the case of data loss, we omit the call to `flip` and leave the backlog in a state where it’s ready to be written. What happens if you attempt to read from it in that state? Well, what you read is a freshly zeroed out remainder of the buffer - `buffer length - content length` of `0`s.

We add a test that triggers data loss, and then tries to continue sending data afterwards. Straight away we are greeted with giant arrays of `0`s in the test output. Quite a simple error in the end; it turns out that this is the ﬁrst time that we’ve ever suffered data loss in a real environment, and this component fundamentally doesn’t handle that scenario!

### What about the OOM, though?

That’s a bit ﬁddlier to explain. Let’s imagine our buffer was small, say 1024 bytes. The following sequence of events then occurs.

1. Send message one - it’s 900 bytes long. This buffers the message in one of the backlogs.
2. Attempt to drain the backlogs to the underlying channel. This succeeds with a partial write of, say, 300 bytes.
3. Send message two - it’s 600 bytes long. This triggers data loss, and, critically, leaves the buffer in a state where it will now send 0s in the place of the last 600 bytes of message one.
4. Send more messages - it doesn’t matter what, really.

At the other end, the reader manages to discard most of the `0`s by interpreting them as empty packets - an accident, we think, rather than an explicit bit of design. Unfortunately, when the real messages turn up again, those ’empty’ packets aren’t really a valid packet length, so the ﬁrst actual packet does not start where the reader thinks it should, and we read a message length from somewhere totally inappropriate. We then try to allocate a buffer of that size. Boom. Or, rather, `OOM`.

### How did it take us so long to work this out?

This bug made it to a blog post. Many of our other bugs do not. Their errors are often just as trivial, but identifying them requires less work.

It would be easy to blame the original author, but in fact, much of the blame lay with us.

1. We gave too much weight to the fact that the code had worked ﬂawlessly for too long. We look straight past the gaps in the test coverage because of this, I think.
2. Despite all the evidence pointing at a congestion problem, we still needed a full fat reproducer to force us to look properly at the buffer full case. Even once we were looking at it, my `ByteBuffer` bug blindness struggled to see the light!
3. At some point in the past, we noticed that code was complicated, and tried to compensate with comments. We could have worked out why it was complicated, and documented that with tests. Alternatively, the tests could have shown us what we could delete (the true utility of coverage). After writing a round-trip fuzz test of the sender code, we found we could hugely simplify it - down to less than half the size it was originally, and requiring none of the comments.
4. Some absolute idiot (you’re ﬁred -Ed) at the beginning called it an impossible `NullPointerException`, and so we went looking for zebras when in fact the usual `ByteBuffer::flip` was what we needed to ﬁnd…



---

</div>

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 3.3.A: The Checker Framework (@ContractQty & @UnitQty)
- **Tool**: The Checker Framework (JSR-308 Pluggable Type-Checking for Java).
- **Application**: Allows developers to define custom type annotations (e.g. `@UnitQty` vs `@ContractQty`) to prevent accidental unit-conversion bugs at compile-time.

#### Reference 3.3.B: Test-Driven Development (TDD) as Design Infrastructure
- **TDD Cycle**: Red -> Green -> Refactor (Kent Beck).
- **At LMAX**: Automated acceptance tests act as living documentation. Tests run in live-emulation environments to continuously measure latency regressions before deployment.
