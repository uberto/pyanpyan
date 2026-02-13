package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*

data class CreateChecklist(
    val id: ChecklistId,
    val name: String,
    val schedule: ChecklistSchedule,
    val items: List<ChecklistItem>,
    val color: ChecklistColor,
    val statePersistence: StatePersistenceDuration
) {
    fun execute(): Checklist {
        return Checklist(
            id = id,
            name = name,
            schedule = schedule,
            items = items,
            color = color,
            statePersistence = statePersistence,
            lastAccessedAt = null
        )
    }
}
