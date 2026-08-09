<div class="page-break"></div>

# Chapter 4.2: Micro Frontends & Modular React Architecture (Cam Jackson, Martin Fowler & Addy Osmani)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. What Are Micro Frontends?
**Micro Frontends** extend the microservices architectural paradigm to the frontend presentation layer. Instead of building a monolithic Single-Page Application (SPA) where all UI components, state stores, and page routes reside in a single codebase and deployment artifact, the user interface is decomposed into independent, autonomous frontend applications owned by cross-functional, domain-driven teams.

```text
                        MONOLITHIC SPA vs. MICRO FRONTENDS

   Monolithic SPA:
   ┌─────────────────────────────────────────────────────────────────┐
   │ Single Monolithic Frontend Application (React / Angular / Vue)  │
   └────────────────────────────────┬────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
     [Cart Service]          [Search Service]        [Checkout Service]

   Micro Frontends Architecture:
   ┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
   │ Cart Micro-Frontend   │ │ Search Micro-Frontend │ │ Checkout M-Frontend   │
   │ (Team Cart - React)   │ │ (Team Search - Vue)   │ │ (Team Checkout - Next)│
   └───────────┬───────────┘ └───────────┬───────────┘ └───────────┬───────────┘
               │                         │                         │
               ▼                         ▼                         ▼
        [Cart Service]            [Search Service]          [Checkout Service]
```

### Core Architectural Principles & Benefits
1. **Independent Deployability**: Team Checkout can build, test, and deploy a bug fix or new feature to production without re-building, re-testing, or re-deploying the Search or Cart micro-frontends.
2. **Autonomous Domain Teams**: Each cross-functional team owns its domain slice vertically—from database schema and backend APIs up to UI components and client-side page routes.
3. **Incremental Technology Migration**: Legacy codebases (e.g., AngularJS or jQuery) can coexist alongside modern frameworks (React 18 / Next.js) inside a unified container application shell, enabling step-by-step modernization without risky rewrite projects.
4. **Resilient Failure Isolation**: A JavaScript runtime error in a non-critical micro-frontend (such as product recommendations) can be caught by local React `ErrorBoundary` wrappers without crashing the entire page or checkout flow.

---

### 2. Composition Techniques & Webpack 5 Module Federation

Frontend micro-component composition can be implemented across three distinct architectural tiers:

| Composition Strategy | Integration Mechanism | Key Advantages | Trade-offs / Risks |
| :--- | :--- | :--- | :--- |
| **Server-Side Composition** | Nginx / CDN Edge Workers stitching HTML fragments via Server-Side Includes (SSI). | Blazing fast initial page load (FCP); SEO friendly. | Higher latency on personalized dynamic content; complex server state. |
| **Build-Time Integration** | NPM packages published to a private registry and linked as dependencies. | Type-safe interfaces; easy local developer setup. | Locks deployment pipelines together; re-bundling required on every update. |
| **Run-Time Integration (Module Federation)** | Dynamic runtime loading of remote JS bundles via Webpack 5 / Vite Federation. | Instant independent deployments; runtime dependency sharing. | Requires resilient dynamic imports, fallback UI handling, and network routing. |

#### Webpack 5 Module Federation Implementation Example

Module Federation allows a **Host Application (Container)** to dynamically fetch compiled remote components over the network at runtime while sharing common vendor dependencies (such as `react` and `react-dom`) to prevent duplicate bundle payload downloads.

##### Host Container `webpack.config.js`:
```javascript
const HtmlWebpackPlugin = require("html-webpack-plugin");
const ModuleFederationPlugin = require("webpack/lib/container/ModuleFederationPlugin");
const deps = require("./package.json").dependencies;

module.exports = {
  mode: "production",
  output: { publicPath: "auto" },
  plugins: [
    new ModuleFederationPlugin({
      name: "host_shell",
      remotes: {
        checkoutApp: "checkoutApp@https://cdn.example.com/checkout/remoteEntry.js",
        searchApp: "searchApp@https://cdn.example.com/search/remoteEntry.js",
      },
      shared: {
        react: { singleton: true, requiredVersion: deps.react },
        "react-dom": { singleton: true, requiredVersion: deps["react-dom"] },
      },
    }),
    new HtmlWebpackPlugin({ template: "./public/index.html" }),
  ],
};
```

##### React Host Integration with Lazy Loading & Error Isolation:
```tsx
import React, { Suspense, Component, ErrorInfo, ReactNode } from "react";

// Dynamically import remote micro-frontend component exposed by Remote Entry
const RemoteCheckoutHeader = React.lazy(() => import("checkoutApp/HeaderComponent"));

interface ErrorBoundaryState {
  hasError: boolean;
}

// Resilient Error Boundary to isolate remote MFE failures
class MfeErrorBoundary extends Component<{ children: ReactNode; fallback: ReactNode }, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(_: Error): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Micro-Frontend loading failure:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback;
    }
    return this.props.children;
  }
}

export const ApplicationShell: React.FC = () => {
  return (
    <div className="app-shell">
      <header className="main-nav">
        <h1>Global E-Commerce Platform</h1>
      </header>

      <main className="content-container">
        <MfeErrorBoundary fallback={<div className="mfe-fallback">Checkout navigation temporarily unavailable.</div>}>
          <Suspense fallback={<div className="mfe-loading">Loading Checkout Module...</div>}>
            <RemoteCheckoutHeader cartItemCount={4} />
          </Suspense>
        </MfeErrorBoundary>
      </main>
    </div>
  );
};
```

---

### 3. Inter-Process Communication (IPC) & State Isolation

A fundamental design anti-pattern in micro-frontend architectures is creating shared global state (such as a single giant Redux store shared across MFEs). Sharing mutable state tightly couples teams, re-creating a distributed monolith.

Instead, micro-frontends should communicate using **loosely coupled, event-driven communication**:

1. **Custom Browser DOM Events (`CustomEvent`)**: Micro-frontends publish and subscribe to standardized events on `window`.
2. **URL Route State & Query Parameters**: Router parameters serve as the primary source of truth for navigation state (`/checkout?cartId=8841`).

```typescript
// Inter-MFE Event Bus Wrapper
export class MicroFrontendEventBus {
  static dispatch<T>(eventName: string, payload: T): void {
    const event = new CustomEvent(eventName, {
      detail: payload,
      bubbles: true,
      composed: true, // Allows crossing Shadow DOM boundaries
    });
    window.dispatchEvent(event);
  }

  static subscribe<T>(eventName: string, handler: (payload: T) => void): () => void {
    const listener = (event: Event) => {
      const customEvent = event as CustomEvent<T>;
      handler(customEvent.detail);
    };
    window.addEventListener(eventName, listener);
    return () => window.removeEventListener(eventName, listener);
  }
}

// Usage Example in Cart Micro-Frontend:
// MicroFrontendEventBus.dispatch("CART_ITEM_ADDED", { sku: "9921", price: 49.99 });
```

---

### 4. Modularizing React Applications with Modern Design Patterns

Addy Osmani highlights how React applications within individual micro-frontends scale cleanly by enforcing modular component design patterns:

1. **Container / Presentational Pattern**:
   - **Container Component**: Manages network data fetching (`useQuery`), business rules, and state mutation.
   - **Presentational Component**: Pure functional UI view taking immutable props and rendering DOM markup.
2. **Provider Pattern (React Context)**: Solves *Prop Drilling* across deep component trees without leaking internal component state to neighboring micro-frontends.
3. **Compound Components Pattern**: Exposes expressive, cohesive sub-components (`<Modal>`, `<Modal.Header>`, `<Modal.Body>`) that implicitly coordinate internal state.
4. **Atomic Design Taxonomy (Brad Frost)**:
   - **Atoms**: Primitive building blocks (`Button`, `Input`, `Label`).
   - **Molecules**: Groups of atoms working together (`SearchInput` = `Input` + `Button`).
   - **Organisms**: Complex UI sections (`Header`, `ProductCardGrid`).
   - **Templates / Pages**: Layout structures combining organisms into complete view routes.

---

## SECTION 2: VERBATIM & RESEARCH TEXTS

<div class="source-attribution">
  <strong>VERBATIM SOURCE</strong><br>
  <strong>Title:</strong> Micro Frontends<br>
  <strong>Author(s):</strong> Cam Jackson & Martin Fowler<br>
  <strong>Published:</strong> June 2019, martinfowler.com<br>
  <strong>Source type:</strong> Architecture Article<br>
  <strong>Original URL:</strong> https://martinfowler.com/articles/micro-frontends.html<br>
  <strong>Note:</strong> The following text presents the core architectural text and research synthesis for educational study.
</div>

### Micro Frontends & React Modularity
The architectural extension of microservices into the presentation tier is formally defined by Cam Jackson and Martin Fowler as "Micro Frontends." This approach directly addresses the scaling bottlenecks of monolithic Single-Page Applications (SPAs) by decentralizing the frontend into vertically aligned, independently deployable domains.

The architectural principles governing micro frontends mandate strict technological agnosticism and isolation. Cross-functional teams are empowered to upgrade or migrate their technology stacks without global coordination. To maintain this autonomy, micro frontends must eschew shared runtime state and global variables, communicating instead through standardized web APIs (such as Custom Events or URL routing). Furthermore, establishing rigid team boundaries prevents the inadvertent leakage of domain logic into shared UI component libraries, which often reintroduces monolithic coupling.

Parallel to the macro-architecture of micro frontends, Addy Osmani emphasizes structural modularity within the UI components themselves, particularly within the React ecosystem. Scaling complex interfaces requires rigorous adherence to component hierarchies and composition patterns. Techniques such as custom hooks abstract stateful logic away from presentation, while Higher-Order Components (HOCs) and render props encapsulate cross-cutting concerns. Adopting a structured taxonomy, such as Brad Frost’s Atomic Design, provides a necessary lexicon—ranging from primitive atoms to complex page templates—that unifies the engineering implementation with product design.

---

## SECTION 3: CITATION & REFERENCE DEEP-DIVES

### Reference 4.2.A: Webpack 5 Module Federation Architecture
- **Creator**: Zack Jackson (2020).
- **Mechanic**: Allows a JavaScript application to dynamically load remote code bundles over HTTP at runtime without build-time linking. Shared libraries (like React or UI design systems) are declared in the federation manifest, ensuring only a single runtime instance is executed.

### Reference 4.2.B: Atomic Design System Taxonomy (Brad Frost)
- **Hierarchy**: Atoms $\rightarrow$ Molecules $\rightarrow$ Organisms $\rightarrow$ Templates $\rightarrow$ Pages.
- **Value**: Establishes a common structural design language between Figma design systems and production React component libraries, preventing UI drift across micro-frontend teams.
