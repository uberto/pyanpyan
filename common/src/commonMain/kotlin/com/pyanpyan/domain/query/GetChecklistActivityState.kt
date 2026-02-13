// common/src/commonMain/kotlin/com/pyanpyan/domain/query/GetChecklistActivityState.kt
package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.Checklist
import kotlinx.datetime.LocalDateTime

sealed class ChecklistActivityState {
    object Active : ChecklistActivityState()
    object Inactive : ChecklistActivityState()
}

fun Checklist.getActivityState(currentTime: LocalDateTime): ChecklistActivityState {
    // Check day of week if specified
    if (schedule.daysOfWeek.isNotEmpty() &&
        currentTime.dayOfWeek !in schedule.daysOfWeek
    ) {
        return ChecklistActivityState.Inactive
    }

    // Check time range
    val currentLocalTime = currentTime.time
    return if (schedule.timeRange.contains(currentLocalTime)) {
        ChecklistActivityState.Active
    } else {
        ChecklistActivityState.Inactive
    }
}
