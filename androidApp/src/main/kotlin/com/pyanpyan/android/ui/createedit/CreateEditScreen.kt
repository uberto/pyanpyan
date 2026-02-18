package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
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
import com.pyanpyan.domain.model.StatePersistenceDuration
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
                    Button(
                        onClick = { viewModel.save(onSuccess = onSave) },
                        enabled = uiState.isValid && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
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

                // Reset Duration Picker
                ResetDurationPicker(
                    selectedDuration = uiState.statePersistence,
                    onDurationSelected = { viewModel.updateStatePersistence(it) }
                )

                // Items Editor
                ItemsEditor(
                    items = uiState.items,
                    onAddItem = { viewModel.addItem() },
                    onRemoveItem = { viewModel.removeItem(it) },
                    onUpdateItemText = { index, text -> viewModel.updateItemText(index, text) },
                    onUpdateItemIcon = { index, iconId -> viewModel.updateItemIcon(index, iconId) }
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
    items: List<ItemData>,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateItemText: (Int, String) -> Unit,
    onUpdateItemIcon: (Int, String?) -> Unit,
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
                IconButton(onClick = onAddItem) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add item",
                        tint = Color(0xFF1B5E20)
                    )
                }
            }

            items.forEachIndexed { index, itemData ->
                ItemRow(
                    item = itemData,
                    index = index,
                    showDelete = items.size > 1,
                    onUpdateText = { onUpdateItemText(index, it) },
                    onUpdateIcon = { onUpdateItemIcon(index, it) },
                    onRemove = { onRemoveItem(index) }
                )
            }
        }
    }
}

@Composable
fun ItemRow(
    item: ItemData,
    index: Int,
    showDelete: Boolean,
    onUpdateText: (String) -> Unit,
    onUpdateIcon: (String?) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showIconPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon button
        IconButton(onClick = { showIconPicker = true }) {
            if (item.iconId != null) {
                Icon(
                    imageVector = getIconForId(item.iconId),
                    contentDescription = "Item icon",
                    tint = Color(0xFF1B5E20)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Add icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = item.title,
            onValueChange = onUpdateText,
            label = { Text("Item ${index + 1}") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        if (showDelete) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            currentIconId = item.iconId,
            onDismiss = { showIconPicker = false },
            onSelectIcon = { iconId ->
                onUpdateIcon(iconId)
                showIconPicker = false
            }
        )
    }
}

@Composable
fun IconPickerDialog(
    currentIconId: String?,
    onDismiss: () -> Unit,
    onSelectIcon: (String?) -> Unit
) {
    val availableIcons = remember {
        listOf(
            "home" to Icons.Filled.Home,
            "phone" to Icons.Filled.Phone,
            "email" to Icons.Filled.Email,
            "favorite" to Icons.Filled.Favorite,
            "star" to Icons.Filled.Star,
            "settings" to Icons.Filled.Settings,
            "account" to Icons.Filled.AccountCircle,
            "calendar" to Icons.Filled.DateRange,
            "notifications" to Icons.Filled.Notifications,
            "location" to Icons.Filled.LocationOn,
            "search" to Icons.Filled.Search,
            "person" to Icons.Filled.Person,
            "info" to Icons.Filled.Info,
            "warning" to Icons.Filled.Warning,
            "lock" to Icons.Filled.Lock,
            "edit" to Icons.Filled.Edit,
            "done" to Icons.Filled.Done,
            "arrow_forward" to Icons.Filled.ArrowForward,
            "arrow_back" to Icons.Filled.ArrowBack,
            "refresh" to Icons.Filled.Refresh
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Remove icon option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { onSelectIcon(null) },
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.small,
                        border = if (currentIconId == null) {
                            BorderStroke(2.dp, Color(0xFF1B5E20))
                        } else null,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "No icon",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Icon grid
                val chunked = availableIcons.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunked.forEach { rowIcons ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowIcons.forEach { (id, icon) ->
                                Surface(
                                    onClick = { onSelectIcon(id) },
                                    modifier = Modifier.size(56.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = if (currentIconId == id) {
                                        BorderStroke(2.dp, Color(0xFF1B5E20))
                                    } else null,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = id,
                                            tint = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

fun getIconForId(iconId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconId) {
        "home" -> Icons.Filled.Home
        "phone" -> Icons.Filled.Phone
        "email" -> Icons.Filled.Email
        "favorite" -> Icons.Filled.Favorite
        "star" -> Icons.Filled.Star
        "settings" -> Icons.Filled.Settings
        "account" -> Icons.Filled.AccountCircle
        "calendar" -> Icons.Filled.DateRange
        "notifications" -> Icons.Filled.Notifications
        "location" -> Icons.Filled.LocationOn
        "search" -> Icons.Filled.Search
        "person" -> Icons.Filled.Person
        "info" -> Icons.Filled.Info
        "warning" -> Icons.Filled.Warning
        "lock" -> Icons.Filled.Lock
        "edit" -> Icons.Filled.Edit
        "done" -> Icons.Filled.Done
        "arrow_forward" -> Icons.Filled.ArrowForward
        "arrow_back" -> Icons.Filled.ArrowBack
        "refresh" -> Icons.Filled.Refresh
        else -> Icons.Filled.Add
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetDurationPicker(
    selectedDuration: StatePersistenceDuration,
    onDurationSelected: (StatePersistenceDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

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
                text = "Auto-Reset",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDuration.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reset checklist after") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    StatePersistenceDuration.entries.forEach { duration ->
                        DropdownMenuItem(
                            text = { Text(duration.displayName) },
                            onClick = {
                                onDurationSelected(duration)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
