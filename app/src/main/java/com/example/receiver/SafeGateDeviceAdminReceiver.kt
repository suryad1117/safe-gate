package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class SafeGateDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("SafeGate", "Device Admin protection enabled")
        Toast.makeText(context, "SafeGate Uninstall Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("SafeGate", "Device Admin protection disabled")
        Toast.makeText(context, "SafeGate Uninstall Protection Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d("SafeGate", "Device Admin disable requested")
        return "SafeGate Uninstall Protection is active. SafeGate authentication is required to deactivate protection or uninstall the application."
    }
}
