# Kotlinx-Serialization Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate from kondor-json to kotlinx-serialization to resolve Android DEX conflicts.

**Architecture:** Replace kondor-json codecs with kotlinx-serialization annotations and custom serializers. Keep RepositoryResult for functional error handling. Use camelCase JSON field naming (Android convention).

**Tech Stack:** kotlinx-serialization-json, Kotlin multiplatform, Android SDK

---

## Task 1: Remove Kondor Dependencies

**Files:**
- Modify: `common/build.gradle.kts:34-43`

**Step 1: Remove kondor dependencies from androidUnitTest**

Edit `common/build.gradle.kts` and remove kondor dependencies:

```kotlin
val androidUnitTest by getting {
    dependencies {
        // Remove these lines:
        // implementation("com.ubertob.kondor:kondor-core:3.6.1")
        // implementation("com.ubertob.kondor:kondor-outcome:3.6.1")
    }
}
```

**Step 2: Remove kondor dependencies from androidInstrumentedTest**

```kotlin
val androidInstrumentedTest by getting {
    dependencies {
        // Remove these lines:
        // implementation("com.ubertob.kondor:kondor-core:3.6.1")
        // implementation("com.ubertob.kondor:kondor-outcome:3.6.1")
    }
}
```

**Step 3: Commit**

```bash
git add common/build.gradle.kts
git commit -m "build: remove kondor dependencies from test sourcesets"
```

---

## Task 2: Create Value Class Serializers

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ValueClassSerializers.kt`

**Step 1: Create ValueClassSerializers.kt file**

Create the file with custom serializers for all value classes:

```kotlin
package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.model.ItemIconId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ChecklistIdSerializer : KSerializer<ChecklistId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChecklistId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChecklistId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ChecklistId {
        return ChecklistId(decoder.decodeString())
    }
}

object ChecklistItemIdSerializer : KSerializer<ChecklistItemId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChecklistItemId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChecklistItemId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ChecklistItemId {
        return ChecklistItemId(decoder.decodeString())
    }
}

object ItemIconIdSerializer : KSerializer<ItemIconId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ItemIconId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemIconId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): ItemIconId {
        return ItemIconId(decoder.decodeString())
    }
}
```

**Step 2: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/json/ValueClassSerializers.kt
git commit -m "feat(serialization): add custom serializers for value classes"
```

---

## Task 3: Add @Serializable to TimeRange Sealed Class

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/TimeRange.kt:1-25`

**Step 1: Add imports and @Serializable annotations**

Replace the entire file content:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TimeRange {
    abstract val isAllDay: Boolean
    abstract fun contains(time: LocalTime): Boolean

    @Serializable
    @SerialName("AllDay")
    data object AllDay : TimeRange() {
        override val isAllDay: Boolean = true
        override fun contains(time: LocalTime): Boolean = true
    }

    @Serializable
    @SerialName("Specific")
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

**Step 2: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/TimeRange.kt
git commit -m "feat(serialization): add @Serializable to TimeRange sealed class"
```

---

## Task 4: Add @Serializable to ChecklistItemState Sealed Interface

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItemState.kt:1-7`

**Step 1: Add imports and @Serializable annotations**

Replace the entire file content:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ChecklistItemState {
    @Serializable
    @SerialName("Pending")
    data object Pending : ChecklistItemState

    @Serializable
    @SerialName("Done")
    data object Done : ChecklistItemState

    @Serializable
    @SerialName("IgnoredToday")
    data object IgnoredToday : ChecklistItemState
}
```

**Step 2: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItemState.kt
git commit -m "feat(serialization): add @Serializable to ChecklistItemState sealed interface"
```

---

## Task 5: Add @Serializable to ChecklistSchedule

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistSchedule.kt:1-11`

**Step 1: Add @Serializable annotation**

Replace the entire file content:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class ChecklistSchedule(
    val daysOfWeek: Set<DayOfWeek>, // empty set = all days
    val timeRange: TimeRange
) {
    val isAlwaysOn: Boolean
        get() = daysOfWeek.isEmpty() && timeRange.isAllDay
}
```

**Step 2: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistSchedule.kt
git commit -m "feat(serialization): add @Serializable to ChecklistSchedule"
```

---

## Task 6: Add @Serializable to ChecklistItem

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt:1-20`

**Step 1: Add imports and @Serializable annotation**

Replace the entire file content:

```kotlin
package com.pyanpyan.domain.model

import com.pyanpyan.domain.repository.json.ChecklistItemIdSerializer
import com.pyanpyan.domain.repository.json.ItemIconIdSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = ChecklistItemIdSerializer::class)
value class ChecklistItemId(val value: String)

@JvmInline
@Serializable(with = ItemIconIdSerializer::class)
value class ItemIconId(val value: String)

@Serializable
data class ChecklistItem(
    val id: ChecklistItemId,
    val title: String,
    val iconId: ItemIconId? = null,
    val state: ChecklistItemState
) {
    fun markDone(): ChecklistItem = copy(state = ChecklistItemState.Done)

    fun ignoreToday(): ChecklistItem = copy(state = ChecklistItemState.IgnoredToday)

    fun reset(): ChecklistItem = copy(state = ChecklistItemState.Pending)
}
```

**Step 2: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistItem.kt
git commit -m "feat(serialization): add @Serializable to ChecklistItem and value classes"
```

---

## Task 7: Add @Serializable to Checklist

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt:1-31`

**Step 1: Add imports and @Serializable annotation**

Replace the entire file content:

```kotlin
package com.pyanpyan.domain.model

import com.pyanpyan.domain.repository.json.ChecklistIdSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = ChecklistIdSerializer::class)
value class ChecklistId(val value: String)

@Serializable
data class Checklist(
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration,
    val lastAccessedAt: Instant? = null
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

**Step 2: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/Checklist.kt
git commit -m "feat(serialization): add @Serializable to Checklist and ChecklistId"
```

---

## Task 8: Add @Serializable to Enums

**Files:**
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistColor.kt:1-12`
- Modify: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/StatePersistenceDuration.kt:1-17`

**Step 1: Add @Serializable to ChecklistColor**

Replace the entire ChecklistColor.kt content:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.serialization.Serializable

@Serializable
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

**Step 2: Add @Serializable to StatePersistenceDuration**

Replace the entire StatePersistenceDuration.kt content:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.serialization.Serializable

@Serializable
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

**Step 3: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/ChecklistColor.kt
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/StatePersistenceDuration.kt
git commit -m "feat(serialization): add @Serializable to enum classes"
```

---

## Task 9: Create Basic Serialization Tests

**Files:**
- Create: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt`

**Step 1: Create test file with basic value class tests**

Create the file:

```kotlin
package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun checklist_id_roundtrip() {
        val original = ChecklistId("test-123")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistId>(jsonString)

        assertEquals(original, decoded)
        assertEquals("\"test-123\"", jsonString)
    }

    @Test
    fun checklist_item_id_roundtrip() {
        val original = ChecklistItemId("item-456")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistItemId>(jsonString)

        assertEquals(original, decoded)
        assertEquals("\"item-456\"", jsonString)
    }

    @Test
    fun item_icon_id_roundtrip() {
        val original = ItemIconId("tooth")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ItemIconId>(jsonString)

        assertEquals(original, decoded)
        assertEquals("\"tooth\"", jsonString)
    }
}
```

**Step 2: Run tests to verify value class serializers work**

Run: `./gradlew :common:testDebugUnitTest --tests ChecklistSerializationTest`
Expected: PASS (3 tests)

**Step 3: Commit**

```bash
git add common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt
git commit -m "test(serialization): add value class serialization tests"
```

---

## Task 10: Add Enum and DateTime Tests

**Files:**
- Modify: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt`

**Step 1: Add enum and datetime tests**

Add these tests to the test class:

```kotlin
    @Test
    fun checklist_color_roundtrip() {
        val original = ChecklistColor.CALM_GREEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistColor>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"CALM_GREEN\""))
    }

    @Test
    fun state_persistence_duration_roundtrip() {
        val original = StatePersistenceDuration.ONE_HOUR
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<StatePersistenceDuration>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"ONE_HOUR\""))
    }

    @Test
    fun day_of_week_roundtrip() {
        val original = DayOfWeek.WEDNESDAY
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<DayOfWeek>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"WEDNESDAY\""))
    }

    @Test
    fun local_time_roundtrip() {
        val original = LocalTime(14, 30)
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<LocalTime>(jsonString)

        assertEquals(original, decoded)
    }

    @Test
    fun instant_roundtrip() {
        val original = Instant.parse("2024-01-15T10:30:00Z")
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<Instant>(jsonString)

        assertEquals(original, decoded)
    }
```

**Step 2: Run tests**

Run: `./gradlew :common:testDebugUnitTest --tests ChecklistSerializationTest`
Expected: PASS (8 tests)

**Step 3: Commit**

```bash
git add common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt
git commit -m "test(serialization): add enum and datetime serialization tests"
```

---

## Task 11: Add Sealed Class Tests

**Files:**
- Modify: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt`

**Step 1: Add sealed class tests**

Add these tests to the test class:

```kotlin
    @Test
    fun timerange_allday_roundtrip() {
        val original = TimeRange.AllDay
        val jsonString = json.encodeToString<TimeRange>(original)
        val decoded = json.decodeFromString<TimeRange>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"type\""))
        assertTrue(jsonString.contains("\"AllDay\""))
    }

    @Test
    fun timerange_specific_roundtrip() {
        val original = TimeRange.Specific(
            startTime = LocalTime(8, 30),
            endTime = LocalTime(16, 45)
        )
        val jsonString = json.encodeToString<TimeRange>(original)
        val decoded = json.decodeFromString<TimeRange>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"type\""))
        assertTrue(jsonString.contains("\"Specific\""))
        assertTrue(jsonString.contains("\"startTime\""))
        assertTrue(jsonString.contains("\"endTime\""))
    }

    @Test
    fun checklist_item_state_pending_roundtrip() {
        val original = ChecklistItemState.Pending
        val jsonString = json.encodeToString<ChecklistItemState>(original)
        val decoded = json.decodeFromString<ChecklistItemState>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"type\""))
        assertTrue(jsonString.contains("\"Pending\""))
    }

    @Test
    fun checklist_item_state_done_roundtrip() {
        val original = ChecklistItemState.Done
        val jsonString = json.encodeToString<ChecklistItemState>(original)
        val decoded = json.decodeFromString<ChecklistItemState>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"Done\""))
    }

    @Test
    fun checklist_item_state_ignored_roundtrip() {
        val original = ChecklistItemState.IgnoredToday
        val jsonString = json.encodeToString<ChecklistItemState>(original)
        val decoded = json.decodeFromString<ChecklistItemState>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"IgnoredToday\""))
    }
```

**Step 2: Run tests**

Run: `./gradlew :common:testDebugUnitTest --tests ChecklistSerializationTest`
Expected: PASS (13 tests)

**Step 3: Commit**

```bash
git add common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt
git commit -m "test(serialization): add sealed class serialization tests"
```

---

## Task 12: Add Complex Object Tests

**Files:**
- Modify: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt`

**Step 1: Add complex object tests**

Add these tests to the test class:

```kotlin
    @Test
    fun checklist_schedule_roundtrip() {
        val original = ChecklistSchedule(
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.FRIDAY
            ),
            timeRange = TimeRange.Specific(
                startTime = LocalTime(6, 0),
                endTime = LocalTime(9, 0)
            )
        )

        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistSchedule>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"daysOfWeek\""))
        assertTrue(jsonString.contains("\"timeRange\""))
    }

    @Test
    fun checklist_item_roundtrip() {
        val original = ChecklistItem(
            id = ChecklistItemId("test-item"),
            title = "Test Item",
            iconId = ItemIconId("icon-1"),
            state = ChecklistItemState.Done
        )

        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistItem>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"id\""))
        assertTrue(jsonString.contains("\"title\""))
        assertTrue(jsonString.contains("\"iconId\""))
        assertTrue(jsonString.contains("\"state\""))
    }

    @Test
    fun checklist_item_with_null_icon_roundtrip() {
        val original = ChecklistItem(
            id = ChecklistItemId("test"),
            title = "Item",
            iconId = null,
            state = ChecklistItemState.Pending
        )

        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<ChecklistItem>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"iconId\": null") || jsonString.contains("\"iconId\":null"))
    }

    @Test
    fun checklist_roundtrip() {
        val original = Checklist(
            id = ChecklistId("test-id"),
            name = "Test Checklist",
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(DayOfWeek.MONDAY),
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
            lastAccessedAt = Instant.parse("2024-01-15T10:30:00Z")
        )

        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<Checklist>(jsonString)

        assertEquals(original, decoded)
        assertTrue(jsonString.contains("\"id\""))
        assertTrue(jsonString.contains("\"name\""))
        assertTrue(jsonString.contains("\"schedule\""))
        assertTrue(jsonString.contains("\"items\""))
        assertTrue(jsonString.contains("\"color\""))
        assertTrue(jsonString.contains("\"statePersistence\""))
        assertTrue(jsonString.contains("\"lastAccessedAt\""))
    }
```

**Step 2: Run tests**

Run: `./gradlew :common:testDebugUnitTest --tests ChecklistSerializationTest`
Expected: PASS (17 tests)

**Step 3: Commit**

```bash
git add common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt
git commit -m "test(serialization): add complex object serialization tests"
```

---

## Task 13: Add List and Format Validation Tests

**Files:**
- Modify: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt`

**Step 1: Add list and format validation tests**

Add these final tests to the test class:

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
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ),
                    timeRange = TimeRange.Specific(
                        startTime = LocalTime(10, 0),
                        endTime = LocalTime(12, 0)
                    )
                ),
                items = emptyList(),
                color = ChecklistColor.WARM_PEACH,
                statePersistence = StatePersistenceDuration.ONE_DAY,
                lastAccessedAt = null
            )
        )

        val jsonString = json.encodeToString(checklists)
        val decoded = json.decodeFromString<List<Checklist>>(jsonString)

        assertEquals(2, decoded.size)
        assertEquals(checklists, decoded)
    }

    @Test
    fun empty_checklist_list_roundtrip() {
        val original = emptyList<Checklist>()
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<List<Checklist>>(jsonString)

        assertEquals(original, decoded)
        assertEquals("[]", jsonString.trim())
    }

    @Test
    fun json_uses_camelcase_field_names() {
        val checklist = Checklist(
            id = ChecklistId("test"),
            name = "Test",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = emptyList(),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = Instant.parse("2024-01-01T00:00:00Z")
        )

        val jsonString = json.encodeToString(checklist)

        // Verify camelCase field names
        assertTrue(jsonString.contains("\"lastAccessedAt\""))
        assertTrue(jsonString.contains("\"statePersistence\""))
        // Verify NOT snake_case
        assertTrue(!jsonString.contains("\"last_accessed_at\""))
        assertTrue(!jsonString.contains("\"state_persistence\""))
    }

    @Test
    fun json_uses_type_discriminator_for_sealed_classes() {
        val timeRange = TimeRange.Specific(
            startTime = LocalTime(8, 0),
            endTime = LocalTime(17, 0)
        )

        val jsonString = json.encodeToString<TimeRange>(timeRange)

        // Verify "type" discriminator field
        assertTrue(jsonString.contains("\"type\""))
        assertTrue(jsonString.contains("\"Specific\""))
    }
```

**Step 2: Run all tests**

Run: `./gradlew :common:testDebugUnitTest --tests ChecklistSerializationTest`
Expected: PASS (21 tests)

**Step 3: Commit**

```bash
git add common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistSerializationTest.kt
git commit -m "test(serialization): add list and format validation tests"
```

---

## Task 14: Delete Old Kondor Files

**Files:**
- Delete: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`
- Delete: `common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 1: Delete ChecklistCodecs.kt**

Run: `rm common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecs.kt`

**Step 2: Delete ChecklistCodecsTest.kt**

Run: `rm common/src/androidUnitTest/kotlin/com/pyanpyan/domain/repository/json/ChecklistCodecsTest.kt`

**Step 3: Commit**

```bash
git add -A
git commit -m "refactor: remove kondor-based codecs and tests"
```

---

## Task 15: Verify Repository Still Works

**Files:**
- Read: `common/src/androidMain/kotlin/com/pyanpyan/domain/repository/JsonChecklistRepository.kt`

**Step 1: Verify JsonChecklistRepository compiles**

The JsonChecklistRepository already uses kotlinx-serialization and should work without changes.

Run: `./gradlew :common:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run all tests**

Run: `./gradlew :common:testDebugUnitTest`
Expected: All tests PASS

**Step 3: Verify no DEX conflicts**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL (no DEX errors)

---

## Task 16: Final Commit and Verification

**Step 1: Run full test suite**

Run: `./gradlew test`
Expected: All tests PASS

**Step 2: Clean build to verify no issues**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 3: Create final summary commit if any loose ends**

If all tests pass and build succeeds, the migration is complete.

---

## Success Criteria

- ✅ All kondor dependencies removed from build files
- ✅ All domain models have @Serializable annotations
- ✅ Custom serializers for value classes work correctly
- ✅ Sealed classes use polymorphic serialization with "type" discriminator
- ✅ JSON uses camelCase field naming
- ✅ All serialization tests pass (21 tests)
- ✅ JsonChecklistRepository works without changes
- ✅ No DEX conflicts when building Android app
- ✅ Old kondor files deleted

---

## Rollback Plan

If issues arise:

1. Revert to previous commit: `git reset --hard HEAD~N` (where N is number of commits)
2. Or revert specific changes: `git revert <commit-hash>`
3. The kondor dependencies can be temporarily re-added to build.gradle.kts if needed

---

## References

- Design doc: `docs/plans/2026-02-14-kotlinx-serialization-migration-design.md`
- kotlinx-serialization docs: https://github.com/Kotlin/kotlinx.serialization
- Domain models: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/`
- Repository: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/`
