package com.pyanpyan.domain.repository.json

import com.pyanpyan.domain.model.*
import kotlinx.datetime.DayOfWeek

object DefaultData {
    fun createDefaultChecklists(): List<Checklist> = listOf(
        createSchoolChecklist()
    )

    private fun createSchoolChecklist() = Checklist(
        id = ChecklistId("school"),
        name = "School",
        schedule = ChecklistSchedule(
            daysOfWeek = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            timeRange = TimeRange.AllDay
        ),
        items = listOf(
            ChecklistItem(
                id = ChecklistItemId("books"),
                title = "Books in bag",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("homework"),
                title = "Homework",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("pe-kit"),
                title = "PE kit",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("breakfast"),
                title = "Breakfast",
                iconId = null,
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("brushing-teeth"),
                title = "Brushing teeth",
                iconId = ItemIconId("tooth"),
                state = ChecklistItemState.Pending
            )
        ),
        color = ChecklistColor.SOFT_BLUE,
        statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
        lastAccessedAt = null
    )
}
