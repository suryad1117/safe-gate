package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun OnboardingAppearanceScreen(
    navController: NavHostController,
    pinManager: PinManager,
    settingsRepository: SettingsRepository,
    currentSettings: SafeGateSettings
) {
    val scope = rememberCoroutineScope()

    var selectedTheme by remember { mutableStateOf(currentSettings.theme) }
    var selectedAccent by remember { mutableStateOf(currentSettings.accentColor) }
    var selectedAppStyle by remember { mutableStateOf(currentSettings.appStyle) }
    var selectedLockScreen by remember { mutableStateOf(currentSettings.lockScreenStyle) }
    var selectedCornerStyle by remember { mutableStateOf(currentSettings.cornerStyle) }
    var animationsEnabled by remember { mutableStateOf(currentSettings.animations) }
    var hapticFeedbackEnabled by remember { mutableStateOf(currentSettings.hapticFeedback) }
    var showAppIconsEnabled by remember { mutableStateOf(currentSettings.showAppIcons) }

    val accentColors = listOf(
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
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
                                iconStyle = AppIconStyle.DEFAULT
                            )
                            pinManager.setThemeSetting(selectedTheme.name.lowercase())
                            pinManager.setAccentColor(selectedAccent.name.lowercase())
                            pinManager.setLockScreenStyle(selectedLockScreen.name.lowercase())

                            // Mark Setup Completed!
                            settingsRepository.setSetupCompleted(true)

                            navController.navigate("dashboard") {
                                popUpTo("welcome") { inclusive = true }
                                popUpTo("onboarding_permissions") { inclusive = true }
                                popUpTo("onboarding_security") { inclusive = true }
                                popUpTo("onboarding_appearance") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("complete_setup_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMPLETE SETUP & ENTER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "APPEARANCE SETUP",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize the visual theme, colors, and styling of Safe Gate.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Theme Selection
            item {
                Text(
                    text = "Theme Preference",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Triple(AppTheme.SYSTEM, "System Default", "Follow Android system appearance setting"),
                            Triple(AppTheme.LIGHT, "Light Mode", "Crisp daytime clean palette"),
                            Triple(AppTheme.DARK, "Dark Mode", "High-contrast midnight cyber theme (Recommended)"),
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

            // 2. Accent Color Selection
            item {
                Text(
                    text = "Accent Color",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        accentColors.chunked(4).forEach { rowColors ->
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

            // 3. App Style Selection
            item {
                Text(
                    text = "App Style",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // 4. Lock Screen Style Selection
            item {
                Text(
                    text = "Lock Screen Style",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Triple(LockScreenStyle.SECURE, "Secure", "Fortress shield layout with rate-limiting badges")
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

            // 5. Corner Style Selection
            item {
                Text(
                    text = "Corners",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}
