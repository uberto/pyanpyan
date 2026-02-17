package com.pyanpyan.android.ui.checklist

import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.RepositoryResult
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModelStateResetTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resets items when statePersistence duration exceeded`() = runTest {
        // Setup: Checklist with lastAccessedAt = 20 minutes ago, persistence = 15 minutes
        val now = Clock.System.now()
        val twentyMinutesAgo = now - 20.minutes

        val checklist = Checklist(
            id = ChecklistId("test"),
            name = "Test",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("1"),
                    title = "Item 1",
                    iconId = null,
                    state = ChecklistItemState.Done
                )
            ),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = twentyMinutesAgo
        )

        val repository = FakeChecklistRepository(checklist)
        val settingsRepository = FakeSettingsRepository()
        val viewModel = ChecklistViewModel(checklist.id, repository, null, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify: Item was reset to Pending
        val uiState = viewModel.uiState.first { !it.isLoading }
        assertEquals(ChecklistItemState.Pending, uiState.checklist?.items?.first()?.state)
    }

    @Test
    fun `does not reset items when within statePersistence duration`() = runTest {
        // Setup: Checklist with lastAccessedAt = 10 minutes ago, persistence = 15 minutes
        val now = Clock.System.now()
        val tenMinutesAgo = now - 10.minutes

        val checklist = Checklist(
            id = ChecklistId("test"),
            name = "Test",
            schedule = ChecklistSchedule(emptySet(), TimeRange.AllDay),
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("1"),
                    title = "Item 1",
                    iconId = null,
                    state = ChecklistItemState.Done
                )
            ),
            color = ChecklistColor.SOFT_BLUE,
            statePersistence = StatePersistenceDuration.FIFTEEN_MINUTES,
            lastAccessedAt = tenMinutesAgo
        )

        val repository = FakeChecklistRepository(checklist)
        val settingsRepository = FakeSettingsRepository()
        val viewModel = ChecklistViewModel(checklist.id, repository, null, settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify: Item remains Done
        val uiState = viewModel.uiState.first { !it.isLoading }
        assertEquals(ChecklistItemState.Done, uiState.checklist?.items?.first()?.state)
    }

    private class FakeChecklistRepository(
        private var checklist: Checklist
    ) : ChecklistRepository {
        override suspend fun getChecklist(id: ChecklistId): RepositoryResult<Checklist?> {
            return RepositoryResult.success(checklist)
        }

        override suspend fun saveChecklist(checklist: Checklist): RepositoryResult<Unit> {
            this.checklist = checklist
            return RepositoryResult.success(Unit)
        }

        override suspend fun getAllChecklists(): RepositoryResult<List<Checklist>> {
            return RepositoryResult.success(listOf(checklist))
        }

        override suspend fun deleteChecklist(id: ChecklistId): RepositoryResult<Unit> {
            return RepositoryResult.success(Unit)
        }

        override suspend fun exportToJson(): RepositoryResult<String> {
            return RepositoryResult.success("{}")
        }

        override suspend fun importFromJson(json: String): RepositoryResult<Unit> {
            return RepositoryResult.success(Unit)
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings: Flow<AppSettings> = flowOf(AppSettings())

        override suspend fun updateSettings(settings: AppSettings): RepositoryResult<Unit> {
            return RepositoryResult.success(Unit)
        }
    }
}
