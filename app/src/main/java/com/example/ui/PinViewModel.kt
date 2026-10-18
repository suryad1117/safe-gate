package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.security.PinAttemptState
import com.example.security.PinManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PinViewModel(
    private val pinManager: PinManager
) : ViewModel() {

    private val _isPinSet = MutableStateFlow(pinManager.hasPin())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(pinManager.isSafeGateUnlocked())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _attemptState = MutableStateFlow(pinManager.getPinAttemptState())
    val attemptState: StateFlow<PinAttemptState> = _attemptState.asStateFlow()

    private val _isBlocked = MutableStateFlow(pinManager.isPinBlocked())
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    private val _remainingBlockMillis = MutableStateFlow(pinManager.getRemainingBlockTimeMillis())
    val remainingBlockMillis: StateFlow<Long> = _remainingBlockMillis.asStateFlow()

    private var countdownJob: Job? = null

    init {
        refreshAttemptState()
    }

    fun refreshAttemptState() {
        val state = pinManager.getPinAttemptState()
        val blocked = pinManager.isPinBlocked()
        val remaining = pinManager.getRemainingBlockTimeMillis()

        _attemptState.value = state
        _isBlocked.value = blocked
        _remainingBlockMillis.value = remaining

        if (blocked) {
            startCountdownTimer()
        } else {
            countdownJob?.cancel()
            countdownJob = null
        }
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remaining = pinManager.getRemainingBlockTimeMillis()
                _remainingBlockMillis.value = remaining
                if (remaining <= 0) {
                    refreshAttemptState()
                    break
                }
                delay(1000L)
            }
        }
    }

    fun refreshPinStatus() {
        _isPinSet.value = pinManager.hasPin()
        refreshAttemptState()
    }

    fun createPin(pin: String): Boolean {
        val success = pinManager.savePin(pin)
        if (success) {
            _isPinSet.value = true
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
            pinManager.setSetupCompleted(true)
            refreshAttemptState()
        }
        return success
    }

    fun createPassword(password: String): Boolean {
        val success = pinManager.savePassword(password)
        if (success) {
            _isPinSet.value = true
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
            pinManager.setSetupCompleted(true)
            refreshAttemptState()
        }
        return success
    }

    fun createPattern(pattern: String): Boolean {
        val success = pinManager.savePattern(pattern)
        if (success) {
            _isPinSet.value = true
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
            pinManager.setSetupCompleted(true)
            refreshAttemptState()
        }
        return success
    }

    fun verifyPin(pin: String): Boolean {
        if (pinManager.isPinBlocked()) {
            refreshAttemptState()
            return false
        }
        val success = pinManager.verifyPin(pin)
        if (success) {
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
        }
        refreshAttemptState()
        return success
    }

    fun verifyPassword(password: String): Boolean {
        if (pinManager.isPinBlocked()) {
            refreshAttemptState()
            return false
        }
        val success = pinManager.verifyPassword(password)
        if (success) {
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
        }
        refreshAttemptState()
        return success
    }

    fun verifyPattern(pattern: String): Boolean {
        if (pinManager.isPinBlocked()) {
            refreshAttemptState()
            return false
        }
        val success = pinManager.verifyPattern(pattern)
        if (success) {
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
        }
        refreshAttemptState()
        return success
    }

    fun authenticateWithBiometric(): Boolean {
        if (pinManager.isBiometricEnabled()) {
            _isAuthenticated.value = true
            pinManager.setSafeGateUnlocked(true)
            return true
        }
        return false
    }

    fun lockApp() {
        pinManager.lockSafeGate()
        _isAuthenticated.value = false
    }

    fun getPinManager(): PinManager = pinManager
}

class PinViewModelFactory(
    private val pinManager: PinManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PinViewModel(pinManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
