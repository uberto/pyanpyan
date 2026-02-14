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
}
