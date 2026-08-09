<div class="page-break"></div>

## Chapter 4.6: Citation & Reference Deep-Dives for Module 4

This chapter provides standalone research profiles, architectural pattern taxonomy, and engineering strategies for all major citations across Module 4.

---

### Deep-Dive 4.6.1: The Model-View-Controller (MVC) Architectural Lineage

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

### Deep-Dive 4.6.2: Micro-Frontend Runtime Integration Mechanics

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

### Deep-Dive 4.6.3: Summary of Cited Works for Module 4

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
