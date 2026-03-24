package com.pyanpyan.android.ui.createedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
                    onTimeRangeChange = { viewModel.updateTimeRange(it) },
                    scheduleChime = uiState.scheduleChime,
                    onScheduleChimeChange = { viewModel.updateScheduleChime(it) }
                )

                // Reset Duration Picker
                ResetDurationPicker(
                    selectedDuration = uiState.statePersistence,
                    onDurationSelected = { viewModel.updateStatePersistence(it) }
                )

                // Timer Picker
                TimerPicker(
                    timerEnabled = uiState.timerEnabled,
                    timerDuration = uiState.timerDuration,
                    onTimerEnabledChange = { viewModel.updateTimerEnabled(it) },
                    onTimerDurationChange = { viewModel.updateTimerDuration(it) }
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
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(ChecklistColor.values()) { color ->
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
            // Food & Drink
            "restaurant" to Icons.Filled.Restaurant,
            "local_cafe" to Icons.Filled.LocalCafe,
            "local_bar" to Icons.Filled.LocalBar,
            "lunch_dining" to Icons.Filled.LunchDining,
            "breakfast_dining" to Icons.Filled.BreakfastDining,
            "dinner_dining" to Icons.Filled.DinnerDining,
            "local_pizza" to Icons.Filled.LocalPizza,
            "cake" to Icons.Filled.Cake,
            "water_drop" to Icons.Filled.WaterDrop,

            // Health & Fitness
            "fitness_center" to Icons.Filled.FitnessCenter,
            "directions_run" to Icons.Filled.DirectionsRun,
            "self_improvement" to Icons.Filled.SelfImprovement,
            "spa" to Icons.Filled.Spa,
            "favorite" to Icons.Filled.Favorite,
            "medical_services" to Icons.Filled.MedicalServices,
            "pool" to Icons.Filled.Pool,
            "sports_soccer" to Icons.Filled.SportsSoccer,

            // Home & Living
            "home" to Icons.Filled.Home,
            "bed" to Icons.Filled.Bed,
            "weekend" to Icons.Filled.Weekend,
            "shower" to Icons.Filled.Shower,
            "clean_hands" to Icons.Filled.CleanHands,
            "cleaning_services" to Icons.Filled.CleaningServices,
            "light" to Icons.Filled.Light,
            "kitchen" to Icons.Filled.Kitchen,

            // Work & Study
            "work" to Icons.Filled.Work,
            "school" to Icons.Filled.School,
            "menu_book" to Icons.Filled.MenuBook,
            "computer" to Icons.Filled.Computer,
            "edit" to Icons.Filled.Edit,
            "folder" to Icons.Filled.Folder,
            "assignment" to Icons.Filled.Assignment,
            "laptop" to Icons.Filled.Laptop,

            // Travel & Transport
            "directions_car" to Icons.Filled.DirectionsCar,
            "directions_bus" to Icons.Filled.DirectionsBus,
            "directions_bike" to Icons.Filled.DirectionsBike,
            "flight" to Icons.Filled.Flight,
            "train" to Icons.Filled.Train,
            "local_shipping" to Icons.Filled.LocalShipping,

            // Entertainment
            "movie" to Icons.Filled.Movie,
            "music_note" to Icons.Filled.MusicNote,
            "headphones" to Icons.Filled.Headphones,
            "videogame_asset" to Icons.Filled.VideogameAsset,
            "theaters" to Icons.Filled.Theaters,
            "sports_esports" to Icons.Filled.SportsEsports,

            // Shopping
            "shopping_cart" to Icons.Filled.ShoppingCart,
            "shopping_bag" to Icons.Filled.ShoppingBag,
            "local_grocery_store" to Icons.Filled.LocalGroceryStore,
            "store" to Icons.Filled.Store,
            "receipt" to Icons.Filled.Receipt,

            // Nature & Animals
            "pets" to Icons.Filled.Pets,
            "park" to Icons.Filled.Park,
            "eco" to Icons.Filled.Eco,
            "forest" to Icons.Filled.Forest,
            "yard" to Icons.Filled.Yard,

            // People & Communication
            "person" to Icons.Filled.Person,
            "people" to Icons.Filled.People,
            "family_restroom" to Icons.Filled.FamilyRestroom,
            "child_care" to Icons.Filled.ChildCare,
            "phone" to Icons.Filled.Phone,
            "email" to Icons.Filled.Email,
            "chat" to Icons.Filled.Chat,

            // Time & Planning
            "calendar_today" to Icons.Filled.CalendarToday,
            "schedule" to Icons.Filled.Schedule,
            "alarm" to Icons.Filled.Alarm,
            "timer" to Icons.Filled.Timer,
            "access_time" to Icons.Filled.AccessTime,

            // Tasks & Actions
            "check_circle" to Icons.Filled.CheckCircle,
            "done" to Icons.Filled.Done,
            "task_alt" to Icons.Filled.TaskAlt,
            "star" to Icons.Filled.Star,
            "bookmark" to Icons.Filled.Bookmark,
            "flag" to Icons.Filled.Flag,

            // Other Useful
            "notifications" to Icons.Filled.Notifications,
            "location_on" to Icons.Filled.LocationOn,
            "attach_money" to Icons.Filled.AttachMoney,
            "volunteer_activism" to Icons.Filled.VolunteerActivism,
            "celebration" to Icons.Filled.Celebration,
            "card_giftcard" to Icons.Filled.CardGiftcard
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "${availableIcons.size} icons available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Remove icon option
                Surface(
                    onClick = { onSelectIcon(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    border = if (currentIconId == null) {
                        BorderStroke(2.dp, Color(0xFF1B5E20))
                    } else null,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "No icon",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No Icon")
                    }
                }

                // Scrollable icon grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableIcons) { (id, icon) ->
                        Surface(
                            onClick = { onSelectIcon(id) },
                            modifier = Modifier
                                .size(56.dp)
                                .aspectRatio(1f),
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

                Spacer(modifier = Modifier.height(8.dp))

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
        // Food & Drink
        "restaurant" -> Icons.Filled.Restaurant
        "local_cafe" -> Icons.Filled.LocalCafe
        "local_bar" -> Icons.Filled.LocalBar
        "lunch_dining" -> Icons.Filled.LunchDining
        "breakfast_dining" -> Icons.Filled.BreakfastDining
        "dinner_dining" -> Icons.Filled.DinnerDining
        "local_pizza" -> Icons.Filled.LocalPizza
        "cake" -> Icons.Filled.Cake
        "water_drop" -> Icons.Filled.WaterDrop

        // Health & Fitness
        "fitness_center" -> Icons.Filled.FitnessCenter
        "directions_run" -> Icons.Filled.DirectionsRun
        "self_improvement" -> Icons.Filled.SelfImprovement
        "spa" -> Icons.Filled.Spa
        "favorite" -> Icons.Filled.Favorite
        "medical_services" -> Icons.Filled.MedicalServices
        "pool" -> Icons.Filled.Pool
        "sports_soccer" -> Icons.Filled.SportsSoccer

        // Home & Living
        "home" -> Icons.Filled.Home
        "bed" -> Icons.Filled.Bed
        "weekend" -> Icons.Filled.Weekend
        "shower" -> Icons.Filled.Shower
        "clean_hands" -> Icons.Filled.CleanHands
        "cleaning_services" -> Icons.Filled.CleaningServices
        "light" -> Icons.Filled.Light
        "kitchen" -> Icons.Filled.Kitchen

        // Work & Study
        "work" -> Icons.Filled.Work
        "school" -> Icons.Filled.School
        "menu_book" -> Icons.Filled.MenuBook
        "computer" -> Icons.Filled.Computer
        "edit" -> Icons.Filled.Edit
        "folder" -> Icons.Filled.Folder
        "assignment" -> Icons.Filled.Assignment
        "laptop" -> Icons.Filled.Laptop

        // Travel & Transport
        "directions_car" -> Icons.Filled.DirectionsCar
        "directions_bus" -> Icons.Filled.DirectionsBus
        "directions_bike" -> Icons.Filled.DirectionsBike
        "flight" -> Icons.Filled.Flight
        "train" -> Icons.Filled.Train
        "local_shipping" -> Icons.Filled.LocalShipping

        // Entertainment
        "movie" -> Icons.Filled.Movie
        "music_note" -> Icons.Filled.MusicNote
        "headphones" -> Icons.Filled.Headphones
        "videogame_asset" -> Icons.Filled.VideogameAsset
        "theaters" -> Icons.Filled.Theaters
        "sports_esports" -> Icons.Filled.SportsEsports

        // Shopping
        "shopping_cart" -> Icons.Filled.ShoppingCart
        "shopping_bag" -> Icons.Filled.ShoppingBag
        "local_grocery_store" -> Icons.Filled.LocalGroceryStore
        "store" -> Icons.Filled.Store
        "receipt" -> Icons.Filled.Receipt

        // Nature & Animals
        "pets" -> Icons.Filled.Pets
        "park" -> Icons.Filled.Park
        "eco" -> Icons.Filled.Eco
        "forest" -> Icons.Filled.Forest
        "yard" -> Icons.Filled.Yard

        // People & Communication
        "person" -> Icons.Filled.Person
        "people" -> Icons.Filled.People
        "family_restroom" -> Icons.Filled.FamilyRestroom
        "child_care" -> Icons.Filled.ChildCare
        "phone" -> Icons.Filled.Phone
        "email" -> Icons.Filled.Email
        "chat" -> Icons.Filled.Chat

        // Time & Planning
        "calendar_today" -> Icons.Filled.CalendarToday
        "schedule" -> Icons.Filled.Schedule
        "alarm" -> Icons.Filled.Alarm
        "timer" -> Icons.Filled.Timer
        "access_time" -> Icons.Filled.AccessTime

        // Tasks & Actions
        "check_circle" -> Icons.Filled.CheckCircle
        "done" -> Icons.Filled.Done
        "task_alt" -> Icons.Filled.TaskAlt
        "star" -> Icons.Filled.Star
        "bookmark" -> Icons.Filled.Bookmark
        "flag" -> Icons.Filled.Flag

        // Other Useful
        "notifications" -> Icons.Filled.Notifications
        "location_on" -> Icons.Filled.LocationOn
        "attach_money" -> Icons.Filled.AttachMoney
        "volunteer_activism" -> Icons.Filled.VolunteerActivism
        "celebration" -> Icons.Filled.Celebration
        "card_giftcard" -> Icons.Filled.CardGiftcard

        // Legacy icons for backwards compatibility
        "settings" -> Icons.Filled.Settings
        "account" -> Icons.Filled.AccountCircle
        "calendar" -> Icons.Filled.DateRange
        "location" -> Icons.Filled.LocationOn
        "search" -> Icons.Filled.Search
        "info" -> Icons.Filled.Info
        "warning" -> Icons.Filled.Warning
        "lock" -> Icons.Filled.Lock
        "arrow_forward" -> Icons.Filled.ArrowForward
        "arrow_back" -> Icons.Filled.ArrowBack
        "refresh" -> Icons.Filled.Refresh

        else -> Icons.Filled.Circle
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

@Composable
fun TimerPicker(
    timerEnabled: Boolean,
    timerDuration: Int,
    onTimerEnabledChange: (Boolean) -> Unit,
    onTimerDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Timer",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = timerEnabled,
                    onCheckedChange = onTimerEnabledChange
                )
            }

            if (timerEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration: $timerDuration minutes",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = timerDuration.toFloat(),
                    onValueChange = { onTimerDurationChange(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 58,  // 60 - 1 - 1 (excludes endpoints)
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Timer starts when checklist opens and alerts you if time runs out",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
