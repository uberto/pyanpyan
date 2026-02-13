package com.pyanpyan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pyanpyan.android.ui.checklist.ChecklistScreen
import com.pyanpyan.android.ui.library.ChecklistLibraryScreen
import com.pyanpyan.android.ui.theme.PyanpyanTheme
import com.pyanpyan.domain.model.ChecklistId

sealed class Screen {
    object Library : Screen()
    data class ChecklistDetail(val checklistId: ChecklistId) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PyanpyanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

                    when (val screen = currentScreen) {
                        is Screen.Library -> {
                            ChecklistLibraryScreen(
                                onChecklistClick = { checklistId ->
                                    currentScreen = Screen.ChecklistDetail(checklistId)
                                },
                                onCreateClick = {
                                    // TODO: Navigate to create screen
                                }
                            )
                        }
                        is Screen.ChecklistDetail -> {
                            ChecklistScreen(
                                checklistId = screen.checklistId,
                                onBackClick = {
                                    currentScreen = Screen.Library
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
