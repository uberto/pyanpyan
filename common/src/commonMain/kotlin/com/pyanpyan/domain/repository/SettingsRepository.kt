package com.pyanpyan.domain.repository

import com.pyanpyan.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /**
     * Observe settings changes
     */
    val settings: Flow<AppSettings>

    /**
     * Update settings
     */
    suspend fun updateSettings(settings: AppSettings): RepositoryResult<Unit>
}
