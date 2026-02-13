package com.pyanpyan.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecklistScheduleTest {
    @Test
    fun always_on_schedule_has_no_restrictions() {
        val schedule = ChecklistSchedule(
            daysOfWeek = emptySet(),
            timeRange = TimeRange.AllDay
        )

        assertTrue(schedule.isAlwaysOn)
    }

    @Test
    fun weekday_only_schedule() {
        val schedule = ChecklistSchedule(
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            timeRange = TimeRange.AllDay
        )

        assertFalse(schedule.isAlwaysOn)
        assertTrue(schedule.daysOfWeek.contains(DayOfWeek.MONDAY))
        assertFalse(schedule.daysOfWeek.contains(DayOfWeek.SATURDAY))
    }

    @Test
    fun morning_routine_schedule() {
        val schedule = ChecklistSchedule(
            daysOfWeek = emptySet(),
            timeRange = TimeRange.Specific(
                startTime = LocalTime(6, 0),
                endTime = LocalTime(9, 0)
            )
        )

        assertFalse(schedule.isAlwaysOn)
    }
}
