package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.data.ScheduleWithApps
import com.example.scheduling.ScheduleCalculator
import com.example.ui.SafeGateViewModel
import com.example.ui.SelectedAppState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavHostController,
    viewModel: SafeGateViewModel,
    selectedAppState: SelectedAppState
) {
    val context = LocalContext.current
    val schedulesWithApps by viewModel.schedulesWithApps.collectAsState()

    // Apps selected for this schedule (starts empty!)
    val selectedScheduleApps by selectedAppState.scheduleApps.collectAsState()

    // Start & End Date/Time Pickers (default start = now, end = 24h later)
    val startCalendar = remember {
        Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val endCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var startDateStr by remember { mutableStateOf(formatDate(startCalendar)) }
    var startTimeStr by remember { mutableStateOf(formatTime(startCalendar)) }

    var endDateStr by remember { mutableStateOf(formatDate(endCalendar)) }
    var endTimeStr by remember { mutableStateOf(formatTime(endCalendar)) }

    var repeatType by remember { mutableStateOf("Once") }
    val customDaysList = remember { mutableStateListOf("Mon", "Tue", "Wed", "Thu", "Fri") }

    var inactivityEnabled by remember { mutableStateOf(false) }

    var editingSchedule by remember { mutableStateOf<ScheduleWithApps?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun openStartDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                startDateStr = formatDate(startCalendar)
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openStartTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                startCalendar.set(Calendar.MINUTE, minute)
                startTimeStr = formatTime(startCalendar)
            },
            startCalendar.get(Calendar.HOUR_OF_DAY),
            startCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    fun openEndDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endCalendar.set(Calendar.YEAR, year)
                endCalendar.set(Calendar.MONTH, month)
                endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                endDateStr = formatDate(endCalendar)
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                endCalendar.set(Calendar.MINUTE, minute)
                endTimeStr = formatTime(endCalendar)
            },
            endCalendar.get(Calendar.HOUR_OF_DAY),
            endCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CREATE SCHEDULE",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("schedule_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            SafeGateBottomBar(
                currentRoute = "schedule",
                navController = navController
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
        ) {
            // 1. SELECT APPS SECTION
            item {
                Text(
                    text = if (selectedScheduleApps.isEmpty()) "Select Apps" else "Selected Apps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (selectedScheduleApps.isEmpty()) {
                            // Initial state: No apps selected
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No apps selected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Choose one, two, or multiple apps to lock",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = { navController.navigate("select_apps_for_schedule") },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("select_apps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Select Apps",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Selected Apps state: e.g. "3 apps selected"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${selectedScheduleApps.size} apps selected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val names = selectedScheduleApps.joinToString(", ") { it.appName }
                                    Text(
                                        text = names,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        maxLines = 2
                                    )
                                }

                                OutlinedButton(
                                    onClick = { navController.navigate("select_apps_for_schedule") },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("change_selected_apps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. START SECTION
            item {
                Text(
                    text = "Start",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScheduleTimePill(
                        label = "Date",
                        value = startDateStr,
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f),
                        onClick = { openStartDatePicker() }
                    )
                    ScheduleTimePill(
                        label = "Time",
                        value = startTimeStr,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                        onClick = { openStartTimePicker() }
                    )
                }
            }

            // 3. END SECTION
            item {
                Text(
                    text = "End",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScheduleTimePill(
                        label = "Date",
                        value = endDateStr,
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f),
                        onClick = { openEndDatePicker() }
                    )
                    ScheduleTimePill(
                        label = "Time",
                        value = endTimeStr,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                        onClick = { openEndTimePicker() }
                    )
                }
            }

            // 4. REPEAT SECTION
            item {
                Text(
                    text = "Repeat",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            item {
                RepeatSelectorCard(
                    selectedRepeat = repeatType,
                    onSelected = { repeatType = it }
                )
            }

            if (repeatType == "Custom") {
                item {
                    CustomDaysPillGroup(selectedDays = customDaysList)
                }
            }

            // 5. INACTIVITY LOCK SECTION
            item {
                Text(
                    text = "Inactivity Lock",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Inactivity Lock",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Strict lock mode: apps stay locked until scheduled End date/time. No PIN bypass, immutable while active.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = inactivityEnabled,
                                onCheckedChange = { inactivityEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .scale(0.85f)
                                    .testTag("inactivity_switch")
                            )
                        }
                    }
                }
            }

            // Validation Error Alert
            if (validationError != null) {
                item {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 6. [ SAVE ] Button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val startMillis = startCalendar.timeInMillis
                        val endMillis = endCalendar.timeInMillis

                        if (selectedScheduleApps.isEmpty()) {
                            validationError = "Please select at least one app to lock."
                            return@Button
                        }

                        if (repeatType == "Once" && endMillis <= startMillis) {
                            validationError = "End date/time must be after start date/time."
                            return@Button
                        }

                        validationError = null

                        val chosenApps = selectedScheduleApps.map { Pair(it.packageName, it.appName) }
                        val customDays = if (repeatType == "Custom") customDaysList.joinToString(",") else ""

                        viewModel.createMultiAppSchedule(
                            selectedApps = chosenApps,
                            startDateTime = startMillis,
                            endDateTime = endMillis,
                            repeatType = repeatType,
                            customDays = customDays,
                            enabled = true,
                            inactivityEnabled = inactivityEnabled,
                            inactivityMinutes = 0
                        )

                        // Clear schedule selection state for next time
                        selectedAppState.clearScheduleApps()

                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_schedule_button"),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = selectedScheduleApps.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // 6. SCHEDULED LOCKS List
            if (schedulesWithApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "SCHEDULED LOCKS (${schedulesWithApps.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                items(schedulesWithApps, key = { it.schedule.id }) { itemWithApps ->
                    val now = System.currentTimeMillis()
                    val isActive = itemWithApps.schedule.enabled && ScheduleCalculator.isScheduleActiveNow(itemWithApps.schedule, now)

                    MultiAppScheduleCard(
                        item = itemWithApps,
                        isActiveNow = isActive,
                        onToggle = { isEnabled ->
                            if (!ScheduleCalculator.isStrictInactivityActive(itemWithApps.schedule)) {
                                viewModel.toggleSchedule(itemWithApps.schedule.id, isEnabled)
                            }
                        },
                        onEdit = {
                            if (!ScheduleCalculator.isStrictInactivityActive(itemWithApps.schedule)) {
                                editingSchedule = itemWithApps
                            }
                        },
                        onDelete = {
                            if (!ScheduleCalculator.isStrictInactivityActive(itemWithApps.schedule)) {
                                viewModel.deleteScheduleById(itemWithApps.schedule.id)
                            }
                        }
                    )
                }
            }
        }

        editingSchedule?.let { scheduleToEdit ->
            EditScheduleDialog(
                itemWithApps = scheduleToEdit,
                onDismiss = { editingSchedule = null },
                onSave = { startMillis, endMillis, repeatType, customDays, enabled, inactEnabled ->
                    val appPairs = scheduleToEdit.apps.map { Pair(it.packageName, it.appName) }
                    viewModel.updateMultiAppSchedule(
                        scheduleId = scheduleToEdit.schedule.id,
                        selectedApps = appPairs,
                        startDateTime = startMillis,
                        endDateTime = endMillis,
                        repeatType = repeatType,
                        customDays = customDays,
                        enabled = enabled,
                        inactivityEnabled = inactEnabled,
                        inactivityMinutes = 0
                    )
                    editingSchedule = null
                }
            )
        }
    }
}

@Composable
private fun ScheduleTimePill(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RepeatSelectorCard(
    selectedRepeat: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("Once", "Daily", "Weekly", "Custom")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { option ->
                val isSelected = selectedRepeat.equals(option, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelected(option) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDaysPillGroup(
    selectedDays: MutableList<String>
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                val isSelected = selectedDays.contains(day)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (isSelected) selectedDays.remove(day)
                            else selectedDays.add(day)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.take(2),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiAppScheduleCard(
    item: ScheduleWithApps,
    isActiveNow: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActiveNow) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActiveNow) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActiveNow) Icons.Default.Lock else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isActiveNow) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        val appNames = item.apps.map { it.appName.ifBlank { it.packageName } }
                        Text(
                            text = if (appNames.isNotEmpty()) appNames.joinToString(", ") else "No Apps",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Repeat: ${item.schedule.repeatType}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isStrictLockActive = item.schedule.inactivityEnabled && isActiveNow

                    if (item.schedule.inactivityEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(
                                    if (isStrictLockActive) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isStrictLockActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isStrictLockActive) "STRICT LOCK ACTIVE" else "INACTIVITY LOCK",
                                    color = if (isStrictLockActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(
                                if (isActiveNow) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isActiveNow) "ACTIVE NOW" else "IDLE",
                            color = if (isActiveNow) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            val isStrictLockActive = item.schedule.inactivityEnabled && isActiveNow
            if (isStrictLockActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Strict Lock Active: Apps locked until end date/time. Cannot be edited, turned off, or deleted.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time range display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "START", color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = ScheduleCalculator.formatDateTime(item.schedule.startDateTime),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "END", color = MaterialTheme.colorScheme.tertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = ScheduleCalculator.formatDateTime(item.schedule.endDateTime),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Edit, Delete & Switch or Immutable Locked Indicator
            if (isStrictLockActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Immutable until schedule ends",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOCKED",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("edit_schedule_button_${item.schedule.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Schedule",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("delete_schedule_button_${item.schedule.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Schedule",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Switch(
                        checked = item.schedule.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditScheduleDialog(
    itemWithApps: ScheduleWithApps,
    onDismiss: () -> Unit,
    onSave: (startMillis: Long, endMillis: Long, repeatType: String, customDays: String, enabled: Boolean, inactivityEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    val startCal = remember {
        Calendar.getInstance().apply { timeInMillis = itemWithApps.schedule.startDateTime }
    }
    val endCal = remember {
        Calendar.getInstance().apply { timeInMillis = itemWithApps.schedule.endDateTime }
    }

    var startDateStr by remember { mutableStateOf(formatDate(startCal)) }
    var startTimeStr by remember { mutableStateOf(formatTime(startCal)) }
    var endDateStr by remember { mutableStateOf(formatDate(endCal)) }
    var endTimeStr by remember { mutableStateOf(formatTime(endCal)) }

    var repeatType by remember { mutableStateOf(itemWithApps.schedule.repeatType) }
    val customDaysList = remember {
        val days = itemWithApps.schedule.customDays.split(",").filter { it.isNotBlank() }
        mutableStateListOf<String>().apply {
            addAll(if (days.isNotEmpty()) days else listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
        }
    }

    var isEnabled by remember { mutableStateOf(itemWithApps.schedule.enabled) }
    var inactivityEnabled by remember { mutableStateOf(itemWithApps.schedule.inactivityEnabled) }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Schedule",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val appNames = itemWithApps.apps.map { it.appName.ifBlank { it.packageName } }
                    Text(
                        text = "Apps: ${appNames.joinToString(", ")}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text("Start Date & Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(context, { _, y, m, d ->
                                    startCal.set(Calendar.YEAR, y); startCal.set(Calendar.MONTH, m); startCal.set(Calendar.DAY_OF_MONTH, d)
                                    startDateStr = formatDate(startCal)
                                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(startDateStr, fontSize = 11.sp) }
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, h, min ->
                                    startCal.set(Calendar.HOUR_OF_DAY, h); startCal.set(Calendar.MINUTE, min)
                                    startTimeStr = formatTime(startCal)
                                }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), false).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(startTimeStr, fontSize = 11.sp) }
                    }
                }

                item {
                    Text("End Date & Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(context, { _, y, m, d ->
                                    endCal.set(Calendar.YEAR, y); endCal.set(Calendar.MONTH, m); endCal.set(Calendar.DAY_OF_MONTH, d)
                                    endDateStr = formatDate(endCal)
                                }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(endDateStr, fontSize = 11.sp) }
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, h, min ->
                                    endCal.set(Calendar.HOUR_OF_DAY, h); endCal.set(Calendar.MINUTE, min)
                                    endTimeStr = formatTime(endCal)
                                }, endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE), false).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(endTimeStr, fontSize = 11.sp) }
                    }
                }

                item {
                    Text("Repeat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    RepeatSelectorCard(selectedRepeat = repeatType, onSelected = { repeatType = it })
                }

                if (repeatType == "Custom") {
                    item {
                        CustomDaysPillGroup(selectedDays = customDaysList)
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Inactivity Lock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Strict lock mode (immutable while active)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = inactivityEnabled,
                            onCheckedChange = { inactivityEnabled = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Schedule Enabled", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it }, modifier = Modifier.scale(0.8f))
                    }
                }

                if (errorMsg != null) {
                    item {
                        Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sMillis = startCal.timeInMillis
                    val eMillis = endCal.timeInMillis
                    if (repeatType == "Once" && eMillis <= sMillis) {
                        errorMsg = "End date/time must be after start date/time."
                        return@Button
                    }
                    val customDays = if (repeatType == "Custom") customDaysList.joinToString(",") else ""
                    onSave(sMillis, eMillis, repeatType, customDays, isEnabled, inactivityEnabled)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(calendar: Calendar): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
}

private fun formatTime(calendar: Calendar): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
}
