package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    @Test
    fun rejects_blank_checklist_name() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CreateChecklist(
                id = ChecklistId("test"),
                name = "   ", // blank
                schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
                items = listOf(
                    ChecklistItem(
                        id = ChecklistItemId("item1"),
                        title = "Item",
                        state = ChecklistItemState.Pending
                    )
                ),
                color = ChecklistColor.SOFT_BLUE,
                statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
            )
        }
        assertEquals("Checklist name cannot be blank", exception.message)
    }

    @Test
    fun rejects_invalid_time_range() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CreateChecklist(
                id = ChecklistId("test"),
                name = "Test",
                schedule = ChecklistSchedule(
                    emptySet(),
                    TimeRange.Specific(
                        startTime = LocalTime(9, 0),
                        endTime = LocalTime(6, 0) // end before start!
                    )
                ),
                items = listOf(
                    ChecklistItem(
                        id = ChecklistItemId("item1"),
                        title = "Item",
                        state = ChecklistItemState.Pending
                    )
                ),
                color = ChecklistColor.SOFT_BLUE,
                statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
            )
        }
        assertEquals("Start time must be before end time", exception.message)
    }

    @Test
    fun rejects_duplicate_item_ids() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CreateChecklist(
                id = ChecklistId("test"),
                name = "Test",
                schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
                items = listOf(
                    ChecklistItem(
                        id = ChecklistItemId("duplicate"),
                        title = "Item 1",
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("duplicate"), // same ID!
                        title = "Item 2",
                        state = ChecklistItemState.Pending
                    )
                ),
                color = ChecklistColor.SOFT_BLUE,
                statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
            )
        }
        assertEquals("Checklist items must have unique IDs", exception.message)
    }
}
