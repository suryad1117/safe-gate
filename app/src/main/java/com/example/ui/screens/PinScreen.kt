package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.security.AuthMethod
import com.example.security.BiometricHelper
import com.example.security.PinManager
import com.example.ui.PinViewModel
import com.example.ui.components.PatternLockView

@Composable
fun PinScreen(
    pinViewModel: PinViewModel,
    isChangePinMode: Boolean = false,
    onPinSuccess: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = pinViewModel.getPinManager()
    val authMethod = remember { pinManager.getAuthMethod() }

    val isPinSet by pinViewModel.isPinSet.collectAsState()
    val attemptState by pinViewModel.attemptState.collectAsState()
    val isBlocked by pinViewModel.isBlocked.collectAsState()
    val remainingBlockMillis by pinViewModel.remainingBlockMillis.collectAsState()

    var enteredValue by remember { mutableStateOf("") }
    var confirmValue by remember { mutableStateOf("") }
    var isConfirmingStep by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordText by remember { mutableStateOf(false) }

    val maxLength = remember(authMethod) {
        when (authMethod) {
            AuthMethod.PIN -> 4
            AuthMethod.PASSWORD -> 8
            AuthMethod.PATTERN -> 0
        }
    }

    LaunchedEffect(Unit) {
        pinViewModel.refreshAttemptState()
    }

    val modeTitle = when {
        isChangePinMode -> {
            when (authMethod) {
                AuthMethod.PIN -> if (isConfirmingStep) "Confirm New PIN" else "Set New 4-Digit PIN"
                AuthMethod.PASSWORD -> if (isConfirmingStep) "Confirm Password" else "Set New 8-Char Password"
                AuthMethod.PATTERN -> if (isConfirmingStep) "Confirm Pattern" else "Draw New Pattern"
            }
        }
        !isPinSet -> {
            when (authMethod) {
                AuthMethod.PIN -> if (isConfirmingStep) "Confirm Your PIN" else "Create 4-Digit Master PIN"
                AuthMethod.PASSWORD -> if (isConfirmingStep) "Confirm Password" else "Create 8-Char Password"
                AuthMethod.PATTERN -> if (isConfirmingStep) "Confirm Pattern" else "Create Pattern"
            }
        }
        isBlocked -> "Attempts exhausted"
        else -> {
            when (authMethod) {
                AuthMethod.PIN -> "Enter PIN"
                AuthMethod.PASSWORD -> "Enter Password"
                AuthMethod.PATTERN -> "Draw Pattern"
            }
        }
    }

    val modeSubtitle = when {
        isChangePinMode || !isPinSet -> {
            when (authMethod) {
                AuthMethod.PIN -> if (isConfirmingStep) "Re-enter the 4 digits to verify" else "Protect Safe Gate with a 4-digit PIN"
                AuthMethod.PASSWORD -> if (isConfirmingStep) "Re-enter your 8-character password" else "Enter exactly 8 characters"
                AuthMethod.PATTERN -> if (isConfirmingStep) "Draw the pattern again to confirm" else "Connect at least 3 dots"
            }
        }
        isBlocked -> "Please wait until the 1-minute lockout ends."
        else -> {
            when (authMethod) {
                AuthMethod.PIN -> "Enter your 4-digit security PIN"
                AuthMethod.PASSWORD -> "Enter your 8-character password"
                AuthMethod.PATTERN -> "Connect dots to unlock Safe Gate"
            }
        }
    }

    fun submitCredential(input: String) {
        if (!isPinSet || isChangePinMode) {
            if (!isConfirmingStep) {
                confirmValue = input
                enteredValue = ""
                isConfirmingStep = true
            } else {
                if (input == confirmValue) {
                    when (authMethod) {
                        AuthMethod.PIN -> pinViewModel.createPin(input)
                        AuthMethod.PASSWORD -> pinViewModel.createPassword(input)
                        AuthMethod.PATTERN -> pinViewModel.createPattern(input)
                    }
                    onPinSuccess()
                } else {
                    errorMessage = when (authMethod) {
                        AuthMethod.PIN -> "PINs do not match. Please try again."
                        AuthMethod.PASSWORD -> "Passwords do not match. Please try again."
                        AuthMethod.PATTERN -> "Patterns do not match. Please try again."
                    }
                    enteredValue = ""
                    confirmValue = ""
                    isConfirmingStep = false
                }
            }
        } else {
            val success = when (authMethod) {
                AuthMethod.PIN -> pinViewModel.verifyPin(input)
                AuthMethod.PASSWORD -> pinViewModel.verifyPassword(input)
                AuthMethod.PATTERN -> pinViewModel.verifyPattern(input)
            }

            if (success) {
                onPinSuccess()
            } else {
                enteredValue = ""
                pinViewModel.refreshAttemptState()
                val currentAttempts = pinManager.getAttemptsUsed()
                if (currentAttempts >= PinManager.MAX_ATTEMPTS) {
                    errorMessage = "Authentication temporarily locked."
                } else {
                    val typeName = when (authMethod) {
                        AuthMethod.PIN -> "PIN"
                        AuthMethod.PASSWORD -> "Password"
                        AuthMethod.PATTERN -> "Pattern"
                    }
                    errorMessage = "Incorrect $typeName"
                }
            }
        }
    }

    fun handleDigitPress(digit: String) {
        if (isBlocked && isPinSet && !isChangePinMode) return

        if (enteredValue.length < maxLength) {
            val newValue = enteredValue + digit
            enteredValue = newValue
            errorMessage = null

            if (newValue.length == maxLength) {
                submitCredential(newValue)
            }
        }
    }

    fun handleBackspace() {
        if (enteredValue.isNotEmpty()) {
            enteredValue = enteredValue.dropLast(1)
            errorMessage = null
        }
    }

    fun handleBiometricClick() {
        if (isBlocked) return
        val activity = context as? FragmentActivity
        if (activity != null) {
            val helper = BiometricHelper(activity)
            if (helper.isAvailable()) {
                helper.authenticate(
                    onSuccess = {
                        pinViewModel.authenticateWithBiometric()
                        onPinSuccess()
                    },
                    onFailure = {
                        // Dismissed
                    }
                )
                return
            }
        }
        if (pinViewModel.authenticateWithBiometric()) {
            onPinSuccess()
        }
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: SafeGate Brand Header & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                // Header with Logo and Brand Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.safegate_logo),
                            contentDescription = "SafeGate Logo",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "SAFE GATE",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = modeTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = modeSubtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (isBlocked && isPinSet && !isChangePinMode) {
                    // Lockout Exhausted Card
                    Card(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(44.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Attempts exhausted",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Please wait until the 1-minute lockout ends.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Try again after:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = formatCountdown(remainingBlockMillis),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    // Dynamic indicators based on AuthMethod
                    when (authMethod) {
                        AuthMethod.PIN -> {
                            // Exactly 4 dots for PIN: [ • • • • ]
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) { index ->
                                    PinDot(
                                        filled = enteredValue.length > index
                                    )
                                }
                            }
                        }

                        AuthMethod.PASSWORD -> {
                            // Exactly 8 dots for Password: [ • • • • • • • • ]
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(8) { index ->
                                    PasswordDot(
                                        filled = enteredValue.length > index
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Text Field Input for 8 characters
                            OutlinedTextField(
                                value = enteredValue,
                                onValueChange = { input ->
                                    if (input.length <= 8) {
                                        enteredValue = input
                                        errorMessage = null
                                        if (input.length == 8) {
                                            submitCredential(input)
                                        }
                                    }
                                },
                                label = { Text("Password (8 chars)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                visualTransformation = if (showPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPasswordText = !showPasswordText }) {
                                        Icon(
                                            imageVector = if (showPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (enteredValue.length == 8) submitCredential(enteredValue)
                                    else errorMessage = "Password must be exactly 8 characters"
                                }),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .testTag("pin_screen_password_input"),
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        AuthMethod.PATTERN -> {
                            // 3x3 pattern grid, no dots displayed above
                            PatternLockView(
                                size = 250.dp,
                                onPatternComplete = { stroke ->
                                    submitCredential(stroke)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Keypad Section (for PIN) or Biometric & Unlock Action
            when (authMethod) {
                AuthMethod.PIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("bio", "0", "del")
                        )

                        for (row in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (key in row) {
                                    when (key) {
                                        "del" -> {
                                            IconButton(
                                                onClick = { handleBackspace() },
                                                enabled = !isBlocked,
                                                modifier = Modifier
                                                    .size(70.dp)
                                                    .clip(CircleShape)
                                                    .testTag("pin_backspace_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Backspace,
                                                    contentDescription = "Backspace",
                                                    tint = if (!isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        "bio" -> {
                                            if (isPinSet && !isChangePinMode) {
                                                IconButton(
                                                    onClick = { handleBiometricClick() },
                                                    enabled = !isBlocked,
                                                    modifier = Modifier
                                                        .size(70.dp)
                                                        .clip(CircleShape)
                                                        .background(if (!isBlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                                        .testTag("pin_biometric_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fingerprint,
                                                        contentDescription = "Biometric Unlock",
                                                        tint = if (!isBlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(70.dp))
                                            }
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(70.dp)
                                                    .clip(CircleShape)
                                                    .background(if (!isBlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                                    .border(
                                                        1.dp,
                                                        if (!isBlocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        CircleShape
                                                    )
                                                    .clickable(enabled = !isBlocked) { handleDigitPress(key) }
                                                    .testTag("pin_key_$key"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontSize = 26.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AuthMethod.PASSWORD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (enteredValue.length == 8) {
                                    submitCredential(enteredValue)
                                } else {
                                    errorMessage = "Password must be exactly 8 characters"
                                }
                            },
                            enabled = !isBlocked,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(50.dp)
                                .testTag("password_unlock_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("UNLOCK", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                        }

                        if (isPinSet && !isChangePinMode) {
                            TextButton(onClick = { handleBiometricClick() }) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Biometric", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                AuthMethod.PATTERN -> {
                    if (isPinSet && !isChangePinMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = { handleBiometricClick() }) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Biometric", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PinDot(
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (filled) 1.25f else 1.0f, label = "pinDotScale")

    Box(
        modifier = modifier
            .size(18.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.5.dp,
                color = if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    )
}

@Composable
fun PasswordDot(
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (filled) 1.2f else 1.0f, label = "passDotScale")

    Box(
        modifier = modifier
            .size(14.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    )
}
