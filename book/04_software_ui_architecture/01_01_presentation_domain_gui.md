<div class="page-break"></div>

# Module 4: Software & UI Architecture Patterns


## Chapter 4.1: Presentation Domain Separation & GUI Architectures (Martin Fowler)

---

### SECTION 1: PRIMER ON THE BASICS

### 1. What Is Presentation Domain Separation (PDS)?
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

### 2. Evolution of GUI Architectures: MVC, MVP, and MVVM

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

### 3. Real-World Code Example: PDS in Modern TypeScript

#### Violating PDS (Anti-Pattern: Mixing UI and Business Rules)
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
  <strong>VERBATIM SOURCE</strong><br>
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
