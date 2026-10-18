package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.example.security.AuthMethod
import com.example.security.BiometricHelper
import com.example.security.DeviceAdminHelper
import com.example.security.PinManager
import com.example.ui.components.PatternLockView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreventUninstallScreen(
    navController: NavHostController,
    pinManager: PinManager
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var isDeviceAdminActive by remember { mutableStateOf(DeviceAdminHelper.isDeviceAdminActive(context)) }
    var isPreventUninstallEnabled by remember { mutableStateOf(pinManager.isPreventUninstallEnabled()) }

    var fingerprintEnabled by remember { mutableStateOf(pinManager.isFingerprintEnabled()) }
    var faceEnabled by remember { mutableStateOf(pinManager.isFaceEnabled()) }
    val isBiometricSupported = remember(context) { BiometricHelper.isBiometricSupported(context) }

    // Dialog & Auth State
    var showAuthDialog by remember { mutableStateOf(false) }
    var pendingAuthAction by remember { mutableStateOf<PendingAuthAction?>(null) }
    var authErrorText by remember { mutableStateOf<String?>(null) }

    val currentAuthMethod = remember { pinManager.getAuthMethod() }

    // Device Admin Activity Result Launcher
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val active = DeviceAdminHelper.isDeviceAdminActive(context)
        isDeviceAdminActive = active
        if (active) {
            pinManager.setPreventUninstallEnabled(true)
            isPreventUninstallEnabled = true
            Toast.makeText(context, "Safe Gate Uninstall Protection is now ACTIVE", Toast.LENGTH_SHORT).show()
        } else {
            pinManager.setPreventUninstallEnabled(false)
            isPreventUninstallEnabled = false
            Toast.makeText(context, "Device Administrator permission was not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Refresh state when resuming
    LaunchedEffect(Unit) {
        isDeviceAdminActive = DeviceAdminHelper.isDeviceAdminActive(context)
        if (isPreventUninstallEnabled && !isDeviceAdminActive) {
            // Keep in sync if user revoked it outside
            isDeviceAdminActive = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prevent Uninstall",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("prevent_uninstall_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
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
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // Status Hero Banner
            item {
                PreventUninstallHeroCard(
                    isProtected = isPreventUninstallEnabled && isDeviceAdminActive,
                    isAdminActive = isDeviceAdminActive
                )
            }

            // Prevent Uninstall Toggle Switch Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isPreventUninstallEnabled && isDeviceAdminActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Prevent Uninstall",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPreventUninstallEnabled && isDeviceAdminActive)
                                    "Safe Gate cannot be uninstalled without authentication."
                                else
                                    "Enable to enforce device administrator & authentication before removal.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = isPreventUninstallEnabled && isDeviceAdminActive,
                            onCheckedChange = { targetState ->
                                if (targetState) {
                                    // Turn ON: Request Device Admin activation
                                    val intent = DeviceAdminHelper.getEnableDeviceAdminIntent(context)
                                    deviceAdminLauncher.launch(intent)
                                } else {
                                    // Turn OFF: Require SafeGate Authentication
                                    pendingAuthAction = PendingAuthAction.DISABLE_PROTECTION
                                    authErrorText = null
                                    showAuthDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("prevent_uninstall_toggle")
                        )
                    }
                }
            }

            // Authentication Required Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Authentication required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "Choose which verification methods are accepted when attempting to uninstall or disable protection:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )

                        // 1. PIN / Master Credential (Always mandatory / active)
                        AuthMethodRadioOption(
                            label = "${currentAuthMethod.name} (Master)",
                            subtitle = "Primary passcode requirement",
                            selected = true,
                            enabled = false, // Always mandatory
                            onClick = {}
                        )

                        // 2. Fingerprint
                        AuthMethodRadioOption(
                            label = "Fingerprint",
                            subtitle = if (isBiometricSupported) "Biometric sensor verification" else "Biometrics not configured on device",
                            selected = fingerprintEnabled && isBiometricSupported,
                            enabled = isBiometricSupported,
                            onClick = {
                                val next = !fingerprintEnabled
                                fingerprintEnabled = next
                                pinManager.setFingerprintEnabled(next)
                            }
                        )

                        // 3. Face
                        AuthMethodRadioOption(
                            label = "Face",
                            subtitle = if (isBiometricSupported) "Facial recognition verification" else "Biometrics not configured on device",
                            selected = faceEnabled && isBiometricSupported,
                            enabled = isBiometricSupported,
                            onClick = {
                                val next = !faceEnabled
                                faceEnabled = next
                                pinManager.setFaceEnabled(next)
                            }
                        )
                    }
                }
            }

            // Uninstall Safe Gate Flow Action Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Uninstall Safe Gate",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "To safely uninstall Safe Gate, verify your master authentication below. If authentication fails, Safe Gate remains securely installed.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        // Visual Flow Chart
                        UninstallFlowDiagram()

                        Button(
                            onClick = {
                                pendingAuthAction = PendingAuthAction.UNINSTALL_APP
                                authErrorText = null
                                showAuthDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("uninstall_safegate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Authenticate & Uninstall Safe Gate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }

            // Android Limitation Info Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Important Android Limitation",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A normal Android application cannot directly replace or bypass the system uninstaller screen. Safe Gate utilizes Android Device Administrator policy and accessibility protection to strictly block uninstallation until Safe Gate authentication is validated.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Scheduled App Lock Rule Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scheduled App Lock Rule",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PIN and biometrics cannot bypass an active scheduled lock. The selected app unlocks automatically only at the configured End date and time.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Security Verification Dialog
    if (showAuthDialog) {
        SecurityVerificationDialog(
            pinManager = pinManager,
            activity = activity,
            title = if (pendingAuthAction == PendingAuthAction.UNINSTALL_APP) "Authorize Safe Gate Uninstall" else "Authorize Disable Protection",
            subtitle = if (pendingAuthAction == PendingAuthAction.UNINSTALL_APP) "Enter your credentials to safely proceed to Android uninstaller" else "Enter credentials to disable uninstall protection",
            errorMessage = authErrorText,
            onDismiss = {
                showAuthDialog = false
                pendingAuthAction = null
                authErrorText = null
            },
            onAuthSuccess = {
                showAuthDialog = false
                if (pendingAuthAction == PendingAuthAction.UNINSTALL_APP) {
                    // Correct authentication:
                    // 1. Deactivate Device Admin
                    DeviceAdminHelper.deactivateDeviceAdmin(context)
                    pinManager.setPreventUninstallEnabled(false)
                    isPreventUninstallEnabled = false
                    isDeviceAdminActive = false

                    // 2. Launch Android uninstall screen
                    val uninstallIntent = DeviceAdminHelper.getUninstallIntent(context)
                    context.startActivity(uninstallIntent)
                    Toast.makeText(context, "Authentication Verified. Please confirm uninstall in Android prompt.", Toast.LENGTH_LONG).show()
                } else if (pendingAuthAction == PendingAuthAction.DISABLE_PROTECTION) {
                    DeviceAdminHelper.deactivateDeviceAdmin(context)
                    pinManager.setPreventUninstallEnabled(false)
                    isPreventUninstallEnabled = false
                    isDeviceAdminActive = false
                    Toast.makeText(context, "Uninstall Protection Disabled", Toast.LENGTH_SHORT).show()
                }
                pendingAuthAction = null
            },
            onAuthFailed = { message ->
                authErrorText = message
            }
        )
    }
}

enum class PendingAuthAction {
    DISABLE_PROTECTION,
    UNINSTALL_APP
}

@Composable
private fun PreventUninstallHeroCard(
    isProtected: Boolean,
    isAdminActive: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    val bgBrush = if (isProtected) {
        Brush.horizontalGradient(listOf(secondaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.15f)))
    } else {
        Brush.horizontalGradient(listOf(errorColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.12f)))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isProtected) secondaryColor.copy(alpha = 0.2f) else errorColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isProtected) Icons.Default.Security else Icons.Default.GppMaybe,
                                    contentDescription = null,
                                    tint = if (isProtected) secondaryColor else errorColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isProtected) "UNINSTALL PROTECTION ACTIVE" else "PROTECTION INACTIVE",
                            fontWeight = FontWeight.Bold,
                            color = if (isProtected) secondaryColor else errorColor,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Badge(
                        containerColor = if (isProtected) secondaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isProtected) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = if (isAdminActive) "Admin Active" else "Admin Inactive",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (isProtected)
                        "Safe Gate is shielded from removal. Device administrator is active and authentication is strictly required before uninstalling."
                    else
                        "Safe Gate can currently be removed without authentication. Enable Prevent Uninstall to lock deletion behind your passcode.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AuthMethodRadioOption(
    label: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = if (enabled) onClick else null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled || selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun UninstallFlowDiagram() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Device Management & Removal Architecture:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        FlowStepRow(step = "1", text = "User initiates Safe Gate uninstall in Android", icon = Icons.Default.TouchApp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowStepRow(step = "2", text = "Android checks active Device Admin rules", icon = Icons.Default.Shield, color = MaterialTheme.colorScheme.primary)
        FlowStepRow(step = "3", text = "Admin deactivation requires Safe Gate authentication", icon = Icons.Default.Lock, color = MaterialTheme.colorScheme.secondary)
        FlowStepRow(step = "4", text = "Authorized removal → Android uninstalls Safe Gate", icon = Icons.Default.Delete, color = MaterialTheme.colorScheme.primary)
        FlowStepRow(step = "X", text = "Admin active without auth → Android blocks removal", icon = Icons.Default.Cancel, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun FlowStepRow(
    step: String,
    text: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityVerificationDialog(
    pinManager: PinManager,
    activity: FragmentActivity?,
    title: String,
    subtitle: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit,
    onAuthFailed: (String) -> Unit
) {
    val method = remember { pinManager.getAuthMethod() }
    var enteredInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val remainingAttempts = remember { pinManager.getRemainingAttempts() }
    val isBlocked = remember { pinManager.isPinBlocked() }
    val remainingMillis = remember { pinManager.getRemainingBlockTimeMillis() }

    // Trigger biometric prompt if available
    val isBiometricAvailable = remember(activity) {
        activity != null && BiometricHelper.isBiometricSupported(activity) && pinManager.isBiometricEnabled()
    }

    fun triggerBiometrics() {
        if (activity != null && isBiometricAvailable) {
            val biometricHelper = BiometricHelper(activity)
            biometricHelper.authenticateCustom(
                title = title,
                subtitle = subtitle,
                negativeButtonText = "Use ${method.name}",
                onSuccess = {
                    pinManager.resetPinAttempts()
                    onAuthSuccess()
                },
                onFailure = {
                    val attemptState = pinManager.recordFailedAttempt()
                    if (attemptState.attemptsUsed >= PinManager.MAX_ATTEMPTS) {
                        val minutes = (pinManager.getRemainingBlockTimeMillis() / 60000).coerceAtLeast(1)
                        onAuthFailed("Too many failed attempts. Locked for $minutes minutes.")
                    } else {
                        val remaining = PinManager.MAX_ATTEMPTS - attemptState.attemptsUsed
                        onAuthFailed("Biometric authentication failed. $remaining attempt(s) remaining.")
                    }
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isBlocked) {
                    val minutes = (remainingMillis / 60000).coerceAtLeast(1)
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Authentication Locked: Too many failed attempts. Please try again in $minutes minute(s). Safe Gate remains installed.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    errorMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    localError?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    when (method) {
                        AuthMethod.PIN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = enteredInput,
                                    onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) enteredInput = it },
                                    label = { Text("Enter Safe Gate PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("security_dialog_pin_input")
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Attempts remaining: $remainingAttempts / ${PinManager.MAX_ATTEMPTS}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        AuthMethod.PASSWORD -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedTextField(
                                    value = enteredInput,
                                    onValueChange = { enteredInput = it },
                                    label = { Text("Enter Master Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("security_dialog_password_input")
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Attempts remaining: $remainingAttempts / ${PinManager.MAX_ATTEMPTS}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        AuthMethod.PATTERN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Draw your master pattern to verify",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                PatternLockView(
                                    size = 220.dp,
                                    onPatternComplete = { pattern ->
                                        if (pinManager.verifyPattern(pattern)) {
                                            pinManager.resetPinAttempts()
                                            onAuthSuccess()
                                        } else {
                                            val attemptState = pinManager.recordFailedAttempt()
                                            if (attemptState.attemptsUsed >= PinManager.MAX_ATTEMPTS) {
                                                val minutes = (pinManager.getRemainingBlockTimeMillis() / 60000).coerceAtLeast(1)
                                                onAuthFailed("Too many failed attempts. Locked for $minutes minutes.")
                                            } else {
                                                val rem = PinManager.MAX_ATTEMPTS - attemptState.attemptsUsed
                                                localError = "Incorrect pattern. $rem attempt(s) remaining. Safe Gate remains installed."
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (isBiometricAvailable) {
                        OutlinedButton(
                            onClick = { triggerBiometrics() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Biometrics (Fingerprint / Face)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isBlocked && method != AuthMethod.PATTERN) {
                Button(
                    onClick = {
                        val verified = when (method) {
                            AuthMethod.PIN -> pinManager.verifyPin(enteredInput)
                            AuthMethod.PASSWORD -> pinManager.verifyPassword(enteredInput)
                            AuthMethod.PATTERN -> false
                        }

                        if (verified) {
                            pinManager.resetPinAttempts()
                            onAuthSuccess()
                        } else {
                            val attemptState = pinManager.recordFailedAttempt()
                            if (attemptState.attemptsUsed >= PinManager.MAX_ATTEMPTS) {
                                val minutes = (pinManager.getRemainingBlockTimeMillis() / 60000).coerceAtLeast(1)
                                onAuthFailed("Wrong PIN/Password. Locked for $minutes minutes. Safe Gate remains installed.")
                            } else {
                                val rem = PinManager.MAX_ATTEMPTS - attemptState.attemptsUsed
                                localError = "Wrong PIN/Password. $rem attempt(s) remaining. Safe Gate remains installed."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("security_dialog_confirm_button")
                ) {
                    Text("Verify & Proceed")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("security_dialog_cancel_button")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    )
}
