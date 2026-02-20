# Optional Checklist Timer Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add optional countdown timer (1-60 minutes) to checklists that auto-starts, freezes on leave, and alerts on expiry.

**Architecture:** ViewModel-based timer with in-memory state. Timer configuration stored in Checklist model, runtime countdown managed by ChecklistViewModel using Kotlin Flow with 1-second delays. No background service needed.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.serialization, Kotlin Coroutines Flow

---

## Task 1: Update Checklist Data Model

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt`

**Step 1: Add timerDurationMinutes field to Checklist**

Add the field after `lastAccessedAt`:

```kotlin
@Serializable
data class Checklist(
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration,
    val lastAccessedAt: Instant? = null,
    val timerDurationMinutes: Int? = null  // NEW: null = no timer, 1-60 = timer enabled
)
```

**Step 2: Verify serialization**

Run build to ensure serialization still works:
```bash
./gradlew :common:build
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt
git commit -m "feat: add timerDurationMinutes field to Checklist model

Add optional timer duration configuration to Checklist.
- null = no timer
- 1-60 = timer duration in minutes

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Add Timer Configuration UI to CreateEditScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt`

**Step 1: Add TimerPicker composable**

Add this composable after the `ResetDurationPicker` composable definition (around line 400+):

```kotlin
@Composable
fun TimerPicker(
    timerEnabled: Boolean,
    timerDuration: Int,
    onTimerEnabledChange: (Boolean) -> Unit,
    onTimerDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Timer (Optional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Timer",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = timerEnabled,
                    onCheckedChange = onTimerEnabledChange
                )
            }

            if (timerEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration: $timerDuration minutes",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = timerDuration.toFloat(),
                    onValueChange = { onTimerDurationChange(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 58,  // 60 - 1 - 1 (excludes endpoints)
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Timer starts when checklist opens and alerts you if time runs out",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

**Step 2: Add TimerPicker to CreateEditScreen content**

In the `CreateEditScreen` composable, after the `ResetDurationPicker` call (around line 140), add:

```kotlin
                // Timer Picker
                TimerPicker(
                    timerEnabled = uiState.timerEnabled,
                    timerDuration = uiState.timerDuration,
                    onTimerEnabledChange = { viewModel.updateTimerEnabled(it) },
                    onTimerDurationChange = { viewModel.updateTimerDuration(it) }
                )
```

**Step 3: Build to check for compilation errors**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: Will fail because ViewModel methods don't exist yet (that's Task 3)

**Step 4: Commit UI changes**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt
git commit -m "feat: add timer configuration picker to CreateEditScreen

Add TimerPicker composable with:
- Enable/disable switch
- 1-60 minute slider
- Helper text explaining behavior

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Update CreateEditViewModel for Timer State

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditViewModel.kt`

**Step 1: Add timer fields to CreateEditUiState**

Find the `CreateEditUiState` data class and add timer fields:

```kotlin
data class CreateEditUiState(
    val name: String = "",
    val color: ChecklistColor = ChecklistColor.LAVENDER,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val timeRange: Pair<LocalTime, LocalTime> = LocalTime(9, 0) to LocalTime(17, 0),
    val statePersistence: StatePersistenceDuration = StatePersistenceDuration.UntilMidnight,
    val items: List<ItemState> = listOf(ItemState()),
    val timerEnabled: Boolean = false,  // NEW
    val timerDuration: Int = 15,  // NEW: default 15 minutes
    val isLoading: Boolean = true,
    val isValid: Boolean = false,
    val errorMessage: String? = null
)
```

**Step 2: Add timer update methods**

Add these methods to `CreateEditViewModel`:

```kotlin
fun updateTimerEnabled(enabled: Boolean) {
    _uiState.update { it.copy(timerEnabled = enabled) }
}

fun updateTimerDuration(duration: Int) {
    _uiState.update { it.copy(timerDuration = duration) }
}
```

**Step 3: Update save() method to include timer**

Find the `save()` method where it creates/updates the Checklist. Update the Checklist instantiation to include timer:

```kotlin
val checklist = Checklist(
    id = checklistId ?: ChecklistId(UUID.randomUUID().toString()),
    name = state.name.trim(),
    schedule = ChecklistSchedule(
        daysOfWeek = state.daysOfWeek,
        timeRange = state.timeRange
    ),
    items = state.items.mapIndexed { index, itemState ->
        ChecklistItem(
            id = itemState.id ?: ChecklistItemId(UUID.randomUUID().toString()),
            title = itemState.text.trim(),
            iconId = itemState.iconId,
            state = ChecklistItemState.Pending
        )
    },
    color = state.color,
    statePersistence = state.statePersistence,
    lastAccessedAt = existingChecklist?.lastAccessedAt,
    timerDurationMinutes = if (state.timerEnabled) state.timerDuration else null  // NEW
)
```

**Step 4: Update loadChecklist() to populate timer state**

In the `init` block or `loadChecklist()` method, when loading existing checklist, populate timer state:

```kotlin
_uiState.update {
    it.copy(
        name = checklist.name,
        color = checklist.color,
        daysOfWeek = checklist.schedule.daysOfWeek,
        timeRange = checklist.schedule.timeRange,
        statePersistence = checklist.statePersistence,
        items = checklist.items.map { item ->
            ItemState(
                id = item.id,
                text = item.title,
                iconId = item.iconId
            )
        },
        timerEnabled = checklist.timerDurationMinutes != null,  // NEW
        timerDuration = checklist.timerDurationMinutes ?: 15,  // NEW
        isLoading = false,
        isValid = true
    )
}
```

**Step 5: Build to verify**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 6: Commit ViewModel changes**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditViewModel.kt
git commit -m "feat: add timer state management to CreateEditViewModel

Add timer enabled/duration fields to UI state.
Update save() to persist timer configuration.
Load timer state when editing existing checklist.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Add Timer State to ChecklistViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`

**Step 1: Define TimerState sealed class**

Add this sealed class at the top of the file (after imports, before ChecklistViewModel):

```kotlin
sealed class TimerState {
    object NotConfigured : TimerState()
    data class Running(val remainingSeconds: Int) : TimerState()
    data class Paused(val remainingSeconds: Int) : TimerState()
    object Expired : TimerState()
    data class Completed(val remainingSeconds: Int) : TimerState()
}
```

**Step 2: Add timerState to ChecklistUiState**

Find `ChecklistUiState` data class and add:

```kotlin
data class ChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = true,
    val timerState: TimerState = TimerState.NotConfigured  // NEW
)
```

**Step 3: Add timerJob property to ViewModel**

In `ChecklistViewModel` class, add a private property:

```kotlin
class ChecklistViewModel(
    private val checklistId: ChecklistId,
    private val repository: ChecklistRepository,
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null  // NEW

    // ... rest of class
}
```

**Step 4: Build to verify**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit state additions**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git commit -m "feat: add timer state to ChecklistViewModel

Define TimerState sealed class with 5 states:
- NotConfigured, Running, Paused, Expired, Completed

Add timerState field to UI state and timerJob property.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Add Timer Countdown Logic to ChecklistViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`

**Step 1: Add startTimer() method**

Add this method to `ChecklistViewModel`:

```kotlin
fun startTimer(durationMinutes: Int) {
    timerJob?.cancel()  // Cancel any existing timer
    timerJob = viewModelScope.launch {
        startTimerFromSeconds(durationMinutes * 60)
    }
}

private suspend fun startTimerFromSeconds(seconds: Int) {
    var remaining = seconds
    while (remaining > 0) {
        _uiState.update { it.copy(timerState = TimerState.Running(remaining)) }
        delay(1000)  // Wait 1 second
        remaining--
    }
    // Timer expired
    soundManager.playCompletionSound()
    _uiState.update { it.copy(timerState = TimerState.Expired) }
}
```

**Step 2: Add pauseTimer() method**

```kotlin
fun pauseTimer() {
    timerJob?.cancel()
    val currentState = _uiState.value.timerState
    if (currentState is TimerState.Running) {
        _uiState.update { it.copy(timerState = TimerState.Paused(currentState.remainingSeconds)) }
    }
}
```

**Step 3: Add resumeTimer() method**

```kotlin
fun resumeTimer() {
    val currentState = _uiState.value.timerState
    if (currentState is TimerState.Paused) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            startTimerFromSeconds(currentState.remainingSeconds)
        }
    }
}
```

**Step 4: Add dismissTimer() method**

```kotlin
fun dismissTimer() {
    timerJob?.cancel()
    _uiState.update { it.copy(timerState = TimerState.NotConfigured) }
}
```

**Step 5: Add stopTimerAsCompleted() method**

```kotlin
private fun stopTimerAsCompleted() {
    timerJob?.cancel()
    val currentState = _uiState.value.timerState
    if (currentState is TimerState.Running) {
        _uiState.update { it.copy(timerState = TimerState.Completed(currentState.remainingSeconds)) }
    }
}
```

**Step 6: Update updateItemState() to check for completion**

Find the `updateItemState()` method and add completion check. After updating the checklist, add:

```kotlin
fun updateItemState(itemId: ChecklistItemId, newState: ChecklistItemState) {
    viewModelScope.launch {
        val checklist = _uiState.value.checklist ?: return@launch
        val item = checklist.findItem(itemId) ?: return@launch
        val updatedItem = item.copy(state = newState)
        val updatedChecklist = checklist.updateItem(updatedItem)

        repository.saveChecklist(updatedChecklist)
            .onSuccess {
                _uiState.update { it.copy(checklist = updatedChecklist) }

                // NEW: Check if checklist is completed
                val allDone = updatedChecklist.items.all {
                    it.state != ChecklistItemState.Pending
                }
                if (allDone) {
                    stopTimerAsCompleted()
                }
            }
            .onFailure { error ->
                Log.e("ChecklistViewModel", "Failed to update item: $error")
            }
    }
}
```

**Step 7: Build to verify**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 8: Commit timer logic**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git commit -m "feat: add timer countdown logic to ChecklistViewModel

Implement timer methods:
- startTimer(): Begin countdown from duration
- pauseTimer(): Freeze timer when leaving
- resumeTimer(): Continue from paused state
- dismissTimer(): Hide timer after expiry
- stopTimerAsCompleted(): Stop when checklist done

Auto-stop timer when all items completed.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: Add Timer Display UI to ChecklistScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`

**Step 1: Add TimerCard composable**

Add this composable after the existing composable functions (around line 150+):

```kotlin
@Composable
fun TimerCard(
    timerState: TimerState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (timerState) {
        is TimerState.Running -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Timer: ${formatTime(timerState.remainingSeconds)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        is TimerState.Expired -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Timer expired!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
        is TimerState.Completed -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Completed with ${formatTime(timerState.remainingSeconds)} remaining",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        else -> {
            // NotConfigured or Paused - don't show card
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
```

**Step 2: Add timer lifecycle logic to ChecklistScreen**

In the `ChecklistScreen` composable, after the `uiState` collection, add:

```kotlin
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsRepository.settings.collectAsState(initial = com.pyanpyan.domain.model.AppSettings())

    // NEW: Timer lifecycle management
    LaunchedEffect(uiState.checklist?.id) {
        val checklist = uiState.checklist
        val timerDuration = checklist?.timerDurationMinutes

        if (timerDuration != null) {
            val currentState = uiState.timerState
            if (currentState is TimerState.Paused) {
                viewModel.resumeTimer()
            } else if (currentState is TimerState.NotConfigured) {
                viewModel.startTimer(timerDuration)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pauseTimer()
        }
    }
```

**Step 3: Add TimerCard to ChecklistScreen layout**

Inside the `Column` after the checklist name `Text`, add (around line 90):

```kotlin
                    Text(
                        text = checklist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // NEW: Timer display
                    if (uiState.timerState !is TimerState.NotConfigured &&
                        uiState.timerState !is TimerState.Paused) {
                        TimerCard(
                            timerState = uiState.timerState,
                            onDismiss = { viewModel.dismissTimer() },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
```

**Step 4: Build to verify**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 5: Build APK and install**

```bash
./gradlew assembleDebug
~/Library/Android/sdk/platform-tools/adb -s adb-38291FDJH000K2-CAFRxL._adb-tls-connect._tcp install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```
Expected: Success

**Step 6: Commit timer UI**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git commit -m "feat: add timer display UI to ChecklistScreen

Add TimerCard composable with three states:
- Running: Shows countdown (MM:SS)
- Expired: Shows alert with dismiss button
- Completed: Shows remaining time

Add timer lifecycle management:
- Auto-start on screen open
- Pause on screen leave
- Resume on screen return

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: Manual Testing and Verification

**Step 1: Test timer creation**

1. Open app on device/emulator
2. Create new checklist with "Enable Timer" switched ON
3. Set duration to 1 minute
4. Save checklist
5. Verify timer starts counting down immediately

**Step 2: Test pause/resume**

1. With timer running at ~30 seconds
2. Press back to return to library
3. Immediately reopen checklist
4. Verify timer resumes from ~30 seconds (not reset)

**Step 3: Test expiry**

1. Create checklist with 1 minute timer
2. Wait for timer to reach 00:00
3. Verify completion sound plays
4. Verify red "Timer expired!" alert shows
5. Tap "Dismiss" button
6. Verify alert disappears

**Step 4: Test completion with time remaining**

1. Create checklist with 2 minute timer and 2 items
2. Mark both items as done before timer expires
3. Verify timer stops
4. Verify green "Completed with X:XX remaining" shows

**Step 5: Test without timer**

1. Create checklist with "Enable Timer" switched OFF
2. Save and open checklist
3. Verify no timer display appears

**Step 6: Test edit timer configuration**

1. Create checklist with timer enabled (15 min)
2. Edit checklist, disable timer
3. Save and open checklist
4. Verify no timer appears
5. Edit again, enable timer (30 min)
6. Save and open checklist
7. Verify 30 minute timer starts

**Step 7: Document test results**

Create a test verification checklist in your commit message if all tests pass:

```bash
git commit --allow-empty -m "test: verify timer feature manual testing

All manual tests passed:
✅ Timer creation and auto-start
✅ Pause on leave, resume on return
✅ Expiry alert with sound
✅ Completion with remaining time
✅ No timer when disabled
✅ Edit timer configuration persists

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: Final Integration and Cleanup

**Step 1: Review all changes**

```bash
git log --oneline -10
git diff HEAD~7 HEAD --stat
```

**Step 2: Run full build**

```bash
./gradlew clean build
```
Expected: BUILD SUCCESSFUL

**Step 3: Test on physical device**

Install on Pixel 8:
```bash
~/Library/Android/sdk/platform-tools/adb -s adb-38291FDJH000K2-CAFRxL._adb-tls-connect._tcp install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Perform smoke tests:
- Create checklist with timer
- Verify countdown works
- Test pause/resume
- Test expiry behavior

**Step 4: Final commit**

If everything works, create final integration commit:

```bash
git commit --allow-empty -m "feat: complete timer feature integration

Timer feature fully functional:
- Optional 1-60 minute countdown timer
- Configuration in CreateEdit screen
- Auto-start on checklist open
- Pause/resume on leave/return
- Alert on expiry with completion sound
- Shows remaining time on early completion

Total changes:
- 1 field added to Checklist model
- ~150 lines in CreateEditScreen (UI + picker)
- ~80 lines in CreateEditViewModel (state)
- ~120 lines in ChecklistViewModel (logic)
- ~150 lines in ChecklistScreen (UI + lifecycle)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Success Criteria

✅ Timer can be enabled/disabled in CreateEdit screen
✅ Timer duration configurable from 1-60 minutes
✅ Timer auto-starts when opening checklist
✅ Countdown updates every second (MM:SS format)
✅ Timer pauses when leaving checklist
✅ Timer resumes from same time when returning
✅ Completion sound plays when timer expires
✅ Alert shows with dismiss button on expiry
✅ Timer stops when checklist completes
✅ Shows "Completed with X:XX remaining" message
✅ Configuration persists across app restarts
✅ No crashes, no memory leaks
✅ Works with screen rotation

## Known Limitations

- Timer state lost if app is killed (by design)
- Timer only runs when screen is active (no background)
- Countdown may drift slightly (~1-2 seconds over long periods)
- No timer history or statistics
- Uses completion sound (no separate timer sound)

## Future Enhancements

Not in scope for this implementation:
- Background timer with notifications
- Multiple timers per checklist
- Timer presets (quick 5/10/15 min buttons)
- Visual progress indicator (circular)
- Custom timer sounds
- Voice announcements
- Timer statistics/history
