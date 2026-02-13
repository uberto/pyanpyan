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
