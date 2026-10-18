package com.example.lock

import android.content.Context

class UnlockSessionManager(
    context: Context
) {
    private val preferences = context.getSharedPreferences(
        "safe_gate_unlock_session",
        Context.MODE_PRIVATE
    )

    fun authorize(
        packageName: String,
        durationMillis: Long = 60_000
    ) {
        preferences.edit()
            .putString("package", packageName)
            .putLong("expires", System.currentTimeMillis() + durationMillis)
            .apply()
    }

    fun isAuthorized(
        packageName: String
    ): Boolean {
        val savedPackage = preferences.getString("package", null)
        val expires = preferences.getLong("expires", 0L)

        if (savedPackage != packageName) {
            return false
        }

        if (System.currentTimeMillis() > expires) {
            clear()
            return false
        }

        return true
    }

    fun clear() {
        preferences.edit()
            .clear()
            .apply()
    }

    fun clearPackage(packageName: String) {
        val savedPackage = preferences.getString("package", null)
        if (savedPackage == packageName) {
            clear()
        }
    }
}
