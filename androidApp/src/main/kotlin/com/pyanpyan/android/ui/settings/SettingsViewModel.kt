package com.pyanpyan.android.ui.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.CompletionSound
import com.pyanpyan.domain.model.SwipeSound
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val checklistRepository: ChecklistRepository,
    private val context: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun updateSwipeSound(sound: SwipeSound) {
        viewModelScope.launch {
            val updated = settings.value.copy(swipeSound = sound)
            repository.updateSettings(updated)
                .onFailure { error ->
                    Log.e("SettingsViewModel", "Failed to update swipe sound: $error")
                }
        }
    }

    fun updateCompletionSound(sound: CompletionSound) {
        viewModelScope.launch {
            val updated = settings.value.copy(completionSound = sound)
            repository.updateSettings(updated)
                .onFailure { error ->
                    Log.e("SettingsViewModel", "Failed to update completion sound: $error")
                }
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            val updated = settings.value.copy(enableHapticFeedback = enabled)
            repository.updateSettings(updated)
                .onFailure { error ->
                    Log.e("SettingsViewModel", "Failed to update haptic feedback: $error")
                }
        }
    }

    fun updateFontFamily(fontName: String?) {
        viewModelScope.launch {
            val sanitizedName = fontName?.takeIf { it.isNotBlank() }
            val updated = settings.value.copy(fontFamilyName = sanitizedName)
            repository.updateSettings(updated)
                .onFailure { error ->
                    Log.e("SettingsViewModel", "Failed to update font family: $error")
                }
        }
    }

    fun updateFontSize(scale: Float) {
        viewModelScope.launch {
            val updated = settings.value.copy(fontSizeScale = scale)
            repository.updateSettings(updated)
                .onFailure { error ->
                    Log.e("SettingsViewModel", "Failed to update font size: $error")
                }
        }
    }

    fun exportToFile(uri: Uri) {
        viewModelScope.launch {
            checklistRepository.exportToJson()
                .onSuccess { jsonString ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Checklists exported successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Could not save file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        Log.e("SettingsViewModel", "Export failed", e)
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SettingsViewModel", "Export failed: $error")
                }
        }
    }

    fun importFromFile(uri: Uri, onShowConfirmation: (onConfirm: () -> Unit) -> Unit) {
        viewModelScope.launch {
            try {
                // Read JSON from file
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw Exception("Could not read file")
                }

                // Parse and reset all item states to Pending
                val checklists = json.decodeFromString<List<Checklist>>(jsonString)
                val resetChecklists = checklists.map { checklist ->
                    checklist.copy(
                        items = checklist.items.map { it.reset() }
                    )
                }
                val resetJson = json.encodeToString(resetChecklists)

                // Show confirmation dialog on main thread
                withContext(Dispatchers.Main) {
                    onShowConfirmation {
                        // User confirmed - proceed with import
                        viewModelScope.launch {
                            checklistRepository.importFromJson(resetJson)
                                .onSuccess {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Checklists imported successfully", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .onFailure { error ->
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                                    }
                                    Log.e("SettingsViewModel", "Import failed: $error")
                                }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val message = when {
                        e.message?.contains("JSON") == true || e is kotlinx.serialization.SerializationException ->
                            "Invalid file format"
                        else -> "Could not read file"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                Log.e("SettingsViewModel", "Import failed", e)
            }
        }
    }
}
