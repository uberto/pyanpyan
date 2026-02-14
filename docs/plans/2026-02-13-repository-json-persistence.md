# Repository JSON Persistence Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Implement repository layer with local JSON file persistence using kondor-json and kondor-outcome

**Architecture:** Kotlin Multiplatform repository interface in common module, Android-specific implementation using internal storage with kondor-json for serialization, kondor-outcome for error handling, and automatic "School" checklist creation on first run.

**Tech Stack:** Kondor-json, Kondor-outcome, kotlinx-coroutines, kotlinx-datetime, Android Context.filesDir

---

## Phase 1: Dependencies

### Task 1: Add Dependencies

**Files:**
- Modify: `common/build.gradle.kts`
- Modify: `androidApp/build.gradle.kts`

**Step 1: Add kondor-json and kondor-outcome to common module**

Update `common/build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...

    // Kondor JSON and Outcome
    implementation("com.ubertob.kondor:kondor-core:2.3.2")
    implementation("com.ubertob.kondor:kondor-outcome:2.3.2")

    // Existing kotlinx-datetime should already be present
}
```

**Step 2: Build to verify dependencies resolve**

Run: `./gradlew common:build`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add common/build.gradle.kts
git commit -m "deps: add kondor-json and kondor-outcome to common module

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 2: Repository Interface (Common Module)

### Task 2: Create RepositoryError Sealed Class

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/RepositoryError.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/RepositoryErrorTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/repository/RepositoryErrorTest.kt
package com.pyanpyan.domain.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RepositoryErrorTest {
    @Test
    fun file_read_error_captures_message_and_cause() {
        val cause = RuntimeException("IO failed")
        val error = RepositoryError.FileReadError("Failed to read", cause)

        assertEquals("Failed to read", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun file_write_error_captures_message() {
        val error = RepositoryError.FileWriteError("Write failed", null)

        assertEquals("Write failed", error.message)
    }

    @Test
    fun json_parse_error_captures_message() {
        val error = RepositoryError.JsonParseError("Invalid JSON", null)

        assertEquals("Invalid JSON", error.message)
    }

    @Test
    fun invalid_data_error_captures_message() {
        val error = RepositoryError.InvalidDataError("Bad data")

        assertEquals("Bad data", error.message)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew common:testDebugUnitTest --tests RepositoryErrorTest`
Expected: FAIL - RepositoryError not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/repository/RepositoryError.kt
package com.pyanpyan.domain.repository

sealed class RepositoryError {
    data class FileReadError(
        val message: String,
        val cause: Throwable? = null
    ) : RepositoryError()

    data class FileWriteError(
        val message: String,
        val cause: Throwable? = null
    ) : RepositoryError()

    data class JsonParseError(
        val message: String,
        val cause: Throwable? = null
    ) : RepositoryError()

    data class InvalidDataError(
        val message: String
    ) : RepositoryError()
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew common:testDebugUnitTest --tests RepositoryErrorTest`
Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/RepositoryError.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/RepositoryErrorTest.kt
git commit -m "feat(domain): add RepositoryError sealed class

- Add FileReadError for read failures
- Add FileWriteError for write failures
- Add JsonParseError for deserialization failures
- Add InvalidDataError for validation failures

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 3: Create ChecklistRepository Interface

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/ChecklistRepository.kt`

**Step 1: Write the interface**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/repository/ChecklistRepository.kt
package com.pyanpyan.domain.repository

import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId
import com.uberto.kondor.outcome.Outcome

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

**Step 2: Build to verify it compiles**

Run: `./gradlew common:build`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/ChecklistRepository.kt
git commit -m "feat(domain): add ChecklistRepository interface

- Add CRUD operations with Outcome error handling
- Add export/import methods for backup
- Use suspend functions for async I/O
- Document behavior for missing data and errors

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 4: Create DefaultData Object

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/DefaultData.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/DefaultDataTest.kt`

**Step 1: Write the failing test**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/DefaultDataTest.kt
package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDataTest {
    @Test
    fun creates_school_checklist() {
        val checklists = DefaultData.createDefaultChecklists()

        assertEquals(1, checklists.size)

        val school = checklists[0]
        assertEquals(ChecklistId("school"), school.id)
        assertEquals("School", school.name)
        assertEquals(ChecklistColor.SOFT_BLUE, school.color)
        assertEquals(StatePersistenceDuration.FIFTEEN_MINUTES, school.statePersistence)
    }

    @Test
    fun school_checklist_has_five_items() {
        val school = DefaultData.createDefaultChecklists()[0]

        assertEquals(5, school.items.size)
        assertEquals("Books in bag", school.items[0].title)
        assertEquals("Homework", school.items[1].title)
        assertEquals("PE kit", school.items[2].title)
        assertEquals("Breakfast", school.items[3].title)
        assertEquals("Brushing teeth", school.items[4].title)
    }

    @Test
    fun school_checklist_is_weekdays_only() {
        val school = DefaultData.createDefaultChecklists()[0]

        assertEquals(5, school.schedule.daysOfWeek.size)
        assert(school.schedule.daysOfWeek.contains(DayOfWeek.MONDAY))
        assert(school.schedule.daysOfWeek.contains(DayOfWeek.FRIDAY))
        assert(!school.schedule.daysOfWeek.contains(DayOfWeek.SATURDAY))
        assert(!school.schedule.daysOfWeek.contains(DayOfWeek.SUNDAY))
    }

    @Test
    fun school_checklist_is_all_day() {
        val school = DefaultData.createDefaultChecklists()[0]

        assertEquals(TimeRange.AllDay, school.schedule.timeRange)
    }

    @Test
    fun all_items_start_as_pending() {
        val school = DefaultData.createDefaultChecklists()[0]

        assert(school.items.all { it.state == ChecklistItemState.Pending })
    }

    @Test
    fun brushing_teeth_has_tooth_icon() {
        val school = DefaultData.createDefaultChecklists()[0]
        val brushingTeeth = school.items[4]

        assertEquals(ItemIconId("tooth"), brushingTeeth.iconId)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew common:testDebugUnitTest --tests DefaultDataTest`
Expected: FAIL - DefaultData not found

**Step 3: Write minimal implementation**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/DefaultData.kt
package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import kotlinx.datetime.DayOfWeek

object DefaultData {
    fun createDefaultChecklists(): List<Checklist> = listOf(
        createSchoolChecklist()
    )

    private fun createSchoolChecklist() = Checklist(
        id = ChecklistId("school"),
        name = "School",
        schedule = ChecklistSchedule(
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            timeRange = TimeRange.AllDay
        ),
        items = listOf(
            ChecklistItem(
                id = ChecklistItemId("books"),
                title = "Books in bag",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("homework"),
                title = "Homework",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("pe-kit"),
                title = "PE kit",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("breakfast"),
                title = "Breakfast",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("brushing-teeth"),
                title = "Brushing teeth",
                iconId = ItemIconId("tooth"),
                state = ChecklistItemState.Pending
            )
        ),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
        lastAccessedAt = null
    )
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew common:testDebugUnitTest --tests DefaultDataTest`
Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/DefaultData.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/DefaultDataTest.kt
git commit -m "feat(domain): add DefaultData with School checklist

- Create School checklist for first app run
- 5 items: Books, Homework, PE kit, Breakfast, Brushing teeth
- Weekdays only (Mon-Fri), all day
- Brushing teeth has tooth icon
- All items start as Pending

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 3: Kondor JSON Codecs (Common Module)

### Task 5: Create Value Class Codecs

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 1: Write failing tests for value class codecs**

```kotlin
// common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt
package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.uberto.kondor.outcome.orThrow
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistCodecsTest {

    @Test
    fun checklist_id_roundtrip() {
        val original = ChecklistId("test-123")
        val json = JChecklistId.toJson(original)
        val decoded = JChecklistId.fromJson(json).orThrow()

        assertEquals(original, decoded)
    }

    @Test
    fun checklist_item_id_roundtrip() {
        val original = ChecklistItemId("item-456")
        val json = JChecklistItemId.toJson(original)
        val decoded = JChecklistItemId.fromJson(json).orThrow()

        assertEquals(original, decoded)
    }

    @Test
    fun item_icon_id_roundtrip() {
        val original = ItemIconId("tooth")
        val json = JItemIconId.toJson(original)
        val decoded = JItemIconId.fromJson(json).orThrow()

        assertEquals(original, decoded)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: FAIL - JChecklistId not found

**Step 3: Write minimal implementation for value classes**

```kotlin
// common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt
package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.uberto.kondor.json.*

// Value class codecs
object JChecklistId : JStringRepresentable<ChecklistId>() {
    override val cons = ::ChecklistId
    override fun fromString(str: String) = ChecklistId(str)
    override fun toString(value: ChecklistId) = value.value
}

object JChecklistItemId : JStringRepresentable<ChecklistItemId>() {
    override val cons = ::ChecklistItemId
    override fun fromString(str: String) = ChecklistItemId(str)
    override fun toString(value: ChecklistItemId) = value.value
}

object JItemIconId : JStringRepresentable<ItemIconId>() {
    override val cons = ::ItemIconId
    override fun fromString(str: String) = ItemIconId(str)
    override fun toString(value: ItemIconId) = value.value
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt
git commit -m "feat(domain): add kondor codecs for value classes

- Add JChecklistId codec
- Add JChecklistItemId codec
- Add JItemIconId codec
- All codecs tested with roundtrip serialization

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 6: Create Enum and DateTime Codecs

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`
- Modify: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 1: Add failing tests for enums and datetime**

Add to `ChecklistCodecsTest.kt`:

```kotlin
@Test
fun checklist_color_roundtrip() {
    val original = ChecklistColor.CALM_GREEN
    val json = JChecklistColor.toJson(original)
    val decoded = JChecklistColor.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun state_persistence_duration_roundtrip() {
    val original = StatePersistenceDuration.ONE_HOUR
    val json = JStatePersistenceDuration.toJson(original)
    val decoded = JStatePersistenceDuration.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun day_of_week_roundtrip() {
    val original = kotlinx.datetime.DayOfWeek.WEDNESDAY
    val json = JDayOfWeek.toJson(original)
    val decoded = JDayOfWeek.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun local_time_roundtrip() {
    val original = kotlinx.datetime.LocalTime(14, 30)
    val json = JLocalTime.toJson(original)
    val decoded = JLocalTime.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun instant_roundtrip() {
    val original = kotlinx.datetime.Instant.parse("2024-01-15T10:30:00Z")
    val json = JInstant.toJson(original)
    val decoded = JInstant.fromJson(json).orThrow()

    assertEquals(original, decoded)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: FAIL - enum/datetime codecs not found

**Step 3: Add enum and datetime codecs**

Add to `ChecklistCodecs.kt`:

```kotlin
import kotlinx.datetime.*

// Enum codecs
object JChecklistColor : JStringRepresentable<ChecklistColor>() {
    override val cons = { ChecklistColor.valueOf(it) }
    override fun fromString(str: String) = ChecklistColor.valueOf(str)
    override fun toString(value: ChecklistColor) = value.name
}

object JStatePersistenceDuration : JStringRepresentable<StatePersistenceDuration>() {
    override val cons = { StatePersistenceDuration.valueOf(it) }
    override fun fromString(str: String) = StatePersistenceDuration.valueOf(str)
    override fun toString(value: StatePersistenceDuration) = value.name
}

object JDayOfWeek : JStringRepresentable<DayOfWeek>() {
    override val cons = { DayOfWeek.valueOf(it) }
    override fun fromString(str: String) = DayOfWeek.valueOf(str)
    override fun toString(value: DayOfWeek) = value.name
}

// DateTime codecs
object JLocalTime : JStringRepresentable<LocalTime>() {
    override val cons = { LocalTime.parse(it) }
    override fun fromString(str: String) = LocalTime.parse(str)
    override fun toString(value: LocalTime) = value.toString()
}

object JInstant : JStringRepresentable<Instant>() {
    override val cons = { Instant.parse(it) }
    override fun fromString(str: String) = Instant.parse(it)
    override fun toString(value: Instant) = value.toString()
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt
git commit -m "feat(domain): add kondor codecs for enums and datetime

- Add JChecklistColor, JStatePersistenceDuration, JDayOfWeek
- Add JLocalTime and JInstant for kotlinx-datetime
- All codecs tested with roundtrip serialization

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 7: Create Sealed Class Codecs

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`
- Modify: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 1: Add failing tests for sealed classes**

Add to `ChecklistCodecsTest.kt`:

```kotlin
@Test
fun timerange_allday_roundtrip() {
    val original = TimeRange.AllDay
    val json = JTimeRange.toJson(original)
    val decoded = JTimeRange.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun timerange_specific_roundtrip() {
    val original = TimeRange.Specific(
        startTime = kotlinx.datetime.LocalTime(8, 30),
        endTime = kotlinx.datetime.LocalTime(16, 45)
    )
    val json = JTimeRange.toJson(original)
    val decoded = JTimeRange.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun checklist_item_state_pending_roundtrip() {
    val original = ChecklistItemState.Pending
    val json = JChecklistItemState.toJson(original)
    val decoded = JChecklistItemState.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun checklist_item_state_done_roundtrip() {
    val original = ChecklistItemState.Done
    val json = JChecklistItemState.toJson(original)
    val decoded = JChecklistItemState.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun checklist_item_state_ignored_roundtrip() {
    val original = ChecklistItemState.IgnoredToday
    val json = JChecklistItemState.toJson(original)
    val decoded = JChecklistItemState.fromJson(json).orThrow()

    assertEquals(original, decoded)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: FAIL - sealed class codecs not found

**Step 3: Add sealed class codecs**

Add to `ChecklistCodecs.kt`:

```kotlin
// TimeRange sealed class
object JTimeRange : JSealed<TimeRange>() {
    override val subtypes = setOf(JAllDay, JSpecificTime)
    override fun extractTypeName(obj: TimeRange) = when (obj) {
        is TimeRange.AllDay -> "AllDay"
        is TimeRange.Specific -> "Specific"
    }
}

object JAllDay : JAny<TimeRange.AllDay>() {
    override fun JsonNodeObject.deserializeOrThrow() = TimeRange.AllDay
}

object JSpecificTime : JAny<TimeRange.Specific>() {
    val start_time by str(JLocalTime, TimeRange.Specific::startTime)
    val end_time by str(JLocalTime, TimeRange.Specific::endTime)

    override fun JsonNodeObject.deserializeOrThrow() =
        TimeRange.Specific(+start_time, +end_time)
}

// ChecklistItemState sealed interface
object JChecklistItemState : JSealed<ChecklistItemState>() {
    override val subtypes = setOf(JPending, JDone, JIgnoredToday)
    override fun extractTypeName(obj: ChecklistItemState) = when (obj) {
        ChecklistItemState.Pending -> "Pending"
        ChecklistItemState.Done -> "Done"
        ChecklistItemState.IgnoredToday -> "IgnoredToday"
    }
}

object JPending : JAny<ChecklistItemState.Pending>() {
    override fun JsonNodeObject.deserializeOrThrow() = ChecklistItemState.Pending
}

object JDone : JAny<ChecklistItemState.Done>() {
    override fun JsonNodeObject.deserializeOrThrow() = ChecklistItemState.Done
}

object JIgnoredToday : JAny<ChecklistItemState.IgnoredToday>() {
    override fun JsonNodeObject.deserializeOrThrow() = ChecklistItemState.IgnoredToday
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt
git commit -m "feat(domain): add kondor codecs for sealed classes

- Add JTimeRange with JAllDay and JSpecificTime
- Add JChecklistItemState with JPending, JDone, JIgnoredToday
- All sealed class variants tested with roundtrip

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 8: Create Complex Object Codecs

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`
- Modify: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 1: Add failing tests for complex objects**

Add to `ChecklistCodecsTest.kt`:

```kotlin
@Test
fun checklist_schedule_roundtrip() {
    val original = ChecklistSchedule(
        daysOfWeek = setOf(
            kotlinx.datetime.DayOfWeek.MONDAY,
            kotlinx.datetime.DayOfWeek.WEDNESDAY,
            kotlinx.datetime.DayOfWeek.FRIDAY
        ),
        timeRange = TimeRange.Specific(
            startTime = kotlinx.datetime.LocalTime(6, 0),
            endTime = kotlinx.datetime.LocalTime(9, 0)
        )
    )

    val json = JChecklistSchedule.toJson(original)
    val decoded = JChecklistSchedule.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun checklist_item_roundtrip() {
    val original = ChecklistItem(
        id = ChecklistItemId("test-item"),
        title = "Test Item",
        iconId = ItemIconId("icon-1"),
        state = ChecklistItemState.Done
    )

    val json = JChecklistItem.toJson(original)
    val decoded = JChecklistItem.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun checklist_item_with_null_icon_roundtrip() {
    val original = ChecklistItem(
        id = ChecklistItemId("test"),
        title = "Item",
        iconId = null,
        state = ChecklistItemState.Pending
    )

    val json = JChecklistItem.toJson(original)
    val decoded = JChecklistItem.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun checklist_roundtrip() {
    val original = Checklist(
        id = ChecklistId("test-id"),
        name = "Test Checklist",
        schedule = ChecklistSchedule(
            daysOfWeek = setOf(kotlinx.datetime.DayOfWeek.MONDAY),
            timeRange = TimeRange.AllDay
        ),
        items = listOf(
            ChecklistItem(
                id = ChecklistItemId("item-1"),
                title = "First Item",
                iconId = ItemIconId("icon-1"),
                state = ChecklistItemState.Done
            )
        ),
        color = ChecklistColor.CALM_GREEN,
        statePersistence = StatePersistenceDuration.ONE_HOUR,
        lastAccessedAt = kotlinx.datetime.Instant.parse("2024-01-15T10:30:00Z")
    )

    val json = JChecklist.toJson(original)
    val decoded = JChecklist.fromJson(json).orThrow()

    assertEquals(original, decoded)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: FAIL - complex object codecs not found

**Step 3: Add complex object codecs**

Add to `ChecklistCodecs.kt`:

```kotlin
// ChecklistSchedule
object JChecklistSchedule : JAny<ChecklistSchedule>() {
    val days_of_week by array(JDayOfWeek, ChecklistSchedule::daysOfWeek)
    val time_range by obj(JTimeRange, ChecklistSchedule::timeRange)

    override fun JsonNodeObject.deserializeOrThrow() =
        ChecklistSchedule(
            daysOfWeek = +days_of_week.toSet(),
            timeRange = +time_range
        )
}

// ChecklistItem
object JChecklistItem : JAny<ChecklistItem>() {
    val id by str(JChecklistItemId, ChecklistItem::id)
    val title by str(ChecklistItem::title)
    val icon_id by str(JItemIconId, ChecklistItem::iconId).nullable()
    val state by obj(JChecklistItemState, ChecklistItem::state)

    override fun JsonNodeObject.deserializeOrThrow() =
        ChecklistItem(
            id = +id,
            title = +title,
            iconId = +icon_id,
            state = +state
        )
}

// Checklist
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

**Step 4: Run tests to verify they pass**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: PASS

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt
git commit -m "feat(domain): add kondor codecs for complex objects

- Add JChecklistSchedule with days_of_week and time_range
- Add JChecklistItem with nullable icon_id
- Add JChecklist with all fields and snake_case naming
- All objects tested with roundtrip serialization

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 9: Create Root Codec and List Tests

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`
- Modify: `common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 1: Add failing tests for root codec**

Add to `ChecklistCodecsTest.kt`:

```kotlin
@Test
fun checklist_list_roundtrip() {
    val checklists = listOf(
        Checklist(
            id = ChecklistId("first"),
            name = "First",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = emptyList(),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.NEVER,
            lastAccessedAt = null
        ),
        Checklist(
            id = ChecklistId("second"),
            name = "Second",
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(
                    kotlinx.datetime.DayOfWeek.SATURDAY,
                    kotlinx.datetime.DayOfWeek.SUNDAY
                ),
                timeRange = TimeRange.Specific(
                    startTime = kotlinx.datetime.LocalTime(10, 0),
                    endTime = kotlinx.datetime.LocalTime(12, 0)
                )
            ),
            items = emptyList(),
            color = ChecklistColor.WARM_PEACH,
            statePersistence = StatePersistenceDuration.ONE_DAY,
            lastAccessedAt = null
        )
    )

    val json = JChecklistData.toJson(checklists)
    val decoded = JChecklistData.fromJson(json).orThrow()

    assertEquals(2, decoded.size)
    assertEquals(checklists, decoded)
}

@Test
fun empty_checklist_list_roundtrip() {
    val original = emptyList<Checklist>()
    val json = JChecklistData.toJson(original)
    val decoded = JChecklistData.fromJson(json).orThrow()

    assertEquals(original, decoded)
}

@Test
fun json_uses_snake_case_field_names() {
    val checklist = Checklist(
        id = ChecklistId("test"),
        name = "Test",
        schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
        items = emptyList(),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
        lastAccessedAt = kotlinx.datetime.Instant.parse("2024-01-01T00:00:00Z")
    )

    val json = JChecklist.toJson(checklist)

    // Verify snake_case field names
    assert(json.contains("\"state_persistence\""))
    assert(json.contains("\"last_accessed_at\""))
    assert(!json.contains("\"statePersistence\""))
    assert(!json.contains("\"lastAccessedAt\""))
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: FAIL - JChecklistData not found

**Step 3: Add root codec**

Add to `ChecklistCodecs.kt`:

```kotlin
// Root codec for the file - top-level array of checklists
object JChecklistData : JList<Checklist>(JChecklist)
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew common:testDebugUnitTest --tests ChecklistCodecsTest`
Expected: PASS (all tests)

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt \
        common/src/commonTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt
git commit -m "feat(domain): add root codec for checklist array

- Add JChecklistData using JList for top-level array
- Test list serialization with multiple checklists
- Test empty list serialization
- Verify snake_case JSON field naming

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 4: Android Repository Implementation

### Task 10: Create JsonChecklistRepository

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/data/JsonChecklistRepository.kt`

**Step 1: Write the implementation**

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/data/JsonChecklistRepository.kt
package com.pyanpyan.android.data

import android.content.Context
import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.RepositoryError
import com.pyanpyan.domain.repository.json.DefaultData
import com.pyanpyan.domain.repository.json.JChecklistData
import com.uberto.kondor.outcome.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class JsonChecklistRepository(
    private val context: Context
) : ChecklistRepository {

    private val fileName = "checklists.json"
    private val file: File
        get() = File(context.filesDir, fileName)

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

    override suspend fun getChecklist(id: ChecklistId): Outcome<RepositoryError, Checklist?> =
        getAllChecklists().map { checklists ->
            checklists.find { it.id == id }
        }

    override suspend fun saveChecklist(checklist: Checklist): Outcome<RepositoryError, Unit> =
        withContext(Dispatchers.IO) {
            getAllChecklists().bind { checklists ->
                val updated = checklists.filter { it.id != checklist.id } + checklist
                saveAllChecklists(updated)
            }
        }

    override suspend fun deleteChecklist(id: ChecklistId): Outcome<RepositoryError, Unit> =
        withContext(Dispatchers.IO) {
            getAllChecklists().bind { checklists ->
                val updated = checklists.filter { it.id != id }
                saveAllChecklists(updated)
            }
        }

    override suspend fun exportToJson(): Outcome<RepositoryError, String> =
        withContext(Dispatchers.IO) {
            Outcome.tryThis {
                if (!file.exists()) "[]" else file.readText()
            }.transformFailure { e ->
                RepositoryError.FileReadError(e.message ?: "Failed to export", e)
            }
        }

    override suspend fun importFromJson(json: String): Outcome<RepositoryError, Unit> =
        withContext(Dispatchers.IO) {
            JChecklistData.fromJson(json)
                .transformFailure { RepositoryError.JsonParseError(it.msg) }
                .bind { checklists -> saveAllChecklists(checklists) }
        }

    private fun saveAllChecklists(checklists: List<Checklist>): Outcome<RepositoryError, Unit> =
        Outcome.tryThis {
            val json = JChecklistData.toJson(checklists)
            file.writeText(json)
        }.transformFailure { e ->
            when (e) {
                is IOException -> RepositoryError.FileWriteError(e.message ?: "Failed to write file", e)
                else -> RepositoryError.FileWriteError(e.message ?: "Unknown error", e)
            }
        }
}
```

**Step 2: Build to verify it compiles**

Run: `./gradlew androidApp:assembleDebug`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/data/JsonChecklistRepository.kt
git commit -m "feat(android): add JsonChecklistRepository implementation

- Use Context.filesDir for app-private storage
- Create default School checklist on first run
- All I/O on Dispatchers.IO
- Use Outcome.tryThis for exception handling
- Read-modify-write pattern for updates/deletes

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 11: Create RepositoryFactory

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/data/RepositoryFactory.kt`

**Step 1: Write the implementation**

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/data/RepositoryFactory.kt
package com.pyanpyan.android.data

import android.content.Context
import com.pyanpyan.domain.repository.ChecklistRepository

object RepositoryFactory {
    private var repository: ChecklistRepository? = null

    fun getRepository(context: Context): ChecklistRepository =
        repository ?: JsonChecklistRepository(context.applicationContext).also {
            repository = it
        }
}
```

**Step 2: Build to verify it compiles**

Run: `./gradlew androidApp:assembleDebug`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/data/RepositoryFactory.kt
git commit -m "feat(android): add RepositoryFactory singleton

- Singleton factory for ChecklistRepository
- Uses application context to avoid leaks
- Lazy initialization on first access

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Phase 5: ViewModel Integration

### Task 12: Update ChecklistLibraryViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt`

**Step 1: Update constructor and replace mock data**

Update the ViewModel:

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt
package com.pyanpyan.android.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.query.ChecklistActivityState
import com.pyanpyan.domain.query.getActivityState
import com.pyanpyan.domain.repository.ChecklistRepository
import com.uberto.kondor.outcome.transform
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

    fun deleteChecklist(checklistId: ChecklistId) = viewModelScope.launch {
        repository.deleteChecklist(checklistId).transform(
            onSuccess = { loadChecklists() },
            onFailure = { error ->
                Log.e("ChecklistLibrary", "Failed to delete checklist: $error")
            }
        )
    }
}
```

**Step 2: Update ChecklistLibraryScreen to pass repository**

Update `ChecklistLibraryScreen.kt` to create ViewModel with repository:

```kotlin
// In ChecklistLibraryScreen.kt, update the composable signature:
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistLibraryScreen(
    onChecklistClick: (ChecklistId) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: ChecklistLibraryViewModel = viewModel {
        ChecklistLibraryViewModel(
            repository = RepositoryFactory.getRepository(LocalContext.current)
        )
    }
) {
    // Rest of the code stays the same
}
```

Add this import at the top:
```kotlin
import androidx.compose.ui.platform.LocalContext
import com.pyanpyan.android.data.RepositoryFactory
```

**Step 3: Build and verify**

Run: `./gradlew androidApp:assembleDebug`
Expected: SUCCESS

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryViewModel.kt \
        androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat(ui): integrate ChecklistLibraryViewModel with repository

- Replace mock data with repository calls
- Use Outcome.transform for error handling
- Pass repository via constructor
- Create ViewModel with RepositoryFactory in Screen

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Task 13: Update ChecklistViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`

**Step 1: Update ChecklistViewModel**

Replace the ViewModel:

```kotlin
// androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
package com.pyanpyan.android.ui.checklist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.command.IgnoreItemToday
import com.pyanpyan.domain.command.MarkItemDone
import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.repository.ChecklistRepository
import com.uberto.kondor.outcome.transform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = false
)

class ChecklistViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    fun loadChecklist(checklistId: ChecklistId) = viewModelScope.launch {
        _uiState.value = ChecklistUiState(isLoading = true)

        repository.getChecklist(checklistId).transform(
            onSuccess = { checklist ->
                _uiState.value = ChecklistUiState(checklist = checklist, isLoading = false)
            },
            onFailure = { error ->
                _uiState.value = ChecklistUiState(isLoading = false)
                Log.e("Checklist", "Failed to load checklist: $error")
            }
        )
    }

    fun markItemDone(itemId: ChecklistItemId) = viewModelScope.launch {
        val checklist = _uiState.value.checklist ?: return@launch
        val item = checklist.findItem(itemId) ?: return@launch

        val command = MarkItemDone(itemId)
        val updatedChecklist = checklist.updateItem(command.execute(item))

        repository.saveChecklist(updatedChecklist).transform(
            onSuccess = { _uiState.value = ChecklistUiState(checklist = updatedChecklist) },
            onFailure = { error ->
                Log.e("Checklist", "Failed to save checklist: $error")
            }
        )
    }

    fun ignoreItemToday(itemId: ChecklistItemId) = viewModelScope.launch {
        val checklist = _uiState.value.checklist ?: return@launch
        val item = checklist.findItem(itemId) ?: return@launch

        val command = IgnoreItemToday(itemId)
        val updatedChecklist = checklist.updateItem(command.execute(item))

        repository.saveChecklist(updatedChecklist).transform(
            onSuccess = { _uiState.value = ChecklistUiState(checklist = updatedChecklist) },
            onFailure = { error ->
                Log.e("Checklist", "Failed to save checklist: $error")
            }
        )
    }
}
```

**Step 2: Update ChecklistScreen to load checklist**

Update `ChecklistScreen.kt`:

```kotlin
// Update ChecklistScreen composable signature and add LaunchedEffect:
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    checklistId: ChecklistId,
    onBackClick: () -> Unit,
    viewModel: ChecklistViewModel = viewModel {
        ChecklistViewModel(
            repository = RepositoryFactory.getRepository(LocalContext.current)
        )
    }
) {
    // Load checklist when screen opens
    LaunchedEffect(checklistId) {
        viewModel.loadChecklist(checklistId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Rest of existing code...
    }
}
```

Add these imports:
```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.pyanpyan.android.data.RepositoryFactory
```

**Step 3: Build and test**

Run: `./gradlew androidApp:installDebug`
Expected: SUCCESS, app should now persist data

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt \
        androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git commit -m "feat(ui): integrate ChecklistViewModel with repository

- Replace mock data with repository calls
- Load checklist by ID using LaunchedEffect
- Save checklist state after mark done/ignore
- Use Outcome.transform for error handling
- Pass repository via constructor

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Summary

**Total Tasks:** 13

**Phase Breakdown:**
- Phase 1: Dependencies (1 task)
- Phase 2: Repository Interface (3 tasks)
- Phase 3: Kondor JSON Codecs (5 tasks)
- Phase 4: Android Repository (2 tasks)
- Phase 5: ViewModel Integration (2 tasks)

**Key Technologies:**
- Kondor-json for type-safe JSON serialization
- Kondor-outcome for functional error handling
- Android internal storage for persistence
- Coroutines for async I/O

**Testing:**
- Unit tests for all Kondor codecs
- Unit tests for DefaultData
- Unit tests for RepositoryError
- Manual testing in app for end-to-end flow

**Data Flow:**
```
ViewModel → Repository → Outcome<Error, Data>
                ↓
         Kondor Codecs ↔ JSON File (Context.filesDir)
```

**First Run:**
- App creates default "School" checklist automatically
- 5 items: Books, Homework, PE kit, Breakfast, Brushing teeth
- Weekdays only (Mon-Fri), all day schedule

**Persistence:**
- All changes saved immediately to `checklists.json`
- Read-modify-write pattern for updates
- Atomic file writes for data integrity
