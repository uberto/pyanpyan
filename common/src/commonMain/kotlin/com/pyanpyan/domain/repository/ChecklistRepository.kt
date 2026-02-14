package com.pyanpyan.domain.repository

import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId

/**
 * Repository interface for managing checklists with functional error handling.
 * All operations return RepositoryResult to handle errors in a type-safe way.
 */
interface ChecklistRepository {
    /**
     * Retrieves all checklists from storage.
     * @return Success with list of checklists, or Failure with error details
     */
    suspend fun getAllChecklists(): RepositoryResult<List<Checklist>>

    /**
     * Retrieves a specific checklist by ID.
     * @param id The checklist identifier
     * @return Success with checklist if found (or null), or Failure with error details
     */
    suspend fun getChecklist(id: ChecklistId): RepositoryResult<Checklist?>

    /**
     * Saves a checklist to storage (creates or updates).
     * @param checklist The checklist to save
     * @return Success if saved, or Failure with error details
     */
    suspend fun saveChecklist(checklist: Checklist): RepositoryResult<Unit>

    /**
     * Deletes a checklist from storage.
     * @param id The checklist identifier to delete
     * @return Success if deleted, or Failure with error details
     */
    suspend fun deleteChecklist(id: ChecklistId): RepositoryResult<Unit>

    /**
     * Exports all checklists to JSON format.
     * @return Success with JSON string, or Failure with error details
     */
    suspend fun exportToJson(): RepositoryResult<String>

    /**
     * Imports checklists from JSON format, replacing existing data.
     * @param json The JSON string containing checklists
     * @return Success if imported, or Failure with error details
     */
    suspend fun importFromJson(json: String): RepositoryResult<Unit>
}
