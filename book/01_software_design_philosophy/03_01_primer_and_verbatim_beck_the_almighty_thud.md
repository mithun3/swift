<div class="page-break"></div>

# Chapter 1.4: The Almighty Thud (Kent Beck & Martin Fowler)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. The "Thud" Phenomenon in Enterprise Architecture
In traditional enterprise software projects, management and architecture teams frequently evaluated design quality by weight and volume. When an external consultant or architect delivered an exhaustive design document, it arrived in massive physical binders. Dropping it on a desk produced a heavy, physical **"Thud"**.

```
                           THE "THUD" FAILURE CYCLE
┌─────────────────────────────────────────────────────────────────────────┐
│ 1. CASE Tools produce hundreds of pages of exhaustive dictionary specs  │
├─────────────────────────────────────────────────────────────────────────┤
│ 2. Document lands on desk with an "Almighty Thud"                       │
├─────────────────────────────────────────────────────────────────────────┤
│ 3. Developers are buried in detail; nobody reads or updates it          │
├─────────────────────────────────────────────────────────────────────────┤
│ 4. Code drifts from documentation -> Documentation becomes useless noise│
└─────────────────────────────────────────────────────────────────────────┘
```

### 2. The Dictionary Mentality vs. Effective Communication
Martin Fowler argues that CASE (Computer-Aided Software Engineering) tools promote a dangerous **"dictionary mentality"**:
- Developers attempt to document *every single attribute*, *every getter/setter*, and *every trivial interaction*.
- Example: Defining a `Contract` class as "a contract between many parties" and `dateSigned` as "the date the contract was signed."
- This results in zero added conceptual value while burying readers in self-evident noise.

### 3. The Art of Minimalist Auxiliary Documentation
Effective documentation is **selective communication**. Good documentation highlights only the non-obvious, high-level interactions and package dependencies, leaving algorithmic details to the source code.

Fowler's guidelines for effective auxiliary documentation:
1. **Package-Level Diagrams**: Show high-level packages and their dependencies to minimize coupling (using tools like UML class diagrams).
2. **Key Collaborations**: Document no more than a dozen key use cases with brief interaction diagrams.
3. **Brevity as a Feature**: Keep package documents brief (under a dozen pages) so they can easily be kept up-to-date as the codebase evolves.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

### Paper 5: The Almighty Thud (1997)

> **VERBATIM SOURCE**
> - **Title:** The Almighty Thud
> - **Author(s):** Martin Fowler
> - **Published:** November/December 1997, Distributed Computing
> - **Source type:** Magazine Column
> 
> *Note: The following text is reproduced verbatim — exact word-for-word.*

I was chatting with a client about an object model review they wanted to me to do. “We can send some documentation in advance, would that be useful?” they asked. I replied in the affirmative, hoping that I was not lying. Two days later the UPS man dropped the package off outside my door, it made a loud noise. It was a good inch and a half of documentation.

I opened it up and found a print-out provided from a CASE tool. It showed a few diagrams, and gave exhaustive descriptions for every class, with every attribute, and every operation. All of these had definitions. The Contract class was defined as “a contract between many parties”, its dateSigned attribute was defined as “the date the contract was signed”. I read through the inch and a half of documentation, but at the end I was little wiser. There was much on what the objects were, but little explanation of what they were meant to do. It wasn’t the first time this had happened, and I’ll be surprised if it is the last.

Why do we bother with models or documentation? They don’t execute, and our customers pay us for working code, not pretty pictures. We bother with models to communicate. The idea is that a graphical object model can show how objects fit together more clearly than looking at the source, an interaction diagram can show a collaboration better than figuring out the call paths across several class definitions. But so often the design documentation fails in this, and leaves me puzzled on my sofa.

Part of the problem is the CASE tools that people use for this kind of work. (CASE tools have two purposes, documentation and code generation, and I’m only talking about the former role here.) CASE tools encourage a dictionary mentality. You make an entry for every class, you show every class and every attribute on the diagrams, you draw an interaction diagram for every use case. They encourage completism by helping you answer the question “have we documented everything?”

That question is the wrong question. If you document everything, you are giving everything an equal weight. Do that for a complex system, and you are buried in detail. In any system there are some aspects that are more important than the others, key parts of the system that once understood, will help someone to learn more. The art in documentation is to find how to document these aspects as clearly as possible. In this you emphasize these areas, and leave the details for the code.

And all this documentation must be brief. Only if it is brief will people read it and understand it. Only if it is brief will you be able to keep it up to date. You won’t be able to talk about everything, and nor should you. A friend of mine told me about a project where they were reluctant to change class names, not because the code took too long to change, but the documentation took too long to change. When documentation becomes a problem you should deal with it. Throw at least half of it away.

#### What should you say?
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
