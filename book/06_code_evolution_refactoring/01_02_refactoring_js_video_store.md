<div class="page-break"></div>

# Chapter 6.3: Refactoring a JavaScript Video Store (Martin Fowler, 2016)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. The Video Store — The Canonical Refactoring Teaching Case

The video store example is the most famous case study in software engineering education. It first appeared in the opening chapter of Martin Fowler's *Refactoring: Improving the Design of Existing Code* (1999), written in Java. In 2016, Fowler revisited it in modern JavaScript to demonstrate that the same refactoring principles apply — but that JavaScript's flexibility opens up multiple valid architectural paths where Java's object-oriented idiom offered only one.

This chapter is important for two reasons:

1. **It shows refactoring in progress** — not a finished design, but the messy before-and-after of real improvement
2. **It shows that refactoring is not about a single "correct" pattern** — multiple approaches (functions, classes, data transformation) are all valid depending on team style and context

```
                THE FOUR PATHS FROM A MONOLITHIC FUNCTION

   START: One 30-line monolithic statement() function (Long Method smell)
                              │
          ┌────────────────┬──┴──────────────┬────────────────────┐
          ▼                ▼                 ▼                    ▼
   Top-Level           Nested Function    Classes            Data
   Functions           + Dispatcher      (OO style)         Transformation
   (functional)        (closure style)   (ES6 class)        (intermediate
                                                             data structure)
          │                │                 │                    │
          └────────────────┴──────────────────┴────────────────────┘
                              │
                    All 4 produce the same observable output.
                    All 4 are valid refactored states.
                    The "best" choice depends on team context.
```

---

### 2. Why Refactor the Video Store Function?

The `statement()` function is an example of the **Long Method** code smell. But Fowler is clear: *a code smell alone is not sufficient reason to refactor*. You need a concrete driver for change.

The driver here is: **add an HTML version of the statement** — a second output format rendering the same data differently. Without refactoring, the only option is to copy-paste the entire `statement()` function and modify the string templates. That creates duplication of the core pricing and renter points logic.

The refactoring eliminates this duplication by separating **calculation logic** (pricing, points) from **rendering logic** (text vs. HTML output).

---

### 3. The Four Approaches — Summary

| Approach | Style | Key Mechanism |
| :--- | :--- | :--- |
| **Top-Level Functions** | Functional / procedural | Standalone named functions; no closures; no classes |
| **Nested Function + Dispatcher** | Closure / partial application | Inner functions close over shared data; a dispatcher selects renderer |
| **ES6 Classes** | Object-oriented | `class Statement` with `text()` and `html()` methods |
| **Data Transformation** | Transformational / pipeline | Computation produces an intermediate data structure; separate renderers consume it |

Fowler's conclusion: **all four are equivalent computations**. The differences are in readability, testability, and alignment with team conventions. The data transformation approach is the most flexible for adding future output formats.

---

### 4. Code Walkthrough — Approach 1: Top-Level Functions

#### JavaScript (Original monolithic function — before refactoring)
```javascript
function statement(customer, movies) {
  let totalAmount = 0;
  let frequentRenterPoints = 0;
  let result = `Rental Record for ${customer.name}\n`;

  for (let r of customer.rentals) {
    let movie = movies[r.movieID];
    let thisAmount = 0;

    // determine amount for each movie
    switch (movie.code) {
      case "regular":
        thisAmount = 2;
        if (r.days > 2) thisAmount += (r.days - 2) * 1.5;
        break;
      case "new":
        thisAmount = r.days * 3;
        break;
      case "childrens":
        thisAmount = 1.5;
        if (r.days > 3) thisAmount += (r.days - 3) * 1.5;
        break;
    }

    // add frequent renter points
    frequentRenterPoints++;
    // add bonus for a two day new release rental
    if (movie.code === "new" && r.days > 2) frequentRenterPoints++;

    result += `\t${movie.title}\t${thisAmount}\n`;
    totalAmount += thisAmount;
  }

  result += `Amount owed is ${totalAmount}\n`;
  result += `You earned ${frequentRenterPoints} frequent renter points\n`;
  return result;
}
```

##### JavaScript (After refactoring to top-level functions)
```javascript
// Pure calculation functions — no rendering concern
function amountFor(rental, movie) {
  switch (movie.code) {
    case "regular":    return 2 + Math.max(0, (rental.days - 2) * 1.5);
    case "new":        return rental.days * 3;
    case "childrens":  return 1.5 + Math.max(0, (rental.days - 3) * 1.5);
    default: throw new Error(`Unknown movie code: ${movie.code}`);
  }
}

function frequentRenterPointsFor(rental, movie) {
  return (movie.code === "new" && rental.days > 2) ? 2 : 1;
}

function movieFor(rental, movies) {
  return movies[rental.movieID];
}

// Text rendering — separate from calculation
function textStatement(customer, movies) {
  const rentals = customer.rentals.map(r => ({ r, movie: movieFor(r, movies) }));
  const totalAmount = rentals.reduce((sum, { r, movie }) => sum + amountFor(r, movie), 0);
  const points = rentals.reduce((sum, { r, movie }) => sum + frequentRenterPointsFor(r, movie), 0);
  const lines = rentals.map(({ r, movie }) => `\t${movie.title}\t${amountFor(r, movie)}`).join("\n");
  return `Rental Record for ${customer.name}\n${lines}\nAmount owed is ${totalAmount}\nYou earned ${points} frequent renter points\n`;
}

// HTML rendering — reuses same calculation functions
function htmlStatement(customer, movies) {
  const rentals = customer.rentals.map(r => ({ r, movie: movieFor(r, movies) }));
  const totalAmount = rentals.reduce((sum, { r, movie }) => sum + amountFor(r, movie), 0);
  const points = rentals.reduce((sum, { r, movie }) => sum + frequentRenterPointsFor(r, movie), 0);
  const rows = rentals.map(({ r, movie }) =>
    `<tr><td>${movie.title}</td><td>${amountFor(r, movie)}</td></tr>`).join("\n");
  return `<h1>Rental Record for <em>${customer.name}</em></h1>
<table>${rows}</table>
<p>Amount owed is <em>${totalAmount}</em></p>
<p>You earned <em>${points}</em> frequent renter points</p>`;
}
```

##### Java Equivalent (Classes approach)
```java
public class StatementRenderer {

    private final Map<String, Movie> movies;

    public StatementRenderer(Map<String, Movie> movies) {
        this.movies = movies;
    }

    public String text(Customer customer) {
        return renderHeader(customer) + renderRentalLines(customer, false) + renderFooter(customer);
    }

    public String html(Customer customer) {
        return renderHeader(customer) + renderRentalLines(customer, true) + renderFooter(customer);
    }

    private double amountFor(Rental rental) {
        Movie movie = movies.get(rental.getMovieId());
        return switch (movie.getCode()) {
            case "regular"  -> 2 + Math.max(0, (rental.getDays() - 2) * 1.5);
            case "new"      -> rental.getDays() * 3.0;
            case "childrens"-> 1.5 + Math.max(0, (rental.getDays() - 3) * 1.5);
            default -> throw new IllegalStateException("Unknown movie code: " + movie.getCode());
        };
    }

    private int frequentRenterPointsFor(Rental rental) {
        Movie movie = movies.get(rental.getMovieId());
        return ("new".equals(movie.getCode()) && rental.getDays() > 2) ? 2 : 1;
    }

    private String renderRentalLines(Customer customer, boolean html) {
        return customer.getRentals().stream()
            .map(r -> html
                ? String.format("<tr><td>%s</td><td>%.1f</td></tr>",
                    movies.get(r.getMovieId()).getTitle(), amountFor(r))
                : String.format("\t%s\t%.1f\n",
                    movies.get(r.getMovieId()).getTitle(), amountFor(r)))
            .collect(Collectors.joining());
    }

    private String renderHeader(Customer customer) { return "Rental Record for " + customer.getName() + "\n"; }
    private String renderFooter(Customer customer) {
        double total = customer.getRentals().stream().mapToDouble(this::amountFor).sum();
        int points = customer.getRentals().stream().mapToInt(this::frequentRenterPointsFor).sum();
        return String.format("Amount owed is %.1f\nYou earned %d frequent renter points\n", total, points);
    }
}
```

##### Python Equivalent
```python
from dataclasses import dataclass
from typing import Dict, List

@dataclass
class Movie:
    title: str
    code: str  # "regular", "new", "childrens"

@dataclass
class Rental:
    movie_id: str
    days: int

@dataclass
class Customer:
    name: str
    rentals: List[Rental]

def amount_for(rental: Rental, movie: Movie) -> float:
    if movie.code == "regular":
        return 2 + max(0, (rental.days - 2) * 1.5)
    elif movie.code == "new":
        return rental.days * 3.0
    elif movie.code == "childrens":
        return 1.5 + max(0, (rental.days - 3) * 1.5)
    raise ValueError(f"Unknown movie code: {movie.code}")

def frequent_renter_points_for(rental: Rental, movie: Movie) -> int:
    return 2 if movie.code == "new" and rental.days > 2 else 1

def text_statement(customer: Customer, movies: Dict[str, Movie]) -> str:
    rentals_with_movies = [(r, movies[r.movie_id]) for r in customer.rentals]
    total = sum(amount_for(r, m) for r, m in rentals_with_movies)
    points = sum(frequent_renter_points_for(r, m) for r, m in rentals_with_movies)
    lines = "\n".join(f"\t{m.title}\t{amount_for(r, m)}" for r, m in rentals_with_movies)
    return (f"Rental Record for {customer.name}\n{lines}\n"
            f"Amount owed is {total}\nYou earned {points} frequent renter points\n")

def html_statement(customer: Customer, movies: Dict[str, Movie]) -> str:
    rentals_with_movies = [(r, movies[r.movie_id]) for r in customer.rentals]
    total = sum(amount_for(r, m) for r, m in rentals_with_movies)
    points = sum(frequent_renter_points_for(r, m) for r, m in rentals_with_movies)
    rows = "\n".join(f"<tr><td>{m.title}</td><td>{amount_for(r, m)}</td></tr>"
                    for r, m in rentals_with_movies)
    return (f"<h1>Rental Record for <em>{customer.name}</em></h1>\n"
            f"<table>{rows}</table>\n"
            f"<p>Amount owed is <em>{total}</em></p>\n"
            f"<p>You earned <em>{points}</em> frequent renter points</p>")
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

### 1. Practical Application of Refactoring
The Video Store example serves as the canonical demonstration of Fowler's refactoring principles applied to a tangible codebase. It illustrates how monolithic, procedural code can be systematically dismantled and reconstructed into a cohesive, object-oriented design without altering external behavior.

### 2. Decomposing Monolithic Functions
The primary focus of this exercise is the decomposition of large, complex functions into smaller, intention-revealing methods. By applying the "Extract Method" pattern, the logic becomes modular, making it easier to isolate bugs and introduce new pricing or rental rules.

### 3. Polymorphism and Design Patterns
As the refactoring progresses, the example demonstrates the transition from complex conditional logic (e.g., switch statements) to polymorphic structures. This application of the State or Strategy pattern inherently makes the codebase more resilient to future changes in business requirements.

---
