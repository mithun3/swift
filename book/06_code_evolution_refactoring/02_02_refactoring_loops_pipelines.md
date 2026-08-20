<div class="page-break"></div>

# Chapter 6.5: Refactoring with Loops and Collection Pipelines (Martin Fowler, 2015)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. The Problem with Imperative Loops

Traditional `for` and `while` loops are powerful but **opaque**. They mix the *iteration mechanism* (how you traverse a collection) with the *transformation logic* (what you do at each step) and the *accumulation logic* (how you collect results). This mixing makes loops harder to read at a glance, harder to compose, and harder to test in isolation.

```
             IMPERATIVE LOOP — THREE CONCERNS MIXED TOGETHER

   for (let rental of customer.rentals) {        ← iteration mechanism
       let movie = movies[rental.movieID];        ← lookup / mapping
       if (movie.code === "regular") {            ← filtering / condition
           totalAmount += amountFor(rental);       ← accumulation
       }
   }

   All three concerns (iteration, mapping, filtering, accumulation) are
   woven together. You cannot test them independently.
```

Collection Pipelines separate these concerns into **named, composable operations**:

```
             COLLECTION PIPELINE — CONCERNS SEPARATED

   customer.rentals
       .map(r => ({ rental: r, movie: movies[r.movieID] }))  ← mapping
       .filter(({ movie }) => movie.code === "regular")       ← filtering
       .reduce((sum, { rental }) => sum + amountFor(rental), 0) ← accumulation

   Each step is pure, independently understandable, and composable.
```

---

### 2. The Core Pipeline Operations

| Operation | Purpose | Input → Output |
| :--- | :--- | :--- |
| `map` / `stream().map()` | Transform each element | Collection<A> → Collection<B> |
| `filter` / `stream().filter()` | Keep elements matching a predicate | Collection<A> → Collection<A subset> |
| `reduce` / `stream().reduce()` | Collapse a collection to a single value | Collection<A> → B |
| `flatMap` / `stream().flatMap()` | Transform each element to a collection, then flatten | Collection<Collection<A>> → Collection<A> |
| `sorted` / `sorted(comparator)` | Sort elements | Collection<A> → Collection<A ordered> |
| `distinct` | Remove duplicates | Collection<A> → Collection<A unique> |
| `forEach` | Side-effecting terminal (avoid in pipelines) | Collection<A> → void |

---

### 3. Code Examples — Replace Loop with Pipeline

#### Java Implementation (Java 8+ Streams)
```java
// BEFORE: Imperative loop
public List<String> getRegularMovieTitles(Customer customer, Map<String, Movie> movies) {
    List<String> titles = new ArrayList<>();
    for (Rental rental : customer.getRentals()) {
        Movie movie = movies.get(rental.getMovieId());
        if ("regular".equals(movie.getCode())) {
            titles.add(movie.getTitle());
        }
    }
    return titles;
}

// AFTER: Collection pipeline with Java Streams
public List<String> getRegularMovieTitles(Customer customer, Map<String, Movie> movies) {
    return customer.getRentals().stream()
        .map(r -> movies.get(r.getMovieId()))
        .filter(movie -> "regular".equals(movie.getCode()))
        .map(Movie::getTitle)
        .collect(Collectors.toList());
}

// More complex example: total amount for regular movies only
public double totalForRegularMovies(Customer customer, Map<String, Movie> movies) {
    return customer.getRentals().stream()
        .filter(r -> "regular".equals(movies.get(r.getMovieId()).getCode()))
        .mapToDouble(r -> amountFor(r, movies.get(r.getMovieId())))
        .sum();
}
```

##### JavaScript / TypeScript Implementation
```javascript
// BEFORE: Imperative loop — Twitter follower enrichment example
function getTopTweeters(tweets) {
  const result = [];
  for (let tweet of tweets) {
    if (tweet.retweetCount > 100) {
      result.push({
        author: tweet.author.toUpperCase(),
        count: tweet.retweetCount
      });
    }
  }
  result.sort((a, b) => b.count - a.count);
  return result.slice(0, 5);
}

// AFTER: Collection pipeline
function getTopTweeters(tweets) {
  return tweets
    .filter(tweet => tweet.retweetCount > 100)
    .map(tweet => ({ author: tweet.author.toUpperCase(), count: tweet.retweetCount }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);
}
```

##### Python Implementation
```python
# BEFORE: Imperative loop
def get_regular_movie_amounts(customer, movies):
    amounts = []
    for rental in customer['rentals']:
        movie = movies[rental['movie_id']]
        if movie['code'] == 'regular':
            amounts.append(amount_for(rental, movie))
    return amounts

# AFTER: List comprehension (Python idiomatic pipeline)
def get_regular_movie_amounts(customer, movies):
    return [
        amount_for(rental, movies[rental['movie_id']])
        for rental in customer['rentals']
        if movies[rental['movie_id']]['code'] == 'regular'
    ]

# With generator for memory efficiency on large datasets:
def get_regular_movie_amounts_gen(customer, movies):
    return (
        amount_for(rental, movies[rental['movie_id']])
        for rental in customer['rentals']
        if movies[rental['movie_id']]['code'] == 'regular'
    )

# Using functools for reduce:
from functools import reduce
def total_amount(customer, movies):
    return reduce(
        lambda acc, r: acc + amount_for(r, movies[r['movie_id']]),
        customer['rentals'],
        0
    )
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

### 1. The Paradigm Shift from Imperative to Declarative
Refactoring loops into collection pipelines represents a fundamental shift from imperative state manipulation to declarative data processing. This approach leverages functional programming concepts (map, filter, reduce) to express the *intent* of an operation rather than the mechanics of its execution.

### 2. Enhancing Readability and Comprehension
Traditional loop structures often obscure the core business logic beneath boilerplate iteration and state management. Pipelines streamline this by chaining pure functions, producing code that reads closer to natural language and is inherently easier to comprehend at a glance.

### 3. Immutability and Side-Effect Reduction
By transitioning to pipelines, developers naturally adopt immutability and reduce side-effects. Operations within a pipeline typically return new collections rather than mutating existing ones, leading to safer, more predictable code that is easier to parallelize and test.

---
