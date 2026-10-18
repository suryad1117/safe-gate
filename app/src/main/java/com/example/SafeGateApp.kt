package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppEntity
import com.example.data.SafeGateSettings
import com.example.data.SettingsRepository
import com.example.ui.PinViewModel
import com.example.ui.SafeGateViewModel
import com.example.ui.SelectedAppState
import com.example.ui.screens.*

@Composable
fun SafeGateApp(
    viewModel: SafeGateViewModel,
    pinViewModel: PinViewModel,
    selectedAppState: SelectedAppState,
    settingsRepository: SettingsRepository,
    currentSettings: SafeGateSettings
) {
    val navController = rememberNavController()
    val isPinSet by pinViewModel.isPinSet.collectAsState()
    val isAuthenticated by pinViewModel.isAuthenticated.collectAsState()

    var activeLockSimulation by remember { mutableStateOf<AppEntity?>(null) }

    val isSetupComplete = currentSettings.setupCompleted || pinViewModel.getPinManager().isSetupCompleted()
    val needsAppLock = isSetupComplete && isPinSet && !isAuthenticated

    val startDestination = remember(isSetupComplete, isPinSet) {
        if (!isSetupComplete) "welcome" else "dashboard"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // First Launch Setup Flow
            composable("welcome") {
                WelcomeScreen(navController = navController)
            }

            composable("onboarding_permissions") {
                OnboardingPermissionsScreen(navController = navController)
            }

            composable("onboarding_security") {
                OnboardingSecurityScreen(
                    navController = navController,
                    pinViewModel = pinViewModel,
                    pinManager = pinViewModel.getPinManager()
                )
            }

            composable("onboarding_appearance") {
                OnboardingAppearanceScreen(
                    navController = navController,
                    pinManager = pinViewModel.getPinManager(),
                    settingsRepository = settingsRepository,
                    currentSettings = currentSettings
                )
            }

            // Authentication Screen
            composable("pin") {
                PinScreen(
                    pinViewModel = pinViewModel,
                    isChangePinMode = false,
                    onPinSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("pin") { inclusive = true }
                        }
                    }
                )
            }

            composable("change_pin") {
                PinScreen(
                    pinViewModel = pinViewModel,
                    isChangePinMode = true,
                    onPinSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable("password_and_lock") {
                PasswordAndLockScreen(
                    navController = navController,
                    pinManager = pinViewModel.getPinManager()
                )
            }

            composable("biometrics") {
                BiometricsScreen(
                    navController = navController,
                    pinManager = pinViewModel.getPinManager()
                )
            }

            composable("prevent_uninstall") {
                PreventUninstallScreen(
                    navController = navController,
                    pinManager = pinViewModel.getPinManager()
                )
            }

            composable("appearance_settings") {
                AppearanceSettingsScreen(
                    navController = navController,
                    pinManager = pinViewModel.getPinManager(),
                    settingsRepository = settingsRepository,
                    currentSettings = currentSettings
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    navController = navController,
                    viewModel = viewModel,
                    onSimulateLock = { app ->
                        activeLockSimulation = app
                    }
                )
            }

            composable("apps") {
                AppSelectionScreen(
                    navController = navController,
                    viewModel = viewModel,
                    selectedAppState = selectedAppState
                )
            }

            composable("schedule") {
                ScheduleScreen(
                    navController = navController,
                    viewModel = viewModel,
                    selectedAppState = selectedAppState
                )
            }

            composable("select_apps_for_schedule") {
                SelectAppsForScheduleScreen(
                    navController = navController,
                    selectedAppState = selectedAppState
                )
            }

            composable("settings") {
                SettingsScreen(
                    navController = navController,
                    pinManager = pinViewModel.getPinManager()
                )
            }

            composable("vault") {
                VaultScreen(
                    navController = navController,
                    viewModel = viewModel,
                    initialTab = 0
                )
            }

            composable("vault_photos") {
                VaultScreen(
                    navController = navController,
                    viewModel = viewModel,
                    initialTab = 0
                )
            }

            composable("vault_files") {
                VaultScreen(
                    navController = navController,
                    viewModel = viewModel,
                    initialTab = 1
                )
            }

            composable("permissions") {
                PermissionCenterScreen(
                    navController = navController
                )
            }
        }

        // Real-Time Lock Simulation Shield Overlay
        AnimatedVisibility(
            visible = activeLockSimulation != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            activeLockSimulation?.let { targetApp ->
                LockScreenOverlay(
                    appName = targetApp.appName,
                    packageName = targetApp.packageName,
                    pinViewModel = pinViewModel,
                    onUnlocked = {
                        activeLockSimulation = null
                    },
                    onDismiss = {
                        activeLockSimulation = null
                    }
                )
            }
        }

        // Safe Gate persistent security lock (protects on start, reopen, recents swipe, etc.)
        if (needsAppLock) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                PinScreen(
                    pinViewModel = pinViewModel,
                    isChangePinMode = false,
                    onPinSuccess = {
                        // User entered correct PIN/Password/Pattern/Biometric
                    }
                )
            }
        }
    }
}
