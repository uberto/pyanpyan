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
    init {
        require(name.isNotBlank()) { "Checklist name cannot be blank" }

        // Validate time range if specific
        val timeRange = schedule.timeRange
        if (timeRange is TimeRange.Specific) {
            require(timeRange.startTime < timeRange.endTime) {
                "Start time must be before end time"
            }
        }

        // Validate unique item IDs
        val uniqueIds = items.map { it.id }.toSet()
        require(uniqueIds.size == items.size) {
            "Checklist items must have unique IDs"
        }
    }

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
