# Export/Import Checklists Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add export and import buttons to Settings screen that allow users to backup/restore all checklists as JSON files with state reset on import.

**Architecture:** Use `rememberLauncherForActivityResult` in SettingsScreen for file picker, add methods to SettingsViewModel for file I/O with state reset logic, add UI card with export/import buttons and confirmation dialog.

**Tech Stack:** Kotlin, Jetpack Compose, Activity Result Contracts, Android ContentResolver, kotlinx-serialization, kotlinx-datetime

---

## Task 1: Add Export/Import Methods to SettingsViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`

**Step 1: Add imports**

Add these imports at the top:

```kotlin
import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.util.Log
import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.repository.ChecklistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
```

**Step 2: Update ViewModel constructor**

Update to accept ChecklistRepository and Context:

```kotlin
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val checklistRepository: ChecklistRepository,
    private val context: Context
) : ViewModel() {
```

**Step 3: Add JSON serializer property**

Add after the settings StateFlow property:

```kotlin
private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}
```

**Step 4: Add exportToFile method**

```kotlin
fun exportToFile(uri: Uri) {
    viewModelScope.launch {
        checklistRepository.exportToJson()
            .onSuccess { jsonString ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Checklists exported successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Could not save file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SettingsViewModel", "Export failed", e)
                }
            }
            .onFailure { error ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
                Log.e("SettingsViewModel", "Export failed: $error")
            }
    }
}
```

**Step 5: Add importFromFile method**

```kotlin
fun importFromFile(uri: Uri, onShowConfirmation: (onConfirm: () -> Unit) -> Unit) {
    viewModelScope.launch {
        try {
            // Read JSON from file
            val jsonString = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: throw Exception("Could not read file")
            }

            // Parse and reset all item states to Pending
            val checklists = json.decodeFromString<List<Checklist>>(jsonString)
            val resetChecklists = checklists.map { checklist ->
                checklist.copy(
                    items = checklist.items.map { it.reset() }
                )
            }
            val resetJson = json.encodeToString(resetChecklists)

            // Show confirmation dialog on main thread
            withContext(Dispatchers.Main) {
                onShowConfirmation {
                    // User confirmed - proceed with import
                    viewModelScope.launch {
                        checklistRepository.importFromJson(resetJson)
                            .onSuccess {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Checklists imported successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .onFailure { error ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                                }
                                Log.e("SettingsViewModel", "Import failed: $error")
                            }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                val message = when {
                    e.message?.contains("JSON") == true || e is kotlinx.serialization.SerializationException ->
                        "Invalid file format"
                    else -> "Could not read file"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            Log.e("SettingsViewModel", "Import failed", e)
        }
    }
}
```

**Step 6: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 7: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt
git commit -m "feat: add export/import methods to SettingsViewModel

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Update SettingsScreen to Accept ChecklistRepository

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`

**Step 1: Add imports**

```kotlin
import com.pyanpyan.domain.repository.ChecklistRepository
```

**Step 2: Update SettingsScreen signature**

Add checklistRepository parameter:

```kotlin
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    repository: SettingsRepository,
    checklistRepository: ChecklistRepository,
    modifier: Modifier = Modifier
) {
```

**Step 3: Update ViewModel initialization**

Update to pass checklistRepository and context:

```kotlin
val viewModel: SettingsViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                repository,
                checklistRepository,
                context
            )
        }
    }
)
```

**Step 4: Build the project (will fail - MainActivity not updated)**

Run: `./gradlew :androidApp:assembleDebug`
Expected: FAIL - MainActivity doesn't pass checklistRepository yet

This is expected.

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt
git commit -m "feat: update SettingsScreen to accept ChecklistRepository

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Add Data Management Card UI with Activity Result Launchers

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`

**Step 1: Add imports**

```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

**Step 2: Add launcher and state after ViewModel initialization**

After `val settings by viewModel.settings.collectAsState()`, add:

```kotlin
// State for import confirmation dialog
var showImportConfirmation by remember { mutableStateOf(false) }
var pendingImportAction by remember { mutableStateOf<(() -> Unit)?>(null) }

// Export launcher
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri?.let { viewModel.exportToFile(it) }
}

// Import launcher
val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let {
        viewModel.importFromFile(it) { onConfirm ->
            pendingImportAction = onConfirm
            showImportConfirmation = true
        }
    }
}
```

**Step 3: Add Data Management Card**

Add after the Typography Card (after its closing braces):

```kotlin
Spacer(modifier = Modifier.padding(8.dp))

Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val today = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                    exportLauncher.launch("pyanpyan_checklists_$today.json")
                },
                modifier = Modifier.weight(0.5f)
            ) {
                Text("Export Checklists")
            }

            Button(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/json"))
                },
                modifier = Modifier.weight(0.5f)
            ) {
                Text("Import Checklists")
            }
        }

        Spacer(modifier = Modifier.padding(4.dp))

        Text(
            text = "Export saves all checklists as JSON. Import replaces all checklists and resets states to Pending.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

**Step 4: Add confirmation dialog**

Add after the closing brace of the main Column (that contains all Cards) but before the Scaffold closing brace:

```kotlin
// Import confirmation dialog
if (showImportConfirmation) {
    AlertDialog(
        onDismissRequest = {
            showImportConfirmation = false
            pendingImportAction = null
        },
        title = { Text("Replace All Checklists?") },
        text = {
            Text("This will delete all current checklists and replace them with imported data. All item progress will be reset. Continue?")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    pendingImportAction?.invoke()
                    showImportConfirmation = false
                    pendingImportAction = null
                }
            ) {
                Text("Replace All")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    showImportConfirmation = false
                    pendingImportAction = null
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
```

**Step 5: Build the project (still fails - MainActivity not updated)**

Run: `./gradlew :androidApp:assembleDebug`
Expected: FAIL - MainActivity still needs updating

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt
git commit -m "feat: add Data Management card with export/import UI

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Update MainActivity to Pass ChecklistRepository

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`

**Step 1: Verify checklistRepository exists**

Check that MainActivity has a checklistRepository property. It should already exist from previous features. If not, add:

```kotlin
private val checklistRepository by lazy {
    JsonChecklistRepository(File(filesDir, "checklists"))
}
```

**Step 2: Find SettingsScreen call**

Locate where SettingsScreen is called in the NavHost.

**Step 3: Update SettingsScreen call**

Add checklistRepository parameter:

Before:
```kotlin
SettingsScreen(
    onBackClick = { navController.popBackStack() },
    repository = settingsRepository
)
```

After:
```kotlin
SettingsScreen(
    onBackClick = { navController.popBackStack() },
    repository = settingsRepository,
    checklistRepository = checklistRepository
)
```

**Step 4: Build the project**

Run: `./gradlew :androidApp:assembleDebug`
Expected: SUCCESS

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat: pass checklistRepository to SettingsScreen

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Manual Testing & Verification

**Step 1: Install on device**

Run: `./gradlew :androidApp:installDebug`

**Step 2: Test export happy path**

1. Launch app
2. Go to Settings
3. Click "Export Checklists"
4. File picker opens with suggested name "pyanpyan_checklists_2026-02-18.json"
5. Choose location (e.g., Downloads)
6. Save file
7. Verify toast shows "Checklists exported successfully"
8. Open file in text editor/file manager
9. Verify it's valid JSON with checklist data

**Step 3: Test import happy path**

1. In Settings, click "Import Checklists"
2. File picker opens
3. Select the exported JSON file
4. Confirmation dialog appears: "Replace All Checklists?"
5. Click "Replace All"
6. Verify toast shows "Checklists imported successfully"
7. Go to Library screen
8. Verify checklists are loaded
9. Open a checklist
10. Verify all item states are Pending (not Done or IgnoredToday)

**Step 4: Test import cancellation**

1. Click "Import Checklists"
2. Select a JSON file
3. Confirmation dialog appears
4. Click "Cancel"
5. Verify no changes (checklists unchanged)
6. Verify no error toast

**Step 5: Test invalid JSON**

1. Create a text file with invalid JSON (e.g., "not json content")
2. Save as .json file
3. Try to import it
4. Verify error toast: "Invalid file format"
5. Verify no changes to checklists

**Step 6: Test file picker cancellation**

1. Click "Export Checklists"
2. Press back button in file picker (cancel)
3. Verify no error, returns to settings
4. Click "Import Checklists"
5. Press back button in file picker
6. Verify no error, returns to settings

**Step 7: Test state reset**

1. Mark some items as Done and some as IgnoredToday
2. Export checklists
3. Open exported JSON file
4. Verify it contains state information (Done/IgnoredToday)
5. Import the same file
6. Verify all states are now Pending

**Step 8: Document any issues**

If any issues found, document them.

---

## Completion Checklist

- [ ] SettingsViewModel has exportToFile and importFromFile methods
- [ ] ViewModel handles file I/O with ContentResolver
- [ ] ViewModel resets all item states to Pending on import
- [ ] SettingsScreen has Data Management card
- [ ] Export/Import buttons launch file pickers
- [ ] Import shows confirmation dialog before replacing data
- [ ] Toasts show success/error messages
- [ ] MainActivity passes checklistRepository to SettingsScreen
- [ ] Manual testing completed successfully
- [ ] All changes committed with proper messages

---

## Notes

**Design Document:** See `docs/plans/2026-02-18-export-import-checklists-design.md` for full design rationale.

**Key Implementation Details:**
- Uses `rememberLauncherForActivityResult` (Compose pattern, not Activity-level registration)
- Export filename includes current date: "pyanpyan_checklists_YYYY-MM-DD.json"
- Import file filter: "application/json" and "text/json" MIME types
- State reset happens before calling repository.importFromJson()
- Confirmation dialog prevents accidental data loss

**Error Handling:**
- File I/O errors show generic user-friendly messages
- JSON parse errors show "Invalid file format"
- All errors logged with Log.e() for debugging
- User cancellations are silent (no error messages)

**Trade-offs:**
- Import replaces all data (no merge option)
- No backup/restore of app settings (only checklists)
- No export format versioning (assumes current schema)
- No cloud storage integration

**Future Enhancements:**
- Export/import individual checklists
- Automatic scheduled backups
- Cloud storage integration (Google Drive, Dropbox)
- Export format versioning for schema changes
- Merge import option (vs replace all)
