package com.pyanpyan.android.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.repository.RepositoryError
import com.pyanpyan.domain.repository.RepositoryResult
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class DataStoreSettingsRepository(
    private val context: Context
) : SettingsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val settingsKey = stringPreferencesKey("settings_json")

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { exception ->
            // If error reading, emit default settings
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { preferences ->
            val jsonString = preferences[settingsKey]
            if (jsonString != null) {
                try {
                    json.decodeFromString<AppSettings>(jsonString)
                } catch (e: Exception) {
                    AppSettings() // Return default on parse error
                }
            } else {
                AppSettings() // Return default if not set
            }
        }

    override suspend fun updateSettings(settings: AppSettings): RepositoryResult<Unit> {
        return try {
            val jsonString = json.encodeToString(settings)
            context.settingsDataStore.edit { preferences ->
                preferences[settingsKey] = jsonString
            }
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Failure(RepositoryError.FileWriteError(e.message ?: "Failed to save settings", e))
        }
    }
}
