package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.model.ChecklistItemState
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkItemDoneTest {

    @Test
    fun `command contains item id`() {
        val command = MarkItemDone(ChecklistItemId("item-1"))
        assertEquals(ChecklistItemId("item-1"), command.itemId)
    }

    @Test
    fun `executing command marks item as done`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )

        val command = MarkItemDone(item.id)
        val result = command.execute(item)

        assertEquals(ChecklistItemState.Done, result.state)
    }
}
