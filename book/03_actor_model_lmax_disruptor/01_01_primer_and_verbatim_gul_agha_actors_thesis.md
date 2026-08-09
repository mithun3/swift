# Module 3: High-Performance Architecture, Actor Model & LMAX Disruptor

<div class="page-break"></div>

## Chapter 3.1: Actors — A Model of Concurrent Computation in Distributed Systems (Gul A. Agha)

---

### SECTION 1: PRIMER ON THE BASICS

### 1. What Is an Actor?
The **Actor Model** is a mathematical model of concurrent computation proposed by Carl Hewitt, Henry Baker, and formalized by **Gul A. Agha** in his 1985 MIT PhD dissertation. 

In the Actor model, the fundamental unit of computation is an **Actor**. An actor is an autonomous, concurrent object that encapsulates state, behavior, and a mail address.

When an actor receives a message (communication), it can execute exactly three primitive operations:

```
                          AN ACTOR'S RESPONSE TO A MESSAGE
                          
                    ┌──────────────────────────────────────────┐
                    │          Incoming Communication          │
                    └────────────────────┬─────────────────────┘
                                         │
                                         ▼
            ┌────────────────────────────┼────────────────────────────┐
            │                            │                            │
            ▼                            ▼                            ▼
┌───────────────────────┐    ┌───────────────────────┐    ┌───────────────────────┐
│ 1. Send Messages      │    │ 2. Create New Actors  │    │ 3. Designate          │
│    To other known     │    │    Dynamically spawn  │    │    Replacement        │
│    mail addresses     │    │    new actor instances│    │    Behavior           │
└───────────────────────┘    └───────────────────────┘    └───────────────────────┘
```

### 2. Key Characteristics of the Actor Paradigm

1. **No Shared State**: Actors do not share mutable memory. Interaction occurs purely via asynchronous message-passing.
2. **Mail Addresses & Dynamic Topology**: Actors communicate by sending messages to a target's *Mail Address*. Mail addresses can be passed in messages, allowing the interconnection network of actors to change dynamically at runtime.
3. **The `become` Command & State Replacement**: In traditional OOP, an object updates its internal fields via mutation (`this.x = y`). In the Actor model, an actor replaces its behavior for processing the *next* message using a `become` operation. This allows actors to represent history-sensitive objects while maintaining mathematical immutability for each processed task.
4. **Unbounded Nondeterminism & Fair Mail Delivery**: Messages sent to an actor are placed in its *Mail Queue*. Messages arrive in arbitrary order, but the underlying mail system guarantees that every message sent will eventually be delivered (Guaranteed Mail Delivery).

### 3. Actors vs. CSP (Communicating Sequential Processes)
- **CSP (Hoare)**: Relies on **Synchronous Communication** (Rendezvous), where both sender and receiver must block until the transfer completes. Topology is static.
- **Actor Model (Hewitt/Agha)**: Relies on **Buffered Asynchronous Communication**. Senders never block. Actors can dynamically spawn new actors and pass mail addresses.

---

<div class="page-break"></div>

### SECTION 2: CONDENSED THESIS CONCEPTS & CODE EXAMPLES

The core of Gul Agha's thesis formalized the Actor Model as a framework for concurrent computation in distributed systems. Instead of dealing with the raw thesis, this section synthesizes its primary contributions and provides modern code examples to illustrate the foundational mechanics.

### 1. Encapsulation and Asynchronous Message Passing
In the Actor model, an actor encapsulates its state and behavior. It cannot be accessed directly by other objects. Interaction happens exclusively via asynchronous message passing.

*Example (Conceptual Akka/Java-like syntax):*
```java
// Define a simple Actor that responds to a message
public class CounterActor extends AbstractActor {
    private int count = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(Increment.class, msg -> {
                count++;
                System.out.println("Count is now: " + count);
            })
            .build();
    }
}

// Usage:
// sender doesn't block waiting for the CounterActor to finish
counterActorRef.tell(new Increment(), ActorRef.noSender());
```

### 2. Dynamic Creation of Actors (Topology)
Actors can create other actors dynamically. This allows the system to scale and adapt its topology on the fly based on the workload.

```java
public class SpawnerActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(SpawnTask.class, task -> {
                // Dynamically spawn a new actor to handle the task
                ActorRef worker = getContext().actorOf(Props.create(WorkerActor.class));
                worker.tell(task.getPayload(), getSelf());
            })
            .build();
    }
}
```

### 3. Mail Addresses and Network Reconfiguration
Messages carry not only data but can also include the "mail addresses" (references) of other actors. This allows actors to learn about new peers and establish new communication pathways dynamically.

```java
public class IntroActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(MeetPeer.class, msg -> {
                ActorRef peer = msg.getPeerAddress();
                // We now know about the peer and can send it a message directly
                peer.tell(new Hello(), getSelf());
            })
            .build();
    }
}
```

### 4. The `become` Operation (State Replacement)
One of the most profound concepts in Agha's formalization is that actors do not strictly "mutate" their state. Instead, they specify a replacement behavior for the *next* message. This elegant mechanism avoids race conditions and shared mutable memory issues.

```java
public class FlipFlopActor extends AbstractActor {
    
    // Initial behavior
    @Override
    public Receive createReceive() {
        return onState();
    }

    private Receive onState() {
        return receiveBuilder()
            .match(Toggle.class, msg -> {
                System.out.println("Turning OFF");
                // Specify replacement behavior for the next message
                getContext().become(offState());
            })
            .build();
    }

    private Receive offState() {
        return receiveBuilder()
            .match(Toggle.class, msg -> {
                System.out.println("Turning ON");
                // Switch behavior back
                getContext().become(onState());
            })
            .build();
    }
}
```

### 5. Unbounded Nondeterminism
Because messages are processed asynchronously and can arrive from multiple sources across a network, the order of message arrival is nondeterministic. The actor model accommodates this by guaranteeing delivery eventually, but without strict ordering unless explicitly managed (e.g., via sequence numbers).

By abstracting away the low-level locking mechanisms, Agha's model paved the way for highly scalable, resilient distributed systems like those built with Erlang, Akka, and the LMAX Disruptor.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 3.1.A: Carl Hewitt's Actor Model Foundations (1973, 1977)
- **Foundational Papers**:
  - Hewitt, Bishop, and Steiger (1973): *"A Universal Modular ACTOR Formalism for Artificial Intelligence"*, IJCAI-73.
  - Hewitt and Baker (1977): *"Laws for Communicating Parallel Processes"*, IFIP 77.
- **Hewitt's Core Vision**: Replaced the sequential von Neumann RAM machine model with an open-systems model of interacting "Actors"—laying the groundwork for Erlang/Elixir (Joe Armstrong), Akka (JVM), and Ray (Python AI distributed clusters).

#### Reference 3.1.B: Robin Milner's CCS (Calculus of Communicating Systems, 1980)
- **Background**: Developed by Turing Award winner Robin Milner (University of Edinburgh).
- **Core Process Algebra**: Expressed concurrent processes using synchronous action-coaction pairs ($a$ and $\bar{a}$).
- **Comparison to Actors**: CCS assumed a static interconnection topology and synchronous interaction, whereas Agha's Actor model supported dynamic topology (passing mail addresses) and asynchronous message buffering.

#### Reference 3.1.C: SAL (Simple Actor Language) & Act3
- **SAL Grammar**: Developed by Agha as a minimal, Algol-like kernel language for proving operational semantics of actors.
- **Act3**: Developed at MIT AI Lab by Hewitt, Agha, Theriault, and Attardi. Featured pattern-matching communication handlers, futures, and automatic continuation creation.
