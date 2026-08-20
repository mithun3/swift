<div class="page-break"></div>

# Chapter 6.5: Refactoring to an Adaptive Model (Martin Fowler)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. What Is an Adaptive Model?

An **Adaptive Model** (also known as a **Data-Driven Model**) is a computational design where business rules are encoded as data — typically JSON, XML, or a domain-specific language (DSL) — rather than as imperative code. A separate interpreter or rule engine reads that data and executes it.

```
             IMPERATIVE CODE vs. ADAPTIVE MODEL

   IMPERATIVE APPROACH (rules baked into code):
   ┌────────────────────────────────────────────┐
   │ if (spec.atNight) result.push("whisper")   │
   │ if (spec.season === "winter") ...           │
   │ if (spec.country === "sparta") ...          │
   │                                            │
   │ PROBLEM: Changing rules requires a         │
   │ code change, test run, and deployment.     │
   └────────────────────────────────────────────┘

   ADAPTIVE MODEL APPROACH (rules in data):
   ┌────────────────────────────────────────────┐
   │ RULES.JSON:                                │
   │ [                                          │
   │   { "when": "atNight",                     │
   │     "then": "whispering death" },          │
   │   { "when": "season.winter",               │
   │     "then": "beefy" }                      │
   │ ]                                          │
   │                                            │
   │ interpreter.evaluate(spec, RULES)          │
   │                                            │
   │ BENEFIT: Rules updated by downloading     │
   │ new JSON. No code change. No deploy.       │
   └────────────────────────────────────────────┘
```

---

### 2. When to Use an Adaptive Model

The adaptive model pattern is powerful but complex. Apply it only when:

| Use Adaptive Model When... | Stay Imperative When... |
| :--- | :--- |
| Rules must run on multiple platforms (web, mobile, server) | Rules are simple and unlikely to change |
| Rules change frequently without code deployment | Only one team/platform needs the rules |
| Non-engineers need to understand or modify the rules | Rules require complex computation not expressible as data |
| A DSL for domain experts is needed | The overhead of an interpreter is not justified |

---

### 3. The Production Rule System Pattern

A **Production Rule System** organizes computation through a collection of *Production Rules*, each structured as:

```
Rule = { condition: <predicate>, action: <consequence> }
```

The interpreter evaluates each rule's condition against the input. If the condition is true, the action is applied. Rules are applied in order (or using priority/salience if ordering matters).

```
             PRODUCTION RULE SYSTEM — EVALUATION FLOW

   INPUT SPEC
       │
       ▼
   ┌───────────────────────────────────────────┐
   │ FOR EACH rule IN rules:                   │
   │   IF evaluate(rule.condition, spec):      │
   │       apply(rule.action, result)          │
   └───────────────────────────────────────────┘
       │
       ▼
   OUTPUT RESULT (list of recommendations)
```

---

### 4. Code Examples — Imperative to Adaptive Model

#### JavaScript Implementation
```javascript
// BEFORE: Imperative recommendation logic
function recommend(spec) {
  const result = [];
  if (spec.atNight) result.push("whispering death");
  if (spec.seasons && spec.seasons.includes("winter")) result.push("beefy");
  if (spec.seasons && spec.seasons.includes("summer")) {
    if (["sparta", "atlantis"].includes(spec.country)) result.push("white lightning");
    if (spec.minDuration >= 150) {
      if (spec.minDuration < 350) result.push("white lightning");
      else if (spec.minDuration < 570) result.push("little master");
      else result.push("wall");
    }
  }
  return [...new Set(result)];
}

// AFTER: Adaptive Model with Production Rule System
const RULES = [
  { condition: spec => spec.atNight,                                   action: "whispering death" },
  { condition: spec => spec.seasons?.includes("winter"),               action: "beefy" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       ["sparta", "atlantis"].includes(spec.country),  action: "white lightning" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       spec.minDuration >= 150 && spec.minDuration < 350, action: "white lightning" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       spec.minDuration >= 350 && spec.minDuration < 570, action: "little master" },
  { condition: spec => spec.seasons?.includes("summer") &&
                       spec.minDuration >= 570,                        action: "wall" },
];

function recommend(spec, rules = RULES) {
  const result = rules
    .filter(rule => rule.condition(spec))
    .map(rule => rule.action);
  return [...new Set(result)];
}
```

##### Java Implementation
```java
// Adaptive Model with Production Rule System in Java
@FunctionalInterface
interface Condition { boolean test(Spec spec); }

record Rule(Condition condition, String recommendation) {}

class RecommendationEngine {
    private final List<Rule> rules;

    public RecommendationEngine(List<Rule> rules) { this.rules = rules; }

    public Set<String> recommend(Spec spec) {
        return rules.stream()
            .filter(rule -> rule.condition().test(spec))
            .map(Rule::recommendation)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Rules loaded from JSON configuration at startup
    public static RecommendationEngine fromJson(String rulesJson) {
        // Parse rulesJson and build Rule objects with compiled conditions
        // In production: use a rules DSL compiler (e.g., Drools, Easy Rules)
        return new RecommendationEngine(parseRules(rulesJson));
    }
}

// Usage:
// RecommendationEngine engine = RecommendationEngine.fromJson(loadRulesFromConfig());
// Set<String> recommendations = engine.recommend(userSpec);
```

##### Python Implementation
```python
from typing import Callable, List, Set
from dataclasses import dataclass

@dataclass
class Spec:
    at_night: bool = False
    seasons: List[str] = None
    country: str = ""
    min_duration: int = 0

@dataclass
class Rule:
    condition: Callable[[Spec], bool]
    recommendation: str

# BEFORE: Imperative
def recommend_imperative(spec: Spec) -> List[str]:
    result = []
    if spec.at_night:
        result.append("whispering death")
    if spec.seasons and "winter" in spec.seasons:
        result.append("beefy")
    return list(set(result))

# AFTER: Adaptive Model
RULES: List[Rule] = [
    Rule(condition=lambda s: s.at_night, recommendation="whispering death"),
    Rule(condition=lambda s: s.seasons and "winter" in s.seasons, recommendation="beefy"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and s.country in ("sparta", "atlantis"), recommendation="white lightning"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and 150 <= s.min_duration < 350, recommendation="white lightning"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and 350 <= s.min_duration < 570, recommendation="little master"),
    Rule(condition=lambda s: s.seasons and "summer" in s.seasons
         and s.min_duration >= 570, recommendation="wall"),
]

def recommend(spec: Spec, rules: List[Rule] = RULES) -> Set[str]:
    """Evaluate all rules and return unique recommendations."""
    return {rule.recommendation for rule in rules if rule.condition(spec)}
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

### 1. Designing for Unforeseen Change
The adaptive model of refactoring focuses on structuring software to gracefully accommodate unknown future requirements. Rather than attempting to predict every possible edge case (which often leads to speculative generality), the goal is to maintain a state of "softness" or malleability in the architecture.

### 2. Continuous Evolution
Adaptive refactoring is not a distinct phase but a continuous, integrated activity. It requires constant vigilance against structural degradation and a commitment to incremental improvement, ensuring the codebase remains aligned with the shifting realities of the business domain.

### 3. Feedback Loops and Safenets
A robust adaptive model relies heavily on rapid feedback loops, primarily provided by a comprehensive suite of automated tests. This safety net allows developers to experiment and iterate aggressively, confidently reshaping the architecture as new insights are gained.

---
