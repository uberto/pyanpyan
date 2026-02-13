package com.pyanpyan.domain.model

import kotlinx.datetime.Instant

@JvmInline
value class ChecklistId(val value: String)

data class Checklist(
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration,
    val lastAccessedAt: Instant? = null
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
