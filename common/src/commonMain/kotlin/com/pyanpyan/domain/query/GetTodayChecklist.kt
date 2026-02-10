package com.pyanpyan.domain.query

import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId

data class GetTodayChecklist(val checklistId: ChecklistId) {
    fun <R> execute(repository: R): Checklist? where R : ChecklistRepository {
        return repository.findById(checklistId)
    }
}

interface ChecklistRepository {
    fun findById(id: ChecklistId): Checklist?
}
