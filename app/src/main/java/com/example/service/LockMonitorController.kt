package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.permissions.PermissionUtils

object LockMonitorController {

    fun start(context: Context) {
        if (!PermissionUtils.allRequiredPermissionsGranted(context)) {
            Log.d("SafeGate", "Cannot start LockMonitorService: missing permissions")
            return
        }
        try {
            val intent = Intent(context, LockMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
            Log.d("SafeGate", "LockMonitorService started via controller")
        } catch (e: Exception) {
            Log.e("SafeGate", "Error starting LockMonitorService", e)
        }
    }

    fun stop(context: Context) {
        try {
            val intent = Intent(context, LockMonitorService::class.java)
            context.stopService(intent)
            Log.d("SafeGate", "LockMonitorService stopped via controller")
        } catch (e: Exception) {
            Log.e("SafeGate", "Error stopping LockMonitorService", e)
        }
    }
}
