package com.pyanpyan.android.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.command.IgnoreItemToday
import com.pyanpyan.domain.command.MarkItemDone
import com.pyanpyan.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val checklist: Checklist? = null,
    val isLoading: Boolean = false
)

class ChecklistViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    init {
        loadChecklist()
    }

    private fun loadChecklist() {
        viewModelScope.launch {
            // Temporary mock data - will be replaced with repository
            val mockChecklist = Checklist(
                id = ChecklistId("morning-routine"),
                name = "Morning Routine",
                schedule = ChecklistSchedule(
                    daysOfWeek = emptySet(),
                    timeRange = TimeRange.AllDay
                ),
                items = listOf(
                    ChecklistItem(
                        id = ChecklistItemId("brush-teeth"),
                        title = "Brush teeth",
                        state = ChecklistItemState.Pending
                    ),
                    ChecklistItem(
                        id = ChecklistItemId("get-dressed"),
                        title = "Get dressed",
                        state = ChecklistItemState.Pending
                    )
                ),
                color = ChecklistColor.SOFT_BLUE,
                statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
                lastAccessedAt = null
            )

            _uiState.value = ChecklistUiState(checklist = mockChecklist)
        }
    }

    fun markItemDone(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = MarkItemDone(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)
    }

    fun ignoreItemToday(itemId: ChecklistItemId) {
        val currentChecklist = _uiState.value.checklist ?: return
        val item = currentChecklist.findItem(itemId) ?: return

        val command = IgnoreItemToday(itemId)
        val updatedItem = command.execute(item)
        val updatedChecklist = currentChecklist.updateItem(updatedItem)

        _uiState.value = _uiState.value.copy(checklist = updatedChecklist)
    }
}
