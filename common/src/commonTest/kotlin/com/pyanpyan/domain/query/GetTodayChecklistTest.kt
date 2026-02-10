package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTodayChecklistTest {

    @Test
    fun `query returns checklist by id`() {
        val checklist = Checklist(
            id = ChecklistId("morning"),
            title = "Morning Routine",
            items = emptyList()
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
