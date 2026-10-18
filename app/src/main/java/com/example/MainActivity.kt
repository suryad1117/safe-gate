package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SafeGateDatabase
import com.example.data.SafeGateRepository
import com.example.data.SafeGateSettings
import com.example.data.SettingsRepository
import com.example.permissions.PermissionUtils
import com.example.security.PinManager
import com.example.service.LockMonitorController
import com.example.ui.PinViewModel
import com.example.ui.PinViewModelFactory
import com.example.ui.SafeGateViewModel
import com.example.ui.SafeGateViewModelFactory
import com.example.ui.SelectedAppState
import com.example.ui.theme.SafeGateTheme

class MainActivity : FragmentActivity() {
    private lateinit var pinManager: PinManager
    private var activePinViewModel: PinViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SafeGateDatabase.getDatabase(applicationContext)
        val repository = SafeGateRepository(database)
        pinManager = PinManager(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)

        // Ensure Safe Gate starts locked if protection is enabled (e.g. after removing from Recents)
        if (pinManager.isSafeGateProtectionEnabled()) {
            pinManager.lockSafeGate()
        }

        setContent {
            val safeGateViewModel: SafeGateViewModel = viewModel(
                factory = SafeGateViewModelFactory(repository)
            )

            val pinViewModel: PinViewModel = viewModel(
                factory = PinViewModelFactory(pinManager)
            )
            activePinViewModel = pinViewModel

            val selectedAppState: SelectedAppState = viewModel()

            val settings by settingsRepository.settingsFlow.collectAsState(initial = SafeGateSettings())

            SafeGateTheme(
                theme = settings.theme,
                accent = settings.accentColor,
                cornerStyle = settings.cornerStyle
            ) {
                SafeGateApp(
                    viewModel = safeGateViewModel,
                    pinViewModel = pinViewModel,
                    selectedAppState = selectedAppState,
                    settingsRepository = settingsRepository,
                    currentSettings = settings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LockMonitorController.start(this)
        if (::pinManager.isInitialized && pinManager.isSafeGateProtectionEnabled()) {
            if (pinManager.shouldLockDueToInactivity()) {
                pinManager.lockSafeGate()
                activePinViewModel?.lockApp()
            } else {
                pinManager.updateLastActiveTime()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::pinManager.isInitialized) {
            pinManager.updateLastActiveTime()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            // Re-lock Safe Gate whenever app is sent to background, user switches apps, or removes from Recents
            if (::pinManager.isInitialized && pinManager.isSafeGateProtectionEnabled()) {
                pinManager.updateLastActiveTime()
                if (pinManager.shouldLockDueToInactivity()) {
                    pinManager.lockSafeGate()
                    activePinViewModel?.lockApp()
                }
            }
        }
    }
}
