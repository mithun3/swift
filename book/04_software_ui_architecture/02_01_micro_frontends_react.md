<div class="page-break"></div>

## Chapter 4.2: Micro Frontends & Modular React Architecture (Cam Jackson, Martin Fowler & Addy Osmani)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. What Are Micro Frontends?
**Micro Frontends** extend the microservices architectural paradigm to the frontend layer. Instead of building a monolithic single-page application (SPA), the user interface is decomposed into independent, autonomous frontend applications owned by cross-functional teams.

```
                        MONOLITHIC SPA vs. MICRO FRONTENDS

   Monolithic SPA:
   ┌─────────────────────────────────────────────────────────────────┐
   │ Single Monolithic Frontend Application (React / Angular / Vue)  │
   └────────────────────────────────┬────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
     [Cart Service]          [Search Service]        [Checkout Service]

   Micro Frontends:
   ┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
   │ Cart Micro-Frontend   │ │ Search Micro-Frontend │ │ Checkout M-Frontend   │
   │ (Team Cart)           │ │ (Team Search)         │ │ (Team Checkout)       │
   └───────────┬───────────┘ └───────────┬───────────┘ └───────────┬───────────┘
               │                         │                         │
               ▼                         ▼                         ▼
        [Cart Service]            [Search Service]          [Checkout Service]
```

#### Core Benefits of Micro Frontends
1. **Independent Deployability**: Team Checkout can deploy a hotfix without re-building or re-deploying the Search or Cart micro-frontends.
2. **Autonomous Teams**: Each team owns its domain vertically—from database to backend APIs to UI components.
3. **Incremental Technology Upgrades**: Legacy components (e.g. AngularJS) can coexist with modern components (React 18 / Next.js) inside a unified container application.

---

#### 2. Composition Techniques for Micro Frontends
- **Server-Side Composition**: Nginx / SSI (Server-Side Includes) or Edge Workers stitching HTML fragments together at the CDN layer.
- **Build-Time Integration**: Packages published to npm and compiled together (Risk: couples deployment pipelines).
- **Run-Time Integration via Webpack Module Federation**: Micro-frontends dynamically fetch exposed remote JavaScript bundles at runtime without bundling them together at compile time.

---

#### 3. Modularizing React Applications with UI Patterns
Addy Osmani highlights how modern React applications scale cleanly by enforcing established design patterns:

1. **Container / Presentational Pattern**:
   - **Container Component**: Handles data fetching, state management, and side effects.
   - **Presentational Component**: Pure functional UI view component taking props and rendering JSX.
2. **Provider Pattern (React Context / Redux)**: Solves *Prop Drilling* by passing shared state down component trees implicitly.
3. **Compound Components Pattern**: Exposing sub-components (`<Select>`, `<Select.Option>`) that implicitly share state to create expressive APIs.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Micro Frontends<br>
  <strong>Author(s):</strong> Cam Jackson & Martin Fowler<br>
  <strong>Published:</strong> June 2019, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Article<br>
  <strong>Original URL:</strong> https://martinfowler.com/articles/micro-frontends.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

#### Micro Frontends & React Modularity
The architectural extension of microservices into the presentation tier is formally defined by Cam Jackson and Martin Fowler as "Micro Frontends." This approach directly addresses the scaling bottlenecks of monolithic Single-Page Applications (SPAs) by decentralizing the frontend into vertically aligned, independently deployable domains.

The architectural principles governing micro frontends mandate strict technological agnosticism and isolation. Cross-functional teams are empowered to upgrade or migrate their technology stacks without global coordination. To maintain this autonomy, micro frontends must eschew shared runtime state and global variables, communicating instead through standardized web APIs (such as Custom Events or URL routing). Furthermore, establishing rigid team boundaries prevents the inadvertent leakage of domain logic into shared UI component libraries, which often reintroduces monolithic coupling.

Parallel to the macro-architecture of micro frontends, Addy Osmani emphasizes structural modularity within the UI components themselves, particularly within the React ecosystem. Scaling complex interfaces requires rigorous adherence to component hierarchies and composition patterns. Techniques such as custom hooks abstract stateful logic away from presentation, while Higher-Order Components (HOCs) and render props encapsulate cross-cutting concerns. Adopting a structured taxonomy, such as Brad Frost’s Atomic Design, provides a necessary lexicon—ranging from primitive atoms to complex page templates—that unifies the engineering implementation with product design.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Reference 4.2.A: Webpack 5 Module Federation
- **Creator**: Zack Jackson (2020).
- **Mechanic**: Allows a JavaScript application to dynamically load code from another build at runtime. Remotes and hosts share common dependencies (like React) to avoid loading duplicate library copies.

#### Reference 4.2.B: Atomic Design System (Brad Frost)
- **Hierarchy**: Atoms -> Molecules -> Organisms -> Templates -> Pages.
- **Value**: Provides a shared design language between product designers (Figma) and frontend engineers (React component libraries).
