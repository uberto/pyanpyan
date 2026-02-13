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
            name = "Daily Tasks",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = listOf(item),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = null
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
