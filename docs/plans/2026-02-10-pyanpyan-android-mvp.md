# Pyanpyan Android MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the MVP of Pyanpyan, a gentle checklist & timer app for ADHD brains on Android using Kotlin Multiplatform.

**Architecture:** Domain-driven design with CQRS, functional style, and platform-agnostic core. Domain logic in `common` module, Android UI in `androidApp` module. Time is injected, state transitions are explicit, all behavior is testable.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose (Android), kotlinx-datetime, kotlinx-serialization, Room (persistence)

---

## Task 1: Project Structure & Dependencies

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts` (root)
- Create: `common/build.gradle.kts`
- Create: `androidApp/build.gradle.kts`

**Step 1: Configure Kotlin Multiplatform modules**

Verify current structure:
```bash
ls -la
```

Expected: Basic Android project structure

**Step 2: Update settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pyanpyan"
include(":androidApp")
include(":common")
```

**Step 3: Create common module build file**

Create `common/build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
```

**Step 4: Sync project**

```bash
./gradlew build --dry-run
```

Expected: Project syncs without errors

**Step 5: Commit**

```bash
git add settings.gradle.kts common/build.gradle.kts
git commit -m "feat: setup Kotlin Multiplatform structure with common module"
```

---

## Task 2: Domain Model - Checklist Item States

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItemState.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemStateTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemStateTest.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistItemStateTest {

    @Test
    fun `state can be Pending`() {
        val state = ChecklistItemState.Pending
        assertTrue(state is ChecklistItemState.Pending)
    }

    @Test
    fun `state can be Done`() {
        val state = ChecklistItemState.Done
        assertTrue(state is ChecklistItemState.Done)
    }

    @Test
    fun `state can be IgnoredToday`() {
        val state = ChecklistItemState.IgnoredToday
        assertTrue(state is ChecklistItemState.IgnoredToday)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: ChecklistItemState"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItemState.kt`:

```kotlin
package com.pyanpyan.domain.model

sealed interface ChecklistItemState {
    data object Pending : ChecklistItemState
    data object Done : ChecklistItemState
    data object IgnoredToday : ChecklistItemState
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all 3 tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItemState.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemStateTest.kt
git commit -m "feat: add ChecklistItemState domain model"
```

---

## Task 3: Domain Model - Checklist Item

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemTest.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistItemTest {

    @Test
    fun `checklist item has id, title, and state`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        assertEquals(ChecklistItemId("item-1"), item.id)
        assertEquals("Brush teeth", item.title)
        assertEquals(ChecklistItemState.Pending, item.state)
    }

    @Test
    fun `item can be marked done`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        val doneItem = item.markDone()

        assertEquals(ChecklistItemState.Done, doneItem.state)
        assertEquals(item.id, doneItem.id)
        assertEquals(item.title, doneItem.title)
    }

    @Test
    fun `item can be ignored today`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        val ignoredItem = item.ignoreToday()

        assertEquals(ChecklistItemState.IgnoredToday, ignoredItem.state)
    }

    @Test
    fun `item can be reset to pending`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Done
        )

        val resetItem = item.reset()

        assertEquals(ChecklistItemState.Pending, resetItem.state)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: ChecklistItem"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt`:

```kotlin
package com.pyanpyan.domain.model

@JvmInline
value class ChecklistItemId(val value: String)

data class ChecklistItem(
    val id: ChecklistItemId,
    val title: String,
    val state: ChecklistItemState
) {
    fun markDone(): ChecklistItem = copy(state = ChecklistItemState.Done)

    fun ignoreToday(): ChecklistItem = copy(state = ChecklistItemState.IgnoredToday)

    fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all 4 tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistItemTest.kt
git commit -m "feat: add ChecklistItem domain model with state transitions"
```

---

## Task 4: Time Abstraction - Clock Interface

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/service/Clock.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/service/FakeClock.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/service/FakeClockTest.kt`:

```kotlin
package com.pyanpyan.domain.service

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeClockTest {

    @Test
    fun `fake clock returns configured instant`() {
        val fixedTime = Instant.parse("2026-02-10T10:00:00Z")
        val clock = FakeClock(fixedTime)

        assertEquals(fixedTime, clock.now())
    }

    @Test
    fun `fake clock can advance time`() {
        val startTime = Instant.parse("2026-02-10T10:00:00Z")
        val clock = FakeClock(startTime)

        clock.advanceBy(kotlinx.datetime.DateTimeUnit.MINUTE, 5)

        val expected = Instant.parse("2026-02-10T10:05:00Z")
        assertEquals(expected, clock.now())
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: Clock"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/service/Clock.kt`:

```kotlin
package com.pyanpyan.domain.service

import kotlinx.datetime.Instant

interface Clock {
    fun now(): Instant
}
```

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/service/FakeClock.kt`:

```kotlin
package com.pyanpyan.domain.service

import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class FakeClock(private var currentTime: Instant) : Clock {
    override fun now(): Instant = currentTime

    fun advanceBy(unit: DateTimeUnit, value: Int) {
        currentTime = currentTime.plus(value, unit)
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all 2 tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/service/Clock.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/service/FakeClock.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/service/FakeClockTest.kt
git commit -m "feat: add Clock interface for time injection with FakeClock"
```

---

## Task 5: Commands - MarkItemDone

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/command/MarkItemDone.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/command/MarkItemDoneTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/command/MarkItemDoneTest.kt`:

```kotlin
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.model.ChecklistItemState
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkItemDoneTest {

    @Test
    fun `command contains item id`() {
        val command = MarkItemDone(ChecklistItemId("item-1"))
        assertEquals(ChecklistItemId("item-1"), command.itemId)
    }

    @Test
    fun `executing command marks item as done`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        val command = MarkItemDone(item.id)
        val result = command.execute(item)

        assertEquals(ChecklistItemState.Done, result.state)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: MarkItemDone"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/command/MarkItemDone.kt`:

```kotlin
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId

data class MarkItemDone(val itemId: ChecklistItemId) {
    fun execute(item: ChecklistItem): ChecklistItem {
        require(item.id == itemId) { "Item ID mismatch" }
        return item.markDone()
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/command/MarkItemDone.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/command/MarkItemDoneTest.kt
git commit -m "feat: add MarkItemDone command"
```

---

## Task 6: Commands - IgnoreItemToday

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/command/IgnoreItemToday.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/command/IgnoreItemTodayTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/command/IgnoreItemTodayTest.kt`:

```kotlin
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.model.ChecklistItemState
import kotlin.test.Test
import kotlin.test.assertEquals

class IgnoreItemTodayTest {

    @Test
    fun `command contains item id`() {
        val command = IgnoreItemToday(ChecklistItemId("item-1"))
        assertEquals(ChecklistItemId("item-1"), command.itemId)
    }

    @Test
    fun `executing command marks item as ignored today`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Exercise",
            state = ChecklistItemState.Pending
        )

        val command = IgnoreItemToday(item.id)
        val result = command.execute(item)

        assertEquals(ChecklistItemState.IgnoredToday, result.state)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: IgnoreItemToday"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/command/IgnoreItemToday.kt`:

```kotlin
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId

data class IgnoreItemToday(val itemId: ChecklistItemId) {
    fun execute(item: ChecklistItem): ChecklistItem {
        require(item.id == itemId) { "Item ID mismatch" }
        return item.ignoreToday()
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/command/IgnoreItemToday.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/command/IgnoreItemTodayTest.kt
git commit -m "feat: add IgnoreItemToday command"
```

---

## Task 7: Domain Model - Timer

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Timer.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimerTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimerTest.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TimerTest {

    @Test
    fun `timer can be created with duration and type`() {
        val timer = Timer(
            id = TimerId("timer-1"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.NotStarted
        )

        assertEquals(TimerId("timer-1"), timer.id)
        assertEquals(60.seconds, timer.duration)
        assertEquals(TimerType.Short, timer.type)
    }

    @Test
    fun `timer can be started`() {
        val timer = Timer(
            id = TimerId("timer-1"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.NotStarted
        )
        val startTime = Instant.parse("2026-02-10T10:00:00Z")

        val started = timer.start(startTime)

        assertTrue(started.state is TimerState.Running)
        assertEquals(startTime, (started.state as TimerState.Running).startedAt)
    }

    @Test
    fun `timer can be completed`() {
        val startTime = Instant.parse("2026-02-10T10:00:00Z")
        val timer = Timer(
            id = TimerId("timer-1"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.Running(startedAt = startTime)
        )

        val completed = timer.complete()

        assertTrue(completed.state is TimerState.Completed)
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: Timer"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Timer.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.datetime.Instant
import kotlin.time.Duration

@JvmInline
value class TimerId(val value: String)

enum class TimerType {
    Short,  // seconds
    Long    // minutes
}

sealed interface TimerState {
    data object NotStarted : TimerState
    data class Running(val startedAt: Instant) : TimerState
    data object Completed : TimerState
}

data class Timer(
    val id: TimerId,
    val duration: Duration,
    val type: TimerType,
    val state: TimerState
) {
    fun start(at: Instant): Timer = copy(state = TimerState.Running(startedAt = at))

    fun complete(): Timer = copy(state = TimerState.Completed)

    fun remainingTime(now: Instant): Duration? {
        return when (val s = state) {
            is TimerState.Running -> {
                val elapsed = now - s.startedAt
                (duration - elapsed).coerceAtLeast(Duration.ZERO)
            }
            else -> null
        }
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/Timer.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimerTest.kt
git commit -m "feat: add Timer domain model with state transitions"
```

---

## Task 8: Checklist Aggregate

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistTest.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistTest {

    @Test
    fun `checklist can contain items`() {
        val items = listOf(
            ChecklistItem(
                id = ChecklistItemId("item-1"),
                title = "Brush teeth",
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("item-2"),
                title = "Get dressed",
                state = ChecklistItemState.Pending
            )
        )

        val checklist = Checklist(
            id = ChecklistId("morning-routine"),
            title = "Morning Routine",
            items = items
        )

        assertEquals(2, checklist.items.size)
        assertEquals("Morning Routine", checklist.title)
    }

    @Test
    fun `checklist can update item state`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )
        val checklist = Checklist(
            id = ChecklistId("morning-routine"),
            title = "Morning Routine",
            items = listOf(item)
        )

        val updated = checklist.updateItem(item.markDone())

        assertEquals(ChecklistItemState.Done, updated.items[0].state)
    }

    @Test
    fun `checklist can reset all items to pending`() {
        val items = listOf(
            ChecklistItem(
                id = ChecklistItemId("item-1"),
                title = "Brush teeth",
                state = ChecklistItemState.Done
            ),
            ChecklistItem(
                id = ChecklistItemId("item-2"),
                title = "Get dressed",
                state = ChecklistItemState.IgnoredToday
            )
        )
        val checklist = Checklist(
            id = ChecklistId("morning-routine"),
            title = "Morning Routine",
            items = items
        )

        val reset = checklist.resetAllItems()

        assertTrue(reset.items.all { it.state == ChecklistItemState.Pending })
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: Checklist"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt`:

```kotlin
package com.pyanpyan.domain.model

@JvmInline
value class ChecklistId(val value: String)

data class Checklist(
    val id: ChecklistId,
    val title: String,
    val items: List<ChecklistItem>
) {
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

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/model/ChecklistTest.kt
git commit -m "feat: add Checklist aggregate with item management"
```

---

## Task 9: Commands - ResetDailyState

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/command/ResetDailyState.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/command/ResetDailyStateTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/command/ResetDailyStateTest.kt`:

```kotlin
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class ResetDailyStateTest {

    @Test
    fun `command resets all ignored items to pending`() {
        val items = listOf(
            ChecklistItem(
                id = ChecklistItemId("item-1"),
                title = "Brush teeth",
                state = ChecklistItemState.Done
            ),
            ChecklistItem(
                id = ChecklistItemId("item-2"),
                title = "Exercise",
                state = ChecklistItemState.IgnoredToday
            )
        )
        val checklist = Checklist(
            id = ChecklistId("morning"),
            title = "Morning Routine",
            items = items
        )

        val command = ResetDailyState()
        val result = command.execute(checklist)

        assertTrue(result.items.all { it.state == ChecklistItemState.Pending })
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: ResetDailyState"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/command/ResetDailyState.kt`:

```kotlin
package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.Checklist

class ResetDailyState {
    fun execute(checklist: Checklist): Checklist {
        return checklist.resetAllItems()
    }
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/command/ResetDailyState.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/command/ResetDailyStateTest.kt
git commit -m "feat: add ResetDailyState command"
```

---

## Task 10: Queries - GetTodayChecklist

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetTodayChecklist.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetTodayChecklistTest.kt`

**Step 1: Write the failing test**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetTodayChecklistTest.kt`:

```kotlin
package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTodayChecklistTest {

    @Test
    fun `query returns checklist by id`() {
        val checklist = Checklist(
            id = ChecklistId("morning"),
            title = "Morning Routine",
            items = emptyList()
        )
        val repository = FakeChecklistRepository(listOf(checklist))

        val query = GetTodayChecklist(ChecklistId("morning"))
        val result = query.execute(repository)

        assertEquals(checklist, result)
    }
}

class FakeChecklistRepository(private val checklists: List<Checklist>) {
    fun findById(id: ChecklistId): Checklist? {
        return checklists.find { it.id == id }
    }
}
```

**Step 2: Run test to verify it fails**

```bash
./gradlew :common:test
```

Expected: FAIL with "Unresolved reference: GetTodayChecklist"

**Step 3: Write minimal implementation**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetTodayChecklist.kt`:

```kotlin
package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId

data class GetTodayChecklist(val checklistId: ChecklistId) {
    fun <R> execute(repository: R): Checklist? where R : ChecklistRepository {
        return repository.findById(checklistId)
    }
}

interface ChecklistRepository {
    fun findById(id: ChecklistId): Checklist?
}
```

**Step 4: Run test to verify it passes**

```bash
./gradlew :common:test
```

Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetTodayChecklist.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetTodayChecklistTest.kt
git commit -m "feat: add GetTodayChecklist query with repository interface"
```

---

## Task 11: Acceptance Test - Mark Item Done Flow

**Files:**
- Create: `common/src/commonTest/kotlin/com/pyanpyan/acceptance/MarkItemDoneFlowTest.kt`

**Step 1: Write the acceptance test**

Create `common/src/commonTest/kotlin/com/pyanpyan/acceptance/MarkItemDoneFlowTest.kt`:

```kotlin
package com.pyanpyan.acceptance

import com.pyanpyan.domain.command.MarkItemDone
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.query.GetTodayChecklist
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Acceptance Test: User marks a checklist item as done
 *
 * Given: A checklist with pending items
 * When: User marks an item as done
 * Then: The item state changes to Done
 */
class MarkItemDoneFlowTest {

    @Test
    fun `user can mark a checklist item as done`() {
        // Given: A checklist with a pending item
        val itemId = ChecklistItemId("brush-teeth")
        val item = ChecklistItem(
            id = itemId,
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )
        val checklistId = ChecklistId("morning-routine")
        val checklist = Checklist(
            id = checklistId,
            title = "Morning Routine",
            items = listOf(item)
        )

        // When: User marks the item as done
        val command = MarkItemDone(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = checklist.updateItem(updatedItem)

        // Then: The item is marked as done
        val query = GetTodayChecklist(checklistId)
        val resultChecklist = InMemoryChecklistRepository(updatedChecklist).let { repo ->
            query.execute(repo)
        }

        val resultItem = resultChecklist?.findItem(itemId)
        assertEquals(ChecklistItemState.Done, resultItem?.state)
    }
}

private class InMemoryChecklistRepository(private val checklist: Checklist) :
    com.pyanpyan.domain.query.ChecklistRepository {
    override fun findById(id: ChecklistId): Checklist? {
        return if (checklist.id == id) checklist else null
    }
}
```

**Step 2: Run test to verify it passes**

```bash
./gradlew :common:test --tests "*MarkItemDoneFlowTest"
```

Expected: PASS - acceptance test passes

**Step 3: Commit**

```bash
git add common/src/commonTest/kotlin/com/pyanpyan/acceptance/MarkItemDoneFlowTest.kt
git commit -m "test: add acceptance test for mark item done flow"
```

---

## Task 12: Acceptance Test - Ignore Item Today Flow

**Files:**
- Create: `common/src/commonTest/kotlin/com/pyanpyan/acceptance/IgnoreItemTodayFlowTest.kt`

**Step 1: Write the acceptance test**

Create `common/src/commonTest/kotlin/com/pyanpyan/acceptance/IgnoreItemTodayFlowTest.kt`:

```kotlin
package com.pyanpyan.acceptance

import com.pyanpyan.domain.command.IgnoreItemToday
import com.pyanpyan.domain.command.ResetDailyState
import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Acceptance Test: User ignores an item today
 *
 * Given: A checklist with pending items
 * When: User ignores an item
 * Then: The item state changes to IgnoredToday
 * And: The next day, the item resets to Pending
 */
class IgnoreItemTodayFlowTest {

    @Test
    fun `user can ignore an item today and it resets tomorrow`() {
        // Given: A checklist with a pending item
        val itemId = ChecklistItemId("exercise")
        val item = ChecklistItem(
            id = itemId,
            title = "Exercise for 20 minutes",
            state = ChecklistItemState.Pending
        )
        val checklist = Checklist(
            id = ChecklistId("daily-tasks"),
            title = "Daily Tasks",
            items = listOf(item)
        )

        // When: User ignores the item today
        val ignoreCommand = IgnoreItemToday(itemId)
        val ignoredItem = ignoreCommand.execute(item)
        val checklistAfterIgnore = checklist.updateItem(ignoredItem)

        // Then: The item is ignored
        assertEquals(ChecklistItemState.IgnoredToday,
            checklistAfterIgnore.findItem(itemId)?.state)

        // When: The next day arrives (daily reset happens)
        val resetCommand = ResetDailyState()
        val checklistNextDay = resetCommand.execute(checklistAfterIgnore)

        // Then: The item is reset to pending
        assertEquals(ChecklistItemState.Pending,
            checklistNextDay.findItem(itemId)?.state)
    }
}
```

**Step 2: Run test to verify it passes**

```bash
./gradlew :common:test --tests "*IgnoreItemTodayFlowTest"
```

Expected: PASS - acceptance test passes

**Step 3: Commit**

```bash
git add common/src/commonTest/kotlin/com/pyanpyan/acceptance/IgnoreItemTodayFlowTest.kt
git commit -m "test: add acceptance test for ignore item today flow"
```

---

## Task 13: Acceptance Test - Timer Flow

**Files:**
- Create: `common/src/commonTest/kotlin/com/pyanpyan/acceptance/TimerFlowTest.kt`

**Step 1: Write the acceptance test**

Create `common/src/commonTest/kotlin/com/pyanpyan/acceptance/TimerFlowTest.kt`:

```kotlin
package com.pyanpyan.acceptance

import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.service.FakeClock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Acceptance Test: User starts and completes a timer
 *
 * Given: A timer with specified duration
 * When: User starts the timer
 * Then: Timer begins counting down
 * When: Time elapses equal to duration
 * Then: Timer can be marked as completed
 */
class TimerFlowTest {

    @Test
    fun `user can start and complete a timer`() {
        // Given: A 60-second timer
        val timer = Timer(
            id = TimerId("brush-teeth-timer"),
            duration = 60.seconds,
            type = TimerType.Short,
            state = TimerState.NotStarted
        )
        val clock = FakeClock(Instant.parse("2026-02-10T10:00:00Z"))

        // When: User starts the timer
        val runningTimer = timer.start(clock.now())

        // Then: Timer is running
        assertTrue(runningTimer.state is TimerState.Running)

        // When: 30 seconds pass
        clock.advanceBy(kotlinx.datetime.DateTimeUnit.SECOND, 30)

        // Then: 30 seconds remain
        val remaining = runningTimer.remainingTime(clock.now())
        assertEquals(30.seconds, remaining)

        // When: Full duration passes
        clock.advanceBy(kotlinx.datetime.DateTimeUnit.SECOND, 30)

        // Then: Timer can be completed
        val completedTimer = runningTimer.complete()
        assertTrue(completedTimer.state is TimerState.Completed)
    }
}
```

**Step 2: Run test to verify it passes**

```bash
./gradlew :common:test --tests "*TimerFlowTest"
```

Expected: PASS - acceptance test passes

**Step 3: Commit**

```bash
git add common/src/commonTest/kotlin/com/pyanpyan/acceptance/TimerFlowTest.kt
git commit -m "test: add acceptance test for timer flow"
```

---

## Task 14: Android App Module Setup

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Create: `androidApp/src/main/AndroidManifest.xml`

**Step 1: Update Android build file**

Modify `androidApp/build.gradle.kts`:

```kotlin
plugins {
    kotlin("android")
    id("com.android.application")
}

android {
    namespace = "com.pyanpyan.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pyanpyan.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
```

**Step 2: Create Android manifest**

Create `androidApp/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Pyanpyan">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Pyanpyan">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Step 3: Sync project**

```bash
./gradlew :androidApp:assembleDebug --dry-run
```

Expected: Build configuration succeeds

**Step 4: Commit**

```bash
git add androidApp/build.gradle.kts androidApp/src/main/AndroidManifest.xml
git commit -m "feat: setup Android app module with Compose"
```

---

## Task 15: Android MainActivity & Theme

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`
- Create: `androidApp/src/main/res/values/strings.xml`
- Create: `androidApp/src/main/res/values/themes.xml`

**Step 1: Create strings resource**

Create `androidApp/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Pyanpyan</string>
</resources>
```

**Step 2: Create theme resource**

Create `androidApp/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Pyanpyan" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

**Step 3: Create Compose theme**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/ui/theme/Theme.kt`:

```kotlin
package com.pyanpyan.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB4D5A8),      // Soft green
    secondary = Color(0xFFFFC5A8),     // Soft orange
    tertiary = Color(0xFFA8D5E3),      // Soft blue
    background = Color(0xFFFFFBF5),    // Warm white
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF2D4A26),
    onSecondary = Color(0xFF4A2D1F),
    onTertiary = Color(0xFF1F3A4A),
    onBackground = Color(0xFF3A3A3A),
    onSurface = Color(0xFF3A3A3A)
)

@Composable
fun PyanpyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Currently only light theme (calm, soft palette for ADHD users)
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
```

**Step 4: Create MainActivity**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`:

```kotlin
package com.pyanpyan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyanpyan.android.ui.theme.PyanpyanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PyanpyanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text("Pyanpyan")
                }
            }
        }
    }
}
```

**Step 5: Build and verify**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/
git add androidApp/src/main/res/
git commit -m "feat: add MainActivity with calm theme for ADHD users"
```

---

## Task 16: ChecklistViewModel Setup

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`
- Modify: `androidApp/build.gradle.kts`

**Step 1: Add ViewModel dependency**

Modify `androidApp/build.gradle.kts`, add to dependencies:

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
```

**Step 2: Create ViewModel**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`:

```kotlin
package com.pyanpyan.android.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.command.IgnoreItemToday
import com.pyanpyan.domain.command.MarkItemDone
import com.pyanpyan.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = false
)

class ChecklistViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    init {
        loadChecklist()
    }

    private fun loadChecklist() {
        viewModelScope.launch {
            // Temporary mock data - will be replaced with repository
            val mockChecklist = Checklist(
                id = ChecklistId("morning-routine"),
                title = "Morning Routine",
                items = listOf(
                    ChecklistItem(
                        id = ChecklistItemId("brush-teeth"),
                        title = "Brush teeth",
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("get-dressed"),
                        title = "Get dressed",
                        state = ChecklistItemState.Pending
                    )
                )
            )

            _uiState.value = ChecklistUiState(checklist = mockChecklist)
        }
    }

    fun markItemDone(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = MarkItemDone(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)
    }

    fun ignoreItemToday(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = IgnoreItemToday(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)
    }
}
```

**Step 3: Sync and build**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add androidApp/build.gradle.kts
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git commit -m "feat: add ChecklistViewModel with domain command integration"
```

---

## Task 17: ChecklistScreen UI

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`

**Step 1: Create Composable screen**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`:

```kotlin
package com.pyanpyan.android.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemState

@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            uiState.checklist?.let { checklist ->
                Text(
                    text = checklist.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(checklist.items) { item ->
                        ChecklistItemRow(
                            item = item,
                            onMarkDone = { viewModel.markItemDone(item.id) },
                            onIgnoreToday = { viewModel.ignoreItemToday(item.id) }
                        )
                    }
                }
            }
        }
    }
}

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                },
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.state == ChecklistItemState.Pending) {
                    Button(
                        onClick = onMarkDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Done")
                    }

                    OutlinedButton(onClick = onIgnoreToday) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}
```

**Step 2: Update MainActivity to show ChecklistScreen**

Modify `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`:

```kotlin
package com.pyanpyan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pyanpyan.android.ui.checklist.ChecklistScreen
import com.pyanpyan.android.ui.theme.PyanpyanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PyanpyanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChecklistScreen()
                }
            }
        }
    }
}
```

**Step 3: Build and verify**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat: add ChecklistScreen UI with swipe actions"
```

---

## Task 18: Plan Completion Summary

**Summary of Completed Work:**

### Domain Layer (Complete)
- ✅ ChecklistItemState (Pending, Done, IgnoredToday)
- ✅ ChecklistItem with state transitions
- ✅ Checklist aggregate
- ✅ Timer with state machine
- ✅ Clock interface for time injection
- ✅ Commands: MarkItemDone, IgnoreItemToday, ResetDailyState
- ✅ Queries: GetTodayChecklist with repository interface
- ✅ Acceptance tests for all core flows

### Android Layer (Complete)
- ✅ Kotlin Multiplatform project structure
- ✅ MainActivity with Compose
- ✅ Calm, ADHD-friendly theme
- ✅ ChecklistViewModel with domain command integration
- ✅ ChecklistScreen UI with item state visualization

### Next Steps (Not in This Plan)
- Persistence layer (Room database)
- Timer UI and animation (Egg → Caterpillar → Butterfly)
- Context memory (15-minute UI state preservation)
- Skinnable UI system
- Localization implementation
- Daily reset scheduling

### Verification Commands

Run all tests:
```bash
./gradlew :common:test
```

Build Android app:
```bash
./gradlew :androidApp:assembleDebug
```

Install on device/emulator:
```bash
./gradlew :androidApp:installDebug
```

---

**Plan complete and saved to `docs/plans/2026-02-10-pyanpyan-android-mvp.md`.**

Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

Which approach?
