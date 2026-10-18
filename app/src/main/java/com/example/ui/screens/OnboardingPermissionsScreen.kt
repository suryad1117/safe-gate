package com.example.ui.screens

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.example.permissions.PermissionUtils
import com.example.service.LockMonitorController

@Composable
fun OnboardingPermissionsScreen(
    navController: NavHostController
) {
    val context = LocalContext.current

    var isUsageAccessEnabled by remember { mutableStateOf(checkUsageAccess(context)) }
    var isOverlayEnabled by remember { mutableStateOf(checkOverlayPermission(context)) }
    var isCameraEnabled by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var isNotificationsEnabled by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }
    var isBatteryOptimizedIgnored by remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isIgnoringBatteryOptimizations(context.packageName) == true
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isCameraEnabled = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isNotificationsEnabled = granted
    }

    fun refreshPermissions() {
        isUsageAccessEnabled = checkUsageAccess(context)
        isOverlayEnabled = checkOverlayPermission(context)
        isCameraEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        isNotificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        isBatteryOptimizedIgnored = (context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    // Dynamic auto-check whenever user returns from Android Settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPermissions()
    }

    val corePermissionsGranted = isUsageAccessEnabled && isOverlayEnabled
    val grantedCount = listOf(isUsageAccessEnabled, isOverlayEnabled, isCameraEnabled, isNotificationsEnabled, isBatteryOptimizedIgnored).count { it }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!corePermissionsGranted) {
                    Text(
                        text = "Usage Access and Overlay permissions are required to enable app locking",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Button(
                    enabled = corePermissionsGranted,
                    onClick = {
                        LockMonitorController.start(context)
                        navController.navigate("onboarding_security")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_continue_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (corePermissionsGranted) "CONTINUE" else "ENABLE REQUIRED TO CONTINUE ($grantedCount/5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    if (corePermissionsGranted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "PERMISSION SETUP",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Safe Gate requires these permissions to detect foreground apps, display the security barrier, and keep protection active.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Badge Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (corePermissionsGranted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (corePermissionsGranted) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary)
                        ) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (corePermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (corePermissionsGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (corePermissionsGranted) "Required permissions active ($grantedCount/5 enabled)"
                                else "Permissions enabled: $grantedCount of 5",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (corePermissionsGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 1. Camera Access
            item {
                PermissionSetupCard(
                    icon = Icons.Default.PhotoCamera,
                    title = "Camera Access",
                    subtitle = "Enables direct photo capture into your Private Vault and intruder selfie detection.",
                    buttonLabel = if (isCameraEnabled) "ENABLED" else "ALLOW",
                    isGranted = isCameraEnabled,
                    onAction = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }

            // 2. Notification Permission
            item {
                PermissionSetupCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Sends immediate warnings upon unauthorized access and scheduled lock alerts.",
                    buttonLabel = if (isNotificationsEnabled) "ENABLED" else "ALLOW",
                    isGranted = isNotificationsEnabled,
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationsEnabled) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings(context)
                        }
                    }
                )
            }

            // 3. Usage Access
            item {
                PermissionSetupCard(
                    icon = Icons.Default.QueryStats,
                    title = "Usage Access (Required)",
                    subtitle = "Allows Safe Gate to identify when a guarded application opens in the foreground.",
                    buttonLabel = if (isUsageAccessEnabled) "ENABLED" else "ENABLE",
                    isGranted = isUsageAccessEnabled,
                    onAction = {
                        openUsageAccessSettings(context)
                    }
                )
            }

            // 4. Display over other apps
            item {
                PermissionSetupCard(
                    icon = Icons.Default.Layers,
                    title = "Display over other apps (Required)",
                    subtitle = "Shows the Safe Gate security lock barrier over protected applications.",
                    buttonLabel = if (isOverlayEnabled) "ENABLED" else "ENABLE",
                    isGranted = isOverlayEnabled,
                    onAction = {
                        openOverlaySettings(context)
                    }
                )
            }

            // 5. Battery Optimization
            item {
                PermissionSetupCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Settings",
                    subtitle = "Allows Safe Gate to run in the background without being suspended by OS battery savers.",
                    buttonLabel = if (isBatteryOptimizedIgnored) "UNRESTRICTED" else "ALLOW",
                    isGranted = isBatteryOptimizedIgnored,
                    onAction = {
                        openBatteryOptimizationSettings(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionSetupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isGranted) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
            )
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buttonLabel,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = buttonLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private fun checkUsageAccess(context: Context): Boolean {
    return PermissionUtils.hasUsageAccess(context)
}

private fun checkOverlayPermission(context: Context): Boolean {
    return PermissionUtils.hasOverlayPermission(context)
}

private fun openUsageAccessSettings(context: Context) {
    PermissionUtils.openUsageAccessSettings(context)
}

private fun openOverlaySettings(context: Context) {
    PermissionUtils.openOverlaySettings(context)
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)
}

private fun openBatteryOptimizationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

