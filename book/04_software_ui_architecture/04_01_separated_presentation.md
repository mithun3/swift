
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
  <strong>VERBATIM SOURCE</strong><br>
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
