package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.lock.LockActivity
import com.example.lock.LockStateManager
import com.example.permissions.PermissionUtils
import kotlinx.coroutines.*

class LockMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var lockStateManager: LockStateManager

    private var lastLockedPackage: String? = null
    private var lastLockLaunchTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        lockStateManager = LockStateManager.getInstance(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        Log.d("SafeGate", "LockMonitorService created and monitoring started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionUtils.allRequiredPermissionsGranted(this)) {
            Log.w("SafeGate", "LockMonitorService missing required permissions, stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    checkForegroundApplication()
                } catch (e: Exception) {
                    Log.e("SafeGate", "Error checking foreground app in LockMonitorService", e)
                }
                delay(600L)
            }
        }
    }

    private suspend fun checkForegroundApplication() {
        if (!PermissionUtils.allRequiredPermissionsGranted(applicationContext)) {
            return
        }

        val foregroundPackage = UsageStatsReader.getForegroundPackage(applicationContext) ?: return

        // If SafeGate itself (or LockActivity) is in foreground, reset tracked app
        if (foregroundPackage == applicationContext.packageName) {
            return
        }

        val now = System.currentTimeMillis()
        val shouldLock = lockStateManager.shouldLock(foregroundPackage, now)

        if (shouldLock) {
            // Prevent spamming intent in the same window cycle
            val elapsedSinceLastLaunch = SystemClock.elapsedRealtime() - lastLockLaunchTime
            if (foregroundPackage == lastLockedPackage && elapsedSinceLastLaunch < 1200) {
                return
            }

            lastLockedPackage = foregroundPackage
            lastLockLaunchTime = SystemClock.elapsedRealtime()

            launchLockScreen(foregroundPackage)
        } else {
            // If the user navigated away to an unlocked app, clear the lastLockedPackage tracker
            if (foregroundPackage != lastLockedPackage) {
                lastLockedPackage = null
            }
        }
    }

    private fun launchLockScreen(packageName: String) {
        Log.d("SafeGate", "Enforcing SafeGate lock barrier for protected app: $packageName")
        val intent = Intent(applicationContext, LockActivity::class.java).apply {
            putExtra("package_name", packageName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("SafeGate", "Failed to launch lock activity for $packageName", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protection Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors protected app usage and enforces schedules"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Safe Gate")
            .setContentText("Protection monitoring is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        Log.d("SafeGate", "LockMonitorService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "safegate_protection_monitor_channel"
    }
}
