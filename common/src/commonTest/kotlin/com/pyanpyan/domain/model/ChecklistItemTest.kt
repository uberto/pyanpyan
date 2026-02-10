package com.pyanpyan.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistItemTest {

    @Test
    fun `checklist item has id, title, and state`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        assertEquals(ChecklistItemId("item-1"), item.id)
        assertEquals("Brush teeth", item.title)
        assertEquals(ChecklistItemState.Pending, item.state)
    }

    @Test
    fun `item can be marked done`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        val doneItem = item.markDone()

        assertEquals(ChecklistItemState.Done, doneItem.state)
        assertEquals(item.id, doneItem.id)
        assertEquals(item.title, doneItem.title)
    }

    @Test
    fun `item can be ignored today`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        val ignoredItem = item.ignoreToday()

        assertEquals(ChecklistItemState.IgnoredToday, ignoredItem.state)
    }

    @Test
    fun `item can be reset to pending`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Done
        )

        val resetItem = item.reset()

        assertEquals(ChecklistItemState.Pending, resetItem.state)
    }
}
