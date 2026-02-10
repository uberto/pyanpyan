package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemId

data class MarkItemDone(val itemId: ChecklistItemId) {
    fun execute(item: ChecklistItem): ChecklistItem {
        require(item.id == itemId) { "Item ID mismatch" }
        return item.markDone()
    }
}
