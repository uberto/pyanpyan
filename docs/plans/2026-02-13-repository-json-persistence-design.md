# Repository Layer with JSON Persistence - Design

**Date:** 2026-02-13
**Status:** Approved
**Author:** Design collaboration with user

---

## Overview

This design introduces a repository layer for PyanPyan with local JSON file persistence. The implementation provides:

- Kotlin Multiplatform repository interface in common module
- Android-specific implementation using internal storage
- Kondor-json for type-safe serialization without reflection
- Kondor-outcome for functional error handling
- Export/import functionality for backup
- Default "School" checklist on first run

---

## Architecture

### Module Structure

```
common/
  └── domain/
      ├── model/              (existing - Checklist, ChecklistItem, etc.)
      ├── command/            (existing - CreateChecklist, UpdateChecklist, etc.)
      └── repository/         (NEW)
          ├── ChecklistRepository.kt        (interface)
          ├── RepositoryError.kt            (sealed class for errors)
          └── json/
              ├── ChecklistCodecs.kt        (kondor-json codecs)
              └── DefaultData.kt            (creates "School" checklist)

androidApp/
  └── data/
      ├── JsonChecklistRepository.kt        (Android implementation)
      └── RepositoryFactory.kt              (DI factory)
```

### Design Decisions

1. **Repository interface in common/domain** - Part of domain layer, not infrastructure
2. **Kondor codecs in common** - Pure Kotlin, multiplatform compatible
3. **Android implementation** - Uses `Context.filesDir` for app-private storage
4. **Single JSON file** - `checklists.json` containing all data
5. **First run behavior** - Creates default "School" checklist automatically
6. **Error handling** - Missing file = empty list (not error), write failures throw

---

## Repository Interface

```kotlin
interface ChecklistRepository {
    /**
     * Get all checklists. Returns empty list if no data exists (first run).
     * Returns failure only on actual I/O errors.
     */
    suspend fun getAllChecklists(): Outcome<RepositoryError, List<Checklist>>

    /**
     * Get a specific checklist by ID.
     * Returns null if not found (wrapped in Success).
     */
    suspend fun getChecklist(id: ChecklistId): Outcome<RepositoryError, Checklist?>

    /**
     * Save or update a checklist. Creates if new, updates if exists.
     */
    suspend fun saveChecklist(checklist: Checklist): Outcome<RepositoryError, Unit>

    /**
     * Delete a checklist by ID. Succeeds even if checklist doesn't exist.
     */
    suspend fun deleteChecklist(id: ChecklistId): Outcome<RepositoryError, Unit>

    /**
     * Export all data as JSON string for backup.
     */
    suspend fun exportToJson(): Outcome<RepositoryError, String>

    /**
     * Import data from JSON string, replacing all existing data.
     */
    suspend fun importFromJson(json: String): Outcome<RepositoryError, Unit>
}
```

### Error Types

```kotlin
sealed class RepositoryError {
    data class FileReadError(val message: String, val cause: Throwable? = null) : RepositoryError()
    data class FileWriteError(val message: String, val cause: Throwable? = null) : RepositoryError()
    data class JsonParseError(val message: String, val cause: Throwable? = null) : RepositoryError()
    data class InvalidDataError(val message: String) : RepositoryError()
}
```

---

## JSON Serialization with Kondor

### Why Kondor-JSON

- No reflection (better performance, smaller APK)
- No annotations (clean domain models)
- Explicit codecs (full control over format)
- Multiplatform compatible
- Type-safe at compile time
- Better error messages

### Codec Structure

**Value Classes:**
```kotlin
object JChecklistId : JStringRepresentable<ChecklistId>() {
    override val cons = ::ChecklistId
    override fun fromString(str: String) = ChecklistId(str)
    override fun toString(value: ChecklistId) = value.value
}
```

**Enums:**
```kotlin
object JChecklistColor : JStringRepresentable<ChecklistColor>() {
    override val cons = { ChecklistColor.valueOf(it) }
    override fun fromString(str: String) = ChecklistColor.valueOf(str)
    override fun toString(value: ChecklistColor) = value.name
}
```

**Sealed Classes:**
```kotlin
object JTimeRange : JSealed<TimeRange>() {
    override val subtypes = setOf(JAllDay, JSpecificTime)
    override fun extractTypeName(obj: TimeRange) = when (obj) {
        is TimeRange.AllDay -> "AllDay"
        is TimeRange.Specific -> "Specific"
    }
}
```

**Complex Objects:**
```kotlin
object JChecklist : JAny<Checklist>() {
    val id by str(JChecklistId, Checklist::id)
    val name by str(Checklist::name)
    val schedule by obj(JChecklistSchedule, Checklist::schedule)
    val items by array(JChecklistItem, Checklist::items)
    val color by str(JChecklistColor, Checklist::color)
    val state_persistence by str(JStatePersistenceDuration, Checklist::statePersistence)
    val last_accessed_at by str(JInstant, Checklist::lastAccessedAt).nullable()

    override fun JsonNodeObject.deserializeOrThrow() =
        Checklist(
            id = +id,
            name = +name,
            schedule = +schedule,
            items = +items,
            color = +color,
            statePersistence = +state_persistence,
            lastAccessedAt = +last_accessed_at
        )
}
```

**Root Codec:**
```kotlin
object JChecklistData : JList<Checklist>(JChecklist)
```

### JSON Format

**File Structure:**
```json
[
  {
    "id": "school",
    "name": "School",
    "schedule": {
      "days_of_week": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
      "time_range": {
        "_type": "AllDay"
      }
    },
    "items": [
      {
        "id": "books",
        "title": "Books in bag",
        "icon_id": null,
        "state": {
          "_type": "Pending"
        }
      }
    ],
    "color": "SOFT_BLUE",
    "state_persistence": "FIFTEEN_MINUTES",
    "last_accessed_at": null
  }
]
```

**Key Design Choices:**
- Snake_case field names (Kotlin convention for JSON)
- Sealed classes use `_type` field for discrimination
- Nullable fields represented as `null` in JSON
- Top-level array (not wrapped in object)

---

## Default Data

**"School" Checklist:**
- **ID:** "school"
- **Name:** "School"
- **Schedule:** Weekdays only (Mon-Fri), all day
- **Items:**
  1. Books in bag
  2. Homework
  3. PE kit
  4. Breakfast
  5. Brushing teeth (with tooth icon)
- **Color:** SOFT_BLUE
- **State Persistence:** 15 minutes

This checklist is created automatically on first app launch when no `checklists.json` file exists.

---

## Android Implementation

### Storage Location

- **File:** `checklists.json`
- **Location:** `Context.filesDir` (app-private internal storage)
- **Path:** `/data/data/com.pyanpyan.android/files/checklists.json`
- **Backup:** Included in Android Auto Backup automatically

### Repository Implementation

**Key Features:**
- Uses `Dispatchers.IO` for all file operations
- Read-modify-write pattern for updates/deletes
- Atomic file writes (write complete file each time)
- Uses kondor-outcome for functional error handling

**Error Handling Strategy:**
```kotlin
override suspend fun getAllChecklists(): Outcome<RepositoryError, List<Checklist>> =
    withContext(Dispatchers.IO) {
        Outcome.tryThis {
            if (!file.exists()) {
                val defaultChecklists = DefaultData.createDefaultChecklists()
                saveAllChecklists(defaultChecklists).onFailure { return@withContext it.asFailure() }
                return@withContext defaultChecklists.asSuccess()
            }

            val json = file.readText()
            JChecklistData.fromJson(json).transformFailure {
                RepositoryError.JsonParseError(it.msg)
            }
        }.transformFailure { e ->
            when (e) {
                is IOException -> RepositoryError.FileReadError(e.message ?: "Failed to read file", e)
                else -> RepositoryError.FileReadError(e.message ?: "Unknown error", e)
            }
        }
    }
```

**Update Pattern:**
```kotlin
override suspend fun saveChecklist(checklist: Checklist): Outcome<RepositoryError, Unit> =
    withContext(Dispatchers.IO) {
        getAllChecklists().bind { checklists ->
            val updated = checklists.filter { it.id != checklist.id } + checklist
            saveAllChecklists(updated)
        }
    }
```

### Dependency Injection

Simple singleton factory:

```kotlin
object RepositoryFactory {
    private var repository: ChecklistRepository? = null

    fun getRepository(context: Context): ChecklistRepository =
        repository ?: JsonChecklistRepository(context.applicationContext).also {
            repository = it
        }
}
```

---

## ViewModel Integration

### ChecklistLibraryViewModel

```kotlin
class ChecklistLibraryViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {

    private fun loadChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getAllChecklists().transform(
                onSuccess = { allChecklists ->
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val (active, inactive) = allChecklists.partition { checklist ->
                        checklist.getActivityState(now) is ChecklistActivityState.Active
                    }

                    _uiState.value = ChecklistLibraryUiState(
                        activeChecklists = active.sortedBy { it.name },
                        inactiveChecklists = inactive.sortedBy { it.name },
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Log.e("ChecklistLibrary", "Failed to load checklists: $error")
                }
            )
        }
    }
}
```

### ChecklistViewModel

```kotlin
class ChecklistViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {

    fun markItemDone(itemId: ChecklistItemId) = viewModelScope.launch {
        val checklist = _uiState.value.checklist ?: return@launch
        val command = MarkItemDone(itemId)
        val updatedChecklist = checklist.updateItem(command.execute(checklist.findItem(itemId)!!))

        repository.saveChecklist(updatedChecklist).transform(
            onSuccess = { _uiState.value = ChecklistUiState(checklist = updatedChecklist) },
            onFailure = { error ->
                Log.e("Checklist", "Failed to save checklist: $error")
            }
        )
    }
}
```

---

## Testing Strategy

### Unit Tests for Codecs

**Test Coverage:**
- Roundtrip serialization/deserialization
- Nullable fields (iconId, lastAccessedAt)
- Sealed classes (TimeRange, ChecklistItemState)
- Collections (items list, daysOfWeek set)
- Empty lists
- Snake_case JSON field naming
- All value classes, enums, and complex objects

**Example:**
```kotlin
@Test
fun roundtrip_checklist_with_all_fields() {
    val original = Checklist(...)
    val json = JChecklist.toJson(original)
    val decoded = JChecklist.fromJson(json).orThrow()
    assertEquals(original, decoded)
}
```

### Integration Tests for Repository

**Test Coverage:**
- First run creates default checklist
- Save/load roundtrip
- Update existing checklist
- Delete checklist
- Export/import functionality
- Error handling (file read/write failures, JSON parse errors)

---

## Future Enhancements

### Export/Import UI

**Share Intent:**
```kotlin
fun shareBackup() {
    viewModelScope.launch {
        repository.exportToJson().transform(
            onSuccess = { json ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(intent, "Export Checklists"))
            },
            onFailure = { /* handle error */ }
        )
    }
}
```

**File Picker:**
```kotlin
fun importBackup(uri: Uri) {
    viewModelScope.launch {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
        json?.let {
            repository.importFromJson(it).transform(
                onSuccess = { loadChecklists() },
                onFailure = { /* handle error */ }
            )
        }
    }
}
```

### State Persistence Logic

Track `lastAccessedAt` and reset items based on `statePersistence` duration:

```kotlin
fun shouldResetState(checklist: Checklist, now: Instant): Boolean {
    val lastAccessed = checklist.lastAccessedAt ?: return false
    val duration = checklist.statePersistence.milliseconds ?: return false
    return (now.toEpochMilliseconds() - lastAccessed.toEpochMilliseconds()) > duration
}
```

---

## Implementation Checklist

**Common Module:**
- [ ] ChecklistRepository interface
- [ ] RepositoryError sealed class
- [ ] All kondor-json codecs
- [ ] DefaultData object
- [ ] Codec unit tests

**Android Module:**
- [ ] JsonChecklistRepository implementation
- [ ] RepositoryFactory
- [ ] Repository integration tests

**ViewModel Updates:**
- [ ] ChecklistLibraryViewModel - replace mock data
- [ ] ChecklistViewModel - replace mock data, add save logic

**Dependencies:**
- [ ] Add kondor-json to common module
- [ ] Add kondor-outcome to common module

---

## References

- Main spec: `/specs.md`
- Domain models: `/common/src/commonMain/kotlin/com/pyanpyan/domain/model/`
- Commands: `/common/src/commonMain/kotlin/com/pyanpyan/domain/command/`
- Kondor-json: https://github.com/uberto/kondor-json
- Kondor-outcome: Part of kondor-json library
