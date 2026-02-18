# Export/Import Checklists Design

## Overview

Add export and import functionality to Settings screen, allowing users to backup and restore all checklists as a single JSON file. Item states are reset to Pending on import.

## User Requirements

- **Export:** Save all checklists to a JSON file using Android file picker
- **Import:** Load checklists from a JSON file, replacing current data
- **State Reset:** All item states (Done/IgnoredToday) reset to Pending on import
- **File Format:** Single JSON file with suggested name "pyanpyan_checklists_[date].json"
- **Confirmation:** Show warning dialog before replacing data on import

## Architecture Overview

### Components

1. **MainActivity** - Register Activity Result contracts for file picker
2. **SettingsScreen** - Add "Data Management" Card with Export/Import buttons
3. **SettingsViewModel** - Add export/import methods that coordinate with repository
4. **ChecklistRepository** - Already has exportToJson() and importFromJson() methods

### Activity Result API

MainActivity registers two contracts:
- `ActivityResultContracts.CreateDocument("application/json")` for export
- `ActivityResultContracts.OpenDocument()` for import with JSON mime type filter

These launchers are passed to SettingsScreen as callbacks.

### Data Flow

**Export Flow:**
```
User clicks "Export Checklists"
    ↓
MainActivity launcher opens file picker (CREATE_DOCUMENT)
    ↓
User selects location and filename
    ↓
MainActivity receives URI → calls ViewModel.exportToFile(uri)
    ↓
ViewModel calls repository.exportToJson()
    ↓
Write JSON string to URI using ContentResolver
    ↓
Show success toast or error message
```

**Import Flow:**
```
User clicks "Import Checklists"
    ↓
MainActivity launcher opens file picker (OPEN_DOCUMENT)
    ↓
User selects JSON file
    ↓
MainActivity receives URI → calls ViewModel.importFromFile(uri)
    ↓
Read JSON string from URI using ContentResolver
    ↓
Show confirmation dialog: "Replace All Checklists?"
    ↓
User confirms
    ↓
Parse JSON to List<Checklist>
    ↓
Reset all item states: items.map { it.reset() }
    ↓
Re-serialize to JSON and call repository.importFromJson()
    ↓
Show success toast or error message
```

## Design Details

### 1. UI Components

**New Card in SettingsScreen:**

Added after the Typography Card:

```
┌─ Data Management Card ──────────────────┐
│ Data Management                          │
│                                          │
│ [Export Checklists] [Import Checklists] │
│                                          │
│ Export saves all checklists as JSON     │
│ Import replaces all checklists          │
└──────────────────────────────────────────┘
```

**Button Layout:**
- Two buttons in a Row with equal weight (0.5f each)
- Spacing: 8dp between buttons
- Padding: 16dp inside card
- Helper text below buttons explaining behavior

**Import Confirmation Dialog:**
```kotlin
AlertDialog(
    title = "Replace All Checklists?",
    text = "This will delete all current checklists and replace them with imported data. All progress will be lost. Continue?",
    confirmButton = { TextButton("Replace All") },
    dismissButton = { TextButton("Cancel") }
)
```

### 2. File Operations

**Export Filename:**
Format: `pyanpyan_checklists_YYYY-MM-DD.json`
Example: `pyanpyan_checklists_2026-02-18.json`

**Import File Filter:**
- MIME types: `application/json`, `text/json`
- File picker shows only JSON files

**ContentResolver Usage:**
```kotlin
// Export
context.contentResolver.openOutputStream(uri)?.use { outputStream ->
    outputStream.write(jsonString.toByteArray())
}

// Import
context.contentResolver.openInputStream(uri)?.use { inputStream ->
    val jsonString = inputStream.bufferedReader().readText()
}
```

### 3. State Reset Logic

The `ChecklistItem` model already has a `reset()` method:
```kotlin
fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
```

On import, before calling `repository.importFromJson()`:
```kotlin
val checklists = Json.decodeFromString<List<Checklist>>(jsonString)
val resetChecklists = checklists.map { checklist ->
    checklist.copy(
        items = checklist.items.map { it.reset() }
    )
}
val resetJson = Json.encodeToString(resetChecklists)
repository.importFromJson(resetJson)
```

### 4. Error Handling

**Error Types:**
- **File Read Error:** "Could not read file"
- **File Write Error:** "Could not save file"
- **JSON Parse Error:** "Invalid file format"
- **Permission Denied:** Handled by Activity Result API (no error shown)
- **User Cancels:** No action, no error message

**Error Display:**
All errors shown as Toast messages at bottom of screen, 3 second duration.

**Success Messages:**
- Export: "Checklists exported successfully"
- Import: "Checklists imported successfully"

### 5. Integration Points

**MainActivity Changes:**
```kotlin
// Register contracts
val exportLauncher = registerForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri?.let { viewModel.exportToFile(it) }
}

val importLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { viewModel.importFromFile(it) }
}

// Pass to SettingsScreen
SettingsScreen(
    onBackClick = { ... },
    repository = settingsRepository,
    onExportClick = { exportLauncher.launch("pyanpyan_checklists_${currentDate}.json") },
    onImportClick = { importLauncher.launch(arrayOf("application/json", "text/json")) }
)
```

**ViewModel Changes:**
```kotlin
fun exportToFile(uri: Uri) {
    viewModelScope.launch {
        repository.exportToJson()
            .onSuccess { jsonString ->
                // Write to URI using ContentResolver
                // Show success toast
            }
            .onFailure { error ->
                // Show error toast
            }
    }
}

fun importFromFile(uri: Uri) {
    viewModelScope.launch {
        try {
            // Read from URI
            val jsonString = readFromUri(uri)
            // Parse, reset states, re-serialize
            val resetJson = resetItemStates(jsonString)
            // Show confirmation dialog
            showConfirmDialog {
                repository.importFromJson(resetJson)
                    .onSuccess { /* Show success toast */ }
                    .onFailure { /* Show error toast */ }
            }
        } catch (e: Exception) {
            // Show error toast
        }
    }
}
```

## Edge Cases & Testing

### Manual Testing Checklist

1. **Export happy path:**
   - Export → choose location → verify file exists → open in text editor → verify JSON format

2. **Import happy path:**
   - Export first → modify file → import → confirm dialog → verify data loaded → check states are Pending

3. **Import cancellation:**
   - Click Import → select file → click Cancel in confirmation → verify no changes

4. **Invalid JSON:**
   - Create text file with invalid JSON → import → verify error shown

5. **Empty export:**
   - Delete all checklists → export → verify "[]" JSON created

6. **File picker cancellation:**
   - Click Export → press back → verify no error
   - Click Import → press back → verify no error

7. **Large file:**
   - Export with many checklists → verify no lag or timeout

8. **Special characters:**
   - Checklist names with emojis/unicode → export → import → verify preserved

### Edge Cases

**Handled:**
- **No storage permission:** Activity Result API handles automatically
- **File not found:** Error toast shown
- **Malformed JSON:** Parse error caught, error toast shown
- **Empty checklists:** Exports as empty JSON array "[]"
- **User cancels:** No error, graceful return to settings

**Intentional Behavior:**
- **Import replaces all data:** Confirmed by dialog warning
- **States always reset:** No option to preserve states (per requirement)
- **lastAccessedAt preserved:** Not reset on import (only item states reset)

### Data Preservation

**Preserved on Export/Import:**
- Checklist ID, name, schedule, items, color, statePersistence
- Item ID, title, iconId
- lastAccessedAt timestamp

**Reset on Import:**
- All item states → Pending
- (Everything else preserved)

## Implementation Complexity

**Estimated effort:** Medium

**Files to modify:** 3 files
- MainActivity.kt (register Activity Result contracts, pass launchers)
- SettingsScreen.kt (add Data Management card with buttons)
- SettingsViewModel.kt (add export/import file operations)

**Files to read:** 1 file
- ChecklistRepository.kt (already has exportToJson/importFromJson)

**Total lines of code:** ~150 new lines

**Risk level:** Low-Medium
- Activity Result API is standard Android pattern
- Repository methods already exist
- Main risk is file I/O errors (handled with try-catch)
- State reset logic is straightforward

**Dependencies:**
- kotlinx-serialization (already in project)
- kotlinx-datetime (for export filename timestamp)
- Android Activity Result API (built-in)

## Future Enhancements

**Not in scope for this feature:**
- Export/import individual checklists (only all at once)
- Automatic backup to cloud storage
- Merge import (currently replaces all data)
- Export format versioning
- Scheduled auto-exports
- Export statistics or analytics data
