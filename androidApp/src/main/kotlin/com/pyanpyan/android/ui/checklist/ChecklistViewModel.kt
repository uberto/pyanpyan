package com.pyanpyan.android.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.command.IgnoreItemToday
import com.pyanpyan.domain.command.MarkItemDone
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = false
)

class ChecklistViewModel(
    private val checklistId: ChecklistId,
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

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

    fun ignoreItemToday(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = IgnoreItemToday(itemId)
        val updatedItem = command.execute(item)
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
}
