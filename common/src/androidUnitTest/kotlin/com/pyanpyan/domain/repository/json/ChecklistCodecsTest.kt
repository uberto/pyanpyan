package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import com.ubertob.kondor.outcome.*
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
}
