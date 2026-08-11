<div class="page-break"></div>

# Chapter 5.1: Refactoring Fundamentals & Preparatory Refactoring (Martin Fowler)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. What Is Refactoring?

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

### 2. Preparatory Refactoring — "Make the Change Easy"

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

### 3. The Two Hats (Kent Beck)

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

### 4. Core Refactoring Catalogue (Fowler's Named Transformations)

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

### 5. Code Smells — When to Refactor

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

### 6. Code Examples — Extract Method Pattern

#### Java Implementation
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

### 7. The Notification Pattern — Replacing Exceptions for Validation

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

> **VERBATIM SOURCE**
> - **Title:** Refactoring Fundamentals & Preparatory Refactoring
> - **Author(s):** Martin Fowler
> - **Published:** 2014-2016, martinfowler.com
> - **Source type:** Architecture Essays & Articles
> - **Original URL:** https://martinfowler.com/articles/preparatory-refactoring-example.html
> 
> *Note: The following text presents the core architectural text and research synthesis for educational study.*

### 1. The Core Philosophy of Refactoring
Fowler's foundational work on refactoring establishes it not merely as a technical chore, but as an essential practice for software sustainability. Refactoring is defined as restructuring code without changing its observable behavior. The primary goal is to improve the internal structure, making the codebase easier to understand and cheaper to modify.

### 2. The Two Hats and Preparatory Refactoring
A critical conceptual model introduced is the "Two Hats" metaphor. Developers must separate the acts of adding functionality and refactoring. By focusing on one activity at a time, cognitive load is reduced and test stability is maintained. Preparatory refactoring ("making the change easy") emphasizes that before a new feature is added, the existing structure should be adapted to seamlessly accommodate it.

### 3. Transformational Mechanics
The mechanics of refactoring rely on a catalogue of precise, behavior-preserving transformations. Each step must be small enough that the test suite continues to pass, ensuring the system remains continuously deployable.

---
