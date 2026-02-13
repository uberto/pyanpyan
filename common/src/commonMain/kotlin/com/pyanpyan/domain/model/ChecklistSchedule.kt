package com.pyanpyan.domain.model

import kotlinx.datetime.DayOfWeek

data class ChecklistSchedule(
    val daysOfWeek: Set<DayOfWeek>, // empty set = all days
    val timeRange: TimeRange
) {
    val isAlwaysOn: Boolean
        get() = daysOfWeek.isEmpty() && timeRange.isAllDay
}
