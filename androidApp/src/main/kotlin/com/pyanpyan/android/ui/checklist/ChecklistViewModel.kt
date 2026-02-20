package com.pyanpyan.android.ui.checklist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.android.sound.SoundManager
import com.pyanpyan.domain.command.IgnoreItemToday
import com.pyanpyan.domain.command.MarkItemDone
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.datetime.Clock

sealed class TimerState {
    object NotConfigured : TimerState()
    data class Running(val remainingSeconds: Int) : TimerState()
    data class Paused(val remainingSeconds: Int) : TimerState()
    object Expired : TimerState()
    data class Completed(val remainingSeconds: Int) : TimerState()
}

data class ChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = false,
    val timerState: TimerState = TimerState.NotConfigured
)

class ChecklistViewModel(
    private val checklistId: ChecklistId,
    private val repository: ChecklistRepository,
    context: Context?,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    private val soundManager: SoundManager = SoundManager(
        context = context?.applicationContext,
        settingsFlow = settingsRepository.settings,
        scope = viewModelScope
    )

    init {
        loadChecklist()
    }

    private fun loadChecklist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getChecklist(checklistId)
                .onSuccess { checklist ->
                    if (checklist == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        return@onSuccess
                    }

                    val now = Clock.System.now()

                    // Check if state should be reset
                    val shouldReset = checklist.lastAccessedAt?.let { lastAccess ->
                        val elapsed = now - lastAccess
                        elapsed > checklist.statePersistence.duration
                    } ?: false

                    val finalChecklist = if (shouldReset) {
                        // Reset all items and update timestamp
                        val reset = checklist.resetAllItems().copy(lastAccessedAt = now)
                        repository.saveChecklist(reset)
                            .onFailure {
                                // If save fails, use original checklist (don't show reset state)
                                _uiState.value = ChecklistUiState(
                                    checklist = checklist,
                                    isLoading = false
                                )
                                return@onSuccess
                            }
                        reset
                    } else {
                        // Just update timestamp
                        val updated = checklist.copy(lastAccessedAt = now)
                        repository.saveChecklist(updated)
                            .onFailure {
                                // If timestamp update fails, still show checklist
                                // This is less critical - just log or ignore
                            }
                        updated
                    }

                    _uiState.value = ChecklistUiState(
                        checklist = finalChecklist,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Error will be shown via SnackBar in UI
                }
        }
    }

    fun markItemDone(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = MarkItemDone(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        handleItemStateChange(updatedChecklist)
    }

    fun ignoreItemToday(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = IgnoreItemToday(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        handleItemStateChange(updatedChecklist)
    }

    fun resetItem(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val updatedItem = item.reset()
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        // Optimistically update UI
        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)

        // Persist to repository
        viewModelScope.launch {
            repository.saveChecklist(updatedChecklist)
                .onFailure { error ->
                    // Revert UI on failure
                    loadChecklist()
                }
        }
    }

    fun startTimer(durationMinutes: Int) {
        timerJob?.cancel()  // Cancel any existing timer
        timerJob = viewModelScope.launch {
            startTimerFromSeconds(durationMinutes * 60)
        }
    }

    private suspend fun startTimerFromSeconds(seconds: Int) {
        var remaining = seconds
        while (remaining > 0) {
            _uiState.value = _uiState.value.copy(timerState = TimerState.Running(remaining))
            delay(1000)  // Wait 1 second
            remaining--
        }
        // Timer expired
        soundManager.playCompletionSound()
        _uiState.value = _uiState.value.copy(timerState = TimerState.Expired)
    }

    fun pauseTimer() {
        timerJob?.cancel()
        val currentState = _uiState.value.timerState
        if (currentState is TimerState.Running) {
            _uiState.value = _uiState.value.copy(timerState = TimerState.Paused(currentState.remainingSeconds))
        }
    }

    fun resumeTimer() {
        val currentState = _uiState.value.timerState
        if (currentState is TimerState.Paused) {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                startTimerFromSeconds(currentState.remainingSeconds)
            }
        }
    }

    fun dismissTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timerState = TimerState.NotConfigured)
    }

    private fun stopTimerAsCompleted() {
        timerJob?.cancel()
        val currentState = _uiState.value.timerState
        if (currentState is TimerState.Running) {
            _uiState.value = _uiState.value.copy(timerState = TimerState.Completed(currentState.remainingSeconds))
        }
    }

    private fun handleItemStateChange(updatedChecklist: Checklist) {
        // Optimistically update UI
        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)

        // Play swipe sound
        soundManager.playSwipeSound()

        // Check if all items completed
        val allDone = updatedChecklist.items.all {
            it.state != ChecklistItemState.Pending
        }
        if (allDone) {
            stopTimerAsCompleted()
            viewModelScope.launch {
                delay(150) // Small delay to avoid overlapping sounds
                soundManager.playCompletionSound()
            }
        }

        // Persist to repository
        viewModelScope.launch {
            repository.saveChecklist(updatedChecklist)
                .onFailure { error ->
                    // Revert UI on failure
                    loadChecklist()
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundManager.release()
    }
}
