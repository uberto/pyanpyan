package com.pyanpyan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pyanpyan.android.data.RepositoryFactory
import com.pyanpyan.android.repository.DataStoreSettingsRepository
import com.pyanpyan.android.ui.checklist.ChecklistScreen
import com.pyanpyan.android.ui.createedit.CreateEditScreen
import com.pyanpyan.android.ui.library.ChecklistLibraryScreen
import com.pyanpyan.android.ui.settings.SettingsScreen
import com.pyanpyan.android.ui.theme.PyanpyanTheme
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.repository.SettingsRepository
import androidx.compose.material3.Text

sealed class Screen {
    data object Library : Screen()
    data class ChecklistDetail(val checklistId: ChecklistId) : Screen()
    data class CreateEdit(val checklistId: ChecklistId? = null) : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = DataStoreSettingsRepository(applicationContext)
        setContent {
            PyanpyanTheme(
                settingsRepository = settingsRepository
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
                    val repository = remember { RepositoryFactory.getRepository(applicationContext) }

                    when (val screen = currentScreen) {
                        is Screen.Library -> {
                            ChecklistLibraryScreen(
                                onChecklistClick = { checklistId ->
                                    currentScreen = Screen.ChecklistDetail(checklistId)
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
                        is Screen.ChecklistDetail -> {
                            ChecklistScreen(
                                checklistId = screen.checklistId,
                                onBackClick = {
                                    currentScreen = Screen.Library
                                },
                                repository = repository,
                                settingsRepository = settingsRepository
                            )
                        }
                        is Screen.CreateEdit -> {
                            CreateEditScreen(
                                checklistId = screen.checklistId,
                                onSave = {
                                    currentScreen = Screen.Library
                                },
                                onCancel = {
                                    currentScreen = Screen.Library
                                },
                                repository = repository
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                onBackClick = {
                                    currentScreen = Screen.Library
                                },
                                repository = settingsRepository
                            )
                        }
                    }
                }
            }
        }
    }
}
