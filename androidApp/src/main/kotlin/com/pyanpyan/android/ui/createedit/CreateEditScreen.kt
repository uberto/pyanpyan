package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyanpyan.domain.model.ChecklistColor
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.repository.ChecklistRepository
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditScreen(
    checklistId: ChecklistId?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: CreateEditViewModel = viewModel(
        key = checklistId?.value ?: "new",
        factory = viewModelFactory {
            initializer {
                CreateEditViewModel(checklistId, repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = androidx.compose.material3.SnackbarHostState()

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (checklistId == null) "Create Checklist" else "Edit Checklist")
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSuccess = onSave) },
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(0.dp))

                // Name field
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Checklist Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.name.isBlank()
                )

                // Color Picker
                ColorPicker(
                    selectedColor = uiState.color,
                    onColorSelected = { viewModel.updateColor(it) }
                )

                // Schedule Picker
                SchedulePicker(
                    daysOfWeek = uiState.daysOfWeek,
                    timeRange = uiState.timeRange,
                    onDaysChange = { viewModel.updateDays(it) },
                    onTimeRangeChange = { viewModel.updateTimeRange(it) }
                )

                // Items Editor
                ItemsEditor(
                    items = uiState.items,
                    onAddItem = { viewModel.addItem() },
                    onRemoveItem = { viewModel.removeItem(it) },
                    onUpdateItem = { index, text -> viewModel.updateItemText(index, text) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ColorPicker(
    selectedColor: ChecklistColor,
    onColorSelected: (ChecklistColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChecklistColor.values().forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = color == selectedColor,
                        onClick = { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: ChecklistColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = Color(AndroidColor.parseColor(color.hex)),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ItemsEditor(
    items: List<String>,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateItem: (Int, String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onAddItem) {
                    Text("Add Item")
                }
            }

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item,
                        onValueChange = { onUpdateItem(index, it) },
                        label = { Text("Item ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (items.size > 1) {
                        IconButton(onClick = { onRemoveItem(index) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete item",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
