# Checklist Library & Slider UI Design

**Date:** 2026-02-12
**Status:** Approved
**Author:** Design collaboration with user

---

## Overview

This design introduces a complete checklist management system with:
- Home screen with Checklist/Timer library tabs
- Time-based checklist scheduling (day of week + time range)
- Slider-based interaction for marking items done/skipped
- Configurable state persistence per checklist
- Visual color coding for quick identification
- Event logging for all user actions

---

## 1. Information Architecture & Navigation

### 1.1 Screen Structure

```
Home Screen (Checklists/Timers tabs)
   ↓ tap checklist
Checklist Detail Screen
   ↓ edit button
Checklist Editor Screen → save → Home

   ↓ tap timer
Timer Detail Screen → back → Home
```

### 1.2 Home Screen

**Top Bar:**
- App title "Pyanpyan"
- Settings icon (top right)

**Tab Bar:**
- Two tabs: "Checklists" | "Timers"
- Material 3 segmented control
- Clear visual indication of selected tab

**Checklists Tab:**
- **Active checklists** (matching current day/time):
  - Displayed at top
  - Full opacity, vibrant colors
  - Colored accent bar on left
  - Alphabetically sorted
  - Shows status: "X pending" or "All done!"
- **Inactive checklists**:
  - Displayed below visual separator
  - 50% opacity, desaturated
  - Shows schedule info instead of status
  - Alphabetically sorted
- FAB (Floating Action Button) to create new checklist

**Interactions:**
- Tap checklist card → open detail screen
- Swipe left → delete (with confirmation)
- Long press → edit

---

## 2. Data Model

### 2.1 Checklist Schedule

```kotlin
data class ChecklistSchedule(
    val daysOfWeek: Set<DayOfWeek>, // empty set = all days
    val timeRange: TimeRange // ALL_DAY constant for unrestricted
)

sealed class TimeRange {
    object AllDay : TimeRange() // No time restrictions

    data class Specific(
        val startTime: LocalTime, // e.g., 06:00
        val endTime: LocalTime     // e.g., 09:00
    ) : TimeRange()
}

// Convenience extension
val ChecklistSchedule.isAlwaysOn: Boolean
    get() = daysOfWeek.isEmpty() && timeRange is TimeRange.AllDay
```

### 2.2 Checklist Color

```kotlin
enum class ChecklistColor(val hex: String, val name: String) {
    SOFT_BLUE("#A8D5E2", "Soft Blue"),
    CALM_GREEN("#C8E6C9", "Calm Green"),
    GENTLE_PURPLE("#D1C4E9", "Gentle Purple"),
    WARM_PEACH("#FFE0B2", "Warm Peach"),
    COOL_MINT("#B2DFDB", "Cool Mint"),
    LIGHT_LAVENDER("#E1BEE7", "Light Lavender"),
    PALE_YELLOW("#FFF9C4", "Pale Yellow"),
    SOFT_ROSE("#F8BBD0", "Soft Rose")
}
```

### 2.3 State Persistence

```kotlin
enum class StatePersistenceDuration(val milliseconds: Long?) {
    ZERO(0),           // Reset immediately when leaving
    ONE_MINUTE(60_000),
    FIFTEEN_MINUTES(900_000),
    ONE_HOUR(3_600_000),
    ONE_DAY(86_400_000),
    NEVER(null)        // Keep state until daily reset
}
```

**Behavior:**
- Timer starts when user navigates away from checklist detail screen
- If user returns before duration expires → state preserved
- If duration expires → all items reset to Pending (sliders return to center)
- Default: 15 minutes
- Separate from daily reset (which happens at day boundary)

### 2.4 Item Icons

```kotlin
@JvmInline
value class ItemIconId(val value: String) // e.g., "tooth", "pill"

data class ChecklistItem(
    val id: ChecklistItemId,
    val title: String,
    val iconId: ItemIconId?, // null = no icon
    val state: ChecklistItemState
)
```

**Icon Configuration:**

Icons defined in single JSON file for easy maintenance:

```json
// assets/icons.json
{
  "version": "1.0",
  "categories": [
    {
      "name": "Morning",
      "icons": [
        { "id": "tooth", "emoji": "🦷", "name": "Brush teeth" },
        { "id": "shower", "emoji": "🚿", "name": "Shower" },
        { "id": "coffee", "emoji": "☕", "name": "Coffee" }
      ]
    },
    {
      "name": "Health",
      "icons": [
        { "id": "pill", "emoji": "💊", "name": "Medicine" },
        { "id": "water", "emoji": "💧", "name": "Drink water" }
      ]
    }
  ]
}
```

### 2.5 Updated Checklist Model

```kotlin
data class Checklist(
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration,
    val lastAccessedAt: Instant? = null
)
```

### 2.6 Activity State Query

```kotlin
sealed class ChecklistActivityState {
    object Active    // matches current day/time
    object Inactive  // doesn't match schedule
}

fun Checklist.getActivityState(currentTime: LocalDateTime): ChecklistActivityState {
    // Check day of week
    if (schedule.daysOfWeek.isNotEmpty() &&
        currentTime.dayOfWeek !in schedule.daysOfWeek) {
        return ChecklistActivityState.Inactive
    }

    // Check time range
    when (val range = schedule.timeRange) {
        is TimeRange.AllDay -> return ChecklistActivityState.Active
        is TimeRange.Specific -> {
            val now = currentTime.time
            return if (now in range.startTime..range.endTime) {
                ChecklistActivityState.Active
            } else {
                ChecklistActivityState.Inactive
            }
        }
    }
}
```

---

## 3. Slider Control Component

### 3.1 Visual Design

The slider replaces button row in checklist item cards:

```
Item Title with optional icon
[Skip] ←—————— ⚫ ——————→ [Done]
       ^       ^         ^
     Left   Center    Right
   (ignored) (pending) (done)
```

**Components:**

1. **Track** - horizontal bar showing drag path
   - Left zone: "Skip Today" indicator
   - Center: neutral state
   - Right zone: "Done" indicator

2. **Thumb** - draggable circle/pill
   - Starts centered when Pending
   - Large touch target (min 48dp)
   - Smooth drag following finger

3. **Labels** - text hints on left/right
   - Left: "Skip" or icon
   - Right: "Done" or checkmark
   - Fade in as thumb approaches

### 3.2 Interaction Mechanics

**Behavior:**
- **Drag threshold**: ≥70% toward side to commit action
- **Spring back**: If released before threshold, returns to center
- **Lock in place**: Once threshold crossed, thumb locks to that side
- **Haptic feedback**: Gentle vibration at threshold (optional)
- **Visual feedback**: Track color intensifies as thumb approaches threshold

**State Persistence:**
- After sliding right → thumb stays right, item shows Done
- After sliding left → thumb stays left, item shows Skipped
- Slider becomes non-interactive once action completed
- Resets based on checklist's state persistence duration

### 3.3 Calm Design Considerations

- Slow, smooth animations (300-400ms)
- No sudden snapping
- Soft colors that intensify gradually
- Optional haptics (can be disabled)

---

## 4. Checklist Detail Screen

### 4.1 Layout

**Top Bar:**
- Back arrow (left) → Home
- Checklist name (center) with color accent
- Edit icon (right) → Checklist Editor

**Content:**
- Scrollable list of checklist items
- Each item in a card with:
  - Optional icon + title
  - Slider control
  - State indicator (for done/skipped)

### 4.2 Item Card States

1. **Pending** (slider at center):
   - Normal card background
   - Slider interactive
   - No status text

2. **Done** (slider at right):
   - Subtle green/primary tint
   - Title has strikethrough
   - "✓ Completed" text below slider
   - Slider locked

3. **Skipped Today** (slider at left):
   - Muted/faded appearance (40% opacity)
   - "⊘ Skipped today" text below slider
   - Slider locked

**Empty State:**
- "No items yet. Tap Edit to add some!"

---

## 5. Checklist Editor Screen

### 5.1 Layout

**Top Bar:**
- Close/Cancel (left) → discard changes
- "Edit Checklist" or "New Checklist" title
- Save button (right)

**Fields:**

1. **Name**
   - Text input, max ~50 characters
   - Required

2. **Color**
   - 8 circular swatches
   - Tappable, selected has checkmark

3. **Active Days**
   - 7 toggle buttons (M T W T F S S)
   - "All days" checkbox shortcut
   - Empty = all days (default)

4. **Active Time**
   - Radio: "All day" (default)
   - Radio: "Specific time range"
     - Shows time pickers when selected

5. **Memory Duration**
   - Horizontal slider with 6 stops
   - Label below shows current value
   - Stops: [0] [1min] [15min] [1hr] [1day] [Never]
   - Default: 15 minutes

6. **Items List**
   - Reorderable (drag handle ☰)
   - Delete button (✕) per item
   - Each item shows: icon + title
   - [+ Add item] button below list

### 5.2 Item Editor

**When adding/editing item:**

Modal or inline editor:
- Icon picker (tap to open icon selector modal)
- Text input for item name
- Cancel/Save buttons

**Icon Picker Modal:**
- Scrollable grid of categorized icons
- Organized by: Morning, Evening, Health, etc.
- Checkbox: "No icon"
- Future: "From photo" (disabled/coming soon)

### 5.3 Validation

- Name cannot be empty
- Must have at least one item
- If "Specific time range", must pick valid times

### 5.4 Save Behavior

- Creates `UpdateChecklist` or `CreateChecklist` command
- Logs appropriate event
- Returns to Home or Detail screen

---

## 6. Event Logging

### 6.1 Event Types

```kotlin
sealed class ChecklistEvent {
    abstract val checklistId: ChecklistId
    abstract val timestamp: Instant

    data class Created(
        override val checklistId: ChecklistId,
        override val timestamp: Instant,
        val name: String,
        val itemCount: Int
    ) : ChecklistEvent()

    data class Updated(
        override val checklistId: ChecklistId,
        override val timestamp: Instant,
        val changes: Set<ChangeType>
    ) : ChecklistEvent()

    data class Accessed(
        override val checklistId: ChecklistId,
        override val timestamp: Instant
    ) : ChecklistEvent()

    data class Deleted(
        override val checklistId: ChecklistId,
        override val timestamp: Instant
    ) : ChecklistEvent()

    enum class ChangeType {
        NAME, SCHEDULE, COLOR, STATE_PERSISTENCE,
        ITEMS_ADDED, ITEMS_REMOVED
    }
}

sealed class TimerEvent {
    abstract val timerId: TimerId
    abstract val timestamp: Instant
    // Created, Started, Completed, Accessed, etc.
}
```

### 6.2 Event Storage

```kotlin
interface EventLog {
    suspend fun append(event: ChecklistEvent)
    suspend fun getEvents(
        checklistId: ChecklistId? = null,
        since: Instant? = null,
        limit: Int? = null
    ): List<ChecklistEvent>

    suspend fun getLastAccessed(checklistId: ChecklistId): Instant?
}
```

### 6.3 Privacy & Usage

- Stored locally only (local-first)
- No personal data (just IDs, counts, timestamps)
- If analytics enabled: only aggregated metrics
- Users can clear event log anytime

**Use Cases:**
1. Track `lastAccessedAt` for state persistence
2. Show "recently used" checklists
3. Debug user flows
4. Optional analytics (aggregated, user opt-in)

---

## 7. Architecture Updates

### 7.1 New Commands

**Checklist Management:**
- `CreateChecklist` - create with name, schedule, color, items
- `UpdateChecklist` - modify any checklist properties
- `DeleteChecklist` - remove checklist
- `AddChecklistItem` - add item with optional icon
- `RemoveChecklistItem` - remove item
- `UpdateChecklistItem` - edit item details
- `ReorderChecklistItems` - change item order

**Existing Item State Commands:**
- `MarkItemDone`
- `IgnoreItemToday`
- `ResetDailyState`

### 7.2 New Queries

**Checklist Queries:**
- `GetAllChecklists` - all user checklists
- `GetActiveChecklists` - matching current day/time
- `GetInactiveChecklists` - not matching schedule
- `GetChecklist` - specific checklist by ID
- `GetChecklistUIState` - UI state for detail screen

**Item Queries:**
- `GetIgnoredItems` - all items ignored today

**Event Queries:**
- `GetChecklistEvents` - event history
- `GetLastAccessed` - last access timestamp

---

## 8. Implementation Phases

### Phase 1: Data Layer
- Update domain models (Checklist, ChecklistItem, Schedule, etc.)
- Implement event logging
- Create new commands and queries
- Add persistence layer

### Phase 2: Home Screen
- Create tab navigation (Checklists/Timers)
- Implement checklist library list
- Add active/inactive filtering and sorting
- Create empty states

### Phase 3: Slider Component
- Build custom slider Composable
- Implement drag mechanics and thresholds
- Add haptic feedback
- Handle state locking

### Phase 4: Detail Screen
- Update ChecklistScreen to use sliders
- Implement state persistence logic
- Add color theming per checklist

### Phase 5: Editor Screen
- Create ChecklistEditorScreen
- Build all form fields
- Implement icon picker
- Add validation

### Phase 6: Polish
- Add animations and transitions
- Implement empty states
- Add loading states
- Test accessibility

---

## 9. Design Principles Applied

- **ADHD-friendly**: Slider interaction is deliberate, not accidental
- **Calm design**: Soft colors, smooth animations, no harsh feedback
- **Forgiving**: State persistence allows returning without penalty
- **Visual clarity**: Color coding and active/inactive states
- **Local-first**: All data stored locally, privacy-respecting
- **No shame**: Skip is first-class action with equal prominence

---

## 10. Open Questions

None - design approved and ready for implementation.

---

## 11. References

- Main spec: `/specs.md`
- Current implementation: `/androidApp/src/main/kotlin/com/pyanpyan/android/`
- Domain models: `/common/src/commonMain/kotlin/com/pyanpyan/domain/`
