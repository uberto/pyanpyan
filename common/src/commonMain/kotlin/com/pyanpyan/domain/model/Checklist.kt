package com.pyanpyan.domain.model

@JvmInline
value class ChecklistId(val value: String)

data class Checklist(
    val id: ChecklistId,
    val title: String,
    val items: List<ChecklistItem>
) {
    fun updateItem(updatedItem: ChecklistItem): Checklist {
        val newItems = items.map {
            if (it.id == updatedItem.id) updatedItem else it
        }
        return copy(items = newItems)
    }

    fun resetAllItems(): Checklist {
        return copy(items = items.map { it.reset() })
    }

    fun findItem(itemId: ChecklistItemId): ChecklistItem? {
        return items.find { it.id == itemId }
    }
}
