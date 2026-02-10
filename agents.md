
# Instructions for AI agents

This file defines the **ruleset and working contract** for AI agents contributing to the Pyanpyan project.

Agents must follow these rules strictly.

---

## 1. Project Context

* Project: **Pyanpyan** (gentle checklist & timer app)
* Architecture: Kotlin Multiplatform
* First target platform: Android
* Other platforms may follow

---

## 2. General Behavior Rules

* Do not praise the user.
* Do not flatter or comment on intelligence or skill.
* Do not explain basic or obvious technical concepts.
* Stay factual and concise.
* Do not add features, constraints, controls, or requirements that are not explicitly requested.
* If something is unspecified, ask or leave it open.

---

## 3. Design & Architecture Principles

### 3.1 Kotlin Multiplatform

* Core domain logic must be platform-agnostic.
* Platform-specific code must be isolated at the edges.
* No Android-specific dependencies in domain modules.

---

### 3.2 Functional Style

* Prefer immutability.
* Prefer pure functions.
* Side effects must be explicit and isolated.
* Time, randomness, and IO must be injected.

---

### 3.3 Domain-Driven Design (DDD)

* Use a clear ubiquitous language aligned with the product spec.
* Model the domain explicitly.
* Separate domain, application, and infrastructure concerns.

Required patterns:

* Commands and Queries (CQRS / CQS)
* Explicit domain state
* Explicit state transitions

Avoid:

* Anemic domain models
* Logic hidden in UI or infrastructure layers

---

### 3.4 Command–Query Separation

* Commands mutate state and return no data (or minimal status only).
* Queries read state and must never mutate it.
* Commands and queries must be testable in isolation.

---

## 4. Kent Beck – Four Rules of Simple Design

All code must follow these rules, in this order:

1. **Passes all the tests**
2. **Reveals intention** (clear naming, clear structure)
3. **No duplication**
4. **Fewest elements possible**

Do not optimize prematurely.

---

## 5. Testing Rules

### 5.1 Acceptance Tests (Mandatory)

* Every feature must have at least one **acceptance test**.
* Acceptance tests must cover the **happy path**.
* Acceptance tests express behavior in domain terms.
* Acceptance tests must not depend on UI frameworks.

---

### 5.2 Unit Tests (Mandatory)

* Every non-trivial function must have unit tests.
* Tests must be deterministic.
* Time must be controlled.
* No shared mutable state between tests.

---

## 6. State & Time Handling

* Domain logic must not read the system clock directly.
* Time must be injected via interfaces or values.
* State transitions must be explicit and replayable.

---

## 7. Scope Discipline

* Implement only what is explicitly specified.
* Do not add defensive checks unless required by the spec.
* Do not introduce abstractions "just in case".
* Keep solutions minimal and reversible.

---

## 8. Working Process

### 8.1 Planning and Logging
* Maintain a `plan.md` file in the project root.
* Use it to track:
    * Tasks completed.
    * Current task in progress.
    * Planned next steps.
* Update it regularly to provide a clear audit trail of work.

### 8.2 Specification Source of Truth
* The `specs.md` file is the primary source of truth for all application requirements.
* Consult `specs.md` before starting any new feature or change.
* Ensure all implementations align with the principles and requirements defined in `specs.md`.

---

## 9. Output Expectations

When producing code or changes:

* Follow the rules above.
* Include tests.
* Do not add commentary, praise, or explanations unless explicitly requested.
* Output only what is needed to move the project forward.

---

End of ruleset.
