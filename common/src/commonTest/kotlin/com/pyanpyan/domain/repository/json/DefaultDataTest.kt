package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDataTest {
    @Test
    fun creates_school_checklist() {
        val checklists = DefaultData.createDefaultChecklists()

        assertEquals(1, checklists.size)

        val school = checklists[0]
        assertEquals(ChecklistId("school"), school.id)
        assertEquals("School", school.name)
        assertEquals(ChecklistColor.SOFT_BLUE, school.color)
        assertEquals(StatePersistenceDuration.FIFTEEN_MINUTES, school.statePersistence)
    }

    @Test
    fun school_checklist_has_five_items() {
        val school = DefaultData.createDefaultChecklists()[0]

        assertEquals(5, school.items.size)
        assertEquals("Books in bag", school.items[0].title)
        assertEquals("Homework", school.items[1].title)
        assertEquals("PE kit", school.items[2].title)
        assertEquals("Breakfast", school.items[3].title)
        assertEquals("Brushing teeth", school.items[4].title)
    }

    @Test
    fun school_checklist_is_weekdays_only() {
        val school = DefaultData.createDefaultChecklists()[0]

        assertEquals(5, school.schedule.daysOfWeek.size)
        assert(school.schedule.daysOfWeek.contains(DayOfWeek.MONDAY))
        assert(school.schedule.daysOfWeek.contains(DayOfWeek.FRIDAY))
        assert(!school.schedule.daysOfWeek.contains(DayOfWeek.SATURDAY))
        assert(!school.schedule.daysOfWeek.contains(DayOfWeek.SUNDAY))
    }

    @Test
    fun school_checklist_is_all_day() {
        val school = DefaultData.createDefaultChecklists()[0]

        assertEquals(TimeRange.AllDay, school.schedule.timeRange)
    }

    @Test
    fun all_items_start_as_pending() {
        val school = DefaultData.createDefaultChecklists()[0]

        assert(school.items.all { it.state == ChecklistItemState.Pending })
    }

    @Test
    fun brushing_teeth_has_tooth_icon() {
        val school = DefaultData.createDefaultChecklists()[0]
        val brushingTeeth = school.items[4]

        assertEquals(ItemIconId("tooth"), brushingTeeth.iconId)
    }
}
