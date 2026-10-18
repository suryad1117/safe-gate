package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinAttemptState(
    val attemptsUsed: Int,
    val windowStart: Long
)

enum class AuthMethod {
    PIN,
    PASSWORD,
    PATTERN
}

class PinManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "safe_gate_security",
        Context.MODE_PRIVATE
    )

    companion object {
        const val MAX_ATTEMPTS = 3
        const val LOCKOUT_DURATION_MS = 3_600_000L // 1 hour = 3,600,000 ms
        const val WINDOW_DURATION_MS = 3_600_000L // 1 hour sliding window

        private const val KEY_ATTEMPTS_USED = "pin_attempts_used"
        private const val KEY_WINDOW_START = "pin_window_start"

        private const val AUTH_METHOD = "auth_method" // "PIN", "PASSWORD", "PATTERN"
        private const val CREDENTIAL_HASH = "credential_hash"
        private const val CREDENTIAL_SALT = "credential_salt"
        private const val MIN_PATTERN_DOTS = "min_pattern_dots"

        private const val FINGERPRINT_ENABLED = "fingerprint_enabled"
        private const val FACE_ENABLED = "face_enabled"
        private const val BIOMETRIC_ENABLED = "biometric_enabled"

        private const val AUTO_LOCK_TIMEOUT = "auto_lock_timeout" // "immediately", "1min", "5min"
        private const val THEME_SETTING = "theme_setting" // "dark", "light", "amoled", "system"
        private const val ACCENT_COLOR = "accent_color" // "purple", "blue", "green", "red", "orange"
        private const val LOCK_SCREEN_STYLE = "lock_screen_style" // "modern", "minimal", "secure"
        private const val APP_ICON_STYLE = "app_icon_style" // "default", "minimal", "shield", "lock"

        private const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val SCHEDULE_NOTIFS_ENABLED = "schedule_notifs_enabled"
        private const val VIBRATION_ENABLED = "vibration_enabled"
        private const val INTRUDER_SELFIE_ENABLED = "intruder_selfie_enabled"
        private const val VAULT_AUTO_LOCK = "vault_auto_lock"
        private const val PREVENT_UNINSTALL_ENABLED = "prevent_uninstall_enabled"

        private const val KEY_SAFEGATE_UNLOCKED = "safegate_session_unlocked"
        private const val KEY_SAFEGATE_UNLOCK_TIME = "safegate_unlock_timestamp"
        private const val KEY_IS_SETUP_COMPLETED = "safegate_setup_completed_flag"

        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
    }

    // ---------------- 3 ATTEMPTS PER HOUR SECURITY LOGIC ----------------
    fun getPinAttemptState(currentTime: Long = System.currentTimeMillis()): PinAttemptState {
        val attempts = preferences.getInt(KEY_ATTEMPTS_USED, 0)
        val windowStart = preferences.getLong(KEY_WINDOW_START, 0L)

        if (attempts > 0 && windowStart > 0L) {
            val elapsed = currentTime - windowStart
            if (elapsed >= WINDOW_DURATION_MS) {
                // 1 hour has elapsed: Reset counter & start fresh window
                resetPinAttempts()
                return PinAttemptState(attemptsUsed = 0, windowStart = 0L)
            }
            return PinAttemptState(attemptsUsed = attempts, windowStart = windowStart)
        }
        return PinAttemptState(attemptsUsed = 0, windowStart = 0L)
    }

    fun isPinBlocked(currentTime: Long = System.currentTimeMillis()): Boolean {
        val state = getPinAttemptState(currentTime)
        return state.attemptsUsed >= MAX_ATTEMPTS
    }

    fun getRemainingBlockTimeMillis(currentTime: Long = System.currentTimeMillis()): Long {
        val state = getPinAttemptState(currentTime)
        if (state.attemptsUsed >= MAX_ATTEMPTS && state.windowStart > 0L) {
            val remaining = (state.windowStart + WINDOW_DURATION_MS) - currentTime
            return remaining.coerceAtLeast(0L)
        }
        return 0L
    }

    fun getAttemptsUsed(currentTime: Long = System.currentTimeMillis()): Int {
        return getPinAttemptState(currentTime).attemptsUsed
    }

    fun getRemainingAttempts(currentTime: Long = System.currentTimeMillis()): Int {
        val used = getAttemptsUsed(currentTime)
        return (MAX_ATTEMPTS - used).coerceAtLeast(0)
    }

    fun recordFailedAttempt(currentTime: Long = System.currentTimeMillis()): PinAttemptState {
        val state = getPinAttemptState(currentTime)
        val newWindowStart = if (state.attemptsUsed == 0 || state.windowStart <= 0L) currentTime else state.windowStart
        val newAttempts = (state.attemptsUsed + 1).coerceAtMost(MAX_ATTEMPTS)

        preferences.edit()
            .putInt(KEY_ATTEMPTS_USED, newAttempts)
            .putLong(KEY_WINDOW_START, newWindowStart)
            .apply()

        return PinAttemptState(attemptsUsed = newAttempts, windowStart = newWindowStart)
    }

    fun resetPinAttempts() {
        preferences.edit()
            .putInt(KEY_ATTEMPTS_USED, 0)
            .putLong(KEY_WINDOW_START, 0L)
            .apply()
    }

    // ---------------- AUTH METHOD ----------------
    fun getAuthMethod(): AuthMethod {
        val methodStr = preferences.getString(AUTH_METHOD, AuthMethod.PIN.name)
        return try {
            AuthMethod.valueOf(methodStr ?: AuthMethod.PIN.name)
        } catch (e: Exception) {
            AuthMethod.PIN
        }
    }

    fun setAuthMethod(method: AuthMethod) {
        preferences.edit().putString(AUTH_METHOD, method.name).apply()
    }

    fun hasPin(): Boolean {
        return preferences.contains(CREDENTIAL_HASH) && preferences.contains(CREDENTIAL_SALT)
    }

    fun isAuthCredentialSet(): Boolean = hasPin()

    // ---------------- SAVE CREDENTIALS SECURELY ----------------
    fun saveCredential(credential: String, method: AuthMethod): Boolean {
        if (credential.isBlank()) return false
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashSecret(credential, salt)

        preferences.edit()
            .putString(AUTH_METHOD, method.name)
            .putString(CREDENTIAL_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(CREDENTIAL_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()

        // Reset rate limiter on new credential setup
        resetPinAttempts()
        return true
    }

    fun savePin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        return saveCredential(pin, AuthMethod.PIN)
    }

    fun savePassword(password: String): Boolean {
        if (password.length != 8) return false
        return saveCredential(password, AuthMethod.PASSWORD)
    }

    fun getMinPatternDots(): Int {
        return preferences.getInt(MIN_PATTERN_DOTS, 4).coerceIn(4, 9)
    }

    fun setMinPatternDots(count: Int) {
        val valid = count.coerceIn(4, 9)
        preferences.edit().putInt(MIN_PATTERN_DOTS, valid).apply()
    }

    fun savePattern(patternString: String, minDots: Int = getMinPatternDots()): Boolean {
        val dots = patternString.split("-")
        if (dots.size < minDots) return false
        setMinPatternDots(minDots)
        return saveCredential(patternString, AuthMethod.PATTERN)
    }

    // ---------------- VERIFY CREDENTIALS ----------------
    fun verifyCredential(input: String, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (isPinBlocked(currentTime)) {
            return false
        }

        val storedHash = preferences.getString(CREDENTIAL_HASH, null) ?: return false
        val storedSalt = preferences.getString(CREDENTIAL_SALT, null) ?: return false

        val isValid = try {
            val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
            val calculatedHash = hashSecret(input, salt)
            val expectedHash = Base64.decode(storedHash, Base64.NO_WRAP)
            calculatedHash.contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }

        if (isValid) {
            resetPinAttempts()
        } else {
            recordFailedAttempt(currentTime)
        }

        return isValid
    }

    fun verifyPin(pin: String, currentTime: Long = System.currentTimeMillis()): Boolean = verifyCredential(pin, currentTime)
    fun verifyPassword(password: String, currentTime: Long = System.currentTimeMillis()): Boolean = verifyCredential(password, currentTime)
    fun verifyPattern(patternString: String, currentTime: Long = System.currentTimeMillis()): Boolean = verifyCredential(patternString, currentTime)

    // ---------------- BIOMETRICS ----------------
    fun isFingerprintEnabled(): Boolean = preferences.getBoolean(FINGERPRINT_ENABLED, true)
    fun setFingerprintEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(FINGERPRINT_ENABLED, enabled).apply()
        updateBiometricAggregate()
    }

    fun isFaceEnabled(): Boolean = preferences.getBoolean(FACE_ENABLED, true)
    fun setFaceEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(FACE_ENABLED, enabled).apply()
        updateBiometricAggregate()
    }

    fun isBiometricEnabled(): Boolean = preferences.getBoolean(BIOMETRIC_ENABLED, true)
    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(BIOMETRIC_ENABLED, enabled)
            .putBoolean(FINGERPRINT_ENABLED, enabled)
            .putBoolean(FACE_ENABLED, enabled)
            .apply()
    }

    private fun updateBiometricAggregate() {
        val anyBio = isFingerprintEnabled() || isFaceEnabled()
        preferences.edit().putBoolean(BIOMETRIC_ENABLED, anyBio).apply()
    }

    // ---------------- AUTO LOCK & APPEARANCE ----------------
    fun getAutoLockTimeout(): String = preferences.getString(AUTO_LOCK_TIMEOUT, "immediately") ?: "immediately"
    fun setAutoLockTimeout(timeout: String) = preferences.edit().putString(AUTO_LOCK_TIMEOUT, timeout).apply()

    fun getThemeSetting(): String = preferences.getString(THEME_SETTING, "dark") ?: "dark"
    fun setThemeSetting(theme: String) = preferences.edit().putString(THEME_SETTING, theme).apply()

    fun getThemePreference(): String = getThemeSetting()
    fun setThemePreference(theme: String) = setThemeSetting(theme)

    fun getAccentColor(): String = preferences.getString(ACCENT_COLOR, "purple") ?: "purple"
    fun setAccentColor(accent: String) = preferences.edit().putString(ACCENT_COLOR, accent).apply()

    fun getLockScreenStyle(): String = preferences.getString(LOCK_SCREEN_STYLE, "modern") ?: "modern"
    fun setLockScreenStyle(style: String) = preferences.edit().putString(LOCK_SCREEN_STYLE, style).apply()

    fun getAppIconStyle(): String = preferences.getString(APP_ICON_STYLE, "default") ?: "default"
    fun setAppIconStyle(style: String) = preferences.edit().putString(APP_ICON_STYLE, style).apply()

    // ---------------- NOTIFICATIONS & HAPTICS ----------------
    fun isNotificationsEnabled(): Boolean = preferences.getBoolean(NOTIFICATIONS_ENABLED, true)
    fun setNotificationsEnabled(enabled: Boolean) = preferences.edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply()

    fun isScheduleNotificationsEnabled(): Boolean = preferences.getBoolean(SCHEDULE_NOTIFS_ENABLED, true)
    fun setScheduleNotificationsEnabled(enabled: Boolean) = preferences.edit().putBoolean(SCHEDULE_NOTIFS_ENABLED, enabled).apply()

    fun isVibrationEnabled(): Boolean = preferences.getBoolean(VIBRATION_ENABLED, true)
    fun setVibrationEnabled(enabled: Boolean) = preferences.edit().putBoolean(VIBRATION_ENABLED, enabled).apply()

    fun isIntruderSelfieEnabled(): Boolean = preferences.getBoolean(INTRUDER_SELFIE_ENABLED, false)
    fun setIntruderSelfieEnabled(enabled: Boolean) = preferences.edit().putBoolean(INTRUDER_SELFIE_ENABLED, enabled).apply()

    fun isDarkModeEnabled(): Boolean = preferences.getBoolean("dark_mode_enabled", true)
    fun setDarkModeEnabled(enabled: Boolean) = preferences.edit().putBoolean("dark_mode_enabled", enabled).apply()

    fun isVaultAutoLock(): Boolean = preferences.getBoolean(VAULT_AUTO_LOCK, true)
    fun setVaultAutoLock(enabled: Boolean) = preferences.edit().putBoolean(VAULT_AUTO_LOCK, enabled).apply()

    fun isPreventUninstallEnabled(): Boolean = preferences.getBoolean(PREVENT_UNINSTALL_ENABLED, false)
    fun setPreventUninstallEnabled(enabled: Boolean) = preferences.edit().putBoolean(PREVENT_UNINSTALL_ENABLED, enabled).apply()

    // ---------------- PERSISTENT LOCK & RECENT APPS PROTECTION ----------------
    fun isSafeGateProtectionEnabled(): Boolean = hasPin()

    fun isSafeGateUnlocked(): Boolean {
        return preferences.getBoolean(KEY_SAFEGATE_UNLOCKED, false)
    }

    fun setSafeGateUnlocked(unlocked: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SAFEGATE_UNLOCKED, unlocked)
            .putLong(KEY_SAFEGATE_UNLOCK_TIME, if (unlocked) System.currentTimeMillis() else 0L)
            .putLong("app_last_active_timestamp", if (unlocked) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun lockSafeGate() {
        setSafeGateUnlocked(false)
    }

    // ---------------- INACTIVITY LOCK (INDEPENDENT OF SCHEDULE) ----------------
    fun isAppInactivityLockEnabled(): Boolean = preferences.getBoolean("app_inactivity_lock_enabled", true)
    fun setAppInactivityLockEnabled(enabled: Boolean) = preferences.edit().putBoolean("app_inactivity_lock_enabled", enabled).apply()

    fun getInactivityLockMinutes(): Int = preferences.getInt("inactivity_lock_minutes", 0) // 0 = Immediately
    fun setInactivityLockMinutes(minutes: Int) = preferences.edit().putInt("inactivity_lock_minutes", minutes).apply()

    fun updateLastActiveTime(time: Long = System.currentTimeMillis()) {
        preferences.edit().putLong("app_last_active_timestamp", time).apply()
    }

    fun getLastActiveTime(): Long = preferences.getLong("app_last_active_timestamp", 0L)

    fun shouldLockDueToInactivity(currentTime: Long = System.currentTimeMillis()): Boolean {
        if (!isSafeGateProtectionEnabled()) return false
        if (!isAppInactivityLockEnabled()) return false
        val minutes = getInactivityLockMinutes()
        if (minutes <= 0) return true // Immediate lock upon reopen/background
        val lastActive = getLastActiveTime()
        if (lastActive <= 0L) return true
        val elapsed = currentTime - lastActive
        return elapsed >= (minutes * 60 * 1000L)
    }

    fun isSetupCompleted(): Boolean {
        return preferences.getBoolean(KEY_IS_SETUP_COMPLETED, false) || hasPin()
    }

    fun setSetupCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_IS_SETUP_COMPLETED, completed).apply()
    }

    // ---------------- HASHING HELPERS ----------------
    private fun hashSecret(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
