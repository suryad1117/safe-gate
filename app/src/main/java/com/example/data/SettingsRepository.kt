package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safegate_settings")

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class AccentColor {
    NONE,
    BLUE,
    NAVY,
    PURPLE,
    PRUSSIAN_BLUE,
    BLACK,
    GREEN,
    RED,
    ORANGE,
    PINK,
    TEAL,
    CYAN
}

enum class AppStyle {
    CLASSIC,
    MODERN,
    MINIMAL
}

enum class LockScreenStyle {
    CLASSIC,
    MODERN,
    SECURE
}

enum class CornerStyle {
    SQUARE,
    ROUNDED,
    EXTRA_ROUNDED
}

enum class AppIconStyle {
    DEFAULT,
    SHIELD,
    LOCK
}

data class AppearanceSettings(
    val theme: AppTheme = AppTheme.DARK,
    val accentColor: AccentColor = AccentColor.PURPLE,
    val appStyle: AppStyle = AppStyle.MODERN,
    val lockScreenStyle: LockScreenStyle = LockScreenStyle.MODERN,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val animations: Boolean = true,
    val hapticFeedback: Boolean = true,
    val showAppIcons: Boolean = true
)

data class SafeGateSettings(
    val theme: AppTheme = AppTheme.DARK,
    val accentColor: AccentColor = AccentColor.PURPLE,
    val appStyle: AppStyle = AppStyle.MODERN,
    val lockScreenStyle: LockScreenStyle = LockScreenStyle.MODERN,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val animations: Boolean = true,
    val hapticFeedback: Boolean = true,
    val showAppIcons: Boolean = true,
    val appIconStyle: AppIconStyle = AppIconStyle.DEFAULT,
    val setupCompleted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val scheduleNotificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoLockTimeout: String = "immediately"
)

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_ACCENT = stringPreferencesKey("accent_color")
        val KEY_APP_STYLE = stringPreferencesKey("app_style")
        val KEY_LOCK_SCREEN = stringPreferencesKey("lock_screen_style")
        val KEY_CORNER_STYLE = stringPreferencesKey("corner_style")
        val KEY_ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val KEY_HAPTIC = booleanPreferencesKey("haptic_feedback_enabled")
        val KEY_SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        val KEY_APP_ICON = stringPreferencesKey("app_icon_style")
        val KEY_SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_SCHEDULE_NOTIFS = booleanPreferencesKey("schedule_notifications_enabled")
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEY_AUTO_LOCK = stringPreferencesKey("auto_lock_timeout")
    }

    val settingsFlow: Flow<SafeGateSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeStr = preferences[KEY_THEME] ?: AppTheme.DARK.name
            val accentStr = preferences[KEY_ACCENT] ?: AccentColor.PURPLE.name
            val appStyleStr = preferences[KEY_APP_STYLE] ?: AppStyle.MODERN.name
            val lockScreenStr = preferences[KEY_LOCK_SCREEN] ?: LockScreenStyle.MODERN.name
            val cornerStr = preferences[KEY_CORNER_STYLE] ?: CornerStyle.ROUNDED.name
            val animations = preferences[KEY_ANIMATIONS] ?: true
            val haptic = preferences[KEY_HAPTIC] ?: true
            val showIcons = preferences[KEY_SHOW_APP_ICONS] ?: true
            val iconStr = preferences[KEY_APP_ICON] ?: AppIconStyle.DEFAULT.name
            val setupDone = preferences[KEY_SETUP_COMPLETED] ?: false
            val notifs = preferences[KEY_NOTIFICATIONS] ?: true
            val schedNotifs = preferences[KEY_SCHEDULE_NOTIFS] ?: true
            val vib = preferences[KEY_VIBRATION] ?: true
            val autoLock = preferences[KEY_AUTO_LOCK] ?: "immediately"

            SafeGateSettings(
                theme = parseEnum(themeStr, AppTheme.DARK),
                accentColor = parseEnum(accentStr, AccentColor.PURPLE),
                appStyle = parseEnum(appStyleStr, AppStyle.MODERN),
                lockScreenStyle = parseEnum(lockScreenStr, LockScreenStyle.MODERN),
                cornerStyle = parseEnum(cornerStr, CornerStyle.ROUNDED),
                animations = animations,
                hapticFeedback = haptic,
                showAppIcons = showIcons,
                appIconStyle = parseEnum(iconStr, AppIconStyle.DEFAULT),
                setupCompleted = setupDone,
                notificationsEnabled = notifs,
                scheduleNotificationsEnabled = schedNotifs,
                vibrationEnabled = vib,
                autoLockTimeout = autoLock
            )
        }

    suspend fun saveAppearance(
        theme: AppTheme,
        accent: AccentColor,
        appStyle: AppStyle = AppStyle.MODERN,
        lockScreen: LockScreenStyle = LockScreenStyle.MODERN,
        cornerStyle: CornerStyle = CornerStyle.ROUNDED,
        animations: Boolean = true,
        hapticFeedback: Boolean = true,
        showAppIcons: Boolean = true,
        iconStyle: AppIconStyle = AppIconStyle.DEFAULT
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme.name
            preferences[KEY_ACCENT] = accent.name
            preferences[KEY_APP_STYLE] = appStyle.name
            preferences[KEY_LOCK_SCREEN] = lockScreen.name
            preferences[KEY_CORNER_STYLE] = cornerStyle.name
            preferences[KEY_ANIMATIONS] = animations
            preferences[KEY_HAPTIC] = hapticFeedback
            preferences[KEY_SHOW_APP_ICONS] = showAppIcons
            preferences[KEY_APP_ICON] = iconStyle.name
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SETUP_COMPLETED] = completed
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setScheduleNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCHEDULE_NOTIFS] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VIBRATION] = enabled
        }
    }

    suspend fun setAutoLockTimeout(timeout: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK] = timeout
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String, default: T): T {
        return try {
            java.lang.Enum.valueOf(T::class.java, value.uppercase())
        } catch (e: Exception) {
            default
        }
    }
}
