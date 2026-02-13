package com.pyanpyan.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChecklistTest {

fun createTestChecklist(
    id: ChecklistId = ChecklistId("test-checklist"),
    name: String = "Test Checklist",
    schedule: ChecklistSchedule = ChecklistSchedule(
        daysOfWeek = emptySet(),
        timeRange = TimeRange.AllDay
    ),
    items: List<ChecklistItem> = emptyList(),
    color: ChecklistColor = ChecklistColor.SOFT_BLUE,
    statePersistence: StatePersistenceDuration = StatePersistenceDuration.DEFAULT,
    lastAccessedAt: Instant? = null
): Checklist = Checklist(id, name, schedule, items, color, statePersistence, lastAccessedAt)

    @Test
    fun `checklist can contain items`() {
        val items = listOf(
            ChecklistItem(
                id = ChecklistItemId("item-1"),
                title = "Brush teeth",
                state = ChecklistItemState.Pending
            ),
            ChecklistItem(
                id = ChecklistItemId("item-2"),
                title = "Get dressed",
                state = ChecklistItemState.Pending
            )
        )

        val checklist = createTestChecklist(
            id = ChecklistId("morning-routine"),
            name = "Morning Routine",
            items = items
        )

        assertEquals(2, checklist.items.size)
        assertEquals("Morning Routine", checklist.name)
    }

    @Test
    fun `checklist can update item state`() {
        val item = ChecklistItem(
            id = ChecklistItemId("item-1"),
            title = "Brush teeth",
            state = ChecklistItemState.Pending
        )
        val checklist = createTestChecklist(
            id = ChecklistId("morning-routine"),
            name = "Morning Routine",
            items = listOf(item)
        )

        val updated = checklist.updateItem(item.markDone())

        assertEquals(ChecklistItemState.Done, updated.items[0].state)
    }

    @Test
    fun `checklist can reset all items to pending`() {
        val items = listOf(
            ChecklistItem(
                id = ChecklistItemId("item-1"),
                title = "Brush teeth",
                state = ChecklistItemState.Done
            ),
            ChecklistItem(
                id = ChecklistItemId("item-2"),
                title = "Get dressed",
                state = ChecklistItemState.IgnoredToday
            )
        )
        val checklist = createTestChecklist(
            id = ChecklistId("morning-routine"),
            name = "Morning Routine",
            items = items
        )

        val reset = checklist.resetAllItems()

        assertTrue(reset.items.all { it.state == ChecklistItemState.Pending })
    }

    @Test
    fun `checklist has name and visual properties`() {
        val checklist = Checklist(
            id = ChecklistId("morning"),
            name = "Morning Routine",
            schedule = ChecklistSchedule(
                daysOfWeek = emptySet(),
                timeRange = TimeRange.AllDay
            ),
            items = listOf(),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = null
        )

        assertEquals("Morning Routine", checklist.name)
        assertEquals(ChecklistColor.SOFT_BLUE, checklist.color)
        assertEquals(StatePersistenceDuration.FIFTEEN_MINUTES, checklist.statePersistence)
    }

    @Test
    fun `checklist tracks last access time`() {
        val now = Clock.System.now()
        val checklist = Checklist(
            id = ChecklistId("test"),
            name = "Test",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = listOf(),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = now
        )

        assertEquals(now, checklist.lastAccessedAt)
    }
}
