package com.example.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.receiver.SafeGateDeviceAdminReceiver

object DeviceAdminHelper {

    fun getDeviceAdminComponentName(context: Context): ComponentName {
        return ComponentName(context, SafeGateDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val component = getDeviceAdminComponentName(context)
            dpm?.isAdminActive(component) == true
        } catch (e: Exception) {
            Log.e("SafeGate", "Error checking device admin status", e)
            false
        }
    }

    fun getEnableDeviceAdminIntent(context: Context): Intent {
        val component = getDeviceAdminComponentName(context)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate SafeGate Device Administrator to prevent unauthorized uninstallation and removal without authentication."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun deactivateDeviceAdmin(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val component = getDeviceAdminComponentName(context)
            if (dpm?.isAdminActive(component) == true) {
                dpm.removeActiveAdmin(component)
                Log.d("SafeGate", "Device Admin successfully removed")
            }
        } catch (e: Exception) {
            Log.e("SafeGate", "Error removing device admin", e)
        }
    }

    fun getUninstallIntent(context: Context): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        return Intent(Intent.ACTION_DELETE, packageUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
