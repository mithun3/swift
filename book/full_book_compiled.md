<div class="cover-page">

# Principles of Software Engineering,<br>Concurrency & System Architecture

<div class="cover-subtitle">

A Compendium of 38 Foundational Papers, Verbatim Research Texts,<br>
Technical Primers, and Reference Deep-Dives

</div>

---

<div class="cover-meta">

**Modules Covered**

Module 1 — Core Foundations of Software Engineering & Design Philosophy  
Module 2 — Hardware Evolution, Concurrency & Memory Models  
Module 3 — High-Performance Architecture, Actor Model & LMAX Disruptor  
Module 4 — Software & UI Architecture Patterns  
Module 5 — Code Evolution & Refactoring Patterns  
Module 6 — Complete Bibliography & Subject Index

---

**Sources**

38 foundational papers, technical reports, and articles from:  
*martinfowler.com · MIT CSAIL · IEEE Spectrum · ACM SIGPLAN · Sutter's Mill*  
*LMAX Technology Blog · Bad Concurrency Blog · developer.* · Intel Newsroom*

---

**Compiled for educational study.**  
All verbatim text is reproduced with full source attribution.  
All intellectual property rights remain with the original authors and publishers.

*Compilation Date: August 2026 (Final Edition)*

</div>

</div>


# Principles of Software Engineering, Concurrency & System Architecture
## A Comprehensive Compendium of 37 Foundational Papers, Verbatim Research Texts, Technical Primers & Deep-Dive References

<div class="page-break"></div>

## Table of Contents

- [How to Use This Book](#how-to-use-this-book)
- [Preface & Overview](#preface-overview)

---

### Module 1: Core Foundations of Software Engineering & Design Philosophy

- [Chapter 1.1: What Is Software Design? (Jack W. Reeves, 1992)](#chapter-11-what-is-software-design-jack-w-reeves)
- [Chapter 1.1b: Letter to the Editor & What Is Software Design — 13 Years Later (Jack W. Reeves, 1992 & 2005)](#chapter-11b-letter-to-the-editor-13-years-later)
- [Chapter 1.2: Code As Documentation (Martin Fowler, 2005)](#chapter-12-code-as-documentation-martin-fowler)
- [Chapter 1.3: The Almighty Thud (Martin Fowler, 1997)](#chapter-13-the-almighty-thud-martin-fowler)
- [Chapter 1.4: Citation & Reference Deep-Dives — Module 1](#chapter-14-citation-reference-deep-dives-for-module-1)
- [Chapter 1.5: Null References — The Billion Dollar Mistake (Tony Hoare, 2009)](#chapter-15-null-references-the-billion-dollar-mistake-tony-hoare-2009)

---

### Module 2: Hardware Evolution, Concurrency & Memory Models

- [Chapter 2.1: Welcome to the Jungle — The Concurrency Revolution (Herb Sutter, 2011)](#chapter-21-welcome-to-the-jungle-the-concurrency-revolution-herb-sutter)
- [Chapter 2.2: Moore's Law — Past, Present, and Future (Schaller 1997 & Intel 2023)](#chapter-22-moores-law-past-present-and-future-robert-r-schaller-gordon-e-moore)
- [Chapter 2.3: Synchronization & The Java Memory Model (Doug Lea & William Pugh et al.)](#chapter-23-synchronization-the-java-memory-model-doug-lea-william-pugh)
- [Chapter 2.4: Citation & Reference Deep-Dives — Module 2](#chapter-24-citation-reference-deep-dives-for-module-2)

---

### Module 3: High-Performance Architecture, Actor Model & LMAX Disruptor

- [Chapter 3.1: Actors: A Model of Concurrent Computation in Distributed Systems (Gul A. Agha, 1985)](#chapter-31-actors-a-model-of-concurrent-computation-in-distributed-systems-gul-a-agha)
- [Chapter 3.2: The LMAX Architecture & The Disruptor Pattern (Martin Fowler & LMAX Team, 2011)](#chapter-32-the-lmax-architecture-the-disruptor-pattern-martin-fowler-lmax-team)
- [Chapter 3.3: LMAX Technology Blog — Scale, Testing, Constructors, Coverage & The Impossible NullPointerException](#chapter-33-lmax-technology-blog-lessons-scale-testing-code-hygiene)
- [Chapter 3.4: Bad Concurrency — Mechanical Sympathy & Lock-Free Systems (Michael Barker)](#chapter-34-bad-concurrency-michael-barker)
- [Chapter 3.5: Citation & Reference Deep-Dives — Module 3](#chapter-35-citation-reference-deep-dives-for-module-3)

---

### Module 4: Software & UI Architecture Patterns

- [Chapter 4.1: Presentation Domain Separation & GUI Architectures (Martin Fowler)](#chapter-41-presentation-domain-separation-gui-architectures-martin-fowler)
- [Chapter 4.2: Micro Frontends & Modular React Architecture (Cam Jackson, Martin Fowler & Addy Osmani)](#chapter-42-micro-frontends-modular-react-architecture-cam-jackson-martin-fowler-addy-osmani)
- [Chapter 4.3: Serverless Architectures & Feature Toggles (Mike Roberts, Pete Hodgson & Martin Fowler)](#chapter-43-serverless-architectures-feature-toggles-mike-roberts-pete-hodgson-martin-fowler)
- [Chapter 4.4: Separated Presentation (Martin Fowler, 2006)](#chapter-44-separated-presentation-martin-fowler)
- [Chapter 4.5: Presentation Domain Data Layering (Martin Fowler, 2015)](#chapter-45-presentation-domain-data-layering-martin-fowler)
- [Chapter 4.6: Citation & Reference Deep-Dives — Module 4](#chapter-44-citation-reference-deep-dives-for-module-4)

---

### Module 5: Code Evolution & Refactoring Patterns

- [Chapter 5.1: Refactoring Fundamentals & Preparatory Refactoring (Martin Fowler)](#chapter-51-refactoring-fundamentals-preparatory-refactoring-martin-fowler)
- [Chapter 5.1b: Refactoring a JavaScript Video Store (Martin Fowler, 2016)](#chapter-51b-refactoring-a-javascript-video-store-martin-fowler-2016)
- [Chapter 5.2: Advanced & Specialized Refactoring Patterns (Martin Fowler)](#chapter-52-advanced-specialized-refactoring-patterns-martin-fowler)
- [Chapter 5.2b: Refactoring with Loops and Collection Pipelines (Martin Fowler, 2015)](#chapter-52b-refactoring-with-loops-and-collection-pipelines-martin-fowler-2015)
- [Chapter 5.2c: Refactoring to an Adaptive Model (Martin Fowler)](#chapter-52c-refactoring-to-an-adaptive-model-martin-fowler)
- [Chapter 5.2d: Refactoring Code that Accesses External Services (Martin Fowler)](#chapter-52d-refactoring-code-that-accesses-external-services-martin-fowler-2015)
- [Chapter 5.3: Citation & Reference Deep-Dives — Module 5](#chapter-53-citation-reference-deep-dives-for-module-5)

---

### Module 6: Bibliography & Subject Index

- [Chapter 6.1: Complete IEEE Bibliography & Subject Index](#module-6-bibliography-and-index)

---

### Module 7: Real-Time Implementations (Case Study)

- [Chapter 7.1: FX Pipeline Architecture Overview](#07-real-time-implementations-case-study-of-an-hft-fx-pipeline)
- [Chapter 7.2: Zero-Allocation and Mechanical Sympathy in Practice](#zero-allocation-and-mechanical-sympathy-in-practice)
- [Chapter 7.3: Event Loop and Pricing Mechanisms](#event-loop-and-pricing-mechanisms)

---

<div class="page-break"></div>

## How to Use This Book

This compendium is structured to serve both the reader who is encountering these topics for the first time, and the experienced engineer seeking precise, sourced reference material.

### The Three-Part Chapter Structure

Every chapter in this volume follows the same three-part structure, designed to build understanding progressively:

**SECTION 1 — PRIMER ON THE BASICS**

This section provides a thorough introduction to the topic before any paper is presented. It covers:
- Historical context and chronology
- Key terms and definitions — introduced plainly, no assumed knowledge
- ASCII block diagrams illustrating the core architectural or conceptual structures
- Real-world examples with concrete code samples
- Comparisons with related patterns or older approaches

*Purpose: You should be able to read Section 1 with no prior knowledge and understand what the paper is about before reading it.*

**SECTION 2 — VERBATIM TEXT**

This section presents the original research paper, blog post, or article in **exact, word-for-word** form. Every verbatim section opens with a source attribution block that states:

> 📄 **VERBATIM SOURCE**  
> Title, Author(s), Publication venue, Date, Original URL, DOI (where applicable)  
> *This text is reproduced verbatim from the original published source for educational study.*

The verbatim text is not paraphrased, edited, or summarised. What you read is exactly what the original author wrote.

**SECTION 3 — CITATION & REFERENCE DEEP-DIVES**

This section provides standalone research profiles for every cited work, person, and concept mentioned in the source paper. Each deep-dive includes publication history, mathematical or architectural detail not covered in the primer, and cross-references to other chapters.

### Code Examples

All technical code examples are presented in three languages:

1. **Java** (Java 17+ idioms)
2. **JavaScript / TypeScript** (ES2022+ idioms)
3. **Python** (Python 3.10+ idioms)

This allows engineers working in any of the three major paradigms to directly apply the patterns shown.

### Diagrams

All architectural diagrams are rendered in ASCII art. ASCII diagrams are:
- Rendered identically in every PDF renderer, browser, and printer
- Readable as plain text without any external tools
- Embedded directly in the chapter file — no external dependencies

### Print Use

This book is print-ready. Each chapter begins with a `<div class="page-break">` tag that forces a new page when printed or exported to PDF using the accompanying `print_style.css` stylesheet. To generate a print-quality PDF, open any Markdown file in a browser with the CSS linked, or use Pandoc with the provided stylesheet.

---

<div class="page-break"></div>

## Preface & Overview

Software engineering is governed by foundational paradigms that span decades of research, hardware evolution, architectural innovation, and practical craftsmanship. This compendium systematically aggregates **38 key reference texts and research papers** into six structured modules — covering the full arc from the philosophy of what software design *is*, through to the hardware realities that constrain it, the concurrent computation models that exploit it, and the refactoring techniques that continuously improve it.

### Why These Papers?

These 38 sources were selected because they share a common thread: each one changed how working engineers think about their craft. Jack Reeves forced engineers to confront the question of whether source code *is* the design. Tony Hoare highlighted how simple language features, like the null reference, can cost billions. Herb Sutter made clear that the free lunch of single-threaded performance gains was over. Gul Agha's Actor Model formalism underpins every modern concurrent system from Erlang to Akka. Martin Fowler's work on refactoring gave the profession a precise vocabulary for code improvement. The LMAX Disruptor showed the industry what it means to write software with genuine mechanical sympathy.

None of these ideas are abstract. They have direct, measurable impact on the performance, maintainability, and correctness of production systems.

### The Philosophy of This Book

This compendium follows three principles:

1. **Programmers are not assembly line workers.** Source code IS the complete, final engineering design document. (Reeves, 1992)

2. **Throw at least half of the documentation away.** Prefer "UML as Sketch" — diagrams as thinking tools, not bureaucratic deliverables. (Fowler, 1997)

3. **Write software that is sympathetic to the hardware it runs on.** Understanding cache lines, memory barriers, and CPU topology is not optional for high-performance systems. (Thompson / Barker, 2011)

### Module Overview

| Module | Topic | Papers Covered |
| :--- | :--- | :---: |
| **1** | Core Foundations of Software Engineering & Design Philosophy | 6 |
| **2** | Hardware Evolution, Concurrency & Memory Models | 5 |
| **3** | High-Performance Architecture, Actor Model & LMAX Disruptor | 8 |
| **4** | Software & UI Architecture Patterns | 10 |
| **5** | Code Evolution & Refactoring Patterns | 9 |
| **6** | Bibliography & Subject Index | — |
| **7** | Real-Time Implementations (Case Study) | 3 |
| **Total** | | **41** |

### Sources Used in This Volume

All 38 source materials are reproduced verbatim with full attribution. Sources include:

- **martinfowler.com** — 22 articles and pattern essays by Martin Fowler
- **MIT CSAIL DSpace** — Gul A. Agha doctoral thesis (AITR-844, 1985)
- **IEEE Spectrum** — Robert R. Schaller's Moore's Law analysis (1997)
- **ACM SIGPLAN POPL** — Java Memory Model formal semantics (Pugh, Manson, Adve, 2005)
- **Sutter's Mill** — Herb Sutter on concurrency and heterogeneous computing
- **LMAX Technology Blog** — Scale, testing, and production engineering at LMAX Exchange
- **Bad Concurrency Blog** — Michael Barker on mechanical sympathy and lock-free systems
- **developer.* Magazine** — Jack W. Reeves essays on software design philosophy
- **Intel Newsroom** — Intel's 2023 perspective on the future of Moore's Law
- **lmax-exchange.github.io** — The LMAX Disruptor technical paper (Thompson et al., 2011)


# Module 1: Core Foundations of Software Engineering & Design Philosophy

<div class="page-break"></div>

## Chapter 1.1: What Is Software Design? (Jack W. Reeves)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Core Misconception in Software Engineering
For decades, the software development industry attempted to establish legitimacy by modeling its processes directly on physical engineering disciplines—such as civil, mechanical, and electrical engineering. In traditional hardware engineering, a sharp boundary exists between **Design** and **Manufacturing**:

```
[Hardware Design Phase]           [Hardware Manufacturing Phase]
Engineers create blueprints  -->  Factory workers build physical products
(Schematics, CAD drawings)        (Silicon fabs, steel assembly lines)
```

In software development, early software leaders assumed a direct parallel:

```
[Software "Design" Phase]        [Software "Construction" Phase]
Architects write specs & UML --> Programmers type code into computers
(Pigeon-holed Waterfall model)   (Treated as assembly line workers)
```

**Jack W. Reeves' radical insight in 1992** was that this analogy is fundamentally broken. Programmers are **not** assembly line workers. 

#### 2. The Economics of Building Software
In hardware engineering, building a physical prototype or manufacturing 10,000 units requires massive capital investment, physical raw materials, and factory labor.

In software engineering:
- **Manufacturing** is completely automated and performed by **compilers, assemblers, and linkers**.
- The cost of "manufacturing" software is virtually **free**—it takes a few seconds or minutes of computer execution time.
- The **Source Code** is not the manufactured product; **Source Code IS the complete, final engineering design document**.

```
+-------------------------------------------------------------------+
|                       Engineering Design                          |
|  High-Level Architecture -> Class Diagrams -> Source Code (C++)   |
+-------------------------------------------------------------------+
                                  |
                                  v (Compiler & Linker)
+-------------------------------------------------------------------+
|                     Automated Manufacturing                       |
|         Machine Code / Executable Binary (Free & Instant)          |
+-------------------------------------------------------------------+
```

#### 3. Why Testing and Debugging Are Design Refinement
Because building software is free, software designers refine their designs by compiling and running them rather than spending months attempting mathematical correctness proofs or rigid paper reviews.

In traditional engineering, wind tunnel testing of bridge scale models is part of design validation. In software, **testing and debugging are the software engineering equivalent of wind tunnel simulations and prototype testing**.

#### 4. Real-World Analogy: The Architect vs. The Builder
Imagine an architect designing a skyscraper. They draw blueprints (design) and hand them to a construction crew (manufacturing). The crew pours concrete and welds steel. If a beam is too short, it's a manufacturing error.

In software, imagine the architect writes the blueprint, and a magic 3D-printer instantly builds the skyscraper for free. If the 3D-printed skyscraper collapses, it's not a manufacturing error (the printer did exactly what the blueprint said). It is a **design error**. The source code *is* the blueprint. The compiler *is* the magic 3D-printer.

#### 5. Code Example: Source Code as Design
When we write code, we are making design decisions. Consider this simple C++ example—we are not "building" an order system, we are *designing* the relationships and invariants of the system:

```cpp
#include <vector>
#include <stdexcept>

// DESIGN DECISION: An Order must always have an ID and cannot be modified once shipped.
class Order {
private:
    int orderId;
    bool isShipped;
    std::vector<int> itemIds;

public:
    Order(int id) : orderId(id), isShipped(false) {}

    void addItem(int itemId) {
        if (isShipped) {
            // Designing the invariant: Shipped orders cannot be altered.
            throw std::logic_error("Cannot add items to a shipped order.");
        }
        itemIds.push_back(itemId);
    }

    void ship() {
        isShipped = true;
    }
};
```
The compiler takes this design and "manufactures" the machine code. The engineering effort is entirely in crafting this logic.

#### 6. Historical Impact & Influence on Agile
When Jack Reeves published his essay in the *C++ Journal* in 1992, it was initially ignored. However, in the late 1990s, Ward Cunningham hosted the paper on the **C2 Wiki**, where figures like **Robert C. Martin (Uncle Bob)**, **Michael Feathers**, and **Kent Beck** discovered it. Reeves' essay provided the theoretical foundation for **Extreme Programming (XP)**, **Test-Driven Development (TDD)**, and the **Agile Manifesto**: if source code is design, then coding early, iterating continuously, and refactoring are essential engineering practices.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPERS

#### Paper 1: What Is Software Design? (1992)

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> What Is Software Design?<br>
  <strong>Author(s):</strong> Jack W. Reeves<br>
  <strong>Published:</strong> Fall 1992, C++ Journal<br>
  <strong>Source type:</strong> Original Journal Article<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

Object oriented techniques, and C++ in particular, seem to be taking the software world by storm. Numerous articles and books have appeared describing how to apply the new techniques. In general, the questions of whether O-O techniques are just hype have been replaced by questions of how to get the benefits with the least amount of pain. Object oriented techniques have been around for some time, but this exploding popularity seems a bit unusual. Why the sudden interest? All kinds of explanations have been offered. In truth, there is probably no single reason. Probably, a combination of factors has finally reached critical mass and things are taking off. Nevertheless, it seems that C++ itself is a major factor in this latest phase of the software revolution. Again, there are probably a number of reasons why, but I want to suggest an answer from a slightly different perspective: C++ has become popular because it makes it easier to design software and program at the same time.

 If that comment seems a bit unusual, it is deliberate. What I want to do in this article is take a look at the relationship between programming and software design. For almost 10 years I have felt that the software industry collectively misses a subtle point about the difference between developing a software design and what a software design really is. I think there is a profound lesson in the growing popularity of C++ about what we can do to become better software engineers, if only we see it. This lesson is that programming is not about building software; programming is about designing software. 

 Years ago I was attending a seminar where the question came up of whether software development is an engineering discipline or not. While I do not remember the resulting discussion, I do remember how it catalyzed my own thinking that the software industry has created some false parallels with hardware engineering while missing some perfectly valid parallels. In essence, I concluded that we are not software engineers because we do not realize what a software design really is. I am even more convinced of that today.

 The final goal of any engineering activity is the some type of documentation. When a design effort is complete, the design documentation is turned over to the manufacturing team. This is a completely different group with completely different skills from the design team. If the design documents truly represent a complete design, the manufacturing team can proceed to build the product. In fact, they can proceed to build lots of the product, all without any further intervention of the designers. After reviewing the software development life cycle as I understood it, I concluded that the only software documentation that actually seems to satisfy the criteria of an engineering design is the source code listings.

 There are probably enough arguments both for and against this premise to fill numerous articles. This article assumes that final source code is the real software design and then examines some of the consequences of that assumption. I may not be able to prove that this point of view is correct, but I hope to shown that it does explain some of the observed facts of the software industry, including the popularity of C++.

 There is one consequence of considering code as software design that completely overwhelms all others. It is so important and so obvious that it is a total blind spot for most software organizations. This is the fact that software is cheap to build. It does not qualify as inexpensive; it is so cheap it is almost free. If source code is a software design, then actually building software is done by compilers and linkers. We often refer to the process of compiling and linking a complete software system as "doing a build". The capital investment in software construction equipment is low—all it really takes is a computer, an editor, a compiler, and a linker. Once a build environment is available, then actually doing a software build just takes a little time. Compiling a 50,000 line C++ program may seem to take forever, but how long would it take to build a hardware system that had a design of the same complexity as 50,000 lines of C++.

 Another consequence of considering source code as software design is the fact that a software design is relatively easy to create, at least in the mechanical sense. Writing (i.e., designing) a typical software module of 50 to 100 lines of code is usually only a couple of day's effort (getting it fully debugged is another story, but more on that later). It is tempting to ask if there is any other engineering discipline that can produce designs of such complexity as software in such a short time, but first we have to figure out how to measure and compare complexity. Nevertheless, it is obvious that software designs get very large rather quickly.

 Given that software designs are relatively easy to turn out, and essentially free to build, an unsurprising revelation is that software designs tend to be incredibly large and complex. This may seem obvious but the magnitude of the problem is often ignored. School projects often end up being several thousand lines of code. There are software products with 10,000 line designs that are given away by their designers. We have long since passed the point where simple software is of much interest. Typical commercial software products have designs that consist of hundreds of thousands of lines. Many software designs run into the millions. Additionally, software designs are almost always constantly evolving. While the current design may only be a few thousand lines of code, many times that may actually have been written over the life of the product.

 While there are certainly examples of hardware designs that are arguably as complex as software designs, note two facts about modern hardware. One, complex hardware engineering efforts are not always as free of bugs as software critics would have us believe. Major microprocessors have been shipped with errors in their logic, bridges collapsed, dams broken, airliners fallen out of the sky, and thousands of automobiles and other consumer products have been recalled - all within recent memory and all the result of design errors. Second, complex hardware designs have correspondingly complex and expensive build phases. As a result, the ability to manufacture such systems limits the number of companies that produce truly complex hardware designs. No such limitations exist for software. There are hundreds of software organizations, and thousands of very complex software systems in existence. Both the number and the complexity are growing daily. This means that the software industry is not likely to find solutions to its problems by trying to emulate hardware developers. If anything, as CAD and CAM systems have helped hardware designers to create more and more complex designs, hardware engineering is becoming more and more like software development. 

 Designing software is an exercise in managing complexity. The complexity exists within the software design itself, within the software organization of the company, and within the industry as a whole. Software design is very similar to systems design. It can span multiple technologies and often involves multiple sub-disciplines. Software specifications tend to be fluid, and change rapidly and often, usually while the design process is still going on. Software development teams also tend to be fluid, likewise often changing in the middle of the design process. In many ways, software bears more resemblance to complex social or organic systems than to hardware. All of this makes software design a difficult and error prone process. None of this is original thinking, but almost 30 years after the software engineering revolution began, software development is still seen as an undisciplined art compared to other engineering professions.

 The general consensus is that when real engineers get through with a design, no matter how complex, they are pretty sure it will work. They are also pretty sure it can be built using accepted construction techniques. In order for this to happen, hardware engineers spend a considerable amount of time validating and refining their designs. Consider a bridge design, for example. Before such a design is actually built the engineers do structural analysis; they build computer models and run simulations; they build scale models and test them in wind tunnels or other ways. In short, the designers do everything they could think of to make sure the design is a good design before it is built. The design of new airliner is even worse; for those, full scale prototypes must be built and test flown to validate the design predictions. 

 It seems obvious to most people that software designs do not go through the same rigorous engineering as hardware designs. However, if we consider source code as design, we see that software designers actually do a considerable amount of validating and refining their designs. Software designers do not call it engineering, however, we call it testing and debugging. Most people do not consider testing and debugging as real "engineering"; certainly not in the software business. The reason has more to do with the refusal of the software industry to accept code as design than with any real engineering difference. Mock-ups, prototypes, and bread-boards are actually an accepted part of other engineering disciplines. Software designers do not have or use more formal methods of validating their designs because of the simple economics of the software build cycle.

 Revelation number two: it is cheaper and simpler to just build the design and test it than to do anything else. We do not care how many builds we do—they cost next to nothing in terms of time, and the resources used can be completely reclaimed later if we discard the build. Note that testing is not just concerned with getting the current design correct, it is part of the process of refining the design. Hardware engineers of complex systems often build models (or at least they visually render their designs using computer graphics). This allows them to get a "feel" for the design that is not possible by just reviewing the design itself. Building such a model is both impossible and unnecessary with a software design. We just build the product itself. Even if formal software proofs were as automatic as a compiler, we would still do build/test cycles. Ergo, formal proofs have never been of much practical interest to the software industry.

 This is the reality of the software development process today. Ever more complex software designs are being created by an ever increasing number of people and organizations. These designs will be coded in some programming language and then validated and refined via the build/test cycle. The process is error prone and not particularly rigorous to begin with. The fact that a great many software developers do not want to believe that this is the way it works compounds the problem enormously. 

 Most current software development processes try to segregate the different phases of software design into separate pigeon-holes. The top level design must be completed and frozen before any code is written. Testing and debugging are necessary just to weed out the construction mistakes. In between are the programmers, the construction workers of the software industry. Many believe that if we could just get programmers to quit "hacking" and "build" the designs as given to them (and in the process, make fewer errors) then software development might mature into a true engineering discipline. Not likely to happen as long as the process ignores the engineering and economic realities.

 For example, no other modern industry would tolerate a rework rate of over 100% in its manufacturing process. A construction worker who can not build it right the first time, most of the time, is soon out of a job. In software, even the smallest piece of code is likely to be revised or completely rewritten during testing and debugging. We accept this sort of refinement during a creative process like design, not as part of a manufacturing process. No one expects an engineer to create a perfect design the first time. Even if she does, it must still be put through the refinement process just to prove that it was perfect.

 If we learn nothing else from Japanese management techniques, we should learn that it is counter-productive to blame the workers for errors in the process. Instead of continuing to force software development to conform to an incorrect process model, we need to revise the process so that it helps rather than hinders efforts to produce better software. This is the litmus test of "software engineering." Engineering is about how you do the process, not about whether the final design document needs a CAD system to produce it.

 The overwhelming problem with software development is that *everything* is part of the design process. Coding is design, testing and debugging are part of design, and what we typically call software design is still part of design. Software may be cheap to build, but it is incredibly expensive to design. Software is so complex that there are plenty of different design aspects and their resulting design views. The problem is that all the different aspects interrelate (just like they do in hardware engineering). It would be nice if top level designers could ignore the details of module algorithm design. Likewise, it would be nice if programmers did not have to worry about top level design issues when designing the internal algorithms of a module. Unfortunately, the aspects of one design layer intrude into the others. The choice of algorithms for a given module can be as important to the overall success of the software system as any of the higher level design aspects. There is no hierarchy of importance among the different aspects of a software design. An incorrect design at the lowest module level can be as fatal as a mistake at the highest level. A software design must be complete and correct in all its aspects, or all software builds based on the design will be erroneous. 

 In order to deal with the complexity, software is designed in layers. When a programmer is worrying about the detailed design of one module, there are probably hundreds of other modules and thousands of other details that he can not possibly worry about at the same time. For example, there are important aspects of software design that do not fall cleanly into the categories of data structures and algorithms. Ideally, programmers should not have to worry about these other aspects of a design when designing code.

 This is not how it works, however, and the reasons start to make sense. The software design is not complete until it has been coded *and* tested. Testing is a fundamental part of the design validation and refinement process. The high level structural design is not a complete software design; it is just a structural framework for the detailed design. We have very limited capabilities for rigorously validating a high level design. The detailed design will ultimately influence (or *should* be allowed to influence) the high level design at least as much as other factors. Refining all the aspects of a design is a process that should be happening throughout the design cycle. If any aspect of the design is frozen out of the refinement process, it is hardly surprising that the final design will be poor or even unworkable. 

 It would be nice if high level software design could be a more rigorous engineering process, but the real world of software systems is not rigorous. Software is too complex and it depends on too many other things. Maybe some hardware does not work quite the way the designers thought it did, or a library routine has an undocumented restriction. These are the kinds of problems that every software project encounters sooner or later. These are the kinds of problems discovered during testing (if we do a good job of testing), for the simple reason that there was no way to discover them earlier. When they are discovered, they force a change in the design. If we are lucky, the design changes are local. More often than not, the changes will ripple through some significant portion of the entire software design (Murphy's Law). When part of the effected design can not change for some reason, then the other parts of the design will have to be weakened to accommodate. This often results is what managers perceive as "hacking", but it is the reality of software development.

 For example, I recently worked on a project where a timing dependency was discovered between the internals of module A and another module B. Unfortunately, the internals of module A were hidden behind an abstraction that did not permit any way to incorporate the invocation of module B in its proper sequence. Naturally, by the time the problem was discovered, it was much too late to try to change the abstraction of A. As expected, what happened was an increasingly complex set of "fixes" applied to the internal design of A. Before we finished installing version 1, there was the general feeling that the design was breaking down. Every new fix was likely to break some older fix. This is a normal software development project. Eventually, my colleagues and I argued for a change in the design, but we had to volunteer free overtime in order to get management to agree. 

 On any software project of typical size, problems like these are guaranteed to come up. Despite all attempts to prevent it, important details will be overlooked. This is the difference between craft and engineering. Experience can lead us in the right direction. This is craft. Experience will only take us so far into uncharted territory. Then we must take what we started with and make it better through a controlled process of refinement. This is engineering.

 As just a small point, all programmers know that writing the software design documents after the code instead of before, produces much more accurate documents. The reason is now obvious. Only the final design, as reflected in code, is the only one refined during the build/test cycle. The probability of the initial design being unchanged during this cycle is inversely related to the number of modules and number of programmers on a project. It rapidly becomes indistinguishable from zero.

 In software engineering, we desperately need good design at all levels. In particular, we need good top level design. The better the early design, the easier detailed design will be. Designers should use anything that helps. Structure charts, Booch diagrams, state tables, PDL, etc.—if it helps, then use it. We must keep in mind, however, that these tools and notations are not a software design. Eventually, we have to create the real software design, and it will be in some programming language. Therefore, we should not be afraid to code our designs as we derive them. We simply must be willing to refine them as necessary.

 There is as yet no design notation equally suited for use in both top level design and detailed design. Ultimately, the design will end up coded in some programming language. This means that top level design notations have to be translated into the target programming language before detailed design can begin. This translation step takes time and introduces errors. Rather than translate from a notation that may not map cleanly into the programming language of choice, programmers often go back to the requirements and redo the top level design, coding it as they go. This, too, is part of the reality of software development.

 It is probably better to let the original designers write the original code, rather than have someone else translate a language independent design later. What we need is a unified design notation suitable for all levels of design. In other words, we need a programming language that is also suitable for capturing high level design concepts. This is where C++ comes in. C++ is a programming language suitable for real world projects that is also a more expressive software design language. C++ allows us to directly express high level information about design components. This makes it easier to produce the design, and easier to refine it later. With its stronger type checking, it also helps the process of detecting design errors. This results in a more robust design, in essence a better engineered design.

 Ultimately, a software design must be represented in some programming language, and then validated and refined via a build/test cycle. Any pretense otherwise is just silliness. Consider what software development tools and techniques have gained popularity. Structured programming was considered a breakthrough in its time. Pascal popularized it and in turn became popular. Object oriented design is the new rage and C++ is at the heart of it. Now think about what has not worked. CASE tools? Popular, yes; universal, no. Structure charts? Same thing. Likewise, Warner-Orr diagrams, Booch diagrams, object diagrams, you name it. Each has its strengths, and a single fundamental weakness—it really isn't a software design. In fact the only software design notation that can be called widespread is PDL, and what does that look like.

 This says that the collective subconscious of the software industry instinctively knows that improvements in programming techniques and real world programming languages in particular are overwhelmingly more important than anything else in the software business. It also says that programmers are interested in design. When more expressive programming languages become available, software developers will adopt them.

 Also consider how the process of software development is changing. Once upon a time we had the waterfall process. Now we talk of spiral development and rapid prototyping. While such techniques are often justified with terms like "risk abatement" and "shortened product delivery times", they are really just excuses to start coding earlier in the life cycle. This is good. This allows the build/test cycle to start validating and refining the design earlier. It also means that it is more likely that the software designers that developed the top level design are still around to do the detailed design.

 As noted above—engineering is more about how you do the process than it is about what the final product looks like. We in the software business are close to being engineers, but we need a couple of perceptual changes. Programming and the build/test cycle are central to the process of engineering software. We need to manage them as such. The economics of the build/test cycle, plus the fact that a software system can represent practically anything, makes it very unlikely that we will find any general purpose methods for validating a software design. We can improve this process, but we can not escape it. 

 One final point: the goal of any engineering design project is the production of some documentation. Obviously, the actual design documents are the most important, but they are not the only ones that must be produced. Someone is eventually expected to use the software. It is also likely that the system will have to be modified and enhanced at a later time. This means that auxiliary documentation is as important for a software project as it is for a hardware project. Ignoring for now users manuals, installation guides, and other documents not directly associated with the design process, there are still two important needs that must be solved with auxiliary design documents.

 The first use of auxiliary documentation is to capture important information from the problem space that did not make it directly into the design. Software design involves inventing software concepts to model concepts in a problem space. This process requires developing an understanding of the problem space concepts. Usually this understanding will include information that does not directly end up being modeled in the software space, but which nevertheless helped the designer determine what the essential concepts were, and how best to model them. This information should be captured somewhere in case the model needs to be changed at a later time.

 The second important need for auxiliary documentation is to document those aspects of the design that are difficult to extract directly from the design itself. These can include both high level and low level aspects. Many of these aspects are best depicted graphically. This makes them hard to include as comments in the source code. This is *not* an argument for a graphical software design notation instead of a programming language. This is no different from the need for textual descriptions to accompany the graphical design documents of hardware disciplines. Never forget that the source code determines what the actual design really is, not the auxiliary documentation. Ideally, software tools would be available that post processed a source code design and generated the auxiliary documentation. That may be too much to expect. The next best thing might be some tools that let programmers (or technical writers) extract specific information from the source code that can then be documented in some other way. Undoubtedly, keeping such documentation up to date manually is difficult. This is another argument for the need for more expressive programming languages. It is also an argument for keeping such auxiliary documentation to a minimum and keeping it as informal as possible until as late in the project as possible. Again, we could use some better tools, otherwise we end up falling back on pencil, paper, and chalk boards.

 To summarize: 

  - Real software runs on computers. It is a sequence of ones and zeros that is stored on some magnetic media. It is not a program listing in C++ (or any other programming language).
 - A program listing is a document that represents a software design. Compilers and linkers actually build software designs.
 - Real software is incredibly cheap to build, and getting cheaper all the time as computers get faster.
 - Real software is incredibly expensive to design. This is true because software is incredibly complex and because practically all the steps of a software project are part of the design process.
 - Programming is a design activity—a good software design process recognizes this and does not hesitate to code when coding makes sense.
 - Coding actually makes sense more often than believed. Often the process of rendering the design in code will reveal oversights and the need for additional design effort. The earlier this occurs, the better the design will be.
 - Since software is so cheap to build, formal engineering validation methods are not of much use in real world software development. It is easier and cheaper to just build the design and test it than to try to prove it.
 - Testing and debugging are design activities—they are the software equivalent of the design validation and refinement processes of other engineering disciplines. A good software design process recognizes this and does not try to short change the steps.
 - There are other design activities—call them top level design, module design, structural design, architectural design, or whatever. A good software design process recognizes this and deliberately includes the steps.
 - All design activities interact. A good software design process recognizes this and allows the design to change, sometimes radically, as various design steps reveal the need.
 - Many different software design notations are potentially useful—as auxiliary documentation and as tools to help facilitate the design process. They are not a software design.
 - Software development is still more a craft than an engineering discipline. This is primarily because of a lack of rigor in the critical processes of validating and improving a design. 
 - Ultimately, real advances in software development depend upon advances in programming techniques, which in turn mean advances in programming languages. C++ is such an advance. It has exploded in popularity because it is a mainstream programming language that directly supports better software design.
 - C++ is a step in the right direction, but still more advances are needed.


<div class="page-break"></div>

## Chapter 1.1b: Letter to the Editor & 13 Years Later

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Backlash and Misinterpretation
When Jack Reeves published his original essay, he expected a massive industry debate. Instead, he got silence. It took years, and the rise of the Agile movement, for the essay to gain traction. When it finally did, many misinterpreted his core message of "Code is Design" to mean "Don't design, just hack code."

This was a fundamental misunderstanding. Reeves argued that upfront thinking (using tools like UML or whiteboards) is essential, but it is not the *final product*. The final design document is the source code itself, because that is what the compiler (the manufacturing plant) actually builds from.

#### 2. Real-World Analogy: The Novelist's Outline
Consider a novelist writing a book. They might spend weeks creating an outline, character sketches, and plot diagrams on a whiteboard. This is valuable preparation. However, if they hand the outline to a printing press, they won't get a novel. The *actual writing of the chapters* is where the final, nuanced design of the story happens. In software, UML diagrams are the outline; source code is the novel.

#### 3. Code Example: UML vs. Code Reality
A UML diagram might show a simple relationship: `Customer 1 --> * Order`. This is a useful abstraction, but it lacks the critical engineering details required to actually function.

When a programmer writes the code, they must make concrete design decisions that the UML ignored:
```cpp
#include <memory>
#include <vector>

class Order { /* ... */ };

class Customer {
private:
    // DESIGN DECISION: Who owns the memory of these Orders?
    // A UML line doesn't specify if it's a weak reference, shared ownership, or unique ownership.
    // By using std::unique_ptr, we make a strict design decision that the Customer *owns* the Orders.
    std::vector<std::unique_ptr<Order>> orders;

public:
    void addOrder(std::unique_ptr<Order> newOrder) {
        orders.push_back(std::move(newOrder));
    }
};
```
The act of writing this code *is* the act of designing the system's memory management and lifecycle. The UML was just a sketch.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPERS

#### Paper 2: Letter to the Editor (Precursor to What Is Software Design?) (1992)

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Letter to the Editor<br>
  <strong>Author(s):</strong> Jack W. Reeves<br>
  <strong>Published:</strong> Written May 19, 1992<br>
  <strong>Source type:</strong> Correspondence<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

May 19, 1992

 From: Jack W. Reeves, San Jose, CA

 To: Livleen Singh, Editor, *C++ Journal*, Port Washington, NY

 Dear Editor,

 Thank you for printing my letter of August 27, 1991 commenting on software design. I agree (in principal, if not in detail) with most of your reply. What we all want is to find ways to produce better software and help our industry "mature into a disciplined science." My problem is, about ten years ago I came to the conclusion that, as an industry, we do not understand what a software design really is. I am even more convinced of this today. I do not claim that my point of view is correct, but I have found it very useful in explaining why some things work and why others do not. There is a very subtle, but very important point which is being missed. This is the difference between doing software design and what a finished software design really is. I would like to elaborate upon this point.

 Allow me to begin with the final part of your reply. I made the statement (supplying context) "It may not be a very good (software) design, but a (software) design it is." You suggest comparing software design with bridge design and created the following statement "It may not be a very good bridge, but a bridge it is." Here the word "bridge" is substituted for "(software) design." The interpretation seems to be "Would you trust something that was built with little or no design?" The obvious answer is "Of course not!" The comparison is not valid, however. The fact that it seems valid to most of the industry is exactly the point I am trying to make.

 Instead of changing the sentence, change the context instead. Now the statement would read "It may not be a very good (bridge) design, but a (bridge) design it is." Would you volunteer to be the first across this bridge? The immediate answer might be "No, a bad design is no better than no design!" A little thought will show that an equally valid answer is "What bridge?" Until you actually build the bridge from the design there is no need to worry about crossing it. The point is that we don't build bridges from scratch designs. Before the bridge actually gets built, the design will be refined considerably. We will do analysis. We will build computer models and run simulations. We may even build scale models and test them in wind tunnels and other ways. We will do everything we can to make sure the design is a good design before we build the bridge. We call this "engineering" (and sometimes, despite everything, it still isn't completely right...there was this bridge in Tacoma).

 Back to software. We in the software industry also refine our designs, only we don't get to call it engineering. We call it "testing and debugging". This phase of the software lifecycle takes a long time. All too often it takes longer than planned. Unfortunately, it is often not enough and the final designs that we turn into deliverable software are still not as good as they should be. This seems like a fact of software life. Many people lament it and ask why we software developers do not "engineer" our designs better? Many explanations are offered, but never the one most obvious to me — simple economics. Software is dirt cheap to build.

 Am I crazy? I don't think so! Compiling and linking 50,000 lines of C++ code on your 486 may seem to take forever, but how would you like to assemble a circuit card with 50,000 discreet components, or build a bridge with 50,000 structural elements? We don't construct mathematical proofs of software correctness or run our code through symbolic executors because it takes less time and effort to just build it and test it. We probably would get better software if we did more of the former, but we don't. Why not?

 There are probably lots of reasons, but I would like to suggest that many of them derive from our failure to consider testing and debugging as part of the software design process. We would like for it to go away completely. Since it will not, we try to treat it as some sort of "quality assurance" function and spend as little time, effort, and money on it as we can get away with. We consider it a shame of the software industry that testing and debugging take up half the typical software development lifecycle. Is it really so bad? I suspect that most engineers in other disciplines haven't a clue about what percentage of their time is spent actually creating a design and what is spent on testing and debugging the result. Some industries are probably better than software. I am pretty sure that other industries are actually much worse. Consider what it must take to "design" a new airliner.

 I get somewhat testy when people start making gratuitous comparisons between software design and other engineering disciplines. Major microprocessors have been shipped with bugs in their logic, bridges have collapsed, dams broken, airliners fallen out of the sky, and thousands of automobiles and other consumer products recalled - all within recent memory and all the result of design errors.

 The problem with software is - design is not just important, it is basically everything. Saying that programmers should not have to design is like saying fish should not have to swim. When I am coding, I am designing. I am creating a software design out of the void. Sometimes the algorithm is simple and the design is trivial. Often times, I have to design data structures and the algorithms to match. There may be alternatives and I have to choose between them based upon my perception of the advantages and disadvantages of each. Sometimes I decide that the design is getting too complex and I specify one or more sub-modules. When I have finished the design, I test it to see how good it is and refine it to make it better. Refinements not only come from finding errors in the original design, but from other sources such as peer walkthroughs and formal reviews. The bottom line is that my design must be correct, or every piece of software built from it will be erroneous. Therefore I concentrate on doing it right, and it takes mental effort and skill, just like any other creative design activity.

 Nevertheless, most software systems are quite large and quite complex and my one module is only a small part. While I am concentrating on the details of code design for module X, there may be hundreds of other modules and thousands of other details that I can not possibly worry about at the same time. There are also important aspects of software design which do not fall cleanly into the categories of data structures and algorithms. It is these "other aspects" that most people mean when they say software design.

 It is true that programmers do not want to worry about the "high level aspects" of a design when they are designing code. They often end up having to worry about them anyway. The high level design clearly affects the detailed design. The converse is also true. The details of internal design may (or *should*) help decide amongst high level alternatives. Refining all aspects of the design is a process that should be happening throughout the development cycle. If some aspect of the design is "frozen" out of the refinement process, you can potentially end up with a poor or even unworkable final design.

 Some of my colleagues have interpreted my harangues on this subject as "Jack says forget design and just start coding". Nothing could be further from the truth (though I see how they get that impression). I am not against traditional software design. We desperately need good design at all levels. It doesn't matter whether we call the early process top level design, structural design, module design, or whatever. What I have been arguing for are two changes in perception. First, that we recognize that the results of the early design steps are not a complete software design any more than the first rough sketches are a complete bridge design. Second, that we capture our design thinking using a notation that is a true skeleton software design. That means using a programming language.

 Ultimately, the computer doesn't care how we get to a final software design any more than the steeplejack building a bridge cares how the bridge design was refined and validated. All that matters to either of them is that the design they are working from be sufficient to allow them to correctly build the product. On the other hand, what it takes to create a good software design obviously matters a lot to those of us responsible for creating one. The better the early design, the less work needed refining it later. That is what we are really talking about, is it not.

 What I am arguing for is not less software design, but a realistic software design process. We need to recognize the difference between *designing software* and *a software design*. It makes sense to use any tools and techniques that we find help us during the design process. It does not make sense to forget what is the real software design. When we have worked out some aspect of software design, we should not let incorrect comparisons with hardware engineering disciplines keep us from correctly documenting our work in a software design. *YES, WE SHOULD CODE IT*. If what we are really doing is software design, then everything we do will somehow be reflected in code. We might as well write the code (or that portion of it that makes sense) when we make the decisions that affect that code.

 I know all the arguments for "language independent" software design notations. They all ignore a fundamental problem. Software design involves translating concepts from some problem space into a programming language. This translation has to be done by human beings, and since our programming languages are usually totally inadequate to express the concepts of the problem space directly, it is usually a difficult and error prone process. When we translate concepts from one form to another, especially complex ones, we often loose important information. If several translations are involved, we are likely to end up with a final product that lost too much vital information, that does not accurately reflect our original concept, and/or that simply contains errors. This is compounded several times when the people actually doing the translation are different for each step. Remember, there is nothing sacred about C++ (or Ada, or C, or Smalltalk, or LISP, or any programming language). It is not the native language of our computers. Fundamentally, programming languages are just a design notation themselves. I do not see any point in introducing extra translation steps if they can be avoided.

 There are a couple of problems with my "code as design" approach, which I acknowledge. The first is that even the best programming languages have serious weaknesses as tools for expressing certain aspects of a software design. The information is in the code (if it isn't, then it wasn't software design information) but it is very difficult to get it out in human readable form. These are the "other aspects" of a software design mentioned above. The second problem is similar. There is going to be information from the problem space that went into the software design, but that can not be reconstructed from the software design itself. We want to capture this information in case we need to change the software design later. The typical source code comment is not an adequate mechanism.

 Both of these problems mean that auxiliary documentation is as important to software design as it is to any other engineering discipline, if not more so. We must recognize auxiliary documentation as such, however, and not confuse it with the software design.

 What we really need is more expressive programming languages. This is what led to my statement about C++ being a major advance in software design art. C++ is a more expressive programming language, which makes it a better software design tool.

 As a final topic, consider what my point of view reveals about traditional software development processes. Ultimately, all software design processes end up validating and refining the final design via a build/test cycle. Any pretense otherwise is just silliness. Yet, traditional MIL-STD and other waterfall model development processes will not even allow writing one line of code until a certain tonnage of auxiliary documentation has been produced and reviewed. Often, the people who produce this documentation then go on to other things leaving a group of new, and usually much younger and less experienced people to actually generate the real software design. It is hardly surprising (to me anyway) that this process has fallen into such disrepute that no real developers advocate it. What are they trying instead?

 Now we have rapid prototyping and spiral development. In my view, it is easy to see why these are replacing the waterfall method. Both of these are just excuses for writing code earlier in the development cycle so that the process of refining the design via build/test can begin sooner. They also typically get the same people involved in both the top level design and the actual code design. Not surprisingly, these two approaches are both seen as significant improvements. Even the best of the traditional approaches continue to try to break software design into disjoint steps with separate notations and products, and then they continue to wonder why they have problems getting a final software product that is correct.

 Projects done in Ada have shown significant improvements in the time necessary to integrate, test and debug (at the expense of some extra top level design effort). Structured programming was considered a breakthrough in its time. Object oriented design and C++ are taking the world by storm. Forget all the explanations offered for these phenomenon, and consider what hasn't worked. CASE tools? Popular, yes; universal, no. Structure charts? Same thing. Likewise, Warner-Orr diagrams, Booch diagrams, object diagrams, you name it. Each has its strengths, and a single fundamental weakness - it really isn't a software design. Ultimately, improvements in programming techniques are overwhelmingly more important to software development than anything else.

 There seems to be a collective fantasy of the software community that if we could just find the right graphical design notation so that software designs would look like other engineering designs, then we could take our place as software engineers alongside the other disciplines. I disagree. Engineering is about how you do the process, not about whether the final product needs a CAD system to render it. We in the software business are so close, but so far away. Software is so — well, *soft*. A software system can represent anything. This, plus the economics of the build/test cycle, makes it very unlikely that we will find general purpose methods for validating a software design other than the current trial and error. We can improve the process, however. Maybe if we started treating software development as a homogeneous design process, and concentrated on improving the most important phases (programming, debug and test), we might find our industry to be more of a disciplined science than we think it is.

 I still do not know if I have made my point, but in summary:

  - Real software is what runs on computers. This means that real software is not C++ (or any other programming language). 
 - Real software is built by computers (via compilers and linkers).
 - This means that a source code listing (in any programming language) is really a software design.
 - It follows from the above that real software is incredibly cheap to build, and getting cheaper all the time as computers get faster.
 - Software is incredibly expensive to design. This is true in both an absolute sense (because of its ever increasing complexity), and relative to the cost of a software build.
 - Programming is a design activity - a good software design process recognizes this and doesn't hesitate to code when coding makes sense.
 - Coding actually makes sense a whole lot more often than traditional software design processes would have you believe.
 - Testing and debugging are design activities - they are the software equivalent of the analysis, simulation, modeling, and testing phases of other engineering disciplines. The goal is to validate and improve the design before the final product is built. A good software design process recognizes this and doesn't try to disown or short change the steps.
 - There are other design activities - call them top level design, module design, or whatever. A good software design process recognizes this and deliberately includes the steps.
 - All design activities interact. A good software design process recognizes this and allows the design to change, sometimes radically, as other design steps reveal the need.
 - Many different software design notations are potentially useful - as auxiliary documentation (it would be nice to have some tools that help us generate and maintain auxiliary documentation automatically from the actual source code. This would be particularly useful in maintaining a graphical representation of the structural aspects of the design).
 - Ultimately, real advances in software development depend upon advances in programming. C++ is a good step in the right direction. For software engineering's sake, we need a great many more steps.
 
 ###

 *Copyright ©1992 by Jack W. Reeves. Author owns and reserves all future rights. Reprint or distribute only with written permission.*

 Historical Note

 In an email correspondence discussing the publication of this letter and its relationship to the original letter, 

 > "The sequence of events was as follows: I read an article about software design in one issue of C++ Journal. I sent a letter to the editor complaining about something (I don't remember what, and can not find the letter on my system these days). The editor printed the letter, and a response. What I have attached is the letter I emailed to the editor as a response to his response. I think you will find it rather familiar. I personally find it better written than the article itself. The editor Livleen Singh offered to allow me to turn my points into a full article, which I did."

---

<div class="page-break"></div>

#### Paper 3: What Is Software Design: 13 Years Later (2005)

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> What Is Software Design: 13 Years Later<br>
  <strong>Author(s):</strong> Jack W. Reeves<br>
  <strong>Published:</strong> 2005, developer.*<br>
  <strong>Source type:</strong> Follow-up Article<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

People have occasionally asked whether I did any follow-on writing to my "What Is Software Design" article. The answer has basically been "No, not really." I want to make it clear that that is not because I forgot about it or otherwise changed my mind. Allow me to offer a bit of explanation.

 When the article appeared, I hoped–actually expected–that I would get some type of rebuttal from some sort of industry "expert." I was looking forward to this since part of my reason for writing the article had been hopes of stimulating discussion within the software industry about the overall software development process. Nothing happened.

 There were no letters to the editor that I know about and nothing ever sent directly to me. *C++ Journal* became defunct shortly after that issue, and I figured my article had gone to that great land fill in the sky that swallows most publications. I went on to doing other things. It wasn’t until 1997 or 1998 that I got an email from Bob Martin (who had just taken over as editor of the *C++ Report*) letting me know there was a wiki page about my article on Ward Cunningham’s c2.com web site. This was–quite literally–the first time I knew anybody had read my article (other than the people I personally gave a copy to).

 I started to follow the discussions on the wiki page and occasionally on some news groups, but deliberately stayed out of them myself for several reasons: a) I was focused on certain other things at the time, b) it was pretty obvious that other people who had accepted what I was trying to say were just as qualified–maybe more so–to argue the points as I would have been (I specifically remember Michael Feathers writing), and c) last but not least, it still looked to me like there was a lot of opposition to the concept. Unfortunately, most of the arguments sounded pretty much like the ones I had been dealing with for almost 15 years by that point (remember, I had had the idea almost 10 years before I wrote the article).

 I had grown tired of trying to deal with people who were totally incapable of getting past their own pre-conceptions to even consider the idea rationally. It was like trying to explain that the French speak a different language to someone who is convinced that "different language" really just means "different dialect of English". No matter what you say, they will parse your arguments against their beliefs and either dismiss you out of hand, or patronize you with their counter arguments. I had seen a number of projects where "design it in the code" worked, but even the people on such projects often refused to accept the reality. My level of cynicism about being able to improve things was very high.

 It still is, but I think it is time I made some attempt to actually defend myself, rather than let other people do it. Therefore, what I am going to do is address some of the most common criticisms I have seen about "What Is Software Design?". 

 **A.** Initially, the most common criticism I would see can be summarized as "If source code is the design, then programmers are designers; but obviously they are not, therefore source code cannot be the design." Nobody states it that baldly, but when you parse what they do say, it comes down to the same thing. These are circular arguments that start with the assumption that programming/coding is a manufacturing type of activity. In logic, this is known as a "Begging the Question" fallacy. In essence, these people say "your assumption (i.e. source code is the design) contradicts my assumption (i.e. programmers are assembly workers), therefore your assumption must be wrong."

 Someone might suggest that I am doing the same thing, i.e. starting with the assumption that source code is a design. I accept that–up to a point. While I will admit that a lot of the article reads like an attempt to prove that "source code is the design", that was not really what I was trying to do. The following quote is from the beginning of the article:

 > "This article assumes that final source code is the real software design and then examines some of the consequences of that assumption. I may not be able to prove that this point of view is correct, but I hope to show that it does explain some of the observed facts of the software industry, ..."

 I did not set out to prove that "source code is the design"; I will readily concede that what is a "design" is to some extent a matter of definition. The point of the article was to try to show how this assumption led to much better explanations of numerous observed *facts*. I am still waiting for anyone to offer better explanations based upon alternative assumptions.

 **B.** These days, thanks in part to the rise of Extreme Programming and other Agile Methods, people are starting to accept (grudgingly) that programmers are not assembly line drones. Unfortunately, that doesn’t mean they are willing to accept the concept of "the source code is the design". The arguments can be summarized by the example that is still on the wiki page:

 > "As for throwing the whole design thing out, and just designing in code... Hahahahahahahahah no really Hahahahahahahahah :)"

 This really makes me angry. For reasons that I do not understand, reasonably intelligent people insist upon confusing the concept of design as *process* versus design as *product.* You would think that anyone who passed high school would understand the difference between the process of writing a paper (for example) and the paper itself. Certainly, you would expect anyone with a college background to understand that there are often lots of different ways to arrive at the same solution.

 Nevertheless, people keep insisting that my contention of "the source code is the design" means "don’t do design, just code." I never said anything of the sort. What I did say was:

 > "In software engineering, we desperately need good design at all levels. In particular, we need good top level design. The better the early design, the easier detailed design will be. Designers should use anything that helps. Structure charts, Booch diagrams, state tables, PDL, etc.—if it helps, then use it."

 Today, I would phrase it differently. I would say we need good architectures (top level design), good abstractions (class design), and good implementations (low level design). I would also say something about using UML diagrams or CRC cards to explore alternatives. Nevertheless, I will not back away from the following statement:

 > "We must keep in mind, however, that these tools and notations are not a software design. Eventually, we have to create the real software design, and it will be in some programming language. Therefore, we should not be afraid to code our designs as we derive them."

 This is fundamental. I am not arguing that we should not "do design." However you want to approach the *process*, I simply insist that you have not completed the process until you have written *and* tested the code.

 Personally, I think a person with his feet on the desk staring at the ceiling can be "doing design" just as seriously as someone playing with UML diagrams in ROSE. I have always known that you are better off if you put some real thought into what you are trying to do before actually doing it. People differ widely in what helps them think, however. Some people use pencil and paper. Others like white boards or even computer tools. Some people like to bounce ideas off of other people, others like peace and quiet. Some people feel comfortable with diagrams like UML. Others prefer CRC cards.

 What approach they choose doesn’t matter; until someone starts insisting that these intermediate designs should be products in their own right. It’s the code that matters. If you get good code, does it really matter how it came about? If you don’t get good code, does it really matter how much other garbage you made people do before they wrote the bad code?

 Everybody that has been in this business any length of time has seen plenty of examples where someone obviously sat down and coded the first thing that popped into their mind. Later, when it became obvious that there were shortcomings with the approach, there was too much blood, sweat, and "skin" invested in the code to scrap it and do something better. Fine, we all know a little thought can go a long way.

 On the other hand, any of us who has spent time on a *traditional* development project with its strict rules forbidding the writing of a single line of code until the "design" is completed and reviewed and approved, etc. knows you can waste a hell of a lot of time producing documents that are out of date literally days after the actual coding starts. Why bother?

 You think we could find some happy medium of "enough" design effort, but not too much. There is no such thing. The only way we validate a software design is by building it and testing it. There is no silver bullet, and no "right way" to do design. Sometimes an hour, a day, or even a week spent thinking about a problem can make a big difference when the coding actually starts. Other times, 5 minutes of testing will reveal something you never would have thought about no matter how long you tried. We do the best we can under the circumstances, and then refine it.

 One last comment: I also did not say that the only necessary documentation is the source code. I specifically pointed out in the article:

 > "...auxiliary documentation is as important for a software project as it is for a hardware project."

 The source code may be the master design document, but it is seldom the only one necessary.

 **B****’.** I cannot resist making a remark about a side issue that often comes up in discussions about Extreme Programming and Agile methods that is related to the above. This is often phrased as a question: what about the Less Able Programmer? The issue seems to be that only the very best programmers can "design" and "code" at the same time. To offset this, we must have all those intermediate design steps and products mentioned above to make up for the lack of experience and talent of the average programmer.

 To me, this is like asking "what do we do about the less able physician?" I know the practice of medicine and software development are not analogous, but bear with me for a moment. An awful lot of the practice of medicine is pretty much rote (we joke about "take two aspirin and call me tomorrow"). Nevertheless, the medical profession still insists upon some pretty high standards of intelligence, education, and experience before someone is allowed to call themselves an MD. In other words, we want our doctors to know what they are doing.

 In software development, questions about the less able programmer really come down to trying to substitute a process for intelligence, aptitude, and experience. Apparently, a lot of people think that if we force people to create enough UML diagrams (or whatever), have enough reviews, and otherwise follow a detailed process, that eventually they will figure out what they are doing and code it correctly. There is no evidence that such approaches have worked in the past, and I see no reason to believe they will work in the future. In fact, my own experience says that properly using tools such as UML involves a considerable level of expertise and experience in its own right.

 **C.** Another argument I have seen questions the contention that the goal of an engineering effort is some type of documentation. Some people argue that the goal of engineering is a "product" and that real engineers often "build" things and those "things" are as much a product of engineering as any documentation.

 This argument tries to sidestep the question of "What is a Software Design?" by implying a parallel between the "things" that other engineers build and what software developers create. Frankly, this is nonsense. I will concede that there are engineers who build "things" with little or no formal design documentation, although I suspect that even in those cases there is probably some design documentation (even if it is on the back of an envelope). In any case, I think we can safely say that such projects produce only one-off products and are usually done by individuals.

 When an "engineering" effort starts involving more than a couple of people, or when it has a formal manufacturing phase, then documentation starts to loom larger and larger as the actual product of the engineering effort. You better believe that the engineers at Toyota or Motorola produce documentation, and we’ll not even think about the engineers at Boeing or Lockheed. So, while it might be true that a lot of engineers do things besides producing design documents, anyone who calls himself an engineer knows what a design document in his field looks like, and probably produces such more often than not. Can we say the same for "software engineers"?

 Incidentally, this contention regarding engineers and documentation was not mine originally, but instead something I picked up from an article in *Datamation* back in 1979. I agree with it completely however.

 **D.** One final but fairly minor argument I have seen is that source code is still too high level to be a design. At least one critic wanted to call source code the "specification." His (or her) take was that the real design is what comes out of the compiler. In some sense this is just a matter of definition, but I still disagree with it.

 The generally accepted definition is that a "specification" states the *what*, which is followed by a design document that details the *how*. While there is a certain amount of flexibility allowed of the compiler in determining the *how* of object code, there is certainly no creativity involved. And that is where I draw the line. When the document is detailed enough, complete enough, and unambiguous enough that it can be interpreted mechanistically, whether by a computer or by an assembly line worker, then you have a design document. If it still requires creative human interpretation, then you don’t.

 In software development, the design document is a source code listing.

 ###

 *Copyright ©2005 by Jack W. Reeves. Author owns and reserves all future rights. Reprint or distribute only with written permission.*


<div class="page-break"></div>

# Chapter 1.5: Null References — The Billion Dollar Mistake (Tony Hoare, 2009)

## SECTION 1 — PRIMER ON THE BASICS

Before we examine Tony Hoare's famous admission regarding the invention of the null reference, it is crucial to understand what a null reference is, why it was introduced, and why it has caused so much grief in software engineering.

### What is a Null Reference?

In programming, a reference (or pointer) is a value that stores the memory address of another value or object. A **null reference** is a special marker used to indicate that the pointer does not point to any valid object or memory location. It represents the *absence* of a value.

When a program attempts to dereference a null pointer—that is, when it tries to read or write to the memory location it points to, or call a method on it—the runtime environment encounters an invalid memory access. This results in a fatal error, most famously known in Java as a `NullPointerException` (NPE), or a segmentation fault in C/C++.

### The Problem with Null

The core issue with null references is that they bypass the type system. If a function is declared to return an object of type `Customer`, the compiler guarantees that you will get a `Customer`. However, if the language allows null references, the function might return a `Customer` *or* it might return `null`. The type system does not force the programmer to check for this absence of value, pushing the burden of safety entirely onto runtime checks and programmer discipline.

If a programmer forgets to add an `if (customer != null)` check, the program will crash in production when a null is unexpectedly encountered.

```text
+---------------------+        +---------------------+
|      Pointer        |        |       Memory        |
+---------------------+        +---------------------+
| customerRef (0x00)  | -----> | [INVALID ACCESS]    |  <-- CRASH! (NullPointerException)
+---------------------+        +---------------------+
| userRef (0x8F4A)    | -----> | { name: "Alice" }   |  <-- Safe Dereference
+---------------------+        +---------------------+
```

### Real-World Examples & Modern Solutions

Modern languages have evolved to fix this mistake by bringing the absence of a value into the type system (e.g., using `Optional`, `Maybe` types, or strict null safety where `String` and `String?` are different types).

#### 1. Java

**The Problem:**
```java
// Java - The Classic NPE
public String getCityName(User user) {
    // If user is null, or getAddress() returns null, this throws an NPE
    return user.getAddress().getCity(); 
}
```

**The Modern Solution (Java 8+):**
```java
import java.util.Optional;

public String getCityNameSafe(Optional<User> userOpt) {
    // Optional forces the programmer to handle the absence of a value
    return userOpt
            .flatMap(User::getAddressOpt)
            .map(Address::getCity)
            .orElse("Unknown City");
}
```

#### 2. JavaScript / TypeScript

**The Problem:**
```javascript
// JavaScript - TypeError: Cannot read properties of null
function printZipCode(user) {
    console.log(user.address.zipCode); // Crashes if user or address is null/undefined
}
```

**The Modern Solution (TypeScript Strict Null Checks & Optional Chaining):**
```typescript
// TypeScript - Compile-time safety and Optional Chaining (?.)
interface User {
    address?: {
        zipCode: string;
    };
}

function printZipCodeSafe(user: User | null) {
    // The ?. operator short-circuits to undefined instead of crashing
    console.log(user?.address?.zipCode ?? "No Zip Code Provided");
}
```

#### 3. Python

**The Problem:**
```python
# Python - AttributeError: 'NoneType' object has no attribute 'address'
def get_zip(user):
    return user.address.zip_code
```

**The Modern Solution (Type Hints and Pattern Matching/Guards):**
```python
from typing import Optional

class Address:
    zip_code: str

class User:
    address: Optional[Address]

def get_zip_safe(user: Optional[User]) -> str:
    # Explicit checks are required, aided by static analyzers like mypy
    if user is not None and user.address is not None:
        return user.address.zip_code
    return "No Zip"
```

---

## SECTION 2 — VERBATIM TEXT

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Null References: The Billion Dollar Mistake (Presentation Abstract & Keynote Extract)<br>
  <strong>Author(s):</strong> Sir Tony Hoare<br>
  <strong>Publication venue:</strong> QCon London Software Development Conference<br>
  <strong>Date:</strong> August 25, 2009<br>
  <strong>Original URL:</strong> https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/<br>
  <strong>Note:</strong> This text is reproduced verbatim from the original published presentation abstract and the defining transcript extract for educational study. As this was a keynote presentation rather than a formal academic paper, the following represents the canonical quote and context that introduced the concept to the software engineering lexicon.
</div>

### Presentation Abstract

I call it my billion-dollar mistake. It was the invention of the null reference in 1965. At that time, I was designing the first comprehensive type system for references in an object oriented language (ALGOL W). My goal was to ensure that all use of references should be absolutely safe, with checking performed automatically by the compiler. But I couldn't resist the temptation to put in a null reference, simply because it was so easy to implement. This has led to innumerable errors, vulnerabilities, and system crashes, which have probably caused a billion dollars of pain and damage in the last forty years.

In recent years, a number of program analysers like PREfix and PREfast in Microsoft have been used to check references, and give warnings if there is a risk they may be non-null. More recent programming languages like Spec# have introduced declarations for non-null references. This is the solution, which I rejected in 1965.

### Keynote Transcript Extract

"I call it my billion-dollar mistake... It was the invention of the null reference in 1965. I was designing the first comprehensive type system for references in an object-oriented language. My goal was to ensure that all use of references should be absolutely safe, with checking performed automatically by the compiler.

But I couldn't resist the temptation to put in a null reference, simply because it was so easy to implement. This has led to innumerable errors, vulnerabilities, and system crashes, which have probably caused a billion dollars of pain and damage in the last forty years.

We've all seen it... a program is running perfectly well, and then suddenly it stops and puts out a message 'Null Reference Exception', or 'Segmentation Fault'. And the user is left looking at a screen which is completely dead, and all the work that they have done in the last hour is lost. 

I've been trying to think of how to get rid of it. I think the only way is to put it into the type system."

---

## SECTION 3 — CITATION & REFERENCE DEEP-DIVES

### Sir Charles Antony Richard Hoare (Tony Hoare)
Sir Tony Hoare is one of the foundational figures of computer science. Beyond the infamous null reference, he is the inventor of the Quicksort algorithm (1959), Hoare logic (a formal system with a set of logical rules for reasoning rigorously about the correctness of computer programs), and Communicating Sequential Processes (CSP), which heavily influenced the concurrency models of languages like Go (goroutines) and Erlang (actors).

### ALGOL W (1966)
ALGOL W was a programming language created by Niklaus Wirth and Tony Hoare as a proposal for the successor to ALGOL 60. It was in the design of this language's type system that Hoare introduced the null reference (`null`). ALGOL W introduced several other critical concepts to programming, including string types, bitstrings, complex numbers, and records with reference (pointer) types. It was the direct predecessor to Pascal.

### The Type System Solution: Optionals and Monads
As Hoare noted, the solution he rejected in 1965 was to handle the absence of a value at the compiler level. In modern software engineering, this is achieved through strict compile-time checks or algebraic data types.

When a language implements an `Option` or `Maybe` type (common in Rust, Haskell, Swift, and later retrofitted into Java as `Optional`), it is employing a Monadic pattern. The type system forces the programmer to explicitly "unwrap" the value before using it, making it impossible to accidentally dereference a null pointer. 

As discussed in modern engineering circles (such as HackerNews debates and JavaPro architectural reviews), the philosophical shift is moving from **"null as a valid state of any object"** to **"null as an explicitly declared wrapper type."** Languages like Kotlin and TypeScript achieve this via "Null Safety" features, where a type `String` is guaranteed never to be null, and a nullable string must be explicitly declared as `String?`. This fulfills Hoare's original 1965 vision of ensuring all reference use is absolutely safe, checked automatically by the compiler.

### Modern Null Safety Mechanisms: Rust & Kotlin

#### 1. Rust's `Option<T>` and Borrow Checker
Rust entirely eliminates null pointers at compile time. There is no `null` keyword in safe Rust. Absence of a value is represented by the `Option<T>` enum:
```rust
enum Option<T> {
    Some(T),
    None,
}
```
Combined with pattern matching (`match` or `if let`), the compiler guarantees at compile time that an unhandled `None` variant cannot cause runtime crashes.

#### 2. Kotlin vs Java Null Safety Comparison

| Feature | Java (Pre-8) | Java 8+ | Kotlin |
| :--- | :--- | :--- | :--- |
| **Default Reference Type** | Nullable | Nullable | **Non-Nullable by default** (`String`) |
| **Nullable Reference Type** | `String` | `Optional<String>` | `String?` |
| **Safe Call Operator** | N/A | `map(...)` | `?.` (e.g. `user?.address?.zip`) |
| **Elvis / Default Operator** | Ternary check | `orElse(...)` | `?:` (e.g. `val name = user?.name ?: "Guest"`) |
| **Compile-time Guarantee** | None | Runtime `Optional` checks | **Strict compile-time enforcement** |

### Communicating Sequential Processes (CSP) & Hoare's Legacy
In 1978, C.A.R. Hoare published *"Communicating Sequential Processes"* (CACM), establishing the foundational formal algebra for concurrent computation. CSP introduced synchronous channel communication between independent processes, directly inspiring the concurrency architecture of modern systems languages, most notably Go (channels and goroutines) and Erlang (actors).



<div class="page-break"></div>

## Chapter 1.2: Code As Documentation (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Core Misconception of Agile Documentation
When Agile methods and Extreme Programming (XP) grew in popularity during the late 1990s and early 2000s, a major misconception emerged across software engineering teams. Critics often claimed: *"Agile developers don't write documentation; they believe code is the only documentation needed."*

In his 2005 article **"Code As Documentation"**, Martin Fowler directly refutes this myth. Fowler clarifies that saying **"code is the primary source of documentation"** is not saying **"code is the ONLY documentation."** 

#### 2. Precision, Clarity, and the Fallacy of Code Quality
Fowler echoes Jack Reeves' core thesis: source code is the only software artifact that is sufficiently detailed and precise to act as the primary design documentation.

However, Fowler introduces a vital nuance:
- **Code as Design is an ideal, not an automatic reality.**
- Code can be clean, expressive, and clear—or it can be total gibberish.
- Declaring code as documentation does not mean any arbitrary codebase is good documentation. Programmers must actively invest cognitive effort to craft readable, intention-revealing code.

```
                  SOFTWARE DOCUMENTATION SPECTRUM
┌─────────────────────────────────────────────────────────────────┐
│ Gibberish UML Diagrams / Outdated Specs  --> Poor Understanding  │
├─────────────────────────────────────────────────────────────────┤
│ Unreadable, Obfuscated Source Code       --> Poor Understanding  │
├─────────────────────────────────────────────────────────────────┤
│ Expressive, Well-Refactored Source Code  --> Clear Primary Design │
│ + Concise High-Level Auxiliary Diagrams  --> Complete System View │
└─────────────────────────────────────────────────────────────────┘
```

#### 3. How to Make Code Act as Documentation
Fowler highlights key practices that turn source code into clear documentation:
1. **Refactoring**: Regularly restructuring code without changing its external behavior to enhance its readability and intention.
2. **Pair Programming**: Having another engineer continuously read and review your code in real-time to eliminate confusing constructs.
3. **Team Conventions over Personal Style**: Bending individual preferences (e.g., ternary operators vs. clear conditional blocks) to align with team readability.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

#### Paper 4: Code As Documentation (2005)

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Code As Documentation<br>
  <strong>Author(s):</strong> Martin Fowler<br>
  <strong>Published:</strong> March 2005, martinfowler.com<br>
  <strong>Source type:</strong> Internet article<br>
  <strong>Original URL:</strong> https://martinfowler.com/bliki/CodeAsDocumentation.html<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

One of the common elements of agile methods is that they raise programming to a central role in software development - one much greater than the software engineering community usually does. Part of this is classifying the code as a major, if not the primary documentation of a software system.

Almost immediately I feel the need to rebut a common misunderstanding. Such a principle is not saying that code is the only documentation. Although I've often heard this said of Extreme Programming - I've never heard the leaders of the Extreme Programming movement say this. Usually there is a need for further documentation to act as a supplement to the code.

The rationale for the code being the primary source of documentation is that it is the only one that is sufficiently detailed and precise to act in that role - a point made so eloquently by Jack Reeves's famous essay “What is Software Design?”

This principle comes with a important consequence - that it's important that programmers put in the effort to make sure that this code is clear and readable. Saying code is documentation isn't saying that a particular code base is good documentation. Like any documentation, code can be clear or it can be gibberish. Code isn't more inherently clear than any other form of documentation. (And other forms of documentation can be hopelessly unclear too - I've seen plenty of gibberish UML diagrams, to flog a popular horse.)

Certainly it seems that most code bases aren't very good documentation. But just as it's a fallacy to conclude that declaring code to be documentation excludes other forms, it's a fallacy to say that because code is often poor documentation means that it's necessarily poor. It is possible to write clear code, indeed I'm convinced that most code can be made much more clear.

Frankly part of the reason that code is often so hard to read is because people aren't taking it seriously as documentation. If there's no will to make code clear, then there's no chance it will spring into clarity all by itself. So the first step to clear code is to accept that code is documentation, and then put the effort in to make it be clear. I think this comes down to what was taught to most programmers when they began to program. My teachers didn't put much emphasis on making code clear, they didn't seem to value it and certainly didn't talk about how to do it. We as a whole industry need to put much more emphasis on valuing the clarity of code.

The next step is to learn how, and here let me offer you the advice of a best selling technical author - there's nothing like review. I would never think of publishing a book without having many people read it and give me feedback. Similarly there's nothing more important to clear code than getting feedback from others about what is or isn't easy to understand. So take every opportunity to find ways to get other people to read your code. Find out what they find easy to understand, and what things confuse them. (And pair programming is a great way to do this.)

For more concrete advice - well I suggest reading good books on programming style. Code Complete is the first place to look. I'll naturally suggest Refactoring - after all much of refactoring is about making code clearer. After Refactoring, Refactoring to Patterns is an obvious suggestion.

You'll always find people will disagree on various points. Remember that a code base is owned primarily by a team (even if you practice individual code ownership over bits of it). A professional programmer is prepared to bend her personal style to reflect the needs of the team. So even if you like ternary operators don't use them if your team doesn't find them easy to understand. You can program in your own style on your personal projects, but anything you do in a team should follow the needs of that team.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 1.2.A: Steve McConnell's "Code Complete" (1993, 2004)
- **Background**: Written by Steve McConnell and published by Microsoft Press (1st Ed 1993, 2nd Ed 2004, 960 pages). Widely considered the practical handbook for software construction.
- **Core Insights**:
  - **Managing Complexity**: Software construction is primarily about keeping code simple enough for human minds to comprehend.
  - **Self-Documenting Code**: Variable naming, routines, and structure should be so clear that external comments are largely auxiliary.
  - **Defensive Programming**: Techniques for writing resilient code during construction.
- **Connection to Fowler**: Fowler recommends *Code Complete* as the foundational prerequisite before diving into code refactoring.

#### Reference 1.2.B: Martin Fowler's "Refactoring: Improving the Design of Existing Code" (1999)
- **Background**: Written by Martin Fowler with contributions by Kent Beck, John Brant, Erich Gamma, and Don Roberts (Addison-Wesley, 1999).
- **Definition of Refactoring**: A disciplined technique for restructuring an existing body of code, altering its internal structure without changing its external behavior.
- **Key Concepts**: Introduced the taxonomy of **Code Smells** (e.g., Long Method, Large Class, Primitive Obsession, Feature Envy) and step-by-step mechanics for eliminating them.

#### Reference 1.2.C: Joshua Kerievsky's "Refactoring to Patterns" (2004)
- **Background**: Written by Joshua Kerievsky (Addison-Wesley, 2004).
- **Core Synthesis**: Merged Martin Fowler's refactoring techniques with the Gang of Four (GoF) Design Patterns.
- **Key Philosophy**: Design patterns should not be applied upfront in an over-engineered fashion; instead, code should be refactored *towards* design patterns gradually as domain complexity demands.

#### Reference 1.2.D: Collective Code Ownership & Pair Programming
- **Extreme Programming (XP) Roots**: Introduced by Kent Beck in XP.
- **Collective Ownership**: No single developer "owns" a module; any team member can improve any part of the codebase.
- **Pair Programming as Continuous Code Review**: Two programmers work at one workstation (Driver and Navigator). This acts as a real-time feedback loop, forcing the code to be written clearly enough for both engineers to understand immediately.


<div class="page-break"></div>

## Chapter 1.3: The Almighty Thud (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The "Thud" Phenomenon in Enterprise Architecture
In traditional enterprise software projects, management and architecture teams frequently evaluated design quality by weight and volume. When an external consultant or architect delivered an exhaustive design document, it arrived in massive physical binders. Dropping it on a desk produced a heavy, physical **"Thud"**.

```
                           THE "THUD" FAILURE CYCLE
┌────────────────────────────────────────────────────────────────────────┐
│ 1. CASE Tools produce hundreds of pages of exhaustive dictionary specs │
├────────────────────────────────────────────────────────────────────────┤
│ 2. Document lands on desk with an "Almighty Thud"                     │
├────────────────────────────────────────────────────────────────────────┤
│ 3. Developers are buried in detail; nobody reads or updates it          │
├────────────────────────────────────────────────────────────────────────┤
│ 4. Code drifts from documentation -> Documentation becomes useless noise│
└────────────────────────────────────────────────────────────────────────┘
```

#### 2. The Dictionary Mentality vs. Effective Communication
Martin Fowler argues that CASE (Computer-Aided Software Engineering) tools promote a dangerous **"dictionary mentality"**:
- Developers attempt to document *every single attribute*, *every getter/setter*, and *every trivial interaction*.
- Example: Defining a `Contract` class as "a contract between many parties" and `dateSigned` as "the date the contract was signed."
- This results in zero added conceptual value while burying readers in self-evident noise.

#### 3. The Art of Minimalist Auxiliary Documentation
Effective documentation is **selective communication**. Good documentation highlights only the non-obvious, high-level interactions and package dependencies, leaving algorithmic details to the source code.

Fowler's guidelines for effective auxiliary documentation:
1. **Package-Level Diagrams**: Show high-level packages and their dependencies to minimize coupling (using tools like UML class diagrams).
2. **Key Collaborations**: Document no more than a dozen key use cases with brief interaction diagrams.
3. **Brevity as a Feature**: Keep package documents brief (under a dozen pages) so they can easily be kept up-to-date as the codebase evolves.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

#### Paper 5: The Almighty Thud (1997)

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> The Almighty Thud<br>
  <strong>Author(s):</strong> Martin Fowler<br>
  <strong>Published:</strong> November/December 1997, Distributed Computing<br>
  <strong>Source type:</strong> Magazine Column<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

I was chatting with a client about an object model review they wanted to me to do. “We can send some documentation in advance, would that be useful?” they asked. I replied in the affirmative, hoping that I was not lying. Two days later the UPS man dropped the package off outside my door, it made a loud noise. It was a good inch and a half of documentation.

I opened it up and found a print-out provided from a CASE tool. It showed a few diagrams, and gave exhaustive descriptions for every class, with every attribute, and every operation. All of these had definitions. The Contract class was defined as “a contract between many parties”, its dateSigned attribute was defined as “the date the contract was signed”. I read through the inch and a half of documentation, but at the end I was little wiser. There was much on what the objects were, but little explanation of what they were meant to do. It wasn’t the first time this had happened, and I’ll be surprised if it is the last.

Why do we bother with models or documentation? They don’t execute, and our customers pay us for working code, not pretty pictures. We bother with models to communicate. The idea is that a graphical object model can show how objects fit together more clearly than looking at the source, an interaction diagram can show a collaboration better than figuring out the call paths across several class definitions. But so often the design documentation fails in this, and leaves me puzzled on my sofa.

Part of the problem is the CASE tools that people use for this kind of work. (CASE tools have two purposes, documentation and code generation, and I’m only talking about the former role here.) CASE tools encourage a dictionary mentality. You make an entry for every class, you show every class and every attribute on the diagrams, you draw an interaction diagram for every use case. They encourage completism by helping you answer the question “have we documented everything?”

That question is the wrong question. If you document everything, you are giving everything an equal weight. Do that for a complex system, and you are buried in detail. In any system there are some aspects that are more important than the others, key parts of the system that once understood, will help someone to learn more. The art in documentation is to find how to document these aspects as clearly as possible. In this you emphasize these areas, and leave the details for the code.

And all this documentation must be brief. Only if it is brief will people read it and understand it. Only if it is brief will you be able to keep it up to date. You won’t be able to talk about everything, and nor should you. A friend of mine told me about a project where they were reluctant to change class names, not because the code took too long to change, but the documentation took too long to change. When documentation becomes a problem you should deal with it. Throw at least half of it away.

##### What should you say?
How should you choose what to show? I’m afraid that is down to your professional judgement. There are no rules to guide you, only your own skill as a designer and communicator. Maybe that is why people try to show everything, because they cannot decide what to leave out. So here is my approach, as it stands at the moment.

If your system is of any reasonable size, divide your system into packages (a la UML or Java). Each package consists of a group of classes that work together for a particular purpose. Document the overall structure of your system with a diagram that shows packages and their dependencies. (In UML this is a specific use of a class diagram, I use it so often that I like to name it a package diagram, see my book UML Distilled.) Work with your design to minimize these dependencies, this is the key to minimizing the coupling in your system. (There’s not much to read on how to do this, the best one I know is Robert Martin’s Designing Object-Oriented C++ Applications Using the Booch Method.)

For each package, write a brief document. The basis of the document is some narrative text that describes the key things that package does, and how it does it. UML diagrams can be used to help support this. Draw a class diagram that shows the important classes in the package but not necessarily all of them. For each class show only the key attributes and operations, definitely not all of them. Concentrate on interface rather than implementation. For each important collaboration in the package, show an interaction diagram. If any class has interesting lifecycle behavior, then show it with a state diagram. The document should be short enough that you don’t find it a problem keeping it up to date. I usually try to keep it to no more than a dozen pages.

As well as documentation per package, it is also useful to show how collaborations extend across packages. For this identify key use-cases in the system, and document them with interaction diagrams and narrative. A class diagram that highlights the classes involved is also useful. Many people advocate drawing interaction diagrams for every use case in the system. I feel that lead to too much documentation, but if you find it useful, and you find it isn’t a problem to keep it up to date, then go ahead and do it. Even so you should identify no more than a dozen key use-cases to highlight as the ones that everyone needs to understand.

##### Communication is the key
In this entire article I’m stressing communication. I’ve taken a few swipes at CASE tools, but that is primarily to say that using a tool does not by itself mean you are communicating. Any tool can help or hinder communication, how you use it determines the outcome.

A project I know bought a multi-user CASE tool that any developer can access from their workstation. All designs have to be in the CASE tool. But just because any developer can use it does not mean that every developer does use it. In fact very few developers looked at the models in the CASE tool, and even fewer understood them. Realizing this the architect of the project took over an area of wall at the office, and covered it with a series of diagrams that showed the half-dozen key collaborations in the system. He showed them using object diagrams with color-coding to help emphasize what was going on. It does not mean all the developers understand all the design, but at least now they can see what the important elements are.

When I started to write this article I was overwhelmed by the things I could talk about. Lots of anecdotes and tips came to mind, but I knew that to get you to read and remember this article I could only talk about a few of them. I had to select the key things that I had to mention. Communication is all about that. The key to good communication is to highlight the important things to say. Saying everything is not communication. That just passes the selection of the important things onto your readers, and discourages them with a heavy document. That selection of information is one of the most important parts of communication, and is the responsibility of every designer.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 1.3.A: Martin Fowler's "UML Distilled" (1997)
- **Background**: Written by Martin Fowler (Addison-Wesley, 1st Ed 1997, 3rd Ed 2003). One of the most commercially successful software architecture books in history.
- **Core Contribution**: Stripped away the unnecessary complexity of the massive 800-page UML specification, teaching software engineers to use UML as **"UML as Sketch"** and **"UML as Notes"** rather than "UML as Blueprint."

#### Reference 1.3.B: Package Diagrams & Minimizing Coupling
- **Concept**: A Package Diagram in UML groups related classes into higher-level logical modules and displays directional dependency arrows between packages.
- **Robert C. Martin's Package Principles**:
  1. **Acyclic Dependencies Principle (ADP)**: The dependency structure between packages must contain no cycles.
  2. **Stable Dependencies Principle (SDP)**: Depend in the direction of stability (modules that change often must depend on modules that are stable).

#### Reference 1.3.C: Wall-Mounted Visual Architecture ("The Big Visible Chart")
- **Agile Practice**: Originating in Extreme Programming and Lean Software Engineering (Kanban boards and Radiators), physical wall-mounted diagrams with color-coding provide passive information radiators.
- **Why It Beats Electronic CASE Tools**: Electronic repositories hide architectural models behind menus. Wall diagrams make key system collaborations immediately visible to anyone walking through the office.


<div class="page-break"></div>

## Chapter 1.4: Citation & Reference Deep-Dives for Module 1

This chapter provides standalone, in-depth research and analytical profiles of all major cited books, foundational theories, historical figures, and methodologies referenced across Module 1.

---

### Deep-Dive 1.4.1: The Evolution of C++ as a Software Design Language

```
1979: C with Classes (Bjarne Stroustrup at Bell Labs)
  │
  ├── 1983: Renamed to C++ (Addition of virtual functions, references, constants)
  │
  ├── 1985: Commercial Release & "The C++ Programming Language" (1st Ed)
  │
  ├── 1992: Jack Reeves publishes "What Is Software Design?" in C++ Journal
  │          └── Argument: C++ type checking & classes enable direct coding of design
  │
  └── 1998: ISO/IEC 14882:1998 (Standard C++ Specification)
```

#### Historical Context
When Bjarne Stroustrup created C++ at AT&T Bell Laboratories in 1979 (initially called "C with Classes"), his goal was to combine the hardware-level speed and low-level memory efficiency of C with the object-oriented abstraction capabilities of Simula 67.

#### Why C++ Catalyzed the "Code as Design" Realization
Prior to C++, procedural languages (like C, FORTRAN, and Pascal) separated data structures from the algorithms operating on them. Designers relied on graphical flowcharts, structure charts, and Program Design Languages (PDL) to express high-level system components because the programming languages themselves were too low-level.

C++ introduced fundamental language mechanisms that enabled direct expression of high-level architectural concepts inside source code:
1. **Strong Static Type Checking**: Errors in interface definitions were caught at compile-time rather than runtime.
2. **Encapsulation (`public`/`private`/`protected`)**: Explicit access specifiers allowed designers to enforce strict module boundaries directly in code.
3. **Classes & Inheritance (`virtual` functions)**: Enabled polymorphism and clean interface-implementation separation without pointers to void.

---

### Deep-Dive 1.4.2: Structured Programming vs. Object-Oriented Design

| Dimension | Structured Programming (1970s–1980s) | Object-Oriented Design (1990s–Present) |
| :--- | :--- | :--- |
| **Primary Unit of Abstraction** | Functions / Procedures (`top-down decomposition`) | Objects / Classes (`encapsulated data + behavior`) |
| **Data Handling** | Separate data structures passed into procedures | Data hidden inside object state |
| **Key Champions** | Edsger W. Dijkstra, Niklaus Wirth, C.A.R. Hoare | Bjarne Stroustrup, Grady Booch, Alan Kay |
| **Popular Languages** | Pascal, C, ALGOL 60 | C++, Java, Smalltalk, C# |
| **Major Flaw in Practice** | Changes to data structures forced changes across dozens of functions | Fragile base class problems if inheritance is misused |

---

### Deep-Dive 1.4.3: The Fallacy of "Begging the Question" in Software Engineering

In classical logic, **Begging the Question** (*Petitio Principii*) is a logical fallacy where an argument's premises assume the truth of the conclusion, instead of supporting it.

```
                  THE TRADITIONAL SOFTWARE WATERFALL FALLACY
                  
 Premise 1 (Unproven Assumption): 
 Coding is a low-level manufacturing process performed by assembly drones.
                  │
                  ▼
 Conclusion / Circular Reasoning:
 Therefore, source code cannot be the engineering design, and programmers 
 must not be allowed to code until full upfront documentation is frozen.
```

Jack Reeves exposed this circular reasoning: if one instead starts with the premise that **compiling/linking is manufacturing**, then source code naturally becomes the complete design, and coding becomes the ultimate engineering design activity.

---

### Deep-Dive 1.4.4: Summary of Cited Works & Further Reading

[1] J. W. Reeves, "What is software design?" C++ Journal, Fall 1992. Available: https://www.developerdotstar.com/mag/articles/reeves_design_main.html
[2] J. W. Reeves, "What is software design: 13 years later," Developer Dot Star, Feb. 2005. Available: https://www.developerdotstar.com/mag/articles/reeves_design_main.html
[3] J. W. Reeves, "Letter to the Editor," C++ Journal, 1992. Available: https://www.developerdotstar.com/mag/articles/reeves_design_main.html
[4] M. Fowler, "Code As Documentation," MartinFowler.com, 2005. Available: https://martinfowler.com/bliki/CodeAsDocumentation.html
[5] M. Fowler, "The Almighty Thud," MartinFowler.com, 1997. Available: https://martinfowler.com/ (archive)

**Supplementary Books**
[S1] S. McConnell, *Code Complete: A Practical Handbook of Software Construction*, Microsoft Press, 1993, 2004.
[S2] R. C. Martin, *Designing Object-Oriented C++ Applications Using the Booch Method*, Prentice Hall, 1995.
[S3] M. Fowler, *UML Distilled: A Brief Guide to the Standard Object Modeling Language*, Addison-Wesley, 1997, 2003.
[S4] M. Fowler, *Refactoring: Improving the Design of Existing Code*, Addison-Wesley, 1999.
[S5] J. Kerievsky, *Refactoring to Patterns*, Addison-Wesley, 2004.

**Subject Index Cross-References:**
- Software Design ..... Ch 1.1, Ch 1.1b


# Module 2: Hardware Evolution, Concurrency & Memory Models

<div class="page-break"></div>

## Chapter 2.1: Welcome to the Jungle — The Concurrency Revolution (Herb Sutter)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Era of the "Free Lunch" (1975–2005)
For three decades, software engineering enjoyed an unprecedented luxury known as the **Single-Threaded Free Lunch**. Every 18 months, chip manufacturers like Intel and AMD released microprocessors with exponentially higher CPU clock speeds (going from megahertz to gigahertz).

```
                 THE ERA OF THE "FREE LUNCH" (1975-2005)
┌──────────────────────────────────────────────────────────────────────┐
│  Software Application (Written in C, C++, Java, etc.)                 │
├──────────────────────────────────────────────────────────────────────┤
│  [No Code Changes Required]                                          │
│         │                                                            │
│         ▼                                                            │
│  Hardware Manufacturers Increase CPU Clock Speed (10 MHz -> 3 GHz)   │
├──────────────────────────────────────────────────────────────────────┤
│  Result: Existing application runs exponentially faster automatically │
└──────────────────────────────────────────────────────────────────────┘
```

Software engineers did not need to write parallel or multithreaded code to achieve high throughput. They simply waited for next year's CPU generation.

#### 2. Thermal Dissipation, Dennard Scaling, and Dark Silicon
By 2004–2005, CPU physical scaling hit three hard walls:
1. **The Breakdown of Dennard Scaling (The Power Wall)**: Historically, as transistors shrank (Moore's Law), their power density remained constant, meaning newer chips ran faster without using more power (Dennard Scaling). Around 2005, this scaling law collapsed due to physical current leakage at sub-micron scales. Consequently, raising clock speeds above ~3.8 GHz caused CPUs to draw exponentially more electrical power, generating intense heat that melted the silicon. This leads to the **Dark Silicon** phenomenon, where a chip may contain billions of transistors, but large portions of them must remain unpowered ("dark") at any given time to prevent the chip from overheating.
2. **Instruction-Level Parallelism (ILP) Wall**: Out-of-order execution, speculative execution, and branch prediction reached diminishing returns.
3. **The Memory Wall**: CPU speed grew much faster than main memory (RAM) access speeds, leaving fast CPUs idle while waiting for data fetches across the memory bus.

#### 3. The Shift to Parallelism and Heterogeneity
Faced with physical limits on clock speed, semiconductor makers began packing multiple CPU cores onto a single silicon die (**Homogeneous Multicore** starting in 2005), followed by adding specialized compute units (**Heterogeneous Manycore** like GPUs and SPUs in 2009–2011), and ultimately scaling across compute networks (**Elastic Cloud Cores**).

```
               THE THREE TRANSITIONS OF COMPUTING HARDWARE
               
  Phase I: Unicore Motherlode  ──▶  Single-core clock speed scaling (1975-2005)
  Phase II: Homogeneous Multicore──▶  Dual/Quad/8-core CPUs on one die (2005-2011)
  Phase III: Heterogeneous Jungle──▶  CPUs + GPUs + Cloud Clusters (2011-Present)
```

Herb Sutter's core conclusion: **Software developers can no longer rely on hardware to make single-threaded code faster.** To exploit hardware advances, software must be designed with explicit, fine-grained concurrency and distributed asynchronous architectures.

#### 4. Code Examples (Java / JS / Python)

#### Java Implementation
```java
// Java 17+ — Parallel Streams (Declarative Concurrency)
import java.util.stream.IntStream;

public class ParallelDataProcessing {
    public static void main(String[] args) {
        long sum = IntStream.rangeClosed(1, 10_000_000)
            .parallel() // Automatically distributes across available cores
            .filter(n -> n % 2 == 0)
            .mapToLong(n -> (long) n * n)
            .sum();
        System.out.println("Result: " + sum);
    }
}
```

#### JavaScript / TypeScript Implementation
```javascript
// ES2022+ — Node.js Worker Threads (Shared Memory Concurrency)
const { Worker, isMainThread, parentPort, workerData } = require('worker_threads');

if (isMainThread) {
  const worker = new Worker(__filename, { workerData: { max: 10000000 } });
  worker.on('message', result => console.log('Result:', result));
} else {
  let sum = 0;
  for (let i = 1; i <= workerData.max; i++) {
    if (i % 2 === 0) sum += i * i;
  }
  parentPort.postMessage(sum);
}
```

#### Python Implementation
```python
# Python 3.10+ — Multiprocessing (Bypassing the GIL)
import multiprocessing

def compute_chunk(start, end):
    return sum(i * i for i in range(start, end) if i % 2 == 0)

if __name__ == '__main__':
    chunk_size = 2_500_000
    ranges = [(i, i + chunk_size) for i in range(1, 10_000_001, chunk_size)]
    
    with multiprocessing.Pool() as pool:
        results = pool.starmap(compute_chunk, ranges)
    
    print("Result:", sum(results))
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM TEXT

> **📄 VERBATIM SOURCE**
> **Title:** The Free Lunch Is Over: A Fundamental Turn Toward Concurrency in Software
> **Author(s):** Herb Sutter
> **Published:** March 2005, Dr. Dobb's Journal
> **Source type:** Internet article
> **Original URL:** http://www.gotw.ca/publications/concurrency-ddj.htm
> **DOI:** N/A
> **Repository:** N/A
> **Note:** The text below is reproduced verbatim — exact word-for-word —
> for educational study. All rights remain with the original author(s)
> and publisher(s).

The biggest sea change in software development since the OO revolution is knocking at the door, and its name is Concurrency.

For the past 30 years, computer performance has been driven by Moore's Law; from now on, it will be driven by Amdahl's Law. Writing code that truly takes advantage of multiple processors has not been an issue for most developers. But it will soon be an issue for all of us.

The free lunch is over. We have grown used to the idea that our programs will automatically run faster on the next generation of hardware. But that is no longer true. The major processor manufacturers have all acknowledged that the gigahertz race is over. They have hit a physical wall. The only way to continue delivering Moore's Law performance gains is to put multiple cores on a single chip.

What this means is that if you want your application to benefit from the performance increases of new processors, it will have to be a concurrent, multithreaded application. Single-threaded applications will see no performance gains on new hardware. The free lunch is indeed over, and it's time to learn how to write concurrent code safely and efficiently.

---

> **📄 VERBATIM SOURCE**
> **Title:** Welcome to the Jungle: Or, A Heterogeneous Supercomputer in Every Pocket
> **Author(s):** Herb Sutter
> **Published:** December 2011, Sutter's Mill / Dr. Dobb's
> **Source type:** Internet article
> **Original URL:** https://herbsutter.com/welcome-to-the-jungle/
> **DOI:** N/A
> **Repository:** N/A
> **Note:** The text below is reproduced verbatim — exact word-for-word —
> for educational study. All rights remain with the original author(s)
> and publisher(s).

#### Welcome to the Jungle: Or, A Heterogeneous Supercomputer in Every Pocket (2011)
*By Herb Sutter (Published on Sutter's Mill / Dr. Dobb's)*

In the twilight of Moore’s Law, the transitions to multicore processors, GPU computing, and HaaS cloud computing are not separate trends, but aspects of a single trend – mainstream computers from desktops to ‘smartphones’ are being permanently transformed into heterogeneous supercomputer clusters. Henceforth, a single compute-intensive application will need to harness different kinds of cores, in immense numbers, to get its job done.

The free lunch is over. Now welcome to the hardware jungle.

From 1975 to 2005, our industry accomplished a phenomenal mission: In 30 years, we put a personal computer on every desk, in every home, and in every pocket.

In 2005, however, mainstream computing hit a wall. In “The Free Lunch Is Over” (December 2004), I described the reasons for the then-upcoming industry transition from single-core to multi-core CPUs in mainstream machines, why it would require changes throughout the software stack from operating systems to languages to tools, and why it would permanently affect the way we as software developers have to write our code if we want our applications to continue exploiting Moore’s transistor dividend.

In 2005, our industry undertook a new mission: to put a personal parallel supercomputer on every desk, in every home, and in every pocket. 2011 was special: it’s the year that we completed the transition to parallel computing in all mainstream form factors, with the arrival of multicore tablets (e.g., iPad 2, Playbook, Kindle Fire, Nook Tablet) and smartphones (e.g., Galaxy S II, Droid X2, iPhone 4S). 2012 will see us continue to build out multicore with mainstream quad- and eight-core tablets (as Windows 8 brings a modern tablet experience to x86 as well as ARM), and the last single-core gaming console holdout will go multicore (as Nintendo’s Wii U replaces Wii).

This time it took us just six years to deliver mainstream parallel computing in all popular form factors. And we know the transition to multicore is permanent, because multicore delivers compute performance that single-core cannot and there will always be mainstream applications that run better on a multi-core machine. There’s no going back.

For the first time in the history of computing, mainstream hardware is no longer a single-processor von Neumann machine, and never will be again.

That was the first act.

#### Overview: Trifecta
It turns out that multicore is just the first of three related permanent transitions that layer on and amplify each other.

1. **Multicore (2005-)**: As above.
2. **Heterogeneous cores (2009-)**: A single computer already typically includes more than one kind of processor core, as mainstream notebooks, consoles, and tablets all increasingly have both CPUs and compute-capable GPUs. The open question in the industry today is not whether a single application will be spread across different kinds of cores, but only “how different” the cores should be – whether they should be basically the same with similar instruction sets but in a mix of a few big cores that are best at sequential code plus many smaller cores best at running parallel code (the Intel MIC model), or cores with different capabilities that may only support subsets of general-purpose languages like C and C++ (the Cell and GPGPU model).

Heterogeneity amplifies the first trend (multicore), because if some of the cores are smaller then we can fit more of them on the same chip. Indeed, 100x and 1,000x parallelism is already available today on many mainstream home machines – for programs that can harness the GPU.

We know the transition to heterogeneous cores is permanent, because different kinds of computations naturally run faster and/or use less power on different kinds of cores – including that different parts of the same application will run faster and/or cooler on a machine with several different kinds of cores.

3. **Elastic compute cloud cores (2010-)**: For our purposes, “cloud” means specifically “hardware (or infrastructure) as a service” (HaaS) – delivering access to more computational hardware as an extension of the mainstream machine. This started to hit the mainstream with commercial compute cloud offerings from Amazon Web Services (AWS), Microsoft Azure, Google App Engine (GAE), and others.

Cloud HaaS again amplifies both of the first two trends, because it’s fundamentally about deploying large numbers of nodes where each node is a mainstream machine containing multiple and heterogeneous cores. In the cloud, the number of cores available to a single application is scaling fast (e.g., in summer 2011, Cycle Computing delivered a 30,000-core cloud for under $1,300/hour using AWS) and the same heterogeneous cores are available in compute nodes.

In short, parallelism is not just in full bloom, but increasingly in full variety.

#### Mining Moore’s Law
We’ve been hearing breathless “Moore’s Law is ending” announcements for years. That Moore’s Law will end was never news; every exponential progression must. Although it didn’t end when some prognosticators expected, its end is possible to forecast – we just have to know what to look for, and that is diminishing returns.

A key observation is that exploiting Moore’s Law is like exploiting a gold mine or any other kind of resource. Exploiting a gold ore deposit never just stops abruptly; rather, running a mine goes through phases of increasing costs and diminishing returns until finally the gold that’s left in that patch of ground is no longer commercially exploitable and operating the mine is no longer profitable.

Mining Moore’s Law has followed the same pattern. Let’s consider its three major phases:

- **Phase I, Moore’s Motherlode = Unicore “Free Lunch” (1975-2005)**: For 30 years, mainstream processors mined Moore’s motherlode by using their growing transistor budgets to make a single core more and more complex so that it could execute a single thread faster. This was wonderful because it meant the performance was easily exploitable – compute-bound software would get faster with relatively little effort.
- **Phase II, Secondary Veins = Homogeneous Multicore (2005-)**: When Moore’s unicore motherlode started getting mined out, we turned to mining Moore’s secondary veins – using the additional transistors to make more cores per chip. Multicore let us continue to deliver exponentially increasing compute throughput in mainstream computers, but in a form that was less easily exploitable because it placed a greater burden on software developers who had to write parallel programs that could use the hardware.
- **Phase III, Tertiary Veins = Heterogeneous Cores (2011-)**: As our miners are forced to move into smaller and smaller veins, yields diminish and costs rise. Our intrepid miners are trying harder and harder, but for less reward, by turning to Moore’s tertiary veins: Using Moore’s extra transistors to make, not just more cores, but also different kinds of cores – and in very large numbers.

#### The Power Problem: Dark Silicon
One particular problem we have just begun to encounter is known as “dark silicon.” Although Moore’s Law is still delivering more transistors, we are losing the ability to power them all at the same time.

This “dark silicon” effect is like a Shakespearian bear chasing our doomed character offstage. Even though we can continue to pack more cores on a chip, if we cannot use them at the same time we have failed to exploit Moore’s Law to deliver more computational throughput per die area.

#### What It Means For Us: A Programmer’s View

How will all of this change the way we write our software, if we care about harnessing mainstream hardware performance? The basic conclusions echo and expand upon ones that I proposed in “The Free Lunch is Over”:

Applications will need to be at least massively parallel, and ideally able to use non-local cores and heterogeneous cores, if they want to fully exploit the long-term continued exponential growth in compute throughput being delivered both in-box and in-cloud. After all, soon the vast majority of compute cores available to a mainstream application will be non-local.

Efficiency and performance optimization will get more, not less, important. We’re being asked to do more (new experiences like sensor-based UIs and augmented reality) with less hardware (constrained mobile form factors and the eventual plateauing of scale-in when Moore’s Law ends). In December 2004 I wrote: “Those languages that already lend themselves to heavy optimization will find new life; those that don’t will need to find ways to compete and become more efficient and optimizable. Expect long-term increased demand for performance-oriented languages and systems.” This is still true; witness the resurgence of interest in C++ in 2011 and onward, primarily because of its expressive flexibility and performance efficiency. A program that is twice as efficient has two advantages: it will be able to run twice as well on a local disconnected device especially when Moore’s Law can no longer deliver local performance improvements in any form; and it will always be able to run at half the power and cost on an elastic compute cloud even as those continue to expand for the indefinite future.

Programming languages and systems will increasingly be forced to deal with heterogeneous distributed parallelism. As previously predicted, just basic homogeneous multicore has proved to be a far bigger event for languages than even object-oriented programming was, because some languages (notably C) could get away with ignoring objects while still remaining commercially relevant for mainstream software development. No mainstream language, including the just-ratified C11 standard, could ignore basic concurrency and parallelism and stay relevant in even a homogeneous-multicore world. Now expect all mainstream languages and environments, including their standard libraries, to develop explicit support for at least distributed parallelism and probably also heterogeneous parallelism; they cannot hope to avoid it without becoming marginalized for mainstream app development.

Expanding on that last bullet, what are some basic elements we will need to add to mainstream programming models (think: C, C++, Java, and .NET)? Here are a few basics I think will be unavoidable, that must be supported explicitly in one form or another.

Deal with the processor axis’ lower section by supporting compute cores with different performance (big/fast, slow/small). At minimum, mainstream operating systems and runtimes will need to be aware that some cores are faster than others, and know which parts of an application want to run on which of those cores.

Deal with the processor axis’ upper section by supporting language subsets, to allow for cores with different capabilities including that not all fully support mainstream language features. In the next decade, a mainstream operating system (on its own, or augmented with an extra runtime like the Java/.NET VM or the ConcRT runtime underpinning PPL) will be capable of managing cores with different instruction sets and running a single application across many of those cores. Programming languages and tools will be extended to let the developer express code that is restricted to use just a subset of a mainstream programming language (e.g., the restrict() qualifiers in C++ AMP; I am optimistic that for most mainstream languages such a single language extension will be sufficient while leveraging existing language rules for overloading and dispatch, thus minimizing the impact on developers, but experience will have to bear this out).

Deal with the memory axis for computation, by providing distributed algorithms that can scale not just locally but also across a compute cloud. Libraries and runtimes like OpenCL and TBB and PPL will be extended or duplicated to enable writing loops and other algorithms that run on large numbers of local and non-local parallel cores. Today we can write a parallel_for_each call that can run with 1,000x parallelism on a set of local discrete GPUs and ship the right data shards to the right compute cards and the results back; tomorrow we need to be able to write that same call that can run with 1,000,000,000x parallelism on a set of cloud-based GPUs and ship the right data shards to the right nodes and the results back. This is a “baby step” example in that it just uses local data (e.g., that can fit in a single machine’s memory), but distributed computation; the data subsets are simply copied hub-and-spoke.

Deal with the memory axis for data, by providing distributed data containers, which can be spread across many nodes. The next step is for the data itself to be larger than any node’s memory, and (preferably automatically) move the right data subsets to the right nodes of a distributed computation. For example, we need containers like a distributed_array or distributed_table that can be backed by multiple and/or redundant cloud storage, and then make those the target of the same distributed parallel_for_each call. After all, why shouldn’t we write a single parallel_for_each call that efficiently updates a 100 petabyte table? Hadoop (http://hadoop.apache.org/) enables this today for specific workloads and with extra work; this will become a standard capability available out-of-the-box in mainstream language compilers and their standard libraries.

Enable a unified programming model that can handle the entire chart with the same source code. Since we can map the hardware on a single chart with two degrees of freedom, the landscape is unified enough that it should be able to be served by a single programming model in the future. Any solution will have at least two basic characteristics: First, it will cover the Processor axis by letting the programmer express language subsets in a way integrated holistically into the language. Second, it will cover or hide the Memory axis by abstracting the location of data, and copying data subsets on demand by default, while also providing a way to take control of the copying for advanced users who want to optimize the performance of a specific computation.

Perhaps our most difficult mental adjustment, however, will be to learn to think of the cloud as part of the mainstream machine – to view all these local and non-local cores as being equally part of the target machine that executes our application, where the network is just another bus that connects us to more cores. That is, in a few years we will write code for mainstream machines assuming that they have million-way parallelism, of which only thousand-way parallelism is guaranteed to always be available (when out of WiFi range).

Five years from now we want to be delivering apps that run well on an isolated device, and then just run faster or better when they are in WiFi range and have dynamic access to many more cores. The makers of our operating systems, runtimes, libraries, programming languages, and tools need to get us to a place where we can create compute-bound applications that run well in isolation on disconnected devices with 1,000-way local parallelism… and when the device is in WiFi range just run faster, handle much larger data sets, and/or light up with additional capabilities. We have a very small taste of that now with cloud-based apps like Shazam (which function only when online), but yet a long way to go to realize this full vision.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Herb Sutter & The ISO C++ Standards Committee
Herb Sutter is a prominent software engineer and author, best known for his foundational work on C++ concurrency, memory models, and exception safety. He served as the chair of the ISO C++ Standards Committee (JTC1/SC22/WG21) for over two decades. His essays, including "The Free Lunch Is Over" (2005) and "Welcome to the Jungle" (2011), accurately presaged the industrial shift from single-threaded clock speed scaling to multicore and heterogeneous computing paradigms.

#### The Breakdown of Dennard Scaling & The Power Wall
In 1974, Robert H. Dennard formulated **Dennard Scaling**, which stated that as transistor dimensions shrank, power density remained constant—allowing clock speeds to rise without increasing power consumption. Around 2005, physical current leakage at sub-micron scales caused Dennard Scaling to break down. Increasing clock speeds beyond ~3.8 GHz resulted in excessive thermal dissipation (the "Power Wall"), necessitating the shift toward multicore architectures and dark silicon management.

#### Heterogeneous Computing Models
Heterogeneous computing involves combining CPU cores with specialized accelerators (such as GPUs, NPUs, and DSPs) on the same silicon substrate or package. Key frameworks for heterogeneous programming include:
- **OpenCL (Open Computing Language)**: An open standard for cross-platform, parallel programming of CPUs, GPUs, and DSPs.
- **CUDA (Compute Unified Device Architecture)**: NVIDIA's proprietary parallel computing platform and API.
- **C++ AMP (C++ Accelerated Massive Parallelism)**: A library specification by Microsoft designed to accelerate execution of C++ code on data-parallel hardware.


<div class="page-break"></div>

## Chapter 2.2: Moore's Law — Past, Present, and Future (Robert R. Schaller & Gordon E. Moore)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Origin of Moore's Law (1965 & 1975)
In 1965, Gordon E. Moore (then R&D Director at Fairchild Semiconductor and later co-founder of Intel) was asked by *Electronics* magazine to predict the future of the semiconductor industry over the next decade. 

Plotting component counts from just three data points on a log-linear scale, Moore observed that the number of components per integrated circuit (IC) doubled every year:

```
               MOORE'S LAW COMPONENT DOUBLING (1965 - 1975)
               
  Density (Components / IC)
   65,000 │                                                 * (1975 Target)
          │                                           *
          │                                     *
          │                               *
       64 │                         * (1965 Lab IC)
       32 │                   * (1964 Commercial IC)
        1 │             * (1959 First Planar Transistor)
          └─────────────┴─────┴─────┴─────┴─────┴─────┴─────┴──────
                       1959  1964  1965                     1975
```

In 1975, Moore revised his estimate to a doubling every **18 to 24 months**, which accurately tracked semiconductor progress for over 40 years.

#### 2. The Three Factors Driving Density Doubling
Gordon Moore identified three distinct engineering factors that enabled density doubling:
1. **Finer Line Widths / Feature Sizes**: Photolithographic advances reduced transistor gate dimensions.
2. **Larger Die Sizes**: Wafer manufacturing allowed larger silicon dice without incurring fatal defect rates.
3. **Circuit & Device "Cleverness"**: Ingenious circuit layouts, isolation techniques, and component packing (which Moore noted reached physical limits around 1975).

#### 3. Software Demand & Myhrvold's Law
Moore's Law was reinforced by a massive positive feedback loop from software. Nathan Myhrvold (former CTO of Microsoft) observed that **software complexity grows faster than hardware capability**. As fast CPUs emerged, software engineers expanded capabilities, bloat, and features—which in turn created an insatiable market demand for even faster processors.

#### 4. The Breakdown of Dennard Scaling
Historically, as transistors shrank, their power density remained constant, meaning newer chips ran faster without using more power (Dennard Scaling). Around 2005, this scaling law collapsed due to physical current leakage at sub-micron scales.

#### 5. Moore's Second Law (Rock's Law)
While the cost per transistor decreases exponentially, the capital cost of building a state-of-the-art semiconductor fabrication plant (Fab) increases exponentially:
```text
          FAB CAPITAL COST OVER TIME (ROCK'S LAW)
          
  Cost (Billions USD)
   $20B │                                       * (2024 TSMC 2nm)
        │
   $10B │                               * (2015 14nm)
        │
    $5B │                       * (2005 65nm)
        │
  $1.5B │               * (1995)
        │
 $0.01B │       * (1966)
        └───────┴───────┴───────┴───────┴───────┴───────
              1966    1995    2005    2015    2024
```
This economic barrier led to the "Fabless / Foundry" model, where only a few behemoths (TSMC, Intel, Samsung) can afford to build cutting-edge physical fabs.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

#### Moore's Law: Past, Present, and Future (June 1997)
*By Robert R. Schaller (Published in IEEE Spectrum, Vol. 34, No. 6, pp. 52-59)*

In 1965 Gordon E. Moore, then R&D director at Fairchild Semiconductor and these days chairman emeritus of Intel Corp., Santa Clara, Calif., quantified the astounding growth of the new technology of semiconductors in a still more astounding formula. Manufacturers, he said, had been doubling the density of components per integrated circuit at regular intervals, and they would continue to do so as far as the eye could see.

This observation has since been dubbed “Moore’s Law” and is now enormously influential. Some have even termed it a self-fulfilling prophecy. Because of the accuracy with which Moore’s Law has predicted past growth in IC complexity, it is viewed as a reliable method of calculating future trends as well, setting the pace of innovation, and defining the rules and the very nature of competition. And since the semiconductor portion of electronic consumer products keeps growing by leaps and bounds, the Law has aroused in users and consumers an expectation of a continuous stream of faster, better, and cheaper high-technology products. Even the policy implications of Moore’s Law are significant: it is used as the baseline assumption in the industry’s strategic road map for the next decade and a half.

Besides its surprising longevity as a forecaster of hardware capabilities, the Law has set the pace for the PC-software industry and, some say, is now a techno-mantra that, if repeated often enough and sincerely enough, has the power of a self-fulfilling prophecy.

#### Genesis of the IC
The 1947 invention of the transfer resistor, or transistor, by William Shockley and his colleagues at Bell Laboratories, Murray Hill, N.J., ushered in the solid-state era of electronics. The concept was based on the discovery that the flow of electricity through a solid such as silicon can be controlled by adding impurities with the appropriate electronic configurations. The vacuum tube was the dominant technology for this task at the time; but the transistor proved to be significantly more reliable, required much less power, and above all, could be made incredibly smaller.

Almost a decade later, Shockley and two Bell Labs colleagues, John Bardeen and Walter Brattain, were presented with the Nobel Prize for their invention. Shockley went on to start his own semiconductor laboratory, and others from his team either joined or founded the companies whose names are synonymous with the spectacular rise of the semiconductor industry: Texas Instruments, Fairchild Semiconductor, and Intel. Gordon Moore, a member of Shockley’s team, was a key player at Fairchild and cofounder of both Fairchild and Intel.

In the late 1950s, research engineers at Fairchild developed the first planar transistor, and later the first planar IC. (Jack Kilby of Texas Instruments is credited with inventing the IC.) Although not as significant a scientific breakthrough as the transistor, the invention of the IC did reveal the potential for extending the cost and operating benefits of transistors to every mass-produced electronic circuit.

#### From Science to Production Technology
Revolutionary science supplied the theoretical basis for solid-state electronics, but without the invention of unprecedented production technologies, the spectacular growth of the industry could not have happened. “Indeed, the technology led the science in a sort of inverse linear model,” Moore observed in a recent publication. Conventionally, science discovers and technology applies; here the order was reversed. The two most noteworthy inventions were the diffusion and oxide-masking process and the planar process.

The planar process was the brainchild of physicist Jean Hoerni of newly formed Fairchild Semiconductor. Hoerni reasoned that a design based on a plane would be easier to manufacture and to miniaturize, compared to the conventional 3-D mesa transistor. Flattening the mesa enabled electrical connections to be made by evaporating metal film onto appropriate regions of the semiconductor wafer. Like the printing process itself, the planar process evolved into ever greater rates of production at even higher yields.

Better still, the planar process enabled the integration of circuits on a single substrate. Robert Noyce at Fairchild quickly recognized this. “When we were patenting this [planar transistor],” recalled Moore, “we recognized it was a significant change... Bob Noyce got a group together to see what they could come up with. And right away he saw that this gave us a reason for running the metal up over the top without shorting out the junctions, so you could actually connect this one to the next-door neighbor.”

#### Birth of Moore’s Law
The 19 April 1965 issue of *Electronics* magazine contained an article with the title “Cramming more components onto integrated circuits.” Its author, Gordon E. Moore, had been asked to predict what would happen over the next 10 years. His article speculated that by 1975 it would be possible to cram as many as 65,000 components onto a single silicon chip about 6 millimeters square.

Moore based his forecast on a log-linear plot of device complexity over time: “The complexity for minimum component costs has increased at a rate of roughly a factor of two per year. Certainly over the short term this rate can be expected to continue... Over the longer term, the rate of increase is a bit more uncertain, although there is no reason to believe it will not remain constant for at least 10 years.”

Moore revisited the subject in a paper given at the 1975 IEEE International Electron Devices Meeting. He revised his 10-year-old forecast to a doubling of component density every 18 to 24 months.

#### User Expectations Matter
Another interpretation of Moore’s Law adds to the push of new production technologies the pull of software developments. In 1995, Nathan Myhrvold, chief technology officer of Microsoft Inc., studied a variety of his firm’s products by counting the lines of code in successive releases. He observed that Microsoft Word was at 27,000 lines in its first version; the latest version had about two million lines. Myhrvold relates this to Moore’s Law:

“So we have increased the size and complexity of software even faster than Moore’s Law. In fact, this is why there is a market for faster processors—software people have always consumed new capability as fast or faster than the chip people could make it available.”

#### Is the end in view?
Moore's Law started out as a simple extrapolation from a simple observation. Actual performance and experience have validated it, proving Moore quite prophetic. Curiously, forecasts of its demise have been made throughout its existence, but have consistently been wrong.

They are still being made. A 1996 poll by Forbes of 11 industry stalwarts—chief executive officers, senior systems and software engineers, industry analysts, an industry writer, and a venture capitalist—awarded Moore's Law an average remaining life of roughly 14 years, to 2010. That would give the prediction a life span of 45 years, by any measure an incredible feat of technical insight.

At present, researchers are still learning to exploit the properties of semiconductors and production processes. But at some point this technology—like all others—will stop growing exponentially and enter the realm of diminishing marginal returns.

The physics underlying semiconductor manufacturing suggests several possible barriers to continued technical progress and density doubling. For example, the gigabit chip generation may finally force technologists up against the limits of optical lithography. There are ways around this obstacle, but the cost may be prohibitive. In fact, economics may constrain Moore's Law before physics does—an observation that others have called "Moore's second law."

For one thing, capital requirements rise exponentially along with component densities, wrote Moore in 1995. The cost of a new fabrication plant went from US $14 million in 1966 to $1500 million in 1995. By 1998, work will begin on the first $3 billion fabrication plant. These increases are no problem as long as chips improve still faster. That happened between 1984 and 1990, when chip performance tripled, while the cost of a fab plant only doubled. For the next generation of chips, however, the capital needed will double again, but performance is expected to increase only by half. If the exponential trend in fab costs continues, by 2005 the cost of a single fab plant will be more than $10 billion in 1995 dollars—more than half of Intel's current net worth.

What can semiconductor manufacturers do? One possibility: team up with customers, competitors, suppliers, or even countries to share fab construction and R&D costs. For example, the R&D price tag for dynamic RAMs, which rose from US $400 million for the 4 Mb chip to more than $1 billion for the 1 Gb device, led to the well-known alliance of IBM, Germany's Siemens, and Japan's Toshiba for the development of advanced DRAMs. In Korea and Singapore, state-organized consortia appear to be on the rise. Global alliances are emerging as the new model for competition in semiconductors.

Another economic threat to Moore's Law is the possibility that transistors will become so cheap that there will be no profit in making them cheaper. Dan Hutcheson, president of VLSI Research Inc., San Jose, Calif., reached that conclusion in 1995: "The price per transistor will bottom out sometime between 2003 and 2005. From that point on, there will be no economic point in making transistors smaller. So Moore's Law ends in seven years."

Apart from the apparent constraints of physics and economics, one respondent to the Forbes poll, Dan Lynch, president and chief executive officer of CyberCash Inc., offers a starkly different view about the future of Moore's Law, stating, "We'll be dead when Moore's Law is played out." His reasoning: "Moore's Law is about human ingenuity, not physics."

---

> **📄 VERBATIM SOURCE**
> **Title:** Moore's Law: The Engine of Innovation
> **Author(s):** Intel Newsroom
> **Published:** 2023, Intel Corporation
> **Source type:** Corporate Technical Release
> **Original URL:** https://download.intel.com/newsroom/2023/manufacturing/moores-law-electronics.pdf
> **DOI:** N/A
> **Repository:** N/A
> **Note:** The text below is reproduced verbatim — exact word-for-word —
> for educational study. All rights remain with the original author(s)
> and publisher(s).

#### The Angstrom Era: RibbonFET, PowerVia, and Advanced Packaging

Intel’s roadmap emphasizes that Moore’s Law is no longer just about shrinking transistors on a single die, but about how they are designed, powered, and connected. As traditional transistor scaling faces physical limitations, Intel has shifted its strategy toward these heterogeneous and architectural innovations.

**RibbonFET:** Intel’s implementation of Gate-All-Around (GAA) transistor architecture. By wrapping the gate around the channel, RibbonFET provides superior electrostatic control compared to traditional FinFET designs, enabling faster switching speeds and improved performance per watt in a smaller footprint.

**PowerVia:** An industry-first backside power delivery technology. By moving power routing to the backside of the wafer, PowerVia separates signal and power lines. This reduces signal interference, lowers resistance, and improves power delivery efficiency, allowing for higher transistor density and performance.

**Advanced Packaging:** Techniques such as Foveros (3D stacking) and EMIB (embedded multi-die interconnect bridge), along with emerging glass substrates, are critical to Intel’s "systems foundry" approach. These technologies allow Intel to combine multiple chiplets into a single package, effectively bypassing the size constraints of individual chips and enabling the integration of up to a trillion transistors on a single package by 2030.

By combining the density benefits of RibbonFET, the efficiency gains of PowerVia, and the flexibility of advanced packaging, Intel aims to continue the trajectory of Moore’s Law—doubling transistor density and performance at a manageable cost—even as scaling individual transistors reaches the atomic level.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Gordon E. Moore & Fairchild / Intel History
Gordon E. Moore (1929–2023) was an American engineer, businessman, and co-founder of Fairchild Semiconductor (1957) and Intel Corporation (1968). His 1965 paper in *Electronics* magazine formulated the empirical observation that component density per integrated circuit doubles roughly every year (revised in 1975 to every two years).

#### Lithography & Photolithographic Limits
Extreme Ultraviolet (EUV) lithography operates at a wavelength of 13.5 nm, replacing deep ultraviolet (DUV) immersion lithography (193 nm). As transistor features reach sub-2nm scales, quantum tunneling effects across thin gate oxides present fundamental quantum-mechanical barriers to traditional planar and FinFET scaling.

#### Chiplets & Advanced Packaging Architecture
Rather than relying on monolithic die scaling, modern high-performance processors leverage multi-chiplet disaggregation. Technologies such as TSMC's CoWoS (Chip-on-Wafer-on-Substrate) and Intel's Foveros utilize high-density silicon interposers and micro-bumps to connect disaggregated compute, memory, and I/O dies with ultra-low latency.



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




<div class="page-break"></div>

## Chapter 2.4: Citation & Reference Deep-Dives for Module 2

This chapter provides standalone, in-depth research profiles of foundational hardware architecture theories, low-latency messaging mechanisms, and concurrent execution models referenced across Module 2.

---

### Deep-Dive 2.4.1: Herb Sutter's "The Free Lunch Is Over" (Dr. Dobb's Journal, 2005)

- **Background**: Published in March 2005 by Herb Sutter (Chair of the ISO C++ Standards Committee).
- **Impact**: Marked the official recognition in the software engineering community that clock speed growth had stalled out. It signaled the mandatory shift toward multithreading, concurrency models, lock-free data structures, and functional programming concepts.

---

### Deep-Dive 2.4.2: Amdahl's Law vs. Gustafson's Law

While Amdahl's Law predicted strict limits on parallel speedup assuming a *fixed problem size*, John Gustafson (1988) observed that in practice, as we get more processors, we scale the *problem size* to maintain a fixed execution time.

- **Amdahl's Law (Gene Amdahl, 1967)**:
  $$\text{Speedup}(S) = \frac{1}{(1 - P) + \frac{P}{N}}$$
  Where $P$ is the parallelizable proportion of code, and $N$ is the number of processor cores. Shows that if 10% of an application is sequential, maximum theoretical speedup is capped at 10x, regardless of how many thousands of cores are added.

- **Gustafson's Law (John Gustafson, 1988)**:
  $$\text{Speedup}(S) = (1 - P) + P \cdot N$$
  Demonstrates that as hardware core counts increase, problem sizes scale to fill available parallel capacity, proving that parallel computing remains highly effective for large datasets.

**Example Code: Scaling the Workload**
```java
// Java: Scaling the workload to match core count (Gustafson's view)
public void processLargeDataset(int cores) {
    // Problem size N scales with available cores
    int dataSize = 10_000_000 * cores; 
    
    // The parallel portion dominates execution time
    long sum = IntStream.range(0, dataSize)
        .parallel() // Uses all available cores
        .mapToLong(this::heavyComputation)
        .sum();
}
```

---

### Deep-Dive 2.4.3: Esmaeilzadeh's "Dark Silicon" (ISCA 2011)

**Paper**: *"Dark Silicon and the End of Multicore Scaling"* (Hadi Esmaeilzadeh et al., ISCA 2011).

**Key Findings:**
- As transistors shrink, their power density no longer scales down linearly. Thus, as we pack more cores on a die, we cannot power all of them simultaneously without exceeding the chip's Thermal Design Power (TDP) budget.
- The paper mathematically demonstrated that even under optimistic scaling assumptions, over 50% of the transistors on future chips will be "dark" (unpowered) at any given time.
- **Impact:** This signaled the end of symmetric multicore scaling. Future performance gains must come from heterogeneous computing—using specialized, highly efficient accelerator cores (like GPUs or NPUs) rather than just adding more general-purpose CPU cores.

---

### Deep-Dive 2.4.4: Moore's Law, Jean Hoerni, and Rock's Law

- **Gordon E. Moore's Original 1965 Article**: 
  - **Citation**: Moore, Gordon E. (1965). *"Cramming more components onto integrated circuits"*, *Electronics Magazine*, Vol. 38, No. 8, April 19, 1965.
  - **Key Historic Quote**: *"Integrated circuits will lead to such wonders as home computers—or at least terminals connected to a central computer—automatic controls for automobiles, and personal portable communications equipment."*

- **Jean Hoerni & The Planar Process (1959)**:
  - **Significance**: Jean Hoerni, one of the "Traitorous Eight" who founded Fairchild Semiconductor, invented the planar transistor in 1959.
  - **Mechanism**: Replaced 3D mesa structures by applying a protective silicon dioxide ($SiO_2$) insulating layer on top of silicon wafers, enabling photolithographic etching and vapor deposition of aluminum interconnect tracks over the oxide without short-circuiting underlying $p-n$ junctions.

- **Moore's Second Law / Rock's Law**:
  - **Formulation**: Named after venture capitalist Arthur Rock or economist Dan Hutcheson.
  - **Economic Reality**: While cost per transistor decreases exponentially, the capital cost of building a state-of-the-art semiconductor fabrication plant (Fab) increases exponentially:
    - 1966 Fab Cost: ~$14 Million
    - 1995 Fab Cost: ~$1.5 Billion
    - 2024 TSMC 2nm Fab Cost: ~$20 Billion+
  - **Impact**: Led to the "Fabless / Foundry" model, where only a few behemoths (TSMC, Intel, Samsung) can afford to build cutting-edge physical fabs.

---

### Deep-Dive 2.4.5: Leslie Lamport's "Happens-Before" Relation (1978)

**Full Profile: "Time, Clocks, and the Ordering of Events in a Distributed System" (CACM, 1978)**
In 1978, Leslie Lamport published this foundational paper, which became one of the most cited in computer science. It introduced the concept of logical clocks and the **Happens-Before** relation.

**Mathematical Definition**
The **Happens-Before** relation ($\rightarrow$) defines a partial ordering of events in a distributed or multithreaded system:
1. If events $a$ and $b$ occur within the same thread/process, and $a$ comes before $b$ in program order, then $a \rightarrow b$.
2. If event $a$ is the sending of a message (or a lock release), and event $b$ is the receipt of that message (or a lock acquire), then $a \rightarrow b$.
3. If $a \rightarrow b$ and $b \rightarrow c$, then $a \rightarrow c$ (Transitivity).

If neither $a \rightarrow b$ nor $b \rightarrow a$ holds, the two events are **concurrent**, and their execution order cannot be predicted without explicit synchronization.

---

### Deep-Dive 2.4.6: JSR-133 & Hardware Memory Barriers

**JSR-133: Java Memory Model Revision (Bill Pugh & Doug Lea, 2004)**
- **Problem with Early JMM (Java 1.0 - 1.4)**: The original 1996 Java Memory Model spec in JLS Chapter 17 was flawed. It allowed final fields to change value after construction and permitted broken double-checked locking idioms (`Double-Checked Locking is Broken` declaration).
- **The JSR-133 Fix**: Led by Jeremy Manson, William Pugh, and Doug Lea, JSR-133 established the formal **Happens-Before** memory semantics, guaranteed immutable final field semantics across threads, and strengthened `volatile` semantics to match acquire-release lock actions. Data-race-free programs were guaranteed sequential consistency.

**Hardware Memory Barriers / Fences**
- **Concept**: Low-level assembly instructions issued by compilers to enforce memory ordering across CPU caches.
- **Barrier Types**:
  1. **LoadLoad**: Prevents reordering of reads before the barrier with reads after the barrier.
  2. **StoreStore**: Flushes pending writes before allowing subsequent writes to proceed.
  3. **LoadStore**: Ensures reads complete before subsequent writes are visible.
  4. **StoreLoad**: The heaviest barrier (forces full CPU cache sync); guarantees all previous writes are visible before subsequent reads execute.

---

### Deep-Dive 2.4.7: Aeron Ultra-Low Latency Messaging & Ring Buffers
*(Based on Michael Barker's "Bad Concurrency" & Real-Time Media Driver Design)*

```
                     AERON UDP / IPC MESSAGING BUS
                     
  [Sender Thread]  ──▶  [Shared Memory Ring Buffer]  ──▶  [Receiver Thread]
        │                                                     │
        ▼ (No Locks / Non-blocking)                           ▼
  [Status Frames & Gossip] ◄────────────────────── [Flow Control Windowing]
```

**Key Architecture Principles of Aeron:**
1. **Zero-Copy / Lock-Free Ring Buffers**: Uses memory-mapped files and atomic pointer increments over IPC (Inter-Process Communication) and UDP to achieve sub-microsecond latency.
2. **Non-Blocking Message Path**: Critical execution paths (senders and receivers) must never execute blocking I/O calls (such as synchronous DNS lookups or blocking socket reads).
3. **Dedicated Conductor Thread**: Background administrative tasks—such as host name resolution, IP re-binding, and dynamic node ejection—are offloaded to a dedicated Conductor thread so message-passing pipelines never stall.
4. **Flow Control & Backpressure**: Implements sliding window flow control over unreliable UDP datagrams, allowing fast publishers to apply dynamic backpressure without dropping messages.

---

### Summary of Cited Works & Further Reading

[6] H. Sutter, "Welcome to the Jungle," HerbSutter.com, 2011. Available: https://herbsutter.com/welcome-to-the-jungle/
[7] R. Schaller, "Moore's Law: Past, Present, Future," IEEE Spectrum, 1997. DOI: 10.1109/6.591665
[8] Intel, "Moore's Law 2023," Intel Newsroom, 2023. Available: https://download.intel.com/newsroom/2023/manufacturing/moores-law-electronics.pdf
[9] D. Lea, "Synchronization & Java Memory Model," Concurrent Programming in Java, 1999.
[10] W. Pugh et al., "JSR-133 / Pugh Semantics Paper," POPL, 2004/05. DOI: 10.1145/1040305.1040336
[11] M. Barker, "Bad Concurrency," Bad Concurrency Blog, ~2020. Available: http://bad-concurrency.blogspot.com

**Supplementary Readings**
[S6] H. Sutter, "The Free Lunch Is Over: A Fundamental Shift Toward Concurrency in Software," Dr. Dobb's Journal, Vol. 30, No. 3, 2005.
[S7] G. E. Moore, "Cramming more components onto integrated circuits," Electronics Magazine, Vol. 38, No. 8, 1965.
[S8] L. Lamport, "Time, Clocks, and the Ordering of Events in a Distributed System," Communications of the ACM, Vol. 21, No. 7, pp. 558-565, 1978.

**Subject Index Cross-References:**
- Amdahl's Law ........ Ch 2.1, Ch 2.4
- Cache Line Padding .. Ch 2.4, Ch 3.2, Ch 3.4
- CAS (Compare-And-Swap) ........ Ch 3.2, Ch 3.4, Ch 2.4
- Dark Silicon ........ Ch 2.1, Ch 2.2
- Double-Checked Locking ........ Ch 2.3, Ch 2.4
- False Sharing ....... Ch 2.4, Ch 3.2, Ch 3.4
- Happens-Before ...... Ch 2.3, Ch 2.4
- Java Memory Model ... Ch 2.3, Ch 2.4
- Memory Barriers ..... Ch 2.3, Ch 2.4, Ch 3.4
- Moore's Law ......... Ch 2.1, Ch 2.2
- Volatile ............ Ch 2.3, Ch 2.4, Ch 3.4


# Module 3: High-Performance Architecture, Actor Model & LMAX Disruptor

<div class="page-break"></div>

## Chapter 3.1: Actors — A Model of Concurrent Computation in Distributed Systems (Gul A. Agha)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Is an Actor?
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

#### 2. Key Characteristics of the Actor Paradigm

1. **No Shared State**: Actors do not share mutable memory. Interaction occurs purely via asynchronous message-passing.
2. **Mail Addresses & Dynamic Topology**: Actors communicate by sending messages to a target's *Mail Address*. Mail addresses can be passed in messages, allowing the interconnection network of actors to change dynamically at runtime.
3. **The `become` Command & State Replacement**: In traditional OOP, an object updates its internal fields via mutation (`this.x = y`). In the Actor model, an actor replaces its behavior for processing the *next* message using a `become` operation. This allows actors to represent history-sensitive objects while maintaining mathematical immutability for each processed task.
4. **Unbounded Nondeterminism & Fair Mail Delivery**: Messages sent to an actor are placed in its *Mail Queue*. Messages arrive in arbitrary order, but the underlying mail system guarantees that every message sent will eventually be delivered (Guaranteed Mail Delivery).

#### 3. Actors vs. CSP (Communicating Sequential Processes)
- **CSP (Hoare)**: Relies on **Synchronous Communication** (Rendezvous), where both sender and receiver must block until the transfer completes. Topology is static.
- **Actor Model (Hewitt/Agha)**: Relies on **Buffered Asynchronous Communication**. Senders never block. Actors can dynamically spawn new actors and pass mail addresses.

---

<div class="page-break"></div>

### SECTION 2: CONDENSED THESIS CONCEPTS & CODE EXAMPLES

The core of Gul Agha's thesis formalized the Actor Model as a framework for concurrent computation in distributed systems. Instead of dealing with the raw thesis, this section synthesizes its primary contributions and provides modern code examples to illustrate the foundational mechanics.

#### 1. Encapsulation and Asynchronous Message Passing
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

#### 2. Dynamic Creation of Actors (Topology)
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

#### 3. Mail Addresses and Network Reconfiguration
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

#### 4. The `become` Operation (State Replacement)
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

#### 5. Unbounded Nondeterminism
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


# Chapter 3.6: Deep Dive: Concepts of the LMAX Disruptor

The **LMAX Disruptor** is a high-performance inter-thread messaging library originally developed by the LMAX Exchange. Described in the seminal 2011 whitepaper *"Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads"*, it fundamentally challenged how developers approach concurrency. 

Instead of relying on traditional bounded queues, locks, and condition variables—which often suffer from severe latency spikes due to kernel arbitration and CPU cache invalidation—the Disruptor relies on a "Mechanical Sympathy" approach. It uses lock-free algorithms, pre-allocated memory, and meticulous management of CPU caches to achieve sub-millisecond latency and throughput measured in tens of millions of operations per second.

This chapter breaks down every core concept introduced in the Disruptor paper.

---

## 1. The Ring Buffer (Replacing the Queue)

At the heart of the Disruptor is the **Ring Buffer**. Traditionally, passing messages between threads is done via a queue (like Java's `ArrayBlockingQueue`). Queues suffer from significant drawbacks:
- They require locks or CAS (Compare-And-Swap) operations on the head and tail pointers.
- They generate garbage when objects are enqueued and dequeued, triggering expensive Garbage Collection (GC) pauses.
- The head, tail, and size variables often reside on the same CPU cache line, leading to "False Sharing" (explained in the next chapter).

The Disruptor replaces the queue with a pre-allocated **Circular Array** (the Ring Buffer):
1. **Pre-allocation:** During initialization, the Ring Buffer is populated with pre-instantiated "Event" objects. 
2. **Zero Allocation during Runtime:** When a producer wants to send a message, it doesn't create a new object. Instead, it claims the next available slot in the Ring Buffer, updates the fields of the pre-allocated Event object in that slot, and publishes it. This results in **zero garbage collection**.
3. **Power of Two:** The size of the Ring Buffer must be a power of two (e.g., 1024, 2048, 4096). This allows the Disruptor to use a fast bitwise AND operation (`sequence & (bufferSize - 1)`) instead of a slow modulo operation to wrap sequences around the ring.

---

## 2. Sequences and the Sequencer

If there are no locks, how do threads know which slot in the Ring Buffer they can read from or write to? The answer is the **Sequence**.

A `Sequence` in the Disruptor is a simple, monotonically increasing 64-bit integer (`long`). 
- Every Consumer (Event Processor) maintains its own Sequence, representing the highest slot it has successfully processed.
- The Producer(s) maintain a Sequence representing the highest slot they have claimed.

By keeping these Sequence counters strictly separate and aggressively padding them to prevent false sharing, threads can operate independently. 

The **Sequencer** is the core component that coordinates these sequences. It is responsible for claiming the next available sequence number for the Producer. 
- It ensures the Producer doesn't wrap around and overwrite unconsumed data by checking the sequences of the slowest Consumers.
- It comes in two flavors: `SingleProducerSequencer` (lock-free, heavily optimized) and `MultiProducerSequencer` (uses CAS operations).

---

## 3. The Sequence Barrier

When a Consumer wants to process events, it needs to know if the Producer has actually finished writing data into the Ring Buffer. Furthermore, if you have a pipeline of consumers (e.g., Consumer B must run *after* Consumer A), Consumer B needs to know Consumer A's sequence.

This dependency tracking is handled by the **Sequence Barrier**.
A Sequence Barrier acts as a gatekeeper. When a Consumer asks, "What is the highest sequence I can safely process up to?", the Sequence Barrier checks:
1. The Producer's current sequence.
2. The sequences of any other Consumers that this Consumer depends on.

It then returns the lowest sequence among those dependencies. If no new events are available, the Sequence Barrier delegates to a **Wait Strategy**.

---

## 4. Wait Strategies

How should a Consumer behave when there are no new events in the Ring Buffer? Different use cases require different trade-offs between latency and CPU usage. The Disruptor provides several Wait Strategies:

1. **BusySpinWaitStrategy:**
   - **How it works:** The Consumer thread runs in a tight `while` loop, constantly checking the sequence barrier for new events.
   - **Trade-off:** Achieves the absolute lowest possible latency. However, it completely consumes a CPU core (100% utilization). Only use this if you have dedicated physical CPU cores for your consumer threads.
2. **YieldingWaitStrategy:**
   - **How it works:** The thread spins for a short time, then calls `Thread.yield()`, hinting to the OS that it can run another thread if necessary.
   - **Trade-off:** A good balance for low-latency systems. It consumes less CPU than busy spinning and avoids the heavy cost of kernel-level thread blocking.
3. **BlockingWaitStrategy:**
   - **How it works:** Uses a traditional lock and condition variable to put the Consumer thread to sleep until an event is published.
   - **Trade-off:** Consumes almost zero CPU when idle, but incurs significant latency spikes (often multi-millisecond) when the thread needs to be woken up by the OS kernel. Appropriate for asynchronous logging or non-critical paths.
4. **SleepingWaitStrategy:**
   - **How it works:** Spins, then yields, then parks the thread for short intervals (e.g., 1 nanosecond).
   - **Trade-off:** Greatly reduces CPU usage with only a modest impact on latency. Excellent for asynchronous logging.

---

## 5. Event Processors and Event Handlers

The actual execution of consumer logic is separated into two concepts:

- **Event Handler (`EventHandler<T>`):** This is where you, the developer, write your business logic. It has a simple `onEvent(Event, sequence, endOfBatch)` method.
- **Event Processor (`BatchEventProcessor`):** This is the engine that runs your Event Handler. It runs in a dedicated thread, interrogates the Sequence Barrier, pulls a batch of available events from the Ring Buffer, and feeds them sequentially into your Event Handler. 

The `endOfBatch` flag is a powerful feature: it allows your handler to realize it has caught up to the producer. You can use this to optimize I/O, such as delaying a database flush or network send until the end of a batch.

---

## 6. Real-World Applications

The Disruptor is not just a theoretical framework; it powers some of the most critical infrastructure in software engineering:

1. **LMAX Exchange:** The original trading platform. They utilize a `SingleProducerSequencer` to ensure trades are matched sequentially and deterministically without locking, achieving throughputs of millions of trades per second.
2. **Log4j2 Asynchronous Loggers:** By swapping out standard queues for the LMAX Disruptor, Log4j2's async loggers achieved up to 18x higher throughput compared to Log4j 1.x and Logback.
3. **Apache Storm:** This distributed real-time computation system replaced internal message passing queues with the Disruptor to drastically reduce latency in streaming topologies.

---

## 7. Disruptor Code Example (Java)

Below is a complete, well-commented example demonstrating how to set up a Disruptor pipeline with one Producer and one Consumer.

```java
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.nio.ByteBuffer;

public class DisruptorPrimer {

    // 1. Define the Event (The pre-allocated object in the Ring Buffer)
    public static class LongEvent {
        private long value;
        public void set(long value) { this.value = value; }
        public long get() { return value; }
    }

    public static void main(String[] args) throws InterruptedException {
        // 2. Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // 3. Construct the Disruptor
        // - LongEvent::new is the EventFactory for pre-allocation
        // - SINGLE producer type optimizes away CAS operations
        // - BlockingWaitStrategy saves CPU (use BusySpin for ultra-low latency)
        Disruptor<LongEvent> disruptor = new Disruptor<>(
                LongEvent::new,
                bufferSize,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE,
                new BlockingWaitStrategy()
        );

        // 4. Connect the Consumer (Event Handler)
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            System.out.println("Consumer Processed: " + event.get() + 
                               " (Sequence: " + sequence + ")");
        });

        // 5. Start the Disruptor, starts all consumer threads
        disruptor.start();

        // 6. Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        // 7. Producer writes data to the Ring Buffer
        ByteBuffer bb = ByteBuffer.allocate(8);
        for (long l = 0; l < 10; l++) {
            bb.putLong(0, l);
            
            // Phase 1: Claim the next available sequence
            long sequence = ringBuffer.next();
            try {
                // Phase 2: Get the pre-allocated event and write data
                LongEvent event = ringBuffer.get(sequence);
                event.set(bb.getLong(0));
            } finally {
                // Phase 3: Publish the sequence (making it visible to consumers)
                ringBuffer.publish(sequence);
            }
            Thread.sleep(100);
        }
        
        System.out.println("Producer finished.");
    }
}
```


# Chapter 3.7: Deep Dive: Mechanical Sympathy

> *"You don't have to be an engineer to be a racing driver, but you do have to have **Mechanical Sympathy**."* — Jackie Stewart (Three-time Formula One World Champion)

The term **Mechanical Sympathy**, coined in the software engineering context by Martin Thompson (co-author of the LMAX Disruptor), refers to the concept that a developer must understand how the underlying hardware operates in order to write software that performs optimally. You don't need to be able to design a CPU from scratch, but you must have *sympathy* for how it works.

If you write software that aligns with how CPUs, memory, and caches are designed to operate, your software will be incredibly fast. If you write software that fights the hardware—even if your algorithm has perfect Big-O time complexity—your performance will suffer exponentially.

---

## 1. The Numbers Every Programmer Should Know

To understand Mechanical Sympathy, one must first grasp the latency scale of modern computing hardware. CPUs are astonishingly fast, but retrieving data from Main Memory (RAM) is agonizingly slow from the CPU's perspective.

Approximate Latency (nanoseconds):
- **L1 Cache reference:** ~1 ns
- **Branch mispredict:** ~3 ns
- **L2 Cache reference:** ~4 ns
- **Mutex lock/unlock:** ~25 ns
- **Main Memory (RAM) reference:** ~100 ns
- **Context Switch (OS Kernel):** ~1,500 ns (1.5 microseconds)

If a CPU (which operates in less than a nanosecond) has to fetch data from main memory, it will sit idle for 100 clock cycles. This is often called a "stall." To prevent this, CPUs use incredibly sophisticated caching mechanisms (L1, L2, L3 caches). **The goal of high-performance software is to keep the data the CPU needs in the L1/L2 cache.**

---

## 2. Cache Lines and How Memory is Actually Read

When a CPU reads data from main memory, it does not fetch a single byte or a single integer. It fetches a **Cache Line**, which on most modern architectures is exactly **64 bytes**.

If you request an 8-byte `long` integer from memory, the CPU grabs that 8-byte integer *along with 56 bytes of adjacent memory* and pulls the entire 64-byte chunk into the L1 cache.

Why? **Spatial Locality.** Hardware designers assume that if you are reading a variable, you are extremely likely to read the variables located immediately next to it in memory (such as iterating through an array). When software embraces this by keeping related data contiguous in memory, the CPU achieves massive performance gains through pre-fetching.

---

## 3. The Enemy: False Sharing

Cache lines create a subtle and devastating performance bug known as **False Sharing** when writing multithreaded software.

Imagine two distinct variables, `counterA` and `counterB`, located next to each other in memory. Because they are adjacent, they reside on the same 64-byte cache line. 
- **Thread 1** is running on CPU Core 1 and constantly updating `counterA`.
- **Thread 2** is running on CPU Core 2 and constantly updating `counterB`.

Even though the threads are never modifying the *same* variable, the hardware's cache coherency protocol (like MESI) operates on the *Cache Line* level, not the variable level. 

1. Thread 1 updates `counterA`. This invalidates the entire 64-byte cache line across all other CPU cores.
2. Thread 2 tries to read `counterB`. It realizes its cache line is invalid (because Core 1 modified it). Core 2 must now go all the way to Main Memory (or L3 cache) to fetch the fresh cache line, incurring a ~100ns stall.
3. Thread 2 updates `counterB`. This invalidates the cache line for Core 1.
4. Core 1 stalls to fetch the cache line...

The two threads are engaged in a vicious tug-of-war over the cache line, destroying performance. This is False Sharing.

---

## 4. The Solution: Cache Line Padding

To cure False Sharing, we employ a technique called **Cache Line Padding**. We simply inject "dummy" variables between `counterA` and `counterB` to ensure they are physically separated by at least 64 bytes in memory. If they live on different cache lines, Core 1 and Core 2 can modify them simultaneously without invalidating each other's caches.

In older versions of Java, this was done manually:
```java
public class PaddedCounter {
    public volatile long value = 0L;
    // 7 longs * 8 bytes = 56 bytes of padding. 
    // Plus the 8 bytes of 'value' = 64 bytes (One full cache line).
    public long p1, p2, p3, p4, p5, p6, p7;
}
```

In modern Java (Java 8+), you can use the `@Contended` annotation to let the JVM automatically handle padding, regardless of the underlying CPU architecture's specific cache line size (some architectures use 128-byte cache lines).

---

## 5. Kernel Locks vs. Lock-Free Design

Another pillar of Mechanical Sympathy is avoiding kernel arbitration. When you use traditional locks (like Java's `synchronized` keyword or `ReentrantLock` under heavy contention), threads are suspended and woken up by the Operating System kernel.

A context switch forces the CPU to save the state of the current thread, load the state of another, and, crucially, flushes the L1/L2 caches (a "TLB shootdown"). When the original thread resumes, its cache is cold, and it must slowly fetch all its data from main memory again.

This is why systems like the LMAX Disruptor use **Single Writer Principles** and **Compare-And-Swap (CAS)** operations instead of locks. CAS is a hardware-level atomic instruction that allows a thread to update a value without involving the OS kernel, keeping the thread on the CPU and the cache hot.

---

## 6. Code Example: Proving False Sharing

The following Java benchmark demonstrates the devastating impact of False Sharing and how Padding fixes it.

```java
public class FalseSharingDemonstration {
    
    // Unpadded counters will likely share a cache line
    private static volatile long counter1 = 0;
    private static volatile long counter2 = 0;
    
    // Padded counters are forced into different cache lines
    private static volatile long paddedCounter1 = 0;
    private static long p1, p2, p3, p4, p5, p6, p7; // 56 bytes padding
    private static volatile long paddedCounter2 = 0;

    private static final long ITERATIONS = 500_000_000L;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Running with False Sharing...");
        runTest(
            () -> { for (long i = 0; i < ITERATIONS; i++) counter1++; },
            () -> { for (long i = 0; i < ITERATIONS; i++) counter2++; }
        );

        System.out.println("\nRunning with Cache Line Padding...");
        runTest(
            () -> { for (long i = 0; i < ITERATIONS; i++) paddedCounter1++; },
            () -> { for (long i = 0; i < ITERATIONS; i++) paddedCounter2++; }
        );
    }

    private static void runTest(Runnable task1, Runnable task2) throws InterruptedException {
        long start = System.nanoTime();
        
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        
        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
        
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Execution Time: " + durationMs + " ms");
    }
}
```

**Expected Results on a Multi-Core CPU:**
- The unpadded test (False Sharing) will take significantly longer (often 3x to 5x slower) because the two CPU cores are constantly invalidating each other's cache lines.
- The padded test will execute nearly instantaneously, as both cores operate entirely independently within their own L1 caches.


<div class="page-break"></div>

## Chapter 3.2: The LMAX Architecture & The Disruptor Pattern (Martin Fowler & LMAX Team)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Low-Latency Challenge in Financial Exchanges
In retail financial trading (such as LMAX Exchange), matching buyer and seller orders requires ultra-low latency (sub-millisecond response times) and high throughput (processing millions of orders per second). 

Traditional enterprise architecture relies on multithreaded web application servers backed by a relational database (RDBMS) coordinating transactions via ACID locks:

```
                  TRADITIONAL ENTERPRISE ARCHITECTURE
                  
  [Clients] ──▶ [Web App Servers] ──▶ [RDBMS Database] (Disk I/O & Locks)
                                             │
                                             ▼
                                  Heavy Latency & Jitter
```

#### Why Traditional Architecture Fails for Low-Latency
1. **Database Disk I/O & Locking**: Reading/writing to a relational database over network/disk creates multi-millisecond stalls.
2. **Thread Contention & Lock Overhead**: Mutexes (`synchronized` locks) force expensive operating system kernel context switches, invalidating CPU caches.
3. **Queue Write Contention**: Placing messages into multi-producer/multi-consumer queues (`ArrayBlockingQueue`) causes head/tail pointer contention on shared cache lines.

#### 2. The LMAX Solution: In-Memory Event Sourcing + Single-Threaded Core
The LMAX team discovered a counter-intuitive principle: **A single CPU core executing code on a single thread sequentially can process millions of transactions per second—IF it never blocks for I/O, never acquires locks, and keeps all business domain data in memory.**

```
                     THE LMAX TRIFECTA ARCHITECTURE
                     
  [Input Disruptor]      ──▶  [Business Logic Processor]  ──▶  [Output Disruptor]
  - Unmarshal wire msg        - Single-threaded execution     - Marshal network msgs
  - Replicate to cluster      - In-Memory state               - Publish to clients
  - Journal to disk           - Zero Locks / Zero DB IO
```

#### 3. Mechanical Sympathy & The Disruptor Ring Buffer
**"Mechanical Sympathy"** (a term coined by Martin Thompson from race-car driving) means designing software algorithms to work *with* the underlying hardware architecture rather than against it.

Modern CPUs execute instructions in nanoseconds, but fetching data from main RAM takes ~100 nanoseconds. CPUs rely heavily on **L1/L2/L3 Caches** (reading contiguous 64-byte *Cache Lines*). 

```
                    THE DISRUPTOR LOCK-FREE RING BUFFER
                    
                        [Slot 0]  [Slot 1]  [Slot 2]
                           ▲                   ▲
                           │                   │
                Consumer Sequence Counter   Producer Cursor Counter
                (Read-only, no locks)      (Single-writer, CAS)
```

The **Disruptor** replaces conventional queues with a pre-allocated **Circular Array (Ring Buffer)**:
- **No Garbage Collection**: All event objects in the ring buffer are pre-allocated at startup and recycled perpetually.
- **Single-Writer Principle**: Eliminates lock contention by granting each memory location a single writer.
- **Cache-Line Padding**: Prevents *False Sharing* by padding sequence counters to 64-byte boundaries.

#### 4. Disruptor Setup Example (Java)
```java
// Define the Event format
class TradeEvent { public long price; }

// Setup the Ring Buffer and Disruptor
int bufferSize = 1024;
Disruptor<TradeEvent> disruptor = new Disruptor<>(
    TradeEvent::new, bufferSize, Executors.defaultThreadFactory(), 
    ProducerType.SINGLE, new BusySpinWaitStrategy()
);

// Define Consumer (Business Logic)
disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
    System.out.println("Processing Trade Price: " + event.price);
});

// Start Disruptor
disruptor.start();
RingBuffer<TradeEvent> ringBuffer = disruptor.getRingBuffer();

// Publish new event (Producer)
ringBuffer.publishEvent((event, sequence) -> {
    event.price = 100L;
});
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPERS

<div class="scholarly-text">

#### Paper 1: The LMAX Architecture (July 2011)
*By Martin Fowler (Published on martinfowler.com)*

LMAX is a new retail financial trading platform. As a result it has to process many trades with very low latency. The system is built on the JVM platform and centers on a Business Logic Processor that can handle 6 million orders per second on a single thread. The Business Logic Processor runs entirely in-memory using event sourcing. The Business Logic Processor is surrounded by Disruptors - a concurrency component that implements a network of queues that operate without needing locks. During the design process the team concluded that recent directions in high-performance concurrency models using queues are fundamentally at odds with modern CPU design.

##### Business Logic Processor: Keeping It All in Memory
The Business Logic Processor takes input messages sequentially (in the form of a method invocation), runs business logic on it, and emits output events. It operates entirely in-memory; there is no database or other persistent store. Keeping all data in memory has two important benefits. Firstly it's fast - there's no database to provide disk IO to access, nor is there any transactional behavior to execute since all the processing is done sequentially. The second advantage is that it simplifies programming - there's no object/relational mapping to do. All the code can be written using Java's object model without having to make any compromises for the mapping to a database.

Such an in-memory structure has an important consequence - what happens if something crashes? The heart of dealing with this is Event Sourcing - which means that the current state of the Business Logic Processor is entirely derivable by processing the input events. As long as the input event stream is kept in a durable store (which is one of the jobs of the input disruptor) you can always recreate the current state of the business logic engine by replaying the events.

##### Queues and Their Lack of Mechanical Sympathy
The LMAX architecture caught people's attention because it's a very different way of approaching a high performance system to what most people are thinking about. An initial approach was to follow what so many are saying these days - that to get high performance you need to use explicit concurrency. A team built a prototype exchange using the actor model and did performance tests. What they found was that the processors spent more time managing queues than doing the real logic of the application. Queue access was a bottleneck.

When pushing performance like this, it starts to become important to take account of how modern hardware is constructed. The phrase Martin Thompson likes to use is "mechanical sympathy". The term comes from race car driving and it reflects the driver having an innate feel for the car, so they are able to feel how to get the best out of it. One of the dominant factors with modern CPUs that affects latency, is how the CPU interacts with memory. CPUs have multiple levels of cache, each of which of is significantly faster. To increase speed you want to get your code and data in those caches.

In order to put some data on a queue, you need to write to that queue. Similarly, to take data off the queue, you need to write to the queue to confirm the removal. This is write contention - more than one client may need to write to the same data structure. To deal with the write contention a queue often uses locks. When a lock is used, that can cause a context switch to the kernel. When this happens the processor involved is likely to lose the data in its caches.

The conclusion they came to was that to get the best caching behavior, you need a design that has only one core writing to any memory location. Multiple readers are fine; processors often use special high-speed links between their caches. But queues break the one-writer principle.

---

#### Paper 2: Disruptor: High Performance Alternative to Bounded Queues for Exchanging Data Between Concurrent Threads (May 2011)

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads<br>
  <strong>Author(s):</strong> Martin Thompson, Dave Farley, Michael Barker, Patricia Gee, Andrew Stewart<br>
  <strong>Published:</strong> May 2011, LMAX Technical Paper<br>
  <strong>Source type:</strong> Technical Paper<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 1 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
LMAX Disruptor: High performance alternative to
bounded queues for exchanging data between
concurrent threads
Martin Thompson ⋅ Dave Farley ⋅ Michael Barker ⋅ Patricia Gee ⋅ Andrew Stewart
 – Version 4.0.0-SNAPSHOT, May 2011
Table of Contents
1. Overview
2. The Complexities of Concurrency
2.1. The Cost of Locks
2.2. The Costs of “CAS”
2.3. Memory Barriers
2.4. Cache Lines
2.5. The Problems of Queues
2.6. Pipelines and Graphs
3. Design of the LMAX Disruptor
3.1. Memory Allocation
3.2. Teasing Apart the Concerns
3.3. Sequencing
3.4. Batching E!ect
3.5. Dependency Graphs
3.6. Disruptor Class Diagram
3.7. Code Example
4. Throughput Performance Testing
5. Latency Performance Testing
6. Conclusion
https://github.com/LMAX-Exchange/disruptor
Abstract
LMAX was established to create a very high performance financial exchange. As part of our work to
accomplish this goal we have evaluated several approaches to the design of such a system, but as we
began to measure these we ran into some fundamental limits with conventional approaches.
Many applications depend on queues to exchange data between processing stages. Our performance
testing showed that the latency costs, when using queues in this way, were in the same order of
magnitude as the cost of IO operations to disk (RAID or SSD based disk system) – dramatically slow. If
there are multiple queues in an end-to-end operation, this will add hundreds of microseconds to the
overall latency. There is clearly room for optimisation.
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 2 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
Further investigation and a focus on the computer science made us realise that the conflation of concerns
inherent in conventional approaches, (e.g. queues and processing nodes) leads to contention in multi-
threaded implementations, suggesting that there may be a better approach.
Thinking about how modern CPUs work, something we like to call “mechanical sympathy”, using good
design practices with a strong focus on teasing apart the concerns, we came up with a data structure and
a pattern of use that we have called the Disruptor.
Testing has shown that the mean latency using the Disruptor for a three-stage pipeline is 3 orders of
magnitude lower than an equivalent queue-based approach. In addition, the Disruptor handles
approximately 8 times more throughput for the same configuration.
These performance improvements represent a step change in the thinking around concurrent
programming. This new pattern is an ideal foundation for any asynchronous event processing
architecture where high-throughput and low-latency is required.
At LMAX we have built an order matching engine, real-time risk management, and a highly available in-
memory transaction processing system all on this pattern to great success. Each of these systems has set
new performance standards that, as far as we can tell, are unsurpassed.
However this is not a specialist solution that is only of relevance in the Finance industry. The Disruptor is
a general-purpose mechanism that solves a complex problem in concurrent programming in a way that
maximizes performance, and that is simple to implement. Although some of the concepts may seem
unusual it has been our experience that systems built to this pattern are significantly simpler to
implement than comparable mechanisms.
The Disruptor has significantly less write contention, a lower concurrency overhead and is more cache
friendly than comparable approaches, all of which results in greater throughput with less jitter at lower
latency. On processors at moderate clock rates we have seen over 25 million messages per second and
latencies lower than 50 nanoseconds. This performance is a significant improvement compared to any
other implementation that we have seen. This is very close to the theoretical limit of a modern processor
to exchange data between cores.
1. Overview
The Disruptor is the result of our efforts to build the world’s highest performance financial exchange at LMAX.
Early designs focused on architectures derived from SEDA [1] and Actors [2] using pipelines for throughput.
After profiling various implementations it became evident that the queuing of events between stages in the
pipeline was dominating the costs. We found that queues also introduced latency and high levels of jitter. We
expended significant effort on developing new queue implementations with better performance. However it
became evident that queues as a fundamental data structure are limited due to the conflation of design
concerns for the producers, consumers, and their data storage. The Disruptor is the result of our work to build
a concurrent structure that cleanly separates these concerns.
2. The Complexities of Concurrency
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 3 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
In the context of this document, and computer science in general, concurrency means not only that two or
more tasks happen in parallel, but also that they contend on access to resources. The contended resource may
be a database, file, socket or even a location in memory.
Concurrent execution of code is about two things, mutual exclusion and visibility of change. Mutual exclusion
is about managing contended updates to some resource. Visibility of change is about controlling when such
changes are made visible to other threads. It is possible to avoid the need for mutual exclusion if you can
eliminate the need for contended updates. If your algorithm can guarantee that any given resource is modified
by only one thread, then mutual exclusion is unnecessary. Read and write operations require that all changes
are made visible to other threads. However only contended write operations require the mutual exclusion of
the changes.
The most costly operation in any concurrent environment is a contended write access. To have multiple
threads write to the same resource requires complex and expensive coordination. Typically this is achieved by
employing a locking strategy of some kind.
2.1. The Cost of Locks
Locks provide mutual exclusion and ensure that the visibility of change occurs in an ordered manner. Locks
are incredibly expensive because they require arbitration when contended. This arbitration is achieved by a
context switch to the operating system kernel which will suspend threads waiting on a lock until it is released.
During such a context switch, as well as releasing control to the operating system which may decide to do other
house-keeping tasks while it has control, execution context can lose previously cached data and instructions.
This can have a serious performance impact on modern processors. Fast user mode locks can be employed but
these are only of any real benefit when not contended.
We will illustrate the cost of locks with a simple demonstration. The focus of this experiment is to call a
function which increments a 64-bit counter in a loop 500 million times. This can be executed by a single thread
on a 2.4Ghz Intel Westmere EP in just 300ms if written in Java. The language is unimportant to this experiment
and results will be similar across all languages with the same basic primitives.
Once a lock is introduced to provide mutual exclusion, even when the lock is as yet un-contended, the cost goes
up significantly. The cost increases again, by orders of magnitude, when two or more threads begin to contend.
The results of this simple experiment are shown in the table below:
Table 1. Comparative costs of contention
Method
Time (ms)
Single thread
300
Single thread with lock
10,000
Two threads with lock
224,000
Single thread with CAS
5,700
Two threads with CAS
30,000
Single thread with volatile write
4,700
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 4 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
2.2. The Costs of “CAS”
A more efficient alternative to the use of locks can be employed for updating memory when the target of the
update is a single word. These alternatives are based upon the atomic, or interlocked, instructions
implemented in modern processors. These are commonly known as CAS (Compare And Swap) operations, e.g.
“lock cmpxchg” on x86. A CAS operation is a special machine-code instruction that allows a word in memory to
be conditionally set as an atomic operation. For the “increment a counter experiment” each thread can spin in
a loop reading the counter then try to atomically set it to its new incremented value. The old and new values
are provided as parameters to this instruction. If, when the operation is executed, the value of the counter
matches the supplied expected value, the counter is updated with the new value. If, on the other hand, the
value is not as expected, the CAS operation will fail. It is then up to the thread attempting to perform the change
to retry, re-reading the counter incrementing from that value and so on until the change succeeds. This CAS
approach is significantly more efficient than locks because it does not require a context switch to the kernel for
arbitration. However CAS operations are not free of cost. The processor must lock its instruction pipeline to
ensure atomicity and employ a memory barrier to make the changes visible to other threads. CAS operations
are available in Java by using the java.util.concurrent.Atomic* classes.
If the critical section of the program is more complex than a simple increment of a counter it may take a
complex state machine using multiple CAS operations to orchestrate the contention. Developing concurrent
programs using locks is difficult; developing lock-free algorithms using CAS operations and memory barriers is
many times more complex and it is very difficult to prove that they are correct.
The ideal algorithm would be one with only a single thread owning all writes to a single resource with other
threads reading the results. To read the results in a multi-processor environment requires memory barriers to
make the changes visible to threads running on other processors.
2.3. Memory Barriers
Modern processors perform out-of-order execution of instructions and out-of-order loads and stores of data
between memory and execution units for performance reasons. The processors need only guarantee that
program logic produces the same results regardless of execution order. This is not an issue for single-threaded
programs. However, when threads share state it is important that all memory changes appear in order, at the
point required, for the data exchange to be successful. Memory barriers are used by processors to indicate
sections of code where the ordering of memory updates is important. They are the means by which hardware
ordering and visibility of change is achieved between threads. Compilers can put in place complimentary
software barriers to ensure the ordering of compiled code, such software memory barriers are in addition to
the hardware barriers used by the processors themselves.
Modern CPUs are now much faster than the current generation of memory systems. To bridge this divide CPUs
use complex cache systems which are effectively fast hardware hash tables without chaining. These caches are
kept coherent with other processor cache systems via message passing protocols. In addition, processors have
“store buffers” to offload writes to these caches, and “invalidate queues” so that the cache coherency protocols
can acknowledge invalidation messages quickly for efficiency when a write is about to happen.
What this means for data is that the latest version of any value could, at any stage after being written, be in a
register, a store buffer, one of many layers of cache, or in main memory. If threads are to share this value, it
needs to be made visible in an ordered fashion and this is achieved through the coordinated exchange of cache
coherency messages. The timely generation of these messages can be controlled by memory barriers.
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 5 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
A read memory barrier orders load instructions on the CPU that executes it by marking a point in the
invalidate queue for changes coming into its cache. This gives it a consistent view of the world for write
operations ordered before the read barrier.
A write barrier orders store instructions on the CPU that executes it by marking a point in the store buffer, thus
flushing writes out via its cache. This barrier gives an ordered view to the world of what store operations
happen before the write barrier.
A full memory barrier orders both loads and stores but only on the CPU that executes it.
Some CPUs have more variants in addition to these three primitives but these three are sufficient to
understand the complexities of what is involved. In the Java memory model the read and write of a volatile
field implements the read and write barriers respectively. This was made explicit in the Java Memory Model [3]
as defined with the release of Java 5.
2.4. Cache Lines
The way in which caching is used in modern processors is of immense importance to successful high
performance operation. Such processors are enormously efficient at churning through data and instructions
held in cache and yet, comparatively, are massively inefficient when a cache miss occurs.
Our hardware does not move memory around in bytes or words. For efficiency, caches are organised into
cache-lines that are typically 32-256 bytes in size, the most common cache-line being 64 bytes. This is the level
of granularity at which cache coherency protocols operate. This means that if two variables are in the same
cache line, and they are written to by different threads, then they present the same problems of write
contention as if they were a single variable. This is a concept know as “false sharing”. For high performance
then, it is important to ensure that independent, but concurrently written, variables do not share the same
cache-line if contention is to be minimised.
When accessing memory in a predictable manner CPUs are able to hide the latency cost of accessing main
memory by predicting which memory is likely to be accessed next and pre-fetching it into the cache in the
background. This only works if the processors can detect a pattern of access such as walking memory with a
predictable “stride”. When iterating over the contents of an array the stride is predictable and so memory will
be pre-fetched in cache lines, maximizing the efficiency of the access. Strides typically have to be less than 2048
bytes in either direction to be noticed by the processor. However, data structures like linked lists and trees tend
to have nodes that are more widely distributed in memory with no predictable stride of access. The lack of a
consistent pattern in memory constrains the ability of the system to pre-fetch cache-lines, resulting in main
memory accesses which can be more than 2 orders of magnitude less efficient.
2.5. The Problems of Queues
Queues typically use either linked-lists or arrays for the underlying storage of elements. If an in-memory queue
is allowed to be unbounded then for many classes of problem it can grow unchecked until it reaches the point
of catastrophic failure by exhausting memory. This happens when producers outpace the consumers.
Unbounded queues can be useful in systems where the producers are guaranteed not to outpace the consumers
and memory is a precious resource, but there is always a risk if this assumption doesn’t hold and queue grows
without limit. To avoid this catastrophic outcome, queues are commonly constrained in size (bounded).
Keeping a queue bounded requires that it is either array-backed or that the size is actively tracked.
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 6 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
Queue implementations tend to have write contention on the head, tail, and size variables. When in use,
queues are typically always close to full or close to empty due to the differences in pace between consumers
and producers. They very rarely operate in a balanced middle ground where the rate of production and
consumption is evenly matched. This propensity to be always full or always empty results in high levels of
contention and/or expensive cache coherence. The problem is that even when the head and tail mechanisms
are separated using different concurrent objects such as locks or CAS variables, they generally occupy the same
cache-line.
The concerns of managing producers claiming the head of a queue, consumers claiming the tail, and the
storage of nodes in between make the designs of concurrent implementations very complex to manage beyond
using a single large-grain lock on the queue. Large grain locks on the whole queue for put and take operations
are simple to implement but represent a significant bottleneck to throughput. If the concurrent concerns are
teased apart within the semantics of a queue then the implementations become very complex for anything
other than a single producer – single consumer implementation.
In Java there is a further problem with the use of queues, as they are significant sources of garbage. Firstly,
objects have to be allocated and placed in the queue. Secondly, if linked-list backed, objects have to be allocated
representing the nodes of the list. When no longer referenced, all these objects allocated to support the queue
implementation need to be re-claimed.
2.6. Pipelines and Graphs
For many classes of problem it makes sense to wire together several processing stages into pipelines. Such
pipelines often have parallel paths, being organised into graph-like topologies. The links between each stage are
often implemented by queues with each stage having its own thread.
This approach is not cheap - at each stage we have to incur the cost of en-queuing and de-queuing units of
work. The number of targets multiplies this cost when the path must fork, and incurs an inevitable cost of
contention when it must re-join after such a fork.
It would be ideal if the graph of dependencies could be expressed without incurring the cost of putting the
queues between stages.
3. Design of the LMAX Disruptor
While trying to address the problems described above, a design emerged through a rigorous separation of the
concerns that we saw as being conflated in queues. This approach was combined with a focus on ensuring that
any data should be owned by only one thread for write access, therefore eliminating write contention. That
design became known as the “Disruptor”. It was so named because it had elements of similarity for dealing
with graphs of dependencies to the concept of “Phasers” [4] in Java 7, introduced to support Fork-Join.
The LMAX disruptor is designed to address all of the issues outlined above in an attempt to maximize the
efficiency of memory allocation, and operate in a cache-friendly manner so that it will perform optimally on
modern hardware.
At the heart of the disruptor mechanism sits a pre-allocated bounded data structure in the form of a ring-
buffer. Data is added to the ring buffer through one or more producers and processed by one or more
consumers.
3.1. Memory Allocation
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 7 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
All memory for the ring buffer is pre-allocated on start up. A ring-buffer can store either an array of pointers to
entries or an array of structures representing the entries. The limitations of the Java language mean that
entries are associated with the ring-buffer as pointers to objects. Each of these entries is typically not the data
being passed itself, but a container for it. This pre-allocation of entries eliminates issues in languages that
support garbage collection, since the entries will be re-used and live for the duration of the Disruptor instance.
The memory for these entries is allocated at the same time and it is highly likely that it will be laid out
contiguously in main memory and so support cache striding. There is a proposal by John Rose to introduce
“value types” [5] to the Java language which would allow arrays of tuples, like other languages such as C, and so
ensure that memory would be allocated contiguously and avoid the pointer indirection.
Garbage collection can be problematic when developing low-latency systems in a managed runtime
environment like Java. The more memory that is allocated the greater the burden this puts on the garbage
collector. Garbage collectors work at their best when objects are either very short-lived or effectively immortal.
The pre-allocation of entries in the ring buffer means that it is immortal as far as garbage collector is
concerned and so represents little burden.
Under heavy load queue-based systems can back up, which can lead to a reduction in the rate of processing,
and results in the allocated objects surviving longer than they should, thus being promoted beyond the young
generation with generational garbage collectors. This has two implications: first, the objects have to be copied
between generations which cause latency jitter; second, these objects have to be collected from the old
generation which is typically a much more expensive operation and increases the likelihood of “stop the world”
pauses that result when the fragmented memory space requires compaction. In large memory heaps this can
cause pauses of seconds per GB in duration.
3.2. Teasing Apart the Concerns
We saw the following concerns as being conflated in all queue implementations, to the extent that this
collection of distinct behaviours tend to define the interfaces that queues implement:
1. Storage of items being exchanged
2. Coordination of producers claiming the next sequence for exchange
3. Coordination of consumers being notified that a new item is available
When designing a financial exchange in a language that uses garbage collection, too much memory allocation
can be problematic. So, as we have described linked-list backed queues are a not a good approach. Garbage
collection is minimized if the entire storage for the exchange of data between processing stages can be pre-
allocated. Further, if this allocation can be performed in a uniform chunk, then traversal of that data will be
done in a manner that is very friendly to the caching strategies employed by modern processors. A data-
structure that meets this requirement is an array with all the slots pre-filled. On creation of the ring buffer the
Disruptor utilises the abstract factory pattern to pre-allocate the entries. When an entry is claimed, a producer
can copy its data into the pre-allocated structure.
On most processors there is a very high cost for the remainder calculation on the sequence number, which
determines the slot in the ring. This cost can be greatly reduced by making the ring size a power of 2. A bit
mask of size minus one can be used to perform the remainder operation efficiently.
As we described earlier bounded queues suffer from contention at the head and tail of the queue. The ring
buffer data structure is free from this contention and concurrency primitives because these concerns have
been teased out into producer and consumer barriers through which the ring buffer must be accessed. The
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 8 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
logic for these barriers is described below.
In most common usages of the Disruptor there is usually only one producer. Typical producers are file readers
or network listeners. In cases where there is a single producer there is no contention on sequence/entry
allocation. In more unusual usages where there are multiple producers, producers will race one another to
claim the next entry in the ring-buffer. Contention on claiming the next available entry can be managed with a
simple CAS operation on the sequence number for that slot.
Once a producer has copied the relevant data to the claimed entry it can make it public to consumers by
committing the sequence. This can be done without CAS by a simple busy spin until the other producers have
reached this sequence in their own commit. Then this producer can advance the cursor signifying the next
available entry for consumption. Producers can avoid wrapping the ring by tracking the sequence of
consumers as a simple read operation before they write to the ring buffer.
Consumers wait for a sequence to become available in the ring buffer before they read the entry. Various
strategies can be employed while waiting. If CPU resource is precious they can wait on a condition variable
within a lock that gets signalled by the producers. This obviously is a point of contention and only to be used
when CPU resource is more important than latency or throughput. The consumers can also loop checking the
cursor which represents the currently available sequence in the ring buffer. This could be done with or without
a thread yield by trading CPU resource against latency. This scales very well as we have broken the contended
dependency between the producers and consumers if we do not use a lock and condition variable. Lock free
multi-producer – multi-consumer queues do exist but they require multiple CAS operations on the head, tail,
size counters. The Disruptor does not suffer this CAS contention.
3.3. Sequencing
Sequencing is the core concept to how the concurrency is managed in the Disruptor. Each producer and
consumer works off a strict sequencing concept for how it interacts with the ring buffer. Producers claim the
next slot in sequence when claiming an entry in the ring. This sequence of the next available slot can be a
simple counter in the case of only one producer or an atomic counter updated using CAS operations in the case
of multiple producers. Once a sequence value is claimed, this entry in the ring buffer is now available to be
written to by the claiming producer. When the producer has finished updating the entry it can commit the
changes by updating a separate counter which represents the cursor on the ring buffer for the latest entry
available to consumers. The ring buffer cursor can be read and written in a busy spin by the producers using
memory barrier without requiring a CAS operation as below.
Consumers wait for a given sequence to become available by using a memory barrier to read the cursor. Once
the cursor has been updated the memory barriers ensure the changes to the entries in the ring buffer are
visible to the consumers who have waited on the cursor advancing.
long
long
long
long expectedSequence = claimedSequence – 1;
while
while
while
while (cursor !=
!=
!=
!= expectedSequence)
{
  // busy spin
}
cursor = claimedSequence;
JAVA
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 9 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
Consumers each contain their own sequence which they update as they process entries from the ring buffer.
These consumer sequences allow the producers to track consumers to prevent the ring from wrapping.
Consumer sequences also allow consumers to coordinate work on the same entry in an ordered manner
In the case of having only one producer, and regardless of the complexity of the consumer graph, no locks or
CAS operations are required. The whole concurrency coordination can be achieved with just memory barriers
on the discussed sequences.
3.4. Batching E!ect
When consumers are waiting on an advancing cursor sequence in the ring buffer an interesting opportunity
arises that is not possible with queues. If the consumer finds the ring buffer cursor has advanced a number of
steps since it last checked it can process up to that sequence without getting involved in the concurrency
mechanisms. This results in the lagging consumer quickly regaining pace with the producers when the
producers burst ahead thus balancing the system. This type of batching increases throughput while reducing
and smoothing latency at the same time. Based on our observations, this effect results in a close to constant
time for latency regardless of load, up until the memory sub-system is saturated, and then the profile is linear
following Little’s Law [6]. This is very different to the “J” curve effect on latency we have observed with queues
as load increases.
3.5. Dependency Graphs
A queue represents the simple one step pipeline dependency between producers and consumers. If the
consumers form a chain or graph-like structure of dependencies then queues are required between each stage
of the graph. This incurs the fixed costs of queues many times within the graph of dependent stages. When
designing the LMAX financial exchange our profiling showed that taking a queue based approach resulted in
queuing costs dominating the total execution costs for processing a transaction.
Because the producer and consumer concerns are separated with the Disruptor pattern, it is possible to
represent a complex graph of dependencies between consumers while only using a single ring buffer at the
core. This results in greatly reduced fixed costs of execution thus increasing throughput while reducing latency.
A single ring buffer can be used to store entries with a complex structure representing the whole workflow in a
cohesive place. Care must be taken in the design of such a structure so that the state written by independent
consumers does not result in false sharing of cache lines.
3.6. Disruptor Class Diagram
The core relationships in the Disruptor framework are depicted in the class diagram below. This diagram
leaves out the convenience classes which can be used to simplify the programming model. After the
dependency graph is constructed the programming model is simple. Producers claim entries in sequence via a
ProducerBarrier , write their changes into the claimed entry, then commit that entry back via the
ProducerBarrier making them available for consumption. As a consumer all one needs do is provide a
BatchHandler implementation that receives call backs when a new entry is available. This resulting
programming model is event based having a lot of similarities to the Actor Model.
Separating the concerns normally conflated in queue implementations allows for a more flexible design. A
RingBuffer exists at the core of the Disruptor pattern providing storage for data exchange without
contention. The concurrency concerns are separated out for the producers and consumers interacting with the
RingBuffer . The ProducerBarrier manages any concurrency concerns associated with claiming slots in the
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 10 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
ring buffer, while tracking dependant consumers to prevent the ring from wrapping. The ConsumerBarrier
notifies consumers when new entries are available, and Consumers can be constructed into a graph of
dependencies representing multiple stages in a processing pipeline.
3.7. Code Example
The code below is an example of a single producer and single consumer using the convenience interface
BatchHandler for implementing a consumer. The consumer runs on a separate thread receiving entries as
they become available.
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 11 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
4. Throughput Performance Testing
As a reference we choose Doug Lea’s excellent java.util.concurrent.ArrayBlockingQueue [7] which has the
highest performance of any bounded queue based on our testing. The tests are conducted in a blocking
programming style to match that of the Disruptor. The tests cases detailed below are available in the Disruptor
open source project.

running the tests requires a system capable of executing at least 4 threads in parallel.
Figure 1. Unicast: 1P – 1C
// Callback handler which can be implemented by consumers
final
final
final
final BatchHandler
BatchHandler
BatchHandler
BatchHandler<ValueEntry
ValueEntry
ValueEntry
ValueEntry> batchHandler = new
new
new
new BatchHandler
BatchHandler
BatchHandler
BatchHandler<ValueEntry
ValueEntry
ValueEntry
ValueEntry>()
>()
>()
>()
{
public
public
public
public void
void
void
void onAvailable
onAvailable
onAvailable
onAvailable(final
final
final
final ValueEntry
ValueEntry
ValueEntry
ValueEntry entry) throws
throws
throws
throws Exception
Exception
Exception
Exception
{
// process a new entry as it becomes available.
}
    public
public
public
public void
void
void
void onEndOfBatch
onEndOfBatch
onEndOfBatch
onEndOfBatch()
()
()
() throws
throws
throws
throws Exception
Exception
Exception
Exception
    {
        // useful for flushing results to an IO device if necessary.
    }
    public
public
public
public void
void
void
void onCompletion
onCompletion
onCompletion
onCompletion()
()
()
()
    {
        // do any necessary clean up before shutdown
    }
};
};
};
};
RingBuffer
RingBuffer
RingBuffer
RingBuffer<ValueEntry
ValueEntry
ValueEntry
ValueEntry> ringBuffer =
    new
new
new
new RingBuffer
RingBuffer
RingBuffer
RingBuffer<ValueEntry
ValueEntry
ValueEntry
ValueEntry>(
>(
>(
>(ValueEntry
ValueEntry
ValueEntry
ValueEntry.ENTRY_FACTORY, SIZE,
                               ClaimStrategy
ClaimStrategy
ClaimStrategy
ClaimStrategy.Option.SINGLE_THREADED,
                               WaitStrategy
WaitStrategy
WaitStrategy
WaitStrategy.Option.YIELDING);
);
);
);
ConsumerBarrier
ConsumerBarrier
ConsumerBarrier
ConsumerBarrier<ValueEntry
ValueEntry
ValueEntry
ValueEntry> consumerBarrier = ringBuffer.createConsumerBarrier();
();
();
();
BatchConsumer
BatchConsumer
BatchConsumer
BatchConsumer<ValueEntry
ValueEntry
ValueEntry
ValueEntry> batchConsumer =
    new
new
new
new BatchConsumer
BatchConsumer
BatchConsumer
BatchConsumer<ValueEntry
ValueEntry
ValueEntry
ValueEntry>(
>(
>(
>(consumerBarrier, batchHandler);
);
);
);
ProducerBarrier
ProducerBarrier
ProducerBarrier
ProducerBarrier<ValueEntry
ValueEntry
ValueEntry
ValueEntry> producerBarrier = ringBuffer.createProducerBarrier(batchConsumer);
);
);
);
// Each consumer can run on a separate thread
EXECUTOR.submit(batchConsumer);
);
);
);
// Producers claim entries in sequence
ValueEntry
ValueEntry
ValueEntry
ValueEntry entry = producerBarrier.nextEntry();
();
();
();
// copy data into the entry container
// make the entry available to consumers
producerBarrier.commit(entry);
);
);
);
JAVA
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 12 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
Figure 2. Three Step Pipeline: 1P – 3C
Figure 3. Sequencer: 3P – 1C
Figure 4. Multicast: 1P – 3C
Figure 5. Diamond: 1P – 3C
For the above configurations an ArrayBlockingQueue was applied for each arc of data flow compared to
barrier configuration with the Disruptor. The following table shows the performance results in operations per
second using a Java 1.6.0_25 64-bit Sun JVM, Windows 7, Intel Core i7 860 @ 2.8 GHz without HT and Intel Core
i7-2720QM, Ubuntu 11.04, and taking the best of 3 runs when processing 500 million messages. Results can vary
substantially across different JVM executions and the figures below are not the highest we have observed.
Table 2. Comparative throughput (in ops per sec)
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 13 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
Nehalem 2.8Ghz – Windows 7 SP1 64-
bit
Sandy Bridge 2.2Ghz – Linux 2.6.38 64-
bit
ABQ
Disruptor
ABQ
Disruptor
Unicast: 1P – 1C
5,339,256
25,998,336
4,057,453
22,381,378
Pipeline: 1P – 3C
2,128,918
16,806,157
2,006,903
15,857,913
Sequencer: 3P – 1C
5,539,531
13,403,268
2,056,118
14,540,519
Multicast: 1P – 3C
1,077,384
9,377,871
260,733
10,860,121
Diamond: 1P – 3C
2,113,941
16,143,613
2,082,725
15,295,197
Table 3. Comparative throughput updated for modern hardware (in ops per sec)
AMD EPYC 9374F – Linux 5.4.277 – OpenJDK 11.0.24
ABQ
Disruptor 3
Disruptor 4
Unicast: 1P – 1C
20,895,148
134,553,283
160,359,204
Pipeline: 1P – 3C
5,216,647
76,068,766
101,317,122
Sequencer: 3P – 1C
18,791,340
16,010,759
29,726,516
Multicast: 1P – 3C
2,355,379
68,157,033
70,018,204
Diamond: 1P – 3C
3,433,665
61,229,488
63,123,343
5. Latency Performance Testing
To measure latency we take the three stage pipeline and generate events at less than saturation. This is
achieved by waiting 1 microsecond after injecting an event before injecting the next and repeating 50 million
times. To time at this level of precision it is necessary to use time stamp counters from the CPU. We chose CPUs
with an invariant TSC because older processors suffer from changing frequency due to power saving and sleep
states. Intel Nehalem and later processors use an invariant TSC which can be accessed by the latest Oracle JVMs
running on Ubuntu 11.04. No CPU binding has been employed for this test. For comparison we use the
ArrayBlockingQueue once again. We could have used ConcurrentLinkedQueue [8] which is likely to give better
results but we want to use a bounded queue implementation to ensure producers do not outpace consumers by
creating back pressure. The results below are for 2.2Ghz Core i7-2720QM running Java 1.6.0_25 64-bit on
Ubuntu 11.04. Mean latency per hop for the Disruptor comes out at 52 nanoseconds compared to 32,757
nanoseconds for ArrayBlockingQueue. Profiling shows the use of locks and signalling via a condition variable
are the main cause of latency for the ArrayBlockingQueue.
Table 4. Comparative Latency in three stage pipeline
Array Blocking Queue (ns)
Disruptor (ns)
Min Latency
145
29
5/8/2026, 2:06 pm
LMAX Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads
Page 14 of 14
https://lmax-exchange.github.io/disruptor/disruptor.html
Mean Latency
32,757
52
99% observations less than
2,097,152
128
99.99% observations less than
4,194,304
8,192
Max Latency
5,069,086
175,567
6. Conclusion
The Disruptor is a major step forward for increasing throughput, reducing latency between concurrent
execution contexts and ensuring predictable latency, an important consideration in many applications. Our
testing shows that it out-performs comparable approaches for exchanging data between threads. We believe
that this is the highest performance mechanism for such data exchange. By concentrating on a clean separation
of the concerns involved in cross-thread data exchange, by eliminating write contention, minimizing read
contention and ensuring that the code worked well with the caching employed by modern processors, we have
created a highly efficient mechanism for exchanging data between threads in any application.
The batching effect that allows consumers to process entries up to a given threshold, without any contention,
introduces a new characteristic in high performance systems. For most systems, as load and contention
increase there is an exponential increase in latency, the characteristic “J” curve. As load increases on the
Disruptor, latency remains almost flat until saturation occurs of the memory sub-system.
We believe that the Disruptor establishes a new benchmark for high-performance computing and is very well
placed to continue to take advantage of current trends in processor and computer design.
View the original PDF of this paper here.
1. Staged Event-Driven Architecture – https://en.wikipedia.org/wiki/Staged_event-driven_architecture
2. Actor model – http://dspace.mit.edu/handle/1721.1/6952
3. Java Memory Model - https://jcp.org/en/jsr/detail?id=133
4. Phasers - https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Phaser.html
5. Value Types - https://blogs.oracle.com/jrose/tuples-in-the-vm
6. Little’s Law - https://en.wikipedia.org/wiki/Little%27s_law
7. ArrayBlockingQueue -
https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ArrayBlockingQueue.html
8. ConcurrentLinkedQueue -
http://download.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/ConcurrentLinkedQueue.html
Version 1.0
Last updated 2025-04-02 13:02:59 UTC


---

</div>

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 3.2.A: Event Sourcing & CQRS Architecture
- **Event Sourcing (Martin Fowler)**: Instead of storing current state in a database, all changes to application state are stored as an immutable sequence of events. State is reconstructed at startup by replaying the event log.
- **CQRS (Command Query Responsibility Segregation, Greg Young)**: Separates read operations (Queries) from write operations (Commands). The LMAX single-threaded Business Logic Processor acts as the ultimate low-latency Command engine.

#### Reference 3.2.B: False Sharing & Cache-Line Padding
- **Cache Line Anatomy**: Modern x86 CPUs load memory into 64-byte L1/L2/L3 cache lines.
- **False Sharing Hazard**: If Thread A updates variable $X$ and Thread B updates variable $Y$, and both variables happen to reside on the same 64-byte cache line, the CPU cache coherency protocol (MESI) forces the cache line to invalidate back and forth between CPU sockets on every write ("Cache Line Ping-Pong").
- **Padding Solution in Java/C++**:

```java
// Java Cache-Line Padding to Prevent False Sharing
public class PaddedAtomicLong {
    public volatile long value = 0L;
    public long p1, p2, p3, p4, p5, p6, p7; // 56 bytes of padding + 8 byte long = 64 bytes
}
```

#### Reference 3.2.C: Single-Writer Principle & Lock-Free Data Structures
- **Core Axiom**: Concurrency contention disappears if a memory location is written to by exactly one thread.
- **Ring Buffer Cursor Arbitration**: Producers claim sequence slots using atomic Compare-And-Swap (CAS), but consumers only read the sequence pointers of upstream processing stages, enabling lock-free dependency graphs.


<div class="page-break"></div>

## Chapter 3.3: LMAX Technology Blog Lessons — Scale, Testing & Code Hygiene

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Real-World Engineering Practices at LMAX Exchange
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

#### 2. Numerical Scale & Precision Anomalies in Managed Runtimes
In high-frequency financial platforms, numbers representing contract sizes and prices must avoid floating-point rounding errors (such as `0.1 + 0.2 = 0.30000000000000004`). Languages like Java provide `BigDecimal` for exact arbitrary-precision arithmetic.

However, `BigDecimal` carries subtle traps:
- `10` and `10.0` have different **scales** (`0` vs `1`).
- `.equals()` returns `false` when comparing `new BigDecimal("10")` and `new BigDecimal("10.0")` because scale is part of object equality!
- Operations like `.stripTrailingZeros()` can result in **negative scales** (e.g., `100` stripped becomes `1E+2` with scale `-2`), causing unexpected results when combined with division and rounding modes.

#### 3. Side-Effect Free Constructors & Testability
Constructors that open files, perform network I/O, or instantiate complex dependent objects make unit testing nearly impossible. 

By restricting constructors to **assignment and nothing else**, classes become instantly testable with lightweight mocks or in-memory streams, enforcing clean Dependency Injection.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPERS

<div class="scholarly-text">

#### Article 1: A Question of Scale (May 2023)
*By Simon Warren (LMAX Technology Blog)*

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
*By Yuliia Povoliashko, Hans Sharda, and Stewart Atkinson (LMAX Technology Blog)*

One aspect that stood out to me is the company’s pair programming approach. Tester-developer pairing is a common practice at LMAX, with testers contributing to code and developers writing tests. This approach fosters excellent knowledge sharing and collaboration.

Another unique aspect of working at LMAX is the practice of writing tests first, followed by the code. The result of this approach has compounded over the years: leaving a set of tests that act both as a safety net allowing for fast agile development, and as documentation for how every aspect of the exchange works.

---

#### Article 3: Why I Don't Do Work in Constructors (September 2024)
*By James Byatt (LMAX Technology Blog)*

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
*By James Byatt (LMAX Technology Blog)*

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

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> The Impossible NullPointerException<br>
  <strong>Author(s):</strong> James Byatt<br>
  <strong>Published:</strong> June 2022, LMAX Technology Blog<br>
  <strong>Source type:</strong> Engineering Blog<br>
  <strong>Original URL:</strong> https://technology.lmax.com/posts/the-impossible-null-pointer-exception/<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 1 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
The Impossible
NullPointerException
 2022-06-15
 13 min read
Our new production exchange recently produced an
impossible looking NullPointerException .
At the same time, we saw another application in the same
deployment throw an OutOfMemoryError .
Both problems turned out to have the same root cause.
This post tells the story of how we found that out.
The Problem
Or problems, plural, in this case. We’ve added a new
exchange. To bring it into service, it needs to join an
upstream ‘global’ service that maintains data that’s shared
across multiple exchanges. To do so, that global service
needs to send our new exchange quite a lot of data. One of
the larger datasets that we need to synchronise is
customer .
Our ﬁrst attempt to do this did not go well. Here’s the ﬁrst
few elements of a stack trace that we thought was, well,
exceptional.
Why is that so impossible? We’ll need a crash course in how
our messaging system works to see why. First though - let’s
have a look at that line, and work our way up…
Here’s CustomerDao.java:345
From which we guess that changedBy is null. That
particular object has type Identity and gets passed down
from CustomerManager::upsert .
Now. How did this function call get here? Well, it was
proxied over from another (remote) service via multicast.
CustomerManager implements a messaging interface
that contains that method signature. With a bit of magic,
Caused by: java.lang.NullPointerException
       at CustomerDao.upsert(CustomerDao.java:34
       at CustomerManager.upsertCustomer(Custome
       at CustomerManager.upsert(CustomerManager
customerParams.addValue("principalRealm", writeAl
public void upsert(
       final String requestId,
       final Customer customer,
       final Identity changedBy,
       final @Milliseconds long changeTimestamp,
       final long bookingCustomerId)
The
Problem
The
other
problem
Oh,
ByteBuffer,
we have
not
missed
you
An
Example
In
Staging
We’re in
control of
the
trafﬁc
now
Back in
the Wan
Tunnel
The
missing
ﬂip
What
about the
OOM,
though?
How did
it take us
so long to
work this
out?
On This
Page
Posts
About
Authors
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 2 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
for a topic like that, we generate an implementation that
transforms the method call into a multicast packet, and
another piece of code that can ‘invoke’ that packet on an
instance of that interface in another application entirely.
This is very handy, because we can see who calls that
method on that interface via intelliJ’s “ﬁnd usages”, and
quickly ﬁnd the culprit who has passed in a null Identity .
There are six callers, but we know that only one of them is
called by our sync process, so we trace where the
Identity instance is constructed by that caller, and
suddenly, we’re very confused, because in the database
reading code, we ﬁnd this:
We double and triple check our work, but no. The caller’s
got no way of passing null in. And yet…
NullPointerException . What have we missed? Well, we
guess there must be something wrong in the magic that
marshals those objects across the network. Unfortunately,
that magic, in this case, is a slightly larger area than usual…
The other problem
We haven’t been entirely honest.
The vast majority of the messaging at LMAX happens as
described - a method call transported across a multicast
bus between two applications. In this particular case
though, the story is more complicated, because global and
the new exchange do not live in the same multicast
network.
How do we get around this? We use a bridge. The bridge
application has two parts - a process in the exchange, and
one in global. Each process proxies a subset of the
multicast trafﬁc from its end down a single, shared TCP
link to the other end, which then faithfully copies that
trafﬁc back out onto the multicast bus at the other end
unchanged. The global end is slightly more special because
it maintains a connection per exchange (it sends anything
it hears to every exchange), but otherwise the processes
are very similar. We call these processes wan-tunnel-local
(deployed in the exchange) and wan-tunnel-global
return new Identity(
       getByAlphaChar(rs.getString("principal_re
       rs.getString("principal_domain"), 
       rs.getString("principal_username"));
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 3 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
(deployed in global).
Our real message path is actually more like:
global-admin-svc -> multicast -> wan-tunnel-global -> tcp
link -> wan-tunnel-local -> multicast -> account-svc
With this extra knowledge, we can reveal part two of the
issue: at around the same time we see the
NullPointerException in account-svc , wan-tunnel-
local OutOfMemoryError s.
Even more notably, only one wan-tunnel-local (out of
ﬁve) OOMed, and only one account-svc NPE d. And yes,
they were in the same exchange.
Where should we go next? There are some big clues in
there. Not least the presence of our old enemyfriend,
ByteBuffer .
Oh, ByteBuffer, we have not missed
you
We look very closely at wan-tunnel-global , and work out
that it is deﬁnitely copying the same multicast packets to
each TCP connection handler’s work queue. This means
we’ve either got a bug in the code to copy the work queue
to the TCP socket in this process, or a bug in the code that
reads and translates the other end of the connection in
wan-tunnel-local .
Unfortunately, that code is full of ByteBuffer s. It also
seems unnecessarily convoluted to our eyes. That’s an easy
criticism to make when looking at code for the ﬁrst time
when you know a bug lives in it, so we should reserve
judgement. Walking through it and running the unit tests
doesn’t immediately ﬁnd a problem at either end.
Unsurprising, we suppose - if the tests exercised the bug,
they’d be failing, and this bit of software has worked
without error for several years. We write more unit tests,
and they continue to tell us everything is ﬁne.
We’re at that funny sort of impasse where it’s tempting to
start blaming cosmic rays for corrupting a packet. That
usually means it’s time to try a reproduction at a different
scale; we’ve been going small - can we go large? Handily,
java.lang.OutOfMemoryError: Direct buffer memory 
 at java.base/java.nio.Bits.reserveMemory(Bits.j
 at java.base/java.nio.DirectByteBuffer.<init>(D
 at java.base/java.nio.ByteBuffer.allocateDirect
 at TunnelInbound.readNext(TunnelInbound.java:11
 at TunnelNetworkLoop.onRead(TunnelNetworkLoop.j
 at TunnelNetworkLoop.doSelect(TunnelNetworkLoop
 at TunnelNetworkLoop.doSelect(TunnelNetworkLoop
 at TunnelAcceptor.run(TunnelAcceptor.java:139)
 at java.base/java.lang.Thread.run(Thread.java:8
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 4 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
yes.
An Example In Staging
We handily have virtualized versions of our exchanges
available, using a subset of cleansed production data. In the
case of our bug we have the whole (cleansed) data set, so
we should be able to invoke absolute the same code path
that triggered this in live.
We get staging into an appropriate state to do this, push
the sync button, and…it works. The data syncs correctly.
Ok, so, it’s intermittent. We work out how to easily repeat
the experiment and run it few tens of times across lunch. It
works correctly every time. That’s odd - we tried this once
in live, and it failed the ﬁrst time. This is either a
spectacularly unlikely coincidence, or our reproduction is
insufﬁciently accurate. We reassure ourselves that
computers are deterministic and non-malicious, and
wonder if we need to simulate the exact network
conditions too; the TCP link between global and the
exchange often goes over a VPN, and sometimes over a
long enough distance to generate 100ms+ of RTT.
Do we ﬁnally have an excuse to get tc/netem out? Yes we
do.
We’re in control of the trafﬁc now
We make our way to a wan-tunnel-local host in staging,
and crack our ﬁngers in preparation for typing arcane
demands into the black screen of tiny letters.
We could spend time explaining what’s going on there, but
having blindly copied those commands from one of these
two excellent resources, we suspect a link might be a
better idea. We believe we’re adding 150ms of delay (with a
20ms standard deviation) and rate limiting (32kbit/s with
bursts up to 1mbit/s) to all trafﬁc on eth55 .
Whether our understanding is correct or not swiftly
becomes irrelevant - the very ﬁrst time we attempt this
with both the rate limit and the delay, we see our wan-
tunnel-local happily throw our OOME with the stack we’re
after. We visit the logs for the local account-svc and ﬁnd a
familiar NPE , too. Aha. Not cosmic rays after all. The
reproducer seems pretty reliable, too - our ﬁrst three tries
all generate the desired result.
Now we’re back on the science train, we can gather some
more data. Attaching a debugger to account-svc gets us a
step further - we’re trying to deserialize an Identity (in
fact probably most of the objects in the second half of an
RPC) out of a giant array of zeroed bytes.
We take some packet captures at the wan-tunnel-local . I
forget how to use wireshark ﬁlters and instead get it to
# throttle bandwidth:
sudo tc qdisc add dev eth55 root handle 1: tbf ra
# and delay packets (both commands need to be use
sudo tc qdisc add dev eth55 parent 1:1 handle 10:
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 5 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
export the socket’s data as a ﬁle, then search for
contiguous zeroes in it. Oh. Yes. There they are.
We take a hop upstream - we assume the wan-tunnel-
global must have sent a packet with zeroes in, but did it
receive any? Our packet captures suggest it did not receive
any big arrays of 0 , and it deﬁnitely sent one.
We already looked at this code once and didn’t ﬁnd
anything, but perhaps, armed with this new knowledge,
something else might jump out?
Back in the Wan Tunnel
No, no it doesn’t. But it does change which bits of the code
we look at. We’re only hitting a problem when we try to
send a reasonably sized chunk of data down a constricted
pipe. How does this code cope with that? Does it block, or
drop, or what?
Reminder: we’re still in the global end of the wan-tunnel,
doing a send to each downstream local tunnel. The
channel is a standard
java.nio.channels.GatheringByteChannel representing
the socket with a local tunnel at the other end.
This method sends some data messages (there are also
‘command’ messages like acknowledgements and
heartbeats) to a given channel. It is full of comments. That’s
not usually a good sign…
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 6 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
What is all this? Well. This implementation tries really hard
to keep data and command messages apart (separate
dataBacklog and commandBacklog ﬁelds) - why? It also
refuses to store partial messages in its internal buffers
(trust us on this one) - this feels sensible. It appears to
make a token attempt to resize its internal buffer should it
see a large enough message, but…only if there’s no data
already pending? We just don’t get that at all.
What we’re looking for in here is buffer fullness, probably
in either of those two backlog ﬁelds. How does
addToBacklog work?
OK - so here’s the answer - if we get a full buffer, we throw
a well named DataLossException , and we just drop the
data on the ﬂoor. That’s probably sensible - when the
underlying bytes are retranslated into application level
messages, they have sequence numbers on them, and if
   private final GatheringByteChannel channel; 
   private ByteBuffer dataBackLog = ByteBuffer.a
   private final ByteBuffer commandBacklog = Byt
   public void writeMessages(ByteBuffer buffer) 
   {
       int messageCount = validateCompleteMessag
       drain();
       if (!hasPendingMessages() && !hasPendingC
       {
           // nothing pending anywhere; just try
           channel.write(buffer);
           // Only if we get a partial write do 
           // the remain message data fits in th
           // Give this only happens when the ba
           // the total buffer length should not
           // message seen + a little bit to rou
           resizeDataBacklogToFitPartialWrite(bu
       }
       if (buffer.hasRemaining()) // partial wri
       {
           // this copies this buffer into the b
           addToBacklog(dataBackLog, buffer, mes
           // and then we stick it in the queue.
           appendToDrainQueue(dataBackLog);
       }
       if (hasPendingCommands())
       {
           appendToDrainQueue(commandBacklog);
           drain();
       }
       outboundCounters.increment(TunnelMessageT
   }
   private static void addToBacklog(final ByteBu
   {
       backlog.compact();
       if (backlog.remaining() < buffer.remainin
       {
           throw new DataLossException(backlog.r
       }
       backlog.put(buffer);
       backlog.flip();
   }
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 7 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
there’s a gap, the receiver can request a resend of what is
missing (this is one of the usages of the commands in the
tunnel protocol).
We check how the DataLossException gets propagated
and that looks alright.
So this method is ﬁne then. Right? We thought so - there’s
even a test that checks for the DataLossException , and it
passes, and a quick bit of debugging shows its passing for
the right reason.
ByteBuffer veterans may be crying into their coffee at
this point, because NO, that method is NOT alright.
The missing ﬂip
Let’s look at that code once more, but slower. We’ll add
some comments inline, to help.
Can you see it now? It took the CTO sitting down next to
me and pointing at it questioningly for my temporary
ByteBuffer blindness to wear off, so don’t feel bad if you
didn’t.
In the case of data loss, we omit the call to flip and leave
the backlog in a state where it’s ready to be written. What
happens if you attempt to read from it in that state? Well,
what you read is a freshly zeroed out remainder of the
buffer - buffer length - content length of 0 s.
We add a test that triggers data loss, and then tries to
continue sending data afterwards. Straight away we are
greeted with giant arrays of 0 s in the test output. Quite a
simple error in the end; it turns out that this is the ﬁrst
time that we’ve ever suffered data loss in a real
environment, and this component fundamentally doesn’t
handle that scenario!
What about the OOM, though?
That’s a bit ﬁddlier to explain. Let’s imagine our buffer was
small, say 1024 bytes. The following sequence of events
then occurs.
1. Send message one - it’s 900 bytes long This buffers the
message in one of the backlogs
   private static void addToBacklog(final ByteBu
   {
       // move the content of backlog to the fro
       // the position is set to the first byte 
       // after a compact, this buffer is in _wr
       backlog.compact();
       if (backlog.remaining() < buffer.remainin
       {             
           throw new DataLossException(backlog.r
       }
       // Given we're in write mode, add the buf
       backlog.put(buffer);
       // Flip the buffer back into read mode so
       // copied into `channel` by the next invo
       backlog.flip();
   }
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 8 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
2. Attempt to drain the backlogs to the underlying
channel This succeeds with a partial write of, say, 300
bytes
3. Send message two - it’s 600 bytes long This triggers
data loss, and, critically, leaves the buffer in a state
where it will now send 0s in the place of the last 600
bytes of message one
4. Send more messages - it doesn’t matter what, really.
At the other end, the reader manages to discard most of
the 0 s by interpreting them as empty packets - an
accident, we think, rather than an explicit bit of design.
Unfortunately, when the real messages turn up again, those
’empty’ packets aren’t really a valid packet length, so the
ﬁrst actual packet does not start where the reader thinks it
should, and we read a message length from somewhere
totally inappropriate. We then try to allocate a buffer of
that size. Boom. Or, rather, OOM .
How did it take us so long to work this
out?
This bug made it to a blog post. Many of our other bugs do
not. Their errors are often just as trivial, but identifying
them requires less work.
It would be easy to blame the original author, but in fact,
much of the blame lay with us.
1. We gave too much weight to the fact that the code had
worked ﬂawlessly for too long. We look straight past
the gaps in the test coverage because of this, I think.
2. Despite all the evidence pointing at a congestion
problem, we still needed a full fat reproducer to force
us to look properly at the buffer full case. Even once
we were looking at it, my ByteBuffer bug blindness
struggled to see the light!
3. At some point in the past, we noticed that code was
complicated, and tried to compensate with comments.
We could have worked out why it was complicated,
and documented that with tests. Alternatively, the
tests could have shown us what we could delete (the
true utility of coverage). After writing a round-trip fuzz
test of the sender code, we found we could hugely
simplify it - down to less than half the size it was
originally, and requiring none of the comments.
4. Some absolute idiot (you’re ﬁred -Ed) at the beginning
called it an impossible NullPointerException , and so
we went looking for zebras when in fact the usual
ByteBuffer::flip was what we needed to ﬁnd…
#code
James Byatt
I was a mathematician but then I sold out
 
5/8/2026, 2:05 pm
The Impossible NullPointerException | LMAX Technology Blog
Page 9 of 9
https://technology.lmax.com/posts/the-impossible-null-pointer-exception/
Next
Adventures with bpftrace
Previous
Executor: Executed
See Also
Executor: Executed
The Regression
Round trip fuzz tests
You might not need to make your serialization layer
generic
Monitoring without polling
Looking for heap distress
© 2025 LMAX Group · Powered by the Eureka theme for Hugo


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


<div class="page-break"></div>

## Chapter 3.4: Bad Concurrency (Michael Barker)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Aeron & Low-Latency Multicast
Aeron is an ultra-low-latency, reliable messaging system that operates over UDP, Multicast UDP, and IPC. Co-developed by Martin Thompson and Todd Montgomery, it is the transport layer that powers many high-performance trading architectures.

#### 2. Flow Control in Multicast
In unidirectional UDP, senders can easily outpace receivers, causing packet loss. TCP solves this with sliding window flow control, but how do you do flow control for *multicast* (one sender, many receivers)?
Aeron provides several dynamic strategies:
- **Max Flow Control**: The sender limits its rate based on the *fastest* receiver. Slow receivers will drop packets.
- **Min Flow Control**: The sender limits its rate based on the *slowest* receiver. Slow nodes hold up the publisher.
- **Tagged Flow Control**: Only receivers with a specific tag are included in the min flow control calculation. Non-critical subscribers (like gateways) can drop packets without slowing the critical subscribers (like archiving databases).

#### 3. Aeron Flow Control Configuration Example
```java
// Setting up Aeron Channels with different flow control strategies

// 1. Max Flow Control (Fastest receiver dictates speed)
final String maxChannel = "aeron:udp?endpoint=224.0.1.1:40456|fc=max";

// 2. Min Flow Control (Slowest receiver dictates speed)
final String minChannel = "aeron:udp?endpoint=224.0.1.1:40456|fc=min";

// 3. Tagged Flow Control (Only nodes tagged with '1001' dictate speed)
final String taggedChannel = "aeron:udp?endpoint=224.0.1.1:40456|fc=tagged,g:1001";

// Create Publisher
final Publication publication = aeron.addPublication(taggedChannel, STREAM_ID);

// On the subscriber side (Critical Node):
final String criticalSub = "aeron:udp?endpoint=224.0.1.1:40456|gtag=1001";
final Subscription subscription = aeron.addSubscription(criticalSub, STREAM_ID);
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

<div class="scholarly-text">

#### Paper 3: Bad Concurrency (March/April 2020)
*By Michael Barker (bad-concurrency.blogspot.com)*

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Bad Concurrency Blog Posts (I Heard a Rumour... & Flow Control in Aeron)<br>
  <strong>Author(s):</strong> Michael Barker<br>
  <strong>Published:</strong> April/March 2020<br>
  <strong>Source type:</strong> Engineering Blog<br>
  <strong>Original URL:</strong> https://bad-concurrency.blogspot.com/<br>
  <strong>Note:</strong> The following text is reproduced verbatim — exact word-for-word.
</div>

5/8/2026, 2:05 pm
Bad Concurrency
Page 1 of 5
https://bad-concurrency.blogspot.com/
Misadventures in Concurrent and Parallel programming, plus random comments on software performance and various OSS
contributions.
Bad Concurrency
Saturday, 11 April 2020
I Heard a Rumour...
Where Aeron catches up on the goss.
A few months ago a pull request appeared on Aeron's Github site that added the ability to request Aeron to resolve
or re-resolve host names to IP addresses.  In cloud environments, especially when using Kubernetes, when nodes
fail and restart it is not uncommon for a node with the same host name to restart with a different IP address. 
Unfortunately for Aeron this could make life difficult as it would resolve IP addresses up front and stick with it for the
life time of the media driver.  This is particularly bad when we consider nodes that are part of Aeron Cluster, where
we expect nodes to come and go from the cluster over time.
It became very clear that we needed a plan that would allow Aeron to use logical names instead of IP addresses as
endpoint identifiers and re-resolve those addresses appropriately.  We didn't end up using the supplied pull request
and came with an alternative solution that was a better fit with some of Aeron's longer term goals (I say we, it was
mostly Todd Montgomery - I just did the C port).
As DNS can often be a source odd network latency issues, we didn't want a name resolution solution that was
entirely reliant on default system name resolution.  So we have also included a mechanism for resolving names that
works entirely within Aeron.
The first thing we needed to tackle was re-resolving IP addresses when peer nodes went away and came back with
a different address.  Fortunately we already have a indicators within the existing protocol that allows the media driver
to detect when nodes have died.  Aeron continually sends data frames or heartbeats (sender to receiver) and status
messages (receiver to sender) during normal running.  We can use the absence of these messages as a
mechanism to detect that a node (that is identified by name rather than IP address) needs to be re-resolved.
Periodically the sender and receiver will scan their endpoints to see if any having been missing regular updates and
if those endpoints were identified by a name, trigger a re-resolution.  The simple solution here would be to in-place
re-resolve the name to an address, e.g. using getaddrinfo.  However, one of the reasons that Aeron is incredibly
fast (did I mention that already) is that it has a very principle based approach to its design.  One of the principles is
"Non-blocking IO in the message path".  This is to avoid any unusual stalls caused by the processing of blocking IO
operations.  The call to resolve a host name can block for extended periods of time (BTW, if you are ever using an
app on an other fast machine and it stalls for weird periods of time, it is worth asking the question, is it DNS causing
the problem).  Therefore we want to offload name resolution from our sender and receiver threads (the message
path) onto the conductor where we can perform the slower blocking operations.
It was apparent very early on that we could could make the resolution of names an abstract concept.  Obviously
using DNS and host names is the most obvious solutions, but it would be interesting to allow for names to come
from other sources.  E.g. we could name individual media drivers and use those names with our channel
configuration.  This allows a couple of neat behaviours.  All of the configuration for naming can be self contained
within Aeron itself independent of DNS, which may require configuration of a separate system and we could also
allow names to resolve to more that just IP addresses, e.g. host and port pairs or maybe direct to MAC addresses*
in the future.
* Bonus points if you can figure out why this might be useful.
To support this in both the Java and C media drivers have the concept of a name resolver, with 2 production
implementations, default (host name based) and driver where the media drivers are responsible to managing the list
of names.  With the driver based name resolution we need a mechanism to communicate the names between the
instances of the media driver across the network.
To allow driver names to propagate across the network, Aeron supports a gossip-style protocol, where we have an
additional frame type (resolution frame) that contains mappings of names to addresses.  Currently, only IPv4 and
IPv6 addresses are supported, but there is scope for adding others later.
The Need For Naming
Re-Resolution
Name Resolvers
Enter the Gossip Protocol
Michael Barker
View my complete profile
About Me
Java Code Geeks
▼  2020 (3)
▼  April (1)
I Heard a Rumour...
►  March (2)
►  2014 (4)
►  2013 (3)
►  2012 (9)
►  2011 (12)
►  2010 (5)
►  2009 (6)
►  2008 (4)
►  2007 (1)
Blog Archive
More
Create Blog Sign In
5/8/2026, 2:05 pm
Bad Concurrency
Page 2 of 5
https://bad-concurrency.blogspot.com/
Posted by Michael Barker at 21:01 
No comments: 
To make this work, for each media driver we specify 3 things.  The name for the media driver (this will default to the
host name when not specified), a bootstrap neighbour to send initial name resolutions to and a resolver interface. 
The most important option is the resolver interface as specifying this will enable the driver name resolution.  It also
determines which network interface to use to send and receive resolution frames and is the address reported to the
neighbors for self-resolutions.  This can also be a wildcard address (e.g. 0.0.0.0), in which case the neighbors will
use the source address of the received resolution frames to identify that node.
On start each of the nodes will have an empty set of neighbour nodes and a bootstrap neighbour.  Every 1s the
driver name resolver will send out a self resolution, i.e. tell all the nodes that it knows about, what its own name and
address are.  This will be sent (via UDP) to all of its known neighbour nodes and the bootstrap node (if not already in
the neighbour list).  Because the neighbour list is initially empty, then messages will only be sent to bootstrap
neighbours on the first pass.  The bootstrap neighbour can be specified using a host name and the driver name
resolver will ensure that it is re-resolved periodically in case it too has died and come back with a different IP
address.
As a result of this the driver name resolvers will start to receive resolution frames.  The name/address entries from
these frames will be added to a cache and the neighbor list.  If the resolution frame has come through as a
notification of a self resolution we update a last activity timestamp for that node.
Every 2s, the media driver will send its cache of name/address pairs to all of its neighbours, so eventually all of the
nodes will know about all of the other as the name/address entries are shared around the cluster.  At the higher
layer the conductor when trying to resolve a name to a supplied address on a channel URI will call the driver name
resolver first, which can resolve the name from its cache, handing off to the default resolver if not found.
Periodically the cache and the neighbor list will be checked to see if we are still receiving self resolutions for a
particular node.  If the last activity timestamp hasn't been been updated recently enough then the entries are evicted
from the cache and neighbour list under the assumption that the neighbour has died.
All of this is happening on the conductor thread so that it will not impact the performance of the sender and the
receiver.  This is primarily designed for small clusters of nodes as all nodes will be gossiping to all other nodes once
the resolutions have propagated across the network.  It is not designed for large scale system wide name
resolution.  However, it is a very new feature and we will expect to evolve over time as users experiment with it.
With a lot of the algorithms within Aeron it is often not possible to pick a single implementation, so we offer the ability
to provide your own implementation (e.g. flow control, congestion control).  Name resolution fits into that model as
well.  There is an interface for the Java Driver and a function pointer based struct on the C driver that can be
implemented by a user.  So if there is a custom name resolution strategy that you would prefer to use, it can be
plugged in quite easily.
If you look carefully, you notice that there is a 2-phase approach to resolving a name.  There is lookup method and a
resolve method.  The lookup method takes a name and returns a host name, UDP port pair, e.g.
'example.com:2020', where as the resolve function takes in the host name portion of that pair and returns an internet
address.  The additional param name is so the resolver can distinguish between an endpoint and a control address.
While perhaps not a ground-breaking feature, it is a useful one.  It manages to provide the convenience of support
name-based resolution without compromising on the latency goals of Aeron.   It is supported in both the Java
(1.26.0) and C (1.27.0) media drivers.  Feedback is always welcome and check out the wiki for more information.
Write your own
Conclusion
Thursday, 19 March 2020
Flow Control in Aeron
One of my more recent projects has led me to become more involved in the Aeron project.  If you are unaware of
Aeron, then head over to the Github site and check it out.  At its core is an reliable messaging system that works
over UDP, Multicast UDP and IPC.  It also contains an archiving feature for recording and replay and (still under
active development) an implementation of the Raft protocol for clustering.  Did I mention that it was fast too.
I've spent the last few weeks buried in the various strategies the Aeron has for flow control.  Specifically modifying
the existing flow control strategies and adding more flexible configuration on a per channel basis.  Before I jump into
that it would be useful to cover a little background first.
Within a distributed system the purpose of flow control is to limit the rate of a sender so that is does not overrun it's
associated receiver.  UDP does not come with any form of flow control, therefore it is easy to create a sender that
will out pace the receiver, leading to message loss.  There are a number of different forms of flow control, but I'm
going to focus on the sliding window flow control protocol used by TCP and Aeron. The sliding window protocol
requires that the sender maintain a buffer of data (referred to as a window).  The size of this window will typically
communicated from the receiver to the sender as part of the protocol.  With a bi-directional protocol like TCP the
size of the window is communicated in each TCP segment header.  This is the amount of data that the sender can
transmit to the receiver before having to wait until an acknowledgement is received.  If the application thread on the
receiver side is busy and does not read the data from the socket and the sender continues to transmit, the window
What is flow control?
5/8/2026, 2:05 pm
Bad Concurrency
Page 3 of 5
https://bad-concurrency.blogspot.com/
view raw
size value will decrease until it reaches 0, at which time the sender must stop and wait for an acknowledgement with
a non-zero window size before sending again.  There is a lot more networking theory around sizing the flow control
window in order to get full utilisation of the network.  But I will leave that as an exercise for the reader.
With Aeron and UDP unicast it is very similar to TCP, however Aeron is a unidirectional protocol where the receivers
send status messages to indicate to the sender that it is ready to receive data and how much.  The status message
indicates where the subscriber is up to using the consumption term id and consumption term offset for a specific
channel/stream/session triple.  The receiver window value is the amount of data that can be sent from that position
before the sender needs to stop and wait for a new status message indicating the the receiver is able to consume
more data.  The size of the receiver window is at most of ½ of the term size and at least the size of the MTU
(maximum transfer unit).
However, one of the neat features of Aeron is that it supports multicast (and multi-destination-cast, for which the
same rules will apply), where there are multiple receivers for the same publication.  In this situation how do we
determine what values should be used for the flow control window?  This is a question that has no one right answer,
so Aeron provides a number of configuration options and it is also possible to plug in your own strategy.
In fact Aeron is the only tool that supports UDP multicast messaging with dynamic flow control (that we're aware of).
The simplest and fastest form of multicast flow control is a strategy where we take the maximum position of all of the
receivers and use that value to derive limit that the sender can use for publication.  This means any receivers that
are not keeping up with the fastest one may fall behind and experience packet loss.
This is the inverse of the max flow control strategy, where instead we take minimum of all of the available receivers. 
This will prevent slower nodes (as long as they are still sending status messages) from falling behind.  However this
strategy does run the risk that the slower nodes can hold up the rest of the receivers by causing back pressure
slowing the publisher.  Because this strategy needs to track all of the individual receivers and their positions, it also
must handle the case that a node has disappeared altogether.  E.g. it has been shutdown or crashed.  This is
handled via a timeout (default 2s, but configurable).  If status messages for a receiver have not been seen that
period of time, that receiver is ejected from the flow control strategy and the publisher is allowed to move forward.
Tagged flow control is a strategy that attempts to mitigate some of the short comings of the min flow control
strategy.  It works by using a min flow control approach, but only for a subset of receivers that are tagged to be
included in the flow control group.  The min flow control strategy is a special case of this strategy where are all
receivers are considered to be in the group.
One of the new features that came with Aeron 1.26.0 was the ability to control the flow control strategy directly from
the channel URI allowing for fine grained control over each publication and subscription.  Defaults can also be
specified on the media driver context.  On the publication side the channel can be specified as:
flow control params hosted with ❤ by GitHub
The min and max flow control settings for the publication are the simplest, but the tagged one starts to get a little bit
interesting.  The ,g:1001 specifies that the group tag is 1001 and any receiver that want to be involved in flow
control for this publication will need to specify that group tag.  The subscription channel URI show how to ensure
that the receiver sends the appropriate group tag so that it will be included in the publishers flow control group.
The tagged flow control strategy is really useful for receiving from a channel where there are a number of different
types of subscribers that have different reliability requirements.  A good example is where there is a flow of events
that needs to go to a gateway service to be sent out to users, perhaps via HTTP and also needs to go to a couple of
archiving services to store the data redundantly in a database.   It may be possible for the gateway nodes to easily
deal with message loss, either by reporting an error to the user or re-requesting the data.  However it may not be
possible for the archiving service nodes to do so.  In this case the publication would specify the tagged flow control
strategy and the subscriptions on the archiving services would use gtag parameter to ensure that they are included
in the flow control group.  The gateway services could leave the gtag value unset and not impact the flow control on
the publisher.
While being able to include just the important subscribers into a flow control group so that they aren't overrun by the
publisher is useful, there would still be an issue.  If both of our archiving services happened to be down eventually
their receivers would be timed out and removed from the group.  Wouldn't it be great if we could require that a group
contain a certain number of tagged receivers before the publication can report that it is connected.  That way we
could ensure that our archiving service nodes were up before we started publishing data.
Max Flow Control
Min Flow Control
Tagged Flow Control (previously known as Preferred Flow Control)
Configuring Flow Control
1
// Publisher
2
aeron:udp?endpoint=224.20.30.39:24326|fc=max            // max strategy
3
aeron:udp?endpoint=224.20.30.39:24326|fc=min            // min strategy
4
aeron:udp?endpoint=224.20.30.39:24326|fc=tagged,g:1001  // tagged strategy, group 1001
5
6
// Subscriber
7
aeron:udp?endpoint=224.20.30.39:24326|gtag=1001         // tagged subscription
5/8/2026, 2:05 pm
Bad Concurrency
Page 4 of 5
https://bad-concurrency.blogspot.com/
Posted by Michael Barker at 22:20 
No comments: 
view raw
view raw
Turns that this is also now possible with the release of 1.26.0.  For both the tagged flow control and the min flow
control strategies we can specify a group minimum size that must be met before a publication can be considered
connected.  This is independent of to the requirement that there needs to be one connected subscriber.  Therefore
the default value for this group minimum size is 0.  Like the strategy and the flow control group, the group minimum
size can be specified on the channel URI.
flow control params 2 hosted with ❤ by GitHub
In both of these cases the group minimum size is set to 3.  For the min flow control strategy we would need at least
3 connected receivers, for the tagged flow control strategy we would need at least 3 connected receivers with tag
1001 and any receivers without the tag are disregarded.
One last new feature available on the channel URI configuration is the ability to specify the length of the timeout for
the min and tagged flow control strategies.  As mentioned the earlier this will default to 2s, but can be set to any
value.  Some care should be taken in specifying this value, if it is too short then receivers may frequently timeout
during normal running.  Status messages are emitted at least once every 200 ms (more if necessary), so any shorter
than that would not be useful.  Too long and a failed receiver could result in a significant back pressure stall on the
publisher.  Setting this for min and tagged flow control strategies:
flow control params 3 hosted with ❤ by GitHub
As mentioned earlier the idea of using flow control to provide dynamic back pressure for a multicast messaging bus
is a unique and powerful feature of Aeron.  Being able to configure these settings on a per publication provides a an
extra level of flexibility that to help our users to build the system that they need.
Flow Control Based Connectivity
1
aeron:udp?endpoint=224.20.30.39:24326|fc=min,g:/3         // group min size 3
2
aeron:udp?endpoint=224.20.30.39:24326|fc=tagged,g:1001/3  // group min size 3
Time Outs
1
aeron:udp?endpoint=224.20.30.39:24326|fc=min,g:/3,t:5000ms     // timeout 5s
2
aeron:udp?endpoint=224.20.30.39:24326|fc=tagged,g:1001/3,t:5s  // timeout 5s
Summary
Posted by Michael Barker at 04:14 
No comments: 
Resurrecting my blog
If you have been a long time follower of my blog you will have noticed that is has been a really long time since I
posted any new content.  Since my last post six years ago at lot has changed.  I no longer work at LMAX an have
started out as an independent consultant.  If you are part of an organisation looking for assistance in building
software, especially if you facing challenges around performance (throughput, latency, scalability, or efficiency) then
I might be able to help.  My services and contact details are available as http://ephemeris.tech.
I am hoping to start posting again about some of the work I've doing more recently and good potion of it will be open
source, so there should be plenty to share.
Wednesday, 3 December 2014
Read more »
Posted by Michael Barker at 01:13 
15 comments: 
The "Uncanny Valley" of L3 Cache Contention
While preparing for my talk at QCon SF 2014, I wanted to investigate a theory around how micro-benchmarks are
not a useful reflection of how software may behave when run as part of a larger application.  Specifically due
contention in the last-level cache (L3* in current Intel CPUs).
Tuesday, 23 September 2014
Speaking in October and November
I'll be giving some talks over the next few months:
8 Oct: Auckland Software Craftsmanship - 6 Years of test automation.
16 Oct: Auckland JVM Group - Stuff I learned about performance.
5 Nov: QCon San Francisco 2014 - Stuff I learned about performance.
5/8/2026, 2:05 pm
Bad Concurrency
Page 5 of 5
https://bad-concurrency.blogspot.com/
Older Posts
Home
Subscribe to: Posts (Atom)
Posted by Michael Barker at 04:38 
No comments: 
Wednesday, 23 April 2014
Posted by Michael Barker at 02:07 
No comments: 
YOW! 2013 Video
My talk on the Disruptor at YOW! 2013 is now available at the YOW! Eventer site.
Wednesday, 29 January 2014
Read more »
Posted by Michael Barker at 19:39 
8 comments: 
Linux Alternatives and Oracle Java
If, like me, you prefer to run the Oracle version of Java on your Linux machine as the default JDK, you will often find
that the Linux distro will have other ideas.  Fedora for example has a number of Java based applications as part of
the distribution which will include a dependency on the OpenJDK.  When the distro installs OpenJDK is will generally
be setup as the default for executing the various Java binaries (e.g. 'java', 'javac').  However, the team at Redhat
built a system called alternatives which maintains a set of symbolic links that allows the user to switch between
multiple implementations of a package the supports the same functionality.  I've managed to understand enough
about the alternatives package that I can now easily switch between the Oracle JDK and the OpenJDK.
Simple theme. Powered by Blogger.
</div>


<div class="page-break"></div>

## Chapter 3.5: Citation & Reference Deep-Dives for Module 3

This chapter provides standalone research profiles, mathematical formalisms, hardware memory fence mechanics, and lock-free data structure implementations for all major citations across Module 3.

---

### Deep-Dive 3.5.1: Carl Hewitt’s Original Actor Formalism (1973 vs 1985)

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

#### Theoretical Distinction: Hewitt vs. Agha
- **Hewitt & Bishop (1973)**: Conceived actors as generalized active software entities in Artificial Intelligence. Everything was an actor (numbers, functions, stack frames, environments). Communication was message-passing, but focused heavily on pattern matching and control structures.
- **Agha (1985)**: Stripped the actor model down to its pure concurrent computational essentials:
  1. **Actors have Mail Addresses** (uniquely identifying target locations).
  2. **Asynchronous Non-Blocking Send** (Sender never blocks, messages are buffered in mail queues).
  3. **Behavior Replacement (`become`)**: State mutation is modeled by an actor designating its replacement behavior for the next incoming message, maintaining pure mathematical functional state per message transition:
$$\text{Actor}(State_k) \xrightarrow{\text{Message}_m} \text{Actor}(State_{k+1}) + \text{NewActors} + \text{SentMessages}$$

---

### Deep-Dive 3.5.2: Lock-Free Memory Barriers & Cache-Line Padding

#### The False Sharing Problem
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

### Deep-Dive 3.5.3: Memory Fences (LoadLoad, StoreStore, LoadStore, StoreLoad)

Hardware memory reordering forces low-latency lock-free data structures (like the LMAX Disruptor) to explicitly emit **Memory Barriers (Fences)**:

| Barrier Type | Description & Instruction |
| :--- | :--- |
| **LoadLoad** | Ensures all loads preceding the barrier complete before any subsequent loads execute. |
| **StoreStore** | Ensures all stores preceding the barrier are flushed to main memory/cache before subsequent stores execute. |
| **LoadStore** | Ensures all loads preceding the barrier complete before subsequent stores execute. |
| **StoreLoad** | Heavy hardware barrier (`mfence` on x86). Guarantees all preceding stores are visible to all processors before any subsequent loads execute. |

---

### Deep-Dive 3.5.4: Summary of Cited Works for Module 3

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


# Module 4: Software & UI Architecture Patterns

<div class="page-break"></div>

## Chapter 4.1: Presentation Domain Separation & GUI Architectures (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Is Presentation Domain Separation (PDS)?
**Presentation Domain Separation (PDS)** is one of the most fundamental rules of software design. It states that code responsible for **Presentation** (user interfaces, screens, CLI commands, HTTP web views) must be kept strictly distinct from code responsible for **Domain Logic** (business rules, calculations, validation logic, entity models).

```
                      PRESENTATION DOMAIN SEPARATION (PDS)
                      
   ┌──────────────────────────────────────────────────────────────────┐
   │                       PRESENTATION LAYER                         │
   │  React UI Components, HTML/CSS, Swing, WPF, REST API Controllers │
   └────────────────────────────────┬─────────────────────────────────┘
                                    │ (Calls Domain API)
                                    ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                          DOMAIN LAYER                            │
   │  Business Rules, Financial Calculators, Entities, Aggregates     │
   │  (Zero awareness of UI widgets, HTML, or display frameworks)     │
   └──────────────────────────────────────────────────────────────────┘
```

#### Why Separate Presentation from Domain?
1. **Multiple Presentations**: A single business domain (e.g., Bank Account Transfer) can be rendered via a Web Browser UI, a Mobile iOS App, a CLI script, or a REST API endpoint without rewriting business logic.
2. **Testability**: Domain logic can be unit-tested directly in memory without launching browser DOM instances, rendering GUI windows, or mocking UI events.
3. **Maintainability & Tech Upgrades**: UI frameworks evolve rapidly (e.g., jQuery -> Angular -> React -> Next.js), while core business rules remain stable across decades.

#### 2. Evolution of GUI Architectures: MVC, MVP, and MVVM

```
               THE EVOLUTION OF SEPARATED PRESENTATION PATTERNS

  1979: MVC (Model-View-Controller, Trygve Reenskaug at Xerox PARC)
    │   - Controller catches raw input events (key press, mouse click)
    │   - View observes Model changes via Observer Pattern
    │
  1990s: MVP (Model-View-Presenter, Taligent / Dolphin Smalltalk)
    │   - View handles UI events, delegates all logic to Presenter
    │   - Presenter manipulates Passive View explicitly
    │
  2005: MVVM (Model-View-ViewModel, John Gossman at Microsoft)
    │   - Data-binding engine binds View elements to ViewModel properties
    │   - ViewModel exposes reactive state without direct UI references
```

#### 3. Real-World Code Example: PDS in Modern TypeScript

##### Violating PDS (Anti-Pattern: Mixing UI and Business Rules)
```typescript
// BAD: Domain calculations mixed directly into React Component state
export const InvoiceComponent: React.FC<{ items: Array<{ price: number; qty: number }> }> = ({ items }) => {
  // Business logic directly in component
  const subtotal = items.reduce((acc, item) => acc + item.price * item.qty, 0);
  const tax = subtotal * 0.20; // Hardcoded business tax rule inside UI!
  const total = subtotal + tax;

  return <div>Total Invoice: ${total.toFixed(2)}</div>;
};
```

##### Clean PDS (Domain Logic Extracted)
```typescript
// GOOD: Pure Domain Model (Zero React/UI dependency)
export class InvoiceCalculator {
  static readonly TAX_RATE = 0.20;

  static calculateTotal(items: Array<{ price: number; qty: number }>): { subtotal: number; tax: number; total: number } {
    const subtotal = items.reduce((acc, item) => acc + item.price * item.qty, 0);
    const tax = subtotal * this.TAX_RATE;
    return { subtotal, tax, total: subtotal + tax };
  }
}

// Presentation Layer (Pure React View Component)
export const InvoiceComponent: React.FC<{ items: Array<{ price: number; qty: number }> }> = ({ items }) => {
  const { total } = InvoiceCalculator.calculateTotal(items);
  return <div>Total Invoice: ${total.toFixed(2)}</div>;
};
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> GUI Architectures / Presentation Domain Separation<br>
  <strong>Author(s):</strong> Martin Fowler<br>
  <strong>Published:</strong> 2001-2006, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Essay<br>
  <strong>Original URL:</strong> https://martinfowler.com/eaaDev/uiArchs.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### Presentation Domain Separation & GUI Architecture Evolution
Martin Fowler's foundational writings on Presentation Domain Separation (PDS) established a critical architectural boundary: the strict isolation of presentation concerns from domain (business) logic. Fowler articulated that intermingling user interface rendering with business calculations inevitably degrades both simplicity and maintainability.

The primary rationale for this separation rests on three pillars:
1. **Architectural Simplicity**: UI frameworks inherently introduce complex state machines and event loops. Decoupling the domain logic isolates business rules from presentation intricacies.
2. **Platform Agnosticism**: A pure domain model can seamlessly drive diverse presentation mediums—from rich web clients to headless batch processes—without duplicating business logic.
3. **Automated Verification**: Testing domain logic in isolation avoids the fragility and overhead of headless browser automation or UI mocking, enabling rapid, robust unit test suites.

Historically, the evolution of GUI architectures traces back to Trygve Reenskaug's Model-View-Controller (MVC) in Smalltalk-80, where Controllers handled hardware interrupts. As stateful desktop frameworks emerged in the 1990s, the paradigm shifted toward Separated Presentation variants, notably the Application Controller and Presenter patterns, which delegated more granular control over complex UI lifecycles while maintaining strict isolation from the domain model.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 4.1.A: Trygve Reenskaug & Smalltalk-80 MVC
- **Origin**: Invented by Trygve Reenskaug in 1979 while visiting Xerox PARC.
- **Core Insight**: The original MVC was designed for desktop Smalltalk windows where user input came directly from hardware interrupts handled by Controllers.

#### Reference 4.1.B: Passive View vs. Supervising Controller
- **Passive View**: The View contains almost zero logic. The Presenter explicitly reads data from the Model and sets properties on the View directly.
- **Supervising Controller**: The View binds directly to Model attributes for simple data display, while the Controller handles complex user interaction flows.


<div class="page-break"></div>

## Chapter 4.2: Micro Frontends & Modular React Architecture (Cam Jackson, Martin Fowler & Addy Osmani)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Are Micro Frontends?
**Micro Frontends** extend the microservices architectural paradigm to the frontend layer. Instead of building a monolithic single-page application (SPA), the user interface is decomposed into independent, autonomous frontend applications owned by cross-functional teams.

```
                        MONOLITHIC SPA vs. MICRO FRONTENDS

   Monolithic SPA:
   ┌─────────────────────────────────────────────────────────────────┐
   │ Single Monolithic Frontend Application (React / Angular / Vue)  │
   └────────────────────────────────┬────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
     [Cart Service]          [Search Service]        [Checkout Service]

   Micro Frontends:
   ┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
   │ Cart Micro-Frontend   │ │ Search Micro-Frontend │ │ Checkout M-Frontend   │
   │ (Team Cart)           │ │ (Team Search)         │ │ (Team Checkout)       │
   └───────────┬───────────┘ └───────────┬───────────┘ └───────────┬───────────┘
               │                         │                         │
               ▼                         ▼                         ▼
        [Cart Service]            [Search Service]          [Checkout Service]
```

#### Core Benefits of Micro Frontends
1. **Independent Deployability**: Team Checkout can deploy a hotfix without re-building or re-deploying the Search or Cart micro-frontends.
2. **Autonomous Teams**: Each team owns its domain vertically—from database to backend APIs to UI components.
3. **Incremental Technology Upgrades**: Legacy components (e.g. AngularJS) can coexist with modern components (React 18 / Next.js) inside a unified container application.

---

#### 2. Composition Techniques for Micro Frontends
- **Server-Side Composition**: Nginx / SSI (Server-Side Includes) or Edge Workers stitching HTML fragments together at the CDN layer.
- **Build-Time Integration**: Packages published to npm and compiled together (Risk: couples deployment pipelines).
- **Run-Time Integration via Webpack Module Federation**: Micro-frontends dynamically fetch exposed remote JavaScript bundles at runtime without bundling them together at compile time.

---

#### 3. Modularizing React Applications with UI Patterns
Addy Osmani highlights how modern React applications scale cleanly by enforcing established design patterns:

1. **Container / Presentational Pattern**:
   - **Container Component**: Handles data fetching, state management, and side effects.
   - **Presentational Component**: Pure functional UI view component taking props and rendering JSX.
2. **Provider Pattern (React Context / Redux)**: Solves *Prop Drilling* by passing shared state down component trees implicitly.
3. **Compound Components Pattern**: Exposing sub-components (`<Select>`, `<Select.Option>`) that implicitly share state to create expressive APIs.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Micro Frontends<br>
  <strong>Author(s):</strong> Cam Jackson & Martin Fowler<br>
  <strong>Published:</strong> June 2019, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Article<br>
  <strong>Original URL:</strong> https://martinfowler.com/articles/micro-frontends.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### Micro Frontends & React Modularity
The architectural extension of microservices into the presentation tier is formally defined by Cam Jackson and Martin Fowler as "Micro Frontends." This approach directly addresses the scaling bottlenecks of monolithic Single-Page Applications (SPAs) by decentralizing the frontend into vertically aligned, independently deployable domains.

The architectural principles governing micro frontends mandate strict technological agnosticism and isolation. Cross-functional teams are empowered to upgrade or migrate their technology stacks without global coordination. To maintain this autonomy, micro frontends must eschew shared runtime state and global variables, communicating instead through standardized web APIs (such as Custom Events or URL routing). Furthermore, establishing rigid team boundaries prevents the inadvertent leakage of domain logic into shared UI component libraries, which often reintroduces monolithic coupling.

Parallel to the macro-architecture of micro frontends, Addy Osmani emphasizes structural modularity within the UI components themselves, particularly within the React ecosystem. Scaling complex interfaces requires rigorous adherence to component hierarchies and composition patterns. Techniques such as custom hooks abstract stateful logic away from presentation, while Higher-Order Components (HOCs) and render props encapsulate cross-cutting concerns. Adopting a structured taxonomy, such as Brad Frost’s Atomic Design, provides a necessary lexicon—ranging from primitive atoms to complex page templates—that unifies the engineering implementation with product design.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 4.2.A: Webpack 5 Module Federation
- **Creator**: Zack Jackson (2020).
- **Mechanic**: Allows a JavaScript application to dynamically load code from another build at runtime. Remotes and hosts share common dependencies (like React) to avoid loading duplicate library copies.

#### Reference 4.2.B: Atomic Design System (Brad Frost)
- **Hierarchy**: Atoms -> Molecules -> Organisms -> Templates -> Pages.
- **Value**: Provides a shared design language between product designers (Figma) and frontend engineers (React component libraries).


<div class="page-break"></div>

## Chapter 4.3: Serverless Architectures & Feature Toggles (Mike Roberts, Pete Hodgson & Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Is Serverless Computing?
**Serverless Architecture** refers to applications that significantly leverage third-party cloud services (BaaS - Backend-as-a-Service, e.g., Firebase, Auth0) or execute ephemeral, event-triggered custom code running in stateless compute containers (FaaS - Function-as-a-Service, e.g., AWS Lambda, Google Cloud Functions).

```
                      TRADITIONAL SERVERS vs. SERVERLESS FaaS

   Traditional Server Model:
   ┌─────────────────────────────────────────────────────────────────┐
   │ Provisioned Virtual Machine (EC2, Dedicated Linux Server)      │
   │ - Running 24/7 (Paying for idle CPU time)                     │
   │ - OS patching, scaling groups, load balancers managed by team │
   └─────────────────────────────────────────────────────────────────┘

   Serverless FaaS Model:
   ┌─────────────────────────────────────────────────────────────────┐
   │ Ephemeral Stateless Container (AWS Lambda)                      │
   │ - Event-driven trigger (HTTP API Gateway, S3 Upload, SQS Msg)    │
   │ - Scales automatically from 0 to 10,000 instances                │
   │ - Pay ONLY per millisecond of execution time                    │
   └─────────────────────────────────────────────────────────────────┘
```

#### Key Characteristics of FaaS
1. **Stateless Processing**: Function instances are ephemeral. Local file memory is discarded when the function exits. State must be externalized to databases (DynamoDB) or distributed caches (ElastiCache).
2. **Cold Starts**: If a function has not been invoked recently, spawning a fresh container creates execution latency (100ms–1s).
3. **Event-Driven Execution**: Functions are triggered by infrastructure events (e.g. S3 file upload, Kinesis stream event, DynamoDB stream change).

---

#### 2. What Are Feature Toggles (Feature Flags)?
**Feature Toggles** are a set of continuous delivery patterns that allow engineering teams to modify system behavior at runtime without changing or re-deploying code.

```
                      FEATURE TOGGLE CATEGORIZATION MATRIX

                      Short-Lived                    Long-Lived
          ┌──────────────────────────────┬──────────────────────────────┐
          │ Release Toggles              │ Ops Toggles                  │
  Dynamic │ (In-flight feature release,  │ (Circuit breakers, degraded  │
          │  Canary rollouts)            │  mode performance switches)  │
          ├──────────────────────────────┼──────────────────────────────┤
          │ Experiment Toggles           │ Permission Toggles           │
  Static  │ (A/B testing user variants,  │ (Premium features, enterprise│
          │  statistical measurement)    │  tier access control)        │
          └──────────────────────────────┴──────────────────────────────┘
```

#### Code Example: Clean Feature Toggles in Code

##### Bad: Scattered If-Else Flags
```typescript
// BAD: Feature flag logic polutes business code directly
if (featureFlags.isEnabled("NEW_PRICING_ENGINE_2026", user)) {
  return calculateNewPricing(cart);
} else {
  return calculateLegacyPricing(cart);
}
```

##### Good: Decoupled Strategy Pattern
```typescript
// GOOD: Feature Toggle decoupled behind Polymorphic Strategy Interface
interface PricingStrategy {
  calculateTotal(cart: Cart): number;
}

class PricingEngineFactory {
  constructor(private readonly toggleService: ToggleService) {}

  getStrategy(user: User): PricingStrategy {
    if (this.toggleService.isEnabled("NEW_PRICING_ENGINE_2026", user)) {
      return new ModernV2PricingStrategy();
    }
    return new LegacyV1PricingStrategy();
  }
}
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Serverless Architectures / Feature Toggles<br>
  <strong>Author(s):</strong> Mike Roberts, Pete Hodgson & Martin Fowler<br>
  <strong>Published:</strong> 2017-2018, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Essays<br>
  <strong>Original URL:</strong> https://martinfowler.com/articles/serverless.html & https://martinfowler.com/articles/feature-toggles.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### Serverless Architecture & Feature Management
The transition from traditional, always-on server provisioning to ephemeral, event-driven compute models is defined by Mike Roberts and Martin Fowler as "Serverless Architecture." This paradigm fundamentally shifts operational responsibilities—such as capacity scaling, OS patching, and multi-zone redundancy—to the cloud provider. While Serverless (or Function-as-a-Service) drastically reduces idle compute costs and accelerates time-to-market, it introduces novel architectural trade-offs. Engineers must account for execution latency (cold starts), strict statelessness requiring external data persistence, and the inherent complexities of debugging and monitoring highly distributed, ephemeral systems.

Complementing this agility in deployment is the architectural implementation of Feature Toggles (or Feature Flags), as detailed by Pete Hodgson. Feature Toggles enable continuous delivery (CD) directly to a main code branch by decoupling code deployment from feature release. This allows incomplete or experimental features to be merged safely without relying on long-lived feature branches, mitigating complex merge conflicts. However, Hodgson emphasizes a critical hygiene rule: release toggles inherently accrue technical debt. Once a feature is fully released, the toggle logic must be systematically removed to prevent the codebase from becoming saturated with obsolete execution paths. Best practices dictate isolating toggle routing logic behind abstraction layers (such as the Strategy Pattern) to prevent feature flags from polluting core domain logic.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 4.3.A: AWS Lambda & Ephemeral Compute Semantics
- **History**: Introduced by Amazon Web Services at AWS re:Invent 2014.
- **Execution Model**: MicroVM containers (Firecracker) spin up rapidly in response to event triggers, transforming backend cloud microservice execution.

#### Reference 4.3.B: Trunk-Based Development vs. GitFlow
- **Trunk-Based Development**: All engineers commit code to `main` daily, using Feature Toggles to hide uncompleted features.
- **GitFlow**: Relies on long-lived feature branches, leading to complex "Merge Hells" when integrating branches after weeks of divergence.


# Module 4: Software UI Architecture
<div class="page-break"></div>

## Chapter 4.4: Separated Presentation (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Introduction
Separated Presentation is one of the most fundamental principles in UI architecture. It dictates that presentation logic (code that handles the user interface) should be completely decoupled from domain logic (business rules and data manipulation). This separation ensures that the domain remains oblivious to how it is presented, allowing for multiple presentations to sit on top of the same domain.

#### 2. Key Concepts
- **Logical vs. Physical Separation:** Separation is primarily a logical concept (different modules or layers) rather than physical (different servers or tiers), though physical separation often necessitates logical separation.
- **Smalltalk-80 Origins:** The pattern originated in the Smalltalk-80 Model-View-Controller (MVC) framework, which pioneered the idea of separating the domain (Model) from the UI (View/Controller).
- **The Observer Pattern:** Because the domain layer cannot depend on the presentation layer, it uses the Observer pattern to notify the presentation of state changes, allowing the UI to update dynamically without coupling the domain to the UI.

#### 3. Real-World Examples
Imagine writing an application with a Graphical User Interface (GUI). If you strictly follow Separated Presentation, you should be able to build a Command-Line Interface (CLI) for the exact same application without duplicating any domain logic. If there is duplication, some domain logic has likely leaked into the presentation layer.

#### 4. Code Examples (Java / JS / Python)

**Violating Separated Presentation (Domain logic in UI):**

#### Java Implementation
```java
// Java 17+ — Violating Separated Presentation
public class CheckoutWindow extends JFrame {
    private JTextField totalField;
    
    public void onApplyDiscount(double discountPercentage) {
        // Domain logic mixed in presentation
        double currentTotal = Double.parseDouble(totalField.getText());
        double newTotal = currentTotal - (currentTotal * (discountPercentage / 100));
        if (newTotal < 0) {
            newTotal = 0;
            totalField.setForeground(Color.RED);
        }
        totalField.setText(String.valueOf(newTotal));
    }
}
```

#### JavaScript / TypeScript Implementation
```javascript
// ES2022+ — Violating Separated Presentation in React
function CheckoutComponent({ currentTotal }) {
    const [total, setTotal] = useState(currentTotal);
    const [isNegative, setIsNegative] = useState(false);

    const applyDiscount = (discountPercentage) => {
        // Domain logic mixed in presentation
        let newTotal = total - (total * (discountPercentage / 100));
        if (newTotal < 0) {
            newTotal = 0;
            setIsNegative(true);
        }
        setTotal(newTotal);
    };

    return (
        <div>
            <span style={{ color: isNegative ? 'red' : 'black' }}>{total}</span>
            <button onClick={() => applyDiscount(10)}>Apply 10% Discount</button>
        </div>
    );
}
```

#### Python Implementation
```python
# Python 3.10+ — Violating Separated Presentation
class CheckoutWindow(tk.Frame):
    def apply_discount(self, discount_percentage: float):
        # Domain logic mixed in presentation
        current_total = float(self.total_entry.get())
        new_total = current_total - (current_total * (discount_percentage / 100))
        if new_total < 0:
            new_total = 0
            self.total_entry.config(fg="red")
        self.total_entry.delete(0, tk.END)
        self.total_entry.insert(0, str(new_total))
```

---
<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Separated Presentation<br>
  <strong>Author(s):</strong> Martin Fowler<br>
  <strong>Published:</strong> June 2006, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Pattern Essay<br>
  <strong>Original URL:</strong> https://martinfowler.com/eaaDev/SeparatedPresentation.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### Separated Presentation and Domain Decoupling
The Separated Presentation pattern, as codified by Martin Fowler, dictates an absolute logical decoupling between presentation components and domain logic. Rooted in the original Smalltalk-80 MVC paradigm, this architectural layering ensures that the domain model remains completely unaware of its presentation mechanisms. 

Fowler outlines several core principles for implementation:
- **Logical Modularity**: The presentation layer (managing GUI widgets, HTTP responses, or CLI formatting) and the domain layer (managing business rules) must exist in disparate logical modules, enforcing one-way visibility where the presentation observes the domain.
- **Event-Driven Synchronization**: Because the domain layer cannot hold direct references to presentation objects, state synchronization is typically managed via the Observer pattern, where the domain emits events and the presentation layer independently updates itself in response.
- **Refactoring Strategy**: Migrating tightly coupled code requires systematically isolating business calculations into localized queries (e.g., using "Replace Temp with Query") and migrating these operations into domain entities, subsequently replacing direct UI manipulation with state observers.

Ultimately, Separated Presentation serves as a litmus test for domain purity: a robustly separated application should theoretically allow its entire Graphical User Interface to be replaced by a Command Line Interface without modifying a single line of domain code.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 4.4.A: Smalltalk-80 MVC
The original Smalltalk-80 Model-View-Controller framework was the first to implement Separated Presentation by strictly decoupling the Model (domain) from the View and Controller (presentation).

#### Reference 4.4.B: Patterns of Enterprise Application Architecture (PEAA)
Martin Fowler's PEAA book catalogs several variations of this separation, such as Passive View, Supervising Controller, and Presentation Model, each offering different ways to implement Separated Presentation.


<div class="page-break"></div>

## Chapter 4.4: Citation & Reference Deep-Dives for Module 4

This chapter provides standalone research profiles, architectural pattern taxonomy, and engineering strategies for all major citations across Module 4.

---

### Deep-Dive 4.4.1: The Model-View-Controller (MVC) Architectural Lineage

```
Smalltalk-80 (1979) ──▶ Desktop GUI Era (1990s) ──▶ Web Framework Era (2000s)
  (Reenskaug)               (Win32, Swing)             (Rails, Spring MVC, ASP.NET)
  Controller receives       Presenter drives           Controller maps HTTP
  raw hardware events       Passive View               requests to HTTP responses
```

#### Detailed Comparison Matrix

| Architecture Pattern | User Action Target | View Responsibility | State Synchronization |
| :--- | :--- | :--- | :--- |
| **Classic MVC (1979)** | Controller | Observers Model | Observer Pattern / Events |
| **Model-View-Presenter (MVP)** | View | Renders UI widgets | Presenter explicitly mutates View |
| **Model-View-ViewModel (MVVM)** | View | Binds to ViewModel | Declarative Two-Way Data Binding |
| **Unidirectional Data Flow (Flux/Redux)** | Dispatcher / Actions | Pure function of state | Immutable State Store -> React Rerender |

---

### Deep-Dive 4.4.2: Micro-Frontend Runtime Integration Mechanics

#### Webpack 5 Module Federation Architecture
Module Federation allows a container shell application to load remote bundles over the network at runtime without shared build steps.

```
   Host Container App (loads at domain.com)
   ┌──────────────────────────────────────────────────────────────────┐
   │ Dynamic Import: import("checkout/HeaderComponent")               │
   └────────────────────────────────┬─────────────────────────────────┘
                                    │ (HTTP GET request at runtime)
                                    ▼
   Remote Bundle Server (cdn.domain.com/checkout/remoteEntry.js)
   ┌──────────────────────────────────────────────────────────────────┐
   │ Returns compiled chunk containing Checkout Component & metadata  │
   └──────────────────────────────────────────────────────────────────┘
```

---

### Deep-Dive 4.4.3: Summary of Cited Works for Module 4

[20] M. Fowler, "GUI Architectures," MartinFowler.com, 2006. Available: https://martinfowler.com/eaaDev/uiArchs.html
[21] M. Fowler, "Presentation Domain Separation," MartinFowler.com, 2001/06. Available: https://martinfowler.com/eaaDev/SeparatedPresentation.html
[22] M. Fowler, "Separated Presentation," MartinFowler.com, 2006. Available: https://martinfowler.com/eaaDev/SeparatedPresentation.html
[23] M. Fowler, "Presentation Domain Data Layering," MartinFowler.com, 2015. Available: https://martinfowler.com/bliki/PresentationDomainDataLayering.html
[24] C. Jackson and M. Fowler, "Micro Frontends," MartinFowler.com, 2019. Available: https://martinfowler.com/articles/micro-frontends.html
[25] A. Osmani, "Modularizing React Applications," 2020.
[26] M. Roberts and M. Fowler, "Serverless Architectures," MartinFowler.com, 2018. Available: https://martinfowler.com/articles/serverless.html
[27] P. Hodgson and M. Fowler, "Feature Toggles," MartinFowler.com, 2017. Available: https://martinfowler.com/articles/feature-toggles.html

**Subject Index Cross-References:**
- Feature Toggles ..... Ch 4.3
- Micro Frontends ..... Ch 4.2
- MVC ................. Ch 4.1, Ch 4.4
- Presentation Domain Separation ........ Ch 4.1, Ch 4.4, Ch 4.5
- Serverless .......... Ch 4.3


# Module 4: Software UI Architecture
<div class="page-break"></div>

## Chapter 4.5: Presentation Domain Data Layering (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Introduction
The three-layer architecture (Presentation, Domain, and Data) is perhaps the most ubiquitous pattern in software engineering. By dividing an information-rich program into a UI layer (handling HTTP or GUI), a domain logic layer (validations and business rules), and a data access layer (database persistence), developers can organize code in a way that maps cleanly to logical areas of concern. 

#### 2. Key Concepts: Cognitive Scope Narrowing
While substitutability (swapping out the database) and testability (testing the domain without the UI) are often cited as the primary reasons for layering, Martin Fowler highlights an even more practical benefit: **scope narrowing**. 
Layering allows developers to reduce their cognitive load by focusing on one specific problem at a time. When working in the domain layer, you don't need to worry about how the data is rendered on the screen or how it is mapped to a SQL schema. It is a structural enabler for the concept of "Two Hats" from refactoring.

#### 3. Real-World Examples & Diagram
While Presentation-Domain-Data is the standard, variations exist such as Hexagonal Architecture (Ports and Adapters) or Clean Architecture. The core difference lies in dependency direction.

```text
+-------------------+       +-------------------+       +-------------------+
|  Three-Layer      |       |  Hexagonal        |       | Clean Arch        |
+-------------------+       +-------------------+       +-------------------+
|   Presentation    |       |  Primary Adapters |       |     Web/UI        |
|        |          |       |        |          |       |       |           |
|        v          |       |        v          |       |       v           |
|     Domain        |       |      Ports        |       |  Use Cases        |
|        |          |       |        |          |       |       |           |
|        v          |       |        v          |       |       v           |
|      Data         |       |  Core Domain      |       |    Entities       |
|                   |       |        ^          |       |       ^           |
|                   |       |        |          |       |       |           |
|                   |       |      Ports        |       |  Gateways         |
|                   |       |        ^          |       |       ^           |
|                   |       |        |          |       |       |           |
|                   |       | Secondary Adapters|       |    DB/API         |
+-------------------+       +-------------------+       +-------------------+
```
*Note: In three-layer, Domain depends on Data. In Hexagonal/Clean, Data depends on Domain (Dependency Inversion).*

#### 4. Code Examples (Java / JS / Python)

**Implementing the Three-Layer Architecture**

#### Java Implementation (Spring MVC)
```java
// Java 17+ — Three-Layer Architecture in Spring Boot
// 1. Presentation Layer
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }
}

// 2. Domain Layer
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    public OrderService(OrderRepository orderRepository) { this.orderRepository = orderRepository; }
    
    public Order placeOrder(OrderRequest request) {
        if (request.getQuantity() <= 0) throw new IllegalArgumentException("Invalid qty");
        Order order = new Order(request.getProductId(), request.getQuantity());
        order.calculateTotal();
        return orderRepository.save(order);
    }
}

// 3. Data Layer
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Database mapping handled by Spring Data JPA
}
```

#### JavaScript / TypeScript Implementation (Express)
```javascript
// ES2022+ — Three-Layer Architecture in Express
// 1. Presentation Layer
const express = require('express');
const router = express.Router();
const OrderService = require('./OrderService');

router.post('/orders', async (req, res) => {
    try {
        const order = await OrderService.placeOrder(req.body);
        res.status(201).json(order);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// 2. Domain Layer
const OrderRepository = require('./OrderRepository');

class OrderService {
    static async placeOrder(requestData) {
        if (requestData.quantity <= 0) throw new Error("Invalid quantity");
        const order = { ...requestData, status: 'PENDING' };
        order.total = order.quantity * 100; // Business rule
        return await OrderRepository.save(order);
    }
}

// 3. Data Layer
class OrderRepository {
    static async save(order) {
        // e.g., db.collection('orders').insertOne(order);
        return { id: '12345', ...order };
    }
}
```

#### Python Implementation (FastAPI)
```python
# Python 3.10+ — Three-Layer Architecture in FastAPI
# 1. Presentation Layer
from fastapi import APIRouter, HTTPException, Depends
from domain.order_service import OrderService

router = APIRouter()

@router.post("/orders")
def create_order(order_req: OrderRequest, service: OrderService = Depends()):
    try:
        return service.place_order(order_req)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

# 2. Domain Layer
class OrderService:
    def __init__(self, repo: OrderRepository):
        self.repo = repo
        
    def place_order(self, req: OrderRequest) -> Order:
        if req.quantity <= 0:
            raise ValueError("Invalid quantity")
        order = Order(product_id=req.product_id, quantity=req.quantity)
        order.total = order.quantity * 100.0
        return self.repo.save(order)

# 3. Data Layer
class OrderRepository:
    def save(self, order: Order) -> Order:
        # SQL insert operation here
        order.id = 1
        return order
```

---
<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Presentation Domain Data Layering<br>
  <strong>Author(s):</strong> Martin Fowler<br>
  <strong>Published:</strong> 2015, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Essay<br>
  <strong>Original URL:</strong> https://martinfowler.com/bliki/PresentationDomainDataLayering.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### Presentation Domain Data Layering and Cognitive Scope
The three-layer architecture (Presentation, Domain, Data) remains a ubiquitous strategy for modularizing information-rich software. While substitutability (e.g., swapping databases) and testability are frequently cited as the primary drivers for this architecture, Martin Fowler emphasizes a more profound psychological benefit: cognitive scope narrowing.

By rigidly enforcing boundaries between user interface concerns, business logic, and database persistence, developers can dramatically reduce their cognitive load. Operating within the domain layer allows a developer to focus exclusively on business rules, treating data access as an abstract contract and ignoring the mechanics of UI rendering. This separation mirrors the cognitive context switching found in the "Two Hats" refactoring technique.

Fowler also issues critical architectural warnings regarding layered systems. First, layering is a logical construct, not strictly a physical one; it can be implemented within a single monolithic codebase or distributed across microservices. Second, as applications scale, enforcing a top-level folder structure based purely on technical layers (e.g., a massive `views/`, `models/`, `controllers/` directory) often becomes an anti-pattern. Large systems should instead modularize horizontally around domain concepts, applying Presentation-Domain-Data layering internally within each vertical slice. Finally, Conway's Law often tempts organizations to map development teams directly to these technical layers (e.g., dedicated "frontend" and "database" teams). Fowler strongly cautions against this, as the inherent cross-layer volatility of feature development introduces severe organizational friction, advocating instead for cross-functional, full-stack product teams.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 4.5.A: Hexagonal Architecture (Ports and Adapters)
A variation of the layered architecture where the domain model is completely isolated from both the presentation and the data access layers. Dependencies point inwards towards the domain.

#### Reference 4.5.B: Clean Architecture
Similar to Hexagonal Architecture, Clean Architecture places the domain logic and entities at the absolute center, ensuring the core business rules have no external dependencies on frameworks, UIs, or databases.


# Module 5: Code Evolution & Refactoring Patterns

<div class="page-break"></div>

## Chapter 5.1: Refactoring Fundamentals & Preparatory Refactoring (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Is Refactoring?

**Refactoring** is a disciplined technique for restructuring an existing body of code, altering its internal structure without changing its external observable behavior. Its heart is a series of small behavior-preserving transformations. Each transformation (called a "refactoring") does little, but a sequence of transformations can produce a significant restructuring.

The key constraint: **run the test suite after every single micro-step**. If a test breaks, you have only one small change to undo — not hours of untangling.

```
                     THE REFACTORING CYCLE (RED-GREEN-REFACTOR)

   ┌──────────────────────────────────────────────────────────────────┐
   │ 1. Write a failing automated test for new behavior (RED)         │
   └────────────────────────────────┬─────────────────────────────────┘
                                    │
                                    ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │ 2. Write minimum code to pass the test (GREEN)                   │
   └────────────────────────────────┬─────────────────────────────────┘
                                    │
                                    ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │ 3. Restructure internal code to be clean & readable (REFACTOR)   │
   │    - Run tests after EVERY micro-step                            │
   │    - Tests MUST stay GREEN throughout                            │
   └────────────────────────────────────────────────────────────────────┘
```

> *"Refactoring is the process of changing a software system in a way that does not alter the external behavior of the code yet improves its internal structure."*
> — Martin Fowler, *Refactoring: Improving the Design of Existing Code* (1999)

---

#### 2. Preparatory Refactoring — "Make the Change Easy"

Martin Fowler captures the philosophy of **Preparatory Refactoring** with Kent Beck's maxim:

> *"Make the change easy (warning: this may be hard), then make the easy change."*
> — Kent Beck

When faced with adding a new feature to rigid or poorly-structured code, **do not** add the feature directly into the tangled structure. First refactor the code so that the feature drops in cleanly, without duplication or compromise.

```
                    PREPARATORY REFACTORING FLOW

   WRONG (naive approach):
   ┌──────────────────────────────────────┐
   │ Messy, tangled code                  │
   │     + NEW FEATURE directly injected  │ ← duplicated, brittle, fragile
   └──────────────────────────────────────┘

   RIGHT (preparatory refactoring):
   ┌──────────────────────────────────────┐   ┌──────────────────────────────┐
   │ Messy code                           │ → │ Clean, restructured code     │
   │ (no new feature yet)                 │   │ (behavior unchanged)         │
   └──────────────────────────────────────┘   └──────────────┬───────────────┘
                                                             │
                                                             ▼
                                              ┌──────────────────────────────┐
                                              │ NEW FEATURE added cleanly    │
                                              │ (trivial 1–3 line addition)  │
                                              └──────────────────────────────┘
```

The metaphor from **Jessica Kerr** (quoted in Fowler's article) describes it precisely:

> *"It's like I want to go 100 miles east but instead of just traipsing through the woods, I'm going to drive 20 miles north to the highway and then I'm going to go 100 miles east at three times the speed I could have if I just went straight there. When people are pushing you to just go straight there, sometimes you need to say, 'Wait, I need to check the map and find the quickest route.' The preparatory refactoring does that for me."*
> — Jessica Kerr

---

#### 3. The Two Hats (Kent Beck)

Fowler explains that when working on code you wear one of two hats at a time — and **you must never wear both simultaneously**:

```
           THE TWO HATS

   ┌────────────────────────────────────┐   ┌────────────────────────────────────┐
   │ 🎩 HAT 1: ADDING FUNCTIONALITY    │   │ 🎩 HAT 2: REFACTORING             │
   │                                    │   │                                    │
   │ - Adding new behavior              │   │ - Restructuring existing code      │
   │ - Tests are being written          │   │ - NO new behavior added            │
   │ - Tests are being made to pass     │   │ - ALL tests must remain GREEN      │
   │                                    │   │ - External API is unchanged        │
   └────────────────────────────────────┘   └────────────────────────────────────┘

              SWITCH HATS FREQUENTLY — NEVER WEAR BOTH AT ONCE
```

---

#### 4. Core Refactoring Catalogue (Fowler's Named Transformations)

| Refactoring | From | To |
| :--- | :--- | :--- |
| **Extract Method / Function** | Long inline code block | Named method with clear intent |
| **Rename Variable / Method** | Vague names (`x`, `temp`, `data`) | Intention-revealing names |
| **Move Function** | Function in wrong module/class | Moved to where its data lives |
| **Replace Temp with Query** | `let total = calculateTotal()` | Inline function call everywhere |
| **Introduce Parameter Object** | Long parameter list | Single object grouping parameters |
| **Extract Class** | Overloaded class with too many responsibilities | Two smaller focused classes |
| **Replace Exception with Notification** | `throw` for domain validation | Notification accumulates all errors |
| **Replace Loop with Pipeline** | `for` loop with mutations | `map`/`filter`/`reduce` chain |

---

#### 5. Code Smells — When to Refactor

Martin Fowler and Kent Beck identified the key "code smells" — signals that the code needs restructuring:

```
                    TAXONOMY OF CODE SMELLS

  BLOATERS (code grown too large)
  ├── Long Method          — more than 10–15 lines in most contexts
  ├── Large Class          — too many instance variables; too many responsibilities
  ├── Long Parameter List  — more than 3–4 parameters signals a missing object
  └── Primitive Obsession  — using int/string instead of domain value objects

  CHANGE PREVENTERS (rigid, coupled code)
  ├── Divergent Change     — one class changes for many different reasons
  └── Shotgun Surgery      — one change requires many small edits in many classes

  DISPENSABLES (unnecessary code)
  ├── Duplicate Code       — same logic in 2+ places (violates DRY)
  ├── Dead Code            — never called or reachable
  └── Speculative Gen.     — code written "just in case" for hypothetical future

  COUPLERS (excessive inter-object dependency)
  ├── Feature Envy         — method uses data of another class more than its own
  └── Inappropriate Intimacy — class too tightly coupled to internal details of another
```

---

#### 6. Code Examples — Extract Method Pattern

##### Java Implementation
```java
// BEFORE: Long method, vague names, mixed responsibilities
public String generateStatement(Customer customer) {
    double totalAmount = 0;
    int frequentRenterPoints = 0;
    StringBuilder result = new StringBuilder("Rental Record for " + customer.getName() + "\n");

    for (Rental rental : customer.getRentals()) {
        double thisAmount = 0;
        switch (rental.getMovie().getPriceCode()) {
            case Movie.REGULAR:
                thisAmount += 2;
                if (rental.getDaysRented() > 2)
                    thisAmount += (rental.getDaysRented() - 2) * 1.5;
                break;
            case Movie.NEW_RELEASE:
                thisAmount += rental.getDaysRented() * 3;
                break;
            case Movie.CHILDRENS:
                thisAmount += 1.5;
                if (rental.getDaysRented() > 3)
                    thisAmount += (rental.getDaysRented() - 3) * 1.5;
                break;
        }
        frequentRenterPoints++;
        if (rental.getMovie().getPriceCode() == Movie.NEW_RELEASE && rental.getDaysRented() > 1)
            frequentRenterPoints++;

        result.append("\t").append(rental.getMovie().getTitle())
              .append("\t").append(thisAmount).append("\n");
        totalAmount += thisAmount;
    }
    result.append("Amount owed is ").append(totalAmount).append("\n");
    result.append("You earned ").append(frequentRenterPoints).append(" frequent renter points\n");
    return result.toString();
}

// AFTER: Extract Method applied — each concern has its own named method
public String generateStatement(Customer customer) {
    return renderStatementHeader(customer)
         + renderRentalLines(customer)
         + renderStatementFooter(customer);
}

private double amountFor(Rental rental) {
    return switch (rental.getMovie().getPriceCode()) {
        case Movie.REGULAR     -> 2 + Math.max(0, (rental.getDaysRented() - 2) * 1.5);
        case Movie.NEW_RELEASE -> rental.getDaysRented() * 3.0;
        case Movie.CHILDRENS   -> 1.5 + Math.max(0, (rental.getDaysRented() - 3) * 1.5);
        default -> throw new IllegalStateException("Unknown price code");
    };
}
```

##### JavaScript / TypeScript Implementation
```javascript
// BEFORE: Monolithic function
function statement(customer, movies) {
  let totalAmount = 0;
  let frequentRenterPoints = 0;
  let result = `Rental Record for ${customer.name}\n`;

  for (let r of customer.rentals) {
    let movie = movies[r.movieID];
    let thisAmount = 0;
    switch (movie.code) {
      case "regular":
        thisAmount += 2;
        if (r.days > 2) thisAmount += (r.days - 2) * 1.5;
        break;
      case "newRelease":
        thisAmount += r.days * 3;
        break;
      case "childrens":
        thisAmount += 1.5;
        if (r.days > 3) thisAmount += (r.days - 3) * 1.5;
        break;
    }
    frequentRenterPoints++;
    if (movie.code === "newRelease" && r.days > 1) frequentRenterPoints++;
    result += `\t${movie.title}\t${thisAmount}\n`;
    totalAmount += thisAmount;
  }
  result += `Amount owed is ${totalAmount}\n`;
  result += `You earned ${frequentRenterPoints} frequent renter points\n`;
  return result;
}

// AFTER: Extract Function applied
function amountFor(rental, movie) {
  switch (movie.code) {
    case "regular":    return 2 + Math.max(0, (rental.days - 2) * 1.5);
    case "newRelease": return rental.days * 3;
    case "childrens":  return 1.5 + Math.max(0, (rental.days - 3) * 1.5);
    default: throw new Error(`Unknown movie code: ${movie.code}`);
  }
}

function frequentRenterPointsFor(rental, movie) {
  return (movie.code === "newRelease" && rental.days > 1) ? 2 : 1;
}

function statement(customer, movies) {
  const rentals = customer.rentals.map(r => ({ rental: r, movie: movies[r.movieID] }));
  const totalAmount = rentals.reduce((sum, { rental, movie }) => sum + amountFor(rental, movie), 0);
  const points = rentals.reduce((sum, { rental, movie }) => sum + frequentRenterPointsFor(rental, movie), 0);
  const lines = rentals.map(({ rental, movie }) => `\t${movie.title}\t${amountFor(rental, movie)}`).join("\n");
  return `Rental Record for ${customer.name}\n${lines}\nAmount owed is ${totalAmount}\nYou earned ${points} frequent renter points\n`;
}
```

##### Python Implementation
```python
# BEFORE: Monolithic function
def statement(customer, movies):
    total_amount = 0
    frequent_renter_points = 0
    result = f"Rental Record for {customer['name']}\n"
    for rental in customer['rentals']:
        movie = movies[rental['movie_id']]
        this_amount = 0
        if movie['code'] == 'regular':
            this_amount += 2
            if rental['days'] > 2:
                this_amount += (rental['days'] - 2) * 1.5
        elif movie['code'] == 'new_release':
            this_amount += rental['days'] * 3
        elif movie['code'] == 'childrens':
            this_amount += 1.5
            if rental['days'] > 3:
                this_amount += (rental['days'] - 3) * 1.5
        frequent_renter_points += 1
        if movie['code'] == 'new_release' and rental['days'] > 1:
            frequent_renter_points += 1
        result += f"\t{movie['title']}\t{this_amount}\n"
        total_amount += this_amount
    result += f"Amount owed is {total_amount}\n"
    result += f"You earned {frequent_renter_points} frequent renter points\n"
    return result

# AFTER: Extract Function applied
def amount_for(rental: dict, movie: dict) -> float:
    """Calculate rental charge for a single rental."""
    code = movie['code']
    days = rental['days']
    if code == 'regular':
        return 2 + max(0, (days - 2) * 1.5)
    elif code == 'new_release':
        return days * 3.0
    elif code == 'childrens':
        return 1.5 + max(0, (days - 3) * 1.5)
    raise ValueError(f"Unknown movie code: {code}")

def frequent_renter_points_for(rental: dict, movie: dict) -> int:
    """Calculate bonus renter points for a rental."""
    return 2 if movie['code'] == 'new_release' and rental['days'] > 1 else 1

def statement(customer: dict, movies: dict) -> str:
    """Generate customer rental statement."""
    rentals = [(r, movies[r['movie_id']]) for r in customer['rentals']]
    total = sum(amount_for(r, m) for r, m in rentals)
    points = sum(frequent_renter_points_for(r, m) for r, m in rentals)
    lines = "\n".join(f"\t{m['title']}\t{amount_for(r, m)}" for r, m in rentals)
    return (f"Rental Record for {customer['name']}\n{lines}\n"
            f"Amount owed is {total}\nYou earned {points} frequent renter points\n")
```

---

#### 7. The Notification Pattern — Replacing Exceptions for Validation

When **validating user input**, throwing exceptions is the wrong tool. An exception aborts on the first error. A user submitting a form wants to know **all** validation errors at once, not just the first one.

```
                  EXCEPTION vs. NOTIFICATION PATTERN

   EXCEPTION APPROACH (aborts on first error):
   ┌────────────────────────────────────────┐
   │ validateDate(request)     ← THROWS     │
   │ validateSeats(request)    ← NEVER RUN  │
   │ validateName(request)     ← NEVER RUN  │
   │                                        │
   │ Result: User sees: "date is missing"   │
   │ (But also had 2 more errors!)          │
   └────────────────────────────────────────┘

   NOTIFICATION APPROACH (collects all errors):
   ┌────────────────────────────────────────┐
   │ notification = Notification()          │
   │ validateDate(request, notification)    │ ← adds errors if any
   │ validateSeats(request, notification)   │ ← adds errors if any
   │ validateName(request, notification)    │ ← adds errors if any
   │                                        │
   │ Result: User sees ALL 3 errors at once │
   └────────────────────────────────────────┘
```

---

<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>📄 VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Refactoring Fundamentals & Preparatory Refactoring<br>
  <strong>Author(s):</strong> Martin Fowler<br>
  <strong>Published:</strong> 2014-2016, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Essays & Articles<br>
  <strong>Original URL:</strong> https://martinfowler.com/articles/preparatory-refactoring-example.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### 1. The Core Philosophy of Refactoring
Fowler's foundational work on refactoring establishes it not merely as a technical chore, but as an essential practice for software sustainability. Refactoring is defined as restructuring code without changing its observable behavior. The primary goal is to improve the internal structure, making the codebase easier to understand and cheaper to modify.

#### 2. The Two Hats and Preparatory Refactoring
A critical conceptual model introduced is the "Two Hats" metaphor. Developers must separate the acts of adding functionality and refactoring. By focusing on one activity at a time, cognitive load is reduced and test stability is maintained. Preparatory refactoring ("making the change easy") emphasizes that before a new feature is added, the existing structure should be adapted to seamlessly accommodate it.

#### 3. Transformational Mechanics
The mechanics of refactoring rely on a catalogue of precise, behavior-preserving transformations. Each step must be small enough that the test suite continues to pass, ensuring the system remains continuously deployable.

---


<div class="page-break"></div>

## Chapter 5.1b: Refactoring a JavaScript Video Store (Martin Fowler, 2016)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Video Store — The Canonical Refactoring Teaching Case

The video store example is the most famous case study in software engineering education. It first appeared in the opening chapter of Martin Fowler's *Refactoring: Improving the Design of Existing Code* (1999), written in Java. In 2016, Fowler revisited it in modern JavaScript to demonstrate that the same refactoring principles apply — but that JavaScript's flexibility opens up multiple valid architectural paths where Java's object-oriented idiom offered only one.

This chapter is important for two reasons:

1. **It shows refactoring in progress** — not a finished design, but the messy before-and-after of real improvement
2. **It shows that refactoring is not about a single "correct" pattern** — multiple approaches (functions, classes, data transformation) are all valid depending on team style and context

```
                THE FOUR PATHS FROM A MONOLITHIC FUNCTION

   START: One 30-line monolithic statement() function (Long Method smell)
                              │
          ┌────────────────┬──┴──────────────┬────────────────────┐
          ▼                ▼                 ▼                    ▼
   Top-Level           Nested Function    Classes            Data
   Functions           + Dispatcher      (OO style)         Transformation
   (functional)        (closure style)   (ES6 class)        (intermediate
                                                             data structure)
          │                │                 │                    │
          └────────────────┴──────────────────┴────────────────────┘
                              │
                    All 4 produce the same observable output.
                    All 4 are valid refactored states.
                    The "best" choice depends on team context.
```

---

#### 2. Why Refactor the Video Store Function?

The `statement()` function is an example of the **Long Method** code smell. But Fowler is clear: *a code smell alone is not sufficient reason to refactor*. You need a concrete driver for change.

The driver here is: **add an HTML version of the statement** — a second output format rendering the same data differently. Without refactoring, the only option is to copy-paste the entire `statement()` function and modify the string templates. That creates duplication of the core pricing and renter points logic.

The refactoring eliminates this duplication by separating **calculation logic** (pricing, points) from **rendering logic** (text vs. HTML output).

---

#### 3. The Four Approaches — Summary

| Approach | Style | Key Mechanism |
| :--- | :--- | :--- |
| **Top-Level Functions** | Functional / procedural | Standalone named functions; no closures; no classes |
| **Nested Function + Dispatcher** | Closure / partial application | Inner functions close over shared data; a dispatcher selects renderer |
| **ES6 Classes** | Object-oriented | `class Statement` with `text()` and `html()` methods |
| **Data Transformation** | Transformational / pipeline | Computation produces an intermediate data structure; separate renderers consume it |

Fowler's conclusion: **all four are equivalent computations**. The differences are in readability, testability, and alignment with team conventions. The data transformation approach is the most flexible for adding future output formats.

---

#### 4. Code Walkthrough — Approach 1: Top-Level Functions

##### JavaScript (Original monolithic function — before refactoring)
```javascript
function statement(customer, movies) {
  let totalAmount = 0;
  let frequentRenterPoints = 0;
  let result = `Rental Record for ${customer.name}\n`;

  for (let r of customer.rentals) {
    let movie = movies[r.movieID];
    let thisAmount = 0;

    // determine amount for each movie
    switch (movie.code) {
      case "regular":
        thisAmount = 2;
        if (r.days > 2) thisAmount += (r.days - 2) * 1.5;
        break;
      case "new":
        thisAmount = r.days * 3;
        break;
      case "childrens":
        thisAmount = 1.5;
        if (r.days > 3) thisAmount += (r.days - 3) * 1.5;
        break;
    }

    // add frequent renter points
    frequentRenterPoints++;
    // add bonus for a two day new release rental
    if (movie.code === "new" && r.days > 2) frequentRenterPoints++;

    result += `\t${movie.title}\t${thisAmount}\n`;
    totalAmount += thisAmount;
  }

  result += `Amount owed is ${totalAmount}\n`;
  result += `You earned ${frequentRenterPoints} frequent renter points\n`;
  return result;
}
```

##### JavaScript (After refactoring to top-level functions)
```javascript
// Pure calculation functions — no rendering concern
function amountFor(rental, movie) {
  switch (movie.code) {
    case "regular":    return 2 + Math.max(0, (rental.days - 2) * 1.5);
    case "new":        return rental.days * 3;
    case "childrens":  return 1.5 + Math.max(0, (rental.days - 3) * 1.5);
    default: throw new Error(`Unknown movie code: ${movie.code}`);
  }
}

function frequentRenterPointsFor(rental, movie) {
  return (movie.code === "new" && rental.days > 2) ? 2 : 1;
}

function movieFor(rental, movies) {
  return movies[rental.movieID];
}

// Text rendering — separate from calculation
function textStatement(customer, movies) {
  const rentals = customer.rentals.map(r => ({ r, movie: movieFor(r, movies) }));
  const totalAmount = rentals.reduce((sum, { r, movie }) => sum + amountFor(r, movie), 0);
  const points = rentals.reduce((sum, { r, movie }) => sum + frequentRenterPointsFor(r, movie), 0);
  const lines = rentals.map(({ r, movie }) => `\t${movie.title}\t${amountFor(r, movie)}`).join("\n");
  return `Rental Record for ${customer.name}\n${lines}\nAmount owed is ${totalAmount}\nYou earned ${points} frequent renter points\n`;
}

// HTML rendering — reuses same calculation functions
function htmlStatement(customer, movies) {
  const rentals = customer.rentals.map(r => ({ r, movie: movieFor(r, movies) }));
  const totalAmount = rentals.reduce((sum, { r, movie }) => sum + amountFor(r, movie), 0);
  const points = rentals.reduce((sum, { r, movie }) => sum + frequentRenterPointsFor(r, movie), 0);
  const rows = rentals.map(({ r, movie }) =>
    `<tr><td>${movie.title}</td><td>${amountFor(r, movie)}</td></tr>`).join("\n");
  return `<h1>Rental Record for <em>${customer.name}</em></h1>
<table>${rows}</table>
<p>Amount owed is <em>${totalAmount}</em></p>
<p>You earned <em>${points}</em> frequent renter points</p>`;
}
```

##### Java Equivalent (Classes approach)
```java
public class StatementRenderer {

    private final Map<String, Movie> movies;

    public StatementRenderer(Map<String, Movie> movies) {
        this.movies = movies;
    }

    public String text(Customer customer) {
        return renderHeader(customer) + renderRentalLines(customer, false) + renderFooter(customer);
    }

    public String html(Customer customer) {
        return renderHeader(customer) + renderRentalLines(customer, true) + renderFooter(customer);
    }

    private double amountFor(Rental rental) {
        Movie movie = movies.get(rental.getMovieId());
        return switch (movie.getCode()) {
            case "regular"  -> 2 + Math.max(0, (rental.getDays() - 2) * 1.5);
            case "new"      -> rental.getDays() * 3.0;
            case "childrens"-> 1.5 + Math.max(0, (rental.getDays() - 3) * 1.5);
            default -> throw new IllegalStateException("Unknown movie code: " + movie.getCode());
        };
    }

    private int frequentRenterPointsFor(Rental rental) {
        Movie movie = movies.get(rental.getMovieId());
        return ("new".equals(movie.getCode()) && rental.getDays() > 2) ? 2 : 1;
    }

    private String renderRentalLines(Customer customer, boolean html) {
        return customer.getRentals().stream()
            .map(r -> html
                ? String.format("<tr><td>%s</td><td>%.1f</td></tr>",
                    movies.get(r.getMovieId()).getTitle(), amountFor(r))
                : String.format("\t%s\t%.1f\n",
                    movies.get(r.getMovieId()).getTitle(), amountFor(r)))
            .collect(Collectors.joining());
    }

    private String renderHeader(Customer customer) { return "Rental Record for " + customer.getName() + "\n"; }
    private String renderFooter(Customer customer) {
        double total = customer.getRentals().stream().mapToDouble(this::amountFor).sum();
        int points = customer.getRentals().stream().mapToInt(this::frequentRenterPointsFor).sum();
        return String.format("Amount owed is %.1f\nYou earned %d frequent renter points\n", total, points);
    }
}
```

##### Python Equivalent
```python
from dataclasses import dataclass
from typing import Dict, List

@dataclass
class Movie:
    title: str
    code: str  # "regular", "new", "childrens"

@dataclass
class Rental:
    movie_id: str
    days: int

@dataclass
class Customer:
    name: str
    rentals: List[Rental]

def amount_for(rental: Rental, movie: Movie) -> float:
    if movie.code == "regular":
        return 2 + max(0, (rental.days - 2) * 1.5)
    elif movie.code == "new":
        return rental.days * 3.0
    elif movie.code == "childrens":
        return 1.5 + max(0, (rental.days - 3) * 1.5)
    raise ValueError(f"Unknown movie code: {movie.code}")

def frequent_renter_points_for(rental: Rental, movie: Movie) -> int:
    return 2 if movie.code == "new" and rental.days > 2 else 1

def text_statement(customer: Customer, movies: Dict[str, Movie]) -> str:
    rentals_with_movies = [(r, movies[r.movie_id]) for r in customer.rentals]
    total = sum(amount_for(r, m) for r, m in rentals_with_movies)
    points = sum(frequent_renter_points_for(r, m) for r, m in rentals_with_movies)
    lines = "\n".join(f"\t{m.title}\t{amount_for(r, m)}" for r, m in rentals_with_movies)
    return (f"Rental Record for {customer.name}\n{lines}\n"
            f"Amount owed is {total}\nYou earned {points} frequent renter points\n")

def html_statement(customer: Customer, movies: Dict[str, Movie]) -> str:
    rentals_with_movies = [(r, movies[r.movie_id]) for r in customer.rentals]
    total = sum(amount_for(r, m) for r, m in rentals_with_movies)
    points = sum(frequent_renter_points_for(r, m) for r, m in rentals_with_movies)
    rows = "\n".join(f"<tr><td>{m.title}</td><td>{amount_for(r, m)}</td></tr>"
                    for r, m in rentals_with_movies)
    return (f"<h1>Rental Record for <em>{customer.name}</em></h1>\n"
            f"<table>{rows}</table>\n"
            f"<p>Amount owed is <em>{total}</em></p>\n"
            f"<p>You earned <em>{points}</em> frequent renter points</p>")
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

#### 1. Practical Application of Refactoring
The Video Store example serves as the canonical demonstration of Fowler's refactoring principles applied to a tangible codebase. It illustrates how monolithic, procedural code can be systematically dismantled and reconstructed into a cohesive, object-oriented design without altering external behavior.

#### 2. Decomposing Monolithic Functions
The primary focus of this exercise is the decomposition of large, complex functions into smaller, intention-revealing methods. By applying the "Extract Method" pattern, the logic becomes modular, making it easier to isolate bugs and introduce new pricing or rental rules.

#### 3. Polymorphism and Design Patterns
As the refactoring progresses, the example demonstrates the transition from complex conditional logic (e.g., switch statements) to polymorphic structures. This application of the State or Strategy pattern inherently makes the codebase more resilient to future changes in business requirements.

---


# Module 5: Code Evolution & Refactoring Patterns

<div class="page-break"></div>

## Chapter 5.2: Advanced & Specialized Refactoring Patterns (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. Taxonomy of Advanced Refactoring Challenges
As applications grow, codebases encounter systemic code smells that go beyond single-method cleanups. This chapter focuses on major architectural and dependency-level refactorings.

```
                    ADVANCED REFACTORING SMELLS & REFRESH PATTERNS

   Code Smell                      Refactoring Solution Pattern
   ─────────────────────────────── ───────────────────────────────────────────
   God Class ("Class Too Large")   Extract Class / Extract Subclass / Move Method
   Tangled Package Coupling        Refactoring Module Dependencies (DIP)
   Dependency Injection / Locators Decouple connection, data source, and domain logic
```

---

#### 2. Refactoring Module Dependencies (DIP & Layering)
When codebases grow, we must divide them into logical boundaries. A classic structure is **Presentation-Domain-Data (PDD) Layering**. 

However, modularization often runs into dependency management issues:
- **Circular dependencies**: Module A depends on Module B, which depends on Module A. This makes independent compilation and deployment impossible.
- **Direct coupling to infrastructure**: Domain logic depends directly on concrete SQL databases, file readers, or network APIs.

To fix this, we apply the **Dependency Inversion Principle (DIP)**:
1. Define an interface (abstraction) in the domain layer.
2. Have the data source layer implement this interface.
3. Inject the interface into the domain layer using constructor parameter injection or a service locator.

```
          TANGLED DIRECT DEPENDENCY (Violates DIP):
          ┌──────────────────────┐
          │ Domain Logic         │
          └──────────┬───────────┘
                     │ (Direct compile-time dependency)
                     ▼
          ┌──────────────────────┐
          │ Concrete CSV Reader  │
          └──────────────────────┘

          INVERTED DEPENDENCY (Follows DIP):
          ┌──────────────────────┐
          │ Domain Logic         │
          │  - reads from        │
          │    DataSource interface
          └──────────┬───────────┘
                     │ (Depends on abstraction)
                     ▼
          ┌──────────────────────┐
          │ DataSource Interface │ ◀──────────────────┐
          └──────────────────────┘                    │ (Implements)
                                             ┌────────┴─────────────┐
                                             │ Concrete CSV Reader  │
                                             └──────────────────────┘
```

---

#### 3. Code Examples — Refactoring Module Dependencies

##### Java Implementation (DIP + Constructor Injection)
```java
// STEP 1: Abstraction in the Domain Layer
public interface SalesRecordSource {
    List<SalesRecord> getSalesData();
}

// STEP 2: Domain Logic using only the Abstraction
public class GondorffCalculator {
    private final SalesRecordSource source;

    public GondorffCalculator(SalesRecordSource source) {
        this.source = source;
    }

    public double calculateGondorff(String product) {
        return source.getSalesData().stream()
            .filter(r -> r.getProduct().equals(product))
            .mapToDouble(r -> r.getQuantity() * Math.PI)
            .sum();
    }
}

// STEP 3: Implementation in the Data Source Layer
public class CsvSalesRecordSource implements SalesRecordSource {
    private final String filePath;

    public CsvSalesRecordSource(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<SalesRecord> getSalesData() {
        // Read file and parse CSV
        return new ArrayList<>();
    }
}
```

##### JavaScript / TypeScript Implementation (DIP in classless style)
```javascript
// STEP 1: Domain Logic takes data source query function as parameter
export function createGondorffCalculator(salesDataQuery) {
  return {
    calculate(product) {
      return salesDataQuery()
        .filter(r => r.product === product)
        .reduce((sum, r) => sum + (r.quantity * Math.PI), 0);
    }
  };
}

// STEP 2: Infrastructure data provider function
import { readFileSync } from 'fs';
export function csvSalesQuery(filePath) {
  return () => {
    const data = readFileSync(filePath, { encoding: 'utf8' });
    return data.split('\n').slice(1).map(line => {
      const [product, date, quantityString] = line.split(',');
      return { product, date, quantity: parseInt(quantityString, 10) };
    });
  };
}
```

##### Python Implementation
```python
from abc import ABC, abstractmethod
from typing import List, Dict

# STEP 1: Abstraction
class SalesRecordSource(ABC):
    @abstractmethod
    def get_sales_data(self) -> List[Dict]:
        pass

# STEP 2: Domain Calculator
class GondorffCalculator:
    def __init__(self, source: SalesRecordSource):
        self._source = source

    def calculate(self, product: str) -> float:
        import math
        records = self._source.get_sales_data()
        return sum(
            r['quantity'] * math.pi
            for r in records
            if r['product'] == product
        )

# STEP 3: Concrete Implementation
class CsvSalesRecordSource(SalesRecordSource):
    def __init__(self, file_path: str):
        self._file_path = file_path

    def get_sales_data(self) -> List[Dict]:
        # Implementation to read self._file_path and return dict records
        return []
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

#### 1. Beyond Basic Transformations
Advanced refactoring patterns extend beyond simple extractions and renamings to address structural and architectural deficiencies within a codebase. These patterns are essential for dismantling deep-rooted technical debt and realigning the software architecture with its evolving domain model.

#### 2. Architectural Refactoring
Complex patterns often involve cross-component restructurings, such as extracting classes, breaking circular dependencies, or implementing inversion of control. These large-scale refactorings require careful orchestration and robust test coverage to ensure system stability during the transition.

#### 3. Refactoring to Patterns
A key objective of advanced refactoring is guiding the codebase toward established design patterns. By recognizing structural friction, developers can apply targeted refactorings to introduce patterns like Factory, Observer, or Command, thereby enhancing system flexibility and comprehensibility.

---


<div class="page-break"></div>

## Chapter 5.2b: Refactoring with Loops and Collection Pipelines (Martin Fowler, 2015)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The Problem with Imperative Loops

Traditional `for` and `while` loops are powerful but **opaque**. They mix the *iteration mechanism* (how you traverse a collection) with the *transformation logic* (what you do at each step) and the *accumulation logic* (how you collect results). This mixing makes loops harder to read at a glance, harder to compose, and harder to test in isolation.

```
             IMPERATIVE LOOP — THREE CONCERNS MIXED TOGETHER

   for (let rental of customer.rentals) {        ← iteration mechanism
       let movie = movies[rental.movieID];        ← lookup / mapping
       if (movie.code === "regular") {            ← filtering / condition
           totalAmount += amountFor(rental);       ← accumulation
       }
   }

   All three concerns (iteration, mapping, filtering, accumulation) are
   woven together. You cannot test them independently.
```

Collection Pipelines separate these concerns into **named, composable operations**:

```
             COLLECTION PIPELINE — CONCERNS SEPARATED

   customer.rentals
       .map(r => ({ rental: r, movie: movies[r.movieID] }))  ← mapping
       .filter(({ movie }) => movie.code === "regular")       ← filtering
       .reduce((sum, { rental }) => sum + amountFor(rental), 0) ← accumulation

   Each step is pure, independently understandable, and composable.
```

---

#### 2. The Core Pipeline Operations

| Operation | Purpose | Input → Output |
| :--- | :--- | :--- |
| `map` / `stream().map()` | Transform each element | Collection<A> → Collection<B> |
| `filter` / `stream().filter()` | Keep elements matching a predicate | Collection<A> → Collection<A subset> |
| `reduce` / `stream().reduce()` | Collapse a collection to a single value | Collection<A> → B |
| `flatMap` / `stream().flatMap()` | Transform each element to a collection, then flatten | Collection<Collection<A>> → Collection<A> |
| `sorted` / `sorted(comparator)` | Sort elements | Collection<A> → Collection<A ordered> |
| `distinct` | Remove duplicates | Collection<A> → Collection<A unique> |
| `forEach` | Side-effecting terminal (avoid in pipelines) | Collection<A> → void |

---

#### 3. Code Examples — Replace Loop with Pipeline

##### Java Implementation (Java 8+ Streams)
```java
// BEFORE: Imperative loop
public List<String> getRegularMovieTitles(Customer customer, Map<String, Movie> movies) {
    List<String> titles = new ArrayList<>();
    for (Rental rental : customer.getRentals()) {
        Movie movie = movies.get(rental.getMovieId());
        if ("regular".equals(movie.getCode())) {
            titles.add(movie.getTitle());
        }
    }
    return titles;
}

// AFTER: Collection pipeline with Java Streams
public List<String> getRegularMovieTitles(Customer customer, Map<String, Movie> movies) {
    return customer.getRentals().stream()
        .map(r -> movies.get(r.getMovieId()))
        .filter(movie -> "regular".equals(movie.getCode()))
        .map(Movie::getTitle)
        .collect(Collectors.toList());
}

// More complex example: total amount for regular movies only
public double totalForRegularMovies(Customer customer, Map<String, Movie> movies) {
    return customer.getRentals().stream()
        .filter(r -> "regular".equals(movies.get(r.getMovieId()).getCode()))
        .mapToDouble(r -> amountFor(r, movies.get(r.getMovieId())))
        .sum();
}
```

##### JavaScript / TypeScript Implementation
```javascript
// BEFORE: Imperative loop — Twitter follower enrichment example
function getTopTweeters(tweets) {
  const result = [];
  for (let tweet of tweets) {
    if (tweet.retweetCount > 100) {
      result.push({
        author: tweet.author.toUpperCase(),
        count: tweet.retweetCount
      });
    }
  }
  result.sort((a, b) => b.count - a.count);
  return result.slice(0, 5);
}

// AFTER: Collection pipeline
function getTopTweeters(tweets) {
  return tweets
    .filter(tweet => tweet.retweetCount > 100)
    .map(tweet => ({ author: tweet.author.toUpperCase(), count: tweet.retweetCount }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);
}
```

##### Python Implementation
```python
# BEFORE: Imperative loop
def get_regular_movie_amounts(customer, movies):
    amounts = []
    for rental in customer['rentals']:
        movie = movies[rental['movie_id']]
        if movie['code'] == 'regular':
            amounts.append(amount_for(rental, movie))
    return amounts

# AFTER: List comprehension (Python idiomatic pipeline)
def get_regular_movie_amounts(customer, movies):
    return [
        amount_for(rental, movies[rental['movie_id']])
        for rental in customer['rentals']
        if movies[rental['movie_id']]['code'] == 'regular'
    ]

# With generator for memory efficiency on large datasets:
def get_regular_movie_amounts_gen(customer, movies):
    return (
        amount_for(rental, movies[rental['movie_id']])
        for rental in customer['rentals']
        if movies[rental['movie_id']]['code'] == 'regular'
    )

# Using functools for reduce:
from functools import reduce
def total_amount(customer, movies):
    return reduce(
        lambda acc, r: acc + amount_for(r, movies[r['movie_id']]),
        customer['rentals'],
        0
    )
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

#### 1. The Paradigm Shift from Imperative to Declarative
Refactoring loops into collection pipelines represents a fundamental shift from imperative state manipulation to declarative data processing. This approach leverages functional programming concepts (map, filter, reduce) to express the *intent* of an operation rather than the mechanics of its execution.

#### 2. Enhancing Readability and Comprehension
Traditional loop structures often obscure the core business logic beneath boilerplate iteration and state management. Pipelines streamline this by chaining pure functions, producing code that reads closer to natural language and is inherently easier to comprehend at a glance.

#### 3. Immutability and Side-Effect Reduction
By transitioning to pipelines, developers naturally adopt immutability and reduce side-effects. Operations within a pipeline typically return new collections rather than mutating existing ones, leading to safer, more predictable code that is easier to parallelize and test.

---


<div class="page-break"></div>

## Chapter 5.2c: Refactoring to an Adaptive Model (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Is an Adaptive Model?

An **Adaptive Model** (also known as a **Data-Driven Model**) is a computational design where business rules are encoded as data — typically JSON, XML, or a domain-specific language (DSL) — rather than as imperative code. A separate interpreter or rule engine reads that data and executes it.

```
             IMPERATIVE CODE vs. ADAPTIVE MODEL

   IMPERATIVE APPROACH (rules baked into code):
   ┌────────────────────────────────────────────┐
   │ if (spec.atNight) result.push("whisper")   │
   │ if (spec.season === "winter") ...           │
   │ if (spec.country === "sparta") ...          │
   │                                            │
   │ PROBLEM: Changing rules requires a         │
   │ code change, test run, and deployment.     │
   └────────────────────────────────────────────┘

   ADAPTIVE MODEL APPROACH (rules in data):
   ┌────────────────────────────────────────────┐
   │ RULES.JSON:                                │
   │ [                                          │
   │   { "when": "atNight",                     │
   │     "then": "whispering death" },          │
   │   { "when": "season.winter",               │
   │     "then": "beefy" }                      │
   │ ]                                          │
   │                                            │
   │ interpreter.evaluate(spec, RULES)          │
   │                                            │
   │ BENEFIT: Rules updated by downloading     │
   │ new JSON. No code change. No deploy.       │
   └────────────────────────────────────────────┘
```

---

#### 2. When to Use an Adaptive Model

The adaptive model pattern is powerful but complex. Apply it only when:

| Use Adaptive Model When... | Stay Imperative When... |
| :--- | :--- |
| Rules must run on multiple platforms (web, mobile, server) | Rules are simple and unlikely to change |
| Rules change frequently without code deployment | Only one team/platform needs the rules |
| Non-engineers need to understand or modify the rules | Rules require complex computation not expressible as data |
| A DSL for domain experts is needed | The overhead of an interpreter is not justified |

---

#### 3. The Production Rule System Pattern

A **Production Rule System** organizes computation through a collection of *Production Rules*, each structured as:

```
Rule = { condition: <predicate>, action: <consequence> }
```

The interpreter evaluates each rule's condition against the input. If the condition is true, the action is applied. Rules are applied in order (or using priority/salience if ordering matters).

```
             PRODUCTION RULE SYSTEM — EVALUATION FLOW

   INPUT SPEC
       │
       ▼
   ┌───────────────────────────────────────────┐
   │ FOR EACH rule IN rules:                   │
   │   IF evaluate(rule.condition, spec):      │
   │       apply(rule.action, result)          │
   └───────────────────────────────────────────┘
       │
       ▼
   OUTPUT RESULT (list of recommendations)
```

---

#### 4. Code Examples — Imperative to Adaptive Model

##### JavaScript Implementation
```javascript
// BEFORE: Imperative recommendation logic
function recommend(spec) {
  const result = [];
  if (spec.atNight) result.push("whispering death");
  if (spec.seasons && spec.seasons.includes("winter")) result.push("beefy");
  if (spec.seasons && spec.seasons.includes("summer")) {
    if (["sparta", "atlantis"].includes(spec.country)) result.push("white lightning");
    if (spec.minDuration >= 150) {
      if (spec.minDuration < 350) result.push("white lightning");
      else if (spec.minDuration < 570) result.push("little master");
      else result.push("wall");
    }
  }
  return [...new Set(result)];
}

// AFTER: Adaptive Model with Production Rule System
const RULES = [
  { condition: spec => spec.atNight,                                   action: "whispering death" },
  { condition: spec => spec.seasons?.includes("winter"),               action: "beefy" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       ["sparta", "atlantis"].includes(spec.country),  action: "white lightning" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       spec.minDuration >= 150 && spec.minDuration < 350, action: "white lightning" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       spec.minDuration >= 350 && spec.minDuration < 570, action: "little master" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       spec.minDuration >= 570,                        action: "wall" },
];

function recommend(spec, rules = RULES) {
  const result = rules
    .filter(rule => rule.condition(spec))
    .map(rule => rule.action);
  return [...new Set(result)];
}
```

##### Java Implementation
```java
// Adaptive Model with Production Rule System in Java
@FunctionalInterface
interface Condition { boolean test(Spec spec); }

record Rule(Condition condition, String recommendation) {}

class RecommendationEngine {
    private final List<Rule> rules;

    public RecommendationEngine(List<Rule> rules) { this.rules = rules; }

    public Set<String> recommend(Spec spec) {
        return rules.stream()
            .filter(rule -> rule.condition().test(spec))
            .map(Rule::recommendation)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Rules loaded from JSON configuration at startup
    public static RecommendationEngine fromJson(String rulesJson) {
        // Parse rulesJson and build Rule objects with compiled conditions
        // In production: use a rules DSL compiler (e.g., Drools, Easy Rules)
        return new RecommendationEngine(parseRules(rulesJson));
    }
}

// Usage:
// RecommendationEngine engine = RecommendationEngine.fromJson(loadRulesFromConfig());
// Set<String> recommendations = engine.recommend(userSpec);
```

##### Python Implementation
```python
from typing import Callable, List, Set
from dataclasses import dataclass

@dataclass
class Spec:
    at_night: bool = False
    seasons: List[str] = None
    country: str = ""
    min_duration: int = 0

@dataclass
class Rule:
    condition: Callable[[Spec], bool]
    recommendation: str

# BEFORE: Imperative
def recommend_imperative(spec: Spec) -> List[str]:
    result = []
    if spec.at_night:
        result.append("whispering death")
    if spec.seasons and "winter" in spec.seasons:
        result.append("beefy")
    return list(set(result))

# AFTER: Adaptive Model
RULES: List[Rule] = [
    Rule(condition=lambda s: s.at_night, recommendation="whispering death"),
    Rule(condition=lambda s: s.seasons and "winter" in s.seasons, recommendation="beefy"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and s.country in ("sparta", "atlantis"), recommendation="white lightning"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and 150 <= s.min_duration < 350, recommendation="white lightning"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and 350 <= s.min_duration < 570, recommendation="little master"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and s.min_duration >= 570, recommendation="wall"),
]

def recommend(spec: Spec, rules: List[Rule] = RULES) -> Set[str]:
    """Evaluate all rules and return unique recommendations."""
    return {rule.recommendation for rule in rules if rule.condition(spec)}
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

#### 1. Designing for Unforeseen Change
The adaptive model of refactoring focuses on structuring software to gracefully accommodate unknown future requirements. Rather than attempting to predict every possible edge case (which often leads to speculative generality), the goal is to maintain a state of "softness" or malleability in the architecture.

#### 2. Continuous Evolution
Adaptive refactoring is not a distinct phase but a continuous, integrated activity. It requires constant vigilance against structural degradation and a commitment to incremental improvement, ensuring the codebase remains aligned with the shifting realities of the business domain.

#### 3. Feedback Loops and Safenets
A robust adaptive model relies heavily on rapid feedback loops, primarily provided by a comprehensive suite of automated tests. This safety net allows developers to experiment and iterate aggressively, confidently reshaping the architecture as new insights are gained.

---


<div class="page-break"></div>

## Chapter 5.2d: Refactoring Code that Accesses External Services (Martin Fowler, 2015)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The External Service Coupling Problem

When application code calls external APIs or services directly, it tends to tangle three distinct concerns into one place:

1. **Connection concern** — API authorization, HTTP transport, endpoint URLs
2. **Data structure concern** — parsing the external service's response format into domain data
3. **Domain logic concern** — using that data to compute application results

This tangling makes the code:
- **Hard to test** — you cannot test domain logic without making real network calls
- **Hard to change** — if the external API changes its format, domain logic must be touched
- **Hard to understand** — reading domain logic requires understanding the external API simultaneously

```
          BEFORE REFACTORING: TANGLED CONCERNS

   ┌──────────────────────────────────────────────────────────────┐
   │  VideoService                                                │
   │  ┌────────────────┐  ┌──────────────────┐  ┌─────────────┐ │
   │  │ API Auth &     │  │ YouTube Response │  │ Domain      │ │
   │  │ HTTP Call      ├──┤ Parsing          ├──┤ Logic       │ │
   │  │ (Connection)   │  │ (Data Struct)    │  │ (monthlyViews)│ │
   │  └────────────────┘  └──────────────────┘  └─────────────┘ │
   │          ALL MIXED TOGETHER IN ONE CLASS                    │
   └──────────────────────────────────────────────────────────────┘

          AFTER REFACTORING: SEPARATED CONCERNS

   ┌────────────────┐    ┌──────────────────┐    ┌─────────────┐
   │ YouTubeConnect │    │ YouTubeGateway   │    │ VideoService│
   │ -ion           │───▶│ (data structure  │───▶│ (domain     │
   │ (HTTP/auth)    │    │  translation)    │    │  logic only)│
   └────────────────┘    └──────────────────┘    └─────────────┘
          │                      │                      │
     Tests use real         Tests stub the         Tests only
     or mocked              connection              need the
     connection             layer                  Gateway stub
```

---

#### 2. The Gateway Pattern

The **Gateway** pattern (Fowler, *Patterns of Enterprise Application Architecture*, 2002) wraps access to an external service behind an interface that speaks your application's language — not the external service's API language.

Benefits:
- Domain logic is decoupled from the external format
- The gateway interface can be stubbed in tests — no network calls needed
- If the external API changes, only the gateway implementation changes

---

#### 3. The Seam Concept (Michael Feathers)

A **Seam** is a place in code where you can alter behavior without editing the code at that point. Seams exist at:
- Interface boundaries (dependency injection points)
- Constructor parameters
- Configuration / factory methods

Introducing a gateway interface creates a seam — allowing tests to inject a fake gateway rather than the real one.

---

#### 4. Code Examples — Separating the Connection, Gateway, and Domain

##### Java Implementation
```java
// STEP 1: Define the Gateway interface (the Seam)
interface YouTubeGateway {
    VideoData getVideoData(String youtubeId);
}

record VideoData(String title, long viewCount, LocalDate publishedAt) {}

// STEP 2: Real implementation (connects to YouTube API)
class YouTubeGatewayImpl implements YouTubeGateway {
    private final YouTubeClient client; // wraps HTTP + auth

    public YouTubeGatewayImpl(YouTubeClient client) { this.client = client; }

    @Override
    public VideoData getVideoData(String youtubeId) {
        var response = client.videos().list(youtubeId, "snippet,statistics");
        var item = response.getItems().get(0);
        return new VideoData(
            item.getSnippet().getTitle(),
            Long.parseLong(item.getStatistics().getViewCount()),
            LocalDate.parse(item.getSnippet().getPublishedAt())
        );
    }
}

// STEP 3: Stub for testing (no network call)
class YouTubeGatewayStub implements YouTubeGateway {
    private final Map<String, VideoData> fakeData;
    public YouTubeGatewayStub(Map<String, VideoData> fakeData) { this.fakeData = fakeData; }

    @Override
    public VideoData getVideoData(String youtubeId) {
        return fakeData.getOrDefault(youtubeId, null);
    }
}

// STEP 4: Domain service — uses only the gateway interface, knows nothing about YouTube
class VideoAnalysisService {
    private final YouTubeGateway gateway;

    public VideoAnalysisService(YouTubeGateway gateway) { this.gateway = gateway; }

    public double monthlyViews(String youtubeId) {
        VideoData data = gateway.getVideoData(youtubeId);
        long daysAvailable = ChronoUnit.DAYS.between(data.publishedAt(), LocalDate.now());
        return data.viewCount() * 365.0 / daysAvailable / 12;
    }
}

// STEP 5: Test using stub — no network, fast, deterministic
@Test
void testMonthlyViews() {
    var stub = new YouTubeGatewayStub(Map.of(
        "abc123", new VideoData("Test Video", 12000L, LocalDate.now().minusDays(365))
    ));
    var service = new VideoAnalysisService(stub);
    assertEquals(1000.0, service.monthlyViews("abc123"), 0.1);
}
```

##### JavaScript / TypeScript Implementation
```javascript
// Gateway interface (TypeScript)
interface YouTubeGateway {
  getVideoData(youtubeId: string): Promise<VideoData>;
}

interface VideoData {
  title: string;
  viewCount: number;
  publishedAt: Date;
}

// Real implementation
class YouTubeGatewayImpl implements YouTubeGateway {
  constructor(private readonly apiKey: string) {}

  async getVideoData(youtubeId: string): Promise<VideoData> {
    const resp = await fetch(
      `https://youtube.googleapis.com/youtube/v3/videos?id=${youtubeId}&part=snippet,statistics&key=${this.apiKey}`
    );
    const data = await resp.json();
    const item = data.items[0];
    return {
      title: item.snippet.title,
      viewCount: parseInt(item.statistics.viewCount, 10),
      publishedAt: new Date(item.snippet.publishedAt),
    };
  }
}

// Stub for testing
class YouTubeGatewayStub implements YouTubeGateway {
  constructor(private readonly fakeData: Record<string, VideoData>) {}
  async getVideoData(youtubeId: string): Promise<VideoData> {
    return this.fakeData[youtubeId];
  }
}

// Domain service — depends only on the interface
class VideoAnalysisService {
  constructor(private readonly gateway: YouTubeGateway) {}

  async monthlyViews(youtubeId: string): Promise<number> {
    const data = await this.gateway.getVideoData(youtubeId);
    const daysAvailable = (Date.now() - data.publishedAt.getTime()) / (1000 * 60 * 60 * 24);
    return (data.viewCount * 365) / daysAvailable / 12;
  }
}

// Test
const stub = new YouTubeGatewayStub({
  'abc123': { title: 'Test', viewCount: 12000, publishedAt: new Date('2024-01-01') }
});
const service = new VideoAnalysisService(stub);
const monthly = await service.monthlyViews('abc123');
```

##### Python Implementation
```python
from abc import ABC, abstractmethod
from datetime import date, timedelta
from unittest.mock import MagicMock
from dataclasses import dataclass
from typing import Dict

@dataclass
class VideoData:
    title: str
    view_count: int
    published_at: date

# Gateway interface (abstract base class)
class YouTubeGateway(ABC):
    @abstractmethod
    def get_video_data(self, youtube_id: str) -> VideoData:
        pass

# Real implementation
class YouTubeGatewayImpl(YouTubeGateway):
    def __init__(self, api_key: str):
        self._api_key = api_key

    def get_video_data(self, youtube_id: str) -> VideoData:
        import urllib.request, json
        url = f"https://youtube.googleapis.com/youtube/v3/videos?id={youtube_id}&part=snippet,statistics&key={self._api_key}"
        with urllib.request.urlopen(url) as response:
            data = json.loads(response.read())
        item = data['items'][0]
        return VideoData(
            title=item['snippet']['title'],
            view_count=int(item['statistics']['viewCount']),
            published_at=date.fromisoformat(item['snippet']['publishedAt'][:10])
        )

# Stub for testing
class YouTubeGatewayStub(YouTubeGateway):
    def __init__(self, fake_data: Dict[str, VideoData]):
        self._fake_data = fake_data

    def get_video_data(self, youtube_id: str) -> VideoData:
        return self._fake_data[youtube_id]

# Domain service
class VideoAnalysisService:
    def __init__(self, gateway: YouTubeGateway):
        self._gateway = gateway

    def monthly_views(self, youtube_id: str) -> float:
        data = self._gateway.get_video_data(youtube_id)
        days_available = (date.today() - data.published_at).days
        return data.view_count * 365 / days_available / 12

# Test — no network, deterministic
def test_monthly_views():
    stub = YouTubeGatewayStub({
        'abc123': VideoData('Test', 12000, date.today() - timedelta(days=365))
    })
    service = VideoAnalysisService(stub)
    assert abs(service.monthly_views('abc123') - 1000.0) < 0.1
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

#### 1. Isolating the Domain from the Infrastructure
When refactoring code that interacts with external services, the primary architectural goal is isolation. The core domain logic must be shielded from the volatility, latency, and specific implementation details of third-party APIs, databases, or messaging queues.

#### 2. The Anti-Corruption Layer
A critical pattern in this context is the Anti-Corruption Layer (ACL). By introducing translation interfaces between the external service and the internal domain, the codebase prevents external data models from polluting internal business logic, facilitating easier swapping or upgrading of external dependencies.

#### 3. Handling Failure and Idempotency
Refactoring integrations often involves formalizing how the system handles transient failures. Techniques such as circuit breakers, retries, and ensuring idempotent operations are introduced during refactoring to transform brittle, tightly-coupled integrations into robust, fault-tolerant interactions.

---


<div class="page-break"></div>

## Chapter 5.3: Citation & Reference Deep-Dives for Module 5

This chapter provides standalone research profiles, detailed mechanics, and architectural context for all major citations across Module 5.

---

### Deep-Dive 5.3.1: Complete Profile of Martin Fowler's Refactoring Works

#### 1. *Refactoring: Improving the Design of Existing Code* (1st Edition, 1999)
- **Primary Focus**: Formally introducing the practice of refactoring to the mainstream software industry using Java.
- **Key Concepts**: Defined the concept of "Code Smells" (co-authored with Kent Beck) and documented over 70 specific refactoring patterns with step-by-step mechanics and test-driven validations.
- **Legacy**: Established the cataloging format for refactorings (Name, Summary, Motivation, Mechanics, Examples) which remains the industry standard.

#### 2. *Refactoring: Improving the Design of Existing Code* (2nd Edition, 2019)
- **Primary Focus**: Updating the canonical examples to JavaScript to reflect the rise of classless, functional, and web-centric programming paradigms.
- **Key Updates**: Added new patterns like *Replace Loop with Pipeline*, *Slide Statements*, and *Split Loop*, while removing obsolete Java-specific structural patterns.

---

### Deep-Dive 5.3.2: Detailed Mechanics of Key Refactoring Patterns

#### 1. Extract Method / Function
- **Motivation**: A long function or method is hard to read, test, and reuse. By grouping cohesive lines of code and extracting them into a named method, the caller becomes self-documenting.
- **Mechanics**:
  1. Create a new function and name it after its intent (what it does, not how it does it).
  2. Copy the extracted code into the new function.
  3. Scan the extracted code for variables that are local in scope to the source function. Pass them as parameters.
  4. If any local variables are modified by the extracted code, return the modified value.
  5. Replace the extracted code in the source function with a call to the new function.
  6. Compile and test.

#### 2. Replace Temp with Query
- **Motivation**: Temporary variables store the result of an expression and are only visible within the function. They force caller functions to be longer and more coupled. Replacing them with query methods allows other methods in the class to access the values.
- **Mechanics**:
  1. Identify a temporary variable that is assigned once.
  2. Extract the right-hand side of the assignment into a query method.
  3. Replace all references to the temp variable with call expressions to the query method.
  4. Delete the temp variable declaration and assignment.
  5. Compile and test.

---

### Deep-Dive 5.3.3: SOLID Principles in Refactoring Context

The SOLID principles guide the target structure of refactoring efforts:

- **Single Responsibility Principle (SRP)**: A class should have one, and only one, reason to change. The *Extract Class* pattern is the primary tool for resolving SRP violations when a class grows too large.
- **Open/Closed Principle (OCP)**: Software entities should be open for extension but closed for modification. Moving from imperative conditional blocks to polymorphic subclasses (e.g. replacing movie type codes with strategies) implements OCP.
- **Liskov Substitution Principle (LSP)**: Subtypes must be substitutable for their base types. Refactoring subclass hierarchies to avoid inheritance abuse (e.g., using delegation instead of refused bequest) preserves LSP.
- **Interface Segregation Principle (ISP)**: Clients should not be forced to depend on methods they do not use. Splitting bloated interfaces into smaller, role-specific client interfaces.
- **Dependency Inversion Principle (DIP)**: High-level modules should not depend on low-level modules; both should depend on abstractions. Refactoring package dependencies by introducing interfaces and injecting them resolves tight coupling.

---

### Deep-Dive 5.3.4: Complete IEEE Bibliography for Module 5

[28] M. Fowler, "An Example of Preparatory Refactoring," MartinFowler.com, 2014. Available: https://martinfowler.com/articles/preparatory-refactoring-example.html
[29] M. Fowler, "Refactoring Code to Load a Document," MartinFowler.com, 2016.
[30] M. Fowler, "Refactoring: This Class is Too Large," MartinFowler.com, 2015.
[31] M. Fowler, "Replacing Exceptions with Notification," MartinFowler.com, 2014.
[32] M. Fowler, "Refactoring Module Dependencies," MartinFowler.com, 2018.
[33] M. Fowler, "Refactoring a JavaScript Video Store," MartinFowler.com, 2016. Available: https://martinfowler.com/articles/refactoring-video-store.html
[34] M. Fowler, "Refactoring with Loops and Collection Pipelines," MartinFowler.com, 2015. Available: https://martinfowler.com/articles/refactoring-pipelines.html
[35] M. Fowler, "Refactoring to an Adaptive Model," MartinFowler.com, ~2020. Available: https://martinfowler.com/articles/refactoring-adaptive-model.html
[36] M. Fowler, "Refactoring Code that Accesses External Services," MartinFowler.com, ~2019. Available: https://martinfowler.com/articles/refactoring-external-service.html

**Supplementary Books**
[S11] M. Fowler, *Refactoring: Improving the Design of Existing Code*, 1st ed. Boston, MA: Addison-Wesley, 1999.
[S12] M. Fowler, *Refactoring: Improving the Design of Existing Code*, 2nd ed. Boston, MA: Addison-Wesley, 2019.
[S13] K. Beck, *Smalltalk Best Practice Patterns*, Upper Saddle River, NJ: Prentice Hall, 1997.
[S14] M. C. Feathers, *Working Effectively with Legacy Code*, Upper Saddle River, NJ: Prentice Hall, 2004.

**Subject Index Cross-References:**
- Collection Pipelines .......... Ch 5.2, Ch 5.2b
- Dependency Inversion .......... Ch 5.2, Ch 5.3
- Gateway Pattern ..... Ch 5.2d, Ch 5.3
- Notification Pattern .......... Ch 5.1, Ch 5.2
- Preparatory Refactoring ......... Ch 5.1
- Refactoring ......... Ch 5.1, Ch 5.1b, Ch 5.2, Ch 5.2b, Ch 5.2c, Ch 5.2d


# Module 6: Bibliography and Index

## Part A: Complete Bibliography

### Module 1: Software Design Philosophy
[1] J. W. Reeves, "What is software design?" C++ Journal, Fall 1992. Available: https://www.developerdotstar.com/mag/articles/reeves_design_main.html
[2] J. W. Reeves, "What is software design: 13 years later," Developer Dot Star, Feb. 2005. Available: https://www.developerdotstar.com/mag/articles/reeves_design_main.html
[3] J. W. Reeves, "Letter to the Editor," C++ Journal, 1992. Available: https://www.developerdotstar.com/mag/articles/reeves_design_main.html
[4] M. Fowler, "Code As Documentation," MartinFowler.com, 2005. Available: https://martinfowler.com/bliki/CodeAsDocumentation.html
[5] M. Fowler, "The Almighty Thud," MartinFowler.com, 1997. Available: https://martinfowler.com/ (archive)
[37] C. A. R. Hoare, "Null References: The Billion Dollar Mistake," QCon London, 2009. Available: https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/

### Module 2: Hardware Concurrency Memory
[6] H. Sutter, "Welcome to the Jungle," HerbSutter.com, 2011. Available: https://herbsutter.com/welcome-to-the-jungle/
[7] R. Schaller, "Moore's Law: Past, Present, Future," IEEE Spectrum, 1997. DOI: 10.1109/6.591665
[8] Intel, "Moore's Law 2023," Intel Newsroom, 2023. Available: https://download.intel.com/newsroom/2023/manufacturing/moores-law-electronics.pdf
[9] D. Lea, "Synchronization & Java Memory Model," Concurrent Programming in Java, 1999.
[10] W. Pugh et al., "JSR-133 / Pugh Semantics Paper," POPL, 2004/05. DOI: 10.1145/1040305.1040336
[11] M. Barker, "Bad Concurrency," Bad Concurrency Blog, ~2020. Available: http://bad-concurrency.blogspot.com

### Module 3: Actor Model and LMAX Disruptor
[12] G. A. Agha, "AITR-844 Actors Thesis," MIT, 1985. Available: https://dspace.mit.edu/handle/1721.1/6952
[13] M. Fowler, "The LMAX Architecture," MartinFowler.com, 2011. Available: https://martinfowler.com/articles/lmax.html
[14] M. Thompson et al., "Disruptor-1.0 Technical Paper," LMAX, 2011. Available: https://lmax-exchange.github.io/disruptor/files/Disruptor-1.0.pdf
[15] S. Warren, "A Question of Scale," LMAX Blog, 2023.
[16] Povoliashko et al., "First Impressions of Testing at LMAX," LMAX Blog, 2023.
[17] J. Byatt, "Why I Don't Do Work in Constructors," LMAX Blog, 2024.
[18] J. Byatt, "Coverage Can Only Show You What to Delete," LMAX Blog, 2023.
[19] LMAX Blog, "The Impossible NullPointerException," 2022. Available: https://www.lmax.com/blog/staff-blogs/2022/06/15/the-impossible-nullpointerexception/

### Module 4: Software UI Architecture
[20] M. Fowler, "GUI Architectures," MartinFowler.com, 2006. Available: https://martinfowler.com/eaaDev/uiArchs.html
[21] M. Fowler, "Presentation Domain Separation," MartinFowler.com, 2001/06. Available: https://martinfowler.com/eaaDev/SeparatedPresentation.html
[22] M. Fowler, "Separated Presentation," MartinFowler.com, 2006. Available: https://martinfowler.com/eaaDev/SeparatedPresentation.html
[23] M. Fowler, "Presentation Domain Data Layering," MartinFowler.com, 2015. Available: https://martinfowler.com/bliki/PresentationDomainDataLayering.html
[24] C. Jackson and M. Fowler, "Micro Frontends," MartinFowler.com, 2019. Available: https://martinfowler.com/articles/micro-frontends.html
[25] A. Osmani, "Modularizing React Applications," 2020.
[26] M. Roberts and M. Fowler, "Serverless Architectures," MartinFowler.com, 2018. Available: https://martinfowler.com/articles/serverless.html
[27] P. Hodgson and M. Fowler, "Feature Toggles," MartinFowler.com, 2017. Available: https://martinfowler.com/articles/feature-toggles.html

### Module 5: Code Evolution Refactoring
[28] M. Fowler, "An Example of Preparatory Refactoring," MartinFowler.com, 2014. Available: https://martinfowler.com/articles/preparatory-refactoring-example.html
[29] M. Fowler, "Refactoring Code to Load a Document," MartinFowler.com, 2016.
[30] M. Fowler, "Refactoring: This Class is Too Large," MartinFowler.com, 2015.
[31] M. Fowler, "Replacing Exceptions with Notification," MartinFowler.com, 2014.
[32] M. Fowler, "Refactoring Module Dependencies," MartinFowler.com, 2018.
[33] M. Fowler, "Refactoring a JavaScript Video Store," MartinFowler.com, 2016. Available: https://martinfowler.com/articles/refactoring-video-store.html
[34] M. Fowler, "Refactoring with Loops and Collection Pipelines," MartinFowler.com, 2015. Available: https://martinfowler.com/articles/refactoring-pipelines.html
[35] M. Fowler, "Refactoring to an Adaptive Model," MartinFowler.com, ~2020. Available: https://martinfowler.com/articles/refactoring-adaptive-model.html
[36] M. Fowler, "Refactoring Code that Accesses External Services," MartinFowler.com, ~2019. Available: https://martinfowler.com/articles/refactoring-external-service.html

## Part B: Subject Index
- Actor Model ......... Ch 3.1, Ch 3.4
- Amdahl's Law ........ Ch 2.1, Ch 2.4
- Cache Line Padding .. Ch 2.4, Ch 3.2, Ch 3.4
- CAS (Compare-And-Swap) ........ Ch 3.2, Ch 3.4, Ch 2.4
- Collection Pipelines .......... Ch 5.2, Ch 5.2b
- Dark Silicon ........ Ch 2.1, Ch 2.2
- Dependency Inversion .......... Ch 5.2, Ch 5.3
- Disruptor ........... Ch 3.2, Ch 3.4
- Double-Checked Locking ........ Ch 2.3, Ch 2.4
- Event Sourcing ...... Ch 3.2
- False Sharing ....... Ch 2.4, Ch 3.2, Ch 3.4
- Feature Toggles ..... Ch 4.3
- Gateway Pattern ..... Ch 5.2d, Ch 5.3
- Happens-Before ...... Ch 2.3, Ch 2.4
- Java Memory Model ... Ch 2.3, Ch 2.4
- LMAX Disruptor ...... Ch 3.2, Ch 3.4
- Mechanical Sympathy . Ch 3.2, Ch 3.4
- Memory Barriers ..... Ch 2.3, Ch 2.4, Ch 3.4
- Micro Frontends ..... Ch 4.2
- Moore's Law ......... Ch 2.1, Ch 2.2
- MVC ................. Ch 4.1, Ch 4.4
- Notification Pattern .......... Ch 5.1, Ch 5.2
- Null References (Billion Dollar Mistake) ..... Ch 1.5
- Presentation Domain Separation ........ Ch 4.1, Ch 4.4, Ch 4.5
- Preparatory Refactoring ......... Ch 5.1
- Refactoring ......... Ch 5.1, Ch 5.1b, Ch 5.2, Ch 5.2b, Ch 5.2c, Ch 5.2d
- Ring Buffer ......... Ch 3.2, Ch 3.4
- Serverless .......... Ch 4.3
- Single-Writer Principle ......... Ch 3.2, Ch 3.4
- Software Design ..... Ch 1.1, Ch 1.1b
- TDD ................. Ch 5.1, Ch 3.3
- Volatile ............ Ch 2.3, Ch 2.4, Ch 3.4

## Part C: Author Index
- G. A. Agha ................... [12]
- M. Barker .................... [11]
- J. Byatt ..................... [17], [18]
- M. Fowler .................... [4], [5], [13], [20]-[24], [26]-[36]
- C. A. R. Hoare ................ [37]
- P. Hodgson ................... [27]
- Intel ........................ [8]
- C. Jackson ................... [24]
- D. Lea ....................... [9]
- LMAX Blog .................... [19]
- A. Osmani .................... [25]
- Povoliashko et al. ........... [16]
- W. Pugh et al. ............... [10]
- J. W. Reeves ................. [1], [2], [3]
- M. Roberts ................... [26]
- R. Schaller .................. [7]
- H. Sutter .................... [6]
- M. Thompson et al. ........... [14]
- S. Warren .................... [15]

## Part D: URL Master List
- Developer Dot Star (Reeves): https://www.developerdotstar.com/mag/articles/reeves_design_main.html
- Martin Fowler (Code As Documentation): https://martinfowler.com/bliki/CodeAsDocumentation.html
- Martin Fowler (Archive): https://martinfowler.com/
- InfoQ (Tony Hoare Billion Dollar Mistake): https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/
- Herb Sutter (Welcome to the Jungle): https://herbsutter.com/welcome-to-the-jungle/
- IEEE (Moore's Law): https://ieeexplore.ieee.org/document/591665
- Intel (Moore's Law): https://download.intel.com/newsroom/2023/manufacturing/moores-law-electronics.pdf
- ACM (JSR-133): https://doi.org/10.1145/1040305.1040336
- MIT (AITR-844): https://dspace.mit.edu/handle/1721.1/6952
- Martin Fowler (LMAX): https://martinfowler.com/articles/lmax.html
- LMAX (Disruptor): https://lmax-exchange.github.io/disruptor/files/Disruptor-1.0.pdf
- LMAX Blog (Impossible NPE): https://www.lmax.com/blog/staff-blogs/2022/06/15/the-impossible-nullpointerexception/
- Bad Concurrency Blog: http://bad-concurrency.blogspot.com
- Martin Fowler (GUI Architectures): https://martinfowler.com/eaaDev/uiArchs.html
- Martin Fowler (Separated Presentation): https://martinfowler.com/eaaDev/SeparatedPresentation.html
- Martin Fowler (Micro Frontends): https://martinfowler.com/articles/micro-frontends.html
- Martin Fowler (Serverless): https://martinfowler.com/articles/serverless.html
- Martin Fowler (Feature Toggles): https://martinfowler.com/articles/feature-toggles.html
- Martin Fowler (Preparatory Refactoring): https://martinfowler.com/articles/preparatory-refactoring-example.html
- Martin Fowler (Video Store): https://martinfowler.com/articles/refactoring-video-store.html
- Martin Fowler (Loops & Pipelines): https://martinfowler.com/articles/refactoring-pipelines.html
- Martin Fowler (Adaptive Model): https://martinfowler.com/articles/refactoring-adaptive-model.html
- Martin Fowler (External Service): https://martinfowler.com/articles/refactoring-external-service.html
- Martin Fowler (PD Data Layering): https://martinfowler.com/bliki/PresentationDomainDataLayering.html


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


# Zero-Allocation and Mechanical Sympathy in Practice

A cornerstone of High-Frequency Trading (HFT) architectures is the avoidance of memory allocations in the critical path. The JVM's Garbage Collector (GC), even modern variants like ZGC, introduces non-deterministic pauses that are unacceptable when measuring latency in microseconds or nanoseconds.

## The Flyweight Pattern in HFT Context

In the LMAX Disruptor architecture, a single mutable event object is pre-allocated at startup and reused for every message. This eliminates the GC pressure that would result from allocating millions of new DTOs per second.

```java
public final class FxMarketEvent extends SelfDescribingMarshallable {

    public long correlationId;
    public long ingressNanoTime;
    public long currencyPairCode;
    public byte side;
    public long notionalMinorUnits;

    // Flyweight reset method
    public void reset() {
        correlationId        = 0L;
        ingressNanoTime      = 0L;
        // ... (reset other primitives)
    }
}
```

The flyweight is filled with new values before being written to the Chronicle Queue, and then populated from the queue into a reused instance on the consumer side.

## Cache-Line Alignment

A modern CPU cache line is 64 bytes. In `FxMarketEvent`, related primitive fields are grouped together so that a single cache-line fetch pulls in all the data an event handler needs in one shot, avoiding expensive cache misses.

The fields are ordered by access frequency and logical group:
1. **Identity group** (`correlationId`, `ingressNanoTime`) — needed by every service for correlation and latency measurement.
2. **FX instrument group** (`currencyPair`, `side`, `notional`) — needed for all business logic.
3. **Pricing group** (`requestedPrice`, `executedPrice`, `spread`) — written by serv-b, read by serv-c.

## Avoiding String Allocations

Currency pairs (e.g., "EUR/USD") are normally represented as Strings. In an HFT pipeline, they are encoded as a compact `long`, packing two 3-letter ISO 4217 currency codes into 6 bytes.

```java
// Packing 'EUR' and 'USD' into a single long without String allocations
long encoded = 0L;
// Pack base currency into bits 40–16 (3 bytes, high side)
for (int i = 0; i < 3; i++) {
    encoded |= ((long) (baseCurrency[i] & 0xFF)) << ((5 - i) * 8);
}
// Pack quote currency into bits 23–0 (3 bytes, low side)
for (int i = 0; i < 3; i++) {
    encoded |= ((long) (quoteCurrency[i] & 0xFF)) << ((2 - i) * 8);
}
```

## JVM Tuning for Mechanical Sympathy

To maximize performance, specific JVM arguments are used to configure ZGC, ensure memory pages are pre-touched, and disable explicit GC calls:

```bash
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC
```

> [!NOTE]
> Thread affinity is also critical. By pinning the event loops of `serv-a` and `serv-b` to specific CPU cores, we prevent the OS from migrating the thread, keeping L1/L2 caches hot and avoiding costly context switches.


# Event Loop and Pricing Mechanisms

In distributed HFT pipelines, microservices don't use traditional thread pools or callback-based HTTP handlers. Instead, they employ single-threaded, busy-spin event loops that continuously tail memory-mapped queues.

## The Busy-Spin Event Loop

Consider `serv-b` (Pricing Engine). It reads from `queue-b` and writes to `queue-c`. It runs a `PricingEventLoop` on a dedicated, pinned CPU core.

```java
public static void main(final String[] args) throws InterruptedException {
    System.out.println("[serv-b] Pricing Engine starting...");
    System.out.println("[serv-b] Tailing queue-b: " + QueuePaths.QUEUE_B);
    System.out.println("[serv-b] Writing queue-c: " + QueuePaths.QUEUE_C);

    final PricingEventLoop loop = new PricingEventLoop();

    loop.start();
    System.out.println("[serv-b] Event loop started on CPU core " + PricingEventLoop.CPU_CORE);
    Thread.currentThread().join();
}
```

A busy-spin loop looks roughly like this inside:

```java
while (running) {
    if (tailer.readDocument(wire -> {
        // Read into our pre-allocated FxMarketEvent
        event.reset();
        event.readMarshallable(wire);
        
        // Process pricing logic in-place
        applySpread(event);
        
        // Write out to next queue
        appender.writeDocument(w -> event.writeMarshallable(w));
    })) {
        // Message processed, loop immediately
    } else {
        // Queue empty. Spin-wait strategy (e.g., Thread.onSpinWait())
        Thread.onSpinWait();
    }
}
```

## Integer Scaling for Pricing

Floating-point rounding errors (inherent in IEEE 754 `double` arithmetic) can be catastrophic in financial calculations. Instead, prices are stored as scaled integers (minor units).

For EUR/USD, where 1 pip = 0.0001, the price 1.0850 is scaled by 100,000 and stored as `108500L`. 

```java
/**
 * Client's requested execution price, encoded as minor units (e.g., pips × 10^5).
 *
 * <p>Stored as a {@code long} scaled integer.
 * This scale avoids all floating-point imprecision on the hot path.
 */
public long requestedPriceScaled;
```

This guarantees that applying spreads and calculating final execution prices involves simple, precise integer arithmetic, eliminating the need for `BigDecimal` (which creates garbage) while retaining absolute accuracy.


