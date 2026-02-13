package com.pyanpyan.domain.command

import com.pyanpyan.domain.model.*

data class UpdateChecklist(
    val checklist: Checklist,
    val name: String? = null,
    val schedule: ChecklistSchedule? = null,
    val color: ChecklistColor? = null,
    val statePersistence: StatePersistenceDuration? = null
) {
    init {
        // Validate name if provided
        name?.let {
            require(it.isNotBlank()) { "Checklist name cannot be blank" }
        }

        // Validate time range if schedule provided
        schedule?.let {
            val timeRange = it.timeRange
            if (timeRange is TimeRange.Specific) {
                require(timeRange.startTime < timeRange.endTime) {
                    "Start time must be before end time"
                }
            }
        }
    }

    fun execute(): Checklist {
        return checklist.copy(
            name = name ?: checklist.name,
            schedule = schedule ?: checklist.schedule,
            color = color ?: checklist.color,
            statePersistence = statePersistence ?: checklist.statePersistence
        )
    }
}
