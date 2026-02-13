package com.pyanpyan.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.query.ChecklistActivityState
import com.pyanpyan.domain.query.getActivityState
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

class ChecklistLibraryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistLibraryUiState())
    val uiState: StateFlow<ChecklistLibraryUiState> = _uiState.asStateFlow()

    init {
        loadChecklists()
    }

    private fun loadChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Mock data - will be replaced with repository
            val allChecklists = listOf(
                createMockChecklist(
                    id = "morning",
                    name = "Morning Routine",
                    color = ChecklistColor.SOFT_BLUE,
                    itemCount = 5
                ),
                createMockChecklist(
                    id = "workout",
                    name = "Quick Workout",
                    color = ChecklistColor.CALM_GREEN,
                    itemCount = 3
                ),
                createMockChecklist(
                    id = "evening",
                    name = "Evening Wind-down",
                    color = ChecklistColor.GENTLE_PURPLE,
                    schedule = ChecklistSchedule(
                        daysOfWeek = emptySet(),
                        timeRange = TimeRange.Specific(
                            startTime = kotlinx.datetime.LocalTime(18, 0),
                            endTime = kotlinx.datetime.LocalTime(21, 0)
                        )
                    ),
                    itemCount = 4
                ),
                createMockChecklist(
                    id = "weekend",
                    name = "Weekend Projects",
                    color = ChecklistColor.WARM_PEACH,
                    schedule = ChecklistSchedule(
                        daysOfWeek = setOf(
                            kotlinx.datetime.DayOfWeek.SATURDAY,
                            kotlinx.datetime.DayOfWeek.SUNDAY
                        ),
                        timeRange = TimeRange.AllDay
                    ),
                    itemCount = 2
                )
            )

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
    }

    private fun createMockChecklist(
        id: String,
        name: String,
        color: ChecklistColor,
        schedule: ChecklistSchedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
        itemCount: Int
    ): Checklist {
        val items = (1..itemCount).map { i ->
            ChecklistItem(
                id = ChecklistItemId("$id-item-$i"),
                title = "Task $i",
                state = ChecklistItemState.Pending
            )
        }

        return Checklist(
            id = ChecklistId(id),
            name = name,
            schedule = schedule,
            items = items,
            color = color,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES
        )
    }

    fun deleteChecklist(checklistId: ChecklistId) {
        // TODO: Implement delete with repository
        viewModelScope.launch {
            loadChecklists() // Reload for now
        }
    }
}
