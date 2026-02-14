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
        encodeDefaults = true
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
}
