package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTodayChecklistTest {

    @Test
    fun `query returns checklist by id`() {
        val checklist = Checklist(
            id = ChecklistId("morning"),
            name = "Morning Routine",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = emptyList(),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = null
        )
        val repository = FakeChecklistRepository(listOf(checklist))

        val query = GetTodayChecklist(ChecklistId("morning"))
        val result = query.execute(repository)

        assertEquals(checklist, result)
    }
}

class FakeChecklistRepository(private val checklists: List<Checklist>) : ChecklistRepository {
    override fun findById(id: ChecklistId): Checklist? {
        return checklists.find { it.id == id }
    }
}
