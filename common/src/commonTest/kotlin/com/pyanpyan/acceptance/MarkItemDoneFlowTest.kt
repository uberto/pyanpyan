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
            name = "Morning Routine",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = listOf(item),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = null
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
