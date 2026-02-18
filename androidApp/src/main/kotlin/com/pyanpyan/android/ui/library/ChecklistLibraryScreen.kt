package com.pyanpyan.android.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyanpyan.android.R
import com.pyanpyan.domain.model.Checklist
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.repository.ChecklistRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistLibraryScreen(
    onChecklistClick: (ChecklistId) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (ChecklistId) -> Unit,
    onSettingsClick: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: ChecklistLibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ChecklistLibraryViewModel(repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var checklistToDelete by remember { mutableStateOf<Checklist?>(null) }

    // Refresh when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(R.drawable.topbanner),
                        contentDescription = "Pyanpyan",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create checklist")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Active checklists section
            if (uiState.activeChecklists.isNotEmpty()) {
                items(uiState.activeChecklists) { checklist ->
                    ChecklistCard(
                        checklist = checklist,
                        isActive = true,
                        onClick = { onChecklistClick(checklist.id) },
                        onEdit = { onEditClick(checklist.id) },
                        onDelete = { checklistToDelete = checklist }
                    )
                }
            }

            // Divider between active and inactive
            if (uiState.activeChecklists.isNotEmpty() && uiState.inactiveChecklists.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // Inactive checklists section
            if (uiState.inactiveChecklists.isNotEmpty()) {
                items(uiState.inactiveChecklists) { checklist ->
                    ChecklistCard(
                        checklist = checklist,
                        isActive = false,
                        onClick = { onChecklistClick(checklist.id) },
                        onEdit = { onEditClick(checklist.id) },
                        onDelete = { checklistToDelete = checklist }
                    )
                }
            }

            // Empty state
            if (uiState.activeChecklists.isEmpty() && uiState.inactiveChecklists.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No checklists yet.\nTap + to create one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        checklistToDelete?.let { checklist ->
            DeleteConfirmationDialog(
                checklistName = checklist.name,
                onConfirm = {
                    viewModel.deleteChecklist(checklist.id)
                    checklistToDelete = null
                },
                onDismiss = { checklistToDelete = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistCard(
    checklist: Checklist,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                Color(checklist.color.hex.toColorInt())
            } else {
                Color(checklist.color.hex.toColorInt()).copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(checklist.color.hex.toColorInt()))
            )

            // Checklist info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = checklist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isActive) {
                        val pendingCount = checklist.items.count {
                            it.state == com.pyanpyan.domain.model.ChecklistItemState.Pending
                        }
                        if (pendingCount == 0) "All done!" else "$pendingCount pending"
                    } else {
                        getScheduleDescription(checklist)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }

            // Dropdown menu for long-press
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

private fun getScheduleDescription(checklist: Checklist): String {
    val schedule = checklist.schedule

    return buildString {
        if (schedule.daysOfWeek.isNotEmpty()) {
            val days = schedule.daysOfWeek.sortedBy { it.ordinal }
            if (days.size == 7) {
                append("Every day")
            } else {
                append(days.joinToString(", ") {
                    it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
                })
            }
        }

        when (val timeRange = schedule.timeRange) {
            is com.pyanpyan.domain.model.TimeRange.Specific -> {
                if (isNotEmpty()) append(" ")
                append("${timeRange.startTime}-${timeRange.endTime}")
            }
            else -> {}
        }

        if (isEmpty()) {
            append("Inactive")
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    checklistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Checklist?") },
        text = {
            Text("Are you sure you want to delete \"$checklistName\"? This cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
