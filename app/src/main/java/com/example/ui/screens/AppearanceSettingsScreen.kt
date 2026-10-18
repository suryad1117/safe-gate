package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.data.AccentColor
import com.example.data.AppIconStyle
import com.example.data.AppStyle
import com.example.data.AppTheme
import com.example.data.CornerStyle
import com.example.data.LockScreenStyle
import com.example.data.SafeGateSettings
import com.example.data.SettingsRepository
import com.example.security.PinManager
import com.example.ui.theme.getPrimaryColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    navController: NavHostController,
    pinManager: PinManager,
    settingsRepository: SettingsRepository,
    currentSettings: SafeGateSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTheme by remember(currentSettings.theme) { mutableStateOf(currentSettings.theme) }
    var selectedAccent by remember(currentSettings.accentColor) { mutableStateOf(currentSettings.accentColor) }
    var selectedAppStyle by remember(currentSettings.appStyle) { mutableStateOf(currentSettings.appStyle) }
    var selectedLockScreen by remember(currentSettings.lockScreenStyle) { mutableStateOf(currentSettings.lockScreenStyle) }
    var selectedCornerStyle by remember(currentSettings.cornerStyle) { mutableStateOf(currentSettings.cornerStyle) }
    var animationsEnabled by remember(currentSettings.animations) { mutableStateOf(currentSettings.animations) }
    var hapticFeedbackEnabled by remember(currentSettings.hapticFeedback) { mutableStateOf(currentSettings.hapticFeedback) }
    var showAppIconsEnabled by remember(currentSettings.showAppIcons) { mutableStateOf(currentSettings.showAppIcons) }
    var selectedIconStyle by remember(currentSettings.appIconStyle) { mutableStateOf(currentSettings.appIconStyle) }

    val accentColorList = listOf(
        Pair(AccentColor.NONE, "None"),
        Pair(AccentColor.BLUE, "Blue"),
        Pair(AccentColor.NAVY, "Navy"),
        Pair(AccentColor.PURPLE, "Purple"),
        Pair(AccentColor.PRUSSIAN_BLUE, "Prussian Blue"),
        Pair(AccentColor.BLACK, "Black"),
        Pair(AccentColor.GREEN, "Green"),
        Pair(AccentColor.RED, "Red"),
        Pair(AccentColor.ORANGE, "Orange"),
        Pair(AccentColor.PINK, "Pink"),
        Pair(AccentColor.TEAL, "Teal"),
        Pair(AccentColor.CYAN, "Cyan")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appearance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("appearance_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
        ) {
            // 1. Theme Section
            item {
                Text(
                    text = "Theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Triple(AppTheme.SYSTEM, "System Default", "Follow device light/dark system setting"),
                            Triple(AppTheme.LIGHT, "Light", "Crisp daylight high-contrast palette"),
                            Triple(AppTheme.DARK, "Dark", "High-contrast midnight cyber dark theme"),
                            Triple(AppTheme.AMOLED, "AMOLED (Pure Black)", "Deep pure pitch-black canvas for OLED screens")
                        ).forEach { (theme, label, desc) ->
                            val isSelected = selectedTheme == theme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { selectedTheme = theme }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTheme = theme },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Accent Color Section
            item {
                Text(
                    text = "Accent Color",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        accentColorList.chunked(4).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowColors.forEach { (accent, name) ->
                                    val isSelected = selectedAccent == accent
                                    val rawColor = if (accent == AccentColor.NONE) MaterialTheme.colorScheme.primary else getPrimaryColor(accent)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedAccent = accent }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (accent == AccentColor.NONE) MaterialTheme.colorScheme.surfaceVariant
                                                    else rawColor
                                                )
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (accent == AccentColor.NONE) {
                                                Text(
                                                    text = "∅",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. App Style Section
            item {
                Text(
                    text = "App Style",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair(AppStyle.CLASSIC, "Classic"),
                            Pair(AppStyle.MODERN, "Modern"),
                            Pair(AppStyle.MINIMAL, "Minimal")
                        ).forEach { (style, label) ->
                            val isSelected = selectedAppStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAppStyle = style },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // 4. Lock Screen Style Section
            item {
                Text(
                    text = "Lock Screen Style",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Triple(LockScreenStyle.CLASSIC, "Classic", "Standard PIN/Password card keypad layout"),
                            Triple(LockScreenStyle.MODERN, "Modern", "Dynamic accent cards with frosted glow"),
                            Triple(LockScreenStyle.SECURE, "Secure", "Fortress shield layout with attempt tracker")
                        ).forEach { (style, label, desc) ->
                            val isSelected = selectedLockScreen == style
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { selectedLockScreen = style }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLockScreen = style },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Corner Style Section
            item {
                Text(
                    text = "Corners",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair(CornerStyle.SQUARE, "Square"),
                            Pair(CornerStyle.ROUNDED, "Rounded"),
                            Pair(CornerStyle.EXTRA_ROUNDED, "Extra Rounded")
                        ).forEach { (corner, label) ->
                            val isSelected = selectedCornerStyle == corner
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCornerStyle = corner },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // 6. UI Preferences Toggles
            item {
                Text(
                    text = "Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Animations
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Animations", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Smooth screen transitions & effects", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = animationsEnabled,
                                onCheckedChange = { animationsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Haptic Feedback
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Haptic Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Vibrate on keypad and lock touches", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = hapticFeedbackEnabled,
                                onCheckedChange = { hapticFeedbackEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Show App Icons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show App Icons", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Display target app icons on lock screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = showAppIconsEnabled,
                                onCheckedChange = { showAppIconsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // 7. App Icon Style Section
            item {
                Text(
                    text = "App Icon",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            Pair(AppIconStyle.DEFAULT, "Default (Classic Gate)"),
                            Pair(AppIconStyle.SHIELD, "Shield (Aegis Security)"),
                            Pair(AppIconStyle.LOCK, "Lock (Cipher Lock)")
                        ).forEach { (iconStyle, label) ->
                            val isSelected = selectedIconStyle == iconStyle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                                    .clickable { selectedIconStyle = iconStyle }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedIconStyle = iconStyle },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = when (iconStyle) {
                                        AppIconStyle.SHIELD -> Icons.Default.Shield
                                        AppIconStyle.LOCK -> Icons.Default.Lock
                                        else -> Icons.Default.Security
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 8. Apply Changes Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            settingsRepository.saveAppearance(
                                theme = selectedTheme,
                                accent = selectedAccent,
                                appStyle = selectedAppStyle,
                                lockScreen = selectedLockScreen,
                                cornerStyle = selectedCornerStyle,
                                animations = animationsEnabled,
                                hapticFeedback = hapticFeedbackEnabled,
                                showAppIcons = showAppIconsEnabled,
                                iconStyle = selectedIconStyle
                            )
                            pinManager.setThemeSetting(selectedTheme.name.lowercase())
                            pinManager.setAccentColor(selectedAccent.name.lowercase())
                            pinManager.setLockScreenStyle(selectedLockScreen.name.lowercase())
                            pinManager.setAppIconStyle(selectedIconStyle.name.lowercase())
                            Toast.makeText(context, "Appearance Settings Applied", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("apply_appearance_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APPLY CHANGES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
