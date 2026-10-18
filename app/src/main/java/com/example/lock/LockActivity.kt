package com.example.lock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.ScheduleEntity
import com.example.scheduling.ScheduleCalculator
import com.example.security.AuthMethod
import com.example.security.BiometricHelper
import com.example.security.PinManager
import com.example.ui.components.PatternLockView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

open class LockActivity : FragmentActivity() {

    private lateinit var lockStateManager: LockStateManager
    private lateinit var unlockSessionManager: UnlockSessionManager
    private lateinit var biometricHelper: BiometricHelper
    private var targetPackageName: String = ""
    private var targetAppName: String = ""

    private var activeScheduleState = mutableStateOf<ScheduleEntity?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackageName = intent.getStringExtra("package_name") ?: ""
        targetAppName = intent.getStringExtra("app_name") ?: ""

        lockStateManager = LockStateManager.getInstance(applicationContext)
        unlockSessionManager = UnlockSessionManager(applicationContext)
        biometricHelper = BiometricHelper(this)

        if (targetAppName.isBlank() && targetPackageName.isNotBlank()) {
            targetAppName = if (targetPackageName == "safegate_admin" || targetPackageName == "uninstall_protection") {
                "SafeGate Uninstall Protection"
            } else {
                try {
                    val pm = packageManager
                    val info = pm.getApplicationInfo(targetPackageName, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    targetPackageName
                }
            }
        }

        val appIconDrawable = try {
            if (targetPackageName.isNotBlank()) {
                packageManager.getApplicationIcon(targetPackageName)
            } else null
        } catch (e: Exception) {
            null
        }

        checkActiveSchedule()

        setContent {
            MyApplicationTheme {
                val activeSchedule by activeScheduleState

                if (activeSchedule != null) {
                    // Strict Scheduled Lock (No PIN / Biometric bypass allowed)
                    ScheduledLockOverlay(
                        appName = targetAppName.ifBlank { "Protected App" },
                        appIcon = appIconDrawable,
                        schedule = activeSchedule!!,
                        onExitToHome = { returnToHomeScreen() }
                    )
                } else {
                    // Direct Protection Lock (PIN / Password / Pattern & Biometric allowed)
                    val canUseBiometrics = lockStateManager.isBiometricEnabled() && biometricHelper.isAvailable()

                    DirectProtectionLockContent(
                        appName = targetAppName.ifBlank { "Protected App" },
                        appIcon = appIconDrawable,
                        isBiometricSupported = canUseBiometrics,
                        lockStateManager = lockStateManager,
                        onTriggerBiometric = { triggerBiometricPrompt() },
                        onUnlocked = { handleUnlockSuccess() },
                        onExitToHome = { returnToHomeScreen() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkActiveSchedule()
    }

    private fun checkActiveSchedule() {
        lifecycleScope.launch {
            if (targetPackageName.isNotBlank()) {
                val schedule = lockStateManager.getActiveScheduleForApp(targetPackageName)
                activeScheduleState.value = schedule
                if (schedule == null && lockStateManager.isBiometricEnabled() && biometricHelper.isAvailable()) {
                    triggerBiometricPrompt()
                }
            }
        }
    }

    private fun triggerBiometricPrompt() {
        if (biometricHelper.isAvailable()) {
            biometricHelper.authenticate(
                onSuccess = {
                    handleUnlockSuccess()
                },
                onFailure = {
                    // User can enter PIN / Password / Pattern
                }
            )
        }
    }

    private fun handleUnlockSuccess() {
        if (targetPackageName.isNotBlank()) {
            unlockSessionManager.authorize(targetPackageName, 60_000)
            LockStateManager.grantTemporaryUnlock(targetPackageName, 60_000)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun returnToHomeScreen() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            moveTaskToBack(true)
        }
        finish()
    }
}

@Composable
private fun ScheduledLockOverlay(
    appName: String,
    appIcon: Drawable?,
    schedule: ScheduleEntity,
    onExitToHome: () -> Unit
) {
    BackHandler {
        onExitToHome()
    }

    Scaffold(
        containerColor = Color(0xFF0C0E1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0C0E1A),
                            Color(0xFF161226),
                            Color(0xFF1E1736)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top SafeGate Brand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.safegate_logo),
                        contentDescription = "SafeGate",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE GATE",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9399B2),
                        letterSpacing = 1.5.sp,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onExitToHome,
                    modifier = Modifier.testTag("scheduled_lock_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9399B2)
                    )
                }
            }

            // Center Content: Lock Shield, App Info, and Schedule Window
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF6B81), Color(0xFF7C4DFF))
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        AppLockIcon(drawable = appIcon)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF19172B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF6B81),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B81).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (schedule.inactivityEnabled) "STRICT INACTIVITY LOCK" else "SCHEDULED LOCK ACTIVE",
                        color = Color(0xFFFF6B81),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16192E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3352))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF5EE6C0),
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Locked Until",
                            fontSize = 13.sp,
                            color = Color(0xFF9399B2),
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = ScheduleCalculator.formatDateTime(schedule.endDateTime),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5EE6C0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(color = Color(0xFF242B47), thickness = 1.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (schedule.inactivityEnabled) {
                                "Strict lock active. PIN, Password, Pattern, and Biometric unlock are disabled until the scheduled end time. Removing or reopening from Recents cannot bypass this lock."
                            } else {
                                "PIN, Password and Biometric unlock are disabled during the scheduled lock window to guarantee focus and privacy."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFCDD6F4),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Bottom Action: Return to Home Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onExitToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("scheduled_lock_home_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BACK TO HOME",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "App will automatically unlock when the schedule ends.",
                    fontSize = 11.sp,
                    color = Color(0xFF676E8E)
                )
            }
        }
    }
}

@Composable
private fun DirectProtectionLockContent(
    appName: String,
    appIcon: Drawable?,
    isBiometricSupported: Boolean,
    lockStateManager: LockStateManager,
    onTriggerBiometric: () -> Unit,
    onUnlocked: () -> Unit,
    onExitToHome: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = lockStateManager.pinManager
    val authMethod = remember { pinManager.getAuthMethod() }

    var attemptState by remember { mutableStateOf(pinManager.getPinAttemptState()) }
    var isBlocked by remember { mutableStateOf(pinManager.isPinBlocked()) }
    var remainingBlockMillis by remember { mutableStateOf(pinManager.getRemainingBlockTimeMillis()) }

    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshAttempts() {
        attemptState = pinManager.getPinAttemptState()
        isBlocked = pinManager.isPinBlocked()
        remainingBlockMillis = pinManager.getRemainingBlockTimeMillis()
    }

    LaunchedEffect(isBlocked) {
        if (isBlocked) {
            while (true) {
                val remaining = pinManager.getRemainingBlockTimeMillis()
                remainingBlockMillis = remaining
                if (remaining <= 0) {
                    refreshAttempts()
                    break
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    BackHandler {
        onExitToHome()
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun vibrate() {
        try {
            if (pinManager.isVibrationEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    fun onNumberPress(digit: String) {
        if (isBlocked) return
        if (enteredPin.length < 6) {
            vibrate()
            errorMessage = null
            val newPin = enteredPin + digit
            enteredPin = newPin

            if (newPin.length >= 4) {
                if (lockStateManager.verifyPin(newPin)) {
                    refreshAttempts()
                    onUnlocked()
                } else if (newPin.length == 6) {
                    refreshAttempts()
                    if (pinManager.isPinBlocked()) {
                        errorMessage = "Authentication temporarily locked."
                    } else {
                        errorMessage = "Incorrect PIN"
                    }
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (isBlocked) return
        if (enteredPin.isNotEmpty()) {
            vibrate()
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Scaffold(
        containerColor = Color(0xFF0C0E1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0C0E1A),
                            Color(0xFF131627),
                            Color(0xFF1A1E36)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.safegate_logo),
                        contentDescription = "SafeGate",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE GATE",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9399B2),
                        letterSpacing = 1.5.sp,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onExitToHome,
                    modifier = Modifier.testTag("lock_screen_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9399B2)
                    )
                }
            }

            // Target App Identification
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF7C4DFF), Color(0xFF5EE6C0))
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        AppLockIcon(drawable = appIcon)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF16192E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF7C4DFF),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = appName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "APP LOCKED",
                    color = Color(0xFF5EE6C0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isBlocked) {
                    // Lockout Exhausted Card
                    Card(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E172E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B81).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFFF6B81),
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Attempts exhausted",
                                color = Color(0xFFFF6B81),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Please wait until the 1-minute lockout ends.",
                                color = Color(0xFFCCD0E6),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(color = Color(0xFF2E2644), thickness = 1.dp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Try again after:",
                                color = Color(0xFF9399B2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = formatCountdown(remainingBlockMillis),
                                color = Color(0xFF5EE6C0),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    when (authMethod) {
                        AuthMethod.PIN -> {
                            // PIN dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(6) { index ->
                                    val isFilled = index < enteredPin.length
                                    val scale by animateFloatAsState(if (isFilled) 1.25f else 1.0f, label = "pinDot")

                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .scale(scale)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) Color(0xFF7C4DFF) else Color(0xFF262A45)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isFilled) Color(0xFF5EE6C0) else Color(0xFF383F63),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        AuthMethod.PASSWORD -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedTextField(
                                    value = enteredPassword,
                                    onValueChange = { enteredPassword = it },
                                    label = { Text("Password", color = Color(0xFF9399B2)) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = Color(0xFF9399B2)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(0.9f).testTag("lock_password_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF7C4DFF),
                                        unfocusedBorderColor = Color(0xFF262C47),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (lockStateManager.pinManager.verifyPassword(enteredPassword)) {
                                            refreshAttempts()
                                            onUnlocked()
                                        } else {
                                            refreshAttempts()
                                            if (pinManager.isPinBlocked()) {
                                                errorMessage = "Authentication temporarily locked."
                                            } else {
                                                errorMessage = "Incorrect Password"
                                            }
                                            enteredPassword = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(0.9f).height(46.dp).testTag("lock_password_unlock_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                                ) {
                                    Text("UNLOCK", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AuthMethod.PATTERN -> {
                            PatternLockView(
                                size = 220.dp,
                                onPatternComplete = { stroke ->
                                    if (lockStateManager.pinManager.verifyPattern(stroke)) {
                                        refreshAttempts()
                                        onUnlocked()
                                    } else {
                                        refreshAttempts()
                                        if (pinManager.isPinBlocked()) {
                                            errorMessage = "Authentication temporarily locked."
                                        } else {
                                            errorMessage = "Incorrect Pattern"
                                        }
                                    }
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF6B81),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (isBiometricSupported) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = onTriggerBiometric,
                            modifier = Modifier.testTag("use_biometrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFF5EE6C0),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Use Biometrics (Fingerprint / Face)",
                                color = Color(0xFF5EE6C0),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Numeric Keypad (Shown for PIN mode)
            if (authMethod == AuthMethod.PIN) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keypadRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    keypadRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "BIO" -> {
                                        if (isBiometricSupported && !isBlocked) {
                                            KeypadActionButton(
                                                icon = Icons.Default.Fingerprint,
                                                tint = Color(0xFF5EE6C0),
                                                onClick = { onTriggerBiometric() }
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(64.dp))
                                        }
                                    }
                                    "DEL" -> {
                                        KeypadActionButton(
                                            icon = Icons.Default.Backspace,
                                            tint = if (!isBlocked) Color(0xFF9399B2) else Color(0xFF4A516E),
                                            onClick = { onBackspace() }
                                        )
                                    }
                                    else -> {
                                        KeypadNumberButton(
                                            number = key,
                                            enabled = !isBlocked,
                                            onClick = { onNumberPress(key) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AppLockIcon(drawable: Drawable) {
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
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF16192E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF7C4DFF))
        }
    }
}

@Composable
private fun KeypadNumberButton(
    number: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFF191D33) else Color(0xFF121526))
            .border(1.dp, if (enabled) Color(0xFF262C47) else Color(0xFF1B1E33), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Color(0xFF4A516E)
        )
    }
}

@Composable
private fun KeypadActionButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFF14172B))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
