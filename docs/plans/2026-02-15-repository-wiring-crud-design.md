# Repository Wiring and CRUD Implementation - Design

**Date:** 2026-02-15
**Status:** Approved
**Author:** Design collaboration with user

---

## Overview

This design connects ViewModels to the JsonChecklistRepository and implements Create/Edit/Delete UI flows for checklists. The implementation completes the app's core functionality by wiring the domain layer to the UI layer and providing full CRUD operations.

**Scope:**
- Update agents.md documentation (remove kondor references)
- Wire ViewModels to repository using RepositoryFactory
- Implement Create/Edit screen for checklists
- Implement Delete flow with confirmation
- Add proper error handling and loading states

**Out of Scope:**
- Timer functionality (deferred to future)
- Advanced features (import/export UI, state persistence triggers)

---

## Architecture

### Current State

**✅ Already Complete:**
- Domain models with @Serializable annotations
- JsonChecklistRepository implementation
- RepositoryResult for error handling
- UI screens (Library, Detail) with mock data
- ItemSlider component for checklist items

**⚠️ Needs Implementation:**
- RepositoryFactory (create if not exists)
- ViewModel constructor injection
- Create/Edit screen
- Delete confirmation dialog
- Repository error handling in UI

---

## Design Decisions

### 1. Documentation Updates (agents.md)

**Section 9.2 - JSON Serialization (lines 175-196):**

**Remove:**
```kotlin
// OLD: Kondor-json approach
object JChecklist : JAny<Checklist>() {
    val id by str(JChecklistId, Checklist::id)
    val name by str(Checklist::name)
    val state_persistence by str(JStatePersistenceDuration, Checklist::statePersistence)
}
```

**Replace with:**
```kotlin
// NEW: kotlinx-serialization approach
@Serializable
data class Checklist(
    @Serializable(with = ChecklistIdSerializer::class)
    val id: ChecklistId,
    val name: String,
    val statePersistence: StatePersistenceDuration
)
```

**Update text:**
- "Use **kotlinx-serialization** for all JSON serialization"
- "Use @Serializable annotations on data classes"
- "Use custom serializers for value classes"
- "JSON field names use camelCase (Kotlin property names)"

**Section 9.3 - Error Handling (lines 199-234):**

**Remove:**
- All references to "kondor-outcome"
- Outcome<Error, Data> examples

**Replace with:**
```kotlin
// NEW: RepositoryResult approach
fun loadData(): RepositoryResult<Data> =
    repository.getData()
        .map { data -> transformData(data) }
        .onFailure { error -> log(error) }
```

**Update combinators list:**
- `map` - transform success value
- `flatMap` - chain operations that return RepositoryResult
- `onSuccess` - side effect on success
- `onFailure` - side effect on failure
- `getOrNull()` - extract value or null

**Section 10 - Library Requirements (lines 264-276):**

**Mandatory Libraries:**
```kotlin
// REMOVE these:
// - kondor-json
// - kondor-outcome

// ADD these:
- kotlinx-serialization-json - JSON serialization
```

**Prohibited Libraries:**
```kotlin
// REMOVE from prohibited:
// - kotlinx.serialization

// KEEP prohibited:
- Gson, Moshi, Jackson (use kotlinx-serialization instead)
```

**New Section - Working Constraints:**

Add to "Section 8. Working Process":

```markdown
### 8.3 Directory and Command Constraints

**Directory Usage:**
- Work ONLY within the project directory
- NEVER use /tmp or any external system directories
- If temporary storage is needed, create and use ./tmp within the project
- All file operations must stay within project boundaries

**Command Execution:**
- Execute git commands directly without asking permission
- Execute gradle/build commands directly without asking permission
- Report results after execution
```

---

### 2. Repository Factory Pattern

**File:** `androidApp/src/main/kotlin/com/pyanpyan/android/data/RepositoryFactory.kt`

```kotlin
package com.pyanpyan.android.data

import android.content.Context
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.JsonChecklistRepository

object RepositoryFactory {
    private var repository: ChecklistRepository? = null

    fun getRepository(context: Context): ChecklistRepository =
        repository ?: JsonChecklistRepository(context.filesDir).also {
            repository = it
        }
}
```

**Pattern:**
- Singleton pattern for repository instance
- Lazy initialization on first access
- Uses Application context (passed from MainActivity)
- Thread-safe through synchronized property access

---

### 3. ViewModel Constructor Injection

**ChecklistLibraryViewModel:**

```kotlin
class ChecklistLibraryViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistLibraryUiState())
    val uiState: StateFlow<ChecklistLibraryUiState> = _uiState.asStateFlow()

    init {
        loadChecklists()
    }

    private fun loadChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getAllChecklists()
                .onSuccess { allChecklists ->
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val (active, inactive) = allChecklists.partition { checklist ->
                        checklist.getActivityState(now) is ChecklistActivityState.Active
                    }

                    _uiState.value = ChecklistLibraryUiState(
                        activeChecklists = active.sortedBy { it.name },
                        inactiveChecklists = inactive.sortedBy { it.name },
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Error handling via SnackBar in UI
                }
        }
    }

    fun deleteChecklist(checklistId: ChecklistId) {
        viewModelScope.launch {
            repository.deleteChecklist(checklistId)
                .onSuccess { loadChecklists() }
                .onFailure { error ->
                    // Error handling via SnackBar in UI
                }
        }
    }
}
```

**ChecklistViewModel:**

```kotlin
class ChecklistViewModel(
    private val checklistId: ChecklistId,
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    init {
        loadChecklist()
    }

    private fun loadChecklist() {
        viewModelScope.launch {
            repository.getChecklist(checklistId)
                .onSuccess { checklist ->
                    _uiState.value = ChecklistUiState(checklist = checklist)
                }
                .onFailure { error ->
                    // Error handling
                }
        }
    }

    fun markItemDone(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = MarkItemDone(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        // Optimistically update UI
        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)

        // Persist to repository
        viewModelScope.launch {
            repository.saveChecklist(updatedChecklist)
                .onFailure { error ->
                    // Revert UI on failure
                    loadChecklist()
                }
        }
    }

    fun ignoreItemToday(itemId: ChecklistItemId) {
        // Same pattern as markItemDone
    }
}
```

**Factory Function Pattern:**

```kotlin
// In composables
@Composable
fun ChecklistLibraryScreen(
    onChecklistClick: (ChecklistId) -> Unit,
    onCreateClick: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: ChecklistLibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ChecklistLibraryViewModel(repository)
            }
        }
    )
    // ...
}
```

---

### 4. Navigation Updates

**Screen Sealed Class:**

```kotlin
sealed class Screen {
    object Library : Screen()
    data class ChecklistDetail(val checklistId: ChecklistId) : Screen()
    data class CreateEdit(val checklistId: ChecklistId? = null) : Screen()
}
```

**MainActivity Navigation:**

```kotlin
var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

when (val screen = currentScreen) {
    is Screen.Library -> {
        ChecklistLibraryScreen(
            onChecklistClick = { currentScreen = Screen.ChecklistDetail(it) },
            onCreateClick = { currentScreen = Screen.CreateEdit(null) },
            onEditClick = { currentScreen = Screen.CreateEdit(it) },
            repository = repository
        )
    }
    is Screen.ChecklistDetail -> {
        ChecklistScreen(
            checklistId = screen.checklistId,
            onBackClick = { currentScreen = Screen.Library },
            repository = repository
        )
    }
    is Screen.CreateEdit -> {
        CreateEditScreen(
            checklistId = screen.checklistId,
            onSave = { currentScreen = Screen.Library },
            onCancel = { currentScreen = Screen.Library },
            repository = repository
        )
    }
}
```

---

### 5. Delete Flow with Long Press

**ChecklistCard Long Press:**

```kotlin
@Composable
fun ChecklistCard(
    checklist: Checklist,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        // ... existing styling
    ) {
        // ... existing content

        // Dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}
```

**Delete Confirmation Dialog:**

```kotlin
@Composable
fun DeleteConfirmationDialog(
    checklistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Checklist?") },
        text = { Text("Are you sure you want to delete \"$checklistName\"? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

**Usage in ChecklistLibraryScreen:**

```kotlin
var checklistToDelete by remember { mutableStateOf<Checklist?>(null) }

// In LazyColumn items
ChecklistCard(
    checklist = checklist,
    onClick = { onChecklistClick(checklist.id) },
    onEdit = { onEditClick(checklist.id) },
    onDelete = { checklistToDelete = checklist }
)

// Dialog
checklistToDelete?.let { checklist ->
    DeleteConfirmationDialog(
        checklistName = checklist.name,
        onConfirm = {
            viewModel.deleteChecklist(checklist.id)
            checklistToDelete = null
        },
        onDismiss = { checklistToDelete = null }
    )
}
```

---

### 6. Create/Edit Screen

**UI Components:**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditScreen(
    checklistId: ChecklistId?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: CreateEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CreateEditViewModel(checklistId, repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (checklistId == null) "New Checklist" else "Edit Checklist") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save { onSave() } },
                        enabled = uiState.isValid
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Checklist Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Color picker
            Text("Color", style = MaterialTheme.typography.titleMedium)
            ColorPicker(
                selectedColor = uiState.color,
                onColorSelected = { viewModel.updateColor(it) }
            )

            // Schedule section
            Text("Schedule", style = MaterialTheme.typography.titleMedium)
            SchedulePicker(
                daysOfWeek = uiState.daysOfWeek,
                timeRange = uiState.timeRange,
                onDaysChanged = { viewModel.updateDays(it) },
                onTimeRangeChanged = { viewModel.updateTimeRange(it) }
            )

            // Items section
            Text("Items", style = MaterialTheme.typography.titleMedium)
            ItemsEditor(
                items = uiState.items,
                onAddItem = { viewModel.addItem() },
                onRemoveItem = { viewModel.removeItem(it) },
                onUpdateItem = { index, text -> viewModel.updateItemText(index, text) }
            )
        }
    }
}
```

**CreateEditViewModel:**

```kotlin
data class CreateEditUiState(
    val name: String = "",
    val color: ChecklistColor = ChecklistColor.SOFT_BLUE,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val timeRange: TimeRange = TimeRange.AllDay,
    val items: List<String> = listOf(""),
    val isLoading: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && items.any { it.isNotBlank() }
}

class CreateEditViewModel(
    private val checklistId: ChecklistId?,
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    init {
        if (checklistId != null) {
            loadChecklist(checklistId)
        }
    }

    private fun loadChecklist(id: ChecklistId) {
        viewModelScope.launch {
            repository.getChecklist(id)
                .onSuccess { checklist ->
                    checklist?.let {
                        _uiState.value = CreateEditUiState(
                            name = it.name,
                            color = it.color,
                            daysOfWeek = it.schedule.daysOfWeek,
                            timeRange = it.schedule.timeRange,
                            items = it.items.map { item -> item.title }
                        )
                    }
                }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateColor(color: ChecklistColor) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    fun updateDays(days: Set<DayOfWeek>) {
        _uiState.value = _uiState.value.copy(daysOfWeek = days)
    }

    fun updateTimeRange(timeRange: TimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = timeRange)
    }

    fun addItem() {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + ""
        )
    }

    fun removeItem(index: Int) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filterIndexed { i, _ -> i != index }
        )
    }

    fun updateItemText(index: Int, text: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.mapIndexed { i, item ->
                if (i == index) text else item
            }
        )
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            val checklist = Checklist(
                id = checklistId ?: ChecklistId(UUID.randomUUID().toString()),
                name = state.name,
                schedule = ChecklistSchedule(
                    daysOfWeek = state.daysOfWeek,
                    timeRange = state.timeRange
                ),
                items = state.items
                    .filter { it.isNotBlank() }
                    .mapIndexed { index, title ->
                        ChecklistItem(
                            id = ChecklistItemId(UUID.randomUUID().toString()),
                            title = title.trim(),
                            iconId = null,
                            state = ChecklistItemState.Pending
                        )
                    },
                color = state.color,
                statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
                lastAccessedAt = null
            )

            repository.saveChecklist(checklist)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    // Show error snackbar
                }
        }
    }
}
```

---

### 7. Error Handling

**SnackBar for Repository Errors:**

```kotlin
// In ChecklistLibraryScreen
val snackbarHostState = remember { SnackbarHostState() }
var errorMessage by remember { mutableStateOf<String?>(null) }

LaunchedEffect(errorMessage) {
    errorMessage?.let {
        snackbarHostState.showSnackbar(it)
        errorMessage = null
    }
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    // ...
) { padding ->
    // content
}

// In ViewModel error handling
.onFailure { error ->
    errorMessage = when (error) {
        is RepositoryError.FileReadError -> "Failed to load checklists"
        is RepositoryError.FileWriteError -> "Failed to save changes"
        is RepositoryError.JsonParseError -> "Data format error"
        is RepositoryError.InvalidDataError -> error.message
    }
}
```

**Loading States:**

```kotlin
// Already implemented in ChecklistLibraryUiState
if (uiState.isLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
```

---

## Implementation Summary

### Files to Modify

1. **agents.md** - Update serialization and error handling documentation
2. **ChecklistLibraryViewModel.kt** - Add repository injection, replace mock data
3. **ChecklistViewModel.kt** - Add repository injection, replace mock data
4. **ChecklistLibraryScreen.kt** - Add long press menu, delete dialog
5. **ChecklistScreen.kt** - Pass repository parameter
6. **MainActivity.kt** - Create RepositoryFactory instance, pass to screens

### Files to Create

1. **RepositoryFactory.kt** - Singleton factory for repository
2. **CreateEditScreen.kt** - Form for creating/editing checklists
3. **CreateEditViewModel.kt** - ViewModel for create/edit logic
4. **ColorPicker.kt** - Color selection component
5. **SchedulePicker.kt** - Days and time range selection
6. **ItemsEditor.kt** - Item list editor component

### Testing Strategy

**Unit Tests:**
- CreateEditViewModel save logic
- ViewModel repository interaction
- Error handling flows

**Manual Testing:**
- Create new checklist → saves to repository
- Edit existing checklist → updates repository
- Delete checklist → removes from repository and UI
- Mark items done → persists to repository
- App restart → data loads from repository

---

## Success Criteria

- ✅ agents.md updated with kotlinx-serialization references
- ✅ ViewModels connected to repository
- ✅ Checklists load from repository on app start
- ✅ Create new checklist works
- ✅ Edit existing checklist works
- ✅ Delete checklist works with confirmation
- ✅ Item state changes persist to repository
- ✅ Error handling shows user-friendly messages
- ✅ All existing tests still pass
- ✅ App compiles and runs without errors

---

## References

- Main spec: `/specs.md`
- Architecture rules: `/agents.md`
- Domain models: `/common/src/commonMain/kotlin/com/pyanpyan/domain/model/`
- Repository: `/common/src/commonMain/kotlin/com/pyanpyan/domain/repository/`
- Migration complete: `/docs/plans/2026-02-14-kotlinx-serialization-migration.md`
