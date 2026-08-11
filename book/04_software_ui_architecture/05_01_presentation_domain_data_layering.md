<div class="page-break"></div>

# Chapter 4.5: Presentation Domain Data Layering (Martin Fowler)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. Introduction
The three-layer architecture (Presentation, Domain, and Data) is perhaps the most ubiquitous pattern in software engineering. By dividing an information-rich program into a UI layer (handling HTTP or GUI), a domain logic layer (validations and business rules), and a data access layer (database persistence), developers can organize code in a way that maps cleanly to logical areas of concern. 

### 2. Key Concepts: Cognitive Scope Narrowing
While substitutability (swapping out the database) and testability (testing the domain without the UI) are often cited as the primary reasons for layering, Martin Fowler highlights an even more practical benefit: **scope narrowing**. 
Layering allows developers to reduce their cognitive load by focusing on one specific problem at a time. When working in the domain layer, you don't need to worry about how the data is rendered on the screen or how it is mapped to a SQL schema. It is a structural enabler for the concept of "Two Hats" from refactoring.

### 3. Real-World Examples & Diagram
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

### 4. Code Examples (Java / JS / Python)

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

## SECTION 2: VERBATIM & RESEARCH TEXTS

> **VERBATIM SOURCE**
> - **Title:** Presentation Domain Data Layering
> - **Author(s):** Martin Fowler
> - **Published:** 2015, martinfowler.com
> - **Source type:** Architecture Essay
> - **Original URL:** https://martinfowler.com/bliki/PresentationDomainDataLayering.html
> 
> *Note: The following text presents the core architectural text and research synthesis for educational study.*

### Presentation Domain Data Layering and Cognitive Scope
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
