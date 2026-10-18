package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.security.AuthMethod
import com.example.security.BiometricHelper
import com.example.security.PinManager
import com.example.ui.components.PatternLockView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordAndLockScreen(
    navController: NavHostController,
    pinManager: PinManager
) {
    val context = LocalContext.current
    val isBiometricSupported = remember(context) { BiometricHelper.isBiometricSupported(context) }

    var selectedMethod by remember { mutableStateOf(pinManager.getAuthMethod()) }
    var fingerprintEnabled by remember { mutableStateOf(pinManager.isFingerprintEnabled()) }
    var faceEnabled by remember { mutableStateOf(pinManager.isFaceEnabled()) }

    // PIN Subflow State
    var pinValue by remember { mutableStateOf("") }
    var confirmPinValue by remember { mutableStateOf("") }

    // Password Subflow State
    var passwordValue by remember { mutableStateOf("") }
    var confirmPasswordValue by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Pattern Subflow State
    var minPatternDots by remember { mutableStateOf(pinManager.getMinPatternDots()) }
    var patternStep by remember { mutableStateOf(1) } // 1 = draw, 2 = redraw confirm
    var firstPattern by remember { mutableStateOf("") }
    var secondPattern by remember { mutableStateOf("") }
    var confirmedPattern by remember { mutableStateOf<String?>(null) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = MaterialTheme.colorScheme.tertiary
    val successColor = MaterialTheme.colorScheme.secondary
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun calculatePasswordStrength(pass: String): Triple<String, Color, Float> {
        if (pass.isEmpty()) return Triple("Empty", neutralColor, 0f)
        var score = 0
        if (pass.length >= 6) score++
        if (pass.length >= 10) score++
        if (pass.any { it.isDigit() }) score++
        if (pass.any { it.isUpperCase() }) score++
        if (pass.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 2 -> Triple("Weak", errorColor, 0.33f)
            score in 3..4 -> Triple("Medium", warningColor, 0.66f)
            else -> Triple("Strong", successColor, 1.0f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Password & Lock",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("password_lock_back_button")
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
            contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
        ) {
            // Section 1: Choose your unlock method
            item {
                Text(
                    text = "Choose your unlock method",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // PIN Option
                    AuthMethodSelectOption(
                        title = "PIN",
                        subtitle = "4-digit numeric passcode",
                        icon = Icons.Default.Pin,
                        isSelected = selectedMethod == AuthMethod.PIN,
                        onClick = {
                            selectedMethod = AuthMethod.PIN
                            statusMessage = null
                        }
                    )

                    // Password Option
                    AuthMethodSelectOption(
                        title = "Password",
                        subtitle = "8-character password",
                        icon = Icons.Default.Password,
                        isSelected = selectedMethod == AuthMethod.PASSWORD,
                        onClick = {
                            selectedMethod = AuthMethod.PASSWORD
                            statusMessage = null
                        }
                    )

                    // Pattern Option
                    AuthMethodSelectOption(
                        title = "Pattern",
                        subtitle = "Draw a pattern to unlock",
                        icon = Icons.Default.Gesture,
                        isSelected = selectedMethod == AuthMethod.PATTERN,
                        onClick = {
                            selectedMethod = AuthMethod.PATTERN
                            patternStep = 1
                            firstPattern = ""
                            secondPattern = ""
                            statusMessage = null
                        }
                    )
                }
            }

            // Section 2: Method-Specific Setup Card
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
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (selectedMethod) {
                            AuthMethod.PIN -> {
                                Text(
                                    text = "Create PIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = pinValue,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinValue = it },
                                    label = { Text("Enter new PIN (4 digits)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("enter_new_pin_input"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = confirmPinValue,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinValue = it },
                                    label = { Text("Confirm PIN", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (pinValue.length != 4) {
                                            isError = true
                                            statusMessage = "PIN must be exactly 4 digits."
                                            return@Button
                                        }
                                        if (pinValue != confirmPinValue) {
                                            isError = true
                                            statusMessage = "PINs do not match."
                                            return@Button
                                        }

                                        pinManager.savePin(pinValue)
                                        pinManager.setAuthMethod(AuthMethod.PIN)
                                        isError = false
                                        statusMessage = "PIN created successfully!"
                                        Toast.makeText(context, "PIN Saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_pin_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("CREATE PIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }

                            AuthMethod.PASSWORD -> {
                                Text(
                                    text = "Create Password",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = passwordValue,
                                    onValueChange = { if (it.length <= 8) passwordValue = it },
                                    label = { Text("New Password (8 chars)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("enter_new_password_input"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = confirmPasswordValue,
                                    onValueChange = { if (it.length <= 8) confirmPasswordValue = it },
                                    label = { Text("Confirm Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("confirm_password_input"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = showPassword,
                                        onCheckedChange = { showPassword = it },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = "Show password",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable { showPassword = !showPassword }
                                    )
                                }

                                // Password strength meter
                                val (strengthLabel, strengthColor, strengthFraction) = calculatePasswordStrength(passwordValue)
                                if (passwordValue.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Password strength", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Text(text = strengthLabel, color = strengthColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { strengthFraction },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = strengthColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (passwordValue.length != 8) {
                                            isError = true
                                            statusMessage = "Password must be exactly 8 characters long."
                                            return@Button
                                        }
                                        if (passwordValue != confirmPasswordValue) {
                                            isError = true
                                            statusMessage = "Passwords do not match."
                                            return@Button
                                        }

                                        pinManager.savePassword(passwordValue)
                                        pinManager.setAuthMethod(AuthMethod.PASSWORD)
                                        isError = false
                                        statusMessage = "Password created successfully!"
                                        Toast.makeText(context, "Password Saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_password_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("CREATE PASSWORD", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }

                            AuthMethod.PATTERN -> {
                                Text(
                                    text = "Pattern Lock",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Minimum pattern length selector
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Minimum pattern length",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        FilledIconButton(
                                            onClick = {
                                                if (minPatternDots > 4) {
                                                    minPatternDots--
                                                    patternStep = 1
                                                    firstPattern = ""
                                                    secondPattern = ""
                                                    confirmedPattern = null
                                                    statusMessage = null
                                                }
                                            },
                                            enabled = minPatternDots > 4,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("pattern_min_dots_minus")
                                        ) {
                                            Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(width = 56.dp, height = 36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$minPatternDots",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        FilledIconButton(
                                            onClick = {
                                                if (minPatternDots < 9) {
                                                    minPatternDots++
                                                    patternStep = 1
                                                    firstPattern = ""
                                                    secondPattern = ""
                                                    confirmedPattern = null
                                                    statusMessage = null
                                                }
                                            },
                                            enabled = minPatternDots < 9,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("pattern_min_dots_plus")
                                        ) {
                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                PatternLockView(
                                    onPatternComplete = { patternStr ->
                                        val dotCount = patternStr.split("-").size
                                        if (patternStep == 1) {
                                            if (dotCount < minPatternDots) {
                                                isError = true
                                                statusMessage = "Pattern must connect at least $minPatternDots dots."
                                            } else {
                                                firstPattern = patternStr
                                                patternStep = 2
                                                isError = false
                                                statusMessage = "Pattern has $dotCount dots. Redraw to confirm."
                                            }
                                        } else {
                                            secondPattern = patternStr
                                            if (firstPattern == secondPattern) {
                                                confirmedPattern = secondPattern
                                                isError = false
                                                statusMessage = "Pattern confirmed! Tap SAVE to apply."
                                            } else {
                                                isError = true
                                                statusMessage = "Patterns do not match. Try again from step 1."
                                                patternStep = 1
                                                firstPattern = ""
                                                secondPattern = ""
                                                confirmedPattern = null
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = when {
                                        confirmedPattern != null -> "Pattern ready to save"
                                        patternStep == 1 -> "Draw your pattern (min $minPatternDots dots)"
                                        else -> "Redraw pattern to confirm"
                                    },
                                    color = if (confirmedPattern != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (confirmedPattern != null) FontWeight.Bold else FontWeight.Normal
                                )

                                if (patternStep == 2 || confirmedPattern != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(onClick = {
                                        patternStep = 1
                                        firstPattern = ""
                                        secondPattern = ""
                                        confirmedPattern = null
                                        statusMessage = null
                                    }) {
                                        Text("Reset & Start Over", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (confirmedPattern == null) {
                                            isError = true
                                            statusMessage = if (firstPattern.isEmpty()) "Please draw your pattern first." else "Please redraw pattern to confirm."
                                            return@Button
                                        }

                                        pinManager.savePattern(confirmedPattern!!, minPatternDots)
                                        pinManager.setAuthMethod(AuthMethod.PATTERN)
                                        isError = false
                                        statusMessage = "Pattern saved successfully!"
                                        Toast.makeText(context, "Pattern Saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_pattern_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("SAVE PATTERN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }

                        // Status message feedback
                        AnimatedVisibility(
                            visible = statusMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }

            // Section 3: Biometric Authentication
            item {
                Text(
                    text = "Biometric Authentication",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Fingerprint", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Unlock with biometric sensor", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }

                            Switch(
                                checked = fingerprintEnabled,
                                onCheckedChange = {
                                    fingerprintEnabled = it
                                    pinManager.setFingerprintEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Face", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Unlock with facial recognition", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }

                            Switch(
                                checked = faceEnabled,
                                onCheckedChange = {
                                    faceEnabled = it
                                    pinManager.setFaceEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                    }
                }
            }

            // Section 4: Prevent Uninstall Quick Access
            item {
                Text(
                    text = "Uninstall Security",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable { navController.navigate("prevent_uninstall") },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Prevent Uninstall", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (pinManager.isPreventUninstallEnabled()) "Active • Require PIN/Biometrics before removal" else "Off • Configure uninstall protection",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 5: [ SAVE ] Action Button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        pinManager.setAuthMethod(selectedMethod)
                        pinManager.setFingerprintEnabled(fingerprintEnabled)
                        pinManager.setFaceEnabled(faceEnabled)
                        Toast.makeText(context, "Authentication Settings Saved", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_password_lock_settings_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthMethodSelectOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

