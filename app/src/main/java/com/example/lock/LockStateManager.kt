package com.example.lock

import android.content.Context
import android.os.SystemClock
import com.example.data.SafeGateDatabase
import com.example.data.ScheduleEntity
import com.example.scheduling.ScheduleCalculator
import com.example.security.PinManager
import java.util.concurrent.ConcurrentHashMap

class LockStateManager(
    private val context: Context
) {
    private val database = SafeGateDatabase.getDatabase(context)
    val pinManager = PinManager(context)

    companion object {
        @Volatile
        private var instance: LockStateManager? = null

        fun getInstance(context: Context): LockStateManager {
            return instance ?: synchronized(this) {
                instance ?: LockStateManager(context.applicationContext).also { instance = it }
            }
        }

        // Active temporary session authorizations for DIRECT protected apps
        private val temporaryAuthorizations = ConcurrentHashMap<String, Long>()
        private const val DEFAULT_GRACE_PERIOD_MS = 60_000L

        fun grantTemporaryUnlock(packageName: String, durationMs: Long = DEFAULT_GRACE_PERIOD_MS) {
            val expiry = SystemClock.elapsedRealtime() + durationMs
            temporaryAuthorizations[packageName] = expiry
        }

        fun isTemporarilyUnlocked(packageName: String): Boolean {
            val expiry = temporaryAuthorizations[packageName] ?: return false
            return if (SystemClock.elapsedRealtime() < expiry) {
                true
            } else {
                temporaryAuthorizations.remove(packageName)
                false
            }
        }

        fun clearTemporaryUnlock(packageName: String) {
            temporaryAuthorizations.remove(packageName)
        }

        fun clearAllTemporaryUnlocks() {
            temporaryAuthorizations.clear()
        }
    }

    /**
     * Determines whether the given package must be locked right now.
     * Core Rule:
     * 1. If currently inside an active Schedule window (START <= now < END): STRICTLY LOCK (cannot be bypassed).
     * 2. If app is directly marked as Protected: LOCK unless temporarily authorized in session.
     */
    suspend fun shouldLock(
        packageName: String,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        // Do not lock SafeGate itself
        if (packageName == context.packageName) {
            return false
        }

        // 1. Check active schedules (Scheduled Lock: startDateTime <= currentTime < endDateTime)
        val activeSchedule = getActiveScheduleForApp(packageName, currentTime)
        if (activeSchedule != null) {
            // Scheduled lock cannot be bypassed by session unlock!
            clearTemporaryUnlock(packageName)
            UnlockSessionManager(context).clearPackage(packageName)
            return true
        }

        // 2. If not in active schedule, check session unlock for direct protection
        val sessionManager = UnlockSessionManager(context)
        if (isTemporarilyUnlocked(packageName) || sessionManager.isAuthorized(packageName)) {
            return false
        }

        // 3. Check direct protection (App Selection -> isProtected = true)
        val app = database.appDao().getApp(packageName)
        return app?.isProtected == true
    }

    suspend fun getActiveScheduleForApp(
        packageName: String,
        currentTime: Long = System.currentTimeMillis()
    ): ScheduleEntity? {
        val schedules = database.scheduleDao().getSchedulesForAppOnce(packageName)
        return schedules.firstOrNull { schedule ->
            schedule.enabled && ScheduleCalculator.isScheduleActiveNow(schedule, currentTime)
        }
    }

    fun verifyPin(pin: String): Boolean {
        return pinManager.verifyPin(pin)
    }

    fun isBiometricEnabled(): Boolean {
        return pinManager.isBiometricEnabled()
    }
}
