# Checklist Library & Slider UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform single-checklist app into multi-checklist library with time-based scheduling, slider interactions, and full CRUD operations.

**Architecture:** Follow existing CQS pattern. Domain models in `common` module with commands/queries. Android UI in `androidApp` using Jetpack Compose. Event logging for all user actions. Local-first persistence.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Material 3, kotlinx-datetime, kotlinx-serialization

---

## Phase 1: Data Layer Foundation

### Task 1: Add TimeRange Domain Model

**Goal:** Create sealed class for checklist time ranges (AllDay vs Specific)

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/TimeRange.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimeRangeTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimeRangeTest.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeRangeTest {
    @Test
    fun allDay_has_no_restrictions() {
        val range = TimeRange.AllDay

        assertTrue(range.isAllDay)
    }

    @Test
    fun specific_range_has_start_and_end() {
        val start = LocalTime(9, 0)
        val end = LocalTime(17, 0)
        val range = TimeRange.Specific(start, end)

        assertFalse(range.isAllDay)
        assertEquals(start, range.startTime)
        assertEquals(end, range.endTime)
    }

    @Test
    fun time_is_within_specific_range() {
        val range = TimeRange.Specific(
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0)
        )

        val morning = LocalTime(10, 30)
        assertTrue(range.contains(morning))

        val evening = LocalTime(20, 0)
        assertFalse(range.contains(evening))
    }

    @Test
    fun allDay_contains_any_time() {
        val range = TimeRange.AllDay

        assertTrue(range.contains(LocalTime(0, 0)))
        assertTrue(range.contains(LocalTime(12, 0)))
        assertTrue(range.contains(LocalTime(23, 59)))
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests TimeRangeTest
```

Expected: FAIL - TimeRange class not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/model/TimeRange.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.LocalTime

sealed class TimeRange {
    abstract val isAllDay: Boolean
    abstract fun contains(time: LocalTime): Boolean

    object AllDay : TimeRange() {
        override val isAllDay: Boolean = true
        override fun contains(time: LocalTime): Boolean = true
    }

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

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests TimeRangeTest
```

Expected: PASS - all tests green

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/TimeRange.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimeRangeTest.kt
git commit -m "feat(domain): add TimeRange sealed class with AllDay and Specific

- Add TimeRange.AllDay for unrestricted schedules
- Add TimeRange.Specific with start/end times
- Add contains() method to check if time falls within range
- Add isAllDay property for convenience checks"
```

---

### Task 2: Add ChecklistSchedule Domain Model

**Goal:** Create schedule model with days of week and time range

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistSchedule.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistScheduleTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistScheduleTest.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecklistScheduleTest {
    @Test
    fun always_on_schedule_has_no_restrictions() {
        val schedule = ChecklistSchedule(
            daysOfWeek = emptySet(),
            timeRange = TimeRange.AllDay
        )

        assertTrue(schedule.isAlwaysOn)
    }

    @Test
    fun weekday_only_schedule() {
        val schedule = ChecklistSchedule(
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            timeRange = TimeRange.AllDay
        )

        assertFalse(schedule.isAlwaysOn)
        assertTrue(schedule.daysOfWeek.contains(DayOfWeek.MONDAY))
        assertFalse(schedule.daysOfWeek.contains(DayOfWeek.SATURDAY))
    }

    @Test
    fun morning_routine_schedule() {
        val schedule = ChecklistSchedule(
            daysOfWeek = emptySet(),
            timeRange = TimeRange.Specific(
                startTime = LocalTime(6, 0),
                endTime = LocalTime(9, 0)
            )
        )

        assertFalse(schedule.isAlwaysOn)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistScheduleTest
```

Expected: FAIL - ChecklistSchedule not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistSchedule.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.DayOfWeek

data class ChecklistSchedule(
    val daysOfWeek: Set<DayOfWeek>, // empty set = all days
    val timeRange: TimeRange
) {
    val isAlwaysOn: Boolean
        get() = daysOfWeek.isEmpty() && timeRange.isAllDay
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistScheduleTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistSchedule.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistScheduleTest.kt
git commit -m "feat(domain): add ChecklistSchedule model

- Add ChecklistSchedule with daysOfWeek and timeRange
- Add isAlwaysOn computed property
- Empty daysOfWeek set means all days
- TimeRange.AllDay means no time restrictions"
```

---

### Task 3: Add ChecklistColor Enum

**Goal:** Define 8 tranquil colors for checklist identification

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistColor.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistColorTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistColorTest.kt
package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistColorTest {
    @Test
    fun has_8_color_options() {
        assertEquals(8, ChecklistColor.entries.size)
    }

    @Test
    fun each_color_has_hex_and_name() {
        val blue = ChecklistColor.SOFT_BLUE

        assertEquals("#A8D5E2", blue.hex)
        assertEquals("Soft Blue", blue.displayName)
    }

    @Test
    fun can_get_all_colors() {
        val colors = ChecklistColor.entries

        assertEquals(ChecklistColor.SOFT_BLUE, colors[0])
        assertEquals(ChecklistColor.SOFT_ROSE, colors[7])
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistColorTest
```

Expected: FAIL - ChecklistColor not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistColor.kt
package com.pyanpyan.domain.model

enum class ChecklistColor(val hex: String, val displayName: String) {
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

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistColorTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistColor.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistColorTest.kt
git commit -m "feat(domain): add ChecklistColor enum with 8 tranquil colors

- Define 8 ADHD-friendly colors with hex codes
- Each color has displayName for UI
- Colors: Soft Blue, Calm Green, Gentle Purple, Warm Peach,
  Cool Mint, Light Lavender, Pale Yellow, Soft Rose"
```

---

### Task 4: Add StatePersistenceDuration Enum

**Goal:** Define duration options for checklist state memory

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/StatePersistenceDuration.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/StatePersistenceDurationTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/model/StatePersistenceDurationTest.kt
package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatePersistenceDurationTest {
    @Test
    fun has_6_duration_options() {
        assertEquals(6, StatePersistenceDuration.entries.size)
    }

    @Test
    fun zero_resets_immediately() {
        assertEquals(0L, StatePersistenceDuration.ZERO.milliseconds)
    }

    @Test
    fun fifteen_minutes_is_default() {
        assertEquals(900_000L, StatePersistenceDuration.FIFTEEN_MINUTES.milliseconds)
    }

    @Test
    fun never_expires_has_null_duration() {
        assertNull(StatePersistenceDuration.NEVER.milliseconds)
    }

    @Test
    fun all_durations_have_display_names() {
        assertEquals("Reset immediately", StatePersistenceDuration.ZERO.displayName)
        assertEquals("1 minute", StatePersistenceDuration.ONE_MINUTE.displayName)
        assertEquals("15 minutes", StatePersistenceDuration.FIFTEEN_MINUTES.displayName)
        assertEquals("1 hour", StatePersistenceDuration.ONE_HOUR.displayName)
        assertEquals("1 day", StatePersistenceDuration.ONE_DAY.displayName)
        assertEquals("Never", StatePersistenceDuration.NEVER.displayName)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests StatePersistenceDurationTest
```

Expected: FAIL - StatePersistenceDuration not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/model/StatePersistenceDuration.kt
package com.pyanpyan.domain.model

enum class StatePersistenceDuration(
    val milliseconds: Long?,
    val displayName: String
) {
    ZERO(0L, "Reset immediately"),
    ONE_MINUTE(60_000L, "1 minute"),
    FIFTEEN_MINUTES(900_000L, "15 minutes"),
    ONE_HOUR(3_600_000L, "1 hour"),
    ONE_DAY(86_400_000L, "1 day"),
    NEVER(null, "Never");

    companion object {
        val DEFAULT = FIFTEEN_MINUTES
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests StatePersistenceDurationTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/StatePersistenceDuration.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/model/StatePersistenceDurationTest.kt
git commit -m "feat(domain): add StatePersistenceDuration enum

- Add 6 duration options: 0, 1min, 15min, 1hr, 1day, never
- null milliseconds for NEVER (keeps state until daily reset)
- Default is 15 minutes (matches spec)
- Each duration has displayName for UI"
```

---

### Task 5: Add ItemIconId Value Class

**Goal:** Create type-safe wrapper for item icon identifiers

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt`
- Modify: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemTest.kt`

**Step 1: Write the failing test**

```kotlin
// Add to common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemTest.kt
@Test
fun item_can_have_optional_icon() {
    val withIcon = ChecklistItem(
        id = ChecklistItemId("test"),
        title = "Brush teeth",
        iconId = ItemIconId("tooth"),
        state = ChecklistItemState.Pending
    )

    assertEquals(ItemIconId("tooth"), withIcon.iconId)

    val noIcon = ChecklistItem(
        id = ChecklistItemId("test"),
        title = "Other task",
        iconId = null,
        state = ChecklistItemState.Pending
    )

    assertNull(noIcon.iconId)
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistItemTest
```

Expected: FAIL - ItemIconId not found, iconId parameter not in constructor

**Step 3: Write minimal implementation**

```kotlin
// Add to top of common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt
@JvmInline
value class ItemIconId(val value: String)

// Update ChecklistItem data class
data class ChecklistItem(
    val id: ChecklistItemId,
    val title: String,
    val iconId: ItemIconId? = null, // NEW
    val state: ChecklistItemState
) {
    fun markDone(): ChecklistItem = copy(state = ChecklistItemState.Done)
    fun ignoreToday(): ChecklistItem = copy(state = ChecklistItemState.IgnoredToday)
    fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistItemTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemTest.kt
git commit -m "feat(domain): add optional iconId to ChecklistItem

- Add ItemIconId value class for type safety
- Add iconId parameter to ChecklistItem (nullable)
- Update tests to verify icon support"
```

---

### Task 6: Update Checklist Model with New Fields

**Goal:** Add name, schedule, color, statePersistence, lastAccessedAt to Checklist

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt`
- Modify: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistTest.kt`

**Step 1: Write the failing test**

```kotlin
// Add to common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistTest.kt
@Test
fun checklist_has_name_and_visual_properties() {
    val checklist = Checklist(
        id = ChecklistId("morning"),
        name = "Morning Routine",
        schedule = ChecklistSchedule(
            daysOfWeek = emptySet(),
            timeRange = TimeRange.AllDay
        ),
        items = listOf(),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
        lastAccessedAt = null
    )

    assertEquals("Morning Routine", checklist.name)
    assertEquals(ChecklistColor.SOFT_BLUE, checklist.color)
    assertEquals(StatePersistenceDuration.FIFTEEN_MINUTES, checklist.statePersistence)
}

@Test
fun checklist_tracks_last_access_time() {
    val now = Clock.System.now()
    val checklist = Checklist(
        id = ChecklistId("test"),
        name = "Test",
        schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
        items = listOf(),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
        lastAccessedAt = now
    )

    assertEquals(now, checklist.lastAccessedAt)
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistTest
```

Expected: FAIL - constructor parameters don't match

**Step 3: Write minimal implementation**

```kotlin
// Modify common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.Instant

@JvmInline
value class ChecklistId(val value: String)

data class Checklist(
    val id: ChecklistId,
    val name: String, // NEW - was "title"
    val schedule: ChecklistSchedule, // NEW
    val items: List<ChecklistItem>,
    val color: ChecklistColor, // NEW
    val statePersistence: StatePersistenceDuration, // NEW
    val lastAccessedAt: Instant? = null // NEW
) {
    // Keep existing methods
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

**Step 4: Fix existing tests that break**

Update any existing tests in `ChecklistTest.kt` to use new constructor:

```kotlin
// Update existing test helper function if it exists
private fun createTestChecklist(
    id: String = "test",
    name: String = "Test Checklist",
    items: List<ChecklistItem> = emptyList()
) = Checklist(
    id = ChecklistId(id),
    name = name,
    schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
    items = items,
    color = ChecklistColor.SOFT_BLUE,
    statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
    lastAccessedAt = null
)
```

**Step 5: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistTest
```

Expected: PASS

**Step 6: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistTest.kt
git commit -m "feat(domain): extend Checklist model with scheduling and persistence

- Rename title to name for consistency
- Add schedule (ChecklistSchedule) for time-based activation
- Add color (ChecklistColor) for visual identification
- Add statePersistence (StatePersistenceDuration) for memory duration
- Add lastAccessedAt (Instant?) to track when user left screen
- Update all tests to use new constructor"
```

---

### Task 7: Add ChecklistActivityState Query

**Goal:** Determine if checklist is active based on current date/time

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetChecklistActivityState.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetChecklistActivityStateTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetChecklistActivityStateTest.kt
package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.*
import kotlinx.datetime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetChecklistActivityStateTest {
    @Test
    fun always_on_checklist_is_always_active() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.AllDay
            )
        )

        val monday9am = createDateTime(DayOfWeek.MONDAY, 9, 0)
        val saturday3pm = createDateTime(DayOfWeek.SATURDAY, 15, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(monday9am))
        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(saturday3pm))
    }

    @Test
    fun weekday_only_checklist_inactive_on_weekend() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                ),
                timeRange = TimeRange.AllDay
            )
        )

        val monday = createDateTime(DayOfWeek.MONDAY, 10, 0)
        val saturday = createDateTime(DayOfWeek.SATURDAY, 10, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(monday))
        assertEquals(ChecklistActivityState.Inactive, checklist.getActivityState(saturday))
    }

    @Test
    fun morning_routine_active_only_in_time_range() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.Specific(
                    startTime = LocalTime(6, 0),
                    endTime = LocalTime(9, 0)
                )
            )
        )

        val morning7am = createDateTime(DayOfWeek.MONDAY, 7, 0)
        val afternoon3pm = createDateTime(DayOfWeek.MONDAY, 15, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(morning7am))
        assertEquals(ChecklistActivityState.Inactive, checklist.getActivityState(afternoon3pm))
    }

    @Test
    fun weekday_morning_checklist_inactive_on_weekend_morning() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                timeRange = TimeRange.Specific(
                    startTime = LocalTime(6, 0),
                    endTime = LocalTime(9, 0)
                )
            )
        )

        val mondayMorning = createDateTime(DayOfWeek.MONDAY, 7, 0)
        val saturdayMorning = createDateTime(DayOfWeek.SATURDAY, 7, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(mondayMorning))
        assertEquals(ChecklistActivityState.Inactive, checklist.getActivityState(saturdayMorning))
    }

    private fun createChecklist(schedule: ChecklistSchedule) = Checklist(
        id = ChecklistId("test"),
        name = "Test",
        schedule = schedule,
        items = emptyList(),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
    )

    private fun createDateTime(dayOfWeek: DayOfWeek, hour: Int, minute: Int): LocalDateTime {
        // Find next occurrence of the day
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val targetDay = today.plus(
            (dayOfWeek.ordinal - today.dayOfWeek.ordinal + 7) % 7,
            DateTimeUnit.DAY
        )
        return LocalDateTime(
            targetDay.year,
            targetDay.month,
            targetDay.dayOfMonth,
            hour,
            minute
        )
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests GetChecklistActivityStateTest
```

Expected: FAIL - getActivityState method not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetChecklistActivityState.kt
package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.Checklist
import kotlinx.datetime.LocalDateTime

sealed class ChecklistActivityState {
    object Active : ChecklistActivityState()
    object Inactive : ChecklistActivityState()
}

fun Checklist.getActivityState(currentTime: LocalDateTime): ChecklistActivityState {
    // Check day of week if specified
    if (schedule.daysOfWeek.isNotEmpty() &&
        currentTime.dayOfWeek !in schedule.daysOfWeek
    ) {
        return ChecklistActivityState.Inactive
    }

    // Check time range
    val currentLocalTime = currentTime.time
    return if (schedule.timeRange.contains(currentLocalTime)) {
        ChecklistActivityState.Active
    } else {
        ChecklistActivityState.Inactive
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests GetChecklistActivityStateTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetChecklistActivityState.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetChecklistActivityStateTest.kt
git commit -m "feat(domain): add GetChecklistActivityState query

- Add ChecklistActivityState sealed class (Active/Inactive)
- Add getActivityState extension function on Checklist
- Check day of week if schedule has specific days
- Check time range using TimeRange.contains()
- Return Active only if both day and time match"
```

---

### Task 8: Add Event Logging Domain Models

**Goal:** Create event types for tracking user actions

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/event/ChecklistEvent.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/event/ChecklistEventTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/event/ChecklistEventTest.kt
package com.pyanpyan.domain.event

import com.pyanpyan.domain.model.ChecklistId
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistEventTest {
    @Test
    fun created_event_captures_initial_state() {
        val id = ChecklistId("morning")
        val now = Clock.System.now()

        val event = ChecklistEvent.Created(
            checklistId = id,
            timestamp = now,
            name = "Morning Routine",
            itemCount = 3
        )

        assertEquals(id, event.checklistId)
        assertEquals(now, event.timestamp)
        assertEquals("Morning Routine", event.name)
        assertEquals(3, event.itemCount)
    }

    @Test
    fun updated_event_tracks_changes() {
        val event = ChecklistEvent.Updated(
            checklistId = ChecklistId("test"),
            timestamp = Clock.System.now(),
            changes = setOf(
                ChecklistEvent.ChangeType.NAME,
                ChecklistEvent.ChangeType.COLOR
            )
        )

        assertEquals(2, event.changes.size)
        assert(event.changes.contains(ChecklistEvent.ChangeType.NAME))
    }

    @Test
    fun accessed_event_tracks_when_user_opens_checklist() {
        val event = ChecklistEvent.Accessed(
            checklistId = ChecklistId("test"),
            timestamp = Clock.System.now()
        )

        assertEquals(ChecklistId("test"), event.checklistId)
    }

    @Test
    fun deleted_event_tracks_removal() {
        val event = ChecklistEvent.Deleted(
            checklistId = ChecklistId("test"),
            timestamp = Clock.System.now()
        )

        assertEquals(ChecklistId("test"), event.checklistId)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistEventTest
```

Expected: FAIL - ChecklistEvent not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/event/ChecklistEvent.kt
package com.pyanpyan.domain.event

import com.pyanpyan.domain.model.ChecklistId
import kotlinx.datetime.Instant

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
        NAME,
        SCHEDULE,
        COLOR,
        STATE_PERSISTENCE,
        ITEMS_ADDED,
        ITEMS_REMOVED,
        ITEMS_REORDERED
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests ChecklistEventTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/event/ChecklistEvent.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/event/ChecklistEventTest.kt
git commit -m "feat(domain): add ChecklistEvent sealed class for logging

- Add Created event with name and itemCount
- Add Updated event with set of ChangeType
- Add Accessed event for tracking screen opens
- Add Deleted event for tracking removals
- Add ChangeType enum for granular change tracking"
```

---

## Phase 2: Commands for Checklist Management

### Task 9: Add CreateChecklist Command

**Goal:** Command to create new checklist with all properties

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/command/CreateChecklist.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/command/CreateChecklistTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/command/CreateChecklistTest.kt
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateChecklistTest {
    @Test
    fun creates_checklist_with_all_properties() {
        val command = CreateChecklist(
            id = ChecklistId("morning"),
            name = "Morning Routine",
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.AllDay
            ),
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("tooth"),
                    title = "Brush teeth",
                    iconId = ItemIconId("tooth"),
                    state = ChecklistItemState.Pending
                )
            ),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
        )

        val checklist = command.execute()

        assertEquals(ChecklistId("morning"), checklist.id)
        assertEquals("Morning Routine", checklist.name)
        assertEquals(ChecklistColor.SOFT_BLUE, checklist.color)
        assertEquals(1, checklist.items.size)
        assertEquals("Brush teeth", checklist.items[0].title)
    }

    @Test
    fun creates_checklist_with_specific_schedule() {
        val command = CreateChecklist(
            id = ChecklistId("morning"),
            name = "Morning Routine",
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(
                    kotlinx.datetime.DayOfWeek.MONDAY,
                    kotlinx.datetime.DayOfWeek.TUESDAY
                ),
                timeRange = TimeRange.Specific(
                    startTime = LocalTime(6, 0),
                    endTime = LocalTime(9, 0)
                )
            ),
            items = listOf(),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.ONE_HOUR
        )

        val checklist = command.execute()

        assertEquals(2, checklist.schedule.daysOfWeek.size)
        assertEquals(StatePersistenceDuration.ONE_HOUR, checklist.statePersistence)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests CreateChecklistTest
```

Expected: FAIL - CreateChecklist not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/command/CreateChecklist.kt
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*

data class CreateChecklist(
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration
) {
    fun execute(): Checklist {
        return Checklist(
            id = id,
            name = name,
            schedule = schedule,
            items = items,
            color = color,
            statePersistence = statePersistence,
            lastAccessedAt = null
        )
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests CreateChecklistTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/command/CreateChecklist.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/command/CreateChecklistTest.kt
git commit -m "feat(domain): add CreateChecklist command

- Create new checklist with all properties
- Initialize lastAccessedAt to null
- Return new Checklist instance"
```

---

### Task 10: Add UpdateChecklist Command

**Goal:** Command to update checklist properties (name, schedule, color, etc.)

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/command/UpdateChecklist.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/command/UpdateChecklistTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/command/UpdateChecklistTest.kt
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateChecklistTest {
    @Test
    fun updates_checklist_name() {
        val original = createTestChecklist(name = "Old Name")

        val command = UpdateChecklist(
            checklist = original,
            name = "New Name"
        )

        val updated = command.execute()

        assertEquals("New Name", updated.name)
        assertEquals(original.id, updated.id)
        assertEquals(original.color, updated.color)
    }

    @Test
    fun updates_checklist_color() {
        val original = createTestChecklist(color = ChecklistColor.SOFT_BLUE)

        val command = UpdateChecklist(
            checklist = original,
            color = ChecklistColor.WARM_PEACH
        )

        val updated = command.execute()

        assertEquals(ChecklistColor.WARM_PEACH, updated.color)
    }

    @Test
    fun updates_state_persistence() {
        val original = createTestChecklist(
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
        )

        val command = UpdateChecklist(
            checklist = original,
            statePersistence = StatePersistenceDuration.ONE_HOUR
        )

        val updated = command.execute()

        assertEquals(StatePersistenceDuration.ONE_HOUR, updated.statePersistence)
    }

    @Test
    fun updates_multiple_properties_at_once() {
        val original = createTestChecklist(
            name = "Old",
            color = ChecklistColor.SOFT_BLUE
        )

        val command = UpdateChecklist(
            checklist = original,
            name = "New",
            color = ChecklistColor.WARM_PEACH
        )

        val updated = command.execute()

        assertEquals("New", updated.name)
        assertEquals(ChecklistColor.WARM_PEACH, updated.color)
    }

    private fun createTestChecklist(
        name: String = "Test",
        color: ChecklistColor = ChecklistColor.SOFT_BLUE,
        statePersistence: StatePersistenceDuration = StatePersistenceDuration.FIFTEEN_MINUTES
    ) = Checklist(
        id = ChecklistId("test"),
        name = name,
        schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
        items = emptyList(),
        color = color,
        statePersistence = statePersistence
    )
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew common:testDebugUnitTest --tests UpdateChecklistTest
```

Expected: FAIL - UpdateChecklist not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/command/UpdateChecklist.kt
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*

data class UpdateChecklist(
    val checklist: Checklist,
    val name: String? = null,
    val schedule: ChecklistSchedule? = null,
    val color: ChecklistColor? = null,
    val statePersistence: StatePersistenceDuration? = null
) {
    fun execute(): Checklist {
        return checklist.copy(
            name = name ?: checklist.name,
            schedule = schedule ?: checklist.schedule,
            color = color ?: checklist.color,
            statePersistence = statePersistence ?: checklist.statePersistence
        )
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew common:testDebugUnitTest --tests UpdateChecklistTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/command/UpdateChecklist.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/command/UpdateChecklistTest.kt
git commit -m "feat(domain): add UpdateChecklist command

- Update checklist properties with optional parameters
- Only update provided fields (null = keep existing)
- Support name, schedule, color, statePersistence updates"
```

---

## Phase 3: Slider Component

### Task 11: Create SliderState Sealed Class

**Goal:** Model slider position states (Center, Left, Right)

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/SliderState.kt`
- Create: `androidApp/src/test/kotlin/com/pyanpyan/android/ui/components/SliderStateTest.kt`

**Step 1: Write the failing test**

```kotlin
// androidApp/src/test/kotlin/com/pyanpyan/android/ui/components/SliderStateTest.kt
package com.pyanpyan.android.ui.components

import org.junit.Test
import org.junit.Assert.*

class SliderStateTest {
    @Test
    fun center_is_pending_state() {
        val state = SliderState.Center
        assertFalse(state.isCommitted)
    }

    @Test
    fun left_is_skip_committed_state() {
        val state = SliderState.Left
        assertTrue(state.isCommitted)
    }

    @Test
    fun right_is_done_committed_state() {
        val state = SliderState.Right
        assertTrue(state.isCommitted)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew androidApp:testDebugUnitTest --tests SliderStateTest
```

Expected: FAIL - SliderState not found

**Step 3: Write minimal implementation**

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/SliderState.kt
package com.pyanpyan.android.ui.components

sealed class SliderState {
    abstract val isCommitted: Boolean

    object Center : SliderState() {
        override val isCommitted = false
    }

    object Left : SliderState() {
        override val isCommitted = true
    }

    object Right : SliderState() {
        override val isCommitted = true
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew androidApp:testDebugUnitTest --tests SliderStateTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/SliderState.kt \
        androidApp/src/test/kotlin/com/pyanpyan/android/ui/components/SliderStateTest.kt
git commit -m "feat(ui): add SliderState sealed class

- Add Center state for pending (not committed)
- Add Left state for skip (committed)
- Add Right state for done (committed)
- Add isCommitted property to distinguish pending from locked"
```

---

### Task 12: Create ItemSlider Composable

**Goal:** Build interactive slider component with drag mechanics

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt`

**Step 1: Write the implementation** (UI components don't have traditional unit tests)

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt
package com.pyanpyan.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ItemSlider(
    state: SliderState,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Track dimensions
    var trackWidth by remember { mutableStateOf(0f) }

    // Offset state
    val offsetX = remember { Animatable(0f) }

    // Initial position based on state
    LaunchedEffect(state) {
        val target = when (state) {
            SliderState.Center -> 0f
            SliderState.Left -> -trackWidth / 2
            SliderState.Right -> trackWidth / 2
        }
        offsetX.snapTo(target)
    }

    // Drag threshold (70%)
    val threshold = trackWidth * 0.35f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(enabled, state) {
                if (!enabled || state.isCommitted) return@pointerInput

                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val currentOffset = offsetX.value

                            when {
                                currentOffset < -threshold -> {
                                    // Snap to left
                                    offsetX.animateTo(
                                        -trackWidth / 2,
                                        animationSpec = tween(300)
                                    )
                                    onSkip()
                                }
                                currentOffset > threshold -> {
                                    // Snap to right
                                    offsetX.animateTo(
                                        trackWidth / 2,
                                        animationSpec = tween(300)
                                    )
                                    onDone()
                                }
                                else -> {
                                    // Spring back to center
                                    offsetX.animateTo(
                                        0f,
                                        animationSpec = tween(300)
                                    )
                                }
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount)
                                .coerceIn(-trackWidth / 2, trackWidth / 2)
                            offsetX.snapTo(newValue)
                        }
                    }
                )
            }
    ) {
        // Measure track width
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            trackWidth = with(density) { constraints.maxWidth.toFloat() }

            // Left label (Skip)
            Text(
                text = "Skip",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (offsetX.value < 0) 0.8f else 0.3f
                ),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            )

            // Right label (Done)
            Text(
                text = "Done",
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (offsetX.value > 0) 0.8f else 0.3f
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )

            // Thumb
            val thumbX = with(density) {
                (offsetX.value + trackWidth / 2).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbX - 24.dp)
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            offsetX.value < -threshold -> MaterialTheme.colorScheme.onSurfaceVariant
                            offsetX.value > threshold -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
            )
        }
    }
}
```

**Step 2: Manually test in app**

Will test in actual UI once integrated into ChecklistScreen.

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/components/ItemSlider.kt
git commit -m "feat(ui): add ItemSlider composable with drag mechanics

- Implement horizontal drag gesture detection
- Add 70% threshold for committing action
- Spring back to center if released before threshold
- Animate snap to left (skip) or right (done)
- Lock slider when state is committed
- Fade labels based on thumb position
- Change thumb color based on position"
```

---

## Phase 4: Update ViewModel & Detail Screen

### Task 13: Update ChecklistViewModel for New Model

**Goal:** Update ViewModel to work with new Checklist model

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`

**Step 1: Update mock data to use new model**

```kotlin
// Modify the loadChecklist() function in ChecklistViewModel.kt
private fun loadChecklist() {
    viewModelScope.launch {
        // Temporary mock data - will be replaced with repository
        val mockChecklist = Checklist(
            id = ChecklistId("morning-routine"),
            name = "Morning Routine", // Changed from title
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.AllDay
            ),
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("brush-teeth"),
                    title = "Brush teeth",
                    iconId = ItemIconId("tooth"),
                    state = ChecklistItemState.Pending
                ),
                ChecklistItem(
                    id = ChecklistItemId("get-dressed"),
                    title = "Get dressed",
                    iconId = ItemIconId("shirt"),
                    state = ChecklistItemState.Pending
                )
            ),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
        )

        _uiState.value = ChecklistUiState(checklist = mockChecklist)
    }
}
```

**Step 2: Build and verify**

```bash
./gradlew androidApp:assembleDebug
```

Expected: SUCCESS (compiles without errors)

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git commit -m "refactor(ui): update ChecklistViewModel for new Checklist model

- Update mock checklist to use name instead of title
- Add schedule, color, and statePersistence fields
- Add iconId to mock items
- Use new domain model structure"
```

---

### Task 14: Update ChecklistScreen to Use Slider

**Goal:** Replace button row with ItemSlider component

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`

**Step 1: Update imports and ChecklistItemRow**

```kotlin
// Update ChecklistItemRow in ChecklistScreen.kt
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onMarkDone: () -> Unit,
    onIgnoreToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.state) {
                ChecklistItemState.Done -> MaterialTheme.colorScheme.primaryContainer
                ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ChecklistItemState.Pending -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Item title with optional icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // TODO: Add icon rendering when icon loader is implemented
                item.iconId?.let {
                    // Placeholder for icon
                    Text(
                        text = "🔹",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = when (item.state) {
                        ChecklistItemState.Done -> TextDecoration.LineThrough
                        else -> null
                    },
                    color = when (item.state) {
                        ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Slider or status text
            when (item.state) {
                ChecklistItemState.Pending -> {
                    ItemSlider(
                        state = SliderState.Center,
                        onSkip = onIgnoreToday,
                        onDone = onMarkDone,
                        enabled = true
                    )
                }
                ChecklistItemState.Done -> {
                    Column {
                        ItemSlider(
                            state = SliderState.Right,
                            onSkip = {},
                            onDone = {},
                            enabled = false
                        )
                        Text(
                            text = "✓ Completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                ChecklistItemState.IgnoredToday -> {
                    Column {
                        ItemSlider(
                            state = SliderState.Left,
                            onSkip = {},
                            onDone = {},
                            enabled = false
                        )
                        Text(
                            text = "⊘ Skipped today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
```

**Step 2: Add import for ItemSlider**

```kotlin
// Add to imports at top of ChecklistScreen.kt
import com.pyanpyan.android.ui.components.ItemSlider
import com.pyanpyan.android.ui.components.SliderState
```

**Step 3: Update ChecklistScreen to show checklist name**

```kotlin
// Update in ChecklistScreen composable
uiState.checklist?.let { checklist ->
    Text(
        text = checklist.name, // Changed from checklist.title
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    // ... rest of code
}
```

**Step 4: Build and install on emulator**

```bash
./gradlew androidApp:installDebug
```

Expected: SUCCESS, app installs with slider UI

**Step 5: Manual test**
- Open app
- Try dragging sliders
- Verify threshold behavior
- Verify locked sliders for done/skipped items

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git commit -m "feat(ui): replace buttons with slider controls

- Replace Done/Skip buttons with ItemSlider component
- Show slider at center for pending items (interactive)
- Show slider at right for done items (locked)
- Show slider at left for skipped items (locked)
- Add status text below locked sliders
- Add placeholder icon support (to be implemented)
- Update to use checklist.name instead of title"
```

---

## Phase 5: Home Screen with Checklist Library

### Task 15: Create ChecklistLibraryViewModel

**Goal:** ViewModel for managing list of checklists and filtering by active/inactive

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt`

**Step 1: Write the implementation**

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt
package com.pyanpyan.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.query.ChecklistActivityState
import com.pyanpyan.domain.query.getActivityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ChecklistLibraryUiState(
    val activeChecklists: List<Checklist> = emptyList(),
    val inactiveChecklists: List<Checklist> = emptyList(),
    val isLoading: Boolean = false
)

class ChecklistLibraryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistLibraryUiState())
    val uiState: StateFlow<ChecklistLibraryUiState> = _uiState.asStateFlow()

    init {
        loadChecklists()
    }

    private fun loadChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Mock data - will be replaced with repository
            val allChecklists = listOf(
                createMockChecklist(
                    id = "morning",
                    name = "Morning Routine",
                    color = ChecklistColor.SOFT_BLUE,
                    itemCount = 5
                ),
                createMockChecklist(
                    id = "workout",
                    name = "Quick Workout",
                    color = ChecklistColor.CALM_GREEN,
                    itemCount = 3
                ),
                createMockChecklist(
                    id = "evening",
                    name = "Evening Wind-down",
                    color = ChecklistColor.GENTLE_PURPLE,
                    schedule = ChecklistSchedule(
                        daysOfWeek = emptySet(),
                        timeRange = TimeRange.Specific(
                            startTime = kotlinx.datetime.LocalTime(18, 0),
                            endTime = kotlinx.datetime.LocalTime(21, 0)
                        )
                    ),
                    itemCount = 4
                ),
                createMockChecklist(
                    id = "weekend",
                    name = "Weekend Projects",
                    color = ChecklistColor.WARM_PEACH,
                    schedule = ChecklistSchedule(
                        daysOfWeek = setOf(
                            kotlinx.datetime.DayOfWeek.SATURDAY,
                            kotlinx.datetime.DayOfWeek.SUNDAY
                        ),
                        timeRange = TimeRange.AllDay
                    ),
                    itemCount = 2
                )
            )

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
    }

    private fun createMockChecklist(
        id: String,
        name: String,
        color: ChecklistColor,
        schedule: ChecklistSchedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
        itemCount: Int
    ): Checklist {
        val items = (1..itemCount).map { i ->
            ChecklistItem(
                id = ChecklistItemId("$id-item-$i"),
                title = "Task $i",
                state = ChecklistItemState.Pending
            )
        }

        return Checklist(
            id = ChecklistId(id),
            name = name,
            schedule = schedule,
            items = items,
            color = color,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
        )
    }

    fun deleteChecklist(checklistId: ChecklistId) {
        // TODO: Implement delete with repository
        viewModelScope.launch {
            loadChecklists() // Reload for now
        }
    }
}
```

**Step 2: Build to verify**

```bash
./gradlew androidApp:assembleDebug
```

Expected: SUCCESS

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt
git commit -m "feat(ui): add ChecklistLibraryViewModel

- Add ChecklistLibraryUiState with active/inactive lists
- Filter checklists by activity state using current time
- Sort both lists alphabetically by name
- Create mock checklists with different schedules
- Add deleteChecklist method stub"
```

---

### Task 16: Create ChecklistLibraryScreen

**Goal:** Screen showing active and inactive checklists

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`

**Step 1: Write the implementation**

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
package com.pyanpyan.android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistLibraryScreen(
    onChecklistClick: (ChecklistId) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: ChecklistLibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklists") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create checklist")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Active checklists
            if (uiState.activeChecklists.isNotEmpty()) {
                items(uiState.activeChecklists) { checklist ->
                    ChecklistCard(
                        checklist = checklist,
                        isActive = true,
                        onClick = { onChecklistClick(checklist.id) }
                    )
                }
            }

            // Separator if both sections present
            if (uiState.activeChecklists.isNotEmpty() && uiState.inactiveChecklists.isNotEmpty()) {
                item {
                    Divider(
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
                        onClick = { onChecklistClick(checklist.id) }
                    )
                }
            }

            // Empty state
            if (uiState.activeChecklists.isEmpty() && uiState.inactiveChecklists.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No checklists yet.\nTap + to create one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistCard(
    checklist: Checklist,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(checklist.color.hex.toColorInt()))
            )

            // Checklist info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = checklist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isActive) {
                        val pendingCount = checklist.items.count {
                            it.state == com.pyanpyan.domain.model.ChecklistItemState.Pending
                        }
                        if (pendingCount == 0) "All done!" else "$pendingCount pending"
                    } else {
                        getScheduleDescription(checklist)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}

private fun getScheduleDescription(checklist: Checklist): String {
    val schedule = checklist.schedule

    return buildString {
        if (schedule.daysOfWeek.isNotEmpty()) {
            val days = schedule.daysOfWeek.sortedBy { it.ordinal }
            if (days.size == 7) {
                append("Every day")
            } else {
                append(days.joinToString(", ") {
                    it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
                })
            }
        }

        when (val timeRange = schedule.timeRange) {
            is com.pyanpyan.domain.model.TimeRange.Specific -> {
                if (isNotEmpty()) append(" ")
                append("${timeRange.startTime}-${timeRange.endTime}")
            }
            else -> {}
        }

        if (isEmpty()) {
            append("Inactive")
        }
    }
}
```

**Step 2: Build to verify**

```bash
./gradlew androidApp:assembleDebug
```

Expected: SUCCESS

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat(ui): add ChecklistLibraryScreen

- Show active checklists at top with full opacity
- Show inactive checklists below separator with reduced opacity
- Display colored accent bar on left using checklist color
- Show pending count for active checklists
- Show schedule description for inactive checklists
- Add FAB for creating new checklist
- Add empty state message"
```

---

### Task 17: Update MainActivity with Navigation

**Goal:** Add simple navigation between library and detail screens

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`

**Step 1: Add navigation state**

```kotlin
// Modify MainActivity.kt
package com.pyanpyan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pyanpyan.android.ui.checklist.ChecklistScreen
import com.pyanpyan.android.ui.library.ChecklistLibraryScreen
import com.pyanpyan.android.ui.theme.PyanpyanTheme
import com.pyanpyan.domain.model.ChecklistId

sealed class Screen {
    object Library : Screen()
    data class ChecklistDetail(val checklistId: ChecklistId) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                    // TODO: Navigate to create screen
                                }
                            )
                        }
                        is Screen.ChecklistDetail -> {
                            ChecklistScreen(
                                checklistId = screen.checklistId,
                                onBackClick = {
                                    currentScreen = Screen.Library
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Update ChecklistScreen to accept checklistId and back navigation**

```kotlin
// Modify ChecklistScreen.kt signature
@Composable
fun ChecklistScreen(
    checklistId: ChecklistId,
    onBackClick: () -> Unit,
    viewModel: ChecklistViewModel = viewModel()
) {
    // Add TopAppBar with back button
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
            androidx.compose.material3.TopAppBar(
                title = { Text("Checklist") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBackClick) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

            // Existing content
            val uiState by viewModel.uiState.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ... rest of existing code
            }
        }
    }
}
```

**Step 3: Add import for ArrowBack icon**

```kotlin
// Add to imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
```

**Step 4: Build and install**

```bash
./gradlew androidApp:installDebug
```

Expected: SUCCESS

**Step 5: Manual test**
- Open app, should show library screen
- Tap checklist, should navigate to detail
- Tap back arrow, should return to library

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt \
        androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git commit -m "feat(ui): add navigation between library and detail screens

- Add Screen sealed class for navigation state
- Add ChecklistLibraryScreen as home screen
- Add back navigation from ChecklistScreen
- Add TopAppBar with back button to ChecklistScreen
- Pass checklistId to ChecklistScreen (not used yet)
- Add TODO for create screen navigation"
```

---

## Phase 6: Next Steps (Not Included in This Plan)

The following features are designed but not yet implemented:

1. **Checklist Editor Screen** - Full CRUD for checklist properties
2. **Icon Picker** - Load icons from JSON and select in editor
3. **State Persistence Logic** - Track lastAccessedAt and reset based on duration
4. **Event Logging Integration** - Log all user actions
5. **Repository Layer** - Replace mock data with actual persistence
6. **Timer Tab** - Design and implement timer library

---

## Testing Strategy

### Unit Tests Written
- TimeRange (AllDay, Specific, contains)
- ChecklistSchedule (isAlwaysOn, day filtering)
- ChecklistColor (8 colors)
- StatePersistenceDuration (6 durations)
- ChecklistItem (with iconId)
- GetChecklistActivityState (activity filtering)
- ChecklistEvent (all event types)
- CreateChecklist command
- UpdateChecklist command

### Manual Testing Required
- Slider drag interaction and thresholds
- Slider animations (spring back, snap to side)
- Navigation between screens
- Active/inactive filtering in library
- Color accent rendering
- Schedule description formatting

### Integration Testing (Future)
- Repository with real persistence
- Event log storage and retrieval
- State persistence timer
- Icon loading from JSON

---

## Rollout Plan

1. **Phase 1-2 Complete** - Domain models and commands ready
2. **Phase 3-4 Complete** - Slider UI and updated detail screen
3. **Phase 5 Complete** - Library screen with navigation
4. **Phase 6** - Editor screen (separate plan)
5. **Phase 7** - Persistence layer (separate plan)
6. **Phase 8** - Event logging integration (separate plan)
7. **Phase 9** - Timer features (separate plan)

---

## Success Criteria

- [x] Can create checklists with name, schedule, color, items
- [x] Sliders replace buttons in checklist items
- [x] Active/inactive filtering based on current time
- [x] Library screen shows all checklists sorted
- [x] Can navigate between library and detail
- [ ] Can create new checklists (editor screen)
- [ ] Can edit existing checklists
- [ ] Can delete checklists
- [ ] State persistence respects duration setting
- [ ] All user actions are logged as events

---

## File Structure Summary

```
common/src/commonMain/kotlin/com/pyanpyan/domain/
├── model/
│   ├── TimeRange.kt (NEW)
│   ├── ChecklistSchedule.kt (NEW)
│   ├── ChecklistColor.kt (NEW)
│   ├── StatePersistenceDuration.kt (NEW)
│   ├── ChecklistItem.kt (MODIFIED - added iconId)
│   └── Checklist.kt (MODIFIED - added schedule, color, etc.)
├── command/
│   ├── CreateChecklist.kt (NEW)
│   ├── UpdateChecklist.kt (NEW)
│   ├── MarkItemDone.kt (EXISTING)
│   └── IgnoreItemToday.kt (EXISTING)
├── query/
│   └── GetChecklistActivityState.kt (NEW)
└── event/
    └── ChecklistEvent.kt (NEW)

androidApp/src/main/kotlin/com/pyanpyan/android/
├── MainActivity.kt (MODIFIED - added navigation)
├── ui/
│   ├── components/
│   │   ├── SliderState.kt (NEW)
│   │   └── ItemSlider.kt (NEW)
│   ├── library/
│   │   ├── ChecklistLibraryViewModel.kt (NEW)
│   │   └── ChecklistLibraryScreen.kt (NEW)
│   └── checklist/
│       ├── ChecklistViewModel.kt (MODIFIED)
│       └── ChecklistScreen.kt (MODIFIED - using slider)
```
