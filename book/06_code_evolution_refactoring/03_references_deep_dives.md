<div class="page-break"></div>

# Chapter 6.8: Citation & Reference Deep-Dives — Module 6

This chapter provides standalone research profiles, detailed mechanics, and architectural context for all major citations across Module 6.

---

## Deep-Dive 6.8.1: Complete Profile of Martin Fowler's Refactoring Works

### 1. *Refactoring: Improving the Design of Existing Code* (1st Edition, 1999)
- **Primary Focus**: Formally introducing the practice of refactoring to the mainstream software industry using Java.
- **Key Concepts**: Defined the concept of "Code Smells" (co-authored with Kent Beck) and documented over 70 specific refactoring patterns with step-by-step mechanics and test-driven validations.
- **Legacy**: Established the cataloging format for refactorings (Name, Summary, Motivation, Mechanics, Examples) which remains the industry standard.

### 2. *Refactoring: Improving the Design of Existing Code* (2nd Edition, 2019)
- **Primary Focus**: Updating the canonical examples to JavaScript to reflect the rise of classless, functional, and web-centric programming paradigms.
- **Key Updates**: Added new patterns like *Replace Loop with Pipeline*, *Slide Statements*, and *Split Loop*, while removing obsolete Java-specific structural patterns.

---

## Deep-Dive 6.8.2: Detailed Mechanics of Key Refactoring Patterns

### 1. Extract Method / Function
- **Motivation**: A long function or method is hard to read, test, and reuse. By grouping cohesive lines of code and extracting them into a named method, the caller becomes self-documenting.
- **Mechanics**:
  1. Create a new function and name it after its intent (what it does, not how it does it).
  2. Copy the extracted code into the new function.
  3. Scan the extracted code for variables that are local in scope to the source function. Pass them as parameters.
  4. If any local variables are modified by the extracted code, return the modified value.
  5. Replace the extracted code in the source function with a call to the new function.
  6. Compile and test.

### 2. Replace Temp with Query
- **Motivation**: Temporary variables store the result of an expression and are only visible within the function. They force caller functions to be longer and more coupled. Replacing them with query methods allows other methods in the class to access the values.
- **Mechanics**:
  1. Identify a temporary variable that is assigned once.
  2. Extract the right-hand side of the assignment into a query method.
  3. Replace all references to the temp variable with call expressions to the query method.
  4. Delete the temp variable declaration and assignment.
  5. Compile and test.

---

## Deep-Dive 6.8.3: SOLID Principles in Refactoring Context

The SOLID principles guide the target structure of refactoring efforts:

- **Single Responsibility Principle (SRP)**: A class should have one, and only one, reason to change. The *Extract Class* pattern is the primary tool for resolving SRP violations when a class grows too large.
- **Open/Closed Principle (OCP)**: Software entities should be open for extension but closed for modification. Moving from imperative conditional blocks to polymorphic subclasses (e.g. replacing movie type codes with strategies) implements OCP.
- **Liskov Substitution Principle (LSP)**: Subtypes must be substitutable for their base types. Refactoring subclass hierarchies to avoid inheritance abuse (e.g., using delegation instead of refused bequest) preserves LSP.
- **Interface Segregation Principle (ISP)**: Clients should not be forced to depend on methods they do not use. Splitting bloated interfaces into smaller, role-specific client interfaces.
- **Dependency Inversion Principle (DIP)**: High-level modules should not depend on low-level modules; both should depend on abstractions. Refactoring package dependencies by introducing interfaces and injecting them resolves tight coupling.

---

## Deep-Dive 6.8.4: Complete IEEE Bibliography for Module 6

[28] M. Fowler, "An Example of Preparatory Refactoring," MartinFowler.com, 2014. Available: https://martinfowler.com/articles/preparatory-refactoring-example.html
[29] M. Fowler, "Refactoring Code to Load a Document," MartinFowler.com, 2016.
[30] M. Fowler, "Refactoring: This Class is Too Large," MartinFowler.com, 2015.
[31] M. Fowler, "Replacing Exceptions with Notification," MartinFowler.com, 2014.
[32] M. Fowler, "Refactoring Module Dependencies," MartinFowler.com, 2018.
[33] M. Fowler, "Refactoring a JavaScript Video Store," MartinFowler.com, 2016. Available: https://martinfowler.com/articles/refactoring-video-store.html
[34] M. Fowler, "Refactoring with Loops and Collection Pipelines," MartinFowler.com, 2015. Available: https://martinfowler.com/articles/refactoring-pipelines.html
[35] M. Fowler, "Refactoring to an Adaptive Model," MartinFowler.com, 2020. Available: https://martinfowler.com/articles/refactoring-adaptive-model.html
[36] M. Fowler, "Refactoring Code that Accesses External Services," MartinFowler.com, 2019. Available: https://martinfowler.com/articles/refactoring-external-service.html

**Supplementary Books**
[S11] M. Fowler, *Refactoring: Improving the Design of Existing Code*, 1st ed. Boston, MA: Addison-Wesley, 1999.
[S12] M. Fowler, *Refactoring: Improving the Design of Existing Code*, 2nd ed. Boston, MA: Addison-Wesley, 2019.
[S13] K. Beck, *Smalltalk Best Practice Patterns*, Upper Saddle River, NJ: Prentice Hall, 1997.
[S14] M. C. Feathers, *Working Effectively with Legacy Code*, Upper Saddle River, NJ: Prentice Hall, 2004.

**Subject Index Cross-References:**
- Collection Pipelines .......... Ch 6.5
- Dependency Inversion .......... Ch 6.4, Ch 6.7
- Gateway Pattern ............... Ch 6.7
- Notification Pattern .......... Ch 6.2, Ch 6.4
- Preparatory Refactoring ....... Ch 6.2
- Refactoring ................... Ch 6.2, Ch 6.3, Ch 6.4, Ch 6.5, Ch 6.6, Ch 6.7
- TDD & Testing Strategies ...... Ch 6.1
