package com.pyanpyan.android.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pyanpyan.android.ui.components.ItemSlider
import com.pyanpyan.android.ui.components.SliderState
import com.pyanpyan.domain.model.ChecklistId
import com.pyanpyan.domain.model.ChecklistItem
import com.pyanpyan.domain.model.ChecklistItemState
import com.pyanpyan.domain.repository.ChecklistRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    checklistId: ChecklistId,
    onBackClick: () -> Unit,
    repository: ChecklistRepository
) {
    val viewModel: ChecklistViewModel = viewModel(
        key = checklistId.value,
        factory = viewModelFactory {
            initializer {
                ChecklistViewModel(checklistId, repository)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar with back button
            TopAppBar(
                title = { Text("Checklist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

            // Existing content wrapped in Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                uiState.checklist?.let { checklist ->
                    Text(
                        text = checklist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(checklist.items) { item ->
                            ChecklistItemRow(
                                item = item,
                                onMarkDone = { viewModel.markItemDone(item.id) },
                                onIgnoreToday = { viewModel.ignoreItemToday(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onMarkDone: () -> Unit,
    onIgnoreToday: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.state) {
                ChecklistItemState.Done -> MaterialTheme.colorScheme.primaryContainer
                ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ChecklistItemState.Pending -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Item title with optional icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Placeholder for icon (will be implemented later)
                item.iconId?.let {
                    Text(
                        text = "🔹",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = when (item.state) {
                        ChecklistItemState.Done -> TextDecoration.LineThrough
                        else -> null
                    },
                    color = when (item.state) {
                        ChecklistItemState.IgnoredToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Slider or status text
            when (item.state) {
                ChecklistItemState.Pending -> {
                    ItemSlider(
                        state = SliderState.Center,
                        onSkip = onIgnoreToday,
                        onDone = onMarkDone,
                        enabled = true
                    )
                }
                ChecklistItemState.Done -> {
                    Column {
                        ItemSlider(
                            state = SliderState.Right,
                            onSkip = {},
                            onDone = {},
                            enabled = false
                        )
                        Text(
                            text = "✓ Completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                ChecklistItemState.IgnoredToday -> {
                    Column {
                        ItemSlider(
                            state = SliderState.Left,
                            onSkip = {},
                            onDone = {},
                            enabled = false
                        )
                        Text(
                            text = "⊘ Skipped today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
