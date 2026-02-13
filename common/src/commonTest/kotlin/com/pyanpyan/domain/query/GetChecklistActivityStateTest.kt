// common/src/commonTest/kotlin/com/pyanpyan/domain/query/GetChecklistActivityStateTest.kt
package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.*
import kotlinx.datetime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetChecklistActivityStateTest {
    @Test
    fun always_on_checklist_is_always_active() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.AllDay
            )
        )

        val monday9am = createDateTime(DayOfWeek.MONDAY, 9, 0)
        val saturday3pm = createDateTime(DayOfWeek.SATURDAY, 15, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(monday9am))
        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(saturday3pm))
    }

    @Test
    fun weekday_only_checklist_inactive_on_weekend() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                ),
                timeRange = TimeRange.AllDay
            )
        )

        val monday = createDateTime(DayOfWeek.MONDAY, 10, 0)
        val saturday = createDateTime(DayOfWeek.SATURDAY, 10, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(monday))
        assertEquals(ChecklistActivityState.Inactive, checklist.getActivityState(saturday))
    }

    @Test
    fun morning_routine_active_only_in_time_range() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.Specific(
                    startTime = LocalTime(6, 0),
                    endTime = LocalTime(9, 0)
                )
            )
        )

        val morning7am = createDateTime(DayOfWeek.MONDAY, 7, 0)
        val afternoon3pm = createDateTime(DayOfWeek.MONDAY, 15, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(morning7am))
        assertEquals(ChecklistActivityState.Inactive, checklist.getActivityState(afternoon3pm))
    }

    @Test
    fun weekday_morning_checklist_inactive_on_weekend_morning() {
        val checklist = createChecklist(
            schedule = ChecklistSchedule(
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                timeRange = TimeRange.Specific(
                    startTime = LocalTime(6, 0),
                    endTime = LocalTime(9, 0)
                )
            )
        )

        val mondayMorning = createDateTime(DayOfWeek.MONDAY, 7, 0)
        val saturdayMorning = createDateTime(DayOfWeek.SATURDAY, 7, 0)

        assertEquals(ChecklistActivityState.Active, checklist.getActivityState(mondayMorning))
        assertEquals(ChecklistActivityState.Inactive, checklist.getActivityState(saturdayMorning))
    }

    private fun createChecklist(schedule: ChecklistSchedule) = Checklist(
        id = ChecklistId("test"),
        name = "Test",
        schedule = schedule,
        items = emptyList(),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
    )

    private fun createDateTime(dayOfWeek: DayOfWeek, hour: Int, minute: Int): LocalDateTime {
        // Find next occurrence of the day
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val targetDay = today.plus(
            (dayOfWeek.ordinal - today.dayOfWeek.ordinal + 7) % 7,
            DateTimeUnit.DAY
        )
        return LocalDateTime(
            targetDay.year,
            targetDay.month,
            targetDay.dayOfMonth,
            hour,
            minute
        )
    }
}
