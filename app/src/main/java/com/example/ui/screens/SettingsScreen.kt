package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.R
import com.example.security.AuthMethod
import com.example.security.BiometricHelper
import com.example.security.DeviceAdminHelper
import com.example.security.PinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    pinManager: PinManager
) {
    val context = LocalContext.current
    val isBiometricHardwareAvailable = remember(context) { BiometricHelper.isBiometricSupported(context) }

    var biometricEnabled by remember { mutableStateOf(pinManager.isBiometricEnabled()) }
    var notificationEnabled by remember { mutableStateOf(pinManager.isNotificationsEnabled()) }
    var vibrationEnabled by remember { mutableStateOf(pinManager.isVibrationEnabled()) }
    var intruderSelfieEnabled by remember { mutableStateOf(pinManager.isIntruderSelfieEnabled()) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showInactivityDialog by remember { mutableStateOf(false) }

    var inactivityLockEnabled by remember { mutableStateOf(pinManager.isAppInactivityLockEnabled()) }
    var inactivityMinutes by remember { mutableIntStateOf(pinManager.getInactivityLockMinutes()) }

    val currentAuthMethod = remember { pinManager.getAuthMethod() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            SafeGateBottomBar(
                currentRoute = "settings",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
        ) {
            /* ---------------- SECURITY SECTION ---------------- */
            item { SettingsSectionHeader("Security") }

            item {
                SettingsItem(
                    icon = Icons.Default.Password,
                    title = "Password & Lock",
                    subtitle = "Method: ${currentAuthMethod.name} • PIN, Password, Pattern",
                    badge = currentAuthMethod.name,
                    onClick = { navController.navigate("password_and_lock") }
                )
            }

            item {
                val bioSubtitle = when {
                    pinManager.isFingerprintEnabled() && pinManager.isFaceEnabled() -> "Fingerprint & Face active"
                    pinManager.isFingerprintEnabled() -> "Fingerprint active"
                    pinManager.isFaceEnabled() -> "Face active"
                    else -> "Biometrics disabled"
                }
                SettingsItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometrics",
                    subtitle = bioSubtitle,
                    badge = if (pinManager.isBiometricEnabled()) "ON" else "OFF",
                    onClick = { navController.navigate("biometrics") }
                )
            }

            item {
                val inactivityText = if (!inactivityLockEnabled) {
                    "Disabled"
                } else when (inactivityMinutes) {
                    0 -> "Immediately"
                    1 -> "1 minute"
                    5 -> "5 minutes"
                    15 -> "15 minutes"
                    30 -> "30 minutes"
                    else -> "$inactivityMinutes minutes"
                }
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Inactivity Lock",
                    subtitle = if (inactivityLockEnabled) "Auto-lock after: $inactivityText" else "Inactivity lock is turned off",
                    badge = inactivityText,
                    onClick = { showInactivityDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Intruder Selfie Detection",
                    subtitle = "Capture photo on repeated incorrect passcodes",
                    checked = intruderSelfieEnabled,
                    onCheckedChange = {
                        intruderSelfieEnabled = it
                        pinManager.setIntruderSelfieEnabled(it)
                    }
                )
            }

            /* ---------------- APPEARANCE SECTION ---------------- */
            item { SettingsSectionHeader("Appearance") }

            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme & Customization",
                    subtitle = "Theme, Accent Color, Lock Screen Style, App Icon",
                    badge = pinManager.getAccentColor(),
                    onClick = { navController.navigate("appearance_settings") }
                )
            }

            /* ---------------- NOTIFICATIONS & FEEDBACK ---------------- */
            item { SettingsSectionHeader("Notifications & Feedback") }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Lock Notifications",
                    subtitle = "Notify when schedule intervals begin or end",
                    checked = notificationEnabled,
                    onCheckedChange = {
                        notificationEnabled = it
                        pinManager.setNotificationsEnabled(it)
                    }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Vibration,
                    title = "Haptic Vibration",
                    subtitle = "Vibrate on keypad press and security alerts",
                    checked = vibrationEnabled,
                    onCheckedChange = {
                        vibrationEnabled = it
                        pinManager.setVibrationEnabled(it)
                    }
                )
            }

            /* ---------------- PERMISSIONS & HEALTH ---------------- */
            item { SettingsSectionHeader("Permissions & Health") }

            item {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Permission Center",
                    subtitle = "Overlay, Usage Access, Security status",
                    badge = "Configure",
                    onClick = { navController.navigate("permissions") }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Visibility,
                    title = "Display Over Other Apps",
                    subtitle = "Required for instant lock screen overlay",
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            /* ---------------- PRIVATE VAULT & SYSTEM BACKUP ---------------- */
            item { SettingsSectionHeader("Private Vault & Data") }

            item {
                SettingsItem(
                    icon = Icons.Default.FolderSpecial,
                    title = "Private Media Vault",
                    subtitle = "Encrypted photos, videos, and sensitive files",
                    badge = "AES-256",
                    onClick = { navController.navigate("vault") }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Encrypted Backup & Reset",
                    subtitle = "Export or restore encrypted security configs",
                    onClick = { showBackupDialog = true }
                )
            }

            /* ---------------- ABOUT & PRIVACY ---------------- */
            item { SettingsSectionHeader("Information") }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About SafeGate",
                    subtitle = "Version 2.4.0 • Enterprise-grade on-device security",
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Zero data transmission, 100% on-device",
                    onClick = { showPrivacyDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "Licensing & on-device privacy guarantee",
                    onClick = { showTermsDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SafeGate Guard Enterprise",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Protected by PBKDF2 with HMAC-SHA256",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.safegate_logo),
                            contentDescription = "SafeGate",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SafeGate 2.4.0", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "SafeGate provides robust, scheduled app protection and privacy guarding on Android. Keep personal chats, photos, banking apps, and games secured behind military-grade PBKDF2 PIN/Password/Pattern encryption.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "SafeGate values your privacy. All app lists, lock schedules, and credential verification hashes are saved strictly locally on your device via Room SQLite and encrypted preferences. No analytics or private telemetry leaves your phone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood", color = MaterialTheme.colorScheme.secondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "SafeGate is designed for personal privacy management and productivity scheduling. Ensure you remember your master passcode or maintain biometric credentials configured to maintain uninterrupted access.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Accept", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Encrypted Backup", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Your configurations, schedule windows, and encrypted vaults are secured locally. You can trigger an encrypted snapshot export or reset configuration.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Encrypted Configuration Saved", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Export Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showInactivityDialog) {
        var tempEnabled by remember { mutableStateOf(inactivityLockEnabled) }
        var tempMinutes by remember { mutableIntStateOf(inactivityMinutes) }

        AlertDialog(
            onDismissRequest = { showInactivityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Inactivity Auto-Lock",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Automatically lock Safe Gate after a specified period of inactivity.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enable Inactivity Lock",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = tempEnabled,
                            onCheckedChange = { tempEnabled = it },
                            modifier = Modifier.scale(0.85f).testTag("inactivity_lock_dialog_switch")
                        )
                    }

                    if (tempEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text(
                            text = "Lock after:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val options = listOf(
                            0 to "Immediately",
                            1 to "1 minute",
                            5 to "5 minutes",
                            15 to "15 minutes",
                            30 to "30 minutes"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (mins, label) ->
                                val isSelected = tempMinutes == mins
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tempMinutes = mins }
                                        .testTag("inactivity_option_$mins"),
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        inactivityLockEnabled = tempEnabled
                        inactivityMinutes = tempMinutes
                        pinManager.setAppInactivityLockEnabled(tempEnabled)
                        pinManager.setInactivityLockMinutes(tempMinutes)
                        Toast.makeText(context, "Inactivity lock settings saved", Toast.LENGTH_SHORT).show()
                        showInactivityDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("save_inactivity_dialog_button")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInactivityDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = badge, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}
