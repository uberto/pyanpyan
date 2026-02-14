# Repository Wiring and CRUD Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Connect ViewModels to repository and implement Create/Edit/Delete UI flows for complete checklist CRUD functionality.

**Architecture:** Update documentation to reflect kotlinx-serialization migration, wire existing ViewModels to repository using RepositoryFactory pattern, implement Create/Edit screen with form components, add long-press menu for delete with confirmation dialog.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx-serialization, kotlinx-coroutines, Android ViewModel

---

## Task 1: Update agents.md - JSON Serialization Section

**Files:**
- Modify: `agents.md:175-196`

**Step 1: Read current agents.md section**

Read lines 175-196 to see current kondor-json documentation.

**Step 2: Replace JSON serialization section**

Replace lines 175-196 with:

```markdown
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
```

**Step 3: Commit**

```bash
git add agents.md
git commit -m "docs: update JSON serialization section to kotlinx-serialization"
```

---

## Task 2: Update agents.md - Error Handling Section

**Files:**
- Modify: `agents.md:199-234`

**Step 1: Replace error handling section**

Replace lines 199-234 with:

```markdown
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
```

**Step 2: Commit**

```bash
git add agents.md
git commit -m "docs: update error handling section to RepositoryResult"
```

---

## Task 3: Update agents.md - Library Requirements Section

**Files:**
- Modify: `agents.md:264-276`

**Step 1: Replace library requirements section**

Replace lines 264-276 with:

```markdown
### 10.1 Mandatory Libraries

* **kotlinx-serialization-json** - JSON serialization (https://github.com/Kotlin/kotlinx.serialization)
* **kotlinx-datetime** - Date/time handling (multiplatform)
* **kotlinx-coroutines** - Async/concurrency

### 10.2 Prohibited Libraries

Do not use without explicit approval:
* Gson, Moshi, Jackson (use kotlinx-serialization instead)
* Java Date/Time APIs (use kotlinx-datetime instead)
```

**Step 2: Commit**

```bash
git add agents.md
git commit -m "docs: update library requirements to remove kondor"
```

---

## Task 4: Add Working Constraints Section to agents.md

**Files:**
- Modify: `agents.md` (after line 141, in Section 8)

**Step 1: Add new subsection after line 141**

Insert after "### 8.2 Specification Source of Truth":

```markdown
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
```

**Step 2: Commit**

```bash
git add agents.md
git commit -m "docs: add directory and command constraints to working process"
```

---

## Task 5: Create RepositoryFactory

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/data/RepositoryFactory.kt`

**Step 1: Create directory if needed**

Run: `mkdir -p androidApp/src/main/kotlin/com/pyanpyan/android/data`

**Step 2: Create RepositoryFactory.kt**

```kotlin
package com.pyanpyan.android.data

import android.content.Context
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.JsonChecklistRepository

object RepositoryFactory {
    @Volatile
    private var repository: ChecklistRepository? = null

    fun getRepository(context: Context): ChecklistRepository =
        repository ?: synchronized(this) {
            repository ?: JsonChecklistRepository(context.filesDir).also {
                repository = it
            }
        }
}
```

**Step 3: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/data/RepositoryFactory.kt
git commit -m "feat(android): add RepositoryFactory for singleton repository access"
```

---

## Task 6: Update ChecklistLibraryViewModel - Add Repository Constructor

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt:22-30`

**Step 1: Update ViewModel constructor**

Replace lines 22-30:

```kotlin
class ChecklistLibraryViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistLibraryUiState())
    val uiState: StateFlow<ChecklistLibraryUiState> = _uiState.asStateFlow()

    init {
        loadChecklists()
    }
```

**Step 2: Replace loadChecklists() implementation**

Replace lines 31-89 with:

```kotlin
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
                    // Error will be shown via SnackBar in UI
                }
        }
    }
```

**Step 3: Remove createMockChecklist function**

Delete lines 91-114 (the createMockChecklist function - no longer needed).

**Step 4: Update deleteChecklist implementation**

Replace lines 116-121 with:

```kotlin
    fun deleteChecklist(checklistId: ChecklistId) {
        viewModelScope.launch {
            repository.deleteChecklist(checklistId)
                .onSuccess { loadChecklists() }
                .onFailure { error ->
                    // Error will be shown via SnackBar in UI
                }
        }
    }
```

**Step 5: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt
git commit -m "feat(android): wire ChecklistLibraryViewModel to repository"
```

---

## Task 7: Update ChecklistViewModel - Add Repository Constructor

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt:18-58`

**Step 1: Update ViewModel constructor**

Replace lines 18-26 with:

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
```

**Step 2: Replace loadChecklist() implementation**

Replace lines 27-58 with:

```kotlin
    private fun loadChecklist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getChecklist(checklistId)
                .onSuccess { checklist ->
                    _uiState.value = ChecklistUiState(
                        checklist = checklist,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Error will be shown via SnackBar in UI
                }
        }
    }
```

**Step 3: Update markItemDone to persist changes**

Replace lines 60-68 with:

```kotlin
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
```

**Step 4: Update ignoreItemToday to persist changes**

Replace lines 71-80 with:

```kotlin
    fun ignoreItemToday(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = IgnoreItemToday(itemId)
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
```

**Step 5: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git commit -m "feat(android): wire ChecklistViewModel to repository with persistence"
```

---

## Task 8: Update ChecklistLibraryScreen - Add Repository Parameter

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt:25-31`

**Step 1: Add repository parameter and viewModel factory**

Replace lines 25-31 with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistLibraryScreen(
    onChecklistClick: (ChecklistId) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (ChecklistId) -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: ChecklistLibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ChecklistLibraryViewModel(repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
```

**Step 2: Add imports at top of file**

Add these imports after the existing imports (around line 21):

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.pyanpyan.domain.repository.ChecklistRepository
```

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat(android): add repository injection to ChecklistLibraryScreen"
```

---

## Task 9: Update ChecklistScreen - Add Repository Parameter

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt:24-31`

**Step 1: Add repository parameter and viewModel factory**

Replace lines 24-31 with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    checklistId: ChecklistId,
    onBackClick: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: ChecklistViewModel = viewModel(
        key = checklistId.value,
        factory = viewModelFactory {
            initializer {
                ChecklistViewModel(checklistId, repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
```

**Step 2: Add imports at top of file**

Add these imports after the existing imports (around line 17):

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.pyanpyan.domain.repository.ChecklistRepository
```

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git commit -m "feat(android): add repository injection to ChecklistScreen"
```

---

## Task 10: Add Long-Press Menu to ChecklistCard

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt:113-182`

**Step 1: Update ChecklistCard function signature**

Replace line 113-119 with:

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
```

**Step 2: Add combinedClickable import**

Add after existing imports (around line 10):

```kotlin
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
```

**Step 3: Update Card with combinedClickable**

Replace line 120-123 with:

```kotlin
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(
```

**Step 4: Add DropdownMenu inside Card (after Row closes)**

Add before the closing brace of Card (around line 180):

```kotlin
            }

            // Dropdown menu for long-press
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
}
```

**Step 5: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat(android): add long-press menu to ChecklistCard for edit/delete"
```

---

## Task 11: Add Delete Confirmation Dialog to ChecklistLibraryScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt:25-110`

**Step 1: Add state for dialog in ChecklistLibraryScreen function**

Add after `val uiState by viewModel.uiState.collectAsState()` (around line 41):

```kotlin
    var checklistToDelete by remember { mutableStateOf<Checklist?>(null) }
```

**Step 2: Update ChecklistCard calls in LazyColumn**

Replace the ChecklistCard calls (around lines 62-67 and 84-89) with:

```kotlin
            // Active checklists
            if (uiState.activeChecklists.isNotEmpty()) {
                items(uiState.activeChecklists) { checklist ->
                    ChecklistCard(
                        checklist = checklist,
                        isActive = true,
                        onClick = { onChecklistClick(checklist.id) },
                        onEdit = { onEditClick(checklist.id) },
                        onDelete = { checklistToDelete = checklist }
                    )
                }
            }

            // Separator if both sections present
            if (uiState.activeChecklists.isNotEmpty() && uiState.inactiveChecklists.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // Inactive checklists
            if (uiState.inactiveChecklists.isNotEmpty()) {
                items(uiState.inactiveChecklists) { checklist ->
                    ChecklistCard(
                        checklist = checklist,
                        isActive = false,
                        onClick = { onChecklistClick(checklist.id) },
                        onEdit = { onEditClick(checklist.id) },
                        onDelete = { checklistToDelete = checklist }
                    )
                }
            }
```

**Step 3: Add DeleteConfirmationDialog after LazyColumn**

Add before the closing brace of Scaffold content (around line 109):

```kotlin
        }

        // Delete confirmation dialog
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
    }
}
```

**Step 4: Add DeleteConfirmationDialog composable function**

Add at the end of the file (after getScheduleDescription function):

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
        text = {
            Text("Are you sure you want to delete \"$checklistName\"? This cannot be undone.")
        },
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

**Step 5: Add AlertDialog import**

Add after existing imports:

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
```

**Step 6: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat(android): add delete confirmation dialog to ChecklistLibraryScreen"
```

---

## Task 12: Update MainActivity Navigation - Add Screen.CreateEdit

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt:15-18`

**Step 1: Update Screen sealed class**

Replace lines 15-18 with:

```kotlin
sealed class Screen {
    object Library : Screen()
    data class ChecklistDetail(val checklistId: ChecklistId) : Screen()
    data class CreateEdit(val checklistId: ChecklistId? = null) : Screen()
}
```

**Step 2: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat(android): add CreateEdit screen to navigation"
```

---

## Task 13: Update MainActivity - Wire Repository to Screens

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt:20-50`

**Step 1: Add repository creation in onCreate**

Replace lines 20-50 with:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = com.pyanpyan.android.data.RepositoryFactory.getRepository(applicationContext)

        setContent {
            PyanpyanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

                    when (val screen = currentScreen) {
                        is Screen.Library -> {
                            ChecklistLibraryScreen(
                                onChecklistClick = { checklistId ->
                                    currentScreen = Screen.ChecklistDetail(checklistId)
                                },
                                onCreateClick = {
                                    currentScreen = Screen.CreateEdit(null)
                                },
                                onEditClick = { checklistId ->
                                    currentScreen = Screen.CreateEdit(checklistId)
                                },
                                repository = repository
                            )
                        }
                        is Screen.ChecklistDetail -> {
                            ChecklistScreen(
                                checklistId = screen.checklistId,
                                onBackClick = {
                                    currentScreen = Screen.Library
                                },
                                repository = repository
                            )
                        }
                        is Screen.CreateEdit -> {
                            // TODO: Implement CreateEditScreen in next tasks
                            Text("Create/Edit Screen - TODO")
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Verify it compiles and runs**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat(android): wire repository to all screens in MainActivity"
```

---

## Task 14: Create CreateEditViewModel

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditViewModel.kt`

**Step 1: Create directory**

Run: `mkdir -p androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit`

**Step 2: Create CreateEditViewModel.kt**

```kotlin
package com.pyanpyan.android.ui.createedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import java.util.UUID

data class CreateEditUiState(
    val name: String = "",
    val color: ChecklistColor = ChecklistColor.SOFT_BLUE,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val timeRange: TimeRange = TimeRange.AllDay,
    val items: List<String> = listOf(""),
    val statePersistence: StatePersistenceDuration = StatePersistenceDuration.FIFTEEN_MINUTES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getChecklist(id)
                .onSuccess { checklist ->
                    checklist?.let {
                        _uiState.value = CreateEditUiState(
                            name = it.name,
                            color = it.color,
                            daysOfWeek = it.schedule.daysOfWeek,
                            timeRange = it.schedule.timeRange,
                            items = it.items.map { item -> item.title },
                            statePersistence = it.statePersistence,
                            isLoading = false
                        )
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Checklist not found"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load checklist"
                    )
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

    fun updateStatePersistence(duration: StatePersistenceDuration) {
        _uiState.value = _uiState.value.copy(statePersistence = duration)
    }

    fun addItem() {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + ""
        )
    }

    fun removeItem(index: Int) {
        if (_uiState.value.items.size > 1) {
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.filterIndexed { i, _ -> i != index }
            )
        }
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
                name = state.name.trim(),
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
                statePersistence = state.statePersistence,
                lastAccessedAt = null
            )

            repository.saveChecklist(checklist)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to save checklist"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
```

**Step 3: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditViewModel.kt
git commit -m "feat(android): add CreateEditViewModel for checklist form"
```

---

## Task 15: Create CreateEditScreen Basic Layout

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt`

**Step 1: Create CreateEditScreen.kt with basic structure**

```kotlin
package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.repository.ChecklistRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditScreen(
    checklistId: ChecklistId?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: CreateEditViewModel = viewModel(
        key = checklistId?.value ?: "new",
        factory = viewModelFactory {
            initializer {
                CreateEditViewModel(checklistId, repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (checklistId == null) "New Checklist" else "Edit Checklist")
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save { onSave() } },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color picker placeholder
                Text(
                    text = "Color: ${uiState.color.displayName}",
                    style = MaterialTheme.typography.titleMedium
                )

                // Items section placeholder
                Text(
                    text = "Items: ${uiState.items.filter { it.isNotBlank() }.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
```

**Step 2: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt
git commit -m "feat(android): add CreateEditScreen basic layout with name field"
```

---

## Task 16: Add ColorPicker Component to CreateEditScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt`

**Step 1: Add ColorPicker composable at end of file**

```kotlin
@Composable
fun ColorPicker(
    selectedColor: com.pyanpyan.domain.model.ChecklistColor,
    onColorSelected: (com.pyanpyan.domain.model.ChecklistColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Color",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.pyanpyan.domain.model.ChecklistColor.values().chunked(4).forEach { rowColors ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowColors.forEach { color ->
                        ColorOption(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { onColorSelected(color) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: com.pyanpyan.domain.model.ChecklistColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = androidx.compose.ui.graphics.Color(
            android.graphics.Color.parseColor(color.hex)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                3.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        if (isSelected) {
            Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}
```

**Step 2: Add necessary imports**

Add after existing imports:

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
```

**Step 3: Replace color placeholder in CreateEditScreen**

Replace the "Color: ${uiState.color.displayName}" section with:

```kotlin
                // Color picker
                ColorPicker(
                    selectedColor = uiState.color,
                    onColorSelected = { viewModel.updateColor(it) }
                )
```

**Step 4: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt
git commit -m "feat(android): add ColorPicker component to CreateEditScreen"
```

---

## Task 17: Add ItemsEditor Component to CreateEditScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt`

**Step 1: Add ItemsEditor composable at end of file**

```kotlin
@Composable
fun ItemsEditor(
    items: List<String>,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateItem: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Items",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onAddItem) {
                Text("+ Add Item")
            }
        }

        items.forEachIndexed { index, itemText ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = itemText,
                    onValueChange = { onUpdateItem(index, it) },
                    label = { Text("Item ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                if (items.size > 1) {
                    IconButton(onClick = { onRemoveItem(index) }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Remove item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
```

**Step 2: Add Delete icon import**

Add after existing imports:

```kotlin
import androidx.compose.material.icons.filled.Delete
```

**Step 3: Replace items placeholder in CreateEditScreen**

Replace the "Items: ${uiState.items.filter...}" section with:

```kotlin
                // Items editor
                ItemsEditor(
                    items = uiState.items,
                    onAddItem = { viewModel.addItem() },
                    onRemoveItem = { viewModel.removeItem(it) },
                    onUpdateItem = { index, text -> viewModel.updateItemText(index, text) }
                )
```

**Step 4: Verify it compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt
git commit -m "feat(android): add ItemsEditor component to CreateEditScreen"
```

---

## Task 18: Wire CreateEditScreen to MainActivity

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`

**Step 1: Add CreateEditScreen import**

Add after existing imports:

```kotlin
import com.pyanpyan.android.ui.createedit.CreateEditScreen
```

**Step 2: Replace CreateEdit placeholder in MainActivity**

Replace the CreateEdit branch (around line 44-46) with:

```kotlin
                        is Screen.CreateEdit -> {
                            CreateEditScreen(
                                checklistId = screen.checklistId,
                                onSave = {
                                    currentScreen = Screen.Library
                                },
                                onCancel = {
                                    currentScreen = Screen.Library
                                },
                                repository = repository
                            )
                        }
```

**Step 3: Verify it compiles and runs**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Test create flow manually**

Run the app:
1. Tap FAB on library screen
2. Should open create screen
3. Enter checklist name and items
4. Tap Save
5. Should return to library with new checklist

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat(android): wire CreateEditScreen to MainActivity navigation"
```

---

## Task 19: Run Full Test Suite

**Files:**
- None (verification task)

**Step 1: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS (104 tests including 21 serialization tests)

**Step 2: Run Android app build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Manual testing checklist**

Test the following flows:
1. ✅ App launches and loads checklists from repository (default "School" checklist)
2. ✅ Create new checklist → saves to repository
3. ✅ Tap checklist → opens detail view
4. ✅ Mark item done → persists to repository
5. ✅ Long press checklist → shows Edit/Delete menu
6. ✅ Edit checklist → updates repository
7. ✅ Delete checklist → confirms and removes from repository
8. ✅ Restart app → data persists

**Step 4: Document any issues found**

If any issues found, fix them before final commit.

---

## Task 20: Final Verification and Summary Commit

**Files:**
- None (verification task)

**Step 1: Verify working tree is clean**

Run: `git status`
Expected: Clean working tree or only untracked files

**Step 2: Review all commits**

Run: `git log --oneline --since="1 day ago"`
Expected: See all commits from this implementation

**Step 3: Run clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 4: Create summary if needed**

If everything works, the implementation is complete. The following features are now working:

**✅ Documentation:**
- agents.md updated to reflect kotlinx-serialization
- agents.md updated with RepositoryResult error handling
- agents.md updated with working constraints

**✅ Repository Wiring:**
- RepositoryFactory created for singleton repository access
- ChecklistLibraryViewModel wired to repository
- ChecklistViewModel wired to repository with persistence
- All screens receive repository via parameters

**✅ CRUD Operations:**
- Create new checklist with form
- Read checklists from repository (loads on app start)
- Update checklist (edit form)
- Delete checklist (long-press menu with confirmation)
- Item state changes persist to repository

**✅ UI Flows:**
- Long-press menu on checklist cards
- Delete confirmation dialog
- Create/Edit screen with:
  - Name input
  - Color picker (8 colors)
  - Items editor (add/remove/edit)
  - Save/Cancel actions
- Navigation between Library, Detail, and CreateEdit screens

---

## Success Criteria Checklist

- ✅ agents.md updated with kotlinx-serialization references
- ✅ ViewModels connected to repository
- ✅ Checklists load from repository on app start
- ✅ Create new checklist works
- ✅ Edit existing checklist works
- ✅ Delete checklist works with confirmation
- ✅ Item state changes persist to repository
- ✅ Error handling shows user-friendly messages (via SnackBar)
- ✅ All existing tests still pass
- ✅ App compiles and runs without errors

---

## References

- Design doc: `docs/plans/2026-02-15-repository-wiring-crud-design.md`
- Main spec: `specs.md`
- Architecture rules: `agents.md`
- Domain models: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/`
- Repository: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/`
