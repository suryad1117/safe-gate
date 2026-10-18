package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AuthMethod
import com.example.security.PinManager
import com.example.ui.PinViewModel
import com.example.ui.components.PatternLockView

@Composable
fun LockScreenOverlay(
    appName: String,
    packageName: String,
    pinViewModel: PinViewModel,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val pinManager = pinViewModel.getPinManager()
    val authMethod = remember { pinManager.getAuthMethod() }

    val attemptState by pinViewModel.attemptState.collectAsState()
    val isBlocked by pinViewModel.isBlocked.collectAsState()
    val remainingBlockMillis by pinViewModel.remainingBlockMillis.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        pinViewModel.refreshAttemptState()
    }

    fun formatCountdown(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun handleDigit(d: String) {
        if (isBlocked) return
        if (enteredPin.length < 4) {
            val newPin = enteredPin + d
            enteredPin = newPin
            errorMessage = null
            if (newPin.length == 4) {
                if (pinViewModel.verifyPin(newPin)) {
                    onUnlocked()
                } else {
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

    fun handleBackspace() {
        if (isBlocked) return
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    fun submitPassword(pass: String) {
        if (isBlocked) return
        if (pass.length != 8) {
            errorMessage = "Password must be exactly 8 characters"
            return
        }
        if (pinViewModel.verifyPassword(pass)) {
            onUnlocked()
        } else {
            if (pinManager.isPinBlocked()) {
                errorMessage = "Authentication temporarily locked."
            } else {
                errorMessage = "Incorrect Password"
            }
            enteredPassword = ""
        }
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
            // Header with Dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "SAFE GATE",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("lock_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit to SafeGate",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Locked App Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked App",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = appName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "APP LOCKED",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isBlocked) {
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
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Attempts exhausted",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Please wait until the 1-minute lockout ends.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Try again after:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = formatCountdown(remainingBlockMillis),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                } else {
                    when (authMethod) {
                        AuthMethod.PIN -> {
                            // Exactly 4 PIN dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) { index ->
                                    PinDot(
                                        filled = enteredPin.length > index
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        AuthMethod.PASSWORD -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Exactly 8 Password dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(8) { index ->
                                        PasswordDot(
                                            filled = enteredPassword.length > index
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = enteredPassword,
                                    onValueChange = { input ->
                                        if (input.length <= 8) {
                                            enteredPassword = input
                                            errorMessage = null
                                            if (input.length == 8) {
                                                submitPassword(input)
                                            }
                                        }
                                    },
                                    label = { Text("Password (8 chars)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submitPassword(enteredPassword) }),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { submitPassword(enteredPassword) },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("UNLOCK", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AuthMethod.PATTERN -> {
                            // 3x3 pattern grid with no dots
                            PatternLockView(
                                size = 230.dp,
                                onPatternComplete = { stroke ->
                                    if (pinViewModel.verifyPattern(stroke)) {
                                        onUnlocked()
                                    } else {
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
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            // Keypad (for PIN) or Biometric action
            if (authMethod == AuthMethod.PIN) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
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
                                            modifier = Modifier.size(68.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = if (!isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    "bio" -> {
                                        IconButton(
                                            onClick = {
                                                if (pinViewModel.authenticateWithBiometric()) {
                                                    onUnlocked()
                                                }
                                            },
                                            enabled = !isBlocked,
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(if (!isBlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Fingerprint",
                                                tint = if (!isBlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(if (!isBlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                                .border(1.dp, if (!isBlocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                                                .clickable(enabled = !isBlocked) { handleDigit(key) }
                                                .testTag("lock_key_$key"),
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
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        if (pinViewModel.authenticateWithBiometric()) {
                            onUnlocked()
                        }
                    }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Fingerprint / Face", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
