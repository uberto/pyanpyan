package com.pyanpyan.domain.repository

import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId
import com.uberto.kondor.outcome.Outcome

interface ChecklistRepository {
    /**
     * Get all checklists. Returns empty list if no data exists (first run).
     * Returns failure only on actual I/O errors.
     */
    suspend fun getAllChecklists(): Outcome<RepositoryError, List<Checklist>>

    /**
     * Get a specific checklist by ID.
     * Returns null if not found (wrapped in Success).
     */
    suspend fun getChecklist(id: ChecklistId): Outcome<RepositoryError, Checklist?>

    /**
     * Save or update a checklist. Creates if new, updates if exists.
     */
    suspend fun saveChecklist(checklist: Checklist): Outcome<RepositoryError, Unit>

    /**
     * Delete a checklist by ID. Succeeds even if checklist doesn't exist.
     */
    suspend fun deleteChecklist(id: ChecklistId): Outcome<RepositoryError, Unit>

    /**
     * Export all data as JSON string for backup.
     */
    suspend fun exportToJson(): Outcome<RepositoryError, String>

    /**
     * Import data from JSON string, replacing all existing data.
     */
    suspend fun importFromJson(json: String): Outcome<RepositoryError, Unit>
}
