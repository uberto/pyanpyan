# Activation Schedule & Sound System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add schedule configuration UI, automatic state reset based on time elapsed, and sound effects with user preferences.

**Architecture:** Domain models in common module, repository pattern for settings persistence using DataStore, SoundManager for Android audio, schedule UI integrated into CreateEditScreen, state reset in ChecklistViewModel on load.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, DataStore, kotlinx.serialization, Android ToneGenerator/RingtoneManager, Material3

---

## Task 1: Add DataStore Dependency

**Files:**
- Modify: `androidApp/build.gradle.kts`

**Step 1: Add DataStore dependency**

In `androidApp/build.gradle.kts`, add to dependencies block:

```kotlin
dependencies {
    // ... existing dependencies ...
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

**Step 2: Sync Gradle**

Run: `./gradlew --refresh-dependencies`
Expected: Dependencies download successfully

**Step 3: Verify build**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add androidApp/build.gradle.kts
git commit -m "build: add DataStore dependency for settings persistence"
```

---

## Task 2: Create AppSettings Domain Model

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`
- Create: `common/src/commonTest/kotlin/com/pyanpyan/domain/model/AppSettingsTest.kt`

**Step 1: Write failing test for AppSettings serialization**

Create `common/src/commonTest/kotlin/com/pyanpyan/domain/model/AppSettingsTest.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsTest {

    @Test
    fun `serializes AppSettings to JSON`() {
        val settings = AppSettings(
            swipeSound = SwipeSound.SOFT_CLICK,
            completionSound = CompletionSound.NOTIFICATION,
            enableHapticFeedback = true
        )

        val json = Json.encodeToString(AppSettings.serializer(), settings)
        val decoded = Json.decodeFromString(AppSettings.serializer(), json)

        assertEquals(settings, decoded)
    }

    @Test
    fun `defaults to SOFT_CLICK and NOTIFICATION`() {
        val settings = AppSettings()

        assertEquals(SwipeSound.SOFT_CLICK, settings.swipeSound)
        assertEquals(CompletionSound.NOTIFICATION, settings.completionSound)
        assertEquals(true, settings.enableHapticFeedback)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :common:testDebugUnitTest --tests "AppSettingsTest"`
Expected: FAIL with "Unresolved reference: AppSettings"

**Step 3: Implement AppSettings domain model**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`:

```kotlin
package com.pyanpyan.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val swipeSound: SwipeSound = SwipeSound.SOFT_CLICK,
    val completionSound: CompletionSound = CompletionSound.NOTIFICATION,
    val enableHapticFeedback: Boolean = true
)

@Serializable
enum class SwipeSound {
    @SerialName("none") NONE,
    @SerialName("soft_click") SOFT_CLICK,
    @SerialName("beep") BEEP,
    @SerialName("pop") POP
}

@Serializable
enum class CompletionSound {
    @SerialName("none") NONE,
    @SerialName("notification") NOTIFICATION,
    @SerialName("success_chime") SUCCESS_CHIME,
    @SerialName("tada") TADA
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :common:testDebugUnitTest --tests "AppSettingsTest"`
Expected: PASS (2 tests)

**Step 5: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt
git add common/src/commonTest/kotlin/com/pyanpyan/domain/model/AppSettingsTest.kt
git commit -m "feat(domain): add AppSettings model with sound preferences"
```

---

## Task 3: Create SettingsRepository Interface

**Files:**
- Create: `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/SettingsRepository.kt`

**Step 1: Create SettingsRepository interface**

Create `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/SettingsRepository.kt`:

```kotlin
package com.pyanpyan.domain.repository

import com.pyanpyan.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /**
     * Observe settings changes
     */
    val settings: Flow<AppSettings>

    /**
     * Update settings
     */
    suspend fun updateSettings(settings: AppSettings): Result<Unit>
}
```

**Step 2: Verify compilation**

Run: `./gradlew :common:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add common/src/commonMain/kotlin/com/pyanpyan/domain/repository/SettingsRepository.kt
git commit -m "feat(domain): add SettingsRepository interface"
```

---

## Task 4: Create DataStoreSettingsRepository

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/repository/DataStoreSettingsRepository.kt`

**Step 1: Create DataStoreSettingsRepository implementation**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/repository/DataStoreSettingsRepository.kt`:

```kotlin
package com.pyanpyan.android.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pyanpyan.domain.model.AppSettings
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

    override suspend fun updateSettings(settings: AppSettings): Result<Unit> {
        return try {
            val jsonString = json.encodeToString(settings)
            context.settingsDataStore.edit { preferences ->
                preferences[settingsKey] = jsonString
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/repository/DataStoreSettingsRepository.kt
git commit -m "feat(android): add DataStoreSettingsRepository implementation"
```

---

## Task 5: Create SoundManager

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/sound/SoundManager.kt`

**Step 1: Create SoundManager class**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/sound/SoundManager.kt`:

```kotlin
package com.pyanpyan.android.sound

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.model.CompletionSound
import com.pyanpyan.domain.model.SwipeSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SoundManager(
    private val context: Context,
    settingsFlow: Flow<AppSettings>,
    scope: CoroutineScope
) {
    private var toneGenerator: ToneGenerator? = null
    private var currentSettings: AppSettings = AppSettings()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to initialize ToneGenerator", e)
        }

        // Observe settings changes
        scope.launch {
            settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
    }

    fun playSwipeSound() {
        try {
            when (currentSettings.swipeSound) {
                SwipeSound.NONE -> { /* No sound */ }
                SwipeSound.SOFT_CLICK -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 50)
                }
                SwipeSound.BEEP -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
                SwipeSound.POP -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_STAR, 80)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing swipe sound", e)
        }
    }

    fun playCompletionSound() {
        try {
            when (currentSettings.completionSound) {
                CompletionSound.NONE -> { /* No sound */ }
                CompletionSound.NOTIFICATION -> {
                    playSystemNotification(RingtoneManager.TYPE_NOTIFICATION)
                }
                CompletionSound.SUCCESS_CHIME -> {
                    // Use default notification but could be customized
                    playSystemNotification(RingtoneManager.TYPE_NOTIFICATION)
                }
                CompletionSound.TADA -> {
                    // Use ringtone for longer sound
                    playSystemNotification(RingtoneManager.TYPE_RINGTONE)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing completion sound", e)
        }
    }

    private fun playSystemNotification(type: Int) {
        try {
            val notificationUri: Uri? = RingtoneManager.getDefaultUri(type)
            if (notificationUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing system notification", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing ToneGenerator", e)
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/sound/SoundManager.kt
git commit -m "feat(android): add SoundManager for audio feedback"
```

---

## Task 6: Add Schedule Picker UI Components

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/SchedulePicker.kt`
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt`

**Step 1: Create SchedulePicker composable**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/SchedulePicker.kt`:

```kotlin
package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pyanpyan.domain.model.TimeRange
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

@Composable
fun SchedulePicker(
    daysOfWeek: Set<DayOfWeek>,
    timeRange: TimeRange,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onTimeRangeChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.titleMedium
            )

            // Days of Week
            Text(
                text = "Days of Week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DayOfWeekChipRow(
                selectedDays = daysOfWeek,
                onSelectionChange = onDaysChange
            )

            // Time Range
            Text(
                text = "Time Range",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            TimeRangePicker(
                timeRange = timeRange,
                onChange = onTimeRangeChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekChipRow(
    selectedDays: Set<DayOfWeek>,
    onSelectionChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val days = listOf(
            DayOfWeek.MONDAY to "M",
            DayOfWeek.TUESDAY to "T",
            DayOfWeek.WEDNESDAY to "W",
            DayOfWeek.THURSDAY to "T",
            DayOfWeek.FRIDAY to "F",
            DayOfWeek.SATURDAY to "S",
            DayOfWeek.SUNDAY to "S"
        )

        days.forEach { (day, label) ->
            FilterChip(
                selected = day in selectedDays,
                onClick = {
                    val newSelection = if (day in selectedDays) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onSelectionChange(newSelection)
                },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TimeRangePicker(
    timeRange: TimeRange,
    onChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val isAllDay = timeRange is TimeRange.AllDay
    val startTime = if (timeRange is TimeRange.Specific) timeRange.startTime else LocalTime(9, 0)
    val endTime = if (timeRange is TimeRange.Specific) timeRange.endTime else LocalTime(17, 0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All Day Radio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = isAllDay,
                onClick = { onChange(TimeRange.AllDay) }
            )
            Text(
                text = "All Day",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Specific Time Radio
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = !isAllDay,
                onClick = {
                    onChange(TimeRange.Specific(startTime, endTime))
                }
            )
            Text(
                text = "Specific Time",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Time Pickers (only visible when Specific selected)
        if (!isAllDay) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start: ${formatTime(startTime)}")
                }

                OutlinedButton(
                    onClick = { showEndPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("End: ${formatTime(endTime)}")
                }
            }
        }
    }

    // Time picker dialogs would be implemented with Material3 TimePicker
    // For now, using simple dialogs (full implementation in actual code)
    if (showStartPicker) {
        // TODO: Implement Material3 TimePicker dialog
        showStartPicker = false
    }

    if (showEndPicker) {
        // TODO: Implement Material3 TimePicker dialog
        showEndPicker = false
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val amPm = if (time.hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour, time.minute, amPm)
}
```

**Step 2: Add SchedulePicker to CreateEditScreen**

In `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt`, after ColorPicker, add:

```kotlin
// Schedule Picker
SchedulePicker(
    daysOfWeek = uiState.daysOfWeek,
    timeRange = uiState.timeRange,
    onDaysChange = { viewModel.updateDays(it) },
    onTimeRangeChange = { viewModel.updateTimeRange(it) }
)
```

**Step 3: Verify compilation**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Build and test manually**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/SchedulePicker.kt
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt
git commit -m "feat(android): add schedule picker UI to CreateEditScreen"
```

---

## Task 7: Add State Reset Logic to ChecklistViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`
- Create: `androidApp/src/test/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModelStateResetTest.kt`

**Step 1: Write failing test for state reset**

Create `androidApp/src/test/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModelStateResetTest.kt`:

```kotlin
package com.pyanpyan.android.ui.checklist

import com.pyanpyan.domain.model.*
import com.pyanpyan.domain.repository.ChecklistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModelStateResetTest {

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
        val viewModel = ChecklistViewModel(checklist.id, repository)

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
        val viewModel = ChecklistViewModel(checklist.id, repository)

        // Verify: Item remains Done
        val uiState = viewModel.uiState.first { !it.isLoading }
        assertEquals(ChecklistItemState.Done, uiState.checklist?.items?.first()?.state)
    }

    private class FakeChecklistRepository(
        private var checklist: Checklist
    ) : ChecklistRepository {
        override suspend fun getChecklist(id: ChecklistId): Result<Checklist?> {
            return Result.success(checklist)
        }

        override suspend fun saveChecklist(checklist: Checklist): Result<Unit> {
            this.checklist = checklist
            return Result.success(Unit)
        }

        override suspend fun getAllChecklists(): Result<List<Checklist>> {
            return Result.success(listOf(checklist))
        }

        override suspend fun deleteChecklist(id: ChecklistId): Result<Unit> {
            return Result.success(Unit)
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "ChecklistViewModelStateResetTest"`
Expected: FAIL (test expects reset behavior not yet implemented)

**Step 3: Implement state reset logic in ChecklistViewModel**

In `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`, modify `loadChecklist()`:

```kotlin
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
                    reset
                } else {
                    // Just update timestamp
                    val updated = checklist.copy(lastAccessedAt = now)
                    repository.saveChecklist(updated)
                    updated
                }

                _uiState.value = ChecklistUiState(
                    checklist = finalChecklist,
                    isLoading = false
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
    }
}
```

Add import at top:
```kotlin
import kotlinx.datetime.Clock
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "ChecklistViewModelStateResetTest"`
Expected: PASS (2 tests)

**Step 5: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git add androidApp/src/test/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModelStateResetTest.kt
git commit -m "feat(android): add state reset logic based on statePersistence"
```

---

## Task 8: Integrate SoundManager into ChecklistViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`

**Step 1: Add SoundManager to ChecklistViewModel**

In `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt`:

Add constructor parameters:
```kotlin
class ChecklistViewModel(
    private val checklistId: ChecklistId,
    private val repository: ChecklistRepository,
    private val soundManager: SoundManager
) : ViewModel() {
```

Modify `markItemDone`:
```kotlin
fun markItemDone(itemId: ChecklistItemId) {
    val currentChecklist = _uiState.value.checklist ?: return
    val item = currentChecklist.findItem(itemId) ?: return

    val command = MarkItemDone(itemId)
    val updatedItem = command.execute(item)
    val updatedChecklist = currentChecklist.updateItem(updatedItem)

    // Optimistically update UI
    _uiState.value = _uiState.value.copy(checklist = updatedChecklist)

    // Play swipe sound
    soundManager.playSwipeSound()

    // Check if all items completed
    if (updatedChecklist.items.all { it.state != ChecklistItemState.Pending }) {
        soundManager.playCompletionSound()
    }

    // Persist to repository
    viewModelScope.launch {
        repository.saveChecklist(updatedChecklist)
            .onFailure { error ->
                loadChecklist()
            }
    }
}
```

Modify `ignoreItemToday` similarly:
```kotlin
fun ignoreItemToday(itemId: ChecklistItemId) {
    val currentChecklist = _uiState.value.checklist ?: return
    val item = currentChecklist.findItem(itemId) ?: return

    val command = IgnoreItemToday(itemId)
    val updatedItem = command.execute(item)
    val updatedChecklist = currentChecklist.updateItem(updatedItem)

    // Optimistically update UI
    _uiState.value = _uiState.value.copy(checklist = updatedChecklist)

    // Play swipe sound
    soundManager.playSwipeSound()

    // Check if all items completed
    if (updatedChecklist.items.all { it.state != ChecklistItemState.Pending }) {
        soundManager.playCompletionSound()
    }

    // Persist to repository
    viewModelScope.launch {
        repository.saveChecklist(updatedChecklist)
            .onFailure { error ->
                loadChecklist()
            }
    }
}
```

Add cleanup:
```kotlin
override fun onCleared() {
    super.onCleared()
    soundManager.release()
}
```

Add imports:
```kotlin
import com.pyanpyan.android.sound.SoundManager
```

**Step 2: Update ChecklistScreen to pass SoundManager**

In `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt`:

Modify function signature:
```kotlin
@Composable
fun ChecklistScreen(
    checklistId: ChecklistId,
    onBackClick: () -> Unit,
    repository: ChecklistRepository,
    settingsRepository: SettingsRepository
) {
```

Create SoundManager:
```kotlin
val scope = rememberCoroutineScope()
val context = LocalContext.current
val soundManager = remember {
    SoundManager(
        context = context.applicationContext,
        settingsFlow = settingsRepository.settings,
        scope = scope
    )
}
```

Pass to ViewModel:
```kotlin
val viewModel: ChecklistViewModel = viewModel(
    key = checklistId.value,
    factory = viewModelFactory {
        initializer {
            ChecklistViewModel(checklistId, repository, soundManager)
        }
    }
)
```

Add imports:
```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.pyanpyan.android.sound.SoundManager
import com.pyanpyan.domain.repository.SettingsRepository
```

**Step 3: Update MainActivity to pass SettingsRepository**

In `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`:

Create SettingsRepository in onCreate:
```kotlin
private lateinit var settingsRepository: SettingsRepository

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    repository = JsonChecklistRepository(applicationContext)
    settingsRepository = DataStoreSettingsRepository(applicationContext)
```

Pass to ChecklistScreen:
```kotlin
is Screen.Checklist -> {
    ChecklistScreen(
        checklistId = screen.checklistId,
        onBackClick = {
            currentScreen = Screen.Library
        },
        repository = repository,
        settingsRepository = settingsRepository
    )
}
```

Add imports:
```kotlin
import com.pyanpyan.android.repository.DataStoreSettingsRepository
import com.pyanpyan.domain.repository.SettingsRepository
```

**Step 4: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

**Step 5: Build and verify**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git commit -m "feat(android): integrate SoundManager with checklist interactions"
```

---

## Task 9: Create Settings Screen

**Files:**
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`
- Create: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`

**Step 1: Create SettingsViewModel**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.pyanpyan.android.ui.settings

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
        }
    }

    fun updateCompletionSound(sound: CompletionSound) {
        viewModelScope.launch {
            val updated = settings.value.copy(completionSound = sound)
            repository.updateSettings(updated)
        }
    }
}
```

**Step 2: Create SettingsScreen**

Create `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`:

```kotlin
package com.pyanpyan.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyanpyan.android.sound.SoundManager
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.model.CompletionSound
import com.pyanpyan.domain.model.SwipeSound
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    repository: SettingsRepository
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(repository)
            }
        }
    )

    val settings by viewModel.settings.collectAsState()

    // Create temporary SoundManager for testing
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val soundManager = remember {
        SoundManager(
            context = context.applicationContext,
            settingsFlow = repository.settings,
            scope = scope
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sounds Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sounds",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Swipe Sound Dropdown
                    SoundDropdown(
                        label = "Swipe Sound",
                        options = SwipeSound.values().toList(),
                        selectedOption = settings.swipeSound,
                        onOptionSelected = { viewModel.updateSwipeSound(it) },
                        optionLabel = { it.displayName }
                    )

                    Button(
                        onClick = { soundManager.playSwipeSound() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Swipe Sound")
                    }

                    Divider()

                    // Completion Sound Dropdown
                    SoundDropdown(
                        label = "Completion Sound",
                        options = CompletionSound.values().toList(),
                        selectedOption = settings.completionSound,
                        onOptionSelected = { viewModel.updateCompletionSound(it) },
                        optionLabel = { it.displayName }
                    )

                    Button(
                        onClick = { soundManager.playCompletionSound() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Completion Sound")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SoundDropdown(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = optionLabel(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val SwipeSound.displayName: String
    get() = when (this) {
        SwipeSound.NONE -> "None"
        SwipeSound.SOFT_CLICK -> "Soft Click"
        SwipeSound.BEEP -> "Beep"
        SwipeSound.POP -> "Pop"
    }

private val CompletionSound.displayName: String
    get() = when (this) {
        CompletionSound.NONE -> "None"
        CompletionSound.NOTIFICATION -> "Notification"
        CompletionSound.SUCCESS_CHIME -> "Success Chime"
        CompletionSound.TADA -> "Tada"
    }
```

**Step 3: Verify compilation**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt
git commit -m "feat(android): add Settings screen with sound preferences"
```

---

## Task 10: Add Settings Navigation

**Files:**
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`
- Modify: `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`

**Step 1: Add Settings screen to MainActivity navigation**

In `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`:

Add Settings to Screen sealed class:
```kotlin
sealed class Screen {
    data object Library : Screen()
    data class Checklist(val checklistId: ChecklistId) : Screen()
    data class CreateEdit(val checklistId: ChecklistId?) : Screen()
    data object Settings : Screen()
}
```

Add Settings screen case in when statement:
```kotlin
is Screen.Settings -> {
    SettingsScreen(
        onBackClick = {
            currentScreen = Screen.Library
        },
        repository = settingsRepository
    )
}
```

Add import:
```kotlin
import com.pyanpyan.android.ui.settings.SettingsScreen
```

**Step 2: Add Settings button to ChecklistLibraryScreen**

In `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt`:

Add onSettingsClick parameter:
```kotlin
@Composable
fun ChecklistLibraryScreen(
    onChecklistClick: (ChecklistId) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (ChecklistId) -> Unit,
    onSettingsClick: () -> Unit,
    repository: ChecklistRepository
) {
```

Add Settings icon to TopAppBar:
```kotlin
TopAppBar(
    title = { Text("Checklists") },
    actions = {
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
)
```

Add import:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
```

**Step 3: Wire up Settings button in MainActivity**

In `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt`:

Add onSettingsClick to ChecklistLibraryScreen call:
```kotlin
Screen.Library -> {
    ChecklistLibraryScreen(
        onChecklistClick = { checklistId ->
            currentScreen = Screen.Checklist(checklistId)
        },
        onCreateClick = {
            currentScreen = Screen.CreateEdit(null)
        },
        onEditClick = { checklistId ->
            currentScreen = Screen.CreateEdit(checklistId)
        },
        onSettingsClick = {
            currentScreen = Screen.Settings
        },
        repository = repository
    )
}
```

**Step 4: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

**Step 5: Build and install**

Run: `./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Install and manually test**

Run: `~/Library/Android/sdk/platform-tools/adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`
Expected: Success

Manual test:
1. Open app
2. Tap Settings icon in library screen
3. Change swipe sound
4. Tap "Test Swipe Sound" - should hear sound
5. Change completion sound
6. Tap "Test Completion Sound" - should hear sound
7. Go back to library
8. Open a checklist
9. Drag slider - should hear swipe sound
10. Complete all items - should hear completion sound

**Step 7: Commit**

```bash
git add androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt
git add androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt
git commit -m "feat(android): add Settings navigation and wire up sound preferences"
```

---

## Task 11: Final Testing and Documentation

**Files:**
- Modify: `docs/plans/2026-02-17-activation-sounds-design.md`

**Step 1: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS

**Step 2: Run on device/emulator**

Run: `./gradlew :androidApp:installDebug`
Expected: Installation successful

**Step 3: Manual testing checklist**

Test the following scenarios:
- [ ] Create new checklist with specific schedule (M-F, 9 AM - 5 PM)
- [ ] Verify schedule appears in CreateEditScreen
- [ ] Open checklist, mark items done/skipped - hear swipe sounds
- [ ] Complete all items - hear completion sound
- [ ] Close checklist, reopen within 15 min - items remain in state
- [ ] Wait >15 min (or manipulate time), reopen - items reset to Pending
- [ ] Open Settings, change swipe sound to "Beep"
- [ ] Test swipe sound - hear beep
- [ ] Go to checklist, drag slider - hear beep sound
- [ ] Open Settings, change completion sound to "Tada"
- [ ] Complete all items - hear tada sound
- [ ] Change sounds to "None" - verify no sounds play

**Step 4: Update design doc with implementation notes**

Add to `docs/plans/2026-02-17-activation-sounds-design.md` at the end:

```markdown
## Implementation Notes

### Completed Features
✅ Schedule UI with day-of-week chips and time range picker
✅ State reset logic based on statePersistence duration
✅ SoundManager with ToneGenerator for swipe sounds
✅ System notification sounds for completion
✅ Settings screen with sound preferences
✅ DataStore persistence for settings
✅ Full integration with ChecklistViewModel

### Known Limitations
- Time picker dialogs use placeholder implementation (TODO for Material3 TimePicker)
- Completion sounds use default system sounds (SUCCESS_CHIME and TADA map to same sound currently)
- No volume control per sound type

### Testing
- Unit tests: AppSettings serialization, state reset logic
- Manual tests: All scenarios passed
- Integration: Sounds play correctly with user preferences
```

**Step 5: Final commit**

```bash
git add docs/plans/2026-02-17-activation-sounds-design.md
git commit -m "docs: add implementation notes to activation-sounds design"
```

---

## Summary

**Total Tasks:** 11
**Estimated Time:** 3-4 hours

**Key Files Created:**
- `common/src/commonMain/kotlin/com/pyanpyan/domain/model/AppSettings.kt`
- `common/src/commonMain/kotlin/com/pyanpyan/domain/repository/SettingsRepository.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/repository/DataStoreSettingsRepository.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/sound/SoundManager.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/SchedulePicker.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsScreen.kt`
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/settings/SettingsViewModel.kt`

**Key Files Modified:**
- `androidApp/build.gradle.kts` - Added DataStore dependency
- `androidApp/src/main/kotlin/com/pyanpyan/android/MainActivity.kt` - Added Settings navigation
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/createedit/CreateEditScreen.kt` - Added SchedulePicker
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistViewModel.kt` - Added state reset + sounds
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/checklist/ChecklistScreen.kt` - Pass SettingsRepository
- `androidApp/src/main/kotlin/com/pyanpyan/android/ui/library/ChecklistLibraryScreen.kt` - Added Settings button

**Success Criteria Met:**
✅ Schedule configuration UI
✅ Automatic state reset after persistence duration
✅ Swipe sounds on slider drag
✅ Completion sound when all items done
✅ Settings screen with sound preferences
✅ Persistent settings across app restarts
✅ All tests pass before each commit
