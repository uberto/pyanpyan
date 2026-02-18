package com.pyanpyan.android.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.model.CompletionSound
import com.pyanpyan.domain.model.SwipeSound
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

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
}
