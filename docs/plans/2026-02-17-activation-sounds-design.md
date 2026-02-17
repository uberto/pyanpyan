# Checklist Activation Schedule & Sound System Design

## Overview

Add three major features to the Pyanpyan Android app:
1. **Schedule UI** - Allow users to configure when checklists are active (days of week + time range)
2. **State Reset Logic** - Automatically reset checklist items based on statePersistence duration
3. **Sound System** - Play sounds for slider interactions and checklist completion with user-configurable preferences

## Architecture Overview

### Components

1. **Schedule UI** - New section in CreateEditScreen with day-of-week chips and time range picker
2. **State Reset Logic** - ChecklistViewModel checks `lastAccessedAt` + `statePersistence` on load, resets if expired
3. **Sound System** - `SoundManager` class using SoundPool for instant playback
4. **Settings Screen** - New screen for global sound preferences with navigation from library TopAppBar
5. **Settings Repository** - Persist user preferences using DataStore with JSON serialization

### Data Flow

```
User edits schedule in CreateEditScreen
  ↓
Saves to Checklist domain model (schedule field already exists)
  ↓
User opens checklist
  ↓
ViewModel loads, checks if (now - lastAccessedAt) > statePersistence
  ↓
If expired: reset all items, update lastAccessedAt
  ↓
User drags slider
  ↓
ViewModel action triggers SoundManager.playSwipeSound()
  ↓
All items complete?
  ↓
Yes → SoundManager.playCompletionSound()
```

```
User opens Settings from library TopAppBar
  ↓
Loads preferences from SettingsRepository
  ↓
User changes sound preference
  ↓
Saves to SettingsRepository
  ↓
SoundManager observes settings, updates behavior
```

## Design Section 1: Schedule UI Component

### Location
CreateEditScreen, between ColorPicker and ItemsEditor sections

### UI Structure
```
┌─ Schedule Card ────────────────────────┐
│ Schedule                               │
│                                        │
│ Days of Week:                          │
│ [M] [T] [W] [T] [F] [S] [S]           │
│ (toggleable chips, multi-select)       │
│                                        │
│ Time Range:                            │
│ ○ All Day                              │
│ ○ Specific Time                        │
│   [Start: 9:00 AM] [End: 5:00 PM]     │
│   (only visible when Specific selected)│
└────────────────────────────────────────┘
```

### Behavior
- Days default to all selected (matching current CreateEditViewModel default)
- Empty selection = inactive checklist (won't appear in active section)
- Time defaults to "All Day"
- Time pickers use Material3 TimePickerDialog
- State saved in CreateEditViewModel (daysOfWeek, timeRange) - already exists

### UI State
CreateEditViewModel already has:
- `daysOfWeek: Set<DayOfWeek>`
- `timeRange: TimeRange`

Add helper property:
- `isAllDayTime: Boolean` - derived from `timeRange is TimeRange.AllDay`

### New Composables
- `SchedulePicker(daysOfWeek, timeRange, onDaysChange, onTimeRangeChange)` - Main card component
- `DayOfWeekChipRow(selected, onSelectionChange)` - Day selector
- `TimeRangePicker(timeRange, onChange)` - Radio buttons + time pickers

## Design Section 2: State Reset Logic

### When It Happens
1. ChecklistViewModel loads checklist from repository
2. Checks if `(now - lastAccessedAt) > statePersistence.duration`
3. If expired: calls `checklist.resetAllItems()` and saves
4. Updates `lastAccessedAt` to current time

### Implementation
```kotlin
private fun loadChecklist() {
    repository.getChecklist(checklistId)
        .onSuccess { checklist ->
            val now = Clock.System.now()
            val shouldReset = checklist.lastAccessedAt?.let { lastAccess ->
                (now - lastAccess) > checklist.statePersistence.duration
            } ?: false

            val finalChecklist = if (shouldReset) {
                val reset = checklist.resetAllItems().copy(lastAccessedAt = now)
                repository.saveChecklist(reset) // Persist reset state
                reset
            } else {
                val updated = checklist.copy(lastAccessedAt = now)
                repository.saveChecklist(updated) // Update access time
                updated
            }

            _uiState.value = ChecklistUiState(checklist = finalChecklist)
        }
}
```

### Edge Cases
- **First time opening** (lastAccessedAt = null): Don't reset, just set timestamp
- **App killed while checklist open**: On reopen, checks elapsed time
- **Clock changes**: Uses Instant for UTC time, safe from local clock changes
- **statePersistence = NEVER**: Don't reset (duration = infinite)

## Design Section 3: Sound System

### SoundManager Class

Located in: `androidApp/src/main/kotlin/com/pyanpyan/android/sound/SoundManager.kt`

```kotlin
class SoundManager(
    context: Context,
    settingsRepository: SettingsRepository
) {
    private val toneGenerator: ToneGenerator
    private val ringtoneManager: RingtoneManager
    private val settingsFlow: StateFlow<AppSettings>

    fun playSwipeSound()
    fun playCompletionSound()
    fun release()
}
```

### Sound Implementation

**Swipe Sounds** (using ToneGenerator):
- `NONE` - No sound
- `SOFT_CLICK` - TONE_DTMF_1, 50ms duration
- `BEEP` - TONE_PROP_BEEP, standard click
- `POP` - TONE_DTMF_STAR, playful tone

**Completion Sounds** (using RingtoneManager):
- `NONE` - No sound
- `NOTIFICATION` - RingtoneManager.TYPE_NOTIFICATION (default system chime)
- `SUCCESS_CHIME` - TYPE_NOTIFICATION with specific URI
- `TADA` - Longer system notification sound

### Integration Points
- Created in ChecklistViewModel with application context + settings repository
- Released in ViewModel.onCleared()
- Called in `markItemDone()`, `ignoreItemToday()` → swipe sound
- Called when all items completed → completion sound

### Volume & Permissions
- Uses STREAM_NOTIFICATION (respects Do Not Disturb mode)
- ToneGenerator and RingtoneManager don't need runtime permissions
- Works without POST_NOTIFICATIONS permission

## Design Section 4: Settings Screen

### Navigation
- Add Settings icon button to ChecklistLibraryScreen TopAppBar
- MainActivity adds `Screen.Settings` to sealed class
- Navigate: Library → Settings → Back to Library

### UI Structure
```
┌─ Settings Screen ─────────────────────┐
│ ← Settings                             │
│                                        │
│ ┌─ Sounds ────────────────────────┐  │
│ │ Swipe Sound                      │  │
│ │ > Soft Click                  ▼  │  │
│ │   (dropdown: None, Soft Click,   │  │
│ │    Beep, Pop)                    │  │
│ │                                  │  │
│ │ Completion Sound                 │  │
│ │ > Notification                ▼  │  │
│ │   (dropdown: None, Notification, │  │
│ │    Success Chime, Tada)          │  │
│ │                                  │  │
│ │ [Test Swipe Sound]               │  │
│ │ [Test Completion Sound]          │  │
│ └──────────────────────────────────┘  │
│                                        │
│ (Future sections: General, About, etc)│
└────────────────────────────────────────┘
```

### Settings Domain Model

Located in: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`

```kotlin
@Serializable
data class AppSettings(
    val swipeSound: SwipeSound = SwipeSound.SOFT_CLICK,
    val completionSound: CompletionSound = CompletionSound.NOTIFICATION,
    val enableHapticFeedback: Boolean = true // reserved for future
)

@Serializable
enum class SwipeSound {
    @SerialName("none") NONE,
    @SerialName("soft_click") SOFT_CLICK,
    @SerialName("beep") BEEP,
    @SerialName("pop") POP
}

@Serializable
enum class CompletionSound {
    @SerialName("none") NONE,
    @SerialName("notification") NOTIFICATION,
    @SerialName("success_chime") SUCCESS_CHIME,
    @SerialName("tada") TADA
}
```

### SettingsRepository

**Interface** in common module:
```kotlin
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings): Result<Unit>
}
```

**Android Implementation** using DataStore:
- File: `androidApp/src/main/kotlin/com/pyanpyan/android/repository/DataStoreSettingsRepository.kt`
- Uses androidx.datastore.preferences.core
- Stores JSON serialized AppSettings in `settings.json`
- Exposes `Flow<AppSettings>` for reactive updates

### Integration
- SettingsRepository injected into ChecklistViewModel (alongside ChecklistRepository)
- SoundManager created with settingsRepository, observes settings flow
- Settings screen has its own ViewModel (SettingsViewModel)
- Test buttons create temporary SoundManager to preview sounds

## Design Section 5: Completion Detection & Edge Cases

### Completion Detection

When marking item Done/Skipped:
```kotlin
fun markItemDone(itemId: ChecklistItemId) {
    // ... existing update logic ...
    soundManager.playSwipeSound()

    // Check if all items completed
    if (updatedChecklist.items.all {
        it.state != ChecklistItemState.Pending
    }) {
        soundManager.playCompletionSound()
    }
}
```

All items must be in `Done` or `IgnoredToday` state (not `Pending`) to trigger completion sound.

### Edge Cases

**1. Sound System Initialization Failure:**
- Catch exceptions in SoundManager constructor
- Log error with tag "SoundManager"
- Continue without sounds (silent fallback)
- Don't crash the app

**2. Settings Not Loaded Yet:**
- Use default settings (SOFT_CLICK, NOTIFICATION) in SoundManager
- Load asynchronously from DataStore
- Apply new settings when flow emits

**3. Permission Issues (Android 13+):**
- ToneGenerator and RingtoneManager work without runtime permissions
- STREAM_NOTIFICATION doesn't need POST_NOTIFICATIONS
- No permission handling required

**4. Rapid Slider Actions:**
- SoundPool naturally handles overlapping sounds
- ToneGenerator might overlap (acceptable)
- If performance issue, add debounce (100ms)

**5. Background/Foreground Transitions:**
- Sounds only play when app foreground (natural Android behavior)
- SoundManager lifecycle tied to ViewModel (created/released correctly)
- No special handling needed

**6. State Persistence Edge Case:**
- If `statePersistence = NEVER`, duration check returns false (never reset)
- `lastAccessedAt` still tracked for future use
- Time going backward (rare): Instant uses UTC, safe

**7. Time Picker Validation:**
- If user selects start time > end time, show error
- Don't allow saving invalid time ranges
- Or interpret as "wraps midnight" (e.g., 10 PM - 2 AM)

### Testing Strategy

**Unit Tests:**
- State reset logic with mocked Clock
- Completion detection (all combinations of item states)
- Settings serialization/deserialization
- TimeRange validation

**Integration Tests:**
- ChecklistViewModel with real repository, mocked Clock
- Settings flow updates affecting SoundManager

**Manual Testing:**
- Sound playback with different settings
- Schedule UI interactions
- State reset after time expiry
- Completion sound triggering

**Test Requirement:**
All tests must pass before committing each implementation task.

## Files to Create

### Common Module
- `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`
- `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/SettingsRepository.kt`

### Android Module
- `androidApp/src/main/kotlin/com/pyanpyan/android/repository/DataStoreSettingsRepository.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/sound/SoundManager.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/SchedulePicker.kt`

### Tests
- `common/src/commonTest/kotlin/com/pyanpyan/domain/model/AppSettingsTest.kt`
- `androidApp/src/test/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModelStateResetTest.kt`

## Files to Modify

### Android Module
- `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt` - Add Settings screen to navigation
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt` - Add Settings button to TopAppBar
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt` - Add SchedulePicker component
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt` - Add state reset logic, sound integration
- `androidApp/build.gradle.kts` - Add DataStore dependency

## Dependencies to Add

```kotlin
// In androidApp/build.gradle.kts
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

## Success Criteria

1. ✅ Users can configure checklist schedule (days + time) in CreateEditScreen
2. ✅ Inactive checklists (no days selected) appear in "Inactive" section
3. ✅ Checklist items auto-reset after statePersistence duration expires
4. ✅ Swipe sounds play when dragging sliders (configurable)
5. ✅ Completion sound plays when all items done/skipped (configurable)
6. ✅ Settings screen accessible from library TopAppBar
7. ✅ Sound preferences persist across app restarts
8. ✅ All tests pass before each commit
9. ✅ No crashes or exceptions in production

## Future Enhancements (Not in Scope)

- Haptic feedback option (UI present, implementation deferred)
- Custom sound file upload
- Volume control per sound type
- Dark mode preference
- Notification settings

## Implementation Notes

### Completed Features
✅ Schedule UI with day-of-week chips and time range picker
✅ State reset logic based on statePersistence duration
✅ SoundManager with ToneGenerator for swipe sounds
✅ System notification sounds for completion
✅ Settings screen with sound preferences
✅ DataStore persistence for settings
✅ Full integration with ChecklistViewModel

### Known Limitations
- Time picker dialogs use placeholder implementation (TODO for Material3 TimePicker)
- Completion sounds use default system sounds (SUCCESS_CHIME and TADA map to same sound currently)
- No volume control per sound type

### Testing
- Unit tests: AppSettings serialization, state reset logic, ChecklistViewModel state reset
- Manual tests: Requires Android emulator or physical device
- Integration: Sounds play correctly with user preferences (verified through code review)

### Implementation Highlights
- Used DataStore for settings persistence with JSON serialization
- SoundManager observes settings Flow for automatic updates
- Proper resource management with coroutine cancellation and ToneGenerator release
- Thread-safe implementation with @Volatile annotations
- Fixed resource leaks identified during code review
