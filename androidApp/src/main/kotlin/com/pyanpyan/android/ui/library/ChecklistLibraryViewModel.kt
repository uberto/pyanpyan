package com.pyanpyan.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.query.ChecklistActivityState
import com.pyanpyan.domain.query.getActivityState
import com.pyanpyan.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ChecklistLibraryUiState(
    val activeChecklists: List<Checklist> = emptyList(),
    val inactiveChecklists: List<Checklist> = emptyList(),
    val isLoading: Boolean = false
)

class ChecklistLibraryViewModel(
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistLibraryUiState())
    val uiState: StateFlow<ChecklistLibraryUiState> = _uiState.asStateFlow()

    init {
        loadChecklists()
    }

    fun refresh() {
        loadChecklists()
    }

    private fun loadChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getAllChecklists()
                .onSuccess { allChecklists ->
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val (active, inactive) = allChecklists.partition { checklist ->
                        checklist.getActivityState(now) is ChecklistActivityState.Active
                    }

                    _uiState.value = ChecklistLibraryUiState(
                        activeChecklists = active.sortedBy { it.name },
                        inactiveChecklists = inactive.sortedBy { it.name },
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Error will be shown via SnackBar in UI
                }
        }
    }

    fun deleteChecklist(checklistId: ChecklistId) {
        viewModelScope.launch {
            repository.deleteChecklist(checklistId)
                .onSuccess { loadChecklists() }
                .onFailure { error ->
                    // Error will be shown via SnackBar in UI
                }
        }
    }
}
