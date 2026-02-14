package com.pyanpyan.domain.repository

import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.repository.json.DefaultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class JsonChecklistRepository(
    private val storageDir: File
) : ChecklistRepository {

    private val fileName = "checklists.json"
    private val file: File
        get() = File(storageDir, fileName)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun getAllChecklists(): RepositoryResult<List<Checklist>> =
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    val defaultChecklists = DefaultData.createDefaultChecklists()
                    saveAllChecklists(defaultChecklists).let { result ->
                        if (result is RepositoryResult.Failure) return@withContext result
                    }
                    return@withContext RepositoryResult.Success(defaultChecklists)
                }

                val jsonText = file.readText()
                val checklists = json.decodeFromString<List<Checklist>>(jsonText)
                RepositoryResult.Success(checklists)
            } catch (e: IOException) {
                RepositoryResult.Failure(
                    RepositoryError.FileReadError(e.message ?: "Failed to read file", e)
                )
            } catch (e: Exception) {
                RepositoryResult.Failure(
                    RepositoryError.JsonParseError(e.message ?: "Failed to parse JSON", e)
                )
            }
        }

    override suspend fun getChecklist(id: ChecklistId): RepositoryResult<Checklist?> =
        getAllChecklists().map { checklists ->
            checklists.find { it.id == id }
        }

    override suspend fun saveChecklist(checklist: Checklist): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            getAllChecklists().flatMap { checklists ->
                val updated = checklists.filter { it.id != checklist.id } + checklist
                saveAllChecklists(updated)
            }
        }

    override suspend fun deleteChecklist(id: ChecklistId): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            getAllChecklists().flatMap { checklists ->
                val updated = checklists.filter { it.id != id }
                saveAllChecklists(updated)
            }
        }

    override suspend fun exportToJson(): RepositoryResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = if (!file.exists()) "[]" else file.readText()
                RepositoryResult.Success(result)
            } catch (e: Exception) {
                RepositoryResult.Failure(
                    RepositoryError.FileReadError(e.message ?: "Failed to export", e)
                )
            }
        }

    override suspend fun importFromJson(jsonText: String): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val checklists = json.decodeFromString<List<Checklist>>(jsonText)
                saveAllChecklists(checklists)
            } catch (e: Exception) {
                RepositoryResult.Failure(
                    RepositoryError.JsonParseError(e.message ?: "Failed to parse JSON", e)
                )
            }
        }

    private fun saveAllChecklists(checklists: List<Checklist>): RepositoryResult<Unit> =
        try {
            val jsonText = json.encodeToString(checklists)
            file.writeText(jsonText)
            RepositoryResult.Success(Unit)
        } catch (e: IOException) {
            RepositoryResult.Failure(
                RepositoryError.FileWriteError(e.message ?: "Failed to write file", e)
            )
        } catch (e: Exception) {
            RepositoryResult.Failure(
                RepositoryError.FileWriteError(e.message ?: "Unknown error", e)
            )
        }
}
