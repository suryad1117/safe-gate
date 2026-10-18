package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.security.AuthMethod
import com.example.security.BiometricHelper
import com.example.security.PinManager
import com.example.ui.PinViewModel
import com.example.ui.components.PatternLockView

@Composable
fun OnboardingSecurityScreen(
    navController: NavHostController,
    pinViewModel: PinViewModel,
    pinManager: PinManager
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf(AuthMethod.PIN) }

    // PIN State (Exactly 4 digits)
    var pinValue by remember { mutableStateOf("") }
    var confirmPinValue by remember { mutableStateOf("") }

    // Password State (Exactly 8 characters)
    var passwordValue by remember { mutableStateOf("") }
    var confirmPasswordValue by remember { mutableStateOf("") }

    // Pattern State
    var minPatternDots by remember { mutableStateOf(pinManager.getMinPatternDots()) }
    var patternValue by remember { mutableStateOf("") }
    var confirmPatternValue by remember { mutableStateOf("") }

    var isBiometricEnabled by remember { mutableStateOf(pinManager.isBiometricEnabled()) }
    val isBiometricAvailable = remember(context) { BiometricHelper.isBiometricSupported(context) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Button(
                    onClick = {
                        errorMessage = null
                        when (selectedMethod) {
                            AuthMethod.PIN -> {
                                if (pinValue.length != 4 || !pinValue.all { it.isDigit() }) {
                                    errorMessage = "PIN must be exactly 4 digits"
                                    return@Button
                                }
                                if (pinValue != confirmPinValue) {
                                    errorMessage = "PINs do not match"
                                    return@Button
                                }
                                pinManager.savePin(pinValue)
                                pinViewModel.createPin(pinValue)
                            }
                            AuthMethod.PASSWORD -> {
                                if (passwordValue.length != 8) {
                                    errorMessage = "Password must be exactly 8 characters"
                                    return@Button
                                }
                                if (passwordValue != confirmPasswordValue) {
                                    errorMessage = "Passwords do not match"
                                    return@Button
                                }
                                pinManager.savePassword(passwordValue)
                                pinViewModel.createPassword(passwordValue)
                            }
                            AuthMethod.PATTERN -> {
                                if (patternValue.split("-").size < minPatternDots) {
                                    errorMessage = "Pattern must connect at least $minPatternDots dots"
                                    return@Button
                                }
                                if (patternValue != confirmPatternValue) {
                                    errorMessage = "Patterns do not match"
                                    return@Button
                                }
                                pinManager.savePattern(patternValue, minPatternDots)
                                pinViewModel.createPattern(patternValue)
                            }
                        }

                        pinManager.setBiometricEnabled(isBiometricEnabled)
                        pinViewModel.refreshPinStatus()
                        Toast.makeText(context, "Security Method Created", Toast.LENGTH_SHORT).show()
                        navController.navigate("onboarding_appearance")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_security_continue_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "CONTINUE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
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
            contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "CREATE SECURITY",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose your primary unlock method for Safe Gate.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Method Selector Cards (PIN / Password / Pattern)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple(AuthMethod.PIN, "PIN", Icons.Default.Pin),
                        Triple(AuthMethod.PASSWORD, "Password", Icons.Default.Password),
                        Triple(AuthMethod.PATTERN, "Pattern", Icons.Default.Grid4x4)
                    ).forEach { (method, label, icon) ->
                        val isSelected = selectedMethod == method
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedMethod = method
                                    errorMessage = null
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                            ) else CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Credential Inputs based on selected method
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedMethod) {
                            AuthMethod.PIN -> {
                                Text(
                                    text = "CREATE PIN (Exactly 4 digits)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = pinValue,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinValue = it },
                                    label = { Text("Enter 4-digit PIN") },
                                    placeholder = { Text("• • • •") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_pin_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = confirmPinValue,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinValue = it },
                                    label = { Text("Confirm 4-digit PIN") },
                                    placeholder = { Text("• • • •") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_pin_confirm"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                            AuthMethod.PASSWORD -> {
                                Text(
                                    text = "CREATE PASSWORD (Exactly 8 characters)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = passwordValue,
                                    onValueChange = { if (it.length <= 8) passwordValue = it },
                                    label = { Text("Enter 8-character password") },
                                    placeholder = { Text("********") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_password_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = confirmPasswordValue,
                                    onValueChange = { if (it.length <= 8) confirmPasswordValue = it },
                                    label = { Text("Confirm password") },
                                    placeholder = { Text("********") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_password_confirm"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                            AuthMethod.PATTERN -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Minimum pattern length",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        FilledIconButton(
                                            onClick = {
                                                if (minPatternDots > 4) {
                                                    minPatternDots--
                                                    patternValue = ""
                                                    confirmPatternValue = ""
                                                }
                                            },
                                            enabled = minPatternDots > 4,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.size(34.dp).testTag("onboarding_pattern_minus")
                                        ) {
                                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(width = 52.dp, height = 34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$minPatternDots",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        FilledIconButton(
                                            onClick = {
                                                if (minPatternDots < 9) {
                                                    minPatternDots++
                                                    patternValue = ""
                                                    confirmPatternValue = ""
                                                }
                                            },
                                            enabled = minPatternDots < 9,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.size(34.dp).testTag("onboarding_pattern_plus")
                                        ) {
                                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = when {
                                            patternValue.isEmpty() -> "Draw your pattern (min $minPatternDots dots)"
                                            confirmPatternValue.isEmpty() -> "Redraw pattern to confirm"
                                            patternValue == confirmPatternValue -> "Pattern matched! Ready to continue"
                                            else -> "Patterns do not match. Redraw again."
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (patternValue.isNotEmpty() && patternValue == confirmPatternValue) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    PatternLockView(
                                        modifier = Modifier.size(240.dp),
                                        dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        activeColor = MaterialTheme.colorScheme.primary,
                                        lineColor = MaterialTheme.colorScheme.primary,
                                        onPatternComplete = { patternStr ->
                                            val count = patternStr.split("-").size
                                            if (patternValue.isEmpty()) {
                                                if (count < minPatternDots) {
                                                    errorMessage = "Pattern must connect at least $minPatternDots dots."
                                                } else {
                                                    errorMessage = null
                                                    patternValue = patternStr
                                                }
                                            } else {
                                                confirmPatternValue = patternStr
                                                if (patternValue != confirmPatternValue) {
                                                    errorMessage = "Patterns do not match. Try again."
                                                } else {
                                                    errorMessage = null
                                                }
                                            }
                                        }
                                    )
                                    if (patternValue.isNotEmpty()) {
                                        TextButton(onClick = {
                                            patternValue = ""
                                            confirmPatternValue = ""
                                            errorMessage = null
                                        }) {
                                            Text("Reset Pattern", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Biometric Option Toggle
            if (isBiometricAvailable) {
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
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Biometric Setup",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Use Fingerprint or Face to unlock",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { isBiometricEnabled = it },
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
