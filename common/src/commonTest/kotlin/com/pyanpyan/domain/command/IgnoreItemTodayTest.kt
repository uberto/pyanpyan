package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId
import com.pyanpyan.domain.model.ChecklistItemState
import kotlin.test.Test
import kotlin.test.assertEquals

class IgnoreItemTodayTest {

    @Test
    fun `command contains item id`() {
        val command = IgnoreItemToday(ChecklistItemId("item-1"))
        assertEquals(ChecklistItemId("item-1"), command.itemId)
    }

    @Test
    fun `executing command marks item as ignored today`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Exercise",
            state = ChecklistItemState.Pending
        )

        val command = IgnoreItemToday(item.id)
        val result = command.execute(item)

        assertEquals(ChecklistItemState.IgnoredToday, result.state)
    }
}
