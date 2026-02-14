package com.pyanpyan.android.data

import android.content.Context
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.JsonChecklistRepository

object RepositoryFactory {
    @Volatile
    private var repository: ChecklistRepository? = null

    fun getRepository(context: Context): ChecklistRepository =
        repository ?: synchronized(this) {
            repository ?: JsonChecklistRepository(context.filesDir).also {
                repository = it
            }
        }
}
