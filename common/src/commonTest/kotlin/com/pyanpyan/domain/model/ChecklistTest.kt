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
