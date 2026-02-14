# Kotlinx-Serialization Migration - Design

**Date:** 2026-02-14
**Status:** Approved
**Author:** Design collaboration with user

---

## Overview

This design migrates PyanPyan from kondor-json to kotlinx-serialization to resolve Android DEX conflicts. The implementation provides:

- kotlinx-serialization annotations on all domain models
- Custom serializers for value classes and sealed classes
- RepositoryResult retained for functional error handling (similar to kondor-outcome)
- Complete removal of kondor dependencies
- CamelCase JSON field naming (Android convention)
- Comprehensive test coverage for serialization

---

## Problem Statement

Kondor-json is causing DEX conflicts in the Android build. While the JsonChecklistRepository already uses kotlinx-serialization, kondor is still present in test dependencies and codec files. This creates:

- Build conflicts with Android DEX
- Dual serialization approaches in the codebase
- Unnecessary complexity and dependencies

---

## Architecture

### Module Structure

```
common/
  └── domain/
      ├── model/              (add @Serializable annotations)
      │   ├── Checklist.kt
      │   ├── ChecklistItem.kt
      │   ├── ChecklistSchedule.kt
      │   ├── TimeRange.kt
      │   ├── ChecklistItemState.kt
      │   ├── ChecklistColor.kt
      │   └── StatePersistenceDuration.kt
      ├── repository/
      │   ├── ChecklistRepository.kt        (keep as-is)
      │   ├── RepositoryError.kt            (keep as-is)
      │   ├── RepositoryResult.kt           (keep as-is)
      │   └── json/
      │       ├── ValueClassSerializers.kt  (NEW - for value classes)
      │       └── DefaultData.kt            (update for kotlinx-serialization)

androidMain/
  └── repository/
      └── JsonChecklistRepository.kt        (already uses kotlinx-serialization, keep)
```

### Design Decisions

1. **@Serializable on all domain models** - Clean annotation-based approach
2. **Custom serializers in separate file** - Better organization and reusability
3. **CamelCase JSON fields** - Android convention, kotlinx-serialization default
4. **Keep RepositoryResult** - Already implemented, works well for error handling
5. **Remove kondor completely** - From all sourcesets including tests
6. **Tests use kotlinx-serialization** - Verify JSON format and roundtrip serialization

---

## Serialization Implementation

### Value Class Serializers

Value classes like `ChecklistId`, `ChecklistItemId`, and `ItemIconId` need custom serializers because kotlinx-serialization doesn't automatically handle @JvmInline value classes.

**Implementation Pattern:**

```kotlin
object ChecklistIdSerializer : KSerializer<ChecklistId> {
    override val descriptor = PrimitiveSerialDescriptor("ChecklistId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChecklistId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ChecklistId {
        return ChecklistId(decoder.decodeString())
    }
}

object ChecklistItemIdSerializer : KSerializer<ChecklistItemId> {
    override val descriptor = PrimitiveSerialDescriptor("ChecklistItemId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChecklistItemId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ChecklistItemId {
        return ChecklistItemId(decoder.decodeString())
    }
}

object ItemIconIdSerializer : KSerializer<ItemIconId> {
    override val descriptor = PrimitiveSerialDescriptor("ItemIconId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemIconId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ItemIconId {
        return ItemIconId(decoder.decodeString())
    }
}
```

All value class IDs follow this same pattern - they're transparent strings in JSON.

### Sealed Class Serializers

For sealed classes (`TimeRange`, `ChecklistItemState`), we'll use kotlinx-serialization's polymorphic serialization with a custom discriminator field.

**TimeRange:**

```kotlin
@Serializable
sealed class TimeRange {
    abstract val isAllDay: Boolean
    abstract fun contains(time: LocalTime): Boolean

    @Serializable
    @SerialName("AllDay")
    data object AllDay : TimeRange() {
        override val isAllDay: Boolean = true
        override fun contains(time: LocalTime): Boolean = true
    }

    @Serializable
    @SerialName("Specific")
    data class Specific(
        val startTime: LocalTime,
        val endTime: LocalTime
    ) : TimeRange() {
        override val isAllDay: Boolean = false

        override fun contains(time: LocalTime): Boolean {
            return time >= startTime && time <= endTime
        }
    }
}
```

**ChecklistItemState:**

```kotlin
@Serializable
sealed interface ChecklistItemState {
    @Serializable
    @SerialName("Pending")
    data object Pending : ChecklistItemState

    @Serializable
    @SerialName("Done")
    data object Done : ChecklistItemState

    @Serializable
    @SerialName("IgnoredToday")
    data object IgnoredToday : ChecklistItemState
}
```

**JSON Configuration:**

```kotlin
val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    classDiscriminator = "type"  // Use "type" instead of default "type"
}
```

### Enum Serializers

Enums serialize as strings using their name values automatically:

- `ChecklistColor` → `"SOFT_BLUE"`, `"CALM_GREEN"`, etc.
- `StatePersistenceDuration` → `"NEVER"`, `"FIFTEEN_MINUTES"`, etc.
- `DayOfWeek` → `"MONDAY"`, `"TUESDAY"`, etc.

No custom serializers needed.

### DateTime Types

`LocalTime` and `Instant` from kotlinx-datetime have built-in serialization support:

- `LocalTime` → `"14:30:00"` (ISO-8601 format)
- `Instant` → `"2024-01-15T10:30:00Z"` (ISO-8601 format)

No custom serializers needed.

---

## Data Model Annotations

### Checklist

```kotlin
@Serializable
data class Checklist(
    @Serializable(with = ChecklistIdSerializer::class)
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration,
    val lastAccessedAt: Instant? = null
) {
    fun updateItem(updatedItem: ChecklistItem): Checklist {
        val newItems = items.map {
            if (it.id == updatedItem.id) updatedItem else it
        }
        return copy(items = newItems)
    }

    fun resetAllItems(): Checklist {
        return copy(items = items.map { it.reset() })
    }

    fun findItem(itemId: ChecklistItemId): ChecklistItem? {
        return items.find { it.id == itemId }
    }
}
```

### ChecklistItem

```kotlin
@Serializable
data class ChecklistItem(
    @Serializable(with = ChecklistItemIdSerializer::class)
    val id: ChecklistItemId,
    val title: String,
    @Serializable(with = ItemIconIdSerializer::class)
    val iconId: ItemIconId? = null,
    val state: ChecklistItemState
) {
    fun markDone(): ChecklistItem = copy(state = ChecklistItemState.Done)
    fun markIgnored(): ChecklistItem = copy(state = ChecklistItemState.IgnoredToday)
    fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
}
```

### ChecklistSchedule

```kotlin
@Serializable
data class ChecklistSchedule(
    val daysOfWeek: Set<DayOfWeek>,
    val timeRange: TimeRange
)
```

---

## JSON Format

### Example JSON Output

```json
[
  {
    "id": "school",
    "name": "School",
    "schedule": {
      "daysOfWeek": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
      "timeRange": {
        "type": "AllDay"
      }
    },
    "items": [
      {
        "id": "books",
        "title": "Books in bag",
        "iconId": null,
        "state": {
          "type": "Pending"
        }
      },
      {
        "id": "tooth",
        "title": "Brushing teeth",
        "iconId": "tooth",
        "state": {
          "type": "Done"
        }
      }
    ],
    "color": "SOFT_BLUE",
    "statePersistence": "FIFTEEN_MINUTES",
    "lastAccessedAt": "2024-01-15T10:30:00Z"
  }
]
```

### Key Format Decisions

- **CamelCase field names** - Android convention (e.g., `daysOfWeek`, `timeRange`, `lastAccessedAt`)
- **Discriminator field** - `"type"` for sealed classes
- **Value classes** - Transparent strings (e.g., `"id": "school"`)
- **Enums** - String names (e.g., `"color": "SOFT_BLUE"`)
- **DateTime** - ISO-8601 format
- **Nullable fields** - `null` in JSON
- **Collections** - Standard JSON arrays

---

## Repository Layer

### JsonChecklistRepository

**No changes needed** - Already uses kotlinx-serialization. The existing implementation will work once domain models have @Serializable annotations.

```kotlin
class JsonChecklistRepository(
    private val storageDir: File
) : ChecklistRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun getAllChecklists(): RepositoryResult<List<Checklist>> =
        withContext(Dispatchers.IO) {
            try {
                val jsonText = file.readText()
                val checklists = json.decodeFromString<List<Checklist>>(jsonText)
                RepositoryResult.Success(checklists)
            } catch (e: Exception) {
                RepositoryResult.Failure(...)
            }
        }
}
```

### RepositoryResult

**Keep as-is** - Already provides functional error handling similar to kondor-outcome:

```kotlin
sealed class RepositoryResult<out T> {
    data class Success<out T>(val value: T) : RepositoryResult<T>()
    data class Failure(val error: RepositoryError) : RepositoryResult<Nothing>()

    fun map(transform: (T) -> R): RepositoryResult<R>
    fun flatMap(transform: (T) -> RepositoryResult<R>): RepositoryResult<R>
    fun onSuccess(action: (T) -> Unit): RepositoryResult<T>
    fun onFailure(action: (RepositoryError) -> Unit): RepositoryResult<T>
}
```

---

## Testing Strategy

### Test Coverage

**Unit tests for serialization (`ChecklistSerializationTest.kt`):**

1. **Value classes:**
   - ChecklistId roundtrip
   - ChecklistItemId roundtrip
   - ItemIconId roundtrip

2. **Enums:**
   - ChecklistColor roundtrip
   - StatePersistenceDuration roundtrip
   - DayOfWeek roundtrip

3. **DateTime types:**
   - LocalTime roundtrip
   - Instant roundtrip

4. **Sealed classes:**
   - TimeRange.AllDay roundtrip
   - TimeRange.Specific roundtrip
   - ChecklistItemState.Pending roundtrip
   - ChecklistItemState.Done roundtrip
   - ChecklistItemState.IgnoredToday roundtrip

5. **Complex objects:**
   - ChecklistSchedule roundtrip
   - ChecklistItem roundtrip (with and without iconId)
   - Checklist roundtrip (full object)
   - List<Checklist> roundtrip
   - Empty list roundtrip

6. **JSON format validation:**
   - Verify camelCase field names
   - Verify "type" discriminator for sealed classes
   - Verify value classes serialize as strings

### Test Implementation

```kotlin
class ChecklistSerializationTest {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun checklist_id_roundtrip() {
        val original = ChecklistId("test-123")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistId>(jsonString)
        assertEquals(original, decoded)
    }

    @Test
    fun checklist_roundtrip() {
        val original = Checklist(...)
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<Checklist>(jsonString)
        assertEquals(original, decoded)
    }

    @Test
    fun json_uses_camelcase_field_names() {
        val checklist = Checklist(...)
        val jsonString = json.encodeToString(checklist)

        // Verify camelCase field names
        assert(jsonString.contains("\"lastAccessedAt\""))
        assert(jsonString.contains("\"statePersistence\""))
        assert(!jsonString.contains("\"last_accessed_at\""))
        assert(!jsonString.contains("\"state_persistence\""))
    }
}
```

---

## DefaultData Update

Update `DefaultData.kt` to use kotlinx-serialization models:

```kotlin
object DefaultData {
    fun createDefaultChecklists(): List<Checklist> {
        return listOf(
            Checklist(
                id = ChecklistId("school"),
                name = "School",
                schedule = ChecklistSchedule(
                    daysOfWeek = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ),
                    timeRange = TimeRange.AllDay
                ),
                items = listOf(
                    ChecklistItem(
                        id = ChecklistItemId("books"),
                        title = "Books in bag",
                        iconId = null,
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("homework"),
                        title = "Homework",
                        iconId = null,
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("pekit"),
                        title = "PE kit",
                        iconId = null,
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("breakfast"),
                        title = "Breakfast",
                        iconId = null,
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("tooth"),
                        title = "Brushing teeth",
                        iconId = ItemIconId("tooth"),
                        state = ChecklistItemState.Pending
                    )
                ),
                color = ChecklistColor.SOFT_BLUE,
                statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
                lastAccessedAt = null
            )
        )
    }
}
```

---

## Dependency Changes

### Remove from `common/build.gradle.kts`

**Delete kondor dependencies:**

```kotlin
val androidUnitTest by getting {
    dependencies {
        // REMOVE THESE:
        // implementation("com.ubertob.kondor:kondor-core:3.6.1")
        // implementation("com.ubertob.kondor:kondor-outcome:3.6.1")
    }
}
val androidInstrumentedTest by getting {
    dependencies {
        // REMOVE THESE:
        // implementation("com.ubertob.kondor:kondor-core:3.6.1")
        // implementation("com.ubertob.kondor:kondor-outcome:3.6.1")
    }
}
```

**Keep existing dependencies:**

```kotlin
val commonMain by getting {
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    }
}
```

---

## Migration Steps

1. **Update dependencies** - Remove kondor from build.gradle.kts
2. **Create ValueClassSerializers.kt** - Custom serializers for value classes
3. **Add @Serializable annotations** - To TimeRange and ChecklistItemState sealed classes
4. **Add @Serializable annotations** - To all data classes (Checklist, ChecklistItem, ChecklistSchedule)
5. **Update DefaultData.kt** - Remove kondor imports, keep implementation
6. **Delete kondor files** - Remove ChecklistCodecs.kt
7. **Replace tests** - Create ChecklistSerializationTest.kt with kotlinx-serialization tests
8. **Delete old tests** - Remove ChecklistCodecsTest.kt
9. **Verify build** - Run tests and ensure no DEX conflicts

---

## Implementation Checklist

**Dependencies:**
- [ ] Remove kondor-core from androidUnitTest dependencies
- [ ] Remove kondor-outcome from androidUnitTest dependencies
- [ ] Remove kondor-core from androidInstrumentedTest dependencies
- [ ] Remove kondor-outcome from androidInstrumentedTest dependencies

**Serializers:**
- [ ] Create ValueClassSerializers.kt with ChecklistIdSerializer
- [ ] Add ChecklistItemIdSerializer
- [ ] Add ItemIconIdSerializer

**Domain Models:**
- [ ] Add @Serializable to TimeRange sealed class
- [ ] Add @Serializable to ChecklistItemState sealed interface
- [ ] Add @Serializable to ChecklistSchedule
- [ ] Add @Serializable to ChecklistItem
- [ ] Add @Serializable to Checklist
- [ ] Add @Serializable to ChecklistColor enum
- [ ] Add @Serializable to StatePersistenceDuration enum

**Data & Tests:**
- [ ] Update DefaultData.kt (remove kondor imports)
- [ ] Delete ChecklistCodecs.kt
- [ ] Delete ChecklistCodecsTest.kt
- [ ] Create ChecklistSerializationTest.kt with full test coverage

**Verification:**
- [ ] Run all tests
- [ ] Verify no DEX conflicts
- [ ] Verify JSON format matches expected output

---

## References

- Main spec: `/specs.md`
- Domain models: `/common/src/commonMain/kotlin/com/pyanpyan/domain/model/`
- Repository: `/common/src/commonMain/kotlin/com/pyanpyan/domain/repository/`
- kotlinx-serialization: https://github.com/Kotlin/kotlinx.serialization
