package com.pyanpyan.android.ui.createedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import java.util.UUID

data class CreateEditUiState(
    val name: String = "",
    val color: ChecklistColor = ChecklistColor.SOFT_BLUE,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val timeRange: TimeRange = TimeRange.AllDay,
    val items: List<String> = listOf(""),
    val statePersistence: StatePersistenceDuration = StatePersistenceDuration.FIFTEEN_MINUTES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() = name.isNotBlank() && items.any { it.isNotBlank() }
}

class CreateEditViewModel(
    private val checklistId: ChecklistId?,
    private val repository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    init {
        if (checklistId != null) {
            loadChecklist(checklistId)
        }
    }

    private fun loadChecklist(id: ChecklistId) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getChecklist(id)
                .onSuccess { checklist ->
                    checklist?.let {
                        _uiState.value = CreateEditUiState(
                            name = it.name,
                            color = it.color,
                            daysOfWeek = it.schedule.daysOfWeek,
                            timeRange = it.schedule.timeRange,
                            items = it.items.map { item -> item.title },
                            statePersistence = it.statePersistence,
                            isLoading = false
                        )
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Checklist not found"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to load checklist"
                    )
                }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateColor(color: ChecklistColor) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    fun updateDays(days: Set<DayOfWeek>) {
        _uiState.value = _uiState.value.copy(daysOfWeek = days)
    }

    fun updateTimeRange(timeRange: TimeRange) {
        _uiState.value = _uiState.value.copy(timeRange = timeRange)
    }

    fun updateStatePersistence(duration: StatePersistenceDuration) {
        _uiState.value = _uiState.value.copy(statePersistence = duration)
    }

    fun addItem() {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items + ""
        )
    }

    fun removeItem(index: Int) {
        if (_uiState.value.items.size > 1) {
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.filterIndexed { i, _ -> i != index }
            )
        }
    }

    fun updateItemText(index: Int, text: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.mapIndexed { i, item ->
                if (i == index) text else item
            }
        )
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            val checklist = Checklist(
                id = checklistId ?: ChecklistId(UUID.randomUUID().toString()),
                name = state.name.trim(),
                schedule = ChecklistSchedule(
                    daysOfWeek = state.daysOfWeek,
                    timeRange = state.timeRange
                ),
                items = state.items
                    .filter { it.isNotBlank() }
                    .mapIndexed { index, title ->
                        ChecklistItem(
                            id = ChecklistItemId(UUID.randomUUID().toString()),
                            title = title.trim(),
                            iconId = null,
                            state = ChecklistItemState.Pending
                        )
                    },
                color = state.color,
                statePersistence = state.statePersistence,
                lastAccessedAt = null
            )

            repository.saveChecklist(checklist)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to save checklist"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
