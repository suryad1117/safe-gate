package com.example.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ui.ScheduleAppSelectionItem
import com.example.ui.SelectedAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScheduleCandidateApp(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppsForScheduleScreen(
    navController: NavHostController,
    selectedAppState: SelectedAppState
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<ScheduleCandidateApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Read currently selected apps from state
    val initiallySelected = remember {
        selectedAppState.scheduleApps.value.map { it.packageName }.toSet()
    }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(initiallySelected) } }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = loadEligibleApps(context)
            withContext(Dispatchers.Main) {
                allApps = apps
                isLoading = false
            }
        }
    }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SELECT APPS",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("select_apps_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (selectedPackages.isNotEmpty()) {
                        TextButton(onClick = { selectedPackages.clear() }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                        }
                    } else if (filteredApps.isNotEmpty()) {
                        TextButton(onClick = {
                            selectedPackages.clear()
                            selectedPackages.addAll(filteredApps.map { it.packageName })
                        }) {
                            Text("Select All", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Selected: ${selectedPackages.size}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (selectedPackages.size == 1) "1 app will be scheduled"
                            else "${selectedPackages.size} apps will be scheduled",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            val items = allApps
                                .filter { selectedPackages.contains(it.packageName) }
                                .map { ScheduleAppSelectionItem(it.packageName, it.name) }
                            selectedAppState.setScheduleApps(items)
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 140.dp)
                            .testTag("select_apps_done_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DONE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("search_apps_input"),
                placeholder = {
                    Text("🔍 Search apps...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching apps found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        ScheduleAppItemRow(
                            app = app,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) {
                                    selectedPackages.remove(app.packageName)
                                } else {
                                    selectedPackages.add(app.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleAppItemRow(
    app: ScheduleCandidateApp,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (app.icon != null) {
                CandidateAppIcon(drawable = app.icon)
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun CandidateAppIcon(drawable: Drawable) {
    val bitmap = remember(drawable) {
        try {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888).also {
                val canvas = android.graphics.Canvas(it)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
        )
    } else {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun loadEligibleApps(context: Context): List<ScheduleCandidateApp> {
    val pm = context.packageManager
    val result = mutableListOf<ScheduleCandidateApp>()

    try {
        val packages = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        for (pkg in packages) {
            val isLaunchable = pm.getLaunchIntentForPackage(pkg.packageName) != null
            if (isLaunchable && pkg.packageName != context.packageName) {
                val label = pm.getApplicationLabel(pkg).toString()
                val icon = try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                result.add(ScheduleCandidateApp(name = label, packageName = pkg.packageName, icon = icon))
            }
        }
    } catch (e: Exception) {
        // Fallback
    }

    // Default Popular Apps
    val defaults = listOf(
        ScheduleCandidateApp("Instagram", "com.instagram.android"),
        ScheduleCandidateApp("YouTube", "com.google.android.youtube"),
        ScheduleCandidateApp("WhatsApp", "com.whatsapp"),
        ScheduleCandidateApp("Chrome", "com.android.chrome"),
        ScheduleCandidateApp("Facebook", "com.facebook.katana"),
        ScheduleCandidateApp("Telegram", "org.telegram.messenger"),
        ScheduleCandidateApp("TikTok", "com.zhiliaoapp.musically"),
        ScheduleCandidateApp("Calculator", "com.google.android.calculator"),
        ScheduleCandidateApp("Gallery", "com.google.android.apps.photos"),
        ScheduleCandidateApp("Camera", "com.android.camera2"),
        ScheduleCandidateApp("Snapchat", "com.snapchat.android"),
        ScheduleCandidateApp("Twitter / X", "com.twitter.android")
    )

    for (def in defaults) {
        if (result.none { it.packageName == def.packageName }) {
            result.add(def)
        }
    }

    return result.sortedBy { it.name.lowercase() }
}
