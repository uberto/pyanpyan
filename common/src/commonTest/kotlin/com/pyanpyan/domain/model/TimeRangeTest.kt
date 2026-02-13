// common/src/commonTest/kotlin/com/pyanpyan/domain/model/TimeRangeTest.kt
package com.pyanpyan.domain.model

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeRangeTest {
    @Test
    fun allDay_has_no_restrictions() {
        val range = TimeRange.AllDay

        assertTrue(range.isAllDay)
    }

    @Test
    fun specific_range_has_start_and_end() {
        val start = LocalTime(9, 0)
        val end = LocalTime(17, 0)
        val range = TimeRange.Specific(start, end)

        assertFalse(range.isAllDay)
        assertEquals(start, range.startTime)
        assertEquals(end, range.endTime)
    }

    @Test
    fun time_is_within_specific_range() {
        val range = TimeRange.Specific(
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0)
        )

        val morning = LocalTime(10, 30)
        assertTrue(range.contains(morning))

        val evening = LocalTime(20, 0)
        assertFalse(range.contains(evening))
    }

    @Test
    fun allDay_contains_any_time() {
        val range = TimeRange.AllDay

        assertTrue(range.contains(LocalTime(0, 0)))
        assertTrue(range.contains(LocalTime(12, 0)))
        assertTrue(range.contains(LocalTime(23, 59)))
    }

    @Test
    fun time_at_exact_start_is_within_range() {
        val range = TimeRange.Specific(
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0)
        )

        assertTrue(range.contains(LocalTime(9, 0)))
    }

    @Test
    fun time_at_exact_end_is_within_range() {
        val range = TimeRange.Specific(
            startTime = LocalTime(9, 0),
            endTime = LocalTime(17, 0)
        )

        assertTrue(range.contains(LocalTime(17, 0)))
    }
}
