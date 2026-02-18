package com.pyanpyan.android.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyanpyan.android.sound.SoundManager
import com.pyanpyan.domain.model.CompletionSound
import com.pyanpyan.domain.model.SwipeSound
import com.pyanpyan.domain.repository.ChecklistRepository
import com.pyanpyan.domain.repository.SettingsRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    repository: SettingsRepository,
    checklistRepository: ChecklistRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    repository,
                    checklistRepository,
                    context
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    // State for import confirmation dialog
    var showImportConfirmation by remember { mutableStateOf(false) }
    var pendingImportAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportToFile(it) }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importFromFile(it) { onConfirm ->
                pendingImportAction = onConfirm
                showImportConfirmation = true
            }
        }
    }

    // Create ONE SoundManager that observes the repository's settings flow
    val soundManager = remember(context) {
        SoundManager(
            context = context.applicationContext,
            settingsFlow = repository.settings,  // Use repository flow directly
            scope = scope
        )
    }

    DisposableEffect(Unit) {  // Run once, not on every settings change
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Sounds",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    // Swipe Sound Setting
                    SoundDropdown(
                        label = "Swipe Sound",
                        options = SwipeSound.entries,
                        selectedOption = settings.swipeSound,
                        onOptionSelected = { viewModel.updateSwipeSound(it) },
                        optionLabel = { it.displayName }
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Button(
                        onClick = { soundManager.playSwipeSound() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Swipe Sound")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Completion Sound Setting
                    SoundDropdown(
                        label = "Completion Sound",
                        options = CompletionSound.entries,
                        selectedOption = settings.completionSound,
                        onOptionSelected = { viewModel.updateCompletionSound(it) },
                        optionLabel = { it.displayName }
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Button(
                        onClick = { soundManager.playCompletionSound() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Completion Sound")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Haptic Feedback Setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = settings.enableHapticFeedback,
                            onCheckedChange = { viewModel.updateHapticFeedback(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Typography",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    // Font Family Setting
                    var fontMenuExpanded by remember { mutableStateOf(false) }
                    val availableFonts = listOf(
                        null to "System Default",
                        "sans-serif" to "Sans Serif",
                        "serif" to "Serif",
                        "monospace" to "Monospace",
                        "cursive" to "Cursive",
                        "casual" to "Casual",
                        "sans-serif-condensed" to "Sans Serif Condensed"
                    )

                    ExposedDropdownMenuBox(
                        expanded = fontMenuExpanded,
                        onExpandedChange = { fontMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = availableFonts.find { it.first == settings.fontFamilyName }?.second ?: "System Default",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Font Family") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontMenuExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = fontMenuExpanded,
                            onDismissRequest = { fontMenuExpanded = false }
                        ) {
                            availableFonts.forEach { (fontName, displayName) ->
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        viewModel.updateFontFamily(fontName)
                                        fontMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.padding(8.dp))

                    // Font Size Setting
                    Text(
                        text = "Font Size: ${(settings.fontSizeScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Slider(
                        value = settings.fontSizeScale,
                        onValueChange = { viewModel.updateFontSize(it) },
                        valueRange = 0.7f..1.5f,
                        steps = 15,  // 0.05 increments
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    // Preview
                    Text(
                        text = "Preview:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        text = "The quick brown fox jumps over the lazy dog",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val today = Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
                                exportLauncher.launch("pyanpyan_checklists_$today.json")
                            },
                            modifier = Modifier.weight(0.5f)
                        ) {
                            Text("Export Checklists")
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/json"))
                            },
                            modifier = Modifier.weight(0.5f)
                        ) {
                            Text("Import Checklists")
                        }
                    }

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        text = "Export saves all checklists as JSON. Import replaces all checklists and resets states to Pending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Import confirmation dialog
        if (showImportConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    showImportConfirmation = false
                    pendingImportAction = null
                },
                title = { Text("Replace All Checklists?") },
                text = {
                    Text("This will delete all current checklists and replace them with imported data. All item progress will be reset. Continue?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingImportAction?.invoke()
                            showImportConfirmation = false
                            pendingImportAction = null
                        }
                    ) {
                        Text("Replace All")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmation = false
                            pendingImportAction = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SoundDropdown(
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
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = optionLabel(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
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
