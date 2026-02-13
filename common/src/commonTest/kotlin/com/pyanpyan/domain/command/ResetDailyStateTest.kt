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
            name = "Morning Routine",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = items,
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = null
        )

        val command = ResetDailyState()
        val result = command.execute(checklist)

        assertTrue(result.items.all { it.state == ChecklistItemState.Pending })
    }
}
