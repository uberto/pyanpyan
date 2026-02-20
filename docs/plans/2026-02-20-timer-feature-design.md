# Optional Checklist Timer Design

## Goal

Add an optional countdown timer to checklists that:
- Can be configured from 1 minute to 1 hour
- Starts when the checklist opens
- Alerts the user when time runs out
- Freezes when leaving the checklist
- Shows completion time if checklist finishes before timer expires

## User Requirements

- **Optional:** Timer is opt-in, not required for all checklists
- **Configurable:** Set duration from 1 to 60 minutes during checklist creation/editing
- **Auto-start:** Timer begins countdown as soon as checklist screen opens
- **Alert on expiry:** Play sound and show visual alert when timer reaches zero
- **Pause on leave:** Timer freezes when user navigates away from checklist
- **Resume on return:** Timer continues from where it left off when user returns
- **Completion tracking:** Show remaining time if checklist completes before timer expires

## Architecture Overview

### Approach: ViewModel-Based Timer with In-Memory State

**Core Principle:** Timer is a UI-only concern that lives in the ViewModel. Configuration is persisted in the Checklist model, but runtime state (countdown) exists only while the checklist screen is active.

**Why This Approach:**
- Perfectly matches "freeze on leave" requirement
- Simple implementation - no background services needed
- Timer only runs when user is actively viewing the checklist
- ViewModel survives configuration changes (rotation)
- No unnecessary persistence or background work

**Key Components:**
1. **Checklist Model:** Stores timer configuration (`timerDurationMinutes`)
2. **ChecklistViewModel:** Manages timer runtime state and countdown logic
3. **ChecklistScreen:** Displays timer UI with different states
4. **CreateEditScreen:** Provides timer configuration picker
5. **SoundManager:** Plays alert sound when timer expires

## Data Model

### Checklist Model Changes

Add one optional field to the Checklist data class:

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

**Field Details:**
- **Type:** `Int?` (nullable integer)
- **Values:**
  - `null` = No timer configured (default)
  - `1-60` = Timer duration in minutes
- **Persistence:** Serialized to JSON with rest of checklist data
- **Validation:** CreateEditViewModel enforces 1-60 range

### ViewModel Timer State

ChecklistViewModel adds runtime timer state (not persisted):

```kotlin
data class ChecklistUiState(
    val checklist: Checklist?,
    val isLoading: Boolean,
    val timerState: TimerState = TimerState.NotConfigured
)

sealed class TimerState {
    object NotConfigured : TimerState()  // No timer set on checklist
    data class Running(val remainingSeconds: Int) : TimerState()  // Counting down
    data class Paused(val remainingSeconds: Int) : TimerState()  // Frozen (user left screen)
    data class Expired : TimerState()  // Hit zero, alert showing
    data class Completed(val remainingSeconds: Int) : TimerState()  // Checklist done with time left
}
```

**State Transitions:**
- `NotConfigured` → Never changes (checklist has no timer)
- `Running` → `Paused` (user leaves screen)
- `Running` → `Expired` (countdown hits zero)
- `Running` → `Completed` (all checklist items done)
- `Paused` → `Running` (user returns to screen)
- `Expired` → `NotConfigured` (user dismisses alert)

**Why This Works:**
- Clean separation: configuration (model) vs runtime (ViewModel)
- `timerDurationMinutes` persists across app restarts
- `TimerState` is ephemeral - recreated each time checklist opens
- No need to track start/pause timestamps in repository

## UI Design

### CreateEdit Screen - Timer Configuration Picker

Add new section after ResetDurationPicker:

```
┌─ Timer (Optional) ──────────────────────────┐
│ [ ] Enable Timer                             │
│                                              │
│ Duration: 15 minutes                         │
│ Slider: [━━━━●━━━━━━] 1min - 60min          │
│                                              │
│ Timer starts when checklist opens and        │
│ alerts you if time runs out                  │
└──────────────────────────────────────────────┘
```

**Components:**
- **Switch:** "Enable Timer" (default: off)
- **Slider:** 1-60 minutes in 1-minute increments
  - Disabled when switch is off
  - Default value: 15 minutes
  - Shows current value as "Duration: X minutes"
- **Helper Text:** Explains timer behavior

**Behavior:**
- Switch off → `timerDurationMinutes = null` → No timer
- Switch on → `timerDurationMinutes = slider value` → Timer enabled
- Editing existing checklist → Switch reflects current timer state

**Placement:**
- After "Reset Duration" picker
- Before "Items Editor" section
- Follows same Card styling as other pickers

### ChecklistScreen - Timer Display

Timer card appears below checklist name, above items list:

**Running State:**
```
┌─────────────────────────────────────┐
│ ⏱️  Timer: 05:23                    │
└─────────────────────────────────────┘
```
- White/neutral background (MaterialTheme.colorScheme.surface)
- Updates every second
- Format: MM:SS (minutes:seconds)
- Icon: ⏱️ (timer emoji or material icon)

**Expired State:**
```
┌─────────────────────────────────────┐
│ ⚠️  Timer expired!  [Dismiss]       │
└─────────────────────────────────────┘
```
- Red/warning background (MaterialTheme.colorScheme.errorContainer)
- "Dismiss" button hides the card
- Shows alert icon

**Completed State:**
```
┌─────────────────────────────────────┐
│ ✅  Completed with 3:45 remaining   │
└─────────────────────────────────────┘
```
- Green/success background (MaterialTheme.colorScheme.primaryContainer)
- Shows final remaining time
- Checkmark icon
- Persists until user leaves screen

**Hidden States:**
- `NotConfigured` → No display (timer card not rendered)
- `Paused` → No display (card hidden while away)

**Layout Details:**
- Full width card
- 16dp padding inside card
- 8dp margin from checklist name above
- 8dp margin from items list below
- Text: MaterialTheme.typography.titleLarge
- Elevation: 2dp (same as checklist items)

## Timer Behavior & Logic

### Timer Lifecycle

**1. Checklist Opens (ChecklistScreen appears):**
```kotlin
LaunchedEffect(checklist?.id) {
    checklist?.timerDurationMinutes?.let { duration ->
        viewModel.startTimer(duration)
    }
}
```
- If `timerDurationMinutes` is null → `TimerState.NotConfigured` (no display)
- If `timerDurationMinutes` is set → Start countdown
- Initial state: `TimerState.Running(remainingSeconds = duration * 60)`

**2. While Running (every second):**
```kotlin
fun startTimer(durationMinutes: Int) {
    timerJob?.cancel()  // Cancel any existing timer
    timerJob = viewModelScope.launch {
        var remaining = durationMinutes * 60
        while (remaining > 0) {
            _uiState.update { it.copy(timerState = TimerState.Running(remaining)) }
            delay(1000)  // Wait 1 second
            remaining--
        }
        // Timer expired
        soundManager.playCompletionSound()
        _uiState.update { it.copy(timerState = TimerState.Expired) }
    }
}
```
- ViewModel emits Flow updates every second
- UI re-renders with new countdown value
- When reaches 0 → Play completion sound + show alert

**3. User Leaves Checklist (navigates away):**
```kotlin
DisposableEffect(Unit) {
    onDispose {
        viewModel.pauseTimer()
    }
}

fun pauseTimer() {
    timerJob?.cancel()
    val currentState = _uiState.value.timerState
    if (currentState is TimerState.Running) {
        _uiState.update {
            it.copy(timerState = TimerState.Paused(currentState.remainingSeconds))
        }
    }
}
```
- Store current `remainingSeconds` in ViewModel
- Cancel countdown coroutine
- State changes to `Paused(remainingSeconds)`

**4. User Returns to Checklist:**
```kotlin
LaunchedEffect(checklist?.id) {
    val state = viewModel.uiState.value.timerState
    if (state is TimerState.Paused) {
        viewModel.resumeTimer()
    }
}

fun resumeTimer() {
    val currentState = _uiState.value.timerState
    if (currentState is TimerState.Paused) {
        startTimerFromSeconds(currentState.remainingSeconds)
    }
}
```
- Check if `TimerState` is `Paused`
- Resume countdown from stored `remainingSeconds`
- State changes back to `Running(remainingSeconds)`

**5. Checklist Completed (all items done):**
```kotlin
// In ChecklistViewModel when last item is marked done
private fun checkCompletion() {
    val allDone = checklist?.items?.all {
        it.state != ChecklistItemState.Pending
    } ?: false

    if (allDone) {
        stopTimerAsCompleted()
    }
}

private fun stopTimerAsCompleted() {
    timerJob?.cancel()
    val currentState = _uiState.value.timerState
    if (currentState is TimerState.Running) {
        _uiState.update {
            it.copy(timerState = TimerState.Completed(currentState.remainingSeconds))
        }
    }
}
```
- Detect when all items are done/skipped
- Stop countdown immediately
- State changes to `Completed(remainingSeconds)`
- Display shows "Completed with X:XX remaining"

**6. Timer Expires (hits zero):**
- Play completion sound via `SoundManager.playCompletionSound()`
- State changes to `TimerState.Expired`
- Display shows red alert with "Dismiss" button
- Dismiss button → Hides timer card, no state change needed

### Edge Cases Handled

**App killed while timer running:**
- Timer state lost (acceptable)
- Next time checklist opens → Fresh timer starts from configured duration
- No recovery mechanism needed (timer is session-based)

**Screen rotation:**
- ViewModel survives configuration change
- Timer continues uninterrupted
- UI re-renders with current state

**User completes checklist after timer expired:**
- Timer already in `Expired` state
- Completion doesn't change timer state
- Display still shows "Timer expired!" alert

**User rapidly leaves/returns:**
- `timerJob?.cancel()` prevents multiple timers
- Latest timer state always takes precedence
- No race conditions

**Multiple checklists with timers:**
- Each ChecklistViewModel is independent (keyed by checklist ID)
- Leaving Checklist A pauses its timer
- Opening Checklist B starts its own timer
- No interference between checklists

**Timer duration validation:**
- CreateEditViewModel enforces 1-60 minute range
- Slider constrained to valid values
- No need for runtime validation

## Sound Integration

**Sound Behavior:**
- Uses existing `SoundManager.playCompletionSound()`
- Respects user's completion sound setting:
  - None → No sound
  - Notification → System notification sound
  - Success Chime → Success chime
  - Tada → Tada sound
- Plays once when timer hits zero (not continuously)
- No haptic feedback (timer expiry is not a user action)

**Why Reuse Completion Sound:**
- Consistent with existing sound system
- User already configured their preference
- No new settings needed
- Same alert importance as checklist completion

## Implementation Details

### Files to Modify

**1. common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt**
- Add `timerDurationMinutes: Int? = null` field
- Update documentation

**2. androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt**
- Add `TimerPicker` composable function
- Insert after `ResetDurationPicker`
- Follow same pattern as other pickers

**3. androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditViewModel.kt**
- Add `timerEnabled: Boolean` and `timerDuration: Int` to UI state
- Add `updateTimerEnabled()` and `updateTimerDuration()` methods
- Update `save()` to include timer configuration

**4. androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt**
- Add `TimerCard` composable function
- Insert below checklist name, above LazyColumn
- Conditionally render based on `timerState`

**5. androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt**
- Add `timerState: TimerState` to UI state
- Add `timerJob: Job?` private property
- Implement `startTimer()`, `pauseTimer()`, `resumeTimer()` methods
- Update `updateItemState()` to check completion and stop timer
- Pass `SoundManager` to constructor

### Lines of Code Estimate

- Checklist.kt: +1 line (field)
- CreateEditScreen.kt: +60 lines (TimerPicker composable)
- CreateEditViewModel.kt: +30 lines (state + methods)
- ChecklistScreen.kt: +80 lines (TimerCard composable + integration)
- ChecklistViewModel.kt: +70 lines (timer logic)
- **Total:** ~240 new lines

### Complexity Assessment

**Complexity:** Medium

**Risk Level:** Low
- Isolated feature, no complex dependencies
- Uses existing patterns (Flow, ViewModel, Composable)
- No new permissions or services needed
- Timer logic is straightforward (countdown loop)

**Potential Issues:**
- Timer accuracy: 1-second delay may drift slightly (acceptable)
- Memory: Single coroutine per active checklist (minimal)
- Battery: Timer only runs when screen active (negligible)

## Testing Strategy

### Manual Testing Checklist

**Basic Functionality:**
1. Create checklist with 1-minute timer → Verify countdown starts and updates every second
2. Let 1-minute timer expire → Verify sound plays and alert shows
3. Dismiss expired timer alert → Verify card disappears
4. Create checklist without timer → Verify no timer display

**Pause/Resume:**
5. Start checklist with 2-minute timer → Leave at 1:30 → Return → Verify resumes from 1:30
6. Start timer → Leave → Return multiple times → Verify pause/resume works consistently
7. Start timer → Leave → Kill app → Reopen → Verify timer resets to configured duration

**Completion:**
8. Start timer → Complete all items with time remaining → Verify shows "Completed with X:XX"
9. Let timer expire → Then complete checklist → Verify expired alert persists
10. Complete checklist → Then dismiss timer card → Verify behavior

**Configuration:**
11. Create checklist with timer enabled → Edit → Disable timer → Verify persists
12. Create checklist without timer → Edit → Enable timer → Verify persists
13. Test slider with 1 min, 30 min, 60 min values → Verify all work

**Edge Cases:**
14. Rotate screen during countdown → Verify timer continues without interruption
15. Test with completion sound = None → Verify no sound on timer expiry
16. Create multiple checklists with different timers → Verify each independent
17. Start timer → Rapidly leave/return 10 times → Verify no crashes or weird behavior

**Integration:**
18. Test timer with all checklist colors → Verify timer card readable
19. Test timer with different font sizes (Settings) → Verify layout adapts
20. Test timer with checklists of varying item counts → Verify no conflicts

### Success Criteria

- Timer starts automatically when checklist with timer opens
- Countdown updates every second with accurate MM:SS display
- Timer pauses when leaving checklist
- Timer resumes from correct time when returning
- Completion sound plays when timer reaches zero
- Alert displays correctly in expired state
- "Completed with X:XX" shows when checklist finishes with time left
- Timer configuration persists across app restarts
- No crashes, no memory leaks, no battery drain
- Works smoothly with screen rotation

## Future Enhancements

**Not in scope for this feature:**
- Background timer (timer continues when app closed)
- Multiple timers per checklist
- Timer history/statistics
- Custom timer sounds
- Timer notifications
- Countdown intervals other than 1 second
- Visual progress indicator (circular progress)
- Timer presets (5min, 10min, 15min quick buttons)
- Voice announcements ("1 minute remaining")
- Vibration patterns when timer expires

These could be added later if users request them.

## Summary

The optional checklist timer feature adds time-boxing capability to checklists using a simple ViewModel-based approach. Timer configuration is stored in the Checklist model and persisted, while runtime state lives in the ViewModel and exists only during active sessions. The feature integrates cleanly with existing sound settings and follows established UI patterns. Implementation is straightforward with low risk and moderate complexity.
