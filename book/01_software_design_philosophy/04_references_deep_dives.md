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
