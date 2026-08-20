<div class="page-break"></div>

# Chapter 1.6: On the Criteria To Be Used in Decomposing Systems into Modules (David Parnas, 1972)

## SECTION 1 — PRIMER ON THE BASICS

Before reading David Parnas's seminal 1972 paper on modularity, it is important to understand the context of software design at the time and the problem he was trying to solve. Parnas introduced concepts that are now foundational to software engineering, specifically "information hiding."

### The Problem: How Do We Divide a System?

When building a large software system, it must be divided into smaller, manageable pieces called **modules**. But what criteria should we use to decide where the boundaries of these modules lie?

Before Parnas, the conventional wisdom was to decompose systems based on a flowchart of steps—essentially, a sequential or functional decomposition. You would look at the steps the system takes to process data and create a module for each step. 

### Parnas's Radical Idea: Information Hiding

Parnas argued against flowchart-based decomposition. Instead, he proposed that a module should be designed to **hide a difficult design decision or a design decision that is likely to change**. This concept is known as **Information Hiding**.

If a module hides a secret (like the specific data structure used, or the specifics of a hardware interface), then the rest of the system does not need to know about that secret. When the secret inevitably changes, only the module that hides it needs to be modified.

### Real-World Examples & Modern Implementations

### 1. Java

**The Problem (Exposing Data Structures):**
```java
// Java - Exposing internal representation
public class EmployeeDatabase {
    // Bad: The internal array is public. If we want to change to a HashMap later,
    // we break all code that uses this array.
    public Employee[] employees = new Employee[100];
}
```

**The Modern Solution (Information Hiding):**
```java
// Java - Hiding internal representation behind an interface
public class EmployeeDatabase {
    private Map<Integer, Employee> employees = new HashMap<>();

    public void addEmployee(Employee emp) {
        employees.put(emp.getId(), emp);
    }

    public Employee getEmployee(int id) {
        return employees.get(id);
    }
}
```

### 2. TypeScript / JavaScript

**The Problem:**
```typescript
// TypeScript - Exposing how a specific API works
class PaymentProcessor {
    processStripePayment(amount: number, token: string) {
        // hardcoded Stripe logic
    }
}
```

**The Modern Solution:**
```typescript
// TypeScript - Hiding the payment provider behind a generic module interface
interface PaymentModule {
    processPayment(amount: number): boolean;
}

class StripePaymentModule implements PaymentModule {
    processPayment(amount: number): boolean {
        // secret Stripe implementation details hidden here
        return true;
    }
}
```

### 3. Python

**The Problem:**
```python
# Python - Direct database access spread everywhere
def handle_user_request():
    db = sqlite3.connect("users.db")
    # SQL logic scattered, hard to change database engines
```

**The Modern Solution:**
```python
# Python - Hiding the database behind a Repository module
class UserRepository:
    def get_user(self, user_id: int):
        # The fact that we use sqlite3 is a secret hidden by this module
        pass

def handle_user_request(repo: UserRepository):
    user = repo.get_user(1)
```

## SECTION 2 — VERBATIM TEXT

> **VERBATIM SOURCE**  
> On the Criteria To Be Used in Decomposing Systems into Modules, David L. Parnas, Communications of the ACM, December 1972  
> *This text is represented here as a placeholder for educational study.*

(In a full realization of this book, the verbatim text of the 1972 Communications of the ACM paper by David L. Parnas would be inserted here, allowing readers to study his exact phrasing on information hiding and modular decomposition.)

## SECTION 3 — CITATION & REFERENCE DEEP-DIVES

### Information Hiding vs. Encapsulation

While often used interchangeably, information hiding and encapsulation are technically distinct. Encapsulation is a language mechanism (like `private` keywords or classes) that bundles data and methods together. Information hiding is a design principle—a way of thinking about what a module should conceal from other modules. You can have encapsulation without information hiding if you expose all your internal state via public getters and setters. Parnas's work predates object-oriented programming's mainstream adoption and focuses purely on the design principle.
