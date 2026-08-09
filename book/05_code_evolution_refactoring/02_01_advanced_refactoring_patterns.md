
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
