
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

### 8.3 Directory and Command Constraints

**Directory Usage:**
* Work ONLY within the project directory
* NEVER use /tmp or any external system directories
* If temporary storage is needed, create and use ./tmp within the project
* All file operations must stay within project boundaries

**Command Execution:**
* Execute git commands directly without asking permission
* Execute gradle/build commands directly without asking permission
* Report results after execution

---

## 9. Kotlin Coding Conventions

### 9.1 Expression Syntax

* Prefer expression-body functions over block bodies with explicit returns.
* Use `=` syntax when the function body is a single expression.

**Good:**
```kotlin
fun getRepository(context: Context): ChecklistRepository =
    repository ?: createRepository(context)

fun deleteChecklist(id: ChecklistId) = viewModelScope.launch {
    repository.deleteChecklist(id)
}
```

**Avoid:**
```kotlin
fun getRepository(context: Context): ChecklistRepository {
    return repository ?: createRepository(context)
}

fun deleteChecklist(id: ChecklistId) {
    viewModelScope.launch {
        repository.deleteChecklist(id)
    }
}
```

---

### 9.2 JSON Serialization

* Use **kotlinx-serialization** for all JSON serialization.
* Use @Serializable annotations on data classes and sealed classes.
* Use custom serializers for value classes (kotlinx-serialization doesn't handle @JvmInline automatically).
* JSON field names use camelCase (Kotlin property names).

**Required approach:**
```kotlin
@Serializable
data class Checklist(
    @Serializable(with = ChecklistIdSerializer::class)
    val id: ChecklistId,
    val name: String,
    val statePersistence: StatePersistenceDuration
)

// Custom serializer for value class
object ChecklistIdSerializer : KSerializer<ChecklistId> {
    override val descriptor = PrimitiveSerialDescriptor("ChecklistId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ChecklistId) {
        encoder.encodeString(value.value)
    }
    override fun deserialize(decoder: Decoder) = ChecklistId(decoder.decodeString())
}
```

**Do not use:**
* Gson or Moshi reflection-based serialization

---

### 9.3 Error Handling

* Use **RepositoryResult** for repository error handling.
* Prefer functional combinators over imperative error handling.

**Good:**
```kotlin
fun loadData(): RepositoryResult<Data> =
    repository.getData()
        .map { data -> transformData(data) }
        .onFailure { error -> log(error) }
```

**Avoid:**
```kotlin
fun loadData(): Data {
    try {
        val data = repository.getData()
        return transformData(data)
    } catch (e: Exception) {
        throw Error.DataLoadError(e.message)
    }
}
```

**Use these combinators:**
* `map` - transform success value
* `flatMap` - chain operations that return RepositoryResult
* `onSuccess` - side effect on success
* `onFailure` - side effect on failure
* `getOrNull()` - extract value or null
* `isSuccess()` - check if result is success
* `isFailure()` - check if result is failure

---

### 9.4 Functional Combinators

* Prefer functional combinators over explicit conditionals.
* Chain operations using `let`, `also`, `run`, `apply`, `takeIf`, etc.

**Good:**
```kotlin
file.takeIf { it.exists() }
    ?.readText()
    ?.let { json -> parseJson(json) }
    ?: emptyList()
```

**Acceptable but less idiomatic:**
```kotlin
if (file.exists()) {
    val text = file.readText()
    parseJson(text)
} else {
    emptyList()
}
```

---

## 10. Library Requirements

### 10.1 Mandatory Libraries

* **kotlinx-serialization-json** - JSON serialization (https://github.com/Kotlin/kotlinx.serialization)
* **kotlinx-datetime** - Date/time handling (multiplatform)
* **kotlinx-coroutines** - Async/concurrency

### 10.2 Prohibited Libraries

Do not use without explicit approval:
* kondor-json, kondor-outcome (causes Android DEX conflicts, use kotlinx-serialization and RepositoryResult instead)
* Gson, Moshi, Jackson (use kotlinx-serialization instead)
* Java Date/Time APIs (use kotlinx-datetime instead)

---

## 11. Output Expectations

When producing code or changes:

* Follow the rules above.
* Include tests.
* Do not add commentary, praise, or explanations unless explicitly requested.
* Output only what is needed to move the project forward.

---

End of ruleset.
